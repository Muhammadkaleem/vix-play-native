package com.devbytes.vixplayer.app.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
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
import com.devbytes.vixplayer.app.navigation.BRIEFING_ROUTE
import com.devbytes.vixplayer.app.navigation.BRIEFING_SETTINGS_ROUTE
import com.devbytes.vixplayer.app.navigation.BRIEFING_SETUP_ROUTE
import com.devbytes.vixplayer.app.navigation.SPLASH_ROUTE
import com.devbytes.vixplayer.app.navigation.AUDIO_PLAYER_ROUTE
import com.devbytes.vixplayer.app.navigation.AUDIO_ROUTE
import com.devbytes.vixplayer.app.navigation.CAST_ROUTE
import com.devbytes.vixplayer.app.navigation.EQUALIZER_ROUTE
import com.devbytes.vixplayer.app.navigation.FOLDER_BROWSER_ROUTE
import com.devbytes.vixplayer.app.navigation.GESTURE_REMAP_ROUTE
import com.devbytes.vixplayer.app.navigation.HISTORY_ROUTE
import com.devbytes.vixplayer.app.navigation.HOME_ROUTE
import com.devbytes.vixplayer.app.navigation.TAB_HOME_ROUTE
import com.devbytes.vixplayer.app.navigation.VIDEO_LIST_ROUTE
import com.devbytes.vixplayer.app.navigation.videoListRoute
import com.devbytes.vixplayer.app.navigation.FOLDER_VIDEOS_ROUTE
import com.devbytes.vixplayer.app.navigation.folderVideosRoute
import com.devbytes.vixplayer.app.navigation.SHORTS_FEED_ROUTE
import com.devbytes.vixplayer.app.ui.dashboard.ShortsFeedScreen
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
import com.devbytes.vixplayer.app.navigation.TRASH_ROUTE
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
import com.devbytes.vixplayer.app.briefing.ui.BriefingScreen
import com.devbytes.vixplayer.app.briefing.ui.BriefingSettingsScreen
import com.devbytes.vixplayer.app.briefing.ui.SetupWizardScreen
import com.devbytes.vixplayer.app.ui.audio.AudioLibraryScreen
import com.devbytes.vixplayer.app.ui.dashboard.DashboardScreen
import com.devbytes.vixplayer.app.ui.dashboard.DashboardSource
import com.devbytes.vixplayer.app.ui.dashboard.VideoListScreen
import com.devbytes.vixplayer.app.ui.audio.MiniPlayer
import com.devbytes.vixplayer.app.ui.audio.AudioPlayerScreen
import com.devbytes.vixplayer.app.ui.audio.EqualizerScreen
import com.devbytes.vixplayer.app.ui.audio.PlaylistDetailScreen
import com.devbytes.vixplayer.app.ui.audio.PlaylistsScreen
import com.devbytes.vixplayer.app.ui.common.VixBottomNav
import com.devbytes.vixplayer.app.ui.library.FolderBrowserScreen
import com.devbytes.vixplayer.app.ui.library.HistoryScreen
import com.devbytes.vixplayer.app.ui.library.SearchScreen
import com.devbytes.vixplayer.app.ui.library.TrashScreen
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

// ── Navigation motion system ─────────────────────────────────────────────────
// A single classifier picks one of three motions per transition (see classifyMotion),
// so all ~35 destinations share one coherent language with nothing configured per-screen.
// Durations/easings live here as the one place to retune on-device.

private const val DUR_SLIDE = 300           // forward / back shared-axis
private const val DUR_MODAL = 300           // full-screen fade + scale
private const val FADE_THROUGH_OUT = 90     // tab switch: outgoing fade
private const val FADE_THROUGH_IN = 180     // tab switch: incoming fade (delayed by _OUT)

/** Material 3 emphasized easings — the "settles into place" quality that reads as designed. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/** Full-screen surfaces get the modal fade+scale (not a lateral page motion). */
private val FULLSCREEN_ROUTES = setOf(
    PLAYER_ROUTE, AUDIO_PLAYER_ROUTE, SHORTS_FEED_ROUTE, SPLASH_ROUTE,
    BRIEFING_ROUTE, BRIEFING_SETUP_ROUTE, BRIEFING_SETTINGS_ROUTE,
)

private enum class NavMotion { FORWARD, TAB, FULLSCREEN }

/**
 * Classifies a transition by the relationship between the two destinations — not by which
 * screen it is — so a tab switch that *restores a deep sub-screen* is still detected as a
 * tab switch (cross-graph), resolving the saveState/restoreState vs forward-slide conflict.
 */
