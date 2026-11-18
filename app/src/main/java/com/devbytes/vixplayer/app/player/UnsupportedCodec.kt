package com.devbytes.vixplayer.app.player

import androidx.media3.common.C
import androidx.media3.common.Tracks

/**
 * ExoPlayer raises no error for a codec nothing can decode — the track selector just leaves
 * it unselected, so an MKV with (say) DTS audio plays *silently* and a VC-1 video shows a
 * black frame. These helpers make that state visible so the UI can say what happened and
 * Crashlytics can count it (the measurement the AC-3/DTS exclusion decision depends on).
 */

/**
 * Mime of the first [type] track group (audio/video) that has **no decoder at all** on this
 * device, or null when at least one group of that type is playable. Tracks that merely
 * exceed decoder capabilities (e.g. 4K on a 1080p decoder) are not counted — ExoPlayer
 * still attempts those, and a failure there surfaces through `onPlayerError`.
 */
fun Tracks.unsupportedMime(type: @C.TrackType Int): String? {
    val groups = this.groups.filter { it.type == type }
    if (groups.isEmpty()) return null
    val anyPlayable = groups.any { g ->
        (0 until g.length).any { i ->
            val support = g.getTrackSupport(i)
            support == C.FORMAT_HANDLED || support == C.FORMAT_EXCEEDS_CAPABILITIES
        }
    }
    if (anyPlayable) return null
    return groups.firstOrNull()?.getTrackFormat(0)?.sampleMimeType
}

/** Non-fatal recorded once per file+codec so Crashlytics groups by codec, not by file. */
class UnsupportedCodecException(val mime: String, val trackType: String) :
    Exception("No decoder for $trackType codec $mime")
