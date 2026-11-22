package com.devbytes.vixplayer.app.ui.common

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** True on Android TV / Google TV / Fire TV (see [com.devbytes.vixplayer.app.TvDevice]). */
val LocalIsTv = staticCompositionLocalOf { false }

private val RingWidth = 2.5.dp
private val RingCorner = 12.dp
private const val FOCUS_FILL_ALPHA = 0.14f

/**
 * The app's default indication: Material's ripple, plus a ring and a light fill in [color] while
 * the element has focus. Touch never focuses a clickable, so this only shows when moving with a
 * D-pad, a TV remote or a keyboard. Elements clip to their own shape first, so the fill always
 * fits; the ring's corners follow a 12 dp radius.
 */
class FocusRingIndication(private val color: Color) : IndicationNodeFactory {
    private val ripple = ripple()

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        FocusRingNode(interactionSource, color, ripple.create(interactionSource))

    override fun equals(other: Any?) = other is FocusRingIndication && other.color == color
    override fun hashCode() = color.hashCode()
}

// Not a draw node itself: a node that is one would have its delegates skipped when drawing, and the
// ripple would never show. Both parts are delegates instead.
private class FocusRingNode(
    interactionSource: InteractionSource,
    color: Color,
    ripple: DelegatableNode,
) : DelegatingNode() {
    init {
        delegate(ripple)
        delegate(RingDrawNode(interactionSource, color))
    }
}

private class RingDrawNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private var focused = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                focused = when (interaction) {
                    is FocusInteraction.Focus -> true
                    is FocusInteraction.Unfocus -> false
                    else -> return@collect
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        val stroke = RingWidth.toPx()
        val corner = CornerRadius(RingCorner.toPx())
        drawRoundRect(color.copy(alpha = FOCUS_FILL_ALPHA), cornerRadius = corner)
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = corner,
            style = Stroke(stroke),
        )
    }
}

/**
 * The focus ring for elements that opt out of the default indication (`indication = null`), drawn
 * in [shape] while [interactionSource] reports focus.
 */
@Composable
fun Modifier.focusRing(interactionSource: InteractionSource, shape: Shape): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    return if (focused) border(RingWidth, MaterialTheme.colorScheme.primary, shape) else this
}
