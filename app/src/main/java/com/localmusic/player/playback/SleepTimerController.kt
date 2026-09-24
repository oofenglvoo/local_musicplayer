package com.localmusic.player.playback

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object SleepTimerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var job: Job? = null
    private var endTimeMs = 0L
    private var fadeOut = true

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    val fadeEnabled: Boolean get() = fadeOut

    fun start(minutes: Int, fadeOutEnabled: Boolean = true) {
        cancel()
        fadeOut = fadeOutEnabled
        endTimeMs = System.currentTimeMillis() + minutes * 60_000L
        _active.value = true
        job = scope.launch {
            while (System.currentTimeMillis() < endTimeMs) {
                _remainingMs.value = (endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                delay(500)
            }
            _remainingMs.value = 0L
            if (fadeOut) fadeOutAndPause()
            PlayerConnection.controller()?.pause()
            _active.value = false
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _active.value = false
        _remainingMs.value = 0L
    }

    private suspend fun fadeOutAndPause() {
        val steps = 20
        for (i in steps downTo 0) {
            PlayerConnection.setVolume(i / steps.toFloat())
            delay(100)
        }
        PlayerConnection.setVolume(1f)
    }

    fun formatRemaining(): String {
        val ms = _remainingMs.value
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }
}
