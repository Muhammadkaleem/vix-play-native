package com.devbytes.vixplayer.app.ads

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** Theme colours captured on the Compose side and handed to the plain-View ad layout. */
private class AdColors(
    val surface: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
    val onPrimary: Int,
)

/**
 * Inline native ad card, styled to the app so it reads as content with a clear "Ad" badge.
 * Loads its own [NativeAd]; renders nothing until one arrives (so a failed load leaves no
 * empty gap). Callers gate on consent + `adsEnabled`.
 */
@Composable
fun AdNativeCard(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    val colors = AdColors(
        surface = MaterialTheme.colorScheme.surfaceVariant.toArgb(),
        onSurface = MaterialTheme.colorScheme.onSurface.toArgb(),
        onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        primary = MaterialTheme.colorScheme.primary.toArgb(),
        onPrimary = MaterialTheme.colorScheme.onPrimary.toArgb(),
    )

    DisposableEffect(adUnitId) {
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {})
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    nativeAd?.let { ad ->
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx -> buildNativeAdView(ctx, colors) },
            update = { view -> bindNativeAd(view as NativeAdView, ad) },
        )
    }
}

private fun buildNativeAdView(ctx: Context, colors: AdColors): NativeAdView {
    val d = ctx.resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()

    val adView = NativeAdView(ctx)

    val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(colors.surface)
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    val header = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val badge = TextView(ctx).apply {
        text = "Ad"
        setTextColor(colors.onPrimary)
        textSize = 10f
        setPadding(dp(6), dp(1), dp(6), dp(1))
        background = GradientDrawable().apply {
            cornerRadius = dp(3).toFloat()
            setColor(colors.primary)
        }
    }
    val headline = TextView(ctx).apply {
        setTextColor(colors.onSurface)
        textSize = 15f
        maxLines = 1
        setPadding(dp(8), 0, 0, 0)
    }
    header.addView(badge, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    header.addView(headline, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

    val media = MediaView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(140)).apply { topMargin = dp(8) }
    }

    val body = TextView(ctx).apply {
        setTextColor(colors.onSurfaceVariant)
        textSize = 13f
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) }
    }

    val cta = Button(ctx).apply {
        setTextColor(colors.onPrimary)
        isAllCaps = false
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(colors.primary)
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) }
    }

    root.addView(header)
    root.addView(media)
    root.addView(body)
    root.addView(cta)
    adView.addView(root)

    adView.headlineView = headline
    adView.mediaView = media
    adView.bodyView = body
    adView.callToActionView = cta
    return adView
}

private fun bindNativeAd(adView: NativeAdView, ad: NativeAd) {
    (adView.headlineView as? TextView)?.text = ad.headline
    (adView.bodyView as? TextView)?.apply {
        text = ad.body
        visibility = if (ad.body.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    (adView.callToActionView as? Button)?.apply {
        text = ad.callToAction
        visibility = if (ad.callToAction.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    ad.mediaContent?.let { adView.mediaView?.mediaContent = it }
    adView.setNativeAd(ad)
}
