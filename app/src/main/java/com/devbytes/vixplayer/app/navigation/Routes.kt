package com.devbytes.vixplayer.app.navigation

// Start destination — owns permission check before handing off to the main graph
const val SPLASH_ROUTE = "splash"

// Tab graph routes (nested nav containers)
const val TAB_VIDEO_ROUTE    = "tab_video"
const val TAB_AUDIO_ROUTE    = "tab_audio"
const val TAB_NETWORK_ROUTE  = "tab_network"
const val TAB_SETTINGS_ROUTE = "tab_settings"

// Video tab destinations
const val LIBRARY_ROUTE        = "library"
const val FOLDER_BROWSER_ROUTE = "folder_browser?bucketId={bucketId}"
const val SEARCH_ROUTE         = "search?query={query}"
const val HISTORY_ROUTE        = "history"

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
const val ABOUT_PRO_ROUTE         = "about_pro"

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
