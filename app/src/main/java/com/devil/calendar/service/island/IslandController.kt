package com.devil.calendar.service.island

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The island's single source of truth. The overlay, the pill and the service
 * all read [state]; only this class mutates it.
 */
@Singleton
class IslandController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow(IslandState())
    val state: StateFlow<IslandState> = _state.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        if (enabled) {
            IslandService.start(context)
        } else {
            IslandService.stop(context)
        }
    }

    fun setRunning(running: Boolean) {
        _state.value = _state.value.copy(
            running = running,
            // A dead service must not leave an expanded pill behind.
            expanded = if (running) _state.value.expanded else false,
            sources = if (running) _state.value.sources else emptyList(),
        )
    }

    /** Sorted by priority so the most important source owns the pill. */
    fun setSources(sources: List<IslandSource>) {
        val sorted = sources.sortedByDescending { it.kind.priority }
        _state.value = _state.value.copy(
            sources = sorted,
            index = _state.value.index.coerceIn(0, (sorted.size - 1).coerceAtLeast(0)),
        )
    }

    fun toggleExpanded() {
        if (_state.value.sources.isEmpty()) return
        _state.value = _state.value.copy(expanded = !_state.value.expanded)
    }

    fun collapse() {
        _state.value = _state.value.copy(expanded = false)
    }

    fun next() = move(1)

    fun previous() = move(-1)

    private fun move(delta: Int) {
        val state = _state.value
        if (state.sources.size < 2) return
        val next = (state.index + delta).mod(state.sources.size)
        _state.value = state.copy(index = next)
    }
}
