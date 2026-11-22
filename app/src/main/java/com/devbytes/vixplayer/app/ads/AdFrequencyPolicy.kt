package com.devbytes.vixplayer.app.ads

/** Persisted + in-memory frequency counters at decision time. */
data class AdFrequencyState(
    val installedAt: Long,
    val sessionCount: Int,
    /** Last interstitial *or* rewarded ad shown — full-screen ads never run back to back. */
    val lastFullScreenAt: Long,
    val lastRewardedAt: Long,
    /** Interstitials shown on the current local day. */
    val todayCount: Int,
    /** Interstitials shown in the current session (in memory). */
    val sessionShownCount: Int,
)

/** What is happening at this natural break. */
data class BreakContext(
    val isPro: Boolean,
    /** Content actually played during the visit that is ending. */
    val watchedMs: Long,
    /** The player was opened by another app — leaving it must not show an ad. */
    val externalOpen: Boolean,
)

sealed interface AdDecision {
    data object Show : AdDecision
    data class Skip(val reason: String) : AdDecision
}

/**
 * Pure interstitial gate — no clock, storage or Android types, so every rule is unit-tested.
 * The first rule that fails wins; its reason is logged as `interstitial_skipped.reason`.
 */
object AdFrequencyPolicy {

    fun decide(now: Long, s: AdFrequencyState, c: AdConfig, ctx: BreakContext): AdDecision = when {
        !c.adsEnabled || !c.interstitialEnabled -> skip("disabled")
        ctx.isPro -> skip("pro")
        ctx.externalOpen -> skip("external_open")
        now - s.installedAt < c.installGraceMs -> skip("install_grace")
        s.sessionCount <= c.graceSessions -> skip("session_grace")
        ctx.watchedMs < c.minWatchMs -> skip("short_watch")
        now - s.lastFullScreenAt < c.minGapMs -> skip("min_gap")
        now - s.lastRewardedAt < c.rewardedCooldownMs -> skip("rewarded_cooldown")
        s.sessionShownCount >= c.maxPerSession -> skip("session_cap")
        s.todayCount >= c.maxPerDay -> skip("daily_cap")
        else -> AdDecision.Show
    }

    /**
     * Whether to offer "go ad-free" (a rewarded ad for Pro time) right after an interstitial
     * closed: the one moment the trade is obvious. Never to someone already Pro, and at most
     * once per [AdConfig.adFreePromptGapMs] so it stays an offer, not a nag.
     */
    fun offersAdFree(now: Long, lastPromptAt: Long, c: AdConfig, isPro: Boolean): Boolean =
        c.adsEnabled && c.adFreePromptEnabled && !isPro && now - lastPromptAt >= c.adFreePromptGapMs

    private fun skip(reason: String) = AdDecision.Skip(reason)
}
