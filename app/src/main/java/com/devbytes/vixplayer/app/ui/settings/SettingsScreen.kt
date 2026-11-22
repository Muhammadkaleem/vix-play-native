package com.devbytes.vixplayer.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.ads.rememberAdsSupported
import com.devbytes.vixplayer.app.ui.common.LocalIsTv
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ads.AdsViewModel
import com.devbytes.vixplayer.app.ui.premium.findActivity
import com.devbytes.vixplayer.app.navigation.ABOUT_PRO_ROUTE
import com.devbytes.vixplayer.app.navigation.BRIEFING_SETTINGS_ROUTE
import com.devbytes.vixplayer.app.navigation.GESTURE_REMAP_ROUTE
import com.devbytes.vixplayer.app.navigation.NOTIFICATIONS_ROUTE
import com.devbytes.vixplayer.app.navigation.PRO_REWARDS_ROUTE
import com.devbytes.vixplayer.app.navigation.PLAYBACK_DEFAULTS_ROUTE
import com.devbytes.vixplayer.app.navigation.PRIVATE_FOLDER_ROUTE
import com.devbytes.vixplayer.app.navigation.SUBTITLE_DEFAULTS_ROUTE
import com.devbytes.vixplayer.app.navigation.THEME_SKINS_ROUTE
import com.devbytes.vixplayer.app.ui.common.screenInsets

/**
 * Opens the system "Open by default" screen for VixPlay (API 31+), where the user manages
 * which file types the app handles; falls back to the app-details screen below 31 and if the
 * dedicated screen is unavailable on an OEM build. Never throws into the UI.
 */
private fun openDefaultAppSettings(context: Context) {
    val pkgUri = Uri.fromParts("package", context.packageName, null)
    val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, pkgUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
    }
    runCatching { context.startActivity(primary) }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri))
        }
    }
}

private data class SettingsItem(
    val label: String,
    val description: String,
    val iconRes: Int,
    val route: String,
)

private fun settingsItems(adsSupported: Boolean, isTv: Boolean) = listOfNotNull(
    // Pro only removes ads, so where there are none (the Amazon build, TVs) it isn't offered.
    SettingsItem("Vix Pro Rewards", "Watch ads to earn Pro time, status, and history", R.drawable.ic_crown, PRO_REWARDS_ROUTE)
        .takeIf { adsSupported },
    // Touch gestures; a TV is driven by its remote.
    SettingsItem("Gestures", "Remap taps and swipes, sensitivity, and haptics", R.drawable.ic_settings, GESTURE_REMAP_ROUTE)
        .takeIf { !isTv },
    SettingsItem("Theme & Skins", "Base theme, accent color, and player skins", R.drawable.ic_eye, THEME_SKINS_ROUTE),
    SettingsItem("Subtitles", "Default language, styling, and encoding", R.drawable.ic_subtitles, SUBTITLE_DEFAULTS_ROUTE),
    SettingsItem("Playback", "Speed, decoder, resume, and autoplay", R.drawable.ic_speed, PLAYBACK_DEFAULTS_ROUTE),
    SettingsItem("Private Folder", "Lock videos behind a PIN", R.drawable.ic_lock, PRIVATE_FOLDER_ROUTE),
    SettingsItem("Notifications", "Resume reminders, new videos, weekly recap", R.drawable.ic_timer, NOTIFICATIONS_ROUTE),
    SettingsItem("Daily Briefing", "A short brief of your day each morning", R.drawable.ic_timer, BRIEFING_SETTINGS_ROUTE),
    SettingsItem(if (adsSupported) "About / Pro" else "About", "Version, licenses, and app info", R.drawable.ic_info, ABOUT_PRO_ROUTE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit = {},
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val privacyOptionsRequired by adsViewModel.privacyOptionsRequired.collectAsStateWithLifecycle()
    val adsSupported = rememberAdsSupported()
    val isTv = LocalIsTv.current
    val settingsItems = remember(adsSupported, isTv) { settingsItems(adsSupported, isTv) }
    Scaffold(
        contentWindowInsets = screenInsets(),
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
            // Not a nav route — deep-links to the OS "Open by default" screen so the user
            // can make VixPlay the default handler for video/audio file types. Android has
            // no programmatic default-media-player role, so this is the real mechanism.
            item {
                ListItem(
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text("Set as default player") },
                    supportingContent = {
                        Text(
                            text = "Choose which video and audio files open in VixPlay",
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
                    modifier = Modifier.clickable { openDefaultAppSettings(context) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            }

            // Consent regions only: re-open the ad-consent form (Google's privacy-options entry
            // point). Elsewhere there is no consent to change, so the row isn't shown.
            if (privacyOptionsRequired) {
                item {
                    ListItem(
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        headlineContent = { Text("Privacy & ad choices") },
                        supportingContent = {
                            Text(
                                text = "Change your consent for personalised ads",
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
                        modifier = Modifier.clickable {
                            context.findActivity()?.let { adsViewModel.showPrivacyOptions(it) }
                        },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }

            items(settingsItems) { item ->
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
