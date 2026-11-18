package com.devbytes.vixplayer.app.ui.premium

/**
 * Central, adjustable reward model.
 *
 * Changing [REWARD_PER_AD_MS] re-prices *future* rewards without touching already-credited
 * entitlements — those are stored as absolute expiry timestamps, not as "N days", so a rate
 * change never retroactively shrinks or grows time a user has already earned.
 */
object RewardConfig {
    const val HOUR_MS = 3_600_000L

    /** Pro time credited per completed + verified rewarded ad. */
    const val REWARD_PER_AD_MS = 8L * HOUR_MS

    /**
     * Maximum Pro time that can be *banked* at once. Watching more ads never fails, but the
     * balance is capped here (a rolling cap, not a lifetime earning limit). Surfaced to the user
     * as "bank up to 48h". Adjustable without touching already-credited time.
     */
    const val MAX_BANK_MS = 48L * HOUR_MS

    /** Short "+8h" label for the Extend button. */
    val perAdShort: String = "+${REWARD_PER_AD_MS / HOUR_MS}h"

    /** Short "48h" label for the bank cap. */
    val bankShort: String = "${MAX_BANK_MS / HOUR_MS}h"

    /** Human label for a duration (e.g. the per-ad reward shown before the ad starts). */
    fun label(ms: Long = REWARD_PER_AD_MS): String {
        val h = ms / HOUR_MS
        return when {
            h >= 24 && h % 24 == 0L -> (h / 24).let { if (it == 1L) "1 day" else "$it days" }
            h >= 1 -> if (h == 1L) "1 hour" else "$h hours"
            else -> "${ms / 60_000} min"
        }
    }

    /** Illustrative accumulation table for the rewards screen (completed ads → total Pro). */
    val tiers: List<Pair<Int, String>> = listOf(
        1 to "8 hours",
        3 to "1 day",
        6 to "2 days",
        9 to "3 days",
        15 to "5 days",
        30 to "10 days",
        90 to "30 days",
    )
}