private fun classifyMotion(initial: NavBackStackEntry, target: NavBackStackEntry): NavMotion {
    val ir = initial.destination.route
    val tr = target.destination.route
    if (ir in FULLSCREEN_ROUTES || tr in FULLSCREEN_ROUTES) return NavMotion.FULLSCREEN
    // Peer tabs live in different nested graphs (tab_home / tab_video / …).
    return if (initial.destination.parent?.route != target.destination.parent?.route)
        NavMotion.TAB else NavMotion.FORWARD
}

private fun enterFor(motion: NavMotion, slidePx: Int): EnterTransition = when (motion) {
    NavMotion.FULLSCREEN -> fadeIn(tween(DUR_MODAL, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(DUR_MODAL, easing = EmphasizedDecelerate))
    NavMotion.TAB -> fadeIn(tween(FADE_THROUGH_IN, delayMillis = FADE_THROUGH_OUT, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(FADE_THROUGH_IN, delayMillis = FADE_THROUGH_OUT, easing = EmphasizedDecelerate))
    NavMotion.FORWARD -> fadeIn(tween(DUR_SLIDE, easing = FastOutSlowInEasing)) +
        slideInHorizontally(tween(DUR_SLIDE, easing = EmphasizedDecelerate)) { slidePx }
}

private fun exitFor(motion: NavMotion, slidePx: Int): ExitTransition = when (motion) {
    NavMotion.FULLSCREEN -> fadeOut(tween(DUR_MODAL, easing = FastOutSlowInEasing)) +
        scaleOut(targetScale = 0.92f, animationSpec = tween(DUR_MODAL, easing = EmphasizedAccelerate))
    NavMotion.TAB -> fadeOut(tween(FADE_THROUGH_OUT, easing = FastOutSlowInEasing))
    NavMotion.FORWARD -> fadeOut(tween(DUR_SLIDE, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween(DUR_SLIDE, easing = EmphasizedAccelerate)) { -slidePx }
}

private fun popEnterFor(motion: NavMotion, slidePx: Int): EnterTransition = when (motion) {
    NavMotion.FULLSCREEN -> fadeIn(tween(DUR_MODAL, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(DUR_MODAL, easing = EmphasizedDecelerate))
    NavMotion.TAB -> fadeIn(tween(FADE_THROUGH_IN, delayMillis = FADE_THROUGH_OUT, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(FADE_THROUGH_IN, delayMillis = FADE_THROUGH_OUT, easing = EmphasizedDecelerate))
    // Returning screen slides back in from the left.
    NavMotion.FORWARD -> fadeIn(tween(DUR_SLIDE, easing = FastOutSlowInEasing)) +
        slideInHorizontally(tween(DUR_SLIDE, easing = EmphasizedDecelerate)) { -slidePx }
}

private fun popExitFor(motion: NavMotion, slidePx: Int): ExitTransition = when (motion) {
    NavMotion.FULLSCREEN -> fadeOut(tween(DUR_MODAL, easing = FastOutSlowInEasing)) +
        scaleOut(targetScale = 0.92f, animationSpec = tween(DUR_MODAL, easing = EmphasizedAccelerate))
    NavMotion.TAB -> fadeOut(tween(FADE_THROUGH_OUT, easing = FastOutSlowInEasing))
    // Current screen exits to the right on back.
    NavMotion.FORWARD -> fadeOut(tween(DUR_SLIDE, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween(DUR_SLIDE, easing = EmphasizedAccelerate)) { slidePx }
}

/** Switches bottom-nav tabs the same way VixBottomNav does (save/restore per-tab state). */
private fun switchTab(navController: androidx.navigation.NavController, tabRoute: String) {
    navController.navigate(tabRoute) {
        popUpTo(TAB_HOME_ROUTE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun VixPlayNavGraph(
    pendingExternalMedia: List<Uri> = emptyList(),
    onExternalMediaConsumed: () -> Unit = {},
    pulsePlayUri: String? = null,
    onPulsePlayConsumed: () -> Unit = {},
    openBriefing: Boolean = false,
    onBriefingConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val context = LocalContext.current

    // Crash breadcrumb: which screen the user was on when a crash hit.
    LaunchedEffect(currentRoute) {
        FirebaseCrashlytics.getInstance().setCustomKey("route", currentRoute ?: "none")
    }

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
            navController.navigate(TAB_HOME_ROUTE) {
                popUpTo(SPLASH_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
            navController.navigate(playerRoute(Uri.encode(pulsePlayUri)))
            onPulsePlayConsumed()
        }
    }

    // Briefing notification tap: land on the brief with the library home beneath (Back → home).
    LaunchedEffect(openBriefing) {
        if (openBriefing) {
            navController.navigate(TAB_HOME_ROUTE) {
                popUpTo(SPLASH_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
            navController.navigate(BRIEFING_ROUTE)
            onBriefingConsumed()
        }
    }

    val isFullScreen = currentRoute == PLAYER_ROUTE
            || currentRoute == AUDIO_PLAYER_ROUTE
            || currentRoute == SHORTS_FEED_ROUTE
            || currentRoute == SPLASH_ROUTE
            || currentRoute == BRIEFING_ROUTE
            || currentRoute == BRIEFING_SETUP_ROUTE
            || currentRoute == BRIEFING_SETTINGS_ROUTE

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
                                popUpTo(TAB_HOME_ROUTE) { saveState = true }
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

        // 30dp shared-axis offset in px (needs density; computed once per recomposition).
        val slidePx = with(LocalDensity.current) { 30.dp.roundToPx() }

        NavHost(
            navController = navController,
            startDestination = SPLASH_ROUTE,
            modifier = if (isFullScreen) Modifier else Modifier.padding(padding),
            enterTransition = { enterFor(classifyMotion(initialState, targetState), slidePx) },
            exitTransition = { exitFor(classifyMotion(initialState, targetState), slidePx) },
            popEnterTransition = { popEnterFor(classifyMotion(initialState, targetState), slidePx) },
            popExitTransition = { popExitFor(classifyMotion(initialState, targetState), slidePx) },
        ) {

            // ── Splash / Permission gate ─────────────────────────────────────
            composable(SPLASH_ROUTE) {
                SplashScreen(
                    // Suppress the permission request + auto-navigate while an external open
                    // or a resume-nudge deep-link is in flight — the graph is about to route
                    // past splash to the player.
                    autoRequest = externalUri == null && pulsePlayUri == null && !openBriefing,
                    onNavigateToLibrary = {
                        navController.navigate(TAB_HOME_ROUTE) {
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

            // ── Top-level: Shorts feed (TikTok-style vertical pager) ─────────
            composable(SHORTS_FEED_ROUTE) {
                ShortsFeedScreen(onBack = { navController.popBackStack() })
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

            // ── Daily Briefing (full-screen; reached from Settings / notification / home card) ──
            composable(BRIEFING_ROUTE) {
                BriefingScreen(onBack = { navController.popBackStack() })
            }
            composable(BRIEFING_SETUP_ROUTE) {
                SetupWizardScreen(
                    onExit = { navController.popBackStack() },
                    onFinished = { navController.popBackStack() },
                )
            }
            composable(BRIEFING_SETTINGS_ROUTE) {
                BriefingSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSetup = { navController.navigate(BRIEFING_SETUP_ROUTE) },
                    onViewBrief = { navController.navigate(BRIEFING_ROUTE) },
                )
            }

            // ── Tab: Home (dashboard) ────────────────────────────────────────
            navigation(startDestination = HOME_ROUTE, route = TAB_HOME_ROUTE) {

                composable(HOME_ROUTE) {
                    DashboardScreen(
                        onVideoClick = { video ->
                            navController.navigate(playerRoute(Uri.encode(video.uri.toString())))
                        },
                        // Tap a folder tile → its video list; "Browse all" → the Videos tab.
                        onFolderClick = { bucketId, name ->
                            navController.navigate(folderVideosRoute(bucketId, Uri.encode(name)))
                        },
                        onBrowseFolders = { switchTab(navController, TAB_VIDEO_ROUTE) },
                        onViewAll = { source -> navController.navigate(videoListRoute(source.name)) },
                        onOpenShorts = { navController.navigate(SHORTS_FEED_ROUTE) },
                        onSearch = { navController.navigate(searchRoute()) },
                    )
                }

                composable(
                    route = VIDEO_LIST_ROUTE,
                    arguments = listOf(navArgument("source") { type = NavType.StringType }),
                ) { entry ->
                    val source = entry.arguments?.getString("source")
                        ?.let { runCatching { DashboardSource.valueOf(it) }.getOrNull() }
                        ?: DashboardSource.RECENTLY_ADDED
                    VideoListScreen(
                        source = source,
                        onVideoClick = { video ->
                            navController.navigate(playerRoute(Uri.encode(video.uri.toString())))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = FOLDER_VIDEOS_ROUTE,
                    arguments = listOf(
                        navArgument("bucketId") { type = NavType.LongType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val bucketId = entry.arguments?.getLong("bucketId") ?: return@composable
                    val title = entry.arguments?.getString("title")?.let { Uri.decode(it) }.orEmpty()
                    VideoListScreen(
                        folderBucketId = bucketId,
                        folderTitle = title,
                        onVideoClick = { video ->
                            navController.navigate(playerRoute(Uri.encode(video.uri.toString())))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
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
                        onTrashClick = { navController.navigate(TRASH_ROUTE) },
                        onOpenBriefing = { navController.navigate(BRIEFING_ROUTE) },
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

                composable(TRASH_ROUTE) {
                    TrashScreen(onBack = { navController.popBackStack() })
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
