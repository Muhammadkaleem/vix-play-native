package com.devbytes.vixplayer.app.ui.player.components

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.devbytes.vixplayer.app.data.repository.SubtitleColors
import com.devbytes.vixplayer.app.data.repository.SubtitleEdge
import com.devbytes.vixplayer.app.data.repository.SubtitlePreset
import com.devbytes.vixplayer.app.data.repository.SubtitleStyle
import com.devbytes.vixplayer.app.data.repository.SubtitleTypeface

/**
 * Shared subtitle-appearance editor, hosted both full-screen in Settings and as a bottom
 * sheet over the player. Both write the same DataStore-backed [SubtitleStyle], so there
 * is one source of truth regardless of where it was edited.
 *
 * [showPreview] is false in the player, where the real subtitles on the video are a far
 * better preview than a sample string.
 */
@Composable
fun SubtitleStyleEditor(
    style: SubtitleStyle,
    onChange: (SubtitleStyle) -> Unit,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        if (showPreview) {
            SubtitlePreviewStrip(style)
        }

        SectionLabel("Preset")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitlePreset.entries.filter { it != SubtitlePreset.CUSTOM }.forEach { preset ->
                FilterChip(
                    selected = style.preset == preset,
                    onClick = { onChange(SubtitleStyle.from(preset, style)) },
                    label = { Text(preset.label) },
                )
            }
        }

        SectionLabel("Text size")
        Slider(
            value = style.textSizeFraction,
            onValueChange = { onChange(style.custom(textSizeFraction = it)) },
            valueRange = 0.03f..0.12f,
        )

        SectionLabel("Position")
        Text(
            text = "Distance from the bottom of the video.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = style.bottomPaddingFraction,
            onValueChange = { onChange(style.custom(bottomPaddingFraction = it)) },
            valueRange = 0.0f..0.30f,
        )

        SectionLabel("Text colour")
        SwatchRow(
            swatches = SubtitleColors.TEXT_SWATCHES,
            selected = style.textColor,
            onSelect = { onChange(style.custom(textColor = it)) },
        )

        SectionLabel("Background")
        SwatchRow(
            swatches = SubtitleColors.BACKGROUND_SWATCHES,
            selected = style.backgroundColor,
            onSelect = { onChange(style.custom(backgroundColor = it)) },
        )

        SectionLabel("Edge")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitleEdge.OPTIONS.forEach { (value, label) ->
                FilterChip(
                    selected = style.edgeType == value,
                    onClick = { onChange(style.custom(edgeType = value)) },
                    label = { Text(label) },
                )
            }
        }

        SectionLabel("Font")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitleTypeface.entries.forEach { face ->
                FilterChip(
                    selected = style.typeface == face,
                    onClick = { onChange(style.custom(typeface = face)) },
                    label = { Text(face.label) },
                )
            }
        }

        SectionLabel("Embedded styles")
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Override embedded styles",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "ASS/SSA subtitles carry their own fonts and colours. " +
                        "Leave off to show them as authored. Text size always follows " +
                        "your setting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = style.overrideEmbedded,
                onCheckedChange = { onChange(style.copy(overrideEmbedded = it)) },
            )
        }
    }
}

/** Any manual edit drops the preset label to Custom. */
private fun SubtitleStyle.custom(
    textSizeFraction: Float = this.textSizeFraction,
    bottomPaddingFraction: Float = this.bottomPaddingFraction,
    textColor: Int = this.textColor,
    backgroundColor: Int = this.backgroundColor,
    edgeType: Int = this.edgeType,
    typeface: SubtitleTypeface = this.typeface,
) = copy(
    preset = SubtitlePreset.CUSTOM,
    textSizeFraction = textSizeFraction,
    bottomPaddingFraction = bottomPaddingFraction,
    textColor = textColor,
    backgroundColor = backgroundColor,
    edgeType = edgeType,
    typeface = typeface,
)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun SwatchRow(
    swatches: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { (value, label) ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    // Light underlay so translucent swatches read as different: the
                    // background options are black at 0%/50%/80%/100% alpha, which are
                    // indistinguishable when painted straight onto a near-black surface.
                    .background(Color(0xFFBDBDBD))
                    .border(
                        width = if (value == selected) 3.dp else 1.dp,
                        color = if (value == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(value)),
                )
                if (value == android.graphics.Color.TRANSPARENT) {
                    Text(
                        text = label.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Sample caption over a dark strip, so the settings screen shows the effect directly. */
@Composable
private fun SubtitlePreviewStrip(style: SubtitleStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "The quick brown fox",
            color = Color(style.textColor),
            fontSize = (style.textSizeFraction * 320).sp,
            fontFamily = when (style.typeface) {
                SubtitleTypeface.SERIF -> FontFamily.Serif
                SubtitleTypeface.MONO -> FontFamily.Monospace
                SubtitleTypeface.SANS -> FontFamily.SansSerif
                SubtitleTypeface.DEFAULT -> FontFamily.Default
            },
            modifier = Modifier
                .background(Color(style.backgroundColor))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/**
 * Pushes [style] onto Media3's [SubtitleView].
 *
 * Note the split: `applyEmbeddedStyles` follows the user's override toggle, but
 * `applyEmbeddedFontSizes` is always false so the size slider works even on ASS files
 * that carry their own sizes — otherwise the most-wanted adjustment would silently do
 * nothing on exactly the files people complain about.
 */
fun SubtitleView.applySubtitleStyle(style: SubtitleStyle) {
    setApplyEmbeddedStyles(!style.overrideEmbedded)
    setApplyEmbeddedFontSizes(false)
    setFractionalTextSize(style.textSizeFraction)
    setBottomPaddingFraction(style.bottomPaddingFraction)
    setStyle(
        CaptionStyleCompat(
            style.textColor,
            style.backgroundColor,
            android.graphics.Color.TRANSPARENT,
            when (style.edgeType) {
                SubtitleEdge.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                SubtitleEdge.DROP_SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                else -> CaptionStyleCompat.EDGE_TYPE_NONE
            },
            android.graphics.Color.BLACK,
            when (style.typeface) {
                SubtitleTypeface.SERIF -> Typeface.SERIF
                SubtitleTypeface.MONO -> Typeface.MONOSPACE
                SubtitleTypeface.SANS -> Typeface.SANS_SERIF
                SubtitleTypeface.DEFAULT -> Typeface.DEFAULT
            },
        )
    )
}
