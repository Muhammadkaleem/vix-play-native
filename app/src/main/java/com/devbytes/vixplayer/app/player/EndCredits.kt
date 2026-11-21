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
    /** The user said "Not the credits" — never offered again for this file. */
    data object Rejected : DetectedCredits
}

/** Where the credits in use came from — shown in the marking card. */
enum class CreditsSource { SHOW, EPISODE, DETECTED }

/**
 * The credits to use for one episode: a start marked for it, else what detection found in it,
 * else the show's mark — kept as time before the end ([showFromEndMs]), since episode lengths
 * vary while credit rolls don't. Credits run to the end of the file. A show mark needs the
 * duration; one that doesn't fit this episode (a short special) is dropped.
 */
fun resolveCredits(
    episodeStartMs: Long?,
    detected: CreditsWindow?,
    showFromEndMs: Long?,
    durationMs: Long,
): Pair<CreditsWindow, CreditsSource>? {
    val (window, source) = when {
        episodeStartMs != null -> CreditsWindow(episodeStartMs, maxOf(durationMs, episodeStartMs)) to CreditsSource.EPISODE
        detected != null -> detected to CreditsSource.DETECTED
        showFromEndMs != null && durationMs > 0 -> CreditsWindow(durationMs - showFromEndMs, durationMs) to CreditsSource.SHOW
        else -> return null
    }
    if (durationMs > 0 && (window.startMs < durationMs / 2 || window.startMs >= durationMs)) return null
    return window to source
}

/** Why a marked credits start can't be saved yet; null when it can. */
fun creditsMarkError(startMs: Long?, durationMs: Long): String? = when {
    startMs == null -> "Set where the credits start"
    durationMs > 0 && startMs < durationMs / 2 -> "Credits are in the second half of an episode"
    durationMs > 0 && durationMs - startMs < MIN_CREDITS_MS -> "Credits run at least 5 seconds"
    else -> null
}

/** Shorter than this before the end isn't worth a Next episode button. */
const val MIN_CREDITS_MS = 5_000L

/** "From 45:10" for the options tile. */
fun CreditsWindow.label(): String = "From ${formatIntroTime(startMs)}"

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
