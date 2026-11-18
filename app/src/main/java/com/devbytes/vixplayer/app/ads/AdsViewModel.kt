package com.devbytes.vixplayer.app.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes ad readiness and the interstitial hooks to the Compose tree. */
@HiltViewModel
class AdsViewModel @Inject constructor(
    controller: AdsController,
    private val interstitials: InterstitialAdManager,
    private val config: AdConfigProvider,
) : ViewModel() {
    /**
     * Ads are shown only when the user is NOT currently Pro (and not remotely disabled).
     * A rewarded-ad grant flips Pro on, which drops this to false live — every banner/native
     * host reads it, so ads vanish at once (and return at expiry).
     */
    val adsEnabled: StateFlow<Boolean> = controller.adsEnabled

    val canRequestAds: StateFlow<Boolean> = controller.canRequestAds

    fun adConfig(): AdConfig = config.current()

    /** The video player route was entered. */
    fun onPlayerEntered() = interstitials.onPlayerEntered()

    /** The next player visit was opened by another app (its exit must not show an ad). */
    fun markExternalPlayerVisit() = interstitials.markExternalVisit()

    /** The video player route was left for a library screen — a natural break. */
    fun onPlayerExited(activity: Activity) = interstitials.onPlayerExit(activity)

    /** Warm an interstitial while browsing, so a later break never waits. */
    fun preloadInterstitial() = interstitials.preloadIfEligible()
}
