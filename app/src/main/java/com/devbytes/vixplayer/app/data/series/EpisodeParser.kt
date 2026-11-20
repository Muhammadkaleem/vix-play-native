package com.devbytes.vixplayer.app.data.series

import java.util.Locale

/** One file recognised as an episode of a show. */
data class EpisodeInfo(
    /** Normalised identity that groups a show's files across folders ("peaky blinders"). */
    val showKey: String,
    /** Display form of the show's name ("Peaky Blinders"). */
    val showTitle: String,
    /** 0 for specials; 1 when only an episode number is known and no folder names a season. */
    val season: Int,
    val episode: Int,
    /** Last episode a double file covers (S01E01E02 → 2); equals [episode] otherwise. */
    val lastEpisode: Int = episode,
) {
    /** "S02E04" — the label people know episodes by. */
    val code: String get() = String.format(Locale.ROOT, "S%02dE%02d", season, episode)
}

/**
 * Recognises TV episodes from file names — `Show.S01E02`, `Show 1x02`,
 * `Show Season 1 Episode 2`, and `Show - Episode 2` / `Ep 02` / `E02` (the season then comes
 * from a "Season 2" or "S02" folder) — and names the show each belongs to, so a season plays in
 * order and continues across season folders.
 *
 * Deliberately conservative: a name that doesn't clearly mark an episode returns null and is
 * treated as an ordinary video, because a false "episode" would hijack Up Next. Absolute
 * anime-style numbering ("Show - 105") is left to natural name order for the same reason.
 */
