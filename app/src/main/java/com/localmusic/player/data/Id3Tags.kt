package com.localmusic.player.data

import java.io.File

/**
 * Minimal, dependency-free ID3v2 parser used as a fallback when
 * [android.media.MediaMetadataRetriever] fails to read a file (e.g. files with an
 * oversized embedded APIC frame, such as a multi-megabyte PNG cover, which some
 * Android ID3 parsers refuse to process and then drop the whole tag).
 *
 * Supports ID3v2.2 / v2.3 / v2.4 text frames and APIC/PIC pictures.
 */
object Id3Tags {

    data class Tags(
        val title: String?,
        val artist: String?,
        val album: String?,
        val year: Int?,
        val track: Int?,
        val picture: ByteArray?,
        val pictureMime: String?,
    )

    fun read(file: File): Tags? {
        return runCatching { readInternal(file) }.getOrNull()
    }

    private fun readInternal(file: File): Tags? {
        if (!file.exists() || file.length() < 10) return null
        file.inputStream().use { input ->
            val header = ByteArray(10)
            if (input.read(header) != 10) return null
            if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() ||
                header[2] != '3'.code.toByte()
            ) {
                return null
            }
            val major = header[3].toInt() and 0xFF
            val flags = header[5].toInt() and 0xFF
            val tagSize = synchsafe(header[6], header[7], header[8], header[9])
            val tagBytes = ByteArray(tagSize)
            var read = 0
            while (read < tagSize) {
                val n = input.read(tagBytes, read, tagSize - read)
                if (n <= 0) break
                read += n
            }

            val extendedHeader = (flags and 0x40) != 0

            var offset = 0
            if (extendedHeader) {
                offset += when (major) {
                    3 -> 4 + beInt(tagBytes, 0)
                    4 -> synchsafe(tagBytes, 0) + 4
                    else -> 6 + beIntAt(tagBytes, 0)
                }.coerceAtLeast(0)
            }

            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var year: Int? = null
            var track: Int? = null
            var picture: ByteArray? = null
            var pictureMime: String? = null

            val idLen = if (major == 2) 3 else 4
            val sizeLen = if (major == 2) 3 else 4
            val frameHeaderLen = if (major == 2) 6 else 10

            while (offset + frameHeaderLen <= tagBytes.size) {
                val id = String(tagBytes, offset, idLen, Charsets.ISO_8859_1)
                if (id[0] == '\u0000') break
                val frameSize = when {
                    major == 2 -> be24(tagBytes, offset + 3)
                    major == 4 -> synchsafe(
                        tagBytes[offset + 4], tagBytes[offset + 5],
                        tagBytes[offset + 6], tagBytes[offset + 7],
                    )
                    else -> beIntAt(tagBytes, offset + 4)
                }
                val dataStart = offset + frameHeaderLen
                val dataEnd = dataStart + frameSize
                if (frameSize <= 0 || dataEnd > tagBytes.size) break

                when (id) {
                    "TIT2", "TT2" -> title = title ?: decodeTextFrame(tagBytes, dataStart, frameSize)
                    "TPE1", "TP1" -> artist = artist ?: decodeTextFrame(tagBytes, dataStart, frameSize)
                    "TALB", "TAL" -> album = album ?: decodeTextFrame(tagBytes, dataStart, frameSize)
                    "TYER", "TYE", "TDRC" -> {
                        if (year == null) {
                            year = decodeTextFrame(tagBytes, dataStart, frameSize)
                                ?.substringBefore('-')?.trim()?.toIntOrNull()
                        }
                    }
                    "TRCK", "TRK" -> {
                        if (track == null) {
                            track = decodeTextFrame(tagBytes, dataStart, frameSize)
                                ?.substringBefore('/')?.trim()?.toIntOrNull()
                        }
                    }
                    "APIC" -> {
                        if (picture == null) {
                            val pic = decodeApic(tagBytes, dataStart, frameSize)
                            picture = pic?.second
                            pictureMime = pic?.first
                        }
                    }
                    "PIC" -> {
                        if (picture == null) {
                            val pic = decodePic(tagBytes, dataStart, frameSize)
                            picture = pic?.second
                            pictureMime = pic?.first
                        }
                    }
                }

                offset = dataEnd
            }

            if (title == null && artist == null && album == null && picture == null) return null
            return Tags(title, artist, album, year, track, picture, pictureMime)
        }
    }

    private fun decodeTextFrame(data: ByteArray, start: Int, size: Int): String? {
        if (size <= 1) return null
        val encoding = data[start].toInt() and 0xFF
        val content = data.copyOfRange(start + 1, start + size)
        return decodeString(encoding, content)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun decodeString(encoding: Int, bytes: ByteArray): String? = runCatching {
        when (encoding) {
            0 -> String(bytes, Charsets.ISO_8859_1)
            1 -> String(bytes, Charsets.UTF_16).trimStart('\uFEFF')
            2 -> String(bytes, Charsets.UTF_16BE).trimStart('\uFEFF')
            3 -> String(bytes, Charsets.UTF_8)
            else -> String(bytes, Charsets.ISO_8859_1)
        }
    }.getOrNull()

    private fun decodeApic(data: ByteArray, start: Int, size: Int): Pair<String, ByteArray>? {
        if (size < 4) return null
        val encoding = data[start].toInt() and 0xFF
        var i = start + 1
        val end = start + size
        val mimeEnd = indexOfZero(data, i, end, latin = true) ?: return null
        val mime = String(data, i, mimeEnd - i, Charsets.ISO_8859_1)
        i = mimeEnd + 1
        if (i >= end) return null
        i += 1 // picture type
        val descEnd = indexOfZero(data, i, end, latin = encoding == 0 || encoding == 3) ?: return null
        i = if (encoding == 1 || encoding == 2) descEnd + 2 else descEnd + 1
        if (i > end) return null
        val image = data.copyOfRange(i, end)
        if (image.isEmpty()) return null
        return mime to image
    }

    private fun decodePic(data: ByteArray, start: Int, size: Int): Pair<String, ByteArray>? {
        if (size < 6) return null
        val encoding = data[start].toInt() and 0xFF
        val format = String(data, start + 1, 3, Charsets.ISO_8859_1)
        var i = start + 4
        val end = start + size
        i += 1 // picture type
        val descEnd = indexOfZero(data, i, end, latin = encoding == 0) ?: return null
        i = if (encoding == 1) descEnd + 2 else descEnd + 1
        if (i > end) return null
        val image = data.copyOfRange(i, end)
        if (image.isEmpty()) return null
        val mime = when (format.uppercase()) {
            "PNG" -> "image/png"
            "JPG", "JPEG" -> "image/jpeg"
            else -> "image/"
        }
        return mime to image
    }

    private fun indexOfZero(data: ByteArray, from: Int, end: Int, latin: Boolean): Int? {
        var i = from
        if (latin) {
            while (i < end) {
                if (data[i] == 0.toByte()) return i
                i++
            }
        } else {
            while (i + 1 < end) {
                if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) return i
                i += 2
            }
        }
        return null
    }

    private fun synchsafe(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0x7F) shl 21) or
            ((b1.toInt() and 0x7F) shl 14) or
            ((b2.toInt() and 0x7F) shl 7) or
            (b3.toInt() and 0x7F)

    private fun synchsafe(data: ByteArray, off: Int): Int =
        synchsafe(data[off], data[off + 1], data[off + 2], data[off + 3])

    private fun be24(data: ByteArray, off: Int): Int =
        ((data[off].toInt() and 0xFF) shl 16) or
            ((data[off + 1].toInt() and 0xFF) shl 8) or
            (data[off + 2].toInt() and 0xFF)

    private fun beIntAt(data: ByteArray, off: Int): Int =
        ((data[off].toInt() and 0xFF) shl 24) or
            ((data[off + 1].toInt() and 0xFF) shl 16) or
            ((data[off + 2].toInt() and 0xFF) shl 8) or
            (data[off + 3].toInt() and 0xFF)

    private fun beInt(data: ByteArray, off: Int): Int = beIntAt(data, off)
}
