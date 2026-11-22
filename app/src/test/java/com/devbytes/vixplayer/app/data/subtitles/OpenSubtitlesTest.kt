package com.devbytes.vixplayer.app.data.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSubtitlesTest {

    @Test
    fun `hash matches the reference algorithm`() {
        val head = ByteArray(OpenSubtitles.HASH_CHUNK) { ((it * 7 + 3) and 0xFF).toByte() }
        val tail = ByteArray(OpenSubtitles.HASH_CHUNK) { ((it * 13 + 5) and 0xFF).toByte() }
        // From an independent Python implementation (struct '<Q', wrapping 64-bit sum).
        assertEquals("20a01fa01fa30d40", OpenSubtitles.hash(200_000, head, tail))
    }

    @Test
    fun `hash needs at least 64 KB`() {
        assertNull(OpenSubtitles.hash(1_000, ByteArray(1_000), ByteArray(1_000)))
    }

    @Test
    fun `search url is sorted, lowercase and plus-encoded`() {
        val url = OpenSubtitles.searchUrl(SubtitleQuery("The Office", season = 2, episode = 5), listOf("EN"), null)
        assertEquals(
            OpenSubtitles.BASE_URL + "/subtitles?episode_number=5&languages=en&query=the+office&season_number=2&type=episode",
            url,
        )
    }

    @Test
    fun `hash search carries no query`() {
        val url = OpenSubtitles.searchUrl(SubtitleQuery(""), listOf("en", "fr"), "20a01fa01fa30d40")
        assertEquals(OpenSubtitles.BASE_URL + "/subtitles?languages=en%2Cfr&moviehash=20a01fa01fa30d40", url)
    }

    @Test
    fun `an episode file searches for its show, season and episode`() {
        val q = OpenSubtitles.queryFor("Harbour.Lights.S01E04.Storm.Season.1080p.mp4")
        assertEquals(2, listOf(q.season, q.episode).count { it != null })
        assertEquals(1, q.season)
        assertEquals(4, q.episode)
        assertTrue(q.text.contains("Harbour Lights", ignoreCase = true))
    }

    @Test
    fun `a film file searches for its title and year`() {
        val q = OpenSubtitles.queryFor("City.After.Rain.2023.1080p.WEB-DL.x264.mkv")
        assertEquals("City After Rain", q.text)
        assertEquals(2023, q.year)
        assertNull(q.season)
    }

    @Test
    fun `a plain name stays as it is`() {
        val q = OpenSubtitles.queryFor("Neon Avenue.mp4")
        assertEquals("Neon Avenue", q.text)
        assertNull(q.year)
    }

    @Test
    fun `results put the exact release first, then the most downloaded`() {
        val json = """
            {"total_count":3,"data":[
              {"attributes":{"language":"en","release":"Popular","download_count":900,"hearing_impaired":false,
                "files":[{"file_id":1,"file_name":"a.srt"}]}},
              {"attributes":{"language":"en","release":"Exact","download_count":10,"moviehash_match":true,
                "files":[{"file_id":2,"file_name":"b.srt"}]}},
              {"attributes":{"language":"en","release":"Robot","download_count":50,"machine_translated":true,
                "files":[{"file_id":3,"file_name":"c.srt"}]}},
              {"attributes":{"language":"en","release":"No files","download_count":5000,"files":[]}}
            ]}
        """.trimIndent()
        val results = OpenSubtitles.parseSearch(json)
        assertEquals(listOf(2L, 1L, 3L), results.map { it.fileId })
        assertTrue(results[0].hashMatch)
        assertTrue(results[2].translated)
        assertFalse(results[1].translated)
    }

    @Test
    fun `format comes from the name, else the content`() {
        assertEquals(SubtitleFormat.SSA, OpenSubtitles.formatOf("Show.ass", ""))
        assertEquals(SubtitleFormat.SRT, OpenSubtitles.formatOf("Show.srt", "[Script Info]"))
        assertEquals(SubtitleFormat.SSA, OpenSubtitles.formatOf("Show", "﻿[Script Info]\nTitle: x"))
        assertEquals(SubtitleFormat.VTT, OpenSubtitles.formatOf("Show", "WEBVTT\n\n"))
        assertEquals(SubtitleFormat.SRT, OpenSubtitles.formatOf("Show", "1\n00:00:01,000 --> 00:00:02,000\nHi"))
    }
}
