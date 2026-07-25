package com.devbytes.vixplayer.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** A hidden item, listed by scanning the vault directory — never MediaStore. */
data class PrivateItem(
    val file: File,
    val displayName: String,
    val sizeBytes: Long,
) {
    /** file:// URI — ExoPlayer reads it directly; app-private, so no FileProvider needed. */
    val uri: Uri get() = Uri.fromFile(file)
}

/** Outcome of an unlock attempt. */
enum class UnlockResult { OK, WRONG, NO_PIN }

private val Context.vaultStore: DataStore<Preferences> by preferencesDataStore("private_vault")

/**
 * The private folder: a vault in app-private storage (`filesDir/private`), gated by a
 * locally-hashed PIN.
 *
 * **Why app-private, not "hide in place":** on API 30+ scoped storage, a file cannot be
 * hidden from other apps or from MediaStore without `MANAGE_EXTERNAL_STORAGE` (opt-in
 * power mode, gated by a Play Console declaration). App-private storage needs no
 * permission and is invisible to MediaStore by construction, so hidden items genuinely
 * never appear in the library, history, or system gallery — the PRD's actual guarantee.
 *
 * **What this is not:** encryption. The PIN gates *access*; a PIN is only ever *verified*,
 * never read back, so it is stored as a salted PBKDF2 hash. Someone with the unlocked
 * device and ADB/root can still reach the files — per-item encryption is the PRD's P2.
 * The vault is also removed with the app on uninstall.
 */
@Singleton
class PrivateVault @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File by lazy { File(context.filesDir, "private").apply { mkdirs() } }

    // ── PIN / recovery ────────────────────────────────────────────────────────

    /** True once a PIN has been set — drives setup vs. unlock. */
    suspend fun hasPin(): Boolean = context.vaultStore.data.first()[PIN_HASH] != null

    /**
     * Sets [pin] and returns a freshly generated recovery code to show **once**. Both are
     * stored only as salted hashes.
     */
    suspend fun setPin(pin: String): String = withContext(Dispatchers.IO) {
        val recovery = generateRecoveryCode()
        val (pinSalt, pinHash) = hash(pin)
        val (recSalt, recHash) = hash(recovery)
        context.vaultStore.edit {
            it[PIN_SALT] = pinSalt
            it[PIN_HASH] = pinHash
            it[REC_SALT] = recSalt
            it[REC_HASH] = recHash
        }
        recovery
    }

    suspend fun verifyPin(pin: String): UnlockResult = check(pin, PIN_SALT, PIN_HASH)

    /** Verifies a recovery code; on success the caller lets the user set a new PIN. */
    suspend fun verifyRecovery(code: String): UnlockResult =
        check(code.trim().uppercase(), REC_SALT, REC_HASH)

    private suspend fun check(value: String, saltKey: Preferences.Key<String>, hashKey: Preferences.Key<String>): UnlockResult =
        withContext(Dispatchers.IO) {
            val prefs = context.vaultStore.data.first()
            val salt = prefs[saltKey] ?: return@withContext UnlockResult.NO_PIN
            val stored = prefs[hashKey] ?: return@withContext UnlockResult.NO_PIN
            val computed = hashWith(value, Base64.getDecoder().decode(salt))
            if (computed == stored) UnlockResult.OK else UnlockResult.WRONG
        }

    // ── File vault ────────────────────────────────────────────────────────────

    /**
     * Copies each source's bytes into the vault, returning the URIs successfully copied so
     * the caller can remove the public originals (through the consent-based delete path).
     * Copy-then-delete ordering: an interruption leaves the public copy intact.
     */
    suspend fun hide(videos: List<VideoFile>): List<Uri> = withContext(Dispatchers.IO) {
        val copied = mutableListOf<Uri>()
        for (video in videos) {
            val target = File(dir, "${System.currentTimeMillis()}_${video.name}")
            val ok = runCatching {
                context.contentResolver.openInputStream(video.uri)!!.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }.isSuccess
            if (ok) copied.add(video.uri) else runCatching { target.delete() }
        }
        copied
    }

    suspend fun list(): List<PrivateItem> = withContext(Dispatchers.IO) {
        (dir.listFiles()?.filter { it.isFile } ?: emptyList())
            .sortedByDescending { it.lastModified() }
            .map {
                PrivateItem(
                    file = it,
                    // Strip the timestamp prefix added at hide time.
                    displayName = it.name.substringAfter('_', it.name),
                    sizeBytes = it.length(),
                )
            }
    }

    /** Restores an item to public storage (Movies/), then removes the vault copy. */
    suspend fun unhide(item: PrivateItem): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, item.displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        runCatching {
            item.file.inputStream().use { input ->
                resolver.openOutputStream(target)!!.use { input.copyTo(it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(target, ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }, null, null)
            }
        }.onFailure {
            runCatching { resolver.delete(target, null, null) }
            return@withContext false
        }
        item.file.delete()
    }

    /** Permanently removes an item from the vault (no public copy is made). */
    suspend fun delete(item: PrivateItem): Boolean =
        withContext(Dispatchers.IO) { item.file.delete() }

    // ── Hashing ───────────────────────────────────────────────────────────────

    private fun hash(value: String): Pair<String, String> {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(salt) to hashWith(value, salt)
    }

    private fun hashWith(value: String, salt: ByteArray): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    /** Grouped for readability: XXXX-XXXX, from an unambiguous alphabet (no 0/O/1/I). */
    private fun generateRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        val chars = CharArray(8) { alphabet[rnd.nextInt(alphabet.length)] }
        return "${String(chars, 0, 4)}-${String(chars, 4, 4)}"
    }

    private companion object {
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val REC_SALT = stringPreferencesKey("recovery_salt")
        val REC_HASH = stringPreferencesKey("recovery_hash")
        const val ITERATIONS = 120_000
        const val KEY_LENGTH = 256
    }
}
