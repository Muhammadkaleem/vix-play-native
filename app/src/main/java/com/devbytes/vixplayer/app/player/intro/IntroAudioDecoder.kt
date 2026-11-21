package com.devbytes.vixplayer.app.player.intro

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.media3.decoder.ffmpeg.IntroFfmpegAudio
import java.nio.ByteOrder

/**
 * Decodes the opening of an episode's audio and fingerprints it ([AudioFingerprinter]):
 * platform decoders via MediaExtractor + MediaCodec, mixed to mono and brought down to
 * [AudioFingerprinter.SAMPLE_RATE]. Null when the file has no audio track this device can
 * decode (AC-3/DTS on most phones) — that episode just takes no part in detection.
 */
object IntroAudioDecoder {

    private const val TAG = "IntroDetect"
    private const val TIMEOUT_US = 2_000L

    /** Fingerprints the first [limitMs] of [uri]; [keepGoing] is polled so the job can stop. */
    fun fingerprint(context: Context, uri: Uri, limitMs: Long, keepGoing: () -> Boolean): IntArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            // FFmpeg in-process first (AAC, MP3, Opus, FLAC, …): a few seconds where MediaCodec,
            // a round trip to the system codec process per ~21 ms, takes ~50 s. MediaCodec covers
            // what our FFmpeg build leaves out (AC-3 on phones that decode it).
            if (IntroFfmpegAudio.canDecode(mime)) {
                val fingerprinter = AudioFingerprinter()
                var downsampler: Downsampler? = null
                var mono = FloatArray(8192)
                val done = IntroFfmpegAudio.decode(format, extractor, limitMs * 1000, keepGoing) { pcm, frames, channels, rate ->
                    val ds = downsampler ?: Downsampler(rate).also { downsampler = it }
                    if (mono.size < frames) mono = FloatArray(frames)
                    for (i in 0 until frames) {
                        var sum = 0f
                        for (c in 0 until channels) sum += pcm.get()
                        mono[i] = sum / channels / 32768f
                    }
                    ds.feed(mono, frames, fingerprinter)
                }
                return if (done) fingerprinter.codes() else null
            }
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            return decode(extractor, codec, format, limitMs * 1000, keepGoing)
        } catch (e: Exception) {
            Log.i(TAG, "can't decode $uri: ${e.message}")
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun decode(
        extractor: MediaExtractor,
        codec: MediaCodec,
        inputFormat: MediaFormat,
        limitUs: Long,
        keepGoing: () -> Boolean,
    ): IntArray? {
        val fingerprinter = AudioFingerprinter()
        var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var downsampler = Downsampler(inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        var floatPcm = false
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var mono = FloatArray(8192)
        while (true) {
            if (!keepGoing()) return null
            // Feed every free input buffer, then drain every ready output; only wait when
            // neither moved. Even so this path is slow — a round trip to the system's codec
            // process per buffer, ~25–60 s for ten minutes — hence FFmpeg first, above.
            var progressed = false
            while (!inputDone) {
                val index = codec.dequeueInputBuffer(0)
                if (index < 0) break
                progressed = true
                val buffer = codec.getInputBuffer(index)!!
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0 || extractor.sampleTime > limitUs) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    inputDone = true
                } else {
                    codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
            val out = codec.dequeueOutputBuffer(info, if (progressed) 0 else TIMEOUT_US)
            when {
                out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val f = codec.outputFormat
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    downsampler = Downsampler(f.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                    floatPcm = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                }
                out >= 0 -> {
                    val buffer = codec.getOutputBuffer(out)!!.order(ByteOrder.nativeOrder())
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    val frames = if (floatPcm) info.size / 4 / channels else info.size / 2 / channels
                    if (mono.size < frames) mono = FloatArray(frames)
                    if (floatPcm) {
                        val samples = buffer.asFloatBuffer()
                        for (i in 0 until frames) {
                            var sum = 0f
                            for (c in 0 until channels) sum += samples.get()
                            mono[i] = sum / channels
                        }
                    } else {
                        val samples = buffer.asShortBuffer()
                        for (i in 0 until frames) {
                            var sum = 0f
                            for (c in 0 until channels) sum += samples.get()
                            mono[i] = sum / channels / 32768f
                        }
                    }
                    downsampler.feed(mono, frames, fingerprinter)
                    codec.releaseOutputBuffer(out, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || info.presentationTimeUs > limitUs) break
                }
            }
        }
        return fingerprinter.codes()
    }
}

/**
 * Brings mono audio down to [AudioFingerprinter.SAMPLE_RATE] by averaging each output
 * sample's span of input — a box filter, plenty for the 300–2000 Hz the fingerprint reads.
 */
internal class Downsampler(sourceRate: Int) {
    private val step = sourceRate.toDouble() / AudioFingerprinter.SAMPLE_RATE
    private var nextBoundary = step
    private var position = 0.0
    private var sum = 0.0
    private var count = 0
    private val out = FloatArray(4096)
    private var outCount = 0

    fun feed(samples: FloatArray, size: Int, sink: AudioFingerprinter) {
        for (i in 0 until size) {
            sum += samples[i]
            count++
            position += 1.0
            if (position >= nextBoundary) {
                out[outCount++] = (sum / count).toFloat()
                sum = 0.0
                count = 0
                nextBoundary += step
                if (outCount == out.size) {
                    sink.add(out, outCount)
                    outCount = 0
                }
            }
        }
        if (outCount > 0) {
            sink.add(out, outCount)
            outCount = 0
        }
    }
}
