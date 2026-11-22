package com.devbytes.vixplayer.app.player.intro

import com.devbytes.vixplayer.app.player.CreditsWindow
import com.devbytes.vixplayer.app.player.IntroWindow

/** A stretch two episodes share: frames [aStart, aEnd) of one at [bStart, bEnd) of the other. */
data class SharedSegment(val aStart: Int, val aEnd: Int, val bStart: Int, val bEnd: Int) {
    val length: Int get() = aEnd - aStart
}

/** How long a shared stretch may be to count, in fingerprint frames. */
class MatchBounds(val minFrames: Int, val maxFrames: Int)

/**
 * Finds a show's intro from the audio fingerprints ([AudioFingerprinter]) of its episodes'
 * openings: the theme is the longest stretch of near-identical sound that episodes share,
 * wherever it sits in each one (cold opens vary in length). The end credits are found the
 * same way in the episodes' endings ([creditsOf]). Pure — unit-tested on the JVM.
 */
object IntroMatcher {

    /** Frames per second of fingerprint. */
    private const val FPS = (1000 / AudioFingerprinter.HOP_MS).toInt()
    /**
     * Shorter shared stretches are logos, stings or a slice of a title song that plays under
     * each episode's own scenes (Peaky Blinders shares only 10–15 s pieces that way) — too
     * little to be worth a skip, and the manual mark covers those shows.
     */
    const val MIN_FRAMES = 15 * FPS
    /** Longer ones are a mistake (the same episode twice, a recap of the whole thing). */
    const val MAX_FRAMES = 3 * 60 * FPS
    val INTRO = MatchBounds(MIN_FRAMES, MAX_FRAMES)
    /** Credit rolls run longer than title sequences; a stretch the length of the whole scanned ending is a duplicate file. */
    val CREDITS = MatchBounds(MIN_FRAMES, 5 * 60 * FPS)
    /**
     * Credits run to the end of the file, give or take a short tag or a "next time" preview.
     * Something shared that stops earlier than this is a recurring music cue, not the credits.
     */
    const val CREDITS_END_SLACK_MS = 2 * 60_000L
    /** Average differing bits (of 32) under which two stretches count as the same sound. */
    private const val MATCH_BITS = 10.0
    /** Smoothing window for the bit error, and the largest gap bridged inside one match. */
    private const val SMOOTH = 2 * FPS + 1
    private const val MAX_GAP = 2 * FPS
    /** A frame pair this close (differing bits) counts towards placing an alignment. */
    private const val CLOSE_BITS = 9
    /** Of a 10 s stretch, at least this percent of frames must be close to try an alignment. */
    private const val MIN_CLOSE = 25
    private const val STEP = 2
    /** Music repeats (bars, choruses), so shifted alignments match too: try several, keep the longest. */
    private const val CANDIDATE_OFFSETS = 12

    /** The longest stretch [a] and [b] share, or null if there's none within [bounds]. */
    fun shared(a: IntArray, b: IntArray, bounds: MatchBounds = INTRO): SharedSegment? {
        var best: SharedSegment? = null
        for (offset in candidateOffsets(a, b)) {
            val segment = longestMatch(a, b, offset, bounds) ?: continue
            if (best == null || segment.length > best.length) best = segment
        }
        return best
    }

    /**
     * The intro of episode [index] in [episodes] (fingerprints, frame 0 = start of the file),
     * from what it shares with the [others] it's compared against — the stretch most of them
     * agree on. Null when nothing is shared, or the comparisons disagree.
     */
    fun introOf(index: Int, episodes: List<IntArray>, others: List<Int>): IntroWindow? {
        val (start, end) = agreedRange(index, episodes, others, INTRO) ?: return null
        return IntroWindow(start * AudioFingerprinter.HOP_MS, end * AudioFingerprinter.HOP_MS)
    }

    /**
     * An episode's opening extended by a later span: [head] from the start of the file, [tail]
     * from [tailStartMs] — where the seek landed, at or before the end of [head]. Frame 0 stays
     * the file's start, so [introOf] still gives times in the file. Null if the two don't meet.
     */
    fun joinSpans(head: IntArray, tail: IntArray, tailStartMs: Long): IntArray? {
        val cut = (tailStartMs / AudioFingerprinter.HOP_MS).toInt()
        if (cut < 0 || cut > head.size) return null
        return head.copyOf(cut) + tail
    }

    /**
     * The end credits of episode [index], from [endings] — fingerprints of each episode's
     * last minutes, code 0 at [startsMs] into that file — and the [others] it's compared
     * against: the shared stretch most of them agree on, provided it runs to within
     * [CREDITS_END_SLACK_MS] of the episode's end ([durationMs]). Null otherwise.
     */
    fun creditsOf(index: Int, endings: List<IntArray>, startsMs: List<Long>, durationMs: Long, others: List<Int>): CreditsWindow? {
        val (start, end) = agreedRange(index, endings, others, CREDITS) ?: return null
        val startMs = startsMs[index] + start * AudioFingerprinter.HOP_MS
        val endMs = startsMs[index] + end * AudioFingerprinter.HOP_MS
        if (durationMs > 0 && durationMs - endMs > CREDITS_END_SLACK_MS) return null
        return CreditsWindow(startMs, endMs)
    }

