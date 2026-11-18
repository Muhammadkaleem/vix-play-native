package com.devbytes.vixplayer.app.cast

/**
 * Pre-flight check: what a Chromecast running the Default Media Receiver can decode. There is
 * no transcoding on either side, so this is decided from the local file's track mimes *before*
 * a session is started, giving the user the PRD's "Incompatible media" state up front instead
 * of a black screen on the TV.
 *
 * Pure function — unit-tested in `CastCompatibilityTest`.
 */
enum class CastSupport { OK, WARN, INCOMPATIBLE }

data class CastVerdict(val support: CastSupport, val reason: String? = null)

object CastCompatibility {

    private val VIDEO_OK = setOf("video/avc", "video/x-vnd.on2.vp8", "video/x-vnd.on2.vp9")

    /** Decodable on Chromecast Ultra / Google TV but not the 1st–3rd gen dongles. */
    private val VIDEO_NEWER_ONLY = mapOf(
        "video/hevc" to "HEVC video",
        "video/av01" to "AV1 video",
        "video/dolby-vision" to "Dolby Vision video",
    )

    private val AUDIO_OK = setOf(
        "audio/mp4a-latm", "audio/mpeg", "audio/opus", "audio/vorbis", "audio/flac", "audio/raw",
    )

    /** Passed through to the TV; plays only if the TV/soundbar decodes Dolby. */
    private val AUDIO_PASSTHROUGH = mapOf(
        "audio/ac3" to "Dolby AC-3 audio",
        "audio/eac3" to "Dolby E-AC-3 audio",
        "audio/eac3-joc" to "Dolby Atmos audio",
    )

    /**
     * @param videoMime sample mime of the video track (null = audio-only file).
     * @param audioMime sample mime of the selected audio track (null = no audio).
     */
    fun check(videoMime: String?, audioMime: String?): CastVerdict {
        val v = videoMime?.lowercase()
        val a = audioMime?.lowercase()

        if (v != null && v !in VIDEO_OK && v !in VIDEO_NEWER_ONLY) {
            return CastVerdict(CastSupport.INCOMPATIBLE, "Chromecast can't play ${prettyName(v)}")
        }
        if (a != null && a !in AUDIO_OK && a !in AUDIO_PASSTHROUGH) {
            return CastVerdict(CastSupport.INCOMPATIBLE, "Chromecast can't play ${prettyName(a)}")
        }
        VIDEO_NEWER_ONLY[v]?.let {
            return CastVerdict(CastSupport.WARN, "$it needs a newer Chromecast (Ultra / Google TV)")
        }
        AUDIO_PASSTHROUGH[a]?.let {
            return CastVerdict(CastSupport.WARN, "$it plays only if your TV decodes Dolby")
        }
        return CastVerdict(CastSupport.OK)
    }

    private fun prettyName(mime: String): String = when (mime) {
        "video/mp4v-es" -> "MPEG-4 video"
        "video/divx" -> "DivX video"
        "video/mpeg2" -> "MPEG-2 video"
        "video/wvc1" -> "VC-1 (WMV) video"
        "video/3gpp" -> "H.263 video"
        "audio/vnd.dts", "audio/vnd.dts.hd" -> "DTS audio"
        "audio/true-hd" -> "Dolby TrueHD audio"
        "audio/x-ms-wma" -> "WMA audio"
        "audio/alac" -> "ALAC audio"
        "audio/mpeg-l2" -> "MP2 audio"
        else -> mime.substringAfter('/').uppercase() +
            if (mime.startsWith("video/")) " video" else " audio"
    }
}
