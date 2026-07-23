package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Session-local sleep-timer selection. [Off] cancels; [Minutes] arms a wall-clock
 * countdown that pauses the player when it elapses; [EndOfVideo] pauses when the
 * current file finishes (suppressing the normal Ended/replay overlay).
 */
sealed interface SleepTimerMode {
    object Off : SleepTimerMode
    data class Minutes(val value: Int) : SleepTimerMode
    object EndOfVideo : SleepTimerMode
}

/** The offered minute presets — the conventional pod-player set. */
val SLEEP_TIMER_MINUTES: List<Int> = listOf(15, 30, 45, 60)

@Composable
fun SleepTimerSheet(
    activeMode: SleepTimerMode,
    onSelect: (SleepTimerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = "Sleep timer",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        SleepTimerRow(
            label = "Off",
            selected = activeMode is SleepTimerMode.Off,
            onClick = { onSelect(SleepTimerMode.Off) },
        )
        SLEEP_TIMER_MINUTES.forEach { minutes ->
            SleepTimerRow(
                label = "$minutes min",
                selected = activeMode is SleepTimerMode.Minutes && activeMode.value == minutes,
                onClick = { onSelect(SleepTimerMode.Minutes(minutes)) },
            )
        }
        SleepTimerRow(
            label = "End of video",
            selected = activeMode is SleepTimerMode.EndOfVideo,
            onClick = { onSelect(SleepTimerMode.EndOfVideo) },
        )
    }
}

@Composable
private fun SleepTimerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
