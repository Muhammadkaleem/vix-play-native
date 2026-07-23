package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.ui.theme.DarkAccent
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle

/**
 * Transient pill shown on entry when playback resumes from a saved position.
 * The video keeps playing; tapping Restart seeks to 0. Only the Restart button
 * intercepts touches — the rest of the surface falls through to the tap layer.
 */
@Composable
fun ResumeChip(
    positionMs: Long,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ScrimStrong)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Resuming from ",
            style = MaterialTheme.typography.bodyMedium,
            color = OnScrim,
        )
        Text(
            text = formatTimeMs(positionMs),
            style = TimecodeStyle,
            color = OnScrim,
        )
        TextButton(onClick = onRestart) {
            Text(
                text = "Restart",
                style = MaterialTheme.typography.labelLarge,
                color = DarkAccent,
            )
        }
    }
}
