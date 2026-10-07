package com.devbytes.vixplayer.app.navigation

// Start destination — owns permission check before handing off to the main graph
const val SPLASH_ROUTE = "splash"

// Tab graph routes (nested nav containers)
const val TAB_HOME_ROUTE     = "tab_home"
const val TAB_VIDEO_ROUTE    = "tab_video"
const val TAB_AUDIO_ROUTE    = "tab_audio"
const val TAB_NETWORK_ROUTE  = "tab_network"
const val TAB_SETTINGS_ROUTE = "tab_settings"

// Home tab destinations
const val HOME_ROUTE       = "home"
const val VIDEO_LIST_ROUTE = "video_list/{source}"
fun videoListRoute(source: String) = "video_list/$source"

// Videos of one folder (dashboard "Your folders" tap)
const val FOLDER_VIDEOS_ROUTE = "folder_videos/{bucketId}/{title}"
fun folderVideosRoute(bucketId: Long, encodedTitle: String) = "folder_videos/$bucketId/$encodedTitle"

// TikTok-style vertical Shorts feed
const val SHORTS_FEED_ROUTE = "shorts_feed"

// Video tab destinations
const val LIBRARY_ROUTE        = "library"
const val FOLDER_BROWSER_ROUTE = "folder_browser?bucketId={bucketId}"
const val SEARCH_ROUTE         = "search?query={query}"
const val HISTORY_ROUTE        = "history"
const val TRASH_ROUTE          = "trash"

// Audio tab destinations
const val AUDIO_ROUTE     = "audio"
const val PLAYLISTS_ROUTE = "playlists"
const val PLAYLIST_DETAIL_ROUTE = "playlists/{playlistId}"

fun playlistDetailRoute(playlistId: Long) = "playlists/$playlistId"
const val EQUALIZER_ROUTE = "equalizer"

// Network tab destinations
const val NETWORK_ROUTE        = "network"
const val NETWORK_STREAM_ROUTE = "network_stream"
const val SMB_BROWSER_ROUTE    = "smb_browser?path={path}"
const val CAST_ROUTE           = "cast"

// Settings tab destinations
const val SETTINGS_ROUTE          = "settings"
const val GESTURE_REMAP_ROUTE     = "gesture_remap"
const val THEME_SKINS_ROUTE       = "theme_skins"
const val SUBTITLE_DEFAULTS_ROUTE = "subtitle_defaults"
const val PLAYBACK_DEFAULTS_ROUTE = "playback_defaults"
const val PRIVATE_FOLDER_ROUTE    = "private_folder"
const val NOTIFICATIONS_ROUTE     = "notifications"
const val ABOUT_PRO_ROUTE         = "about_pro"

// Daily Briefing destinations (full-screen; reached from Settings, notification, home card)
const val BRIEFING_ROUTE          = "briefing"
const val BRIEFING_SETUP_ROUTE    = "briefing/setup"
const val BRIEFING_SETTINGS_ROUTE = "briefing/settings"

// Top-level destinations (full-screen, no bottom nav)
const val PLAYER_ROUTE       = "player/{uri}"
const val AUDIO_PLAYER_ROUTE = "audio_player/{mediaStoreId}"

fun playerRoute(encodedUri: String) = "player/$encodedUri"
fun audioPlayerRoute(mediaStoreId: Long) = "audio_player/$mediaStoreId"

fun folderBrowserRoute(bucketId: Long? = null) =
    if (bucketId != null) "folder_browser?bucketId=$bucketId" else "folder_browser"

fun searchRoute(encodedQuery: String? = null) =
    if (encodedQuery != null) "search?query=$encodedQuery" else "search"

fun smbBrowserRoute(encodedPath: String? = null) =
    if (encodedPath != null) "smb_browser?path=$encodedPath" else "smb_browser"
