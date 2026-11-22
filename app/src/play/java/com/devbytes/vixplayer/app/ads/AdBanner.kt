package com.devbytes.vixplayer.app.ads

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AdEventsEntryPoint {
    fun adEvents(): AdEvents
}

/**
 * Anchored adaptive banner. Callers gate rendering on consent + `adsEnabled` + route, so
 * this only ever composes where a banner is allowed — never on the player surfaces.
 * [adUnitId] defaults to the app-chrome unit; pass another (e.g. the video-list unit) to
 * reuse the same widget elsewhere.
 *
 * - The banner's height is reserved from the first frame, so content never jumps when it fills.
 * - [visible] = false collapses it to zero height and pauses it (no refresh, no request) while
 *   keeping the loaded ad, so a host that hides it on some routes doesn't re-request on return.
 *   The first request waits until it is visible.
 * - The `AdView` is paused/resumed with the lifecycle and destroyed when it leaves composition.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnits.bannerMain,
    unitName: String = "VixPlay_Banner_Main",
    visible: Boolean = true,
) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    val events = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, AdEventsEntryPoint::class.java).adEvents()
    }
    val adView = remember(adUnitId, adSize) {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)
        }
    }
    val state = remember(adView) { BannerState() }

    DisposableEffect(adView) {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() =
                events.loaded(AdFormat.BANNER, unitName, SystemClock.elapsedRealtime() - state.requestedAt)
            override fun onAdFailedToLoad(error: LoadAdError) =
                events.loadFailed(AdFormat.BANNER, unitName, error.code)
            override fun onAdClicked() = events.clicked(AdFormat.BANNER, unitName)
        }
        adView.setOnPaidEventListener { v -> events.paid(AdFormat.BANNER, unitName, v, adView.responseInfo) }
        onDispose { adView.destroy() }
    }

    // Load on first visibility; pause while hidden so a collapsed banner never refreshes.
    LaunchedEffect(adView, visible) {
        state.visible = visible
        if (visible) {
            if (!state.requested) {
                state.requested = true
                state.requestedAt = SystemClock.elapsedRealtime()
                adView.loadAd(AdRequest.Builder().build())
            } else {
                adView.resume()
            }
        } else if (state.requested) {
            adView.pause()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> if (state.requested && state.visible) adView.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(if (visible) adSize.height.dp else 0.dp),
        factory = { adView },
    )
}

private class BannerState {
    var requested = false
    var requestedAt = 0L
    var visible = false
}
