package com.devbytes.vixplayer.app.data.series

/** What the up-next order needs to know about a video — no Android types, so it unit-tests on the JVM. */
data class SeriesItem(val id: Long, val name: String, val path: String)

/**
 * What plays after the current video, and what the end-of-video screen offers.
 *
 * [nextId] is the single "next" that Autoplay, Play Next and the options-panel tile all use. It
 * never wraps: finishing the last item never loops back to the first. [upcomingIds] is the row
 * of choices, [nextId] first; for ordinary folders it wraps so the row still offers something
 * after the last file.
 */
data class UpNextPlan(
    val nextId: Long?,
    val upcomingIds: List<Long>,
    /** The current file's episode, when it belongs to a show with at least two episodes. */
    val episode: EpisodeInfo?,
    /** Episode codes ("S02E04") for the row's cards, keyed by id; empty for ordinary folders. */
    val codes: Map<Long, String>,
    /** "S02E03" for an episode, "3 of 12" for a folder position; null when unknown. */
    val positionLabel: String?,
)

/**
 * Decides what comes next. An episode of a show with at least two episodes continues with the
 * show's following episode — across season folders, wherever the files sit. Anything else
 * (including a lone episode) continues through its folder in natural name order, so numbered
 * files play forwards ("Lecture 2" after "Lecture 1", never "Lecture 10").
 */
object UpNextResolver {

    /**
     * [folder] holds the videos beside [current]; [library] all videos, needed only when
     * [current] is an episode (pass an empty list otherwise to skip the library query).
     */
    fun resolve(current: SeriesItem, folder: List<SeriesItem>, library: List<SeriesItem>, limit: Int): UpNextPlan {
        val episode = EpisodeParser.parse(current.name, current.path)
        if (episode != null) {
            val show = showEpisodes(current, episode, library + folder)
            if (show.size >= 2) return episodic(current, episode, show, limit)
        }
        return inFolder(current, folder, limit)
    }

    /**
     * One file per episode of [episode]'s show, in watching order. Specials (season 0) form
     * their own run: a season finale never rolls into a behind-the-scenes extra, and a special
     * only leads to the next special.
     */
    private fun showEpisodes(current: SeriesItem, episode: EpisodeInfo, pool: List<SeriesItem>): List<Pair<SeriesItem, EpisodeInfo>> {
        val currentFolder = folderOf(current.path)
        val special = episode.season == 0
        return (pool + current)
            .distinctBy { it.id }
            .mapNotNull { item ->
                EpisodeParser.parse(item.name, item.path)
                    ?.takeIf { it.showKey == episode.showKey && (it.season == 0) == special }
                    ?.let { item to it }
            }
            .groupBy { (_, info) -> info.season to info.episode }
            .map { (_, copies) ->
                // The same episode can exist twice (another folder, another quality): prefer the
                // file being played, then one beside it, then a stable name order.
                copies.firstOrNull { it.first.id == current.id }
                    ?: copies.firstOrNull { folderOf(it.first.path) == currentFolder }
                    ?: copies.minWith(compareBy(NaturalOrder) { it.first.path })
            }
            .sortedWith(compareBy({ rankOf(it.second.season, it.second.episode) }, { it.second.lastEpisode }))
    }

    private fun episodic(current: SeriesItem, episode: EpisodeInfo, show: List<Pair<SeriesItem, EpisodeInfo>>, limit: Int): UpNextPlan {
        // A double file (E01E02) is followed by E03, not E02.
        val currentRank = rankOf(episode.season, episode.lastEpisode)
        val ahead = show
            .filter { (item, info) -> item.id != current.id && rankOf(info.season, info.episode) > currentRank }
            .take(limit)
        return UpNextPlan(
            nextId = ahead.firstOrNull()?.first?.id,
            upcomingIds = ahead.map { it.first.id },
            episode = episode,
            codes = ahead.associate { (item, info) -> item.id to info.code },
            positionLabel = episode.code,
        )
    }

    private fun inFolder(current: SeriesItem, folder: List<SeriesItem>, limit: Int): UpNextPlan {
        val ordered = (folder + current).distinctBy { it.id }.sortedWith(compareBy(NaturalOrder) { it.name })
        val index = ordered.indexOfFirst { it.id == current.id }
        val ahead = ordered.drop(index + 1)
        return UpNextPlan(
            nextId = ahead.firstOrNull()?.id,
            upcomingIds = (ahead + ordered.take(index)).take(limit).map { it.id },
            episode = null,
            codes = emptyMap(),
            positionLabel = if (ordered.size > 1) "${index + 1} of ${ordered.size}" else null,
        )
    }

    /** Season-then-episode order. */
    private fun rankOf(season: Int, episode: Int): Long = season.toLong() * 100_000L + episode

    private fun folderOf(path: String): String = path.replace('\\', '/').substringBeforeLast('/', "")
}
