package com.devbytes.vixplayer.app.player

/**
 * A show's intro, as offsets from the start of an episode. Marked once by the user and
 * offered as "Skip intro" on the show's other episodes (Series Mode phase 3a).
 *
 * Pure data and rules — no Android or Media3 types — so it is unit-tested on the JVM.
 */
data class IntroWindow(val startMs: Long, val endMs: Long) {

    val lengthMs: Long get() = endMs - startMs

    /**
     * True while the skip should be offered: from the intro's start until shortly before
     * its end — the last moment isn't worth a button that would vanish under the thumb.
     */
    fun offersSkipAt(positionMs: Long): Boolean = positionMs >= startMs && positionMs < endMs - END_GRACE_MS

    companion object {
        /** Shorter than this isn't an intro worth a button. */
        const val MIN_LENGTH_MS = 5_000L
        /** Longer than this is a marking mistake, not an intro. */
        const val MAX_LENGTH_MS = 10 * 60_000L
        const val END_GRACE_MS = 1_000L
    }
}

/** Where the window in use came from — shown in the marking panel. */
enum class IntroSource { SHOW, EPISODE }

/** What the player knows about the current episode's intro. */
data class IntroSettings(
    val window: IntroWindow?,
    val source: IntroSource?,
    /** Skip without asking, once per opened episode. A per-show choice. */
    val autoSkip: Boolean,
) {
    companion object {
        val NONE = IntroSettings(window = null, source = null, autoSkip = false)
    }
}

/**
 * The window to use for one episode: its own override first, else the show's — and only
 * if it fits inside the episode ([durationMs] ≤ 0 means not known yet, so don't judge).
 */
fun resolveIntro(episode: IntroWindow?, show: IntroWindow?, durationMs: Long): Pair<IntroWindow, IntroSource>? {
    val (window, source) = when {
        episode != null -> episode to IntroSource.EPISODE
        show != null -> show to IntroSource.SHOW
        else -> return null
    }
    if (durationMs > 0 && window.endMs > durationMs) return null
    return window to source
}

/** Why a marked window can't be saved yet; null when it can. */
fun introMarkError(startMs: Long?, endMs: Long?, durationMs: Long): String? = when {
    startMs == null -> "Set where the intro starts"
    endMs == null -> "Set where the intro ends"
    endMs <= startMs -> "The end must come after the start"
    endMs - startMs < IntroWindow.MIN_LENGTH_MS -> "An intro is at least 5 seconds"
    endMs - startMs > IntroWindow.MAX_LENGTH_MS -> "An intro is at most 10 minutes"
    durationMs > 0 && endMs > durationMs -> "The end is past the episode's end"
    else -> null
}

/** "0:42", "1:05:09" — mm:ss, with hours when needed. */
fun formatIntroTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000).toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.ROOT, "%d:%02d", m, s)
}

/** "0:42–1:52" for the options tile. */
fun IntroWindow.label(): String = "${formatIntroTime(startMs)}–${formatIntroTime(endMs)}"
