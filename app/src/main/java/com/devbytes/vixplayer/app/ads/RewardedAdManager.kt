package com.devbytes.vixplayer.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and shows the single rewarded ad that backs the "Get 1 Day Pro" grant.
 *
 * One ad is kept warm at a time; [show] loads on demand if none is ready. The reward only
 * fires after the user actually finishes the ad (AdMob's `onUserEarnedReward`), so a dismiss
 * part-way through grants nothing — the caller decides what the reward means.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adsController: AdsController,
) {
    private val tag = "RewardedAd"
    private var ad: RewardedAd? = null
    private var loading = false

    /** Pre-loads an ad so [show] is instant. Safe to call repeatedly. */
    fun preload() {
        if (ad != null || loading || !adsController.canRequestAds.value) return
        loading = true
        RewardedAd.load(
            context,
            AdUnits.rewarded,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(tag, "load failed: ${error.message}")
                    ad = null
                    loading = false
                }
            },
        )
    }

    /**
     * Shows a ready ad on [activity]. [onReward] fires only when the ad is fully watched;
     * [onUnavailable] fires if nothing could be loaded (no fill / no consent / offline), so
     * the caller can surface a "try again" instead of silently doing nothing.
     */
    fun show(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ready = ad
        if (ready == null) {
            // Nothing warm — try a fresh load, then show when it arrives.
            if (!adsController.canRequestAds.value) {
                onUnavailable(); return
            }
            RewardedAd.load(
                context,
                AdUnits.rewarded,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(loaded: RewardedAd) {
                        present(activity, loaded, onReward)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(tag, "show-load failed: ${error.message}")
                        onUnavailable()
                    }
                },
            )
            return
        }
        present(activity, ready, onReward)
    }

    private fun present(activity: Activity, rewardedAd: RewardedAd, onReward: () -> Unit) {
        var earned = false
        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload() // keep the next one warm
                if (earned) onReward()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.w(tag, "show failed: ${error.message}")
                ad = null
            }
        }
        rewardedAd.show(activity) { earned = true }
    }
}
