package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.theme.AmoledBackground
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong

/**
 * Full-bleed graceful error state. Swallows touches so nothing reaches the video
 * surface underneath. SW-decoder retry / report actions are deferred (Step 2).
 */
@Composable
fun ErrorOverlay(
    message: String,
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
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = OnScrim,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Can't play this video",
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
 * Gesture-lock state. The dim layer swallows taps (video surface owns nothing);
 * only the unlock chip is active — the purest expression of gesture ownership.
 */
@Composable
fun LockOverlay(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground.copy(alpha = 0.35f))
            .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onUnlock,
            shape = CircleShape,
            color = ScrimStrong,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = "Unlock controls",
                tint = OnScrim,
                modifier = Modifier
                    .padding(14.dp)
                    .size(28.dp),
            )
        }
    }
}
