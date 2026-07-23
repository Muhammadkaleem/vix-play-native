package com.devbytes.vixplayer.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.devbytes.vixplayer.app.navigation.ABOUT_PRO_ROUTE
import com.devbytes.vixplayer.app.navigation.GESTURE_REMAP_ROUTE
import com.devbytes.vixplayer.app.navigation.PLAYBACK_DEFAULTS_ROUTE
import com.devbytes.vixplayer.app.navigation.PRIVATE_FOLDER_ROUTE
import com.devbytes.vixplayer.app.navigation.SUBTITLE_DEFAULTS_ROUTE
import com.devbytes.vixplayer.app.navigation.THEME_SKINS_ROUTE

private data class SettingsItem(
    val label: String,
    val description: String,
    val iconRes: Int,
    val route: String,
)

private val SETTINGS_ITEMS = listOf(
    SettingsItem("Gestures", "Remap taps and swipes, sensitivity, and haptics", R.drawable.ic_settings, GESTURE_REMAP_ROUTE),
    SettingsItem("Theme & Skins", "Base theme, accent color, and player skins", R.drawable.ic_eye, THEME_SKINS_ROUTE),
    SettingsItem("Subtitles", "Default language, styling, and encoding", R.drawable.ic_subtitles, SUBTITLE_DEFAULTS_ROUTE),
    SettingsItem("Playback", "Speed, decoder, resume, and autoplay", R.drawable.ic_speed, PLAYBACK_DEFAULTS_ROUTE),
    SettingsItem("Private Folder", "Lock videos behind a PIN", R.drawable.ic_lock, PRIVATE_FOLDER_ROUTE),
    SettingsItem("About / Pro", "Version, licenses, and app info", R.drawable.ic_info, ABOUT_PRO_ROUTE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            items(SETTINGS_ITEMS) { item ->
                ListItem(
                    leadingContent = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(item.label) },
                    supportingContent = {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    modifier = Modifier.clickable { onNavigate(item.route) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}
