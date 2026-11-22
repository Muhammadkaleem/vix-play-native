package com.devbytes.vixplayer.app.ads

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Amazon Appstore build: no interstitials. */
@Singleton
class InterstitialAdManager @Inject constructor() {
    fun onPlayerEntered() = Unit
    fun markExternalVisit() = Unit
    fun preloadIfEligible() = Unit
    fun onPlayerExit(activity: Activity) = Unit
    val adFreeOffers: Flow<Unit> = emptyFlow()
}
