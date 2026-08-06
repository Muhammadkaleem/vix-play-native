package com.devbytes.vixplayer.app.notifications

import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides what (if anything) Library Pulse posts on a given run. One notification per run,
 * global min-gap cap, quiet-hours aware, precedence Resume > New videos > Digest, and
 * "acknowledge on open" so the app never nags about things the user has already dealt with.
 *
 * [evaluate] also commits the bookkeeping for whatever it returns, so a caller that gets a
 * non-null result should post it; the run's state has already advanced.
 */
@Singleton
class PulseEvaluator @Inject constructor(
    private val settings: SettingsRepository,
    private val playback: PlaybackRepository,
    private val media: MediaRepository,
) {
    /** Records app usage: kills the idle condition and acknowledges the current library. */
    suspend fun onAppOpened(now: Long = System.currentTimeMillis()) {
        settings.setPulseLastAppOpenAt(now)
        val newest = media.queryNewestVideoAddedDate()
        val seen = settings.pulseState().lastSeenNewestVideoDate
        if (newest > seen) settings.setPulseLastSeenNewestVideoDate(newest)
    }

    suspend fun evaluate(now: Long = System.currentTimeMillis()): PulseNotification? {
        val s = settings.pulseSettings.first()
        if (!s.master) return null

        val state = settings.pulseState()
        if (now - state.lastNotificationAt < MIN_GAP_MILLIS) return null

        // Quiet hours: never post overnight.
        val hour = LocalTime.now().hour
        if (hour >= QUIET_START_HOUR || hour < QUIET_END_HOUR) return null

        // 1. Resume nudge — highest priority when the app has gone quiet.
        if (s.resume && now - state.lastAppOpenAt >= IDLE_MILLIS) {
            val candidate = findResumeCandidate()
            if (candidate != null && candidate.id != state.lastResumeNudgeId) {
                val minsLeft = ((candidate.durationMs - candidate.positionMs) / 60_000L).toInt()
                val text = if (minsLeft in 1..600) {
                    "You're $minsLeft min from the end of ${candidate.name}"
                } else {
                    "Pick up ${candidate.name} where you left off"
                }
                commit(now, resumeId = candidate.id)
                return PulseNotification(PulseTrigger.RESUME, "Continue watching", text, candidate.uri)
            }
        }

        // 2. New in library — only after a baseline exists (post-first-open).
        if (s.newVideos && state.lastSeenNewestVideoDate > 0L) {
            val newest = media.queryNewestVideoAddedDate()
            if (newest > state.lastSeenNewestVideoDate) {
                val count = media.countVideosAddedAfter(state.lastSeenNewestVideoDate)
                if (count > 0) {
                    val text = if (count == 1) "1 new video in your library" else "$count new videos in your library"
                    commit(now, seenNewest = newest)
                    return PulseNotification(PulseTrigger.NEW_VIDEOS, "New in your library", text)
                }
            }
        }

        // 3. Weekly digest — only if nothing more specific fired.
        if (s.digest && now - state.lastDigestAt >= DIGEST_MILLIS) {
            commit(now, digestAt = now)
            return PulseNotification(
                PulseTrigger.DIGEST,
                "Your week in VixPlay",
                "See what you watched and what's still waiting.",
            )
        }

        return null
    }

    private data class ResumeCandidate(
        val id: Long,
        val uri: String,
        val name: String,
        val positionMs: Long,
        val durationMs: Long,
    )

    /** The most recently played still-in-progress video (2%..97%), or null. */
    private suspend fun findResumeCandidate(): ResumeCandidate? {
        playback.getRecentPositions(20).forEach { pos ->
            val video = media.queryVideoById(pos.mediaStoreId) ?: return@forEach
            if (video.durationMs <= 0L) return@forEach
            val fraction = pos.positionMs.toFloat() / video.durationMs
            if (fraction in 0.02f..0.97f) {
                return ResumeCandidate(
                    id = video.mediaStoreId,
                    uri = video.uri.toString(),
                    name = video.name.substringBeforeLast('.'),
                    positionMs = pos.positionMs,
                    durationMs = video.durationMs,
                )
            }
        }
        return null
    }

    /** Advances the shared cap timestamp plus whichever trigger-specific marker fired. */
    private suspend fun commit(now: Long, resumeId: Long? = null, seenNewest: Long? = null, digestAt: Long? = null) {
        settings.setPulseLastNotificationAt(now)
        resumeId?.let { settings.setPulseLastResumeNudgeId(it) }
        seenNewest?.let { settings.setPulseLastSeenNewestVideoDate(it) }
        digestAt?.let { settings.setPulseLastDigestAt(it) }
    }

    private companion object {
        val IDLE_MILLIS = TimeUnit.DAYS.toMillis(3)
        val MIN_GAP_MILLIS = TimeUnit.DAYS.toMillis(3)
        val DIGEST_MILLIS = TimeUnit.DAYS.toMillis(7)
        const val QUIET_START_HOUR = 22
        const val QUIET_END_HOUR = 8
    }
}
