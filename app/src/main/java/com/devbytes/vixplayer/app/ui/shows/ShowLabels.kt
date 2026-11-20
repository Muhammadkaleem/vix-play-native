package com.devbytes.vixplayer.app.ui.shows

import com.devbytes.vixplayer.app.data.series.ContinueKind
import com.devbytes.vixplayer.app.data.series.Show
import com.devbytes.vixplayer.app.data.series.ShowEpisode

/** The show's main action: "Resume S02E05 · 12m left", "Play S02E06", "Start S01E01", "Watch again". */
fun continueLabel(show: Show): String {
    val episode = show.continueEpisode
    // A part-watched episode always reads "Resume", even when it's reached as the next unwatched one.
    if (episode.inProgress && show.continueKind != ContinueKind.REWATCH) {
        return "Resume ${episode.info.code} · ${timeLeft(episode)} left"
    }
    return when (show.continueKind) {
        ContinueKind.START -> "Start ${episode.info.code}"
        ContinueKind.RESUME -> "Resume ${episode.info.code}"
        ContinueKind.NEXT -> "Play ${episode.info.code}"
        ContinueKind.REWATCH -> "Watch again"
    }
}

/** "2 seasons · 16 episodes · 5 watched". */
fun showSummary(show: Show): String = buildList {
    val regularSeasons = show.seasons.count { it != 0 }
    add(if (regularSeasons == 1) "1 season" else "$regularSeasons seasons")
    add(if (show.episodes.size == 1) "1 episode" else "${show.episodes.size} episodes")
    if (show.watchedCount > 0) add("${show.watchedCount} watched")
}.joinToString(" · ")

/** "Season 2", or "Specials" for season 0. */
fun seasonLabel(season: Int): String = if (season == 0) "Specials" else "Season $season"

/** "E05 · Rhaenyra the Realms Delight", or "Episode 5" when the name carries no title. */
fun episodeLabel(episode: ShowEpisode): String {
    val number = String.format(java.util.Locale.ROOT, "E%02d", episode.info.episode)
    return episode.info.episodeTitle?.let { "$number · $it" } ?: "Episode ${episode.info.episode}"
}

/** "12m", "1h 05m", "<1m" — what remains of a part-watched episode. */
fun timeLeft(episode: ShowEpisode): String = minutes(episode.file.durationMs - episode.file.positionMs)

/** "48m", "1h 05m" — an episode's running time. */
fun runtime(episode: ShowEpisode): String = minutes(episode.file.durationMs)

private fun minutes(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 60_000L).toInt()
    return when {
        total < 1 -> "<1m"
        total < 60 -> "${total}m"
        else -> String.format(java.util.Locale.ROOT, "%dh %02dm", total / 60, total % 60)
    }
}
