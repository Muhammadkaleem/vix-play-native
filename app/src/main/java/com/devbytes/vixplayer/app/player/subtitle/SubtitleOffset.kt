package com.devbytes.vixplayer.app.player.subtitle

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleParser

/**
 * Mutable carrier for the active subtitle timing offset.
 *
 * The offset is read at *parse* time, and Media3 parses subtitles during extraction,
 * so changing it only affects data parsed afterwards — callers must re-prepare the
 * media source for a new value to take effect on already-buffered cues.
 *
 * Written from the main thread, read on Media3's loader threads, hence [Volatile].
 */
class SubtitleOffsetHolder {
    @Volatile
    var offsetUs: Long = 0L
}

/**
 * Wraps a [SubtitleParser.Factory] so every parser it produces shifts cue timings by
 * the current value in [holder].
 *
 * This is the one supported seam for re-timing cues: [androidx.media3.exoplayer.text.TextRenderer]
 * is final, so the renderer's clock can't be shifted directly. Working at the parser
 * level covers embedded *and* external tracks and shifts in both directions, since the
 * cues are re-stamped before the renderer ever schedules them.
 */
class OffsetSubtitleParserFactory(
    private val holder: SubtitleOffsetHolder,
    private val delegate: SubtitleParser.Factory = DefaultSubtitleParserFactory(),
) : SubtitleParser.Factory {

    override fun supportsFormat(format: Format): Boolean = delegate.supportsFormat(format)

    override fun getCueReplacementBehavior(format: Format): Int =
        delegate.getCueReplacementBehavior(format)

    override fun create(format: Format): SubtitleParser =
        OffsetSubtitleParser(delegate.create(format), holder)
}

/** Delegating parser that re-stamps each [CuesWithTiming] as it is emitted. */
private class OffsetSubtitleParser(
    private val delegate: SubtitleParser,
    private val holder: SubtitleOffsetHolder,
) : SubtitleParser {

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: androidx.media3.common.util.Consumer<CuesWithTiming>,
    ) {
        // Snapshot once per parse so a mid-parse change can't split a file's timing.
        val shiftUs = holder.offsetUs
        delegate.parse(data, offset, length, outputOptions) { cues ->
            output.accept(cues.shiftedBy(shiftUs))
        }
    }

    override fun getCueReplacementBehavior(): Int = delegate.getCueReplacementBehavior()

    override fun reset() = delegate.reset()

    override fun parseToLegacySubtitle(data: ByteArray, offset: Int, length: Int): Subtitle =
        delegate.parseToLegacySubtitle(data, offset, length)
}

/**
 * Returns a copy shifted by [shiftUs], clamped at zero so a negative offset can't push
 * a cue before the start of the stream. Duration is preserved, so cues keep their
 * on-screen time; only their entry point moves.
 */
private fun CuesWithTiming.shiftedBy(shiftUs: Long): CuesWithTiming {
    if (shiftUs == 0L || startTimeUs == C.TIME_UNSET) return this
    return CuesWithTiming(cues, (startTimeUs + shiftUs).coerceAtLeast(0L), durationUs)
}
