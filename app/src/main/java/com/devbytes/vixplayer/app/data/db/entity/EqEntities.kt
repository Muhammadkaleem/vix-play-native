package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Audio output routes. Profiles are stored per route so plugging in headphones can swap
 * to a different curve — the PRD's "apply globally or per-output".
 */
enum class AudioOutput { SPEAKER, HEADSET, BLUETOOTH }

/**
 * The active equalizer configuration for one output route.
 *
 * [bandLevels] is variable-length: band count is hardware-dependent (`Equalizer`
 * reports `numberOfBands`), so it cannot be a fixed set of columns and goes through a
 * TypeConverter instead.
 */
@Entity(tableName = "eq_profile")
data class EqProfile(
    @PrimaryKey val output: String,
    val enabled: Boolean,
    val bandLevels: List<Int>,
    val presetName: String,
    val bassBoost: Int,
    val virtualizer: Int,
    val preampMb: Int,
)

/** A user-saved named curve. */
@Entity(tableName = "eq_preset")
data class EqPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandLevels: List<Int>,
    val bassBoost: Int,
    val virtualizer: Int,
    val preampMb: Int,
)
