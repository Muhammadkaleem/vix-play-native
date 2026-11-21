package com.devbytes.vixplayer.app.player.intro

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.devbytes.vixplayer.app.BuildConfig
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.ShowRepository
import com.devbytes.vixplayer.app.data.series.ShowEpisode
import com.devbytes.vixplayer.app.player.DetectedCredits
import com.devbytes.vixplayer.app.player.DetectedIntro
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Finds the intro and the end credits in every episode of one season of a show (Series Mode
 * phases 3b and 4): decodes each episode's opening and ending, fingerprints them, and asks
 * [IntroMatcher] what they share with their neighbours. Results land per file in
 * [PlaybackRepository]; the season is then marked as scanned with its episode list — intros
 * and credits separately — so a newly added episode triggers a fresh scan, and a season
 * scanned for intros before credits existed gets a credits-only pass.
 *
 * Runs in the app's own process, started when an episode opens — not a WorkManager job: a
 * deferred job can wait minutes on some phones, and an expedited one needs quotas and (below
 * Android 12) a notification, for a few seconds of work. If the process dies mid-scan the
 * season simply isn't marked scanned, and the next episode opened starts it again.
 */
@Singleton
class IntroDetection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shows: ShowRepository,
    private val playback: PlaybackRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scanning = MutableStateFlow<Set<String>>(emptySet())

    /** Scans [season] of [showKey] unless it was already scanned with these [episodeIds]. */
    fun ensureScanned(showKey: String, season: Int, episodeIds: List<Long>, currentId: Long?) {
        val signature = episodeIds.sorted().joinToString(",")
        if (playback.scannedSeason(showKey, season) == signature && playback.creditsScannedSeason(showKey, season) == signature) return
        // One scan per show at a time; another season is picked up the next time one of its episodes opens.
        var started = false
        scanning.update { if (showKey in it) it else { started = true; it + showKey } }
        if (!started) return
        scope.launch {
            try {
                runCatching { scan(showKey, season, currentId) }.onFailure { Log.w(TAG, "scan of $showKey failed", it) }
            } finally {
                scanning.update { it - showKey }
            }
        }
    }

    /** True while [showKey] is being scanned. */
    fun running(showKey: String): Flow<Boolean> = scanning.map { showKey in it }

    /** Bumps for [showKey] when the episode being watched has its result, before the rest of the season. */
    private val ready = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun episodeReady(showKey: String): Flow<Int> = ready.map { it[showKey] ?: 0 }

    private fun onEpisodeReady(showKey: String) = ready.update { it + (showKey to (it[showKey] ?: 0) + 1) }

    private suspend fun scan(showKey: String, season: Int, currentId: Long?) = coroutineScope {
        val entry = shows.show(showKey) ?: return@coroutineScope
        val inSeason = entry.show.episodes.filter { it.info.season == season }
        if (inSeason.isEmpty()) return@coroutineScope
        val signature = signature(inSeason)
        val needIntros = playback.scannedSeason(showKey, season) != signature
        val needCredits = playback.creditsScannedSeason(showKey, season) != signature
        if (!needIntros && !needCredits) return@coroutineScope

        // A season of one is compared with the show's other episodes — themes rarely change.
        val references = if (inSeason.size >= 2) emptyList()
        else entry.show.episodes.filter { it.info.season != season }.take(REFERENCES)
        val targets = inSeason.take(MAX_EPISODES)
        val episodes = targets + references
        val started = SystemClock.elapsedRealtime()
        val job = coroutineContext[Job]
        val openings = arrayOfNulls<IntArray>(episodes.size)
        val endings = arrayOfNulls<AudioPrint>(episodes.size)
        val attempted = BooleanArray(episodes.size)
        // A few episodes decode side by side: ~6 s each with FFmpeg, and the MediaCodec
        // fallback (AC-3) is mostly waiting on the system's codec process anyway.
        val gate = Semaphore(PARALLEL)

        suspend fun fingerprint(indices: List<Int>) = indices.filter { !attempted[it] }.map { i ->
            attempted[i] = true
            async {
                gate.withPermit {
                    if (job?.isActive == false) return@withPermit
                    val episode = episodes[i]
                    val uri = entry.videoOf(episode)?.uri ?: return@withPermit
                    val durationMs = episode.file.durationMs
                    val keepGoing = { job?.isActive != false }
                    val t0 = SystemClock.elapsedRealtime()
                    if (needIntros) {
                        val limitMs = minOf(OPENING_MS, durationMs / 2).takeIf { it > 0 } ?: OPENING_MS
                        openings[i] = IntroAudioDecoder.fingerprint(context, uri, limitMs, keepGoing)
                        if (BuildConfig.DEBUG) openings[i]?.let { dumpForTuning(episode, "", it) }
                    }
                    // The ending — never more than the second half, so short episodes don't overlap the opening scan.
                    if (needCredits && durationMs > 0) {
                        val fromMs = maxOf(durationMs - ENDING_MS, durationMs / 2)
                        endings[i] = IntroAudioDecoder.fingerprintSpan(context, uri, fromMs, Long.MAX_VALUE / 1000, keepGoing)
                        if (BuildConfig.DEBUG) endings[i]?.let { dumpForTuning(episode, "_end", it.codes) }
                    }
                    Log.i(TAG, "${episode.info.code}: opening ${openings[i]?.size ?: "-"} / ending ${endings[i]?.codes?.size ?: "-"} codes in ${SystemClock.elapsedRealtime() - t0} ms")
                }
            }
        }.awaitAll()

        // The nearest fingerprinted episodes on either side — and the references for a lone episode.
        fun neighbours(i: Int, has: (Int) -> Boolean): List<Int> {
            val usable = episodes.indices.filter(has)
            val position = usable.indexOf(i)
            return (usable.subList(maxOf(0, position - 2), minOf(usable.size, position + 3)) +
                usable.filter { it >= targets.size }).distinct().filter { it != i }.take(NEIGHBOURS)
        }

        fun detect(i: Int) {
            val episode = targets[i]
            if (needIntros && openings[i] != null) {
                val window = IntroMatcher.introOf(i, openings.map { it ?: IntArray(0) }, neighbours(i) { openings[it] != null })
                playback.saveDetectedIntro(episode.file.id, window?.let { DetectedIntro.Found(it) } ?: DetectedIntro.None)
                Log.i(TAG, "${episode.info.code} intro: ${window ?: "none"}")
            }
            if (needCredits && endings[i] != null) {
                val credits = IntroMatcher.creditsOf(
                    i,
                    endings.map { it?.codes ?: IntArray(0) },
                    endings.map { it?.startMs ?: 0L },
                    episode.file.durationMs,
                    neighbours(i) { endings[it] != null },
                )
                playback.saveDetectedCredits(episode.file.id, credits?.let { DetectedCredits.Found(it) } ?: DetectedCredits.None)
                Log.i(TAG, "${episode.info.code} credits: ${credits ?: "none"}")
            }
        }

        // First the episode being watched and its nearest neighbours, so its result is ready
        // after one round of decoding rather than after the whole season.
        val current = targets.indexOfFirst { it.file.id == currentId }
        if (current >= 0) {
            val nearest = targets.indices.filter { it != current }.sortedBy { kotlin.math.abs(it - current) }.take(PARALLEL - 1)
            fingerprint(listOf(current) + nearest + references.indices.map { it + targets.size })
            detect(current)
            onEpisodeReady(showKey)
        }
        fingerprint(episodes.indices.toList())
        if (job?.isActive == false) return@coroutineScope
        targets.indices.forEach(::detect)
        if (needIntros) playback.markSeasonScanned(showKey, season, signature)
        if (needCredits) playback.markCreditsScanned(showKey, season, signature)
        Log.i(TAG, "$showKey S$season done in ${SystemClock.elapsedRealtime() - started} ms")
    }

    /** Debug builds only: keeps each fingerprint in the cache so detection can be tuned offline. */
    private fun dumpForTuning(episode: ShowEpisode, suffix: String, codes: IntArray) {
        runCatching {
            val dir = java.io.File(context.cacheDir, "intro").apply { mkdirs() }
            java.io.DataOutputStream(java.io.File(dir, "${episode.info.showKey}_${episode.info.code}$suffix.bin").outputStream().buffered())
                .use { out -> codes.forEach { out.writeInt(it) } }
        }
    }

    private fun signature(episodes: List<ShowEpisode>) = episodes.map { it.file.id }.sorted().joinToString(",")

    private companion object {
        const val TAG = "IntroDetect"
        /** Intros sit near the start; ten minutes covers long cold opens. */
        const val OPENING_MS = 10 * 60_000L
        /** Credits sit at the end; six minutes covers long rolls plus a tag or preview after them. */
        const val ENDING_MS = 6 * 60_000L
        const val MAX_EPISODES = 16
        const val NEIGHBOURS = 4
        const val PARALLEL = 4
        const val REFERENCES = 3
    }
}
