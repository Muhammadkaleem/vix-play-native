package com.devbytes.vixplayer.app.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores share passwords in EncryptedSharedPreferences, keyed by server id — never in
 * the Room DB (which is plaintext and included in backups). Mirrors the JWT decision from
 * the subtitle-search grill: credentials go here, not in a queryable table.
 */
@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "network_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun setPassword(serverId: Long, password: String) {
        prefs.edit().putString(key(serverId), password).apply()
    }

    fun getPassword(serverId: Long): String? = prefs.getString(key(serverId), null)

    fun removePassword(serverId: Long) {
        prefs.edit().remove(key(serverId)).apply()
    }

    private fun key(serverId: Long) = "pw_$serverId"
}
