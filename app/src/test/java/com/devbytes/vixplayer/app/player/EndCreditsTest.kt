package com.devbytes.vixplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndCreditsTest {

    private val credits = CreditsWindow(startMs = 2_700_000, endMs = 2_790_000) // 45:00–46:30
    private val duration = 2_800_000L

    private fun offers(position: Long, hasNext: Boolean = true, watching: Boolean = false, window: CreditsWindow? = credits, durationMs: Long = duration) =
        offersNextEpisode(window, position, durationMs, hasNext, watching)

    @Test fun offeredFromTheCreditsToTheEnd() {
        assertFalse(offers(2_699_000))
        assertTrue(offers(2_700_000))
        // After the credit roll (a tag, a preview) it still is.
        assertTrue(offers(2_795_000))
    }

    @Test fun notWithoutANextEpisode() = assertFalse(offers(2_750_000, hasNext = false))

    @Test fun notOnceWatchingTheCredits() = assertFalse(offers(2_750_000, watching = true))

    @Test fun notWithoutCredits() = assertFalse(offers(2_750_000, window = null))

    @Test fun creditsInTheFirstHalfAreIgnored() {
        // A detection mistake must never cut an episode short.
        val early = CreditsWindow(startMs = 600_000, endMs = 700_000)
        assertFalse(offers(650_000, window = early))
    }

    @Test fun unknownDurationDoesNotBlock() = assertTrue(offers(2_750_000, durationMs = 0))

    // ── Marked by hand ────────────────────────────────────────────────────────────────────

    @Test fun anEpisodesOwnMarkBeatsDetectionAndTheShow() {
        val (window, source) = resolveCredits(episodeStartMs = 2_650_000, detected = credits, showFromEndMs = 60_000, durationMs = duration)!!
        assertEquals(CreditsSource.EPISODE, source)
        assertEquals(2_650_000L, window.startMs)
    }

    @Test fun detectionBeatsTheShowMark() =
        assertEquals(CreditsSource.DETECTED, resolveCredits(null, credits, 60_000, duration)!!.second)

    @Test fun theShowMarkIsTimeBeforeTheEnd() {
        // Marked as "the last 1:30" — lands 1:30 before the end of episodes of any length.
        val short = resolveCredits(null, null, showFromEndMs = 90_000, durationMs = 2_400_000)!!
        val long = resolveCredits(null, null, showFromEndMs = 90_000, durationMs = 3_600_000)!!
        assertEquals(2_310_000L, short.first.startMs)
        assertEquals(3_510_000L, long.first.startMs)
        assertEquals(CreditsSource.SHOW, long.second)
    }

    @Test fun aShowMarkThatDoesntFitIsDropped() {
        // 30 minutes before the end of a 40-minute special is in its first half.
        assertNull(resolveCredits(null, null, showFromEndMs = 1_800_000, durationMs = 2_400_000))
        // And it can't be placed before the duration is known.
        assertNull(resolveCredits(null, null, showFromEndMs = 90_000, durationMs = 0))
    }

    @Test fun markingNeedsTheSecondHalfAndSomeCredits() {
        assertNotNull(creditsMarkError(null, duration))
        assertNotNull(creditsMarkError(1_000_000, duration)) // first half
        assertNotNull(creditsMarkError(duration - 2_000, duration)) // 2 s before the end
        assertNull(creditsMarkError(2_700_000, duration))
    }
}
