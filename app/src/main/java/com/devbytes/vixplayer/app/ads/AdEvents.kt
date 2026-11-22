package com.devbytes.vixplayer.app.ads

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Ad formats as reported in analytics (`ad_format` param). */
enum class AdFormat(val key: String) {
    BANNER("banner"), NATIVE("native"), INTERSTITIAL("interstitial"), REWARDED("rewarded"),
}

/**
 * The one place ad analytics are logged. `ad_impression` is Firebase's standard revenue event,
 * fed from each format's `OnPaidEventListener`, so ARPDAU / revenue per user show up in Firebase
 * without AdMob's own (empty) user columns. Collection follows the app-wide release-only switch.
 */
@Singleton
class AdEvents @Inject constructor(@ApplicationContext context: Context) {

    private val analytics: FirebaseAnalytics? =
        runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()

    /** Kept free of AdMob types so this class builds without the ad SDK (Amazon flavour). */
    fun paid(format: AdFormat, unitName: String, valueMicros: Long, currency: String, adSource: String?) = log(
        FirebaseAnalytics.Event.AD_IMPRESSION,
        FirebaseAnalytics.Param.AD_PLATFORM to "admob",
        FirebaseAnalytics.Param.AD_SOURCE to (adSource ?: "unknown"),
        FirebaseAnalytics.Param.AD_FORMAT to format.key,
        FirebaseAnalytics.Param.AD_UNIT_NAME to unitName,
        FirebaseAnalytics.Param.VALUE to valueMicros / 1_000_000.0,
        FirebaseAnalytics.Param.CURRENCY to currency,
    )

    fun loaded(format: AdFormat, placement: String, latencyMs: Long) =
        log("ad_load", "ad_format" to format.key, "placement" to placement, "latency_ms" to latencyMs)

    fun loadFailed(format: AdFormat, placement: String, code: Int) =
        log("ad_load_failed", "ad_format" to format.key, "placement" to placement, "error_code" to code.toLong())

    fun clicked(format: AdFormat, placement: String) =
        log("ad_click", "ad_format" to format.key, "placement" to placement)

    fun interstitialShown(placement: String) = log("interstitial_shown", "placement" to placement)

    fun interstitialSkipped(placement: String, reason: String) =
        log("interstitial_skipped", "placement" to placement, "reason" to reason)

    fun showFailed(format: AdFormat, code: Int) =
        log("ad_show_failed", "ad_format" to format.key, "error_code" to code.toLong())

    /** The post-interstitial "go ad-free" offer: `shown`, `accepted` or `dismissed`. */
    fun adFreePrompt(action: String) = log("ad_free_prompt", "action" to action)

    fun rewardedStart(entryPoint: String) = log("rewarded_start", "entry_point" to entryPoint)
    fun rewardedComplete(entryPoint: String) = log("rewarded_complete", "entry_point" to entryPoint)
    fun rewardedAbandon(entryPoint: String) = log("rewarded_abandon", "entry_point" to entryPoint)

    private fun log(event: String, vararg params: Pair<String, Any>) {
        val a = analytics ?: return
        runCatching {
            a.logEvent(event, Bundle().apply {
                params.forEach { (k, v) ->
                    when (v) {
                        is String -> putString(k, v)
                        is Long -> putLong(k, v)
                        is Double -> putDouble(k, v)
                        is Int -> putLong(k, v.toLong())
                    }
                }
            })
        }
    }
}
