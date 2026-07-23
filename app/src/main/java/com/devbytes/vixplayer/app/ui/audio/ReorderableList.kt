package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

    val isDragging: Boolean get() = draggingIndex >= 0

    private var rowHeight = 0f

    fun onDragStart(index: Int) {
        draggingIndex = index
        dragOffset = 0f
        // Captured once, before any swap, so the drag never depends on layout that
        // recomposition hasn't caught up with.
        rowHeight = itemInfo(index)?.size?.toFloat() ?: 0f
    }

    /**
     * Swaps once per row-height of travel, accumulating the remainder.
     *
     * Deliberately derives nothing from `layoutInfo` mid-gesture. Two earlier attempts
     * did — first rebasing against the swapped item's offset, then hit-testing the
     * pointer against item bounds — and both read geometry from before `onMove`'s
     * recomposition had landed, so a drag of any length advanced exactly one position.
     * Distance accumulation has no such dependency, and [dragOffset] doubles as the
     * visual displacement.
     */
    fun onDrag(delta: Float) {
        if (draggingIndex < 0 || rowHeight <= 0f) return
        dragOffset += delta
        val lastIndex = listState.layoutInfo.totalItemsCount - 1

        while (dragOffset >= rowHeight && draggingIndex < lastIndex) {
            onMove(draggingIndex, draggingIndex + 1)
            draggingIndex += 1
            dragOffset -= rowHeight
        }
        while (dragOffset <= -rowHeight && draggingIndex > 0) {
            onMove(draggingIndex, draggingIndex - 1)
            draggingIndex -= 1
            dragOffset += rowHeight
        }
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
 *
 * **Keyed on `Unit`, never on the index.** `pointerInput` restarts when its key changes,
 * and a swap changes the dragged row's index — which tore down the block and cancelled
 * the gesture mid-drag, so a drag of any length advanced exactly one position no matter
 * how the swap detection was written. The live index is read through
 * [rememberUpdatedState] instead, which updates without restarting the gesture.
 */
@Composable
fun Modifier.reorderable(state: ReorderState, index: Int): Modifier {
    val currentIndex by rememberUpdatedState(index)
    return this.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.onDragStart(currentIndex) },
            onDrag = { change, amount ->
                change.consume()
                state.onDrag(amount.y)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() },
        )
    }
}
