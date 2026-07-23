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
        val hasStream = resolution != null ||
            info.videoCodecMime != null ||
            info.audioCodecMime != null ||
            info.frameRate != null
        if (hasStream) {
            SectionLabel("Stream")
            resolution?.let { InfoRow("Resolution", it) }
            info.videoCodecMime?.let { InfoRow("Video", prettyCodec(it)) }
            info.audioCodecMime?.let { InfoRow("Audio", prettyCodec(it)) }
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

private fun prettyCodec(mime: String): String = when (mime.lowercase()) {
    "video/avc" -> "H.264"
    "video/hevc" -> "H.265 (HEVC)"
    "video/x-vnd.on2.vp8" -> "VP8"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/av01" -> "AV1"
    "video/mp4v-es" -> "MPEG-4"
    "video/3gpp" -> "H.263"
    "audio/mp4a-latm" -> "AAC"
    "audio/mpeg" -> "MP3"
    "audio/ac3" -> "AC-3"
    "audio/eac3" -> "E-AC-3"
    "audio/vorbis" -> "Vorbis"
    "audio/opus" -> "Opus"
    "audio/flac" -> "FLAC"
    "audio/raw" -> "PCM"
    else -> mime.substringAfter('/').uppercase()
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
