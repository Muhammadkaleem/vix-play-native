package com.devbytes.vixplayer.app.ui.shows

import android.text.format.DateUtils
import com.devbytes.vixplayer.app.data.repository.VideoFile
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

/** "6 episodes · 5h 12m · 2 watched" — one season at a glance. */
fun seasonSummary(episodes: List<ShowEpisode>): String = buildList {
    add(if (episodes.size == 1) "1 episode" else "${episodes.size} episodes")
    episodes.sumOf { it.file.durationMs }.takeIf { it > 0 }?.let { add(minutes(it)) }
    val watched = episodes.count { it.watched }
    if (watched > 0) add(if (watched == episodes.size) "All watched" else "$watched watched")
}.joinToString(" · ")

/** "2h 05m left in this season", or null once it's all watched or nothing's started. */
fun seasonRemaining(episodes: List<ShowEpisode>): String? {
    if (episodes.none { it.watched || it.inProgress } || episodes.all { it.watched }) return null
    val left = episodes.filterNot { it.watched }.sumOf { (it.file.durationMs - it.file.positionMs).coerceAtLeast(0) }
    return "${minutes(left)} left in this season"
}

/** "1080p · 1.2 GB · MKV" from the episode's file; blank parts are skipped. */
fun fileDetails(video: VideoFile?): String {
    if (video == null) return ""
    val shortSide = minOf(video.width, video.height)
    val quality = when {
        shortSide >= 2160 -> "4K"
        shortSide >= 1080 -> "1080p"
        shortSide >= 720 -> "720p"
        shortSide > 0 -> "${shortSide}p"
        else -> null
    }
    val size = video.sizeBytes.takeIf { it > 0 }?.let { bytes ->
        val mb = bytes / (1024.0 * 1024.0)
        if (mb >= 1024) String.format(java.util.Locale.ROOT, "%.1f GB", mb / 1024) else "${mb.toInt()} MB"
    }
    val ext = video.name.substringAfterLast('.', "").uppercase().takeIf { it.length in 2..4 }
    return listOfNotNull(quality, size, ext).joinToString(" · ")
}

/** "Watched 2 days ago" / "Played 3 hours ago"; null if never played. */
fun lastPlayedLabel(episode: ShowEpisode, now: Long = System.currentTimeMillis()): String? {
    val at = episode.file.lastPlayedAt.takeIf { it > 0 } ?: return null
    val ago = if (now - at < DateUtils.MINUTE_IN_MILLIS) "just now"
    else DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString().lowercase()
    return if (episode.watched) "Watched $ago" else "Played $ago"
}

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
