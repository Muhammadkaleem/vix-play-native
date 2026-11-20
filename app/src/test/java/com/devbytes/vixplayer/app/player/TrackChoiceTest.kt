package com.devbytes.vixplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class TrackChoiceTest {

    private val english = Locale.ENGLISH

    private fun track(id: String, language: String?, playable: Boolean = true, selected: Boolean = false) =
        TrackCandidate(id, language, playable, selected)

    private fun pick(vararg tracks: TrackCandidate) = pickAutoSubtitle(tracks.toList(), english)

    // ── Turn on subtitles automatically ───────────────────────────────────────

    @Test fun autoPrefersThePhonesLanguage() = assertEquals("2:0", pick(track("1:0", "hi"), track("2:0", "en")))

    @Test fun threeLetterAndRegionCodesMatchToo() {
        assertEquals("2:0", pick(track("1:0", "hi"), track("2:0", "eng")))
        assertEquals("2:0", pick(track("1:0", "hi"), track("2:0", "en-US")))
    }

    @Test fun autoFallsBackToTheFirstPlayableTrack() = assertEquals("1:0", pick(track("1:0", "hi"), track("2:0", "ur")))

    @Test fun autoSkipsTracksThatCannotPlay() =
        assertEquals("2:0", pick(track("1:0", "en", playable = false), track("2:0", "hi")))

    @Test fun autoLeavesAShowingTrackAlone() = assertNull(pick(track("1:0", "hi", selected = true), track("2:0", "en")))

    @Test fun autoHasNothingToShowWithoutPlayableSubtitles() {
        assertNull(pick())
        assertNull(pick(track("1:0", "en", playable = false)))
    }

    @Test fun unlabelledTracksStillCount() = assertEquals("1:0", pick(track("1:0", null)))

    // ── A show's remembered choice ────────────────────────────────────────────

    private val audio = listOf(track("0:0", "en", selected = true), track("1:0", "hi"))
    private val text = listOf(track("2:0", "en"), track("3:0", "hi"))

    private fun plan(remembered: RememberedTracks?, auto: Boolean = false, audioTracks: List<TrackCandidate> = audio, textTracks: List<TrackCandidate> = text) =
        planTracks(audioTracks, textTracks, remembered, auto, english)

    @Test fun rememberedAudioLanguageIsSelected() =
        assertEquals("1:0", plan(RememberedTracks("hin", null)).audioId)

    @Test fun rememberedAudioAlreadyPlayingChangesNothing() =
        assertNull(plan(RememberedTracks("en", null)).audioId)

    @Test fun rememberedAudioMissingFromThisEpisodeChangesNothing() =
        assertNull(plan(RememberedTracks("ta", null)).audioId)

    @Test fun rememberedSubtitleLanguageIsSelected() =
        assertEquals("3:0", plan(RememberedTracks(null, SubtitleChoice.On("hi"))).subtitleId)

    @Test fun rememberedSubtitlesOffBeatsAutoSubtitles() {
        val showing = listOf(track("2:0", "en", selected = true))
        val result = plan(RememberedTracks(null, SubtitleChoice.Off), auto = true, textTracks = showing)
        assertEquals(true, result.subtitlesOff)
        assertNull(result.subtitleId)
    }

    @Test fun rememberedOnWithAMissingLanguageFallsBackToAnyTrack() =
        assertEquals("2:0", plan(RememberedTracks(null, SubtitleChoice.On("ta"))).subtitleId)

    @Test fun withoutAMemoryTheAutoSettingDecides() {
        assertEquals("2:0", plan(null, auto = true).subtitleId)
        assertNull(plan(null, auto = false).subtitleId)
    }
}
