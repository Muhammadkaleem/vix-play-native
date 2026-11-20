package com.devbytes.vixplayer.app.data.series

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowIndexTest {

    private val root = "/storage/emulated/0/Movies"
    private val minute = 60_000L
    private var nextId = 1L

    private fun file(path: String, watchedMs: Long = 0, lastPlayedAt: Long = 0, durationMs: Long = 50 * minute) =
        EpisodeFile(nextId++, path.substringAfterLast('/'), path, durationMs, watchedMs, lastPlayedAt)

    private fun season(show: String, season: Int, count: Int) =
        (1..count).map { file(String.format("%s/%s S%02d/%s.S%02dE%02d.mkv", root, show, season, show, season, it)) }

    private fun only(files: List<EpisodeFile>) = ShowIndex.build(files).single()

    @Test fun groupsSeasonsFromSeparateFoldersIntoOneShow() {
        val show = only(season("Dark", 1, 3) + season("Dark", 2, 2))
        assertEquals("Dark", show.title)
        assertEquals(listOf(1, 2), show.seasons)
        assertEquals(5, show.episodes.size)
    }

    @Test fun aLoneEpisodeIsNotAShow() =
        assertTrue(ShowIndex.build(listOf(file("$root/Download/Dark.S01E04.mkv"))).isEmpty())

    @Test fun nothingWatchedStartsAtTheFirstEpisode() {
        val show = only(season("Dark", 2, 2) + season("Dark", 1, 2))
        assertEquals(ContinueKind.START, show.continueKind)
        assertEquals("S01E01", show.continueEpisode.info.code)
    }

    @Test fun aHalfWatchedEpisodeResumes() {
        val files = season("Dark", 1, 3).toMutableList()
        files[1] = files[1].copy(positionMs = 20 * minute, lastPlayedAt = 100)
        val show = only(files)
        assertEquals(ContinueKind.RESUME, show.continueKind)
        assertEquals("S01E02", show.continueEpisode.info.code)
    }

    @Test fun aFinishedEpisodeMovesOnAcrossSeasons() {
        val files = (season("Dark", 1, 2) + season("Dark", 2, 2)).toMutableList()
        files[1] = files[1].copy(positionMs = 50 * minute, lastPlayedAt = 100)
        val show = only(files)
        assertEquals(ContinueKind.NEXT, show.continueKind)
        assertEquals("S02E01", show.continueEpisode.info.code)
        assertEquals(1, show.watchedCount)
    }

    @Test fun everythingWatchedStartsOver() {
        val show = only(season("Dark", 1, 2).map { it.copy(positionMs = it.durationMs, lastPlayedAt = it.id) })
        assertEquals(ContinueKind.REWATCH, show.continueKind)
        assertEquals("S01E01", show.continueEpisode.info.code)
    }

    @Test fun specialsComeAfterTheRegularSeasons() =
        assertEquals(listOf(1, 0), only(season("Dark", 1, 2) + file("$root/Dark/Dark.S00E01.mkv")).seasons)

    @Test fun theWatchedCopyOfADuplicateEpisodeWins() {
        val unwatched = file("$root/A/Dark.S01E01.mkv")
        val watched = file("$root/B/Dark.S01E01.720p.mkv", watchedMs = 50 * minute, lastPlayedAt = 100)
        val show = only(listOf(unwatched, watched, file("$root/A/Dark.S01E02.mkv")))
        assertEquals(watched.id, show.episodes.first().file.id)
    }

    @Test fun recentlyWatchedShowsComeFirst() {
        val dark = season("Dark", 1, 2)
        val alpha = season("Alpha", 1, 2).mapIndexed { i, f -> if (i == 0) f.copy(lastPlayedAt = 0) else f }
        val betaWatched = season("Beta", 1, 2).mapIndexed { i, f -> if (i == 0) f.copy(positionMs = 10 * minute, lastPlayedAt = 500) else f }
        assertEquals(listOf("Beta", "Alpha", "Dark"), ShowIndex.build(dark + alpha + betaWatched).map { it.title })
    }

    @Test fun episodeTitlesComeFromTheName() {
        val info = EpisodeParser.parse("House.of.the.Dragon.S01E02.Rhaenyra.the.Realms.Delight.1080p.HMAX.WEB-DL.DDP5.1.mkv")!!
        assertEquals("Rhaenyra the Realms Delight", info.episodeTitle)
        assertEquals(null, EpisodeParser.parse("Paatal.Lok.S02E03.HINDI.720p.10bit.WEBRip.2CH.x265.HEVC-PSA.mkv")!!.episodeTitle)
    }
}
