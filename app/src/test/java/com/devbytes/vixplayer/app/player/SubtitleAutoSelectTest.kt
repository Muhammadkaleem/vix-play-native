package com.devbytes.vixplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class SubtitleAutoSelectTest {

    private fun track(id: String, language: String?, playable: Boolean = true, selected: Boolean = false) =
        SubtitleCandidate(id, language, playable, selected)

    private fun pick(vararg tracks: SubtitleCandidate) = pickAutoSubtitle(tracks.toList(), Locale.ENGLISH)

    @Test fun prefersThePhonesLanguage() = assertEquals("2:0", pick(track("1:0", "hi"), track("2:0", "en")))

    @Test fun threeLetterAndRegionCodesMatchToo() {
        assertEquals("2:0", pick(track("1:0", "hi"), track("2:0", "eng")))
        assertEquals("2:0", pick(track("1:0", "hi"), track("2:0", "en-US")))
    }

    @Test fun fallsBackToTheFirstPlayableTrack() = assertEquals("1:0", pick(track("1:0", "hi"), track("2:0", "ur")))

    @Test fun skipsTracksThatCannotPlay() =
        assertEquals("2:0", pick(track("1:0", "en", playable = false), track("2:0", "hi")))

    @Test fun leavesAShowingTrackAlone() = assertNull(pick(track("1:0", "hi", selected = true), track("2:0", "en")))

    @Test fun nothingToShowWithoutPlayableSubtitles() {
        assertNull(pick())
        assertNull(pick(track("1:0", "en", playable = false)))
    }

    @Test fun unlabelledTracksStillCount() = assertEquals("1:0", pick(track("1:0", null)))
}
