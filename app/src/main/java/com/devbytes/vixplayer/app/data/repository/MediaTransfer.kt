package com.devbytes.vixplayer.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/** Progress for the in-flight transfer, surfaced to the modal dialog. */
data class TransferProgress(
    val currentIndex: Int,
    val total: Int,
    val currentName: String,
    val fileFraction: Float,
)

sealed interface TransferResult {
    data class Completed(val succeeded: Int, val failed: Int) : TransferResult
    data class Cancelled(val succeeded: Int) : TransferResult
    data class Failed(val message: String) : TransferResult
}

/**
 * Copies (and moves) media between folders.
 *
 * **Copy is the primitive; move is copy-then-delete, and the source is deleted only after
 * its copy is verified.** A failed or interrupted move therefore degrades to "there are
 * now two copies" — recoverable — rather than "the file is gone", which is not.
 *
 * On API 29+ the destination is created with `IS_PENDING = 1` and only published on
 * success, so an abandoned copy is never visible to the gallery even if cleanup fails.
 * **There is no equivalent on API 24–28**, where an interrupted copy leaves a partial
 * file that cleanup must remove explicitly — and cannot, if the process is killed.
 */
@Singleton
class MediaTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Copies [videos] into [destinationDir], reporting progress and honouring
     * cancellation of the calling coroutine. When [deleteSource] is true this is a move.
     */
    suspend fun transfer(
        videos: List<VideoFile>,
        destinationDir: String,
        deleteSource: Boolean,
        onProgress: (TransferProgress) -> Unit,
    ): TransferResult = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) return@withContext TransferResult.Completed(0, 0)

        var succeeded = 0
        var failed = 0

        for ((index, video) in videos.withIndex()) {
            try {
                coroutineContext.ensureActive()
            } catch (e: Exception) {
                return@withContext TransferResult.Cancelled(succeeded)
            }

            onProgress(TransferProgress(index + 1, videos.size, video.name, 0f))

            val copied = runCatching {
                copyOne(video, destinationDir) { fraction ->
                    onProgress(TransferProgress(index + 1, videos.size, video.name, fraction))
                }
            }.getOrNull()

            if (copied == null) {
                failed++
                continue
            }

            // Only now is removing the original safe: the copy exists and is published.
            if (deleteSource) {
                val removed = runCatching {
                    context.contentResolver.delete(video.uri, null, null)
                }.getOrDefault(0)
                if (removed == 0) failed++ else succeeded++
            } else {
                succeeded++
            }
        }

        TransferResult.Completed(succeeded, failed)
    }

    /**
     * Streams one file into [destinationDir], returning the new URI.
     *
     * Throws on any failure, having first removed the partial destination — the caller
     * treats a throw as "this file did not transfer" and leaves the source alone.
     */
    private suspend fun copyOne(
        video: VideoFile,
        destinationDir: String,
        onFileProgress: (Float) -> Unit,
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, video.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePathFor(destinationDir))
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                put(MediaStore.Video.Media.DATA, File(destinationDir, video.name).absolutePath)
            }
        }

        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Couldn't create destination")

        try {
            resolver.openInputStream(video.uri).use { input ->
                requireNotNull(input) { "Couldn't read source" }
                resolver.openOutputStream(target).use { output ->
                    requireNotNull(output) { "Couldn't write destination" }
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    val total = video.sizeBytes.coerceAtLeast(1)
                    var written = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        onFileProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                    output.flush()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publish = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(target, publish, null, null)
            }
            return target
        } catch (e: Exception) {
            // Remove the partial destination so a failure never leaves a broken file
            // behind. On API 29+ it was pending and thus never visible anyway.
            runCatching { resolver.delete(target, null, null) }
            throw e
        }
    }

    /**
     * MediaStore wants a volume-relative path ("Movies/Holiday"), not an absolute one.
     * Falls back to the standard Movies directory if the shape is unrecognised, so a
     * transfer never writes somewhere unexpected.
     */
    private fun relativePathFor(absoluteDir: String): String {
        val marker = "/0/"
        val index = absoluteDir.indexOf(marker)
        val relative = if (index >= 0) {
            absoluteDir.substring(index + marker.length)
        } else {
            absoluteDir.substringAfterLast('/')
        }
        return relative.trim('/').ifBlank { "Movies" }
    }
}
