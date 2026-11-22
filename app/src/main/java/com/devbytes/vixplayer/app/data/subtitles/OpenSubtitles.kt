package com.devbytes.vixplayer.app.data.subtitles

import com.devbytes.vixplayer.app.data.series.EpisodeParser
import org.json.JSONObject
import java.net.URLEncoder

/** A subtitle file found on OpenSubtitles.com. */
data class OnlineSubtitle(
    val fileId: Long,
    /** ISO 639-1-ish code from the API ("en", "pt-BR"). */
    val language: String,
    /** The release it was made for — what the viewer matches against their file's name. */
    val release: String,
    val downloads: Int,
    val hearingImpaired: Boolean,
    /** Made for this exact file (OpenSubtitles' hash of it matched). */
    val hashMatch: Boolean,
    /** Machine- or AI-translated rather than made by a person. */
    val translated: Boolean,
)

/** What to search for: free text, or a show's episode. */
data class SubtitleQuery(
    val text: String,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
)

/** Pure helpers for the OpenSubtitles REST API (v1): request building and response parsing. */
object OpenSubtitles {
    const val BASE_URL = "https://api.opensubtitles.com/api/v1"

    /** Each side of the file the hash reads. */
    const val HASH_CHUNK = 64 * 1024

    private val YEAR = Regex("""(?<!\d)((?:19|20)\d{2})(?!\d)""")

    /** Where a film's title stops in its file name: a year or a quality/source/codec tag. */
    private val TITLE_END = Regex(
        """(?<![a-z0-9])(?:\(?(?:19|20)\d{2}\)?|\d{3,4}p|4k|uhd|hdr\d*|10bit|x26[45]|h ?26[45]|hevc|avc|""" +
            """web(?:rip|-?dl)?|blu-?ray|brrip|bdrip|hdrip|hdtv|dvdrip|remux|proper|repack|extended|""" +
            """unrated|imax|amzn|nf|dsnp|hmax|dual|multi)(?![a-z0-9])""",
        RegexOption.IGNORE_CASE,
    )
    private val BRACKETS = Regex("""\[[^\]]*]""")
    private val SEPARATORS = Regex("""[._]+""")
    private val SPACES = Regex("""\s+""")

    /**
     * What a file most likely is, from its name (and path, whose folders can supply a show or
     * season): an episode → show + season + episode, otherwise a cleaned film title and its year.
     */
    fun queryFor(fileName: String, path: String? = null): SubtitleQuery {
        EpisodeParser.parse(fileName, path)?.let { info ->
            return SubtitleQuery(info.showTitle, season = info.season, episode = info.episode)
        }
        val base = fileName.substringBeforeLast('.', fileName)
            .replace(BRACKETS, " ")
            .replace(SEPARATORS, " ")
            .replace(SPACES, " ")
            .trim()
        val year = YEAR.find(base)?.value?.toIntOrNull()
        val title = TITLE_END.find(base)?.let { base.substring(0, it.range.first) } ?: base
        return SubtitleQuery(title.trim(' ', '-', '(').ifBlank { base }, year = year)
    }

    /**
     * The search URL. The API redirects any request whose parameters aren't sorted, lowercase
     * and `+`-encoded, so they're always built that way.
     */
    fun searchUrl(query: SubtitleQuery, languages: List<String>, movieHash: String?): String {
        val params = sortedMapOf<String, String>()
        if (languages.isNotEmpty()) params["languages"] = languages.joinToString(",") { it.lowercase() }
        if (movieHash != null) params["moviehash"] = movieHash
        if (query.text.isNotBlank()) params["query"] = query.text.trim().lowercase()
        query.season?.let { params["season_number"] = it.toString() }
        query.episode?.let { params["episode_number"] = it.toString() }
        if (query.season != null || query.episode != null) params["type"] = "episode"
        query.year?.let { params["year"] = it.toString() }
        return BASE_URL + "/subtitles?" + params.entries.joinToString("&") { (k, v) ->
            k + "=" + URLEncoder.encode(v, "UTF-8")
        }
    }

    /** The subtitles in a search response, one entry per file, best first. */
    fun parseSearch(json: String): List<OnlineSubtitle> {
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        val out = mutableListOf<OnlineSubtitle>()
        for (i in 0 until data.length()) {
            val attrs = data.optJSONObject(i)?.optJSONObject("attributes") ?: continue
            val files = attrs.optJSONArray("files") ?: continue
            val fileId = files.optJSONObject(0)?.optLong("file_id", -1L) ?: -1L
            if (fileId <= 0) continue
            out += OnlineSubtitle(
                fileId = fileId,
                language = attrs.optString("language"),
                release = attrs.optString("release").ifBlank {
                    files.optJSONObject(0)?.optString("file_name").orEmpty()
                },
                downloads = attrs.optInt("download_count"),
                hearingImpaired = attrs.optBoolean("hearing_impaired"),
                hashMatch = attrs.optBoolean("moviehash_match"),
                translated = attrs.optBoolean("machine_translated") || attrs.optBoolean("ai_translated"),
            )
        }
        // A file made for this exact release first, then the most downloaded.
        return out.sortedWith(compareByDescending<OnlineSubtitle> { it.hashMatch }.thenByDescending { it.downloads })
    }

    /**
     * OpenSubtitles' file hash: the size plus the sum of the first and last 64 KB read as
     * little-endian 64-bit words, wrapping, as 16 hex digits. Null for a file under 64 KB.
     */
    fun hash(size: Long, head: ByteArray, tail: ByteArray): String? {
        if (size < HASH_CHUNK || head.size < HASH_CHUNK || tail.size < HASH_CHUNK) return null
        var sum = size
        for (chunk in arrayOf(head, tail)) {
            for (i in 0 until HASH_CHUNK step 8) {
                var word = 0L
                for (b in 7 downTo 0) word = (word shl 8) or (chunk[i + b].toLong() and 0xFF)
                sum += word
            }
        }
        return "%016x".format(sum)
    }

    /** The subtitle format of a downloaded file, from its name, else its first bytes. */
    fun formatOf(fileName: String, text: String): SubtitleFormat {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        SubtitleFormat.entries.firstOrNull { ext in it.extensions }?.let { return it }
        val head = text.trimStart('﻿', ' ', '\n', '\r').take(64)
        return when {
            head.startsWith("[Script Info]", ignoreCase = true) -> SubtitleFormat.SSA
            head.startsWith("WEBVTT") -> SubtitleFormat.VTT
            else -> SubtitleFormat.SRT
        }
    }
}

enum class SubtitleFormat(val extensions: Set<String>, val mimeType: String, val fileExtension: String) {
    SRT(setOf("srt"), "application/x-subrip", "srt"),
    SSA(setOf("ass", "ssa"), "text/x-ssa", "ass"),
    VTT(setOf("vtt"), "text/vtt", "vtt"),
}
