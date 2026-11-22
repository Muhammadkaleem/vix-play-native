package com.devbytes.vixplayer.app

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/** Android TV, Google TV and Fire TV: driven by a remote, no touchscreen. */
object TvDevice {
    private const val FEATURE_FIRE_TV = "amazon.hardware.fire_tv"

    fun isTv(context: Context): Boolean {
        val uiMode = context.getSystemService(UiModeManager::class.java)
        if (uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || pm.hasSystemFeature(FEATURE_FIRE_TV)
    }
}
