package com.devbytes.vixplayer.app.ui.premium

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.ads.RewardedAdManager
import com.devbytes.vixplayer.app.data.repository.CreditResult
import com.devbytes.vixplayer.app.data.repository.ProRepository
import com.devbytes.vixplayer.app.data.repository.RewardEntry
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Backs the "Vix Pro Rewards" flow: shows rewarded ads and credits earned Pro time through the
 * centralized [ProRepository] (the single entitlement source every screen + the ad gate reads).
 */
@HiltViewModel
class PremiumViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pro: ProRepository,
    private val rewardedAds: RewardedAdManager,
) : ViewModel() {

    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    val isProActive: StateFlow<Boolean> =
        pro.isProActive.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val proUntil: StateFlow<Long> =
        pro.proUntil.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val totalEarned: StateFlow<Int> =
        pro.totalEarned.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val ledger: StateFlow<List<RewardEntry>> =
        pro.ledger.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The reward the user will earn for one completed ad, e.g. "8 hours". */
    val rewardLabel: String = RewardConfig.label()

    init {
        viewModelScope.launch { pro.reconcile() } // refresh the anti-rollback clock anchor
    }

    /** True while "Watch ad" waits on an on-demand load — the button shows "Loading ad…". */
    val rewardedLoading: StateFlow<Boolean> = rewardedAds.loading

    /**
     * Warms a rewarded ad. Called when a Pro offer is actually on screen (card visible, modal or
     * rewards screen open) — not on every Home open, where most preloads expired unwatched.
     */
    fun preloadRewarded() = rewardedAds.preload()

    fun logScreenViewed() = log("pro_rewards_viewed")

    /**
     * Shows the rewarded ad; on a completed+verified watch credits [RewardConfig.REWARD_PER_AD_MS]
     * (idempotent via a per-watch transaction id) and reports [Result.Granted]. A part-way dismiss
     * or a no-fill grants nothing and reports [Result.Unavailable].
     */
    fun watchAdForPro(activity: Activity, entryPoint: String = "unknown", onResult: (Result) -> Unit) {
        log("watch_ad_tapped", entryPoint = entryPoint)
        val txId = UUID.randomUUID().toString()
        rewardedAds.show(
            activity,
            onReward = {
                viewModelScope.launch {
                    when (pro.creditReward(txId, RewardConfig.REWARD_PER_AD_MS, RewardConfig.MAX_BANK_MS)) {
                        is CreditResult.Credited -> {
                            log("reward_earned", RewardConfig.REWARD_PER_AD_MS)
                            log("pro_extended")
                            onResult(Result.Granted)
                        }
                        CreditResult.Duplicate -> onResult(Result.Granted) // already credited; no double-count
                    }
                }
            },
            onUnavailable = {
                log("rewarded_ad_unavailable")
                onResult(Result.Unavailable)
            },
            entryPoint = entryPoint,
        )
    }

    private fun log(event: String, earnedMs: Long = -1L, entryPoint: String? = null) {
        runCatching {
            analytics.logEvent(
                event,
                Bundle().apply {
                    if (earnedMs >= 0) putLong("earned_ms", earnedMs)
                    if (entryPoint != null) putString("entry_point", entryPoint)
                },
            )
        }
    }

    enum class Result { Granted, Unavailable }
}
