package com.devbytes.vixplayer.app.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devbytes.vixplayer.app.ads.AdBanner
import com.devbytes.vixplayer.app.ads.AdsViewModel
import com.devbytes.vixplayer.app.navigation.ABOUT_PRO_ROUTE
import com.devbytes.vixplayer.app.navigation.SPLASH_ROUTE
import com.devbytes.vixplayer.app.navigation.AUDIO_PLAYER_ROUTE
import com.devbytes.vixplayer.app.navigation.AUDIO_ROUTE
import com.devbytes.vixplayer.app.navigation.CAST_ROUTE
import com.devbytes.vixplayer.app.navigation.EQUALIZER_ROUTE
import com.devbytes.vixplayer.app.navigation.FOLDER_BROWSER_ROUTE
import com.devbytes.vixplayer.app.navigation.GESTURE_REMAP_ROUTE
import com.devbytes.vixplayer.app.navigation.HISTORY_ROUTE
import com.devbytes.vixplayer.app.navigation.NETWORK_ROUTE
import com.devbytes.vixplayer.app.navigation.NETWORK_STREAM_ROUTE
import com.devbytes.vixplayer.app.navigation.NOTIFICATIONS_ROUTE
import com.devbytes.vixplayer.app.navigation.PLAYBACK_DEFAULTS_ROUTE
import com.devbytes.vixplayer.app.navigation.PLAYER_ROUTE
import com.devbytes.vixplayer.app.navigation.PLAYLISTS_ROUTE
import com.devbytes.vixplayer.app.navigation.PLAYLIST_DETAIL_ROUTE
import com.devbytes.vixplayer.app.navigation.playlistDetailRoute
import com.devbytes.vixplayer.app.navigation.PRIVATE_FOLDER_ROUTE
import com.devbytes.vixplayer.app.navigation.SEARCH_ROUTE
import com.devbytes.vixplayer.app.navigation.SETTINGS_ROUTE
import com.devbytes.vixplayer.app.navigation.SMB_BROWSER_ROUTE
import com.devbytes.vixplayer.app.navigation.SUBTITLE_DEFAULTS_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_AUDIO_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_NETWORK_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_SETTINGS_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_VIDEO_ROUTE
import com.devbytes.vixplayer.app.navigation.THEME_SKINS_ROUTE
import com.devbytes.vixplayer.app.navigation.audioPlayerRoute
import com.devbytes.vixplayer.app.navigation.playerRoute
import com.devbytes.vixplayer.app.navigation.searchRoute
import com.devbytes.vixplayer.app.navigation.smbBrowserRoute
import androidx.compose.foundation.layout.Column
import com.devbytes.vixplayer.app.ui.audio.AudioLibraryScreen
import com.devbytes.vixplayer.app.ui.audio.MiniPlayer
import com.devbytes.vixplayer.app.ui.audio.AudioPlayerScreen
import com.devbytes.vixplayer.app.ui.audio.EqualizerScreen
import com.devbytes.vixplayer.app.ui.audio.PlaylistDetailScreen
import com.devbytes.vixplayer.app.ui.audio.PlaylistsScreen
import com.devbytes.vixplayer.app.ui.common.VixBottomNav
import com.devbytes.vixplayer.app.ui.library.FolderBrowserScreen
import com.devbytes.vixplayer.app.ui.library.HistoryScreen
import com.devbytes.vixplayer.app.ui.library.SearchScreen
import com.devbytes.vixplayer.app.ui.network.CastScreen
import com.devbytes.vixplayer.app.ui.network.NetworkScreen
import com.devbytes.vixplayer.app.ui.network.NetworkStreamScreen
import com.devbytes.vixplayer.app.ui.network.SmbBrowserScreen
import com.devbytes.vixplayer.app.ui.player.PlayerScreen
import com.devbytes.vixplayer.app.ui.settings.AboutProScreen
import com.devbytes.vixplayer.app.ui.settings.GestureRemapScreen
import com.devbytes.vixplayer.app.ui.settings.NotificationsScreen
import com.devbytes.vixplayer.app.ui.settings.PlaybackDefaultsScreen
import com.devbytes.vixplayer.app.ui.settings.PrivateFolderScreen
import com.devbytes.vixplayer.app.ui.settings.SettingsScreen
import com.devbytes.vixplayer.app.ui.settings.SubtitleDefaultsScreen
import com.devbytes.vixplayer.app.ui.settings.ThemeSkinsScreen
import com.devbytes.vixplayer.app.ui.splash.SplashScreen

/**
 * Sentinel for "expand whatever is already playing" — the mini-player has no track to
 * request, and `AudioPlayerViewModel.ensureQueued` ignores non-positive ids, so this
 * opens the now-playing screen without disturbing the queue.
 */
private const val NOW_PLAYING_ID = -1L

