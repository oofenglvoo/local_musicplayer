package com.localmusic.player.util

import java.nio.charset.Charset

/**
 * Heuristic repair for metadata (title/artist/album) that was decoded with the
 * wrong charset upstream, e.g. by a vendor media scanner that read UTF-8 tag
 * bytes as GBK/Latin-1 (or the reverse) before writing them into MediaStore.
 *
 * The broken value survives MediaMetadataRetriever / ContentResolver as a plain
 * String, so the only way to recover it is to re-encode the String back into
 * bytes with the *assumed wrong* charset and decode again with a candidate
 * charset, then score the result.
 */
object TextRepair {

    private val REPLACEMENT = '\uFFFD'

    private val LATIN_CANDIDATES = listOf(
        "ISO-8859-1", "windows-1252", "GBK", "GB18030", "Big5",
    )

    private val RECOVERY_CANDIDATES = listOf(
        "UTF-8", "GBK", "GB18030", "Big5",
    )

    /**
     * Returns a repaired string when the input looks like mis-decoded text and a
     * better candidate can be found; otherwise returns the original unchanged.
     */
    fun repair(text: String): String {
        if (text.isBlank()) return text
        if (!looksSuspicious(text)) return text

        val baseline = score(text)
        var best = text
        var bestScore = baseline
        var bestUtf8 = false
        for (wrong in LATIN_CANDIDATES) {
            val bytes = runCatching { text.toByteArray(charset(wrong)) }.getOrNull() ?: continue
            if (bytes.isEmpty()) continue
            for (target in RECOVERY_CANDIDATES) {
                if (target.equals(wrong, ignoreCase = true)) continue
                val decoded = runCatching {
                    String(bytes, charset(target)).trim('\u0000', '\uFEFF', ' ', '\n', '\r')
                }.getOrNull() ?: continue
                if (decoded.isBlank()) continue
                val valid = !decoded.contains(REPLACEMENT)
                val candidateScore = score(decoded)
                // Prefer a lossless UTF-8 round-trip: it is self-validating, so
                // when the byte sequence decodes cleanly as UTF-8 it is almost
                // always the original text. A GBK/other decode of the same bytes
                // may coincidentally produce "CJK-looking" garbage with a higher
                // raw score, so UTF-8 must win when it is valid.
                val isUtf8 = target.equals("UTF-8", ignoreCase = true) && valid
                val better = when {
                    bestUtf8 && !isUtf8 -> false
                    isUtf8 && !bestUtf8 -> true
                    else -> candidateScore > bestScore
                }
                if (better) {
                    bestScore = candidateScore
                    bestUtf8 = isUtf8
                    best = decoded
                }
            }
        }

        // Only accept a repair that no longer looks broken.
        return if (!looksSuspicious(best) && best != text) best else text
    }

    /**
     * True when the text contains characters typical of UTF-8 bytes that were
     * interpreted one-by-one as Latin-1: accented letters (U+00A0..U+00FF) and
     * the C1 range (U+0080..U+009F) which renders as control-like or as glyphs
     * such as €, Š, ‚, ƒ, „, … on window-1252. Real CJK text never contains
     * these, so even a single run is a strong signal.
     */
    private fun looksSuspicious(text: String): Boolean {
        var cjk = 0
        var latinHigh = 0
        var c1 = 0
        for (ch in text) {
            when (ch.code) {
                // U+00A0 (no-break space) is a normal typographic space that
                // legitimately appears in real CJK metadata; never treat it as
                // a mojibake marker.
                in 0x00A1..0x00FF -> latinHigh++
                in 0x0080..0x009F -> c1++
                in 0x3400..0x9FFF -> cjk++
                in 0xF900..0xFAFF -> cjk++
                in 0x3040..0x30FF -> cjk++
                else -> {
                    if (ch == REPLACEMENT) return true
                }
            }
        }
        // Text that is already predominantly CJK/kana is legitimate; the few
        // non-breaking spaces or accented glyphs it may carry are not mojibake.
        if (cjk > 0 && cjk * 2 >= latinHigh + c1 + cjk) return false

        val markers = latinHigh + c1
        if (markers == 0) return false
        // A run of such characters is essentially impossible in legitimate
        // Chinese/Japanese/Korean metadata.
        return markers >= 2 || (markers >= 1 && markers * 3 >= text.length)
    }

    private fun score(text: String): Int {
        var cjk = 0
        var replacement = 0
        var control = 0
        var latinHigh = 0
        var c1 = 0
        var printableAscii = 0
        for (ch in text) {
            when {
                ch == REPLACEMENT -> replacement++
                ch in '\u3400'..'\u9FFF' || ch in '\uF900'..'\uFAFF' ||
                    ch in '\u3040'..'\u30FF' -> cjk++
                ch.code in 0x0080..0x009F -> c1++
                ch.isISOControl() -> control++
                ch.code in 0x20..0x7E -> printableAscii++
                // Exclude U+00A0: it is a normal space, not a mojibake signal.
                ch.code in 0x00A1..0x00FF -> latinHigh++
            }
        }
        // Reward legible CJK/kana and ASCII; penalise every trace of the
        // mis-decoded Latin-1 encoding (high Latin-1 letters, C1 controls,
        // replacement chars, control chars).
        return cjk * 10 + printableAscii -
            replacement * 20 - control * 20 - c1 * 8 - latinHigh * 6
    }

    private fun charset(name: String): Charset = Charset.forName(name)
}
