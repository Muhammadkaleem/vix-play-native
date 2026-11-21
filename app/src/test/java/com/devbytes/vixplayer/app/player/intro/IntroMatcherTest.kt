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
    private fun theme(seconds: Int, seed: Int = 99): FloatArray {
        val notes = Random(seed).let { r -> DoubleArray(seconds * 3) { 330.0 + r.nextInt(12) * 40.0 } }
        val bed = Random(seed - 1)
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

    // ── End credits ───────────────────────────────────────────────────────────────────────

    /** The scanned ending of an episode: its own story, the shared credits music, then an optional tag. */
    private fun ending(story: Int, credits: Int, tag: Int, seed: Int): IntArray {
        val parts = listOf(story(story, seed), theme(credits, seed = 77), story(tag, seed + 50)).filter { it.isNotEmpty() }
        return AudioFingerprinter().apply { parts.forEach { add(it) } }.codes()
    }

    @Test fun creditsAreFoundInEachEpisodesEnding() {
        // (story before the credits, tag after) — every ending a different shape.
        val shapes = listOf(120 to 0, 150 to 20, 90 to 45, 140 to 10)
        val endings = shapes.mapIndexed { i, (story, tag) -> ending(story, credits = 60, tag = tag, seed = 20 + i) }
        val scanStart = 40 * 60_000L // each ending was decoded from 40:00 into its file
        shapes.forEachIndexed { i, (story, tag) ->
            val duration = scanStart + (story + 60 + tag) * 1000L
            val credits = IntroMatcher.creditsOf(i, endings, endings.map { scanStart }, duration, endings.indices.toList())
            assertNotNull("episode $i", credits)
            val expected = scanStart + story * 1000L
            assertTrue("episode $i credits start at ${credits!!.startMs}, expected $expected", credits.startMs - expected in -1000..2500)
        }
    }

    @Test fun aLongCreditRollIsStillFound() {
        // Longer than an intro may be (3 min): credits use their own bounds.
        val endings = listOf(60, 80, 40).mapIndexed { i, story -> ending(story, credits = 200, tag = 0, seed = 40 + i) }
        val credits = IntroMatcher.creditsOf(0, endings, listOf(0L, 0L, 0L), 260_000L, listOf(1, 2))
        assertNotNull(credits)
        assertEquals(60_000.0, credits!!.startMs.toDouble(), 2_500.0)
    }

    @Test fun sharedMusicFarFromTheEndIsNotCredits() {
        // The same 40 s cue, then three minutes of each episode's own story: a recurring
        // score cue, not the credits — nothing should offer to cut those minutes.
        val endings = listOf(30, 50, 20).mapIndexed { i, story -> ending(story, credits = 40, tag = 180, seed = 60 + i) }
        val durations = listOf(30, 50, 20).map { (it + 40 + 180) * 1000L }
        endings.indices.forEach { i ->
            assertNull("episode $i", IntroMatcher.creditsOf(i, endings, endings.map { 0L }, durations[i], endings.indices.toList()))
        }
    }
}
