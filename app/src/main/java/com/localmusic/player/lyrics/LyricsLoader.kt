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
        val id3Lyrics = readId3Lyrics(File(audioPath))
        if (!id3Lyrics.isNullOrBlank()) return parseEmbedded(id3Lyrics)

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioPath)
            // Android's lyrics key is 21 (13 is ALBUM_ARTIST on current Android).
            // Keep the legacy key as a fallback for a few vendor retriever versions.
            val raw = listOf(
                METADATA_KEY_LYRICS,
                LEGACY_METADATA_KEY_LYRICS,
            ).asSequence()
                .mapNotNull { key -> runCatching { retriever.extractMetadata(key) }.getOrNull() }
                .firstOrNull { it.isNotBlank() }
            if (raw.isNullOrBlank()) {
                null
            } else {
                parseEmbedded(raw)
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun parseEmbedded(raw: String): Lyrics {
        val parsed = LrcParser.parse(raw)
        return if (parsed.lines.isEmpty() && parsed.plainText == null) {
            Lyrics(lines = emptyList(), plainText = raw.trim())
        } else parsed
    }

    /** Reads common ID3v2 USLT/TXXX lyric frames, which MediaMetadataRetriever often hides. */
    private fun readId3Lyrics(file: File): String? {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        if (bytes.size < 10 || bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) return null
        val version = bytes[3].toInt() and 0xff
        val tagSize = synchsafe(bytes, 6)
        val end = (10 + tagSize).coerceAtMost(bytes.size)
        var offset = 10
        while (offset + 10 <= end) {
            val id = String(bytes, offset, 4, Charsets.ISO_8859_1)
            if (id.all { it == '\u0000' || it == ' ' }) break
            val size = if (version >= 4) synchsafe(bytes, offset + 4) else uint32(bytes, offset + 4)
            val dataStart = offset + 10
            val dataEnd = (dataStart + size).coerceAtMost(end)
            if (dataEnd <= dataStart) break
            val payload = bytes.copyOfRange(dataStart, dataEnd).let(::removeUnsynchronization)
            val text = when (id) {
                "USLT", "ULT" -> decodeUslt(payload)
                "TXXX", "TXX" -> decodeTxxx(payload)
                "SYLT" -> decodeSylt(payload)
                else -> null
            }
            if (!text.isNullOrBlank() && looksLikeLyrics(text)) return text
            offset = dataEnd
        }
        return null
    }

    private fun decodeUslt(data: ByteArray): String? {
        if (data.size < 4) return null
        val encoding = data[0].toInt() and 0xff
        val descriptorEnd = findTerminator(data, 4, encoding)
        // USLT = encoding + 3-byte language + description + terminator + text.
        // For an empty UTF-16 description, descriptorEnd points to the first
        // byte of the two-byte terminator, so skip the terminator as well.
        return decodeText(
            data,
            (descriptorEnd + terminatorLength(encoding)).coerceAtMost(data.size),
            data.size,
            encoding,
        )
    }

    private fun decodeTxxx(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val encoding = data[0].toInt() and 0xff
        val descriptionEnd = findTerminator(data, 1, encoding)
        val description = decodeText(data, 1, descriptionEnd, encoding).orEmpty()
        // Some taggers store the lyric text in TXXX:USLT rather than a real
        // USLT frame. Others use descriptions such as "lyrics" or
        // "unsyncedlyrics". Accept all of these common names.
        val isLyricsField = description.equals("USLT", ignoreCase = true) ||
            description.contains("lyric", ignoreCase = true)
        if (!isLyricsField) return null
        return decodeText(data, descriptionEnd + terminatorLength(encoding), data.size, encoding)
    }

    private fun decodeSylt(data: ByteArray): String? {
        if (data.size < 6) return null
        val encoding = data[0].toInt() and 0xff
        val languageEnd = 4
        val descriptorEnd = findTerminator(data, languageEnd + 1, encoding)
        return decodeText(data, descriptorEnd + terminatorLength(encoding), data.size, encoding)
            ?.replace(Regex("[\\u0000-\\u001f]+"), "\\n")
    }

    private fun looksLikeLyrics(text: String): Boolean = text.trim().length >= 2

    private fun findTerminator(data: ByteArray, start: Int, encoding: Int): Int {
        val step = if (encoding == 1 || encoding == 2) 2 else 1
        var i = start
        while (i + step <= data.size) {
            if (data[i] == 0.toByte() && (step == 1 || data[i + 1] == 0.toByte())) return i
            i += step
        }
        return data.size
    }

    private fun terminatorLength(encoding: Int): Int = if (encoding == 1 || encoding == 2) 2 else 1

    private fun decodeText(data: ByteArray, start: Int, end: Int, encoding: Int): String? {
        if (start >= end) return null
        val bytes = data.copyOfRange(start, end)
        val candidates = when (encoding) {
            1 -> {
                when {
                    bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                        listOf(Charsets.UTF_16LE, Charsets.UTF_16)
                    bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                        listOf(Charsets.UTF_16BE, Charsets.UTF_16)
                    else -> listOf(Charsets.UTF_16LE, Charsets.UTF_16)
                }
            }
            2 -> listOf(Charsets.UTF_16BE)
            3 -> listOf(Charsets.UTF_8)
            else -> listOf(Charsets.ISO_8859_1)
        }
        return candidates.mapNotNull { charset ->
            runCatching {
                String(bytes, charset).trim('\u0000', '\uFEFF', ' ', '\n', '\r')
            }.getOrNull()
        }.maxByOrNull { text ->
            // Prefer the candidate that retains real CJK text and has fewer
            // replacement characters. This handles taggers that mark UTF-16
            // but omit or duplicate the BOM.
            val cjk = text.count { it in '\u3400'..'\u9FFF' }
            cjk * 10 - text.count { it == '\uFFFD' }
        }
    }

    private fun removeUnsynchronization(data: ByteArray): ByteArray {
        val out = ArrayList<Byte>(data.size)
        data.forEachIndexed { index, byte ->
            if (byte.toInt() == 0 && index > 0 && data[index - 1].toInt() and 0xff == 0xff) return@forEachIndexed
            out += byte
        }
        return out.toByteArray()
    }

    private fun synchsafe(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0x7f) shl 21) or
            ((data[offset + 1].toInt() and 0x7f) shl 14) or
            ((data[offset + 2].toInt() and 0x7f) shl 7) or
            (data[offset + 3].toInt() and 0x7f)

    private fun uint32(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 24) or
            ((data[offset + 1].toInt() and 0xff) shl 16) or
            ((data[offset + 2].toInt() and 0xff) shl 8) or
            (data[offset + 3].toInt() and 0xff)

    private const val METADATA_KEY_LYRICS = 21
    private const val LEGACY_METADATA_KEY_LYRICS = 13
}
