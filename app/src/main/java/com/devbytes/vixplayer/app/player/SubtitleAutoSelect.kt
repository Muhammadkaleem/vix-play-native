package com.devbytes.vixplayer.app.player

import androidx.media3.common.C
import androidx.media3.common.Tracks
import java.util.Locale

/** One subtitle track as the automatic pick sees it — plain data, so the rule unit-tests on the JVM. */
data class SubtitleCandidate(
    /** The subtitle sheet's `"group:track"` id. */
    val id: String,
    val language: String?,
    val playable: Boolean,
    val selected: Boolean,
)

/**
 * The subtitle track "Turn on subtitles automatically" should show — or null when one is
 * already showing or none can play. Prefers a track in [device]'s language, else the first
 * playable one.
 */
fun pickAutoSubtitle(candidates: List<SubtitleCandidate>, device: Locale): String? {
    if (candidates.any { it.selected }) return null
    val playable = candidates.filter { it.playable }
    return (playable.firstOrNull { sameLanguage(it.language, device) } ?: playable.firstOrNull())?.id
}

/** [pickAutoSubtitle] over the player's current tracks. */
fun Tracks.autoSubtitleId(device: Locale = Locale.getDefault()): String? = pickAutoSubtitle(
    groups.withIndex()
        .filter { it.value.type == C.TRACK_TYPE_TEXT }
        .flatMap { (groupIndex, group) ->
            (0 until group.length).map { trackIndex ->
                SubtitleCandidate(
                    id = "$groupIndex:$trackIndex",
                    language = group.getTrackFormat(trackIndex).language,
                    playable = group.isTrackSupported(trackIndex),
                    selected = group.isTrackSelected(trackIndex),
                )
            }
        },
    device,
)

/** "en", "eng" and "en-US" all match an English device; unknown codes never match. */
private fun sameLanguage(trackLanguage: String?, device: Locale): Boolean {
    if (trackLanguage.isNullOrBlank()) return false
    val track = Locale.forLanguageTag(trackLanguage)
    if (track.language == device.language) return true
    return runCatching { track.isO3Language == device.isO3Language }.getOrDefault(false)
}
