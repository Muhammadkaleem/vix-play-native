package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recently played network URL.
 *
 * The [url] is the primary key so replaying the same link updates its timestamp in place
 * (dedup-to-top) rather than accumulating duplicate rows. [label] is a readable
 * host/filename derived once at record time so the recents list isn't a wall of raw URLs.
 */
@Entity(tableName = "network_stream")
data class NetworkStream(
    @PrimaryKey val url: String,
    val label: String,
    val lastPlayedAt: Long,
)
