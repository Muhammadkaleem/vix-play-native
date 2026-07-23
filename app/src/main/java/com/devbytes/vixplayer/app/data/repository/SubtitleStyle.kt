package com.devbytes.vixplayer.app.data.repository

import android.graphics.Color

/**
 * Typeface choices. The four system families rather than a font picker — Media3 takes a
 * `Typeface`, and bundling or scanning custom fonts is a separate feature.
 */
enum class SubtitleTypeface(val label: String) {
    DEFAULT("Default"),
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Monospace"),
}

/**
 * Text colours offered as a fixed palette rather than free RGB: every option stays
 * high-contrast against the dark backgrounds subtitles sit on, so no combination can
 * produce unreadable captions.
 */
object SubtitleColors {
    const val WHITE = 0xFFFFFFFF.toInt()
    const val YELLOW = 0xFFFFEB3B.toInt()
    const val CYAN = 0xFF4DD0E1.toInt()
    const val GREEN = 0xFF81C784.toInt()

    val TEXT_SWATCHES = listOf(
        WHITE to "White",
        YELLOW to "Yellow",
        CYAN to "Cyan",
        GREEN to "Green",
    )

    /** Box behind the text, from fully clear to solid. */
    val BACKGROUND_SWATCHES = listOf(
        Color.TRANSPARENT to "None",
        0x80000000.toInt() to "Dim",
        0xCC000000.toInt() to "Dark",
        0xFF000000.toInt() to "Solid",
    )
}

/** Edge treatments, mapped to `CaptionStyleCompat.EDGE_TYPE_*` at apply time. */
object SubtitleEdge {
    const val NONE = 0
    const val OUTLINE = 1
    const val DROP_SHADOW = 2

    val OPTIONS = listOf(
        NONE to "None",
        OUTLINE to "Outline",
        DROP_SHADOW to "Shadow",
    )
}

enum class SubtitlePreset(val label: String) {
    NETFLIX("Netflix"),
    CINEMA("Cinema"),
    MINIMAL("Minimal"),
    CLASSIC("Classic"),
    CUSTOM("Custom"),
}

/**
 * User subtitle appearance.
 *
 * [overrideEmbedded] is the PRD's "respect or override embedded ASS styles". It defaults
 * to false so an intentionally-styled release renders as authored; the user's style then
 * applies to the SRT files that carry no styling of their own, which is most of them.
 *
 * Text size is deliberately exempt: it is applied even when embedded styles are
 * respected (see `applyToSubtitleView`), because "subtitles are too small" is the most
 * common complaint and it would otherwise silently do nothing on the very files people
 * are complaining about.
 */
data class SubtitleStyle(
    val preset: SubtitlePreset,
    val textSizeFraction: Float,
    val bottomPaddingFraction: Float,
    val textColor: Int,
    val backgroundColor: Int,
    val edgeType: Int,
    val typeface: SubtitleTypeface,
    val overrideEmbedded: Boolean,
) {
    companion object {
        /** Media3's own defaults, so an untouched install renders exactly as before. */
        val DEFAULT = SubtitleStyle(
            preset = SubtitlePreset.CLASSIC,
            textSizeFraction = 0.0533f,
            bottomPaddingFraction = 0.08f,
            textColor = SubtitleColors.WHITE,
            backgroundColor = Color.TRANSPARENT,
            edgeType = SubtitleEdge.OUTLINE,
            typeface = SubtitleTypeface.DEFAULT,
            overrideEmbedded = false,
        )

        /** Preset definitions. Selecting one replaces every visual property. */
        fun from(preset: SubtitlePreset, current: SubtitleStyle): SubtitleStyle = when (preset) {
            SubtitlePreset.NETFLIX -> current.copy(
                preset = preset,
                textSizeFraction = 0.0600f,
                textColor = SubtitleColors.WHITE,
                backgroundColor = Color.TRANSPARENT,
                edgeType = SubtitleEdge.DROP_SHADOW,
                typeface = SubtitleTypeface.SANS,
            )
            SubtitlePreset.CINEMA -> current.copy(
                preset = preset,
                textSizeFraction = 0.0700f,
                textColor = SubtitleColors.YELLOW,
                backgroundColor = Color.TRANSPARENT,
                edgeType = SubtitleEdge.OUTLINE,
                typeface = SubtitleTypeface.SERIF,
            )
            SubtitlePreset.MINIMAL -> current.copy(
                preset = preset,
                textSizeFraction = 0.0480f,
                textColor = SubtitleColors.WHITE,
                backgroundColor = Color.TRANSPARENT,
                edgeType = SubtitleEdge.NONE,
                typeface = SubtitleTypeface.SANS,
            )
            SubtitlePreset.CLASSIC -> current.copy(
                preset = preset,
                textSizeFraction = 0.0533f,
                textColor = SubtitleColors.WHITE,
                backgroundColor = 0xCC000000.toInt(),
                edgeType = SubtitleEdge.NONE,
                typeface = SubtitleTypeface.DEFAULT,
            )
            SubtitlePreset.CUSTOM -> current.copy(preset = preset)
        }
    }
}
