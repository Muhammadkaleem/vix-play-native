package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** SMB or FTP. Stored as a string column so Room needs no enum converter. */
enum class ShareProtocol { SMB, FTP }

/**
 * A saved network share. The **password is never here** — it lives in
 * `CredentialStore` (EncryptedSharedPreferences), keyed by [id]. Guest/anonymous
 * servers have [isGuest] = true and no stored password.
 */
@Entity(tableName = "network_server")
data class NetworkServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val protocol: String,     // ShareProtocol.name
    val host: String,
    val port: Int,            // 0 = protocol default (445 SMB / 21 FTP)
    val share: String,        // SMB share name; empty for FTP
    val path: String,         // starting path within the share/root
    val username: String,     // empty when guest/anonymous
    val domain: String,       // SMB domain; empty otherwise
    val isGuest: Boolean,
    val displayName: String,
    val addedAt: Long,
) {
    val shareProtocol: ShareProtocol get() = ShareProtocol.valueOf(protocol)
}
