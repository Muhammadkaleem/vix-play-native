package com.devbytes.vixplayer.app.ads

import android.app.Activity
import android.content.Context
import com.devbytes.vixplayer.app.BuildConfig
import com.devbytes.vixplayer.app.data.repository.ProRepository
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns ad readiness for the whole app.
 *
 * [adsEnabled] is the single "may this user see ads" answer: not Pro and not remotely switched
 * off. Banner / native hosts and the interstitial gate all read it, so Pro removes every ad from
 * one place. [ProRepository] stays the only source of the entitlement.
 *
 * [canRequestAds] stays false until EEA consent (Google UMP) is resolved and the Mobile
 * Ads SDK is initialized, so no ad ever loads before consent — the compliance gate.
 */
@Singleton
class AdsController @Inject constructor(
    @ApplicationContext private val context: Context,
    pro: ProRepository,
    private val config: AdConfigProvider,
    private val frequency: AdFrequencyStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val isPro: StateFlow<Boolean> = pro.isProActive
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Not Pro and not remotely disabled. Pro activating drops this to false live. */
    val adsEnabled: StateFlow<Boolean> = isPro
        .map { !it && config.current().adsEnabled }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /** Consent resolved *and* this user may see ads — what every ad load checks. */
    val adsAllowed: StateFlow<Boolean> = combine(adsEnabled, canRequestAds) { e, c -> e && c }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /**
     * True where the consent SDK requires a way to revisit consent (EEA/UK/CH with a published
     * GDPR message). Settings shows its "Privacy & ad choices" row only then.
     */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val sdkInitialized = AtomicBoolean(false)
    private val firstForeground = AtomicBoolean(true)

    /** MainActivity.onStart — counts sessions for the frequency rules. */
    fun onAppForeground() {
        val cold = firstForeground.getAndSet(false)
        scope.launch {
            frequency.onForeground(System.currentTimeMillis(), config.current().sessionTimeoutMs, cold)
        }
    }

    /** MainActivity.onStop. */
    fun onAppBackground() {
        scope.launch { frequency.onBackground(System.currentTimeMillis()) }
    }

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
                    updatePrivacyOptions(consentInfo)
                    if (consentInfo.canRequestAds()) initializeSdk()
                }
                updatePrivacyOptions(consentInfo)
                // Returning users and users outside the EEA can already request ads.
                if (consentInfo.canRequestAds()) initializeSdk()
            },
            {
                // Consent lookup failed — proceed only if a prior consent already allows it.
                if (consentInfo.canRequestAds()) initializeSdk()
            },
        )
    }

    private fun updatePrivacyOptions(info: ConsentInformation) {
        _privacyOptionsRequired.value = info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * Re-opens the consent form so the user can change their choice (Google requires this entry
     * point wherever a GDPR message is shown). The new choice takes effect on the next ad request.
     */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            updatePrivacyOptions(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun initializeSdk() {
        if (sdkInitialized.compareAndSet(false, true)) {
            // Cap served ad content to General-audiences (G) so ads are always consistent with
            // the app's content rating — Play rejected v62 for ad content exceeding the rating.
            // Also tag the app as NOT child-directed / not under-age (it's a general media app,
            // not a Families/kids title). Set before init so the first requests honor it.
            // Team phones listed in local.properties get test ads even on release builds, so
            // internal testing never produces invalid clicks on the live units.
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
                    )
                    .setTagForUnderAgeOfConsent(
                        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE,
                    )
                    .setTestDeviceIds(
                        BuildConfig.AD_TEST_DEVICE_IDS.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    )
                    .build()
            )
            MobileAds.initialize(context) {}
        }
        _canRequestAds.value = true
    }
}
