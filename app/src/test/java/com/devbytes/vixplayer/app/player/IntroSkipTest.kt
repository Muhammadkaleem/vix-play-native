package com.devbytes.vixplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroSkipTest {

    private val intro = IntroWindow(startMs = 42_000, endMs = 112_000)
    private val episode = IntroWindow(startMs = 180_000, endMs = 250_000)

    @Test fun offersSkipFromTheStartUntilJustBeforeTheEnd() {
        assertFalse(intro.offersSkipAt(41_999))
        assertTrue(intro.offersSkipAt(42_000))
        assertTrue(intro.offersSkipAt(110_999))
        assertFalse(intro.offersSkipAt(111_000))
        assertFalse(intro.offersSkipAt(200_000))
    }

    @Test fun anEpisodesOwnIntroWinsOverTheShows() =
        assertEquals(episode to IntroSource.EPISODE, resolveIntro(episode, intro, durationMs = 3_000_000))

    @Test fun otherwiseTheShowsIntroApplies() =
        assertEquals(intro to IntroSource.SHOW, resolveIntro(null, intro, durationMs = 3_000_000))

    @Test fun aDetectedIntroBeatsTheShowsMarkButNotTheEpisodes() {
        val detected = IntroWindow(startMs = 95_000, endMs = 150_000)
        assertEquals(detected to IntroSource.DETECTED, resolveIntro(null, intro, 3_000_000, detected))
        assertEquals(episode to IntroSource.EPISODE, resolveIntro(episode, intro, 3_000_000, detected))
    }

    @Test fun noIntroMarkedMeansNone() = assertNull(resolveIntro(null, null, durationMs = 3_000_000))

    @Test fun anIntroPastTheEpisodesEndIsDropped() =
        // A short special in a show whose intro was marked at 0:42–1:52.
        assertNull(resolveIntro(null, intro, durationMs = 100_000))

    @Test fun anUnknownDurationDoesNotDropTheIntro() =
        assertEquals(intro to IntroSource.SHOW, resolveIntro(null, intro, durationMs = 0))

    @Test fun marksMustBeCompleteAndInOrder() {
        assertEquals("Set where the intro starts", introMarkError(null, null, 3_000_000))
        assertEquals("Set where the intro ends", introMarkError(42_000, null, 3_000_000))
        assertEquals("The end must come after the start", introMarkError(112_000, 42_000, 3_000_000))
        assertNull(introMarkError(42_000, 112_000, 3_000_000))
    }

    @Test fun marksMustLookLikeAnIntro() {
        assertEquals("An intro is at least 5 seconds", introMarkError(42_000, 45_000, 3_000_000))
        assertEquals("An intro is at most 10 minutes", introMarkError(0, 11 * 60_000, 3_000_000))
        assertEquals("The end is past the episode's end", introMarkError(42_000, 112_000, 100_000))
    }

    @Test fun timesAndLabelsReadLikeTheSeekBar() {
        assertEquals("0:42", formatIntroTime(42_500))
        assertEquals("1:05:09", formatIntroTime(3_909_000))
        assertEquals("0:42–1:52", intro.label())
    }
}
