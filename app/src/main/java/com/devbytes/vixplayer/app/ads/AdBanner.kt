package com.devbytes.vixplayer.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Anchored adaptive banner. Callers gate rendering on consent + `adsEnabled` + route, so
 * this only ever composes where a banner is allowed — never on the player surfaces.
 * [adUnitId] defaults to the app-chrome unit; pass another (e.g. the video-list unit) to
 * reuse the same widget elsewhere.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnits.bannerMain,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                this.adUnitId = adUnitId
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp)
                )
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
