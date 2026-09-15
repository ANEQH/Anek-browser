package com.anek.browser.web

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConsoleLevel { LOG, INFO, WARN, ERROR, TIP }

data class ConsoleEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: ConsoleLevel,
    val message: String,
    val source: String = ""
)

/**
 * Ring buffer of `console.*` output captured from `WebChromeClient`.
 *
 * Only populated while the "Capture console" developer option is on, so there
 * is zero overhead in normal browsing.
 */
object ConsoleLog {

    private const val CAPACITY = 500

    private val _entries = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val entries: StateFlow<List<ConsoleEntry>> = _entries.asStateFlow()

    fun add(level: ConsoleLevel, message: String, source: String = "") {
        val current = _entries.value
        val next = if (current.size >= CAPACITY) current.drop(current.size - CAPACITY + 1) else current
        _entries.value = next + ConsoleEntry(level = level, message = message, source = source)
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun fromLevelString(level: Int): ConsoleLevel = when (level) {
        0 -> ConsoleLevel.TIP
        1 -> ConsoleLevel.LOG
        2 -> ConsoleLevel.WARN
        3 -> ConsoleLevel.ERROR
        else -> ConsoleLevel.LOG
    }
}