    /** Frames [start, end) of [index] that most of the comparisons with [others] agree are shared. */
    private fun agreedRange(index: Int, episodes: List<IntArray>, others: List<Int>, bounds: MatchBounds): Pair<Int, Int>? {
        val found = others.filter { it != index }.mapNotNull { other -> shared(episodes[index], episodes[other], bounds) }
        if (found.isEmpty()) return null
        val ranges = found.map { it.aStart to it.aEnd }
        // Each candidate's support: the others that cover at least half of it.
        val supported = ranges.map { r -> ranges.filter { o -> o !== r && overlap(r, o) * 2 >= r.second - r.first } }
        val pick = ranges.indices.maxWith(compareBy<Int>({ supported[it].size }, { ranges[it].second - ranges[it].first }))
        if (ranges.size >= 3 && supported[pick].isEmpty()) return null // three or more comparisons, no two agree
        val group = supported[pick] + ranges[pick]
        val start = group.map { it.first }.sorted()[group.size / 2]
        val end = group.map { it.second }.sorted()[group.size / 2]
        if (end - start < bounds.minFrames) return null
        return start to end
    }

    /**
     * The alignments (b index − a index) where some 10 s of the two sound closest. Every
     * alignment is tried: exact code matches are too rare between two encodes of the same
     * theme to vote with (about 9 of 32 bits differ), but "close" codes cluster tightly where
     * the theme lines up and almost never elsewhere. Every [STEP]th frame is enough to place it.
     */
    internal fun candidateOffsets(a: IntArray, b: IntArray): List<Int> {
        val window = MIN_FRAMES / STEP
        val scored = ArrayList<Pair<Int, Int>>()
        val hits = IntArray(window)
        for (offset in -(a.size - MIN_FRAMES)..(b.size - MIN_FRAMES)) {
            val from = maxOf(0, -offset)
            val to = minOf(a.size, b.size - offset)
            var count = 0
            var best = 0
            var n = 0
            var i = from
            while (i < to) {
                val hit = if (Integer.bitCount(a[i] xor b[i + offset]) <= CLOSE_BITS) 1 else 0
                val slot = n % window
                if (n >= window) count -= hits[slot]
                hits[slot] = hit
                count += hit
                if (count > best) best = count
                n++
                i += STEP
            }
            if (best >= MIN_CLOSE * window / 100) scored += offset to best
        }
        val chosen = ArrayList<Int>()
        for ((offset, _) in scored.sortedByDescending { it.second }) {
            if (chosen.size == CANDIDATE_OFFSETS) break
            if (chosen.none { kotlin.math.abs(it - offset) <= 2 }) chosen += offset
        }
        return chosen
    }

    /** The longest run where [a] and [b] (shifted by [offset]) sound the same. */
    private fun longestMatch(a: IntArray, b: IntArray, offset: Int, bounds: MatchBounds): SharedSegment? {
        val from = maxOf(0, -offset)
        val to = minOf(a.size, b.size - offset)
        if (to - from < bounds.minFrames) return null
        val bits = IntArray(to - from) { Integer.bitCount(a[from + it] xor b[from + it + offset]) }
        // Moving average over [SMOOTH] frames, centred.
        val half = SMOOTH / 2
        val prefix = LongArray(bits.size + 1).also { p -> bits.forEachIndexed { i, v -> p[i + 1] = p[i] + v } }
        val same = BooleanArray(bits.size) { i ->
            val lo = maxOf(0, i - half)
            val hi = minOf(bits.size, i + half + 1)
            (prefix[hi] - prefix[lo]).toDouble() / (hi - lo) <= MATCH_BITS
        }
        var bestStart = -1
        var bestEnd = -1
        var runStart = -1
        var lastSame = -1
        for (i in same.indices) {
            if (!same[i]) continue
            if (runStart < 0 || i - lastSame > MAX_GAP) runStart = i
            lastSame = i
            if (i + 1 - runStart > bestEnd - bestStart) {
                bestStart = runStart
                bestEnd = i + 1
            }
        }
        if (bestStart < 0 || bestEnd - bestStart < bounds.minFrames || bestEnd - bestStart > bounds.maxFrames) return null
        val aStart = from + bestStart
        val aEnd = from + bestEnd
        return SharedSegment(aStart, aEnd, aStart + offset, aEnd + offset)
    }

    private fun overlap(a: Pair<Int, Int>, b: Pair<Int, Int>): Int =
        maxOf(0, minOf(a.second, b.second) - maxOf(a.first, b.first))
}
