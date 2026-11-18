package com.devbytes.vixplayer.app.ads

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Holds the native ads of one screen, keyed by slot. Lives as long as the destination's back-stack
 * entry, so a card scrolled off-screen and back — or a recomposition — reuses its loaded ad instead
 * of firing a new request (the old per-composition loader was the main source of unshown natives).
 * Every ad is destroyed when the screen leaves the back stack.
 */
@HiltViewModel
class NativeAdsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: AdsController,
    private val events: AdEvents,
) : ViewModel() {

    private class Slot(val ad: NativeAd, val loadedAt: Long)

    private val slots = mutableStateMapOf<String, Slot>()
    private val loading = mutableSetOf<String>()
    private val failed = mutableSetOf<String>()

    /** The ad for [slotKey], or null while loading / unfilled. Observable from Compose. */
    fun ad(slotKey: String): NativeAd? = slots[slotKey]?.ad

    /**
     * Loads [slotKey] once. A slot that failed this visit isn't retried (no request loops); a
     * slot older than [MAX_AGE_MS] is refreshed so a long-lived screen doesn't show a stale ad.
     */
    fun request(slotKey: String, adUnitId: String, unitName: String) {
        if (!controller.adsAllowed.value) return
        if (slotKey in loading || slotKey in failed) return
        val existing = slots[slotKey]
        if (existing != null && SystemClock.elapsedRealtime() - existing.loadedAt < MAX_AGE_MS) return
        loading += slotKey
        val start = SystemClock.elapsedRealtime()
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                loading -= slotKey
                ad.setOnPaidEventListener { v -> events.paid(AdFormat.NATIVE, unitName, v, ad.responseInfo) }
                slots.put(slotKey, Slot(ad, SystemClock.elapsedRealtime()))?.ad?.destroy()
                evictOverflow()
                events.loaded(AdFormat.NATIVE, unitName, SystemClock.elapsedRealtime() - start)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading -= slotKey
                    failed += slotKey
                    events.loadFailed(AdFormat.NATIVE, unitName, error.code)
                }

                override fun onAdClicked() = events.clicked(AdFormat.NATIVE, unitName)
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    /** Keeps at most [MAX_SLOTS] ads alive (oldest destroyed first) as the user visits folders. */
    private fun evictOverflow() {
        while (slots.size > MAX_SLOTS) {
            val oldest = slots.minByOrNull { it.value.loadedAt } ?: return
            slots.remove(oldest.key)?.ad?.destroy()
        }
    }

    override fun onCleared() {
        slots.values.forEach { it.ad.destroy() }
        slots.clear()
    }

    private companion object {
        const val MAX_AGE_MS = 30 * 60_000L
        const val MAX_SLOTS = 6
    }
}