@Composable
fun VixPlayNavGraph(
    pendingExternalMedia: List<Uri> = emptyList(),
    onExternalMediaConsumed: () -> Unit = {},
    pulsePlayUri: String? = null,
    onPulsePlayConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val context = LocalContext.current

    // An external open jumps straight into the player, popping the splash gate — the intent
    // already granted read access, so no permission request is needed. Popping splash
    // inclusive makes the player the back-stack root, so Back finishes to the caller (see the
    // PLAYER_ROUTE onBack below). A warm open (onNewIntent) has no splash to pop, so Back
    // returns to wherever the user was.
    val externalUri = pendingExternalMedia.firstOrNull()?.toString()
    LaunchedEffect(externalUri) {
        if (externalUri != null) {
            navController.navigate(playerRoute(Uri.encode(externalUri))) {
                popUpTo(SPLASH_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
            onExternalMediaConsumed()
        }
    }

    // Resume-nudge tap: establish the Video home first, then push the player on top, so Back
    // returns to the library rather than exiting (unlike an external open). The video
    // auto-resumes from its saved position via the player's existing MediaStore-id path.
    LaunchedEffect(pulsePlayUri) {
        if (pulsePlayUri != null) {
            navController.navigate(TAB_VIDEO_ROUTE) {
                popUpTo(SPLASH_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
            navController.navigate(playerRoute(Uri.encode(pulsePlayUri)))
            onPulsePlayConsumed()
        }
    }

    val isFullScreen = currentRoute == PLAYER_ROUTE
            || currentRoute == AUDIO_PLAYER_ROUTE
            || currentRoute == SPLASH_ROUTE

    // "Library chrome" excludes Settings — no ads while configuring the app or reading
    // legal text. Players/splash are already excluded by isFullScreen.
    val inSettingsTab = currentDestination?.hierarchy?.any { it.route == TAB_SETTINGS_ROUTE } == true
    // The folder browser hosts its own inline native/banner ads, so the app-level banner
    // is suppressed there to avoid stacking two ads on one screen.
    val inFolderBrowser = currentRoute == FOLDER_BROWSER_ROUTE

    val adsViewModel: AdsViewModel = hiltViewModel()
    val canRequestAds by adsViewModel.canRequestAds.collectAsState()

    Scaffold(
        bottomBar = {
            if (!isFullScreen) {
                // Mini-player sits above the tabs and inside the same slot, so Scaffold
                // padding accounts for both and content is never hidden behind it.
                Column {
                    // Anchored banner on top (furthest from the nav thumb), only where
                    // ads are allowed and once consent + SDK are ready.
                    if (adsViewModel.adsEnabled && canRequestAds && !inSettingsTab && !inFolderBrowser) {
                        AdBanner()
                    }
                    MiniPlayer(
                        onExpand = {
                            navController.navigate(audioPlayerRoute(NOW_PLAYING_ID)) {
                                launchSingleTop = true
                            }
                        },
                    )
                    VixBottomNav(
                        currentDestination = currentDestination,
                        onNavigate = { tabRoute ->
                            navController.navigate(tabRoute) {
                                popUpTo(TAB_VIDEO_ROUTE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = SPLASH_ROUTE,
            modifier = if (isFullScreen) Modifier else Modifier.padding(padding),
        ) {

            // ── Splash / Permission gate ─────────────────────────────────────
            composable(SPLASH_ROUTE) {
                SplashScreen(
                    // Suppress the permission request + auto-navigate while an external open
                    // or a resume-nudge deep-link is in flight — the graph is about to route
                    // past splash to the player.
                    autoRequest = externalUri == null && pulsePlayUri == null,
                    onNavigateToLibrary = {
                        navController.navigate(TAB_VIDEO_ROUTE) {
                            popUpTo(SPLASH_ROUTE) { inclusive = true }
                        }
                    },
                )
            }

            // ── Top-level: Video Player ──────────────────────────────────────
            composable(
                route = PLAYER_ROUTE,
                arguments = listOf(navArgument("uri") { type = NavType.StringType }),
            ) { entry ->
                val encodedUri = entry.arguments?.getString("uri") ?: return@composable
                PlayerScreen(
                    uri = Uri.decode(encodedUri),
                    // Nothing left to pop ⇒ this was an external open and the player is the
                    // root, so finish and return to the launching app.
                    onBack = {
                        if (!navController.popBackStack()) (context as? Activity)?.finish()
                    },
                )
            }

            // ── Top-level: Audio Player ──────────────────────────────────────
            composable(
                route = AUDIO_PLAYER_ROUTE,
                arguments = listOf(navArgument("mediaStoreId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("mediaStoreId") ?: return@composable
                AudioPlayerScreen(
                    mediaStoreId = id,
                    onBack = { navController.popBackStack() },
                    onEqualizerClick = { navController.navigate(EQUALIZER_ROUTE) },
                )
            }

            // ── Tab: Video ───────────────────────────────────────────────────
            // Folder listing is the Video-tab home; Search and History are reached from it.
            navigation(startDestination = FOLDER_BROWSER_ROUTE, route = TAB_VIDEO_ROUTE) {

                composable(
                    route = FOLDER_BROWSER_ROUTE,
                    arguments = listOf(
                        navArgument("bucketId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    FolderBrowserScreen(
                        onVideoClick = { uri ->
                            navController.navigate(playerRoute(Uri.encode(uri.toString())))
                        },
                        onSearchClick = { navController.navigate(searchRoute()) },
                        onHistoryClick = { navController.navigate(HISTORY_ROUTE) },
                    )
                }

                composable(
                    route = SEARCH_ROUTE,
                    arguments = listOf(
                        navArgument("query") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    SearchScreen(
                        initialQuery = entry.arguments?.getString("query")?.let { Uri.decode(it) },
                        onVideoClick = { uri ->
                            navController.navigate(playerRoute(Uri.encode(uri.toString())))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(HISTORY_ROUTE) {
                    HistoryScreen(
                        onVideoClick = { uri ->
                            navController.navigate(playerRoute(Uri.encode(uri.toString())))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            // ── Tab: Audio ───────────────────────────────────────────────────
            navigation(startDestination = AUDIO_ROUTE, route = TAB_AUDIO_ROUTE) {

                composable(AUDIO_ROUTE) {
                    AudioLibraryScreen(
                        onAudioPlayerClick = { id ->
                            navController.navigate(audioPlayerRoute(id))
                        },
                        onPlaylistsClick = { navController.navigate(PLAYLISTS_ROUTE) },
                        onOpenPlaylist = { navController.navigate(playlistDetailRoute(it)) },
                        onEqualizerClick = { navController.navigate(EQUALIZER_ROUTE) },
                    )
                }

                composable(PLAYLISTS_ROUTE) {
                    PlaylistsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPlaylist = { navController.navigate(playlistDetailRoute(it)) },
                    )
                }

                composable(
                    route = PLAYLIST_DETAIL_ROUTE,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("playlistId") ?: return@composable
                    PlaylistDetailScreen(
                        playlistId = id,
                        onBack = { navController.popBackStack() },
                        onOpenPlayer = { navController.navigate(audioPlayerRoute(NOW_PLAYING_ID)) },
                    )
                }

                composable(EQUALIZER_ROUTE) {
                    EqualizerScreen(onBack = { navController.popBackStack() })
                }
            }

            // ── Tab: Network ─────────────────────────────────────────────────
            navigation(startDestination = NETWORK_ROUTE, route = TAB_NETWORK_ROUTE) {

                composable(NETWORK_ROUTE) {
                    NetworkScreen(
                        onStreamClick = { navController.navigate(NETWORK_STREAM_ROUTE) },
                        onSmbClick = { navController.navigate(smbBrowserRoute()) },
                        onCastClick = { navController.navigate(CAST_ROUTE) },
                    )
                }

                composable(NETWORK_STREAM_ROUTE) {
                    NetworkStreamScreen(
                        onPlayUrl = { uri ->
                            navController.navigate(playerRoute(Uri.encode(uri.toString())))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = SMB_BROWSER_ROUTE,
                    arguments = listOf(
                        navArgument("path") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    SmbBrowserScreen(
                        initialPath = entry.arguments?.getString("path")?.let { Uri.decode(it) },
                        onFileClick = { uri ->
                            navController.navigate(playerRoute(Uri.encode(uri.toString())))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(CAST_ROUTE) {
                    CastScreen(onBack = { navController.popBackStack() })
                }
            }

            // ── Tab: Settings ────────────────────────────────────────────────
            navigation(startDestination = SETTINGS_ROUTE, route = TAB_SETTINGS_ROUTE) {

                composable(SETTINGS_ROUTE) {
                    SettingsScreen(onNavigate = { route -> navController.navigate(route) })
                }

                composable(GESTURE_REMAP_ROUTE) {
                    GestureRemapScreen(onBack = { navController.popBackStack() })
                }

                composable(THEME_SKINS_ROUTE) {
                    ThemeSkinsScreen(onBack = { navController.popBackStack() })
                }

                composable(SUBTITLE_DEFAULTS_ROUTE) {
                    SubtitleDefaultsScreen(onBack = { navController.popBackStack() })
                }

                composable(PLAYBACK_DEFAULTS_ROUTE) {
                    PlaybackDefaultsScreen(onBack = { navController.popBackStack() })
                }

                composable(PRIVATE_FOLDER_ROUTE) {
                    PrivateFolderScreen(
                        onBack = { navController.popBackStack() },
                        onPlay = { uri ->
                            navController.navigate(playerRoute(Uri.encode(uri)))
                        },
                    )
                }

                composable(NOTIFICATIONS_ROUTE) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }

                composable(ABOUT_PRO_ROUTE) {
                    AboutProScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
