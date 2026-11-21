package androidx.media3.decoder.ffmpeg

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.media3.common.C
import androidx.media3.common.Format
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * VixPlay's intro detection, decoding audio with the FFmpeg extension's decoder directly.
 *
 * Lives in Media3's package because [FfmpegAudioDecoder] is package-private there. It decodes
 * in this process: the platform's MediaCodec path makes a round trip to the system's codec
 * process for every ~21 ms of audio, which turned ten minutes into ~50 s; FFmpeg here takes a
 * few seconds. The extension is a checked-in .aar pinned to the Media3 version, so this API
 * can't move under us without a rebuild (see `scripts/build-ffmpeg-aar.sh`).
 */
internal object IntroFfmpegAudio {

    fun canDecode(mime: String): Boolean = FfmpegLibrary.isAvailable() && FfmpegLibrary.supportsFormat(mime)

    /**
     * Decodes the selected track of [extractor] up to [limitUs], handing [sink] 16-bit
     * interleaved PCM with its channel count and sample rate. False if it had to give up.
     */
    fun decode(
        format: MediaFormat,
        extractor: MediaExtractor,
        limitUs: Long,
        keepGoing: () -> Boolean,
        sink: (pcm: ShortBuffer, frames: Int, channels: Int, sampleRate: Int) -> Unit,
    ): Boolean {
        val maxInput = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 64 * 1024
        val decoder = FfmpegAudioDecoder(toMedia3(format), BUFFERS, BUFFERS, maxInput, false)
        try {
            var inputDone = false
            while (true) {
                if (!keepGoing()) return false
                var progressed = false
                if (!inputDone) {
                    decoder.dequeueInputBuffer()?.let { input ->
                        progressed = true
                        input.ensureSpaceForWrite(maxInput)
                        // MediaExtractor leaves position 0 and limit = the sample's size, which
                        // is what the decoder reads.
                        val size = extractor.readSampleData(input.data!!, 0)
                        if (size < 0 || extractor.sampleTime > limitUs) {
                            input.setFlags(C.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            input.timeUs = extractor.sampleTime
                            extractor.advance()
                        }
                        decoder.queueInputBuffer(input)
                    }
                }
                decoder.dequeueOutputBuffer()?.let { output ->
                    progressed = true
                    if (output.isEndOfStream) {
                        output.release()
                        return true
                    }
                    val data = output.data
                    if (data != null && !output.shouldBeSkipped) {
                        val channels = decoder.channelCount
                        val pcm = data.order(ByteOrder.nativeOrder()).asShortBuffer()
                        sink(pcm, pcm.remaining() / channels, channels, decoder.sampleRate)
                    }
                    output.release()
                }
                // The decoder works on its own thread; nothing moved, so give it a moment.
                if (!progressed) Thread.sleep(1)
            }
        } finally {
            decoder.release()
        }
    }

    private fun toMedia3(format: MediaFormat): Format {
        val initializationData = generateSequence(0) { it + 1 }
            .map { "csd-$it" }
            .takeWhile { format.containsKey(it) }
            .map { key -> format.getByteBuffer(key)!!.let { buffer -> ByteArray(buffer.remaining()).also { buffer.duplicate().get(it) } } }
            .toList()
        return Format.Builder()
            .setSampleMimeType(format.getString(MediaFormat.KEY_MIME))
            .setChannelCount(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
            .setSampleRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
            .setInitializationData(initializationData)
            .build()
    }

    private const val BUFFERS = 16
}
