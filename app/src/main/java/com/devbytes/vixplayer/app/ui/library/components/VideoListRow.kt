package com.devbytes.vixplayer.app.ui.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.theme.TrackInactive

/**
 * Compact video row — the "list" view shape, denser than [VideoCard]'s grid card.
 *
 * Shares the 96×54 frame + resume-fill vocabulary already used by [HistoryRow] and the
 * folder-list rows, so the app has one compact-row look rather than three. Carries
 * selection so it can stand in for [VideoCard] on the same multi-select surfaces.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListRow(
    video: VideoFile,
    resumeFraction: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = resumeFraction.coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(
                modifier = Modifier.size(width = 96.dp, height = 54.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Fallback shown behind the frame: visible while decoding and if the
                // frame can't be extracted, so a failed thumbnail is a clear video glyph
                // rather than an empty box.
                Icon(
                    painter = painterResource(R.drawable.ic_nav_video),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
                AsyncImage(
                    // Grab a frame 10% in — the first frame is often black.
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .videoFramePercent(0.1)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 96.dp, height = 54.dp),
                )
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(TrackInactive),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (video.durationMs > 0) {
                Text(
                    text = formatRowDuration(video.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** m:ss, or h:mm:ss past an hour. */
private fun formatRowDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "$m:${s.toString().padStart(2, '0')}"
    }
}
