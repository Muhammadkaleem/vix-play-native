package com.devbytes.vixplayer.app.player.subtitle

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.common.text.Cue
import androidx.media3.common.util.Consumer
import androidx.media3.extractor.text.CuesWithTiming
import io.github.peerless2012.ass.AssRender
import io.github.peerless2012.ass.AssTrack
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.extractor.AssMatroskaExtractor
import io.github.peerless2012.ass.media.type.AssRenderType

/**
 * Styled ASS/SSA subtitles through libass (ass-media's LEGACY mode).
 *
 * Media3's own SSA parser keeps the text and drops most styling: fonts, outlines, colours by
 * style, rotation, scaling, positioning tags. libass draws each line the way the file says, as a
 * bitmap cue, so the usual [androidx.media3.ui.SubtitleView] shows it — no video effects pipeline.
 *
 * **Only SSA tracks inside MKV go through libass** — where styled subtitles (anime, fansubs)
 * nearly always live, and where the file's attached fonts come with them. A separate `.ass` file
 * keeps Media3's renderer: ass-media would render the whole file into bitmaps before the video's
 * size is known, which is slow and heavy on memory for a two-hour film.
 *
 * Limits of LEGACY mode: each line is drawn once, at its start, so animated tags (karaoke,
 * `\move`, `\t`) show their first frame rather than moving.
 *
 * When the user turns on "Override embedded styles", SSA goes back to Media3's renderer so their
 * own style applies. That choice is read at extraction time, so changing it needs a re-prepare.
 */
@OptIn(UnstableApi::class)
class AssSupport {

    val handler = AssHandler(AssRenderType.LEGACY)

    /** True draws SSA with the user's style (Media3) instead of the file's (libass). */
    @Volatile
    var overrideEmbeddedStyles: Boolean = false

    private val defaults = DefaultSubtitleParserFactory()

    /** Picks libass for SSA in MKV, Media3 for everything else. */
    val parserFactory: SubtitleParser.Factory = object : SubtitleParser.Factory {
        override fun supportsFormat(format: Format): Boolean = defaults.supportsFormat(format)

        override fun getCueReplacementBehavior(format: Format): Int = defaults.getCueReplacementBehavior(format)

        override fun create(format: Format): SubtitleParser {
            val media3 = defaults.create(format)
            val inMkv = format.sampleMimeType == MimeTypes.TEXT_SSA && format.containerMimeType == MimeTypes.VIDEO_MATROSKA
            if (!inMkv) return media3
            val libass = LibassSubtitleParser(handler, handler.createTrack(format))
            // Chosen per line, not per track: the setting can arrive from DataStore after the
            // file has started loading, and should apply to whatever is read from then on.
            return object : SubtitleParser by media3 {
                override fun parse(
                    data: ByteArray,
                    offset: Int,
                    length: Int,
                    outputOptions: SubtitleParser.OutputOptions,
                    output: Consumer<CuesWithTiming>,
                ) = (if (overrideEmbeddedStyles) media3 else libass).parse(data, offset, length, outputOptions, output)

                override fun reset() {
                    media3.reset()
                    libass.reset()
                }
            }
        }
    }

    /**
     * The default extractors with Matroska swapped for ass-media's, which also reads the fonts
     * attached to the file. The rest of the settings Media3 pushes (the subtitle parser, parsing
     * during extraction) are passed on to the defaults.
     */
    val extractorsFactory: ExtractorsFactory = object : ExtractorsFactory {
        private val delegate = DefaultExtractorsFactory()
        private var subtitleParserFactory: SubtitleParser.Factory = parserFactory
        private var transcoding = true

        override fun experimentalSetTextTrackTranscodingEnabled(enabled: Boolean): ExtractorsFactory {
            transcoding = enabled
            delegate.experimentalSetTextTrackTranscodingEnabled(enabled)
            return this
        }

        override fun setSubtitleParserFactory(factory: SubtitleParser.Factory): ExtractorsFactory {
            subtitleParserFactory = factory
            delegate.setSubtitleParserFactory(factory)
            return this
        }

        override fun experimentalSetCodecsToParseWithinGopSampleDependencies(codecFlags: Int): ExtractorsFactory {
            delegate.experimentalSetCodecsToParseWithinGopSampleDependencies(codecFlags)
            return this
        }

        override fun createExtractors(): Array<Extractor> = withAss(delegate.createExtractors())

        override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
            withAss(delegate.createExtractors(uri, responseHeaders))

        // ass-media's extractor always parses subtitles while extracting; when Media3 has that
        // turned off, keep its own Matroska extractor.
        private fun withAss(extractors: Array<Extractor>): Array<Extractor> =
            if (!transcoding) extractors else
                extractors.map { if (it is MatroskaExtractor) AssMatroskaExtractor(subtitleParserFactory, handler) else it }
                    .toTypedArray()
    }

