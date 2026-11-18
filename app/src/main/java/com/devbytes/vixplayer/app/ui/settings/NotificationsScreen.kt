package com.devbytes.vixplayer.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.common.screenInsets

/**
 * Library Pulse controls: a master switch plus per-trigger toggles. All local, opt-out any
 * time, frequency-capped by the worker. When notifications are off at the system level the
 * screen says so rather than pretending the toggles do anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val settings by viewModel.pulseSettings.collectAsState()
    val systemEnabled = viewModel.systemNotificationsEnabled()

    Scaffold(
        contentWindowInsets = screenInsets(),
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
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
        Column(modifier = Modifier.padding(padding)) {
            if (!systemEnabled) {
                ListItem(
                    headlineContent = { Text("Notifications are off") },
                    supportingContent = {
                        Text("Turn them on in system settings to receive library reminders.")
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }

            ListItem(
                headlineContent = { Text("Library reminders") },
                supportingContent = {
                    Text("Occasional, on-device reminders. Never more than about twice a week.")
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_timer),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.master,
                        onCheckedChange = { viewModel.setMaster(it) },
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            PulseToggleRow(
                title = "Resume reminders",
                description = "Nudge to finish a video you started.",
                checked = settings.resume,
                enabled = settings.master,
                onChange = { viewModel.setResume(it) },
            )
            PulseToggleRow(
                title = "New videos",
                description = "Let you know when new videos are added.",
                checked = settings.newVideos,
                enabled = settings.master,
                onChange = { viewModel.setNewVideos(it) },
            )
            PulseToggleRow(
                title = "Weekly recap",
                description = "A quiet weekly summary of what you watched.",
                checked = settings.digest,
                enabled = settings.master,
                onChange = { viewModel.setDigest(it) },
            )
        }
    }
}

@Composable
private fun PulseToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Switch(checked = checked && enabled, enabled = enabled, onCheckedChange = onChange)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
