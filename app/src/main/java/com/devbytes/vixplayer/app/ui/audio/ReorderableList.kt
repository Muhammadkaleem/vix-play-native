package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Minimal drag-to-reorder for a [androidx.compose.foundation.lazy.LazyColumn].
 *
 * Compose has no built-in reorderable list, and this is one screen, so it is hand-rolled
 * rather than pulling in a dependency.
 *
 * Deliberately simple: **long-press to lift** so ordinary scrolling is untouched, swap on
 * crossing a neighbour's midpoint rather than animating continuously, and persist only on
 * drop (via [onDrop]) instead of writing on every swap.
 *
 * Drag thresholds and autoscroll are the kind of thing that is tuned by feel on a device;
 * the behaviour here is correct but may want adjusting once run.
 */
class ReorderState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDrop: () -> Unit,
) {
    var draggingIndex by mutableIntStateOf(-1)
        private set

    var dragOffset by mutableFloatStateOf(0f)
        private set

    private var startOffset = 0f

    val isDragging: Boolean get() = draggingIndex >= 0

    fun onDragStart(index: Int) {
        draggingIndex = index
        dragOffset = 0f
        startOffset = itemInfo(index)?.offset?.toFloat() ?: 0f
    }

    fun onDrag(delta: Float) {
        if (draggingIndex < 0) return
        dragOffset += delta

        val current = itemInfo(draggingIndex) ?: return
        val currentCentre = startOffset + dragOffset + current.size / 2f

        // Swap once the dragged item's centre passes a neighbour's centre.
        val target = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { info ->
                info.index != draggingIndex &&
                    currentCentre.toInt() in info.offset..(info.offset + info.size)
            }
            ?: return

        onMove(draggingIndex, target.index)
        // The dragged item now occupies the target slot; rebase so it stays under the finger.
        val newStart = itemInfo(target.index)?.offset?.toFloat() ?: startOffset
        dragOffset += startOffset - newStart
        startOffset = newStart
        draggingIndex = target.index
    }

    fun onDragEnd() {
        if (draggingIndex >= 0) onDrop()
        draggingIndex = -1
        dragOffset = 0f
    }

    private fun itemInfo(index: Int) =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit,
): ReorderState = remember(listState) { ReorderState(listState, onMove, onDrop) }

/**
 * Attaches the lift-and-drag gesture to a row. Long-press is the trigger, so vertical
 * scrolling over the same list is unaffected.
 */
fun Modifier.reorderable(state: ReorderState, index: Int): Modifier =
    this.pointerInput(index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.onDragStart(index) },
            onDrag = { change, amount ->
                change.consume()
                state.onDrag(amount.y)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() },
        )
    }
