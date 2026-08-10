package com.devbytes.vixplayer.app.briefing.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.briefing.ZoneMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onExit: () -> Unit = {},
    onFinished: () -> Unit = {},
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val step = ui.step

    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshConnection() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up briefing") },
                navigationIcon = {
                    IconButton(onClick = { if (step == SetupViewModel.Step.INTENT) onExit() else viewModel.back() }) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // The Sample step hosts BriefingContent (a LazyColumn), which can't live inside a
            // verticalScroll parent (infinite-height constraint crash) — it scrolls itself.
            val scrollable = step != SetupViewModel.Step.SAMPLE
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (step) {
                    SetupViewModel.Step.INTENT -> {
                        Text("Every morning, before you start", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("VixPlay can put together a short brief of your day — your meetings, conflicts, and gaps. It takes about a minute to set up, and everything stays on your device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SetupViewModel.Step.SOURCES -> {
                        Text("What should your briefing include?", style = MaterialTheme.typography.titleLarge)
                        SourceRow("Today's calendar", "Events, conflicts, and gaps — read-only.", enabled = true, checked = true)
                        SourceRow("Urgent email", "Coming soon.", enabled = false, checked = false)
                        SourceRow("Mentions & tags", "Coming soon.", enabled = false, checked = false)
                        SourceRow("News you follow", "Coming soon.", enabled = false, checked = false)
                    }
                    SetupViewModel.Step.SCHEDULE -> {
                        Text("When should it reach you?", style = MaterialTheme.typography.titleLarge)
                        TimeStepper(ui.deliveryMinute, viewModel::setTime)
                        Spacer(Modifier.height(4.dp))
                        Text("On these days", style = MaterialTheme.typography.titleMedium)
                        DaySelector(ui.daysMask, viewModel::toggleDay)
                        Spacer(Modifier.height(4.dp))
                        Text("Time zone", style = MaterialTheme.typography.titleMedium)
                        ZoneChoice("Follow my phone", ui.zoneMode == ZoneMode.FOLLOW_DEVICE) { viewModel.setZoneMode(ZoneMode.FOLLOW_DEVICE) }
                        ZoneChoice("Stay fixed to ${ui.zoneId}", ui.zoneMode == ZoneMode.FIXED_ZONE) { viewModel.setZoneMode(ZoneMode.FIXED_ZONE) }
                        if (!ui.canDeliverExactly) {
                            Text("Exact alarms are off, so delivery may vary by a few minutes. You can allow them in system settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SetupViewModel.Step.CONNECT -> {
                        Text("Connect your calendar", style = MaterialTheme.typography.titleLarge)
                        Text("VixPlay needs permission to read today's events. Nothing leaves your device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (ui.calendarConnected) {
                            Text("✓ Calendar connected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(onClick = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) }) { Text("Grant calendar access") }
                        }
                    }
                    SetupViewModel.Step.SAMPLE -> {
                        Text("Here's a sample from your real calendar", style = MaterialTheme.typography.titleLarge)
                        when {
                            ui.sampleLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            ui.sampleBrief != null -> {
                                val dateLine = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()).format(LocalDate.now())
                                BriefingContent(ui.sampleBrief!!, sample = true, dateLine = dateLine)
                            }
                            else -> Text("Couldn't read your calendar. Go back and connect it to preview your brief.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SetupViewModel.Step.CONFIRM -> {
                        Text("You're all set", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        val t = LocalTime.of(ui.deliveryMinute / 60, ui.deliveryMinute % 60)
                        Text("Your briefing goes live at ${DateTimeFormatter.ofPattern("h:mm a").format(t)} on your selected days.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Bottom action bar.
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (step) {
                    SetupViewModel.Step.INTENT -> PrimaryButton("Get started", onClick = viewModel::next)
                    SetupViewModel.Step.SOURCES -> PrimaryButton("Continue", onClick = viewModel::next)
                    SetupViewModel.Step.SCHEDULE -> PrimaryButton("Continue", enabled = ui.daysMask != 0, onClick = viewModel::next)
                    SetupViewModel.Step.CONNECT -> PrimaryButton("Continue", enabled = ui.calendarConnected, onClick = viewModel::next)
                    SetupViewModel.Step.SAMPLE -> {
                        PrimaryButton("Go live ✓", enabled = ui.sampleBrief != null) { viewModel.goLive { viewModel.next() } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = viewModel::back, modifier = Modifier.weight(1f)) { Text("Adjust time") }
                        }
                    }
                    SetupViewModel.Step.CONFIRM -> PrimaryButton("Done", onClick = onFinished)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun SourceRow(title: String, subtitle: String, enabled: Boolean, checked: Boolean) {
    ListItem(
        headlineContent = { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, enabled = enabled, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun TimeStepper(minuteOfDay: Int, onChange: (Int) -> Unit) {
    val t = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onChange((minuteOfDay - 30 + 1440) % 1440) }) { Text("−30m") }
        Text(DateTimeFormatter.ofPattern("h:mm a").format(t), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { onChange((minuteOfDay + 30) % 1440) }) { Text("+30m") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelector(mask: Int, onToggle: (DayOfWeek) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { day ->
            val on = (mask shr (day.value - 1)) and 1 == 1
            FilterChip(
                selected = on,
                onClick = { onToggle(day) },
                label = { Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun ZoneChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
