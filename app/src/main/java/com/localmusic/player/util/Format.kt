package com.localmusic.player.util

import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.toDurationString(): String {
    if (this <= 0) return "--:--"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
