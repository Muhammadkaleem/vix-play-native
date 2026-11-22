package com.devbytes.vixplayer.app.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Amazon Appstore build: no ad SDK and no consent SDK, so ads are never enabled and there is no
 * consent to collect. Same API as the Google Play [AdsController] so shared code is unchanged.
 */
@Singleton
class AdsController @Inject constructor() {
    private val off: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    val isPro: StateFlow<Boolean> = off
    val adsEnabled: StateFlow<Boolean> = off
    val canRequestAds: StateFlow<Boolean> = off
    val adsAllowed: StateFlow<Boolean> = off
    val privacyOptionsRequired: StateFlow<Boolean> = off

    fun onAppForeground() = Unit
    fun onAppBackground() = Unit
    fun ensureConsentAndInit(activity: Activity) = Unit
    fun showPrivacyOptions(activity: Activity) = Unit
}
