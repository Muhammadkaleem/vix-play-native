package com.devbytes.vixplayer.app.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Amazon Appstore build: no ad units — the slots below never render (ads are never enabled). */
object AdUnits {
    const val bannerMain = ""
    const val bannerVideoList = ""
    const val nativeFolderList = ""
    const val nativeVideoList = ""
}

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnits.bannerMain,
    unitName: String = "",
    visible: Boolean = true,
) = Unit

@Composable
fun AdNativeCard(
    adUnitId: String,
    slotKey: String,
    unitName: String,
    modifier: Modifier = Modifier,
) = Unit
