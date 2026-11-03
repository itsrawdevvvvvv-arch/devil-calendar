package com.devil.calendar.service.island

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers which packages have posted a notification while the listener was
 * connected, so the setup screen can offer an allow list.
 *
 * Package names only - never notification content, and nothing persisted.
 */
@Singleton
class IslandPackageTracker @Inject constructor() {

    private val _known = MutableStateFlow<Set<String>>(emptySet())
    val known: StateFlow<Set<String>> = _known.asStateFlow()

    fun record(packages: Set<String>) {
        if (packages.isEmpty()) return
        _known.value = _known.value + packages
    }
}
