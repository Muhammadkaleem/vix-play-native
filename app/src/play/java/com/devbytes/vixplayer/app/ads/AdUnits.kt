package com.devbytes.vixplayer.app.ads

import com.devbytes.vixplayer.app.BuildConfig

/**
 * AdMob unit IDs in one place.
 *
 * Debug builds serve Google's fixed **test** units — loading or clicking a real ad during
 * development can get the AdMob account suspended, so real IDs are only used in release.
 * Every rendered unit is exposed as a resolved getter so callers never touch a raw ID.
 */
object AdUnits {
    // Google's public test units.
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    // Real units (release only), named by placement.
    private const val REAL_BANNER_MAIN = "ca-app-pub-2601730814150494/1560144896"
    private const val REAL_BANNER_VIDEO_LIST = "ca-app-pub-2601730814150494/8506000658"
    private const val REAL_NATIVE_FOLDER_LIST = "ca-app-pub-2601730814150494/9403375271"
    private const val REAL_NATIVE_VIDEO_LIST = "ca-app-pub-2601730814150494/3884229362"

    // Rewarded unit backing the "Get 1 Day Pro" grant (AdMob app ca-app-pub-2601730814150494).
    private const val REAL_REWARDED = "ca-app-pub-2601730814150494/8927746094"

    // Interstitial shown on leaving the player (created 2026-11-18). Blanking it switches the
    // placement off in release builds — no request is made without a unit.
    private const val REAL_INTERSTITIAL = "ca-app-pub-2601730814150494/4531328759"

    // Parked — not rendered yet.
    const val APP_OPEN = "ca-app-pub-2601730814150494/1958373756"

    /** Anchored banner in the app chrome (Audio / Network / Search / History). */
    val bannerMain: String get() = if (BuildConfig.DEBUG) TEST_BANNER else REAL_BANNER_MAIN

    /** Banner at the top of the in-folder video list. */
    val bannerVideoList: String get() = if (BuildConfig.DEBUG) TEST_BANNER else REAL_BANNER_VIDEO_LIST

    /** Inline native card in the folder list. */
    val nativeFolderList: String get() = if (BuildConfig.DEBUG) TEST_NATIVE else REAL_NATIVE_FOLDER_LIST

    /** Inline native card in the video list. */
    val nativeVideoList: String get() = if (BuildConfig.DEBUG) TEST_NATIVE else REAL_NATIVE_VIDEO_LIST

    /** Rewarded ad backing the "Get 1 Day Pro" grant. */
    val rewarded: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else REAL_REWARDED

    /** Player-exit interstitial; null when no real unit is configured (release + blank ID). */
    val interstitial: String?
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else REAL_INTERSTITIAL.ifBlank { null }
}
