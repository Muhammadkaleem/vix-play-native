package com.devbytes.vixplayer.app.ui.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.devbytes.vixplayer.app.R

/**
 * Network tab hub. "Stream by URL" is live; SMB shares and Cast are shown as honest
 * "coming soon" rows (their own build slices) rather than hidden, matching how the rest
 * of the app stubs not-yet-built destinations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onStreamClick: () -> Unit = {},
    onSmbClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                NetworkHubRow(
                    iconRes = R.drawable.ic_nav_network,
                    title = "Stream by URL",
                    description = "Play an HTTP, HLS, DASH, or RTSP link",
                    onClick = onStreamClick,
                )
                HubDivider()
            }
            item {
                NetworkHubRow(
                    iconRes = R.drawable.ic_folder,
                    title = "Network shares",
                    description = "Browse SMB/FTP servers",
                    comingSoon = true,
                )
                HubDivider()
            }
            item {
                NetworkHubRow(
                    iconRes = R.drawable.ic_cast,
                    title = "Cast to device",
                    description = "Send playback to a TV or Chromecast",
                    comingSoon = true,
                )
                HubDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkHubRow(
    iconRes: Int,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    comingSoon: Boolean = false,
) {
    val enabled = onClick != null && !comingSoon
    val contentTint =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = { Text(title, color = contentTint) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            if (comingSoon) {
                Text(
                    text = "Coming soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        modifier = if (enabled) Modifier.clickable { onClick!!() } else Modifier,
    )
}

@Composable
private fun HubDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    )
}
