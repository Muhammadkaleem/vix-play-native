package com.devbytes.vixplayer.app.ads

import androidx.compose.runtime.Composable
import com.devbytes.vixplayer.app.BuildConfig
import com.devbytes.vixplayer.app.ui.common.LocalIsTv

/**
 * Whether ads can show here: the Play build, off TV. Pro only removes ads, so Pro UI follows this
 * too — the Amazon build and TVs offer neither.
 */
@Composable
fun rememberAdsSupported(): Boolean = BuildConfig.HAS_ADS && !LocalIsTv.current
