package com.devbytes.vixplayer.app.data.db

import androidx.room.TypeConverter

/** Band counts vary by device, so level lists are stored as a delimited column. */
class EqConverters {
    @TypeConverter
    fun fromLevels(levels: List<Int>): String = levels.joinToString(",")

    @TypeConverter
    fun toLevels(raw: String): List<Int> =
        if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.toIntOrNull() }
}
