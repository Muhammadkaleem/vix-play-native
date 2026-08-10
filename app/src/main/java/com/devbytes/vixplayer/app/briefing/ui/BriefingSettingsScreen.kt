package com.devbytes.vixplayer.app.briefing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingSettingsScreen(
    onBack: () -> Unit = {},
    onSetup: () -> Unit = {},
    onViewBrief: () -> Unit = {},
    viewModel: BriefingSettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsState()
    var confirmOff by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Briefing") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (config.sampleApprovedAt == 0L) {
                // Not set up yet.
                ListItem(
                    headlineContent = { Text("Set up Daily Briefing", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("A short brief of your day each morning, from your calendar. On-device.") },
                    leadingContent = { Icon(painterResource(R.drawable.ic_timer), null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickableRow(onSetup),
                )
            } else {
                if (!viewModel.systemNotificationsEnabled()) {
                    HintRow("Notifications are off", "Turn them on in system settings to receive your brief.")
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
                ListItem(
                    headlineContent = { Text("Daily briefing") },
                    supportingContent = {
                        val time = DateTimeFormatter.ofPattern("h:mm a").format(config.deliveryTime)
                        val days = config.days.sortedBy { it.value }.joinToString("") { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
                        Text(if (config.enabled) "On · $time · $days" else "Paused")
                    },
                    leadingContent = { Icon(painterResource(R.drawable.ic_timer), null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Switch(checked = config.enabled, onCheckedChange = { viewModel.setEnabled(it) }) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (!viewModel.canDeliverExactly()) {
                    HintRow("Delivery may vary by a few minutes", "Allow exact alarms in system settings for precise timing.")
                }
                ListItem(
                    headlineContent = { Text("View today's brief") },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickableRow(onViewBrief),
                )
                ListItem(
                    headlineContent = { Text("Reconfigure") },
                    supportingContent = { Text("Change sources, time, or days.") },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickableRow(onSetup),
                )
                ListItem(
                    headlineContent = { Text("Show details on lock screen") },
                    trailingContent = { Switch(checked = config.lockScreenDetail, onCheckedChange = { viewModel.setLockScreenDetail(it) }) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Turn off", color = MaterialTheme.colorScheme.error) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickableRow { confirmOff = true },
                )
            }
        }
    }

    if (confirmOff) {
        AlertDialog(
            onDismissRequest = { confirmOff = false },
            title = { Text("Turn off Daily Briefing?") },
            text = { Text("This cancels the schedule and clears your setup. You can set it up again anytime.") },
            confirmButton = { TextButton(onClick = { confirmOff = false; viewModel.turnOff() }) { Text("Turn off") } },
            dismissButton = { TextButton(onClick = { confirmOff = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HintRow(title: String, body: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(painterResource(R.drawable.ic_info), null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