object EpisodeParser {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "3g2", "ts", "m2ts", "mts", "wmv",
        "flv", "mpg", "mpeg", "vob", "ogv", "rmvb", "divx", "asf", "f4v",
    )

    private val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)

    /** S01E02, s1e2, S01.E02, S01 - E02 — plus a second episode for double files (S01E01E02). */
    private val SEASON_EPISODE = Regex(
        """(?<![a-z0-9])s(\d{1,2})[ ._-]*e(\d{1,4})(?:[ ._-]*e(\d{1,4}))?(?![0-9])""",
        IGNORE_CASE,
    )

    /** 1x02 — the guards keep resolutions like 1920x1080 from matching. */
    private val SEASON_X_EPISODE = Regex("""(?<![a-z0-9])(\d{1,2})x(\d{2,3})(?![0-9])""", IGNORE_CASE)

    /** "Season 1 Episode 2", "Season.2.Ep.10". */
    private val SEASON_EPISODE_WORDS = Regex(
        """(?<![a-z])season[ ._-]*(\d{1,2})[ ._-]*(?:episode|ep)[ ._-]*(\d{1,4})(?![0-9])""",
        IGNORE_CASE,
    )

    /** "Episode 2", "Ep 02", "EP02", "E02" — no season in the name. */
    private val EPISODE_ONLY = Regex(
        """(?<![a-z0-9])(?:episode|ep)[ ._-]*(\d{1,4})(?![0-9])|(?<![a-z0-9])e(\d{1,4})(?![0-9])""",
        IGNORE_CASE,
    )

    /** "Season 2" or a standalone "S02" in a folder name. */
    private val FOLDER_SEASON = Regex(
        """(?<![a-z])season[ ._-]*(\d{1,2})(?![0-9])|(?<![a-z0-9])s(\d{1,2})(?![0-9a-z])""",
        IGNORE_CASE,
    )

    /** Where a folder name stops being a title: season, year, quality, source or language tags. */
    private val FOLDER_NOISE = Regex(
        """(?<![a-z0-9])(?:season\s*\d{1,2}|s\d{1,2}|\(?(?:19|20)\d{2}\)?|\d{3,4}p|4k|uhd|hdr|""" +
            """x26[45]|h ?26[45]|hevc|web(?:rip|-?dl)?|blu-?ray|brrip|bdrip|hdrip|hdtv|dvdrip|""" +
            """complete|dual|multi|hindi|english|urdu|tamil|telugu|esubs?|amzn|nf|hmax|dsnp)(?![a-z0-9])""",
        IGNORE_CASE,
    )

    private val LEADING_TAGS = Regex("""^(?:\s*\[[^\]]*])+""")
    private val BRACKET_TAGS = Regex("""\[[^\]]*]""")
    private val SITE_PREFIX = Regex("""^\s*www\s+\S+(?:\s+[a-z]{2,6})?\s*-\s*""", IGNORE_CASE)
    private val TRAILING_YEAR = Regex("""\s*\(?(?:19|20)\d{2}\)?\s*$""")
    private val SPACES = Regex("""\s+""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")

    /** Folder names that describe storage, not a show — never used as a show's title. */
    private val GENERIC_FOLDERS = setOf(
        "movies", "movie", "films", "videos", "video", "download", "downloads", "dcim", "camera",
        "whatsapp video", "whatsapp videos", "telegram", "telegram video", "screen recordings",
        "screenrecorder", "pictures", "media", "tv", "tv shows", "shows", "series", "0",
    )

    /**
     * The episode [fileName] names, or null when it isn't clearly an episode. [path] is the
     * file's full path; its folders supply the season or show name when the file name lacks them.
     */
    fun parse(fileName: String, path: String? = null): EpisodeInfo? {
        val name = normalize(stripExtension(fileName))
        val folders = parentFolders(path)

        SEASON_EPISODE.find(name)?.let { match ->
            val episode = match.groupValues[2].toInt()
            val second = match.groupValues[3].toIntOrNull()
            val last = if (second != null && second in episode + 1..episode + 3) second else episode
            return episodeOf(name.substring(0, match.range.first), folders, match.groupValues[1].toInt(), episode, last)
        }
        SEASON_X_EPISODE.find(name)?.let { match ->
            return episodeOf(
                name.substring(0, match.range.first), folders,
                match.groupValues[1].toInt(), match.groupValues[2].toInt(),
            )
        }
        SEASON_EPISODE_WORDS.find(name)?.let { match ->
            return episodeOf(
                name.substring(0, match.range.first), folders,
                match.groupValues[1].toInt(), match.groupValues[2].toInt(),
            )
        }
        EPISODE_ONLY.find(name)?.let { match ->
            val episode = match.groupValues[1].ifEmpty { match.groupValues[2] }.toInt()
            val season = folders.firstNotNullOfOrNull(::seasonOf) ?: 1
            return episodeOf(name.substring(0, match.range.first), folders, season, episode)
        }
        return null
    }

    private fun episodeOf(prefix: String, folders: List<String>, season: Int, episode: Int, last: Int = episode): EpisodeInfo? {
        val title = cleanTitle(prefix)?.takeUnless { keyOf(it) in GENERIC_FOLDERS }
            ?: folders.firstNotNullOfOrNull(::titleFromFolder)
            ?: return null
        val key = keyOf(title)
        if (key.isEmpty()) return null
        return EpisodeInfo(key, title, season, episode, last)
    }

    private fun seasonOf(folder: String): Int? =
        FOLDER_SEASON.find(normalize(folder))?.let { it.groupValues[1].ifEmpty { it.groupValues[2] }.toInt() }

    /** "Paatal Lok (2020) Hindi S01 Complete 720p" → "Paatal Lok"; "Season 1" → null. */
    private fun titleFromFolder(folder: String): String? {
        val name = normalize(folder)
        val end = FOLDER_NOISE.find(name)?.range?.first ?: name.length
        return cleanTitle(name.substring(0, end))?.takeUnless { keyOf(it) in GENERIC_FOLDERS }
    }

    private fun cleanTitle(raw: String): String? {
        var title = raw.replace(LEADING_TAGS, "").replace(SITE_PREFIX, "").replace(BRACKET_TAGS, " ")
        val withoutYear = title.replace(TRAILING_YEAR, "")
        if (withoutYear.any(Char::isLetter)) title = withoutYear
        title = title.replace(SPACES, " ").trim { it.isWhitespace() || it in "-–—_.:|," }
        return title.takeIf { it.any(Char::isLetterOrDigit) }
    }

    private fun keyOf(title: String): String =
        title.lowercase(Locale.ROOT).replace(NON_WORD, " ").trim()

    private fun stripExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) return fileName
        val extension = fileName.substring(dot + 1).lowercase(Locale.ROOT)
        return if (extension in VIDEO_EXTENSIONS) fileName.substring(0, dot) else fileName
    }

    /** Dots and underscores separate words in release names. */
    private fun normalize(text: String): String = text.replace('.', ' ').replace('_', ' ')

    /** The file's parent folder, then its grandparent. */
    private fun parentFolders(path: String?): List<String> =
        path.orEmpty().replace('\\', '/').split('/').filter { it.isNotBlank() }
            .dropLast(1).takeLast(2).reversed()
}
