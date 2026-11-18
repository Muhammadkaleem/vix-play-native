package com.devbytes.vixplayer.app.data.repository

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** One credited reward, as stored in the local ledger. */
data class RewardEntry(
    val txId: String,
    val earnedMs: Long,
    val creditedAt: Long,
    val expiryAfter: Long,
    val verified: Boolean,
)

sealed interface CreditResult {
    /** Reward credited; [newExpiry] is the (extended) absolute Pro expiry. */
    data class Credited(val newExpiry: Long) : CreditResult
    /** The transaction id was already processed — no double-credit. */
    data object Duplicate : CreditResult
}

private val Context.proDataStore: DataStore<Preferences> by preferencesDataStore(name = "vixplay_pro")

/**
 * The single source of truth for the "Vix Pro" entitlement (rewarded-ad tier).
 *
 * **Accumulation:** `newExpiry = max(trustedNow, existingExpiry) + earned` — a new reward always
 * extends and never overwrites previously earned time, and the user needn't wait for the current
 * period to end before earning more.
 *
 * **Idempotency:** each rewarded watch carries a unique transaction id; a repeated id is ignored,
 * so a duplicated AdMob reward callback can't double-credit.
 *
 * **Anti-rollback (best effort):** an [effectiveNow] derived from `elapsedRealtime` keeps advancing
 * even if the wall clock is set backwards within a boot, so rolling the clock back can't keep Pro
 * alive past its legitimate expiry *within the same boot*. Across a reboot there is no trusted time
 * source in this repo, so full offline clock-tamper protection is **NOT** claimed — that requires a
 * backend / signed server time (see the SSV note in [com.devbytes.vixplayer.app.ads]).
 */
@Singleton
class ProRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object K {
        val PRO_UNTIL = longPreferencesKey("pro_until")
        val TOTAL_EARNED = intPreferencesKey("pro_total_earned")
        val LEDGER = stringPreferencesKey("pro_ledger")
        val TX_IDS = stringSetPreferencesKey("pro_tx_ids")
        val ANCHOR_WALL = longPreferencesKey("pro_anchor_wall")
        val ANCHOR_ELAPSED = longPreferencesKey("pro_anchor_elapsed")
    }

    /** Monotonic-within-boot "now": never earlier than (anchorWall + elapsed since the anchor). */
    private fun effectiveNow(anchorWall: Long, anchorElapsed: Long): Long {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        return if (anchorElapsed in 1..nowElapsed) {
            maxOf(nowWall, anchorWall + (nowElapsed - anchorElapsed))
        } else {
            nowWall // no anchor yet, or a reboot reset elapsedRealtime — fall back to wall clock
        }
    }

    val proUntil: Flow<Long> = context.proDataStore.data.map { it[K.PRO_UNTIL] ?: 0L }

    val isProActive: Flow<Boolean> = context.proDataStore.data.map { p ->
        val until = p[K.PRO_UNTIL] ?: 0L
        until > 0 && effectiveNow(p[K.ANCHOR_WALL] ?: 0L, p[K.ANCHOR_ELAPSED] ?: 0L) < until
    }

    val totalEarned: Flow<Int> = context.proDataStore.data.map { it[K.TOTAL_EARNED] ?: 0 }

    val ledger: Flow<List<RewardEntry>> = context.proDataStore.data.map { parseLedger(it[K.LEDGER]) }

    suspend fun isProActiveNow(): Boolean = isProActive.first()

    /**
     * Credits [earnedMs] of Pro for a completed + (locally) verified rewarded ad, keyed by the
     * unique [txId]. Idempotent and atomic. Returns [CreditResult.Duplicate] if this id was
     * already processed (no change), else [CreditResult.Credited] with the extended expiry.
     */
    suspend fun creditReward(
        txId: String,
        earnedMs: Long,
        maxBankMs: Long = Long.MAX_VALUE,
    ): CreditResult {
        var result: CreditResult = CreditResult.Duplicate
        context.proDataStore.edit { p ->
            val processed = p[K.TX_IDS] ?: emptySet()
            if (txId in processed) {
                result = CreditResult.Duplicate
                return@edit
            }
            val existing = p[K.PRO_UNTIL] ?: 0L
            val now = effectiveNow(p[K.ANCHOR_WALL] ?: 0L, p[K.ANCHOR_ELAPSED] ?: 0L)
            // Extend from the later of now/existing, then clamp the banked balance to the cap.
            val newExpiry = minOf(maxOf(now, existing) + earnedMs, now + maxBankMs)

            p[K.PRO_UNTIL] = newExpiry
            p[K.TX_IDS] = processed + txId
            p[K.TOTAL_EARNED] = (p[K.TOTAL_EARNED] ?: 0) + 1

            val arr = JSONArray(p[K.LEDGER] ?: "[]")
            arr.put(
                JSONObject()
                    .put("tx", txId)
                    .put("earnedMs", earnedMs)
                    .put("at", System.currentTimeMillis())
                    .put("expiry", newExpiry)
                    .put("verified", true),
            )
            p[K.LEDGER] = arr.toString()

            // Re-anchor the trusted clock to "now".
            p[K.ANCHOR_WALL] = System.currentTimeMillis()
            p[K.ANCHOR_ELAPSED] = SystemClock.elapsedRealtime()

            result = CreditResult.Credited(newExpiry)
        }
        return result
    }

    /**
     * Refreshes the trusted-clock anchor for the current boot. Call at app launch. Keeps the
     * furthest-forward effective time seen so a backward wall-clock jump can't rewind it.
     */
    suspend fun reconcile() {
        context.proDataStore.edit { p ->
            val eff = effectiveNow(p[K.ANCHOR_WALL] ?: 0L, p[K.ANCHOR_ELAPSED] ?: 0L)
            p[K.ANCHOR_WALL] = maxOf(eff, System.currentTimeMillis())
            p[K.ANCHOR_ELAPSED] = SystemClock.elapsedRealtime()
        }
    }

    private fun parseLedger(json: String?): List<RewardEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RewardEntry(
                    txId = o.optString("tx"),
                    earnedMs = o.optLong("earnedMs"),
                    creditedAt = o.optLong("at"),
                    expiryAfter = o.optLong("expiry"),
                    verified = o.optBoolean("verified", true),
                )
            }.sortedByDescending { it.creditedAt }
        }.getOrDefault(emptyList())
    }
}
