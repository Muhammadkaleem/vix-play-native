package com.devbytes.vixplayer.app.player.intro

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.devbytes.vixplayer.app.BuildConfig
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.ShowRepository
import com.devbytes.vixplayer.app.data.series.ShowEpisode
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
 * Finds the intro in every episode of one season of a show (Series Mode phase 3b): decodes
 * each episode's opening, fingerprints it, and asks [IntroMatcher] what it shares with its
 * neighbours. Results land per file in [PlaybackRepository]; the season is then marked as
 * scanned with its episode list, so a newly added episode triggers a fresh scan.
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
        if (playback.scannedSeason(showKey, season) == episodeIds.sorted().joinToString(",")) return
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
        if (playback.scannedSeason(showKey, season) == signature) return@coroutineScope

        // A season of one is compared with the show's other episodes — themes rarely change.
        val references = if (inSeason.size >= 2) emptyList()
        else entry.show.episodes.filter { it.info.season != season }.take(REFERENCES)
        val targets = inSeason.take(MAX_EPISODES)
        val episodes = targets + references
        val started = SystemClock.elapsedRealtime()
        val job = coroutineContext[Job]
        val prints = arrayOfNulls<IntArray>(episodes.size)
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
                    val limitMs = minOf(SCAN_MS, episode.file.durationMs / 2).takeIf { it > 0 } ?: SCAN_MS
                    val t0 = SystemClock.elapsedRealtime()
                    prints[i] = IntroAudioDecoder.fingerprint(context, uri, limitMs) { job?.isActive != false }
                    Log.i(TAG, "${episode.info.code}: ${prints[i]?.size ?: "no audio"} codes in ${SystemClock.elapsedRealtime() - t0} ms")
                    if (BuildConfig.DEBUG) prints[i]?.let { dumpForTuning(episode, it) }
                }
            }
        }.awaitAll()

        fun detect(i: Int) {
            if (prints[i] == null) return
            // The nearest fingerprinted episodes on either side — and the references for a lone episode.
            val usable = prints.indices.filter { prints[it] != null }
            val position = usable.indexOf(i)
            val neighbours = (usable.subList(maxOf(0, position - 2), minOf(usable.size, position + 3)) +
                usable.filter { it >= targets.size }).distinct().filter { it != i }.take(NEIGHBOURS)
            val window = IntroMatcher.introOf(i, prints.map { it ?: IntArray(0) }, neighbours)
            playback.saveDetectedIntro(targets[i].file.id, window?.let { DetectedIntro.Found(it) } ?: DetectedIntro.None)
            Log.i(TAG, "${targets[i].info.code}: ${window ?: "none"}")
        }

        // First the episode being watched and its nearest neighbours, so its intro is ready
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
        playback.markSeasonScanned(showKey, season, signature)
        Log.i(TAG, "$showKey S$season done in ${SystemClock.elapsedRealtime() - started} ms")
    }

    /** Debug builds only: keeps each fingerprint in the cache so detection can be tuned offline. */
    private fun dumpForTuning(episode: ShowEpisode, codes: IntArray) {
        runCatching {
            val dir = java.io.File(context.cacheDir, "intro").apply { mkdirs() }
            java.io.DataOutputStream(java.io.File(dir, "${episode.info.showKey}_${episode.info.code}.bin").outputStream().buffered())
                .use { out -> codes.forEach { out.writeInt(it) } }
        }
    }

    private fun signature(episodes: List<ShowEpisode>) = episodes.map { it.file.id }.sorted().joinToString(",")

    private companion object {
        const val TAG = "IntroDetect"
        /** Intros sit near the start; ten minutes covers long cold opens. */
        const val SCAN_MS = 10 * 60_000L
        const val MAX_EPISODES = 16
        const val NEIGHBOURS = 4
        const val PARALLEL = 4
        const val REFERENCES = 3
    }
}
