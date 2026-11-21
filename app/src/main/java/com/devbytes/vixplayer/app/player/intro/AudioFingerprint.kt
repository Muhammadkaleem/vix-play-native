package com.devbytes.vixplayer.app.player.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * A compact audio fingerprint: one 32-bit code per [HOP_MS] of sound, after the classic
 * band-energy method (Haitsma & Kalker). Each bit says whether the energy difference between
 * two neighbouring frequency bands rose or fell since the previous frame — so it survives
 * volume changes, different encodes and mild noise, and identical music (a show's opening
 * theme) gives nearly identical codes in every episode.
 *
 * Feed mono samples at [SAMPLE_RATE] with [add]; read the codes with [codes]. Pure Kotlin —
 * the decoding lives in [IntroAudioDecoder] — so it is unit-tested on the JVM.
 */
class AudioFingerprinter {

    private val window = FloatArray(FRAME) { (0.5 - 0.5 * cos(2 * PI * it / (FRAME - 1))).toFloat() }
    private val ring = FloatArray(FRAME)
    private var filled = 0
    private var sinceLastFrame = 0
    private val re = DoubleArray(FRAME)
    private val im = DoubleArray(FRAME)
    private var previous: DoubleArray? = null
    private val out = IntArrayBuilder()

    /** Bin range of each of the [BANDS] bands, log-spaced between [LOW_HZ] and [HIGH_HZ]. */
    private val bandEdges = IntArray(BANDS + 1) { i ->
        val hz = exp(ln(LOW_HZ) + (ln(HIGH_HZ) - ln(LOW_HZ)) * i / BANDS)
        (hz * FRAME / SAMPLE_RATE).toInt()
    }

    fun add(samples: FloatArray, count: Int = samples.size) {
        for (i in 0 until count) {
            // A sliding window kept in a ring: the oldest sample falls out at [filled % FRAME].
            ring[filled % FRAME] = samples[i]
            filled++
            sinceLastFrame++
            if (filled >= FRAME && sinceLastFrame >= HOP) {
                sinceLastFrame = 0
                frame()
            }
        }
    }

    fun codes(): IntArray = out.toArray()

    private fun frame() {
        val start = filled % FRAME
        for (i in 0 until FRAME) {
            re[i] = (ring[(start + i) % FRAME] * window[i]).toDouble()
            im[i] = 0.0
        }
        fft(re, im)
        val energy = DoubleArray(BANDS) { b ->
            var sum = 0.0
            for (bin in bandEdges[b] until maxOf(bandEdges[b + 1], bandEdges[b] + 1)) sum += re[bin] * re[bin] + im[bin] * im[bin]
            sum
        }
        val prev = previous
        if (prev != null) {
            var code = 0
            for (m in 0 until BANDS - 1) {
                val now = energy[m] - energy[m + 1]
                val before = prev[m] - prev[m + 1]
                if (now - before > 0) code = code or (1 shl m)
            }
            out.add(code)
        }
        previous = energy
    }

    companion object {
        /** Decoded audio is brought down to this rate: enough for 300–2000 Hz. */
        const val SAMPLE_RATE = 5512
        const val FRAME = 2048
        /** One code per this many samples — 100 ms. */
        const val HOP = 551
        const val HOP_MS = 100L
        /** 33 bands give 32 differences, one per bit. */
        const val BANDS = 33
        private const val LOW_HZ = 300.0
        private const val HIGH_HZ = 2000.0
    }
}

/** In-place iterative radix-2 FFT; [re].size must be a power of two. */
internal fun fft(re: DoubleArray, im: DoubleArray) {
    val n = re.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            var t = re[i]; re[i] = re[j]; re[j] = t
            t = im[i]; im[i] = im[j]; im[j] = t
        }
    }
    var len = 2
    while (len <= n) {
        val angle = -2 * PI / len
        val wr = cos(angle)
        val wi = sin(angle)
        var i = 0
        while (i < n) {
            var cr = 1.0
            var ci = 0.0
            for (k in 0 until len / 2) {
                val a = i + k
                val b = a + len / 2
                val tr = re[b] * cr - im[b] * ci
                val ti = re[b] * ci + im[b] * cr
                re[b] = re[a] - tr
                im[b] = im[a] - ti
                re[a] += tr
                im[a] += ti
                val nr = cr * wr - ci * wi
                ci = cr * wi + ci * wr
                cr = nr
            }
            i += len
        }
        len = len shl 1
    }
}

/** Growable IntArray without boxing. */
internal class IntArrayBuilder {
    private var data = IntArray(1024)
    var size = 0
        private set

    fun add(value: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = value
    }

    fun toArray(): IntArray = data.copyOf(size)
}
