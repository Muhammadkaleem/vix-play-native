package com.devbytes.vixplayer.app.data.series

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpNextResolverTest {

    private val root = "/storage/emulated/0/Movies"
    private var nextId = 1L

    private fun item(path: String) = SeriesItem(nextId++, path.substringAfterLast('/'), path)

    // Season folders, listed newest-first as MediaStore returns them.
    private val s1e3 = item("$root/Show S01/Show.S01E03.mkv")
    private val s1e2 = item("$root/Show S01/Show.S01E02.mkv")
    private val s1e1 = item("$root/Show S01/Show.S01E01.mkv")
    private val season1 = listOf(s1e3, s1e2, s1e1)
    private val s2e1 = item("$root/Show S02/Show.S02E01.mkv")
    private val season2 = listOf(s2e1)
    private val library = season1 + season2

    private fun resolve(current: SeriesItem, folder: List<SeriesItem>, all: List<SeriesItem> = library) =
        UpNextResolver.resolve(current, folder, all, limit = 8)

    @Test fun aSeasonPlaysForwardWhateverTheFolderOrder() {
        val plan = resolve(s1e1, season1)
        assertEquals(s1e2.id, plan.nextId)
        assertEquals(listOf(s1e2.id, s1e3.id, s2e1.id), plan.upcomingIds)
        assertEquals("S01E01", plan.positionLabel)
        assertEquals("S01E02", plan.codes[s1e2.id])
        assertEquals("Show", plan.episode?.showTitle)
    }

    @Test fun theLastEpisodeOfASeasonContinuesInTheNextSeasonFolder() =
        assertEquals(s2e1.id, resolve(s1e3, season1).nextId)

    @Test fun theFinaleHasNoNextAndNeverWrapsToEpisodeOne() {
        val plan = resolve(s2e1, season2)
        assertNull(plan.nextId)
        assertEquals(emptyList<Long>(), plan.upcomingIds)
        assertEquals("Show", plan.episode?.showTitle)
    }

    @Test fun aDoubleEpisodeFileIsFollowedByTheEpisodeAfterIt() {
        val double = item("$root/Twin/Twin.S01E01E02.mkv")
        val third = item("$root/Twin/Twin.S01E03.mkv")
        val folder = listOf(third, double)
        assertEquals(third.id, resolve(double, folder, folder).nextId)
    }

    @Test fun aCopyBesideTheCurrentFileWinsOverOneElsewhere() {
        val here1 = item("$root/A/Show.S01E01.mkv")
        val here2 = item("$root/A/Show.S01E02.mkv")
        val elsewhere2 = item("$root/B/Show.S01E02.720p.mkv")
        assertEquals(here2.id, resolve(here1, listOf(here1, here2), listOf(elsewhere2, here1, here2)).nextId)
    }

    @Test fun aSeasonFinaleDoesNotRollIntoSpecials() {
        val special = item("$root/Show Specials/Show.S00E01.mkv")
        assertNull(resolve(s2e1, season2, library + special).nextId)
    }

    @Test fun aLoneEpisodeFollowsItsFolderInstead() {
        val movieA = item("$root/Download/Movie A.mp4")
        val movieB = item("$root/Download/Movie B.mp4")
        val lone = item("$root/Download/Other.Show.S01E05.mp4")
        val folder = listOf(lone, movieB, movieA)
        val plan = resolve(movieA, folder, folder)
        assertEquals(movieB.id, plan.nextId)
        assertNull(resolve(lone, folder, folder).episode)
    }

    @Test fun numberedFilesPlayInNaturalOrder() {
        val l10 = item("$root/Course/Lecture 10.mp4")
        val l2 = item("$root/Course/Lecture 2.mp4")
        val l1 = item("$root/Course/Lecture 1.mp4")
        val folder = listOf(l10, l2, l1)
        val plan = resolve(l1, folder, emptyList())
        assertEquals(l2.id, plan.nextId)
        assertEquals("1 of 3", plan.positionLabel)
    }

    @Test fun theLastFileInAFolderHasNoNextButTheRowWraps() {
        val a = item("$root/Clips/a.mp4")
        val b = item("$root/Clips/b.mp4")
        val plan = resolve(b, listOf(a, b), emptyList())
        assertNull(plan.nextId)
        assertEquals(listOf(a.id), plan.upcomingIds)
    }
}
