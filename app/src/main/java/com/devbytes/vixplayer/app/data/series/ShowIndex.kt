package com.devbytes.vixplayer.app.data.series

/** A video with what the show view needs — plain data, so grouping unit-tests on the JVM. */
data class EpisodeFile(
    val id: Long,
    val name: String,
    val path: String,
    val durationMs: Long,
    /** Resume position (the player's own fast store). */
    val positionMs: Long,
    /** When it was last played; 0 if never. */
    val lastPlayedAt: Long,
)

data class ShowEpisode(val file: EpisodeFile, val info: EpisodeInfo) {
    val progress: Float
        get() = if (file.durationMs > 0) (file.positionMs.toFloat() / file.durationMs).coerceIn(0f, 1f) else 0f
    val watched: Boolean get() = progress >= ShowIndex.WATCHED
    val inProgress: Boolean get() = !watched && progress >= ShowIndex.STARTED
}

/** What the show's main button does. */
enum class ContinueKind {
    /** Nothing played yet — start at the first episode. */
    START,
    /** The last-played episode is part-way through. */
    RESUME,
    /** The last-played episode is finished — on to the next unwatched one. */
    NEXT,
    /** Every episode is watched — from the top. */
    REWATCH,
}

data class Show(
    val key: String,
    val title: String,
    /** Regular seasons in order, then specials (season 0). One file per episode. */
    val episodes: List<ShowEpisode>,
    val continueEpisode: ShowEpisode,
    val continueKind: ContinueKind,
) {
    /** Season numbers in display order (specials, 0, last). */
    val seasons: List<Int> get() = episodes.map { it.info.season }.distinct()
    val lastPlayedAt: Long get() = episodes.maxOf { it.file.lastPlayedAt }
    val watchedCount: Int get() = episodes.count { it.watched }
}

/**
 * Groups a library into shows: files [EpisodeParser] recognises, one per episode, for shows
 * with at least two regular episodes — the same bar [UpNextResolver] uses, so the shelf and Up
 * Next agree on what counts as a show.
 */
object ShowIndex {

    /** Matches the player's "finished" rule and History's "Watched" label. */
    const val WATCHED = 0.95f

    /** Below this a file was opened rather than watched. */
    const val STARTED = 0.02f

    /** Shows most recently watched first, then the rest by title. */
    fun build(files: List<EpisodeFile>): List<Show> =
        files
            .mapNotNull { file -> EpisodeParser.parse(file.name, file.path)?.let { ShowEpisode(file, it) } }
            .groupBy { it.info.showKey }
            .mapNotNull { (key, episodes) -> showOf(key, episodes) }
            .sortedWith(compareByDescending<Show> { it.lastPlayedAt }.thenBy(NaturalOrder) { it.title })

    private fun showOf(key: String, all: List<ShowEpisode>): Show? {
        val episodes = all
            .groupBy { it.info.season to it.info.episode }
            .map { (_, copies) ->
                // The same episode twice (another folder or quality): keep the one that was
                // watched most recently, else a stable name order.
                copies.maxByOrNull { it.file.lastPlayedAt }?.takeIf { it.file.lastPlayedAt > 0 }
                    ?: copies.minWith(compareBy(NaturalOrder) { it.file.path })
            }
            .sortedWith(compareBy({ it.info.season == 0 }, { it.info.season }, { it.info.episode }))
        val regular = episodes.filter { it.info.season != 0 }
        if (regular.size < 2) return null
        val (episode, kind) = continueFrom(episodes, regular)
        return Show(key, titleOf(episodes), episodes, episode, kind)
    }

    private fun continueFrom(episodes: List<ShowEpisode>, regular: List<ShowEpisode>): Pair<ShowEpisode, ContinueKind> {
        val last = episodes.filter { it.file.lastPlayedAt > 0 }.maxByOrNull { it.file.lastPlayedAt }
            ?: return regular.first() to ContinueKind.START
        if (!last.watched) return last to ContinueKind.RESUME
        val after = episodes.drop(episodes.indexOf(last) + 1).firstOrNull { !it.watched && it.info.season != 0 }
        val next = after ?: regular.firstOrNull { !it.watched }
        return if (next != null) next to ContinueKind.NEXT else regular.first() to ContinueKind.REWATCH
    }

    /** The spelling most of the files use ("Peaky Blinders" over a stray "peaky blinders"). */
    private fun titleOf(episodes: List<ShowEpisode>): String =
        episodes.groupingBy { it.info.showTitle }.eachCount().maxWith(compareBy({ it.value }, { it.key })).key
}
