package com.devbytes.vixplayer.app.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private val Context.adsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vixplay_ads")

/**
 * Durable ad-frequency counters: install time, session count, last full-screen / rewarded ad,
 * and today's interstitial count. The per-session count is memory-only (a new process or a
 * session timeout starts it at zero).
 */
@Singleton
class AdFrequencyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object K {
        val INSTALLED_AT = longPreferencesKey("installed_at")
        val SESSION_COUNT = intPreferencesKey("session_count")
        val LAST_FULLSCREEN = longPreferencesKey("last_fullscreen_at")
        val LAST_REWARDED = longPreferencesKey("last_rewarded_at")
        val DAY_KEY = longPreferencesKey("day_key")
        val TODAY_COUNT = intPreferencesKey("today_count")
        val BACKGROUND_AT = longPreferencesKey("background_at")
    }

    private val sessionShown = AtomicInteger(0)

    private fun today(): Long = LocalDate.now().toEpochDay()

    suspend fun snapshot(): AdFrequencyState {
        val p = context.adsDataStore.data.first()
        return AdFrequencyState(
            installedAt = p[K.INSTALLED_AT] ?: System.currentTimeMillis(),
            sessionCount = p[K.SESSION_COUNT] ?: 0,
            lastFullScreenAt = p[K.LAST_FULLSCREEN] ?: 0L,
            lastRewardedAt = p[K.LAST_REWARDED] ?: 0L,
            todayCount = if (p[K.DAY_KEY] == today()) p[K.TODAY_COUNT] ?: 0 else 0,
            sessionShownCount = sessionShown.get(),
        )
    }

    /**
     * App came to the foreground. Counts a new session on first launch, on a cold start, or
     * after more than [timeoutMs] in the background.
     */
    suspend fun onForeground(now: Long, timeoutMs: Long, coldStart: Boolean) {
        context.adsDataStore.edit { p ->
            if (p[K.INSTALLED_AT] == null) p[K.INSTALLED_AT] = now
            val bgAt = p[K.BACKGROUND_AT] ?: 0L
            if (coldStart || bgAt == 0L || now - bgAt > timeoutMs) {
                p[K.SESSION_COUNT] = (p[K.SESSION_COUNT] ?: 0) + 1
                sessionShown.set(0)
            }
            p[K.BACKGROUND_AT] = 0L
        }
    }

    suspend fun onBackground(now: Long) {
        context.adsDataStore.edit { it[K.BACKGROUND_AT] = now }
    }

    /** An interstitial was shown. */
    suspend fun recordInterstitial(now: Long) {
        sessionShown.incrementAndGet()
        context.adsDataStore.edit { p ->
            val day = today()
            val count = if (p[K.DAY_KEY] == day) p[K.TODAY_COUNT] ?: 0 else 0
            p[K.DAY_KEY] = day
            p[K.TODAY_COUNT] = count + 1
            p[K.LAST_FULLSCREEN] = now
        }
    }

    /** A rewarded ad was shown (the user chose it) — starts the rewarded cooldown. */
    suspend fun recordRewarded(now: Long) {
        context.adsDataStore.edit { p ->
            p[K.LAST_FULLSCREEN] = now
            p[K.LAST_REWARDED] = now
        }
    }
}
