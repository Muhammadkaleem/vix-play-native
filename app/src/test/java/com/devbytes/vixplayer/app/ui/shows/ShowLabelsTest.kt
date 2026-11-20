package com.devbytes.vixplayer.app.ui.shows

import com.devbytes.vixplayer.app.data.series.EpisodeFile
import com.devbytes.vixplayer.app.data.series.ShowEpisode
import com.devbytes.vixplayer.app.data.series.ShowIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShowLabelsTest {

    private val minute = 60_000L

    /** One season of Dark, each episode [durations] minutes long, watched up to [positions] minutes. */
    private fun season(durations: List<Int>, positions: List<Int> = durations.map { 0 }): List<ShowEpisode> {
        val files = durations.mapIndexed { i, d ->
            val path = String.format("/storage/emulated/0/Movies/Dark/Dark.S01E%02d.mkv", i + 1)
            EpisodeFile(i + 1L, path.substringAfterLast('/'), path, d * minute, positions[i] * minute, 0)
        }
        return ShowIndex.build(files).single().episodes
    }

    @Test fun summaryCountsEpisodesAndRunningTime() =
        assertEquals("3 episodes · 2h 30m", seasonSummary(season(listOf(50, 50, 50))))

    @Test fun summaryAddsWatchedCount() =
        assertEquals("3 episodes · 2h 30m · 1 watched", seasonSummary(season(listOf(50, 50, 50), listOf(50, 10, 0))))

    @Test fun summarySaysWhenTheWholeSeasonIsWatched() =
        assertEquals("2 episodes · 1h 40m · All watched", seasonSummary(season(listOf(50, 50), listOf(50, 50))))

    @Test fun remainingCountsWhatIsLeftOfStartedAndUnwatchedEpisodes() =
        // E01 watched, E02 has 40 m left, E03 is untouched: 90 m.
        assertEquals("1h 30m left in this season", seasonRemaining(season(listOf(50, 50, 50), listOf(50, 10, 0))))

    @Test fun noRemainingLineBeforeStartingOrAfterFinishing() {
        assertNull(seasonRemaining(season(listOf(50, 50))))
        assertNull(seasonRemaining(season(listOf(50, 50), listOf(50, 50))))
    }
}
