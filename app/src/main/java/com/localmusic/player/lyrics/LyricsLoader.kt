package com.localmusic.player.lyrics

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

object LyricsLoader {

    suspend fun load(path: String): Lyrics? = withContext(Dispatchers.IO) {
        loadFromLrcFile(path) ?: loadEmbedded(path)
    }

    private fun loadFromLrcFile(audioPath: String): Lyrics? {
        val audio = File(audioPath)
        val parent = audio.parentFile ?: return null
        val baseName = audio.nameWithoutExtension
        val candidates = listOf("$baseName.lrc", "$baseName.LRC", "$baseName.Lrc")
        for (name in candidates) {
            val lrc = File(parent, name)
            if (lrc.exists() && lrc.canRead()) {
                val parsed = runCatching {
                    val text = lrc.readText(charsetGuess(lrc))
                    if (text.isBlank()) null else LrcParser.parse(text)
                }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun charsetGuess(file: File): Charset {
        return try {
            val bytes = file.readBytes()
            when {
                bytes.size >= 3 &&
                    bytes[0] == 0xEF.toByte() &&
                    bytes[1] == 0xBB.toByte() &&
                    bytes[2] == 0xBF.toByte() -> Charsets.UTF_8
                bytes.size >= 2 &&
                    bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
                bytes.size >= 2 &&
                    bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
                else -> Charsets.UTF_8
            }
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    private fun loadEmbedded(audioPath: String): Lyrics? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioPath)
            val raw = METADATA_KEY_LYRICS.let { retriever.extractMetadata(it) }
            if (raw.isNullOrBlank()) {
                null
            } else {
                val parsed = LrcParser.parse(raw)
                if (parsed.lines.isEmpty() && parsed.plainText == null) {
                    Lyrics(lines = emptyList(), plainText = raw.trim())
                } else {
                    parsed
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private const val METADATA_KEY_LYRICS = 13
}
