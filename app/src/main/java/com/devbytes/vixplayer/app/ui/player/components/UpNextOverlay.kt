package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.components.formatDuration
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle

/**
 * End-of-video "Up Next" screen. Header carries a close (X → back) and an Autoplay toggle;
 * the body shows the playlist position, up-next thumbnails, and mode-dependent actions:
 *  - [countdownSec] non-null  → autoplay is counting down ("Playing next in Ns" + Play Now/Cancel)
 *  - [confirmMode]            → autoplay off but asking ("Play Next"/"Not Now" + "Don't ask again")
 *  - otherwise                → manual Play Again / Play Next
 * Empty folder degrades to Play Again only (device back still exits).
 */
@Composable
fun UpNextOverlay(
    currentUri: String,
    upcoming: List<VideoFile>,
    positionLabel: String?,
    autoplayEnabled: Boolean,
    confirmMode: Boolean,
    countdownSec: Int?,
    onToggleAutoplay: (Boolean) -> Unit,
    onReplay: () -> Unit,
    onPlay: (VideoFile, Boolean) -> Unit,
    onNotNow: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    var dontAskAgain by remember { mutableStateOf(false) }
    val next = upcoming.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures {} }, // swallow taps from the surface below
    ) {
        // Blurred frame of the finished video as the backdrop (no-op blur below API 31).
        AsyncImage(
            model = frameRequest(context, currentUri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(30.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(0.55f), Color.Black.copy(0.85f))),
            ),
        )

        // Header: close (X) + Autoplay toggle.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), "Close", tint = OnScrim)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Autoplay", style = MaterialTheme.typography.labelLarge, color = OnScrim)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = autoplayEnabled,
                    onCheckedChange = onToggleAutoplay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnScrim,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = OnScrim.copy(alpha = 0.8f),
                        uncheckedTrackColor = OnScrim.copy(alpha = 0.18f),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }

        // Centred body.
        Column(
            Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(52.dp).border(2.dp, accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check), null, tint = OnScrim, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Up Next", style = MaterialTheme.typography.headlineSmall, color = OnScrim, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    upcoming.isEmpty() -> "Nothing else in this folder"
                    positionLabel != null -> "Finished $positionLabel · next in this folder"
                    else -> "Continue with the next in this folder"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OnScrim.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            if (upcoming.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    items(upcoming, key = { it.mediaStoreId }) { video ->
                        UpNextCard(
                            video = video,
                            featured = video.mediaStoreId == next?.mediaStoreId,
                            accent = accent,
                            onClick = { onPlay(video, false) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            // Actions by mode.
            when {
                countdownSec != null && next != null -> {
                    Text(
                        "Playing next in ${countdownSec}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnScrim.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(R.drawable.ic_close, "Cancel", filled = false, accent = accent) { onNotNow(false) }
                        PillButton(R.drawable.ic_play, "Play Now", filled = true, accent = accent) { onPlay(next, false) }
                    }
                }
                confirmMode && next != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(R.drawable.ic_close, "Not Now", filled = false, accent = accent) { onNotNow(dontAskAgain) }
                        PillButton(R.drawable.ic_play, "Play Next", filled = true, accent = accent) { onPlay(next, dontAskAgain) }
                    }
                    Spacer(Modifier.height(12.dp))
                    DontAskRow(checked = dontAskAgain, accent = accent) { dontAskAgain = it }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(R.drawable.ic_repeat, "Play Again", filled = false, accent = accent, onClick = onReplay)
                        if (next != null) {
                            PillButton(R.drawable.ic_play, "Play Next", filled = true, accent = accent) { onPlay(next, false) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DontAskRow(checked: Boolean, accent: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) accent else Color.Transparent)
                .border(1.5.dp, if (checked) accent else OnScrim.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(painterResource(R.drawable.ic_check), null, tint = OnScrim, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("Don't ask again", style = MaterialTheme.typography.labelLarge, color = OnScrim.copy(alpha = 0.8f))
    }
}

@Composable
private fun UpNextCard(video: VideoFile, featured: Boolean, accent: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.width(190.dp).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .then(if (featured) Modifier.border(2.dp, accent, RoundedCornerShape(12.dp)) else Modifier),
        ) {
            AsyncImage(
                model = frameRequest(context, video.uri.toString()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (featured) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(ScrimStrong),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_play), null, tint = OnScrim, modifier = Modifier.size(22.dp))
                    }
                }
            }
            if (video.durationMs > 0L) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = ScrimStrong,
                ) {
                    Text(
                        formatDuration(video.durationMs),
                        style = TimecodeStyle,
                        color = OnScrim,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            video.name,
            style = MaterialTheme.typography.bodyMedium,
            color = OnScrim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PillButton(
    iconRes: Int,
    label: String,
    filled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (filled) accent else Color.Transparent,
        border = if (filled) null else BorderStroke(1.dp, OnScrim.copy(alpha = 0.3f)),
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(iconRes), null, tint = OnScrim, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = OnScrim, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun frameRequest(context: android.content.Context, uri: String) =
    ImageRequest.Builder(context)
        .data(uri)
        .decoderFactory(VideoFrameDecoder.Factory())
        .videoFramePercent(0.1)
        .build()
