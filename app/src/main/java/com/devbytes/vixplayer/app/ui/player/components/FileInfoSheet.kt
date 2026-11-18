package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle

/**
 * Snapshot of what's shown in the File Info sheet. File fields come from MediaStore,
 * stream fields are read live off the player at open time. Any null value omits its row.
 */
data class FileInfo(
    val name: String,
    val path: String?,
    val sizeBytes: Long?,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val videoCodecMime: String?,
    val audioCodecMime: String?,
    val frameRate: Float?,
    /** Decoder names from the player (e.g. "ffmpeg", "c2.android.aac.decoder"). */
    val videoDecoder: String? = null,
    val audioDecoder: String? = null,
    /** Mime of an audio/video track no renderer on this device can decode (track stays unselected). */
    val unsupportedVideoMime: String? = null,
    val unsupportedAudioMime: String? = null,
)

@Composable
fun FileInfoSheet(
    info: FileInfo?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = "File Info",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (info == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Column
        }

        SectionLabel("File")
        InfoRow("Name", info.name)
        info.path?.takeIf { it.isNotBlank() }?.let { InfoRow("Path", it) }
        info.sizeBytes?.let { InfoRow("Size", formatBytes(it)) }
        info.durationMs?.let { InfoRow("Duration", formatTimeMs(it)) }

        val resolution = info.width?.let { w -> info.height?.let { h -> "${w}\u00D7${h}" } }
        // An unsupported track has no selected format, so it's reported from the track list
        // instead and flagged inline — the fact stays discoverable after the HUD pill is gone.
        val videoCodec = info.videoCodecMime?.let(::prettyCodec)
            ?: info.unsupportedVideoMime?.let { "${prettyCodec(it)} (unsupported)" }
        val audioCodec = info.audioCodecMime?.let(::prettyCodec)
            ?: info.unsupportedAudioMime?.let { "${prettyCodec(it)} (unsupported)" }
        val hasStream = resolution != null ||
            videoCodec != null ||
            audioCodec != null ||
            info.frameRate != null
        if (hasStream) {
            SectionLabel("Stream")
            resolution?.let { InfoRow("Resolution", it) }
            videoCodec?.let { InfoRow("Video", it) }
            info.videoDecoder?.let { InfoRow("Video decoder", prettyDecoder(it)) }
            audioCodec?.let { InfoRow("Audio", it) }
            info.audioDecoder?.let { InfoRow("Audio decoder", prettyDecoder(it)) }
            info.frameRate?.let { InfoRow("Frame rate", "%.2f fps".format(it)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = TimecodeStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Human codec name for a Media3 sample mime; shared with the unsupported-codec messaging. */
internal fun prettyCodec(mime: String): String = when (mime.lowercase()) {
    "video/avc" -> "H.264"
    "video/hevc" -> "H.265 (HEVC)"
    "video/dolby-vision" -> "Dolby Vision"
    "video/x-vnd.on2.vp8" -> "VP8"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/av01" -> "AV1"
    "video/mp4v-es" -> "MPEG-4"
    "video/divx" -> "DivX"
    "video/mpeg2" -> "MPEG-2"
    "video/wvc1" -> "VC-1 (WMV)"
    "video/3gpp" -> "H.263"
    "audio/mp4a-latm" -> "AAC"
    "audio/mpeg" -> "MP3"
    "audio/mpeg-l2" -> "MP2"
    "audio/ac3" -> "Dolby AC-3"
    "audio/eac3", "audio/eac3-joc" -> "Dolby E-AC-3"
    "audio/true-hd" -> "Dolby TrueHD"
    "audio/vnd.dts" -> "DTS"
    "audio/vnd.dts.hd", "audio/vnd.dts.hd;profile=lbr" -> "DTS-HD"
    "audio/vnd.dts.uhd;profile=p2" -> "DTS:X"
    "audio/vorbis" -> "Vorbis"
    "audio/opus" -> "Opus"
    "audio/flac" -> "FLAC"
    "audio/alac" -> "ALAC"
    "audio/x-ms-wma" -> "WMA"
    "audio/raw" -> "PCM"
    else -> mime.substringAfter('/').uppercase()
}

/** Which path decoded the stream — the one visible proof that the FFmpeg extension engaged. */
private fun prettyDecoder(name: String): String = when {
    // FfmpegAudioRenderer reports e.g. "ffmpegLavc60.3.100-mp3" (libavcodec version + codec).
    name.startsWith("ffmpeg", ignoreCase = true) -> "FFmpeg (software)"
    name.startsWith("c2.android.") || name.startsWith("OMX.google.") -> "Android (software) · $name"
    else -> "Hardware · $name"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
