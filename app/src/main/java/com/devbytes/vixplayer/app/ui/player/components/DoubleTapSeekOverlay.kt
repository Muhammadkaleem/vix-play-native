package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.player.gesture.SeekSide
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DoubleTapSeekOverlay(
    side: SeekSide?,
    accumulatedMs: Long,
    triggerKey: Int,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(triggerKey) {
        if (side == null) return@LaunchedEffect
        launch { alpha.animateTo(1f, tween(120)) }
        launch { scale.animateTo(1f, tween(120)) }
        kotlinx.coroutines.delay(550)
        alpha.animateTo(0f, tween(180))
        scale.snapTo(0.85f)
    }

    if (side == null) return

    Box(modifier = modifier.fillMaxSize()) {
        val isLeft = side == SeekSide.LEFT
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .background(Color.White.copy(alpha = 0.07f * alpha.value)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value),
            ) {
                Icon(
                    painter = painterResource(if (isLeft) R.drawable.ic_rewind else R.drawable.ic_forward),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatSeekDelta(accumulatedMs),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

private fun formatSeekDelta(ms: Long): String {
    val s = abs(ms) / 1000
    return if (ms < 0) "−${s}s" else "+${s}s"
}
