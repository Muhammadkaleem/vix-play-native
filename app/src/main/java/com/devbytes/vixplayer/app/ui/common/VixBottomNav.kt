package com.devbytes.vixplayer.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.navigation.TAB_AUDIO_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_NETWORK_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_SETTINGS_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_VIDEO_ROUTE

private data class NavItem(val tabRoute: String, val iconRes: Int, val label: String)

private val NAV_ITEMS = listOf(
    NavItem(TAB_VIDEO_ROUTE,    R.drawable.ic_nav_video,   "Video"),
    NavItem(TAB_AUDIO_ROUTE,    R.drawable.ic_nav_audio,   "Audio"),
    NavItem(TAB_NETWORK_ROUTE,  R.drawable.ic_nav_network, "Network"),
    NavItem(TAB_SETTINGS_ROUTE, R.drawable.ic_settings,    "Settings"),
)

@Composable
fun VixBottomNav(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NAV_ITEMS.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.tabRoute } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.tabRoute) },
                icon = {
                    AnimatedIcon(
                        painter = painterResource(item.iconRes),
                        selected = selected,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(item.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
