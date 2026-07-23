package com.devbytes.vixplayer.app.data.repository

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of a delete attempt.
 *
 * [NeedsConsent] carries the [IntentSender] the caller must launch — the platform then
 * shows its own confirmation, and the caller re-checks what actually survived rather than
 * assuming the request was honoured in full.
 */
sealed interface DeleteResult {
    data class Deleted(val count: Int) : DeleteResult
    data class NeedsConsent(val intentSender: IntentSender) : DeleteResult
    data class Failed(val message: String) : DeleteResult
}

/**
 * Deletes media through MediaStore, across the three flows Android requires.
 *
 * - **API 30+** — `createDeleteRequest` returns a `PendingIntent`; the **OS** shows the
 *   confirmation dialog. Nothing is deleted until the user agrees, so no app-side prompt
 *   is needed (or wanted — a second dialog just trains people to dismiss without reading).
 * - **API 29** — a direct delete throws [RecoverableSecurityException] for files the app
 *   doesn't own; its `IntentSender` yields the same kind of system prompt.
 * - **API 24–28** — a direct delete succeeds outright under `WRITE_EXTERNAL_STORAGE`.
 *   **There is no system confirmation on this path**, which is why the caller shows its
 *   own dialog before reaching here.
 */
@Singleton
class MediaDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun delete(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Deleted(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pending = MediaStore.createDeleteRequest(context.contentResolver, uris)
            return@withContext DeleteResult.NeedsConsent(pending.intentSender)
        }

        var deleted = 0
        for (uri in uris) {
            try {
                deleted += context.contentResolver.delete(uri, null, null)
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    e is RecoverableSecurityException
                ) {
                    // One prompt per file is the platform's own limitation here; the
                    // caller re-queries afterwards, so a partial run stays consistent.
                    return@withContext DeleteResult.NeedsConsent(
                        e.userAction.actionIntent.intentSender
                    )
                }
                return@withContext DeleteResult.Failed("Couldn't delete — permission denied")
            } catch (e: Exception) {
                return@withContext DeleteResult.Failed("Couldn't delete")
            }
        }
        DeleteResult.Deleted(deleted)
    }

    /**
     * True only when the platform is **guaranteed** to confirm — API 30+, where
     * `createDeleteRequest` always prompts.
     *
     * Deliberately excludes API 29: there the prompt only appears for files the app
     * doesn't own, so an app-owned file would be deleted silently. Returning false makes
     * the caller show its own dialog, which can mean two prompts on 29 for a non-owned
     * file. That redundancy is the correct trade for an irreversible operation — no path
     * may delete without at least one confirmation.
     */
    fun systemConfirms(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    companion object {
        /** Result code meaning the user agreed in the system dialog. */
        const val CONSENT_OK = Activity.RESULT_OK
    }
}
