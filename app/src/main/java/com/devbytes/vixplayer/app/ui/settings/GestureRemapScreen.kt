package com.devbytes.vixplayer.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.player.gesture.DragAction
import com.devbytes.vixplayer.app.player.gesture.TapAction

/**
 * Editor for the four remappable gesture slots (left/right vertical drag, left/right
 * double-tap). Every pick persists immediately via [GestureRemapViewModel]. The horizontal
 * scrub (seek), long-press speed-hold and pinch-to-zoom are fixed and noted as always-on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureRemapScreen(
    onBack: () -> Unit = {},
    viewModel: GestureRemapViewModel = hiltViewModel(),
) {
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestures") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::resetToDefaults) {
                        Text("Reset")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            SectionHeader("Swipe up / down")
            Hint("Slide vertically on one half of the video.")

            ChipRow(
                title = "Left side",
                options = DragAction.entries,
                selected = bindings.leftDrag,
                label = { it.label },
                onSelect = viewModel::setLeftDrag,
            )
            ChipRow(
                title = "Right side",
                options = DragAction.entries,
                selected = bindings.rightDrag,
                label = { it.label },
                onSelect = viewModel::setRightDrag,
            )

            SectionHeader("Double-tap")
            Hint("Double-tap one half to jump. The side sets the direction.")

            ChipRow(
                title = "Left side",
                options = TapAction.entries,
                selected = bindings.leftTap,
                label = { it.label },
                onSelect = viewModel::setLeftTap,
            )
            ChipRow(
                title = "Right side",
                options = TapAction.entries,
                selected = bindings.rightTap,
                label = { it.label },
                onSelect = viewModel::setRightTap,
            )

            SectionHeader("Always on")
            Hint(
                "Horizontal swipe scrubs the timeline, long-press holds fast-forward, " +
                    "and pinch zooms the video. These can't be remapped.",
            )
        }
    }
}

@Composable
private fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 2.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}
