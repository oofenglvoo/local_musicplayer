package com.localmusic.player.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class SelectionState {
    var enabled by mutableStateOf(false)
        private set

    private val _selected = mutableStateOf<Set<Long>>(emptySet())
    val selected: Set<Long> get() = _selected.value

    val count: Int get() = _selected.value.size
    val isEmpty: Boolean get() = _selected.value.isEmpty()

    fun start(id: Long) {
        enabled = true
        _selected.value = setOf(id)
    }

    fun toggle(id: Long) {
        val current = _selected.value
        _selected.value = if (id in current) current - id else current + id
        if (_selected.value.isEmpty()) enabled = false
    }

    fun add(id: Long) {
        _selected.value = _selected.value + id
        enabled = true
    }

    fun selectAll(ids: List<Long>) {
        if (ids.isEmpty()) return
        enabled = true
        _selected.value = ids.toSet()
    }

    fun clear() {
        enabled = false
        _selected.value = emptySet()
    }
}

@Composable
fun rememberSelectionState(): SelectionState = remember { SelectionState() }
