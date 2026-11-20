package com.devbytes.vixplayer.app.data.series

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeParserTest {

    private val movies = "/storage/emulated/0/Movies"

    private fun parse(name: String, folder: String = "$movies/Somewhere") = EpisodeParser.parse(name, "$folder/$name")

    private fun assertEpisode(name: String, title: String, season: Int, episode: Int, folder: String = "$movies/Somewhere") {
        val info = parse(name, folder) ?: throw AssertionError("not recognised: $name")
        assertEquals(title, info.showTitle)
        assertEquals(season, info.season)
        assertEquals(episode, info.episode)
    }

    // ── Recognised ────────────────────────────────────────────────────────────

    @Test fun sceneReleaseName() = assertEpisode(
        "Peaky.Blinders.S01E01.1080p.BluRay.x265-RARBG.mp4", "Peaky Blinders", 1, 1,
        folder = "$movies/Peaky.Blinders.S01.1080p.BluRay.x265-RARBG",
    )

    @Test fun episodeTitleAndAudioTagsAfterTheCode() = assertEpisode(
        "House.of.the.Dragon.S01E02.Rhaenyra.the.Realms.Delight.1080p.HMAX.WEB-DL.DDP5.1.Atmos.H.264-CMRG.mkv",
        "House of the Dragon", 1, 2,
    )

    @Test fun languageAndCodecTags() =
        assertEpisode("Paatal.Lok.S02E03.HINDI.720p.10bit.WEBRip.2CH.x265.HEVC-PSA.mkv", "Paatal Lok", 2, 3)

    @Test fun seasonXEpisode() = assertEpisode("the office 4x07 dinner party.avi", "the office", 4, 7)

    @Test fun seasonAndEpisodeInWords() = assertEpisode("Breaking Bad Season 2 Episode 10.mp4", "Breaking Bad", 2, 10)

    @Test fun episodeOnlyTakesSeasonAndTitleFromTheFolder() = assertEpisode(
        "Episode 3.mp4", "Paatal Lok", 1, 3,
        folder = "$movies/Paatal Lok (2020) Hindi S01 Complete 720p AMZN WEBRip",
    )

    @Test fun seasonFolderFallsBackToTheShowFolder() =
        assertEpisode("EP05.mkv", "Dark", 2, 5, folder = "$movies/Dark/Season 2")

    @Test fun fansubTagIsDropped() = assertEpisode("[SubsPlease] Frieren S01E05 (1080p).mkv", "Frieren", 1, 5)

    @Test fun sitePrefixAndYearAreDropped() =
        assertEpisode("www.1TamilMV.wf - Kota Factory (2024) S03E01 Tamil.mkv", "Kota Factory", 3, 1)

    @Test fun doubleEpisodeFile() {
        val info = parse("Show.S01E01E02.mkv")!!
        assertEquals(1, info.episode)
        assertEquals(2, info.lastEpisode)
    }

    @Test fun specialsAreSeasonZero() = assertEquals(0, parse("Show.S00E01.Pilot.Extended.mkv")!!.season)

    @Test fun differentlyFormattedNamesShareAShow() =
        assertEquals(parse("Peaky.Blinders.S01E01.mkv")!!.showKey, parse("Peaky Blinders - S02E01.mp4")!!.showKey)

    @Test fun codeIsZeroPadded() = assertEquals("S02E04", parse("Show S2E4.mp4")!!.code)

    // ── Not episodes ──────────────────────────────────────────────────────────

    @Test fun ordinaryVideosAreNotEpisodes() {
        listOf(
            "20251025_121631.mp4",
            "VID-20261116-WA0001.mp4",
            "Screen_Recording_20231004-112233_Chrome.mp4",
            "Interstellar.2014.1080p.BluRay.x264.DTS-HD.MA.5.1-SWTYBLZ.mkv",
            "Spider-Man.No.Way.Home.2021.2160p.WEB-DL.DDP5.1.Atmos.mkv",
            "Holiday 1920x1080.mp4",
            "Lecture 01 - Introduction.mp4",
            "Star Wars Episode IV A New Hope.mp4",
            "[SubsPlease] Jujutsu Kaisen - 01 (1080p).mkv",
        ).forEach { assertNull(it, parse(it)) }
    }

    @Test fun anEpisodeWithNoShowNameInAGenericFolderIsNotGuessed() =
        assertNull(parse("S01E01.mkv", folder = "/storage/emulated/0/Download"))
}
