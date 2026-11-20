package com.devbytes.vixplayer.app.ui.shows

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.ShowWithVideos
import com.devbytes.vixplayer.app.data.series.Show
import com.devbytes.vixplayer.app.ui.theme.OnScrim

/** Blur needs RenderEffect (API 31+); below that the panel leans on a darker gradient instead. */
private val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * A show on the Home shelf, drawn as a whole series rather than one video: two layers peek
 * above the poster like a stack of episodes, and the lower half is a frosted panel — a blurred
 * copy of the poster that fades in progressively — carrying the title, the show's size, the
 * next action and how much of it is watched.
 */
@Composable
fun ShowPosterCard(entry: ShowWithVideos, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val show = entry.show
    // Further in than the 10% other cards use: openings are often dark titles or recaps.
    // 4:3 rather than a tall poster: at 80% of the screen a 3:4 card filled half of it.
    val poster = ImageRequest.Builder(LocalContext.current)
        .data(entry.videoOf(show.continueEpisode)?.uri)
        .decoderFactory(VideoFrameDecoder.Factory())
        .videoFramePercent(0.3)
        .build()

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // The "stack of episodes" — theme-aware, since it sits on the page rather than the image.
        StackEdge(widthFraction = 0.75f, alpha = 0.10f)
        StackEdge(widthFraction = 0.85f, alpha = 0.20f)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClickLabel = "Open show", onClick = onClick),
        ) {
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // The blur gradient: the same poster, blurred, masked in from about the middle down.
            if (canBlur) {
                AsyncImage(
                    model = poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(0.30f to Color.Transparent, 0.56f to Color.Black),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                        .blur(22.dp, BlurredEdgeTreatment.Rectangle),
                )
            }
            // Legibility scrim over the image (theme-independent, like every over-video surface).
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (canBlur) 0.72f else 0.9f),
                    ),
                ),
            )
            ShowInfo(show, Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun ShowInfo(show: Show, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            show.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnScrim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            sizeLabel(show),
            style = MaterialTheme.typography.bodySmall,
            color = OnScrim.copy(alpha = 0.72f),
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = OnScrim, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(
                continueLabel(show),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnScrim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(OnScrim.copy(alpha = 0.25f))) {
                Box(
                    Modifier
                        .fillMaxWidth(show.watchedCount.toFloat() / show.episodes.size)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                "${show.watchedCount}/${show.episodes.size}",
                style = MaterialTheme.typography.labelSmall,
                color = OnScrim.copy(alpha = 0.72f),
            )
        }
    }
}

/** A sliver of the "episode below" peeking above the poster. */
@Composable
private fun StackEdge(widthFraction: Float, alpha: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(6.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
    )
}

/** "4 Seasons · 24 Episodes". */
private fun sizeLabel(show: Show): String {
    val seasons = show.seasons.count { it != 0 }
    val episodes = show.episodes.size
    return "${if (seasons == 1) "1 Season" else "$seasons Seasons"} · ${if (episodes == 1) "1 Episode" else "$episodes Episodes"}"
}
