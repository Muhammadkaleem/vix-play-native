package com.devbytes.vixplayer.app.player.intro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthetic episodes: each opens with its own "cold open" of a different length, then the
 * same 30 s theme (a chord melody), then its own story. The detector must find the theme in
 * every one, wherever it sits — from the audio alone.
 */
class IntroMatcherTest {

    private val rate = AudioFingerprinter.SAMPLE_RATE

    /**
     * A theme as real ones are: a melody that doesn't simply loop, over a broadband bed (a
     * fixed noise pattern, like percussion) — the same samples in every episode.
     */
    private fun theme(seconds: Int): FloatArray {
        val notes = Random(99).let { r -> DoubleArray(seconds * 3) { 330.0 + r.nextInt(12) * 40.0 } }
        val bed = Random(98)
        return FloatArray(seconds * rate) { i ->
            val t = i.toDouble() / rate
            val f = notes[(t * 3).toInt() % notes.size]
            (0.35 * sin(2 * PI * f * t) + 0.2 * sin(2 * PI * f * 1.5 * t) + 0.25 * (bed.nextDouble() - 0.5)).toFloat()
        }
    }

    /** Unrelated sound: noise shaped by a slowly wandering tone, different per seed. */
    private fun story(seconds: Int, seed: Int): FloatArray {
        val random = Random(seed)
        var f = 300.0 + random.nextDouble() * 1000
        return FloatArray(seconds * rate) { i ->
            if (i % rate == 0) f = 300.0 + random.nextDouble() * 1500
            (0.3 * sin(2 * PI * f * i / rate) + 0.3 * (random.nextDouble() - 0.5)).toFloat()
        }
    }

    private fun episode(coldOpen: Int, seed: Int, themeNoise: Double = 0.0): IntArray {
        val random = Random(seed * 31)
        val tune = theme(30).map { (it + themeNoise * (random.nextDouble() - 0.5)).toFloat() }.toFloatArray()
        val parts = listOf(story(coldOpen, seed), tune, story(60, seed + 100))
        return AudioFingerprinter().apply { parts.forEach { add(it) } }.codes()
    }

    @Test fun fingerprintRunsAtTenCodesASecond() {
        val codes = AudioFingerprinter().apply { add(theme(10)) }.codes()
        assertTrue("got ${codes.size}", codes.size in 95..100)
    }

    @Test fun twoEpisodesShareTheirThemeWhereverItSits() {
        val a = episode(coldOpen = 20, seed = 1)
        val b = episode(coldOpen = 75, seed = 2)
        val shared = IntroMatcher.shared(a, b)
        assertNotNull(shared)
        shared!!
        // Theme is at 20–50 s in A and 75–105 s in B; allow ±1.5 s at the edges.
        assertEquals(200.0, shared.aStart.toDouble(), 15.0)
        assertEquals(500.0, shared.aEnd.toDouble(), 15.0)
        assertEquals(750.0, shared.bStart.toDouble(), 15.0)
    }

    @Test fun unrelatedEpisodesShareNothing() =
        assertNull(IntroMatcher.shared(AudioFingerprinter().apply { add(story(120, 7)) }.codes(), AudioFingerprinter().apply { add(story(120, 8)) }.codes()))

    @Test fun aSeasonGivesEachEpisodeItsOwnWindow() {
        val opens = listOf(10, 45, 0, 90)
        val season = opens.mapIndexed { i, open -> episode(open, seed = 10 + i, themeNoise = 0.05) }
        opens.forEachIndexed { i, open ->
            val window = IntroMatcher.introOf(i, season, season.indices.toList())
            assertNotNull("episode $i", window)
            // Within 2.5 s of the theme, and never more than 1 s into the story either side:
            // trimming a little theme is fine, skipping story isn't.
            val start = window!!.startMs
            val end = window.endMs
            assertTrue("episode $i starts at $start", start - open * 1000L in -1000..2500)
            assertTrue("episode $i ends at $end", (open + 30) * 1000L - end in -1000..2500)
        }
    }

    @Test fun noOthersMeansNoIntro() = assertNull(IntroMatcher.introOf(0, listOf(episode(10, 1)), listOf(0)))
}
