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
 * Removes media through MediaStore, across the flows Android requires.
 *
 * - **API 30+** — `createTrashRequest` moves items to the system trash: **recoverable**
 *   for ~30 days from the Files / Photos apps, and auto-purged after. The **OS** shows
 *   the confirmation dialog, so no app-side prompt is needed (or wanted — a second dialog
 *   just trains people to dismiss without reading). VixPlay deliberately offers no
 *   permanent delete on these devices; that decision belongs to the system trash UI.
 * - **API 29** — a direct delete throws [RecoverableSecurityException] for files the app
 *   doesn't own; its `IntentSender` yields the same kind of system prompt.
 * - **API 24–28** — **no trash concept exists**, so this falls back to a permanent
 *   delete under `WRITE_EXTERNAL_STORAGE`. There is also no system confirmation on this
 *   path, which is why the caller shows its own dialog before reaching here.
 */
@Singleton
class MediaDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun delete(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Deleted(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Trash rather than delete: recoverable for ~30 days via the system Files /
            // Photos apps, and it shows the same OS confirmation createDeleteRequest does.
            // Permanent removal stays the system's decision, not ours.
            val pending = MediaStore.createTrashRequest(
                context.contentResolver, uris, /* value = */ true,
            )
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

    /** True when removal goes to the system trash and is therefore recoverable. */
    fun isRecoverable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

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
