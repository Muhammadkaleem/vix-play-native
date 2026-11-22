package com.devbytes.vixplayer.app.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player
import com.devbytes.vixplayer.app.player.PlaybackKind
import com.devbytes.vixplayer.app.player.PlayerController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one interstitial placement: leaving the video player for the library, after real
 * watching. Never during playback, never on launch, never after an external open, never
 * back-to-back with a rewarded ad — [AdFrequencyPolicy] decides, this class only loads and shows.
 *
 * One ad is kept warm; a break with nothing warm shows nothing (no spinner at a break).
 * Watch time is measured off the shared player itself, so the player screen stays ad-unaware.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: AdsController,
    private val config: AdConfigProvider,
    private val frequency: AdFrequencyStore,
    private val events: AdEvents,
    private val playerController: PlayerController,
) {
    private val tag = "InterstitialAd"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _adFreeOffers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** An interstitial just closed and the user may want the ad-free trade; see [AdFrequencyPolicy.offersAdFree]. */
    val adFreeOffers: Flow<Unit> = _adFreeOffers.asSharedFlow()

    private var ad: InterstitialAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var failures = 0
    private var retryJob: Job? = null

    // ── Watch time of the current player visit ──────────────────────────────
    private var watchedMs = 0L
    private var playingSince = 0L
    private var externalVisit = false
    private var listening = false

    private val playListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && playerController.kind.value == PlaybackKind.VIDEO) {
                playingSince = SystemClock.elapsedRealtime()
            } else {
                flushPlaying()
            }
        }
    }

    private fun flushPlaying() {
        if (playingSince > 0) {
            watchedMs += SystemClock.elapsedRealtime() - playingSince
            playingSince = 0
        }
    }

    fun onPlayerEntered() {
        if (!listening) {
            playerController.player.addListener(playListener)
            listening = true
        }
        watchedMs = 0
        playingSince = if (playerController.player.isPlaying) SystemClock.elapsedRealtime() else 0
        // Warm an ad only if this visit's exit could actually use it: a grace period, a cap or
        // Pro would block it anyway, and an unshown load is exactly the waste we're removing.
        // (The gap / rewarded cooldown may well have passed by the time the user leaves.)
        val ctx = BreakContext(controller.isPro.value, watchedMs = Long.MAX_VALUE, externalOpen = externalVisit)
        scope.launch {
            val d = AdFrequencyPolicy.decide(System.currentTimeMillis(), frequency.snapshot(), config.current(), ctx)
            if (d is AdDecision.Show || (d as AdDecision.Skip).reason in TRANSIENT_SKIPS) preloadIfEligible()
        }
    }

    fun markExternalVisit() {
        externalVisit = true
    }

    /** Loads one ad if this user may see interstitials and none is warm or loading. */
    fun preloadIfEligible() {
        val unit = AdUnits.interstitial ?: return // no unit configured for this build
        if (!controller.adsAllowed.value || !config.current().interstitialEnabled) return
        if (loading || retryJob?.isActive == true) return
        if (ad != null && SystemClock.elapsedRealtime() - loadedAt < AD_TTL_MS) return
        ad = null
        loading = true
        val start = SystemClock.elapsedRealtime()
        InterstitialAd.load(
            context,
            unit,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    failures = 0
                    ad = loaded
                    loadedAt = SystemClock.elapsedRealtime()
                    loaded.setOnPaidEventListener { v ->
                        events.paid(AdFormat.INTERSTITIAL, "VixPlay_Interstitial_PlayerExit", v, loaded.responseInfo)
                    }
                    events.loaded(AdFormat.INTERSTITIAL, PLACEMENT, loadedAt - start)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    ad = null
                    events.loadFailed(AdFormat.INTERSTITIAL, PLACEMENT, error.code)
                    Log.w(tag, "load failed: ${error.code} ${error.message}")
                    scheduleRetry()
                }
            },
        )
    }

    /** Exponential backoff (30 s → 5 min) so a no-fill streak never becomes a request burst. */
    private fun scheduleRetry() {
        failures++
        val wait = (RETRY_BASE_MS shl (failures - 1).coerceAtMost(4)).coerceAtMost(RETRY_MAX_MS)
        retryJob = scope.launch {
            delay(wait)
            preloadIfEligible()
        }
    }

    /** The player route was left for the library. Shows a warm ad if every rule allows it. */
    fun onPlayerExit(activity: Activity) {
        flushPlaying()
        val ctx = BreakContext(
            isPro = controller.isPro.value || !controller.adsAllowed.value,
            watchedMs = watchedMs,
            externalOpen = externalVisit,
        )
        externalVisit = false
        watchedMs = 0
        scope.launch {
            val cfg = config.current()
            val decision = AdFrequencyPolicy.decide(System.currentTimeMillis(), frequency.snapshot(), cfg, ctx)
            if (decision is AdDecision.Skip) {
                events.interstitialSkipped(PLACEMENT, decision.reason)
                Log.i(tag, "skip: ${decision.reason}")
                return@launch
            }
            val ready = ad?.takeIf { SystemClock.elapsedRealtime() - loadedAt < AD_TTL_MS }
            if (ready == null) {
                events.interstitialSkipped(PLACEMENT, "not_ready")
                preloadIfEligible()
                return@launch
            }
            if (!activity.canShow()) {
                events.interstitialSkipped(PLACEMENT, "activity_not_resumed")
                return@launch
            }
            show(activity, ready)
        }
    }

    private fun show(activity: Activity, interstitial: InterstitialAd) {
        ad = null
        interstitial.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                events.interstitialShown(PLACEMENT)
                scope.launch { frequency.recordInterstitial(System.currentTimeMillis()) }
            }

            override fun onAdClicked() = events.clicked(AdFormat.INTERSTITIAL, PLACEMENT)

            override fun onAdDismissedFullScreenContent() {
                preloadIfEligible()
                scope.launch {
                    val now = System.currentTimeMillis()
                    val isPro = controller.isPro.value || !controller.adsAllowed.value
                    val offer = AdFrequencyPolicy.offersAdFree(now, frequency.lastAdFreePromptAt(), config.current(), isPro)
                    Log.i(tag, "closed; offer ad-free: $offer")
                    if (offer) {
                        frequency.recordAdFreePrompt(now)
                        events.adFreePrompt("shown")
                        _adFreeOffers.tryEmit(Unit)
                    }
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                events.showFailed(AdFormat.INTERSTITIAL, error.code)
                preloadIfEligible()
            }
        }
        interstitial.show(activity)
    }

    private fun Activity.canShow(): Boolean =
        !isFinishing && !isDestroyed &&
            (this as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false

    private companion object {
        const val PLACEMENT = "player_exit"
        /** Google expires a loaded interstitial after 1 h; drop ours a little earlier. */
        const val AD_TTL_MS = 55 * 60_000L
        const val RETRY_BASE_MS = 30_000L
        const val RETRY_MAX_MS = 5 * 60_000L
        /** Skip reasons that can clear during one player visit. */
        val TRANSIENT_SKIPS = setOf("min_gap", "rewarded_cooldown")
    }
}
