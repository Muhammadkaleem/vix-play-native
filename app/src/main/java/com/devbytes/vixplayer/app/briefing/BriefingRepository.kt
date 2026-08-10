package com.devbytes.vixplayer.app.briefing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.briefingDataStore: DataStore<Preferences> by preferencesDataStore(name = "briefing_config")

/** Persists [BriefingConfig]. The brief itself is regenerated live, so nothing else is stored. */
@Singleton
class BriefingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ENABLED           = booleanPreferencesKey("enabled")
        val DELIVERY_MINUTE   = intPreferencesKey("delivery_minute")
        val DAYS_MASK         = intPreferencesKey("days_mask")
        val ZONE_MODE         = stringPreferencesKey("zone_mode")
        val FIXED_ZONE        = stringPreferencesKey("fixed_zone")
        val LOCK_SCREEN_DETAIL = booleanPreferencesKey("lock_screen_detail")
        val SAMPLE_APPROVED_AT = longPreferencesKey("sample_approved_at")
    }

    val config: Flow<BriefingConfig> = context.briefingDataStore.data.map { it.toConfig() }

    suspend fun current(): BriefingConfig = context.briefingDataStore.data.first().toConfig()

    private fun Preferences.toConfig() = BriefingConfig(
        enabled = this[Keys.ENABLED] ?: false,
        deliveryMinuteOfDay = this[Keys.DELIVERY_MINUTE] ?: (8 * 60),
        daysMask = this[Keys.DAYS_MASK] ?: BriefingConfig.MON_TO_FRI,
        zoneMode = this[Keys.ZONE_MODE]?.let { runCatching { ZoneMode.valueOf(it) }.getOrNull() } ?: ZoneMode.FOLLOW_DEVICE,
        fixedZoneId = this[Keys.FIXED_ZONE],
        lockScreenDetail = this[Keys.LOCK_SCREEN_DETAIL] ?: false,
        sampleApprovedAt = this[Keys.SAMPLE_APPROVED_AT] ?: 0L,
    )

    suspend fun update(transform: (BriefingConfig) -> BriefingConfig) {
        context.briefingDataStore.edit { prefs ->
            val next = transform(prefs.toConfig())
            prefs[Keys.ENABLED] = next.enabled
            prefs[Keys.DELIVERY_MINUTE] = next.deliveryMinuteOfDay
            prefs[Keys.DAYS_MASK] = next.daysMask
            prefs[Keys.ZONE_MODE] = next.zoneMode.name
            if (next.fixedZoneId != null) prefs[Keys.FIXED_ZONE] = next.fixedZoneId else prefs.remove(Keys.FIXED_ZONE)
            prefs[Keys.LOCK_SCREEN_DETAIL] = next.lockScreenDetail
            prefs[Keys.SAMPLE_APPROVED_AT] = next.sampleApprovedAt
        }
    }
}
