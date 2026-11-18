package com.devbytes.vixplayer.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns ad readiness for the whole app.
 *
 * [adsEnabled] is the single seam a future Pro entitlement flips off — the banner checks
 * it, so removing ads later is a one-line change here, not a UI hunt.
 *
 * [canRequestAds] stays false until EEA consent (Google UMP) is resolved and the Mobile
 * Ads SDK is initialized, so no ad ever loads before consent — the compliance gate.
 */
@Singleton
class AdsController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Master switch. Pro will set this false; for the free tier it stays true. */
    val adsEnabled: Boolean = true

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val sdkInitialized = AtomicBoolean(false)

    /**
     * Requests consent info, shows the UMP form when required, then initializes the SDK and
     * flips [canRequestAds] once consent allows. Needs an [activity] — the form renders on
     * one. Safe to call repeatedly (SDK init is guarded).
     */
    fun ensureConsentAndInit(activity: Activity) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Show the form only if required (EEA/UK, first run); no-op otherwise.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consentInfo.canRequestAds()) initializeSdk()
                }
                // Returning users and users outside the EEA can already request ads.
                if (consentInfo.canRequestAds()) initializeSdk()
            },
            {
                // Consent lookup failed — proceed only if a prior consent already allows it.
                if (consentInfo.canRequestAds()) initializeSdk()
            },
        )
    }

    private fun initializeSdk() {
        if (sdkInitialized.compareAndSet(false, true)) {
            // Cap served ad content to General-audiences (G) so ads are always consistent with
            // the app's content rating — Play rejected v62 for ad content exceeding the rating.
            // Also tag the app as NOT child-directed / not under-age (it's a general media app,
            // not a Families/kids title). Set before init so the first requests honor it.
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
                    )
                    .setTagForUnderAgeOfConsent(
                        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE,
                    )
                    .build()
            )
            MobileAds.initialize(context) {}
        }
        _canRequestAds.value = true
    }
}
