package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.theme.AmoledBackground
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong

/**
 * Full-bleed graceful error state. Swallows touches so nothing reaches the video
 * surface underneath.
 *
 * [onRetry] is supplied only for recoverable failures (network unreachable / timeout /
 * HTTP status) — the expected case for streamed URLs — and renders a Retry action above
 * Back. Codec-type failures pass it null, since re-preparing won't help.
 */
@Composable
fun ErrorOverlay(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Can't play this video",
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground.copy(alpha = 0.85f))
            .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = OnScrim,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OnScrim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OnScrim.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (onRetry != null) {
                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    color = ScrimStrong,
                ) {
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnScrim,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                color = ScrimStrong,
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnScrim,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * Terminal "finished" state (STATE_ENDED). Swallows touches so the ended frame
 * underneath is inert. Replay is always offered; Play next only when a folder-next
 * exists (else the overlay degrades to Replay + Back). No silent auto-advance.
 */
@Composable
fun EndedOverlay(
    hasNext: Boolean,
    onReplay: () -> Unit,
    onPlayNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground.copy(alpha = 0.85f))
            .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = null,
                tint = OnScrim,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Finished",
                style = MaterialTheme.typography.titleMedium,
                color = OnScrim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            EndedAction(iconRes = R.drawable.ic_play, label = "Replay", onClick = onReplay)
            if (hasNext) {
                Spacer(Modifier.height(12.dp))
                EndedAction(iconRes = R.drawable.ic_next, label = "Play next", onClick = onPlayNext)
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                color = ScrimStrong,
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnScrim,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun EndedAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = ScrimStrong,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = OnScrim,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = OnScrim,
            )
        }
    }
}

/**
 * Gesture-lock state, three phases:
 *  - **Locked (just locked):** a centred "Screen locked · Swipe up to unlock" prompt appears,
 *    then fades after a few seconds leaving a clean video view.
 *  - **Resting:** only a subtle lock indicator on the right edge remains.
 *  - **Tapped:** tapping the locked screen re-reveals the prompt (which fades again).
 * A decisive swipe up unlocks. The overlay swallows all single-finger touches so the video
 * surface owns nothing (player gestures are already disabled while locked).
 */
@Composable
fun LockOverlay(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    var promptKey by remember { mutableIntStateOf(0) }
    var showPrompt by remember { mutableStateOf(true) }
    // Each reveal (lock, tap, drag start) restarts the fade-out timer (kept short).
    LaunchedEffect(promptKey) {
        showPrompt = true
        delay(4600)
        showPrompt = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Tap re-reveals the prompt; a decisive swipe up unlocks. Nothing reaches the video.
            .pointerInput(Unit) { detectTapGestures { promptKey++ } }
            .pointerInput(Unit) {
                var total = 0f
                val threshold = 90.dp.toPx()
                detectVerticalDragGestures(
                    onDragStart = { total = 0f; promptKey++ },
                    onDragEnd = { if (total <= -threshold) onUnlock() },
                ) { _, dy -> total += dy }
            },
    ) {
        // Resting indicator (right edge) — visible only once the prompt has faded.
        AnimatedVisibility(
            visible = !showPrompt,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
        ) {
            LockEdgeIndicator(accent)
        }

        // Active prompt (bottom) — shown on lock and on each tap, fades after a few seconds.
        AnimatedVisibility(
            visible = showPrompt,
            enter = fadeIn(tween(200)) + scaleIn(tween(240), initialScale = 0.85f),
            exit = fadeOut(tween(260)) + scaleOut(tween(260), targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
        ) {
            UnlockPrompt(accent)
        }
    }
}

@Composable
private fun UnlockPrompt(accent: Color) {
    // Compact pill: a bouncing up-chevron, a small accent lock badge, and the label — in one
    // short row anchored at the bottom.
    val bounce = rememberInfiniteTransition(label = "chevron")
    val dy by bounce.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bounce",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ScrimStrong)
            .padding(start = 12.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = OnScrim, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Swipe up to unlock",
            style = MaterialTheme.typography.labelLarge,
            color = OnScrim,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp).rotate(180f).offset(y = dy.dp),
        )
    }
}

@Composable
private fun LockEdgeIndicator(accent: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ScrimStrong)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = OnScrim.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp).rotate(180f),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_lock), "Locked", tint = OnScrim, modifier = Modifier.size(18.dp))
        }
    }
}
