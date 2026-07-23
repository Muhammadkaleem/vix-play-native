package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.db.entity.AudioOutput
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/**
 * Multiband EQ plus bass boost, virtualizer and preamp.
 *
 * Band count, frequencies, level range and preset names are all read from the hardware
 * rather than assumed — they differ across devices. On ROMs that refuse `audiofx` the
 * controls are not rendered at all, only an explanation, so nothing looks broken.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit = {},
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val output by viewModel.output.collectAsState()
    val presets by viewModel.presets.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val caps = state.capabilities
        if (!state.supported || caps == null) {
            LibraryEmptyState(
                iconRes = R.drawable.ic_equalizer,
                title = "Equalizer unavailable",
                body = "This device doesn't allow apps to apply audio effects.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Applies to ${output.label()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.enabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                )
            }

            SectionLabel("Preset")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Preset rows can exceed the width on small screens.
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                caps.presetNames.forEachIndexed { index, name ->
                    FilterChip(
                        selected = state.presetName == name,
                        onClick = { viewModel.applyDevicePreset(index, name) },
                        label = { Text(name) },
                    )
                }
            }

            if (presets.isNotEmpty()) {
                SectionLabel("Saved")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { viewModel.applySavedPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.deletePreset(preset) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = "Delete ${preset.name}",
                                    )
                                }
                            },
                        )
                    }
                }
            }

            SectionLabel("Bands")
            caps.bandFrequencies.forEachIndexed { index, freq ->
                val level = state.bandLevels.getOrElse(index) { 0 }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatFrequency(freq),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(56.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { viewModel.setBand(index, it.toInt()) },
                        valueRange = caps.minLevelMb.toFloat()..caps.maxLevelMb.toFloat(),
                        enabled = state.enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${level / 100}dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp),
                    )
                }
            }

            if (caps.hasBassBoost) {
                SectionLabel("Bass boost")
                Slider(
                    value = state.bassBoost.toFloat(),
                    onValueChange = { viewModel.setBassBoost(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = state.enabled,
                )
            }

            if (caps.hasVirtualizer) {
                SectionLabel("Virtualizer")
                Slider(
                    value = state.virtualizer.toFloat(),
                    onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = state.enabled,
                )
            }

            if (caps.hasPreamp) {
                SectionLabel("Preamp")
                Text(
                    text = "Extra gain. High values can distort.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.preampMb.toFloat(),
                    onValueChange = { viewModel.setPreamp(it.toInt()) },
                    valueRange = 0f..1500f,
                    enabled = state.enabled,
                )
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { showSaveDialog = true }, enabled = state.enabled) {
                Text("Save as preset")
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.savePreset(name.trim())
                        showSaveDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

private fun AudioOutput.label(): String = when (this) {
    AudioOutput.SPEAKER -> "speaker"
    AudioOutput.HEADSET -> "headphones"
    AudioOutput.BLUETOOTH -> "Bluetooth"
}

private fun formatFrequency(hz: Int): String =
    if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"