    /** Hooks the handler to the player's track and size changes. Call once, after building it. */
    fun attach(player: ExoPlayer) = handler.init(player)
}

/**
 * Draws the SSA lines of one MKV subtitle track with libass, as bitmap cues the size of the video.
 *
 * ass-media's own LEGACY parser draws with the renderer of the track the player has selected;
 * lines parsed before the player reports its tracks (the opening ones, always) come out blank,
 * and with two SSA tracks one track's lines are drawn from the other. So each track gets its own
 * renderer here, made at the first line — after the file's attached fonts have been read, since
 * Matroska stores them before the first cluster.
 */
@OptIn(UnstableApi::class)
private class LibassSubtitleParser(
    private val handler: AssHandler,
    private val track: AssTrack,
) : SubtitleParser {

    private var render: AssRender? = null
    private var width = 0
    private var height = 0

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>,
    ) {
        // Media3 writes Matroska SSA times as h:mm:ss:cc; libass reads h:mm:ss.cc.
        val line = TIMESTAMP.replace(String(data, offset, length, Charsets.UTF_8)) { "${it.groupValues[1]}.${it.groupValues[2]}" }
        track.readBuffer(line.toByteArray())
        val render = renderer()
        track.getEvents()?.forEach { event ->
            // A fade-in draws nothing at the line's start, so draw (and show) it once faded in.
            val (fadeIn, fadeOut) = FADE.find(event.text)?.destructured?.let { (i, o) -> i.toLong() to o.toLong() } ?: (0L to 0L)
            val images = render.renderFrame(event.start + fadeIn, false)?.images ?: return@forEach
            val cues = images.map { tex ->
                Cue.Builder()
                    .setBitmap(tex.bitmap)
                    .setPosition(tex.x / width.toFloat())
                    .setPositionAnchor(Cue.ANCHOR_TYPE_START)
                    .setLine(tex.y / height.toFloat(), Cue.LINE_TYPE_FRACTION)
                    .setLineAnchor(Cue.ANCHOR_TYPE_START)
                    .setSize(tex.bitmap.width / width.toFloat())
                    .setBitmapHeight(tex.bitmap.height / height.toFloat())
                    .build()
            }
            val durationMs = (event.duration - fadeIn - fadeOut).coerceAtLeast(1)
            output.accept(CuesWithTiming(cues, (event.start + fadeIn) * 1000, durationMs * 1000))
        }
        track.clearEvent()
    }

    private fun renderer(): AssRender = render ?: handler.ass.createRender().also { render ->
        // The video's size, read from the file before any subtitle; the script's own canvas if not.
        val video = handler.videoSize
        width = video.width.takeIf { it > 0 } ?: track.getWidth().takeIf { it > 0 } ?: 1920
        height = video.height.takeIf { it > 0 } ?: track.getHeight().takeIf { it > 0 } ?: 1080
        render.setStorageSize(width, height)
        render.setFrameSize(width, height)
        render.setTrack(track)
        this.render = render
    }

    override fun getCueReplacementBehavior(): Int = Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE

    private companion object {
        val TIMESTAMP = Regex("(\\d+:\\d{2}:\\d{2}):(\\d{2})")
        val FADE = Regex("""\\fad\((\d+),(\d+)\)""")
    }
}
