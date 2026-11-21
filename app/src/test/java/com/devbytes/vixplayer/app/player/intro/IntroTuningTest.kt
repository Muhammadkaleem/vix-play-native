package com.devbytes.vixplayer.app.player.intro

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File

/**
 * Opt-in: runs intro detection on real episodes, for tuning. Skipped unless the environment
 * variable `INTRO_TUNING_DIR` names a folder of 16-bit mono WAVs at 5512 Hz, one per episode
 * in order — e.g. made with `afconvert -f WAVE -d LEI16@5512 -c 1 episode.mp4 e01.wav` — or of
 * the fingerprints a debug build keeps in `cache/intro/` (`adb exec-out run-as … cat`).
 * Prints what each pair shares and each episode's detected intro.
 */
class IntroTuningTest {

    @Test fun detectOnRealEpisodes() {
        val dir = System.getenv("INTRO_TUNING_DIR")?.let(::File)
        assumeTrue(dir != null && dir.isDirectory)
        // WAVs, or fingerprints a debug build dumped (`cache/intro/*.bin`, big-endian ints) —
        // openings only; the endings it dumps for credits (`*_end.bin`) are left out.
        val files = dir!!.listFiles { f -> (f.extension == "wav" || f.extension == "bin") && !f.nameWithoutExtension.endsWith("_end") }!!.sortedBy { it.name }
        val prints = files.map { if (it.extension == "bin") readCodes(it) else fingerprint(it) }
        for (i in files.indices) for (j in files.indices) if (i < j) {
            println("${files[i].name} × ${files[j].name}: ${IntroMatcher.shared(prints[i], prints[j])?.let(::describe) ?: "nothing"}")
        }
        // How alike each pair is along its best alignments: the longest run under several thresholds.
        for (i in files.indices) for (j in files.indices) if (i < j) {
            val offsets = IntroMatcher.candidateOffsets(prints[i], prints[j]).take(4)
            val report = offsets.joinToString("  ") { d ->
                val runs = listOf(9.0, 10.0, 11.0, 12.0, 13.0).joinToString(" ") { t -> "%.0f:%s".format(t, longestRun(prints[i], prints[j], d, t)) }
                "d=${d / 10.0}s [$runs]"
            }
            println("PAIR ${files[i].name} × ${files[j].name}: $report")
        }
        files.indices.forEach { i ->
            val neighbours = (maxOf(0, i - 2)..minOf(files.lastIndex, i + 2)).filter { it != i }
            println("${files[i].name}: intro ${IntroMatcher.introOf(i, prints, neighbours)}")
        }
    }

    /** Longest stretch (as "start+seconds" in a) whose 2 s average of differing bits stays ≤ [threshold]. */
    private fun longestRun(a: IntArray, b: IntArray, offset: Int, threshold: Double): String {
        val from = maxOf(0, -offset)
        val to = minOf(a.size, b.size - offset)
        val bits = IntArray(to - from) { Integer.bitCount(a[from + it] xor b[from + it + offset]) }
        var bestStart = 0; var bestLen = 0; var run = 0
        for (i in bits.indices) {
            val lo = maxOf(0, i - 10); val hi = minOf(bits.size, i + 11)
            var sum = 0; for (k in lo until hi) sum += bits[k]
            if (sum.toDouble() / (hi - lo) <= threshold) { run++; if (run > bestLen) { bestLen = run; bestStart = i - run + 1 } } else run = 0
        }
        return if (bestLen < 30) "-" else "${time(from + bestStart)}+${bestLen / 10}s"
    }

    private fun describe(s: SharedSegment) =
        "${time(s.aStart)}–${time(s.aEnd)} ↔ ${time(s.bStart)}–${time(s.bEnd)} (${s.length / 10.0} s)"

    private fun time(frame: Int) = "%d:%04.1f".format(frame / 600, (frame % 600) / 10.0)

    /** The first ten minutes of a 16-bit mono WAV (header skipped by finding the data chunk). */
    private fun fingerprint(file: File): IntArray {
        val bytes = DataInputStream(file.inputStream().buffered()).use { it.readBytes() }
        var at = 12
        while (at + 8 <= bytes.size && String(bytes, at, 4) != "data") {
            at += 8 + le32(bytes, at + 4)
        }
        val start = at + 8
        val count = minOf((bytes.size - start) / 2, 10 * 60 * AudioFingerprinter.SAMPLE_RATE)
        val samples = FloatArray(count) { i ->
            val lo = bytes[start + 2 * i].toInt() and 0xFF
            val hi = bytes[start + 2 * i + 1].toInt()
            ((hi shl 8) or lo) / 32768f
        }
        return AudioFingerprinter().apply { add(samples) }.codes()
    }

    private fun readCodes(file: File): IntArray = DataInputStream(file.inputStream().buffered()).use { input ->
        IntArray((file.length() / 4).toInt()) { input.readInt() }
    }

    private fun le32(b: ByteArray, at: Int) =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or
            ((b[at + 2].toInt() and 0xFF) shl 16) or ((b[at + 3].toInt() and 0xFF) shl 24)
}
