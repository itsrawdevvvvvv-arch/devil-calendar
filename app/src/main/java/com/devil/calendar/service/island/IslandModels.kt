package com.devil.calendar.service.island

/**
 * What the island is currently showing. Higher priority wins the pill when
 * several sources are live; the user can swipe between them.
 */
enum class IslandKind(val priority: Int) {
    CALL(100),
    TIMER(80),
    REST(70),
    MUSIC(50),
    OTHER(10),
}

/**
 * One live source.
 *
 * [endMillis] drives the countdown; [progress] (0..1) drives the bar. Nothing
 * here is persisted - see the security note on [IslandService].
 */
data class IslandSource(
    val id: String,
    val kind: IslandKind,
    val title: String,
    val subtitle: String = "",
    val value: String = "",
    val progress: Float? = null,
    val endMillis: Long? = null,
    /** Set for sources that came from another app's notification. */
    val packageName: String? = null,
    /** Transport controls rendered in the expanded pill. */
    val actions: List<IslandAction> = emptyList(),
)

/** One transport button, e.g. play / pause / next. */
data class IslandAction(val id: String, val label: String)

data class IslandState(
    val enabled: Boolean = false,
    val running: Boolean = false,
    val expanded: Boolean = false,
    val sources: List<IslandSource> = emptyList(),
    val index: Int = 0,
) {
    val current: IslandSource?
        get() = sources.getOrNull(index.coerceIn(0, (sources.size - 1).coerceAtLeast(0)))

    val hasMultiple: Boolean get() = sources.size > 1

    /** No sources means the idle pill, which is always present. */
    val mode: IslandMode
        get() = when {
            sources.isEmpty() -> IslandMode.IDLE
            expanded -> IslandMode.EXPANDED
            else -> IslandMode.COMPACT
        }
}

enum class IslandMode { IDLE, COMPACT, EXPANDED }
