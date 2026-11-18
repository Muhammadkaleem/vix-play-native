package com.devbytes.vixplayer.app.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and shows the single rewarded ad that backs the Pro grant.
 *
 * One ad is kept warm at a time, preloaded only when a Pro offer is actually on screen (not on
 * every app open — those preloads mostly expired unwatched). [show] loads on demand if nothing
 * is warm and reports it through [loading] so the button can say so. The reward only fires after
 * the user actually finishes the ad (AdMob's `onUserEarnedReward`), so a dismiss part-way through
 * grants nothing — the caller decides what the reward means.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adsController: AdsController,
    private val frequency: AdFrequencyStore,
    private val events: AdEvents,
) {
    private val tag = "RewardedAd"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ad: RewardedAd? = null
    private var loadedAt = 0L
    private var preloading = false

    private val _loading = MutableStateFlow(false)
    /** True while the user is waiting for an on-demand load after tapping "Watch ad". */
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private fun warm(): RewardedAd? = ad?.takeIf { SystemClock.elapsedRealtime() - loadedAt < AD_TTL_MS }

    /** Pre-loads an ad so [show] is instant. Safe to call repeatedly. */
    fun preload() {
        if (warm() != null || preloading || !adsController.canRequestAds.value) return
        preloading = true
        load(
            onLoaded = { preloading = false },
            onFailed = { preloading = false },
        )
    }

    private fun load(onLoaded: (RewardedAd) -> Unit, onFailed: () -> Unit) {
        val start = SystemClock.elapsedRealtime()
        RewardedAd.load(
            context,
            AdUnits.rewarded,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loadedAt = SystemClock.elapsedRealtime()
                    loaded.setOnPaidEventListener { v ->
                        events.paid(AdFormat.REWARDED, "VixPlay_Rewarded_Pro", v, loaded.responseInfo)
                    }
                    events.loaded(AdFormat.REWARDED, "pro", loadedAt - start)
                    onLoaded(loaded)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(tag, "load failed: ${error.code} ${error.message}")
                    events.loadFailed(AdFormat.REWARDED, "pro", error.code)
                    ad = null
                    onFailed()
                }
            },
        )
    }

    /**
     * Shows a ready ad on [activity]. [onReward] fires only when the ad is fully watched;
     * [onUnavailable] fires if nothing could be loaded (no fill / no consent / offline), so
     * the caller can surface a "try again" instead of silently doing nothing.
     */
    fun show(
        activity: Activity,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
        entryPoint: String = "unknown",
    ) {
        if (_loading.value) return // already waiting on a load for an earlier tap
        warm()?.let { present(activity, it, onReward, onUnavailable, entryPoint); return }
        if (!adsController.canRequestAds.value) {
            onUnavailable(); return
        }
        _loading.value = true
        load(
            onLoaded = { loaded ->
                _loading.value = false
                if (activity.isFinishing || activity.isDestroyed) return@load // user left meanwhile
                present(activity, loaded, onReward, onUnavailable, entryPoint)
            },
            onFailed = {
                _loading.value = false
                onUnavailable()
            },
        )
    }

    private fun present(
        activity: Activity,
        rewardedAd: RewardedAd,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
        entryPoint: String,
    ) {
        ad = null
        var earned = false
        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                events.rewardedStart(entryPoint)
                // Starts the interstitial cooldown — no forced ad right after a chosen one.
                scope.launch { frequency.recordRewarded(System.currentTimeMillis()) }
            }

            override fun onAdClicked() = events.clicked(AdFormat.REWARDED, entryPoint)

            override fun onAdDismissedFullScreenContent() {
                if (earned) {
                    events.rewardedComplete(entryPoint)
                    onReward()
                } else {
                    events.rewardedAbandon(entryPoint)
                }
                preload() // keep the next one warm for "Extend"
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(tag, "show failed: ${error.message}")
                events.showFailed(AdFormat.REWARDED, error.code)
                onUnavailable()
                preload()
            }
        }
        rewardedAd.show(activity) { earned = true }
    }

    private companion object {
        /** Google expires a loaded rewarded ad after 1 h; drop ours a little earlier. */
        const val AD_TTL_MS = 55 * 60_000L
    }
}
