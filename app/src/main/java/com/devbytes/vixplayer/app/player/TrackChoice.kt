package com.devbytes.vixplayer.app.player

import androidx.media3.common.C
import androidx.media3.common.Tracks
import java.util.Locale

/** One audio or subtitle track as track choice sees it — plain data, so the rules unit-test on the JVM. */
data class TrackCandidate(
    /** The track sheets' `"group:track"` id. */
    val id: String,
    val language: String?,
    val playable: Boolean,
    val selected: Boolean,
)

/** A show's remembered subtitle choice. */
sealed interface SubtitleChoice {
    data object Off : SubtitleChoice
    /** On, ideally in [language] (null when the picked track had no language tag). */
    data class On(val language: String?) : SubtitleChoice
}

/** What was last picked for a show, applied to each of its episodes. */
data class RememberedTracks(val audioLanguage: String?, val subtitles: SubtitleChoice?)

/** What to change once a file's tracks are known; nulls mean leave as is. */
data class TrackPlan(val audioId: String? = null, val subtitleId: String? = null, val subtitlesOff: Boolean = false)

/**
 * Decides a file's audio and subtitle tracks once its tracks are known. A show's remembered
 * choice comes first; without one, "Turn on subtitles automatically" ([autoSubtitles]) applies;
 * otherwise the file's own defaults stand. One planner, so the two rules can't undo each other.
 */
fun planTracks(
    audio: List<TrackCandidate>,
    text: List<TrackCandidate>,
    remembered: RememberedTracks?,
    autoSubtitles: Boolean,
    device: Locale,
): TrackPlan {
    val audioId = remembered?.audioLanguage?.let { pickByLanguage(audio, it) }
    return when (val subtitles = remembered?.subtitles) {
        SubtitleChoice.Off -> TrackPlan(audioId, subtitlesOff = text.any { it.selected })
        is SubtitleChoice.On -> {
            val showing = text.firstOrNull { it.selected }
            val wanted = subtitles.language
            val keep = showing != null && (wanted == null || sameLanguage(showing.language, wanted))
            val pick = if (keep) null else wanted?.let { pickByLanguage(text, it) } ?: pickAutoSubtitle(text, device)
            TrackPlan(audioId, subtitleId = pick)
        }
        null -> TrackPlan(audioId, subtitleId = if (autoSubtitles) pickAutoSubtitle(text, device) else null)
    }
}

/**
 * The subtitle track "Turn on subtitles automatically" shows — or null when one is already
 * showing or none can play. Prefers [device]'s language, else the first playable track.
 */
fun pickAutoSubtitle(candidates: List<TrackCandidate>, device: Locale): String? {
    if (candidates.any { it.selected }) return null
    val playable = candidates.filter { it.playable }
    return (playable.firstOrNull { sameLanguage(it.language, device.toLanguageTag()) } ?: playable.firstOrNull())?.id
}

/** A playable track in [language], unless one is already playing — null means nothing to change. */
private fun pickByLanguage(candidates: List<TrackCandidate>, language: String): String? {
    if (candidates.any { it.selected && sameLanguage(it.language, language) }) return null
    return candidates.firstOrNull { it.playable && sameLanguage(it.language, language) }?.id
}

/** The player's tracks of [trackType], with the ids the track sheets use. */
fun Tracks.candidates(trackType: Int): List<TrackCandidate> =
    groups.withIndex()
        .filter { it.value.type == trackType }
        .flatMap { (groupIndex, group) ->
            (0 until group.length).map { trackIndex ->
                TrackCandidate(
                    id = "$groupIndex:$trackIndex",
                    language = group.getTrackFormat(trackIndex).language,
                    playable = group.isTrackSupported(trackIndex),
                    selected = group.isTrackSelected(trackIndex),
                )
            }
        }

/** The language of the track with [id] ("group:track"), if tagged. */
fun Tracks.languageOf(id: String): String? = (candidates(C.TRACK_TYPE_AUDIO) + candidates(C.TRACK_TYPE_TEXT))
    .firstOrNull { it.id == id }?.language

/** "en", "eng" and "en-US" are the same language; untagged or unknown codes never match. */
internal fun sameLanguage(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    val first = Locale.forLanguageTag(a)
    val second = Locale.forLanguageTag(b)
    if (first.language.isNotEmpty() && first.language == second.language) return true
    return runCatching { first.isO3Language.isNotEmpty() && first.isO3Language == second.isO3Language }.getOrDefault(false)
}
