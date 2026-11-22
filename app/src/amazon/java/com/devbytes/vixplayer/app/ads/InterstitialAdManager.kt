package com.devbytes.vixplayer.app.ads

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton

/** Amazon Appstore build: no interstitials. */
@Singleton
class InterstitialAdManager @Inject constructor() {
    fun onPlayerEntered() = Unit
    fun markExternalVisit() = Unit
    fun preloadIfEligible() = Unit
    fun onPlayerExit(activity: Activity) = Unit
}
