package com.devbytes.vixplayer.app.ui.dashboard

/** A flat video list the dashboard's "View all" / Shorts entries open in the shared list screen. */
enum class DashboardSource(val title: String) {
    RECENTLY_ADDED("Recently added"),
    FAVORITES("Favorites"),
    HIGH_RES("HD & 4K"),
    NEW_ON_DEVICE("New on this device"),
}
