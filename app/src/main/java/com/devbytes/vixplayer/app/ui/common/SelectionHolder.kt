package com.devbytes.vixplayer.app.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Multi-select bookkeeping, shared by the audio and video libraries.
 *
 * Composed rather than inherited: the two ViewModels have nothing else in common, and
 * this is the only concern they share. Each keeps its own domain actions (what "share"
 * or "trash" means for its media type) and delegates only the selection state.
 *
 * Keyed on `Long` because both libraries identify items by MediaStore id.
 */
class SelectionHolder {

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    val isActive: Boolean get() = _selected.value.isNotEmpty()

    fun toggle(id: Long) {
        val current = _selected.value
        _selected.value = if (id in current) current - id else current + id
    }

    /**
     * Selects exactly [ids] — the caller passes the *visible* list, so select-all means
     * "everything on screen now", not everything in the library. Both screens can be
     * filtered or drilled into, and this keeps the meaning consistent between them.
     */
    fun selectAll(ids: List<Long>) {
        _selected.value = ids.toSet()
    }

    fun clear() {
        if (_selected.value.isNotEmpty()) _selected.value = emptySet()
    }

    /** Filters [items] to the selection, preserving the on-screen order. */
    fun <T> filter(items: List<T>, idOf: (T) -> Long): List<T> =
        items.filter { idOf(it) in _selected.value }
}
