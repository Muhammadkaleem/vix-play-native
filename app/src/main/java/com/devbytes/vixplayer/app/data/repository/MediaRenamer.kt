package com.devbytes.vixplayer.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RenameResult {
    data object Renamed : RenameResult
    data class NeedsConsent(val intentSender: IntentSender) : RenameResult
    data class Failed(val message: String) : RenameResult
}

/**
 * Renames a media file, across the flows Android requires.
 *
 * - **API 30+** — `createWriteRequest` asks the user first; the update runs after consent.
 * - **API 29** — the update throws `RecoverableSecurityException` for files the app
 *   doesn't own, whose `IntentSender` gives the same prompt.
 * - **API 24–28** — updating `DISPLAY_NAME` alone does **not** rename the file on disk on
 *   legacy storage, so the file is renamed through its `DATA` path and MediaStore is then
 *   synced to match. `MediaRepository` already relies on `DATA` for video paths.
 */
@Singleton
class MediaRenamer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Applies [newName] to [uri], keeping [originalName]'s extension.
     *
     * The extension is preserved rather than trusted to the user: renaming `clip.mp4` to
     * `holiday` would otherwise produce a file the system can no longer classify.
     */
    suspend fun rename(
        uri: Uri,
        originalName: String,
        newName: String,
    ): RenameResult = withContext(Dispatchers.IO) {
        val finalName = applyExtension(newName.trim(), originalName)
            ?: return@withContext RenameResult.Failed("Enter a valid name")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pending = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
            return@withContext RenameResult.NeedsConsent(pending.intentSender)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                applyDisplayName(uri, finalName)
            } else {
                renameLegacyFile(uri, finalName)
            }
            RenameResult.Renamed
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                e is android.app.RecoverableSecurityException
            ) {
                RenameResult.NeedsConsent(e.userAction.actionIntent.intentSender)
            } else {
                RenameResult.Failed("Couldn't rename — permission denied")
            }
        } catch (e: Exception) {
            RenameResult.Failed("Couldn't rename")
        }
    }

    /** Runs the actual update once the system has granted write access. */
    suspend fun applyAfterConsent(
        uri: Uri,
        originalName: String,
        newName: String,
    ): RenameResult = withContext(Dispatchers.IO) {
        val finalName = applyExtension(newName.trim(), originalName)
            ?: return@withContext RenameResult.Failed("Enter a valid name")
        try {
            applyDisplayName(uri, finalName)
            RenameResult.Renamed
        } catch (e: Exception) {
            RenameResult.Failed("Couldn't rename")
        }
    }

    private fun applyDisplayName(uri: Uri, name: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        }
        context.contentResolver.update(uri, values, null, null)
    }

    /**
     * Legacy path: on API 24–28 a DISPLAY_NAME update does not move the file, so rename
     * on disk first, then point MediaStore at the new path.
     */
    private fun renameLegacyFile(uri: Uri, name: String) {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val path = context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: throw IllegalStateException("No path")

        val source = File(path)
        val target = File(source.parentFile, name)
        if (!source.renameTo(target)) throw IllegalStateException("Rename failed")

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, target.absolutePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        }
        context.contentResolver.update(uri, values, null, null)
    }

    /**
     * Returns [newName] carrying [originalName]'s extension, or null when the input is
     * unusable — blank, or containing path separators that would move the file.
     */
    private fun applyExtension(newName: String, originalName: String): String? {
        if (newName.isBlank() || newName.contains('/') || newName.contains('\\')) return null
        val extension = originalName.substringAfterLast('.', "")
        if (extension.isBlank()) return newName
        return if (newName.endsWith(".$extension", ignoreCase = true)) {
            newName
        } else {
            "$newName.$extension"
        }
    }
}
