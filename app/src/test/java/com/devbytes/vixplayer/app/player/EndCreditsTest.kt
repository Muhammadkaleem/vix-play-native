package com.devbytes.vixplayer.app.player

import org.junit.Assert.assertFalse
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
}
