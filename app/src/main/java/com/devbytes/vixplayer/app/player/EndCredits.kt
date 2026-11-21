package com.devbytes.vixplayer.app.player

/**
 * An episode's end credits, as offsets from its start — found in its audio by comparing its
 * ending with its neighbours' (Series Mode phase 4). Once the credits start, the player offers
 * the next episode instead of waiting for the file to end.
 *
 * Pure data and rules — no Android or Media3 types — so it is unit-tested on the JVM.
 */
data class CreditsWindow(val startMs: Long, val endMs: Long)

/** What credits detection concluded for one file. */
sealed interface DetectedCredits {
    data class Found(val window: CreditsWindow) : DetectedCredits
    /** Scanned; the ending shares nothing with the other episodes that looks like credits. */
    data object None : DetectedCredits
}

/** Seconds "Next episode" counts down at the credits when autoplay is on. */
const val CREDITS_COUNTDOWN_S = 10

/**
 * Whether to offer the next episode at [positionMs]: from the credits' start to the end of
 * the file (a tag or a "next time" after the credits still counts), only when there is a next
 * episode, and not once the viewer chose to watch the credits. Credits that start in the first
 * half of the file ([durationMs] ≤ 0 means not known yet) are a detection mistake — ignored,
 * so nothing ever offers to cut an episode short.
 */
fun offersNextEpisode(
    credits: CreditsWindow?,
    positionMs: Long,
    durationMs: Long,
    hasNext: Boolean,
    watchingCredits: Boolean,
): Boolean {
    if (credits == null || !hasNext || watchingCredits) return false
    if (durationMs > 0 && credits.startMs < durationMs / 2) return false
    return positionMs >= credits.startMs
}
