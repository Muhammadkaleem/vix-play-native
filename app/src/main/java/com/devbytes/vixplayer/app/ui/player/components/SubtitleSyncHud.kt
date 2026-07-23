package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.ui.theme.DarkAccent
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle

/** Coarse jump applied on a stepper long-press, for badly desynced files. */
const val SUBTITLE_SYNC_COARSE_MS = 500L

/** The PRD's granularity: offset adjusts in 50 ms steps. */
const val SUBTITLE_SYNC_STEP_MS = 50L

/**
 * Interactive sync-adjust panel shown *over the video* so the correction can actually
 * be judged against the picture — a bottom sheet would cover the frame being tuned.
 *
 * Unlike the transient HUD pills this one accepts touches, so it sits above the gesture
 * layer; taps outside it still fall through to the surface below.
 */
@Composable
fun SubtitleSyncHud(
    visible: Boolean,
    offsetMs: Long,
    onStep: (Long) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ScrimStrong)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Subtitle sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnScrim.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatSubtitleOffset(offsetMs),
                    style = TimecodeStyle,
                    color = OnScrim,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Text(
                    text = if (offsetMs == 0L) "In sync" else if (offsetMs > 0) "Subtitles later" else "Subtitles earlier",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnScrim.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    SyncButton(
                        label = "−50 ms",
                        onClick = { onStep(-SUBTITLE_SYNC_STEP_MS) },
                        onLongClick = { onStep(-SUBTITLE_SYNC_COARSE_MS) },
                    )
                    Spacer(Modifier.width(10.dp))
                    SyncButton(label = "Reset", onClick = onReset, accent = false)
                    Spacer(Modifier.width(10.dp))
                    SyncButton(
                        label = "+50 ms",
                        onClick = { onStep(SUBTITLE_SYNC_STEP_MS) },
                        onLongClick = { onStep(SUBTITLE_SYNC_COARSE_MS) },
                    )
                    Spacer(Modifier.width(10.dp))
                    SyncButton(label = "Done", onClick = onDone, accent = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SyncButton(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (accent) DarkAccent else OnScrim.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** "+250 ms" / "−1.20 s" / "0 ms" — signed, and seconds once past a second. */
fun formatSubtitleOffset(ms: Long): String = when {
    ms == 0L -> "0 ms"
    kotlin.math.abs(ms) < 1000 -> "${if (ms > 0) "+" else "−"}${kotlin.math.abs(ms)} ms"
    else -> {
        val secs = kotlin.math.abs(ms) / 1000.0
        "${if (ms > 0) "+" else "−"}${String.format("%.2f", secs)} s"
    }
}
