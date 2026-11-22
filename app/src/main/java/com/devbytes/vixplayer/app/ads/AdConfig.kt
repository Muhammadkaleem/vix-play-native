package com.devbytes.vixplayer.app.ads

import com.devbytes.vixplayer.app.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

private const val SEC = 1_000L
private const val MIN = 60 * SEC
private const val HOUR = 60 * MIN

/**
 * Every ad limit in one immutable snapshot. Defaults live here; each field can be overridden
 * from Firebase Remote Config (`ads_*` keys) without a release — including the kill switches.
 * Values are starting hypotheses (docs/monetisation/AD_MONETISATION_STRATEGY.md §4), not tuned.
 */
data class AdConfig(
    val adsEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
    /** Minimum gap between any two full-screen ads (interstitial or rewarded). */
    val minGapMs: Long = 4 * MIN,
    val maxPerSession: Int = 3,
    val maxPerDay: Int = 8,
    /** No interstitial until this long after first launch. */
    val installGraceMs: Long = 24 * HOUR,
    /** No interstitial in the first N sessions. */
    val graceSessions: Int = 2,
    /** Content that must have actually played in a player visit before its exit can show one. */
    val minWatchMs: Long = 60 * SEC,
    /** No interstitial this soon after the user chose to watch a rewarded ad. */
    val rewardedCooldownMs: Long = 10 * MIN,
    /** Background time after which returning to the app counts as a new session. */
    val sessionTimeoutMs: Long = 30 * MIN,
    /** Video-list native: first card after this many videos… */
    val nativeVideoListFirst: Int = 4,
    /** …then one every this many. */
    val nativeVideoListInterval: Int = 10,
    /** After an interstitial closes, offer to go ad-free with a rewarded ad (a snackbar). */
    val adFreePromptEnabled: Boolean = true,
    /** At most one such offer this often. */
    val adFreePromptGapMs: Long = 3 * 24 * HOUR,
) {
    companion object {
        /**
         * Debug builds (Google test units only) drop the grace periods and shorten the gaps so
         * the interstitial path can be exercised on a device in minutes. The release values
         * are covered by AdFrequencyPolicyTest.
         */
        val DEFAULT: AdConfig = if (BuildConfig.DEBUG) {
            AdConfig(
                minGapMs = 60 * SEC,
                installGraceMs = 0,
                graceSessions = 0,
                minWatchMs = 10 * SEC,
                rewardedCooldownMs = 60 * SEC,
                adFreePromptGapMs = 2 * MIN,
            )
        } else {
            AdConfig()
        }
    }
}

/** Reads [AdConfig] from Remote Config, falling back to [AdConfig.DEFAULT] for anything unset. */
@Singleton
class AdConfigProvider @Inject constructor() {

    private val rc: FirebaseRemoteConfig? = runCatching { FirebaseRemoteConfig.getInstance() }
        .getOrNull()
        ?.also { rc ->
            val d = AdConfig.DEFAULT
            rc.setDefaultsAsync(
                mapOf(
                    "ads_enabled" to d.adsEnabled,
                    "ads_interstitial_enabled" to d.interstitialEnabled,
                    "ads_min_gap_ms" to d.minGapMs,
                    "ads_max_per_session" to d.maxPerSession.toLong(),
                    "ads_max_per_day" to d.maxPerDay.toLong(),
                    "ads_install_grace_ms" to d.installGraceMs,
                    "ads_grace_sessions" to d.graceSessions.toLong(),
                    "ads_min_watch_ms" to d.minWatchMs,
                    "ads_rewarded_cooldown_ms" to d.rewardedCooldownMs,
                    "ads_session_timeout_ms" to d.sessionTimeoutMs,
                    "ads_native_video_first" to d.nativeVideoListFirst.toLong(),
                    "ads_native_video_interval" to d.nativeVideoListInterval.toLong(),
                    "ads_ad_free_prompt_enabled" to d.adFreePromptEnabled,
                    "ads_ad_free_prompt_gap_ms" to d.adFreePromptGapMs,
                ),
            )
        }

    /** The current snapshot; cheap enough to call at every decision point. */
    fun current(): AdConfig {
        val r = rc ?: return AdConfig.DEFAULT
        // Debug ignores server values so the shortened test timings above always apply.
        if (BuildConfig.DEBUG) return AdConfig.DEFAULT
        return runCatching {
            val d = AdConfig.DEFAULT
            fun long(key: String, def: Long) = r.getLong(key).takeIf { it >= 0 } ?: def
            AdConfig(
                adsEnabled = r.getBoolean("ads_enabled"),
                interstitialEnabled = r.getBoolean("ads_interstitial_enabled"),
                minGapMs = long("ads_min_gap_ms", d.minGapMs),
                maxPerSession = long("ads_max_per_session", d.maxPerSession.toLong()).toInt(),
                maxPerDay = long("ads_max_per_day", d.maxPerDay.toLong()).toInt(),
                installGraceMs = long("ads_install_grace_ms", d.installGraceMs),
                graceSessions = long("ads_grace_sessions", d.graceSessions.toLong()).toInt(),
                minWatchMs = long("ads_min_watch_ms", d.minWatchMs),
                rewardedCooldownMs = long("ads_rewarded_cooldown_ms", d.rewardedCooldownMs),
                sessionTimeoutMs = long("ads_session_timeout_ms", d.sessionTimeoutMs),
                nativeVideoListFirst = long("ads_native_video_first", d.nativeVideoListFirst.toLong())
                    .toInt().coerceAtLeast(1),
                nativeVideoListInterval = long("ads_native_video_interval", d.nativeVideoListInterval.toLong())
                    .toInt().coerceAtLeast(2),
                adFreePromptEnabled = r.getBoolean("ads_ad_free_prompt_enabled"),
                adFreePromptGapMs = long("ads_ad_free_prompt_gap_ms", d.adFreePromptGapMs),
            )
        }.getOrDefault(AdConfig.DEFAULT)
    }
}
