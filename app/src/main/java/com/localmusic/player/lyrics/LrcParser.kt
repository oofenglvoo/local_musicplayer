package com.localmusic.player.lyrics

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val plainText: String? = null,
    val offsetMs: Long = 0L,
) {
    val isSynced: Boolean get() = lines.isNotEmpty()

    fun lineIndexAt(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        val effective = positionMs - offsetMs
        var result = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= effective) result = i else break
        }
        return result
    }
}

object LrcParser {

    private val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

    fun parse(content: String): Lyrics {
        val lines = mutableListOf<LyricLine>()
        var offset = 0L
        var hasTimeTags = false

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val offsetMatch = Regex("\\[offset:\\s*([+-]?\\d+)]", RegexOption.IGNORE_CASE)
                .find(line)
            if (offsetMatch != null) {
                offset = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                return@forEach
            }

            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach

            hasTimeTags = true
            val text = line.substring(matches.last().range.last + 1).trim()

            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fractionRaw = match.groupValues[3]
                val millis = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLong() * 100
                    2 -> fractionRaw.toLong() * 10
                    else -> fractionRaw.take(3).toLong()
                }
                lines += LyricLine(minutes * 60_000 + seconds * 1_000 + millis, text)
            }
        }

        if (!hasTimeTags) {
            return Lyrics(lines = emptyList(), plainText = content.trim())
        }

        return Lyrics(
            lines = lines.sortedBy { it.timeMs },
            offsetMs = offset,
        )
    }
}
