package com.devbytes.vixplayer.app.ui.library.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.devbytes.vixplayer.app.ui.library.VideoWithPosition
import com.devbytes.vixplayer.app.ui.theme.TrackInactive
import kotlin.math.roundToInt

private const val WATCHED_THRESHOLD = 0.95f

/**
 * A single recently-played entry: 96×54 frame thumbnail (with a resume-fill bar
 * mirroring [VideoCard]) + title and a "last-played · progress" secondary line.
 * Finished items (≥95%) read "Watched" instead of a percentage.
 */
@Composable
fun HistoryRow(
    entry: VideoWithPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = entry.progressFraction.coerceIn(0f, 1f)
    val watched = fraction >= WATCHED_THRESHOLD
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        entry.lastPlayedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    val progressLabel = if (watched) "Watched" else "${(fraction * 100).roundToInt()}%"

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(modifier = Modifier.size(width = 96.dp, height = 54.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(entry.video.uri)
                            .decoderFactory(VideoFrameDecoder.Factory())
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
                    text = entry.video.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$relativeTime · $progressLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
