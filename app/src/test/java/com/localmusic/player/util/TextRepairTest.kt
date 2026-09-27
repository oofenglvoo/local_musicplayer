package com.localmusic.player.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reproduces the real-device mojibake case captured from a Xiaomi phone:
 * the vendor media scanner read a UTF-8 ID3 title byte-by-byte as ISO-8859-1
 * and stored the result into MediaStore. [TextRepair.repair] must undo it.
 */
class TextRepairTest {

    private fun fromCodePoints(cps: IntArray): String = String(cps, 0, cps.size)

    private val kyleTitle = fromCodePoints(KYLE_TITLE_CP)
    private val kyleAlbum = fromCodePoints(KYLE_ALBUM_CP)

    @Test
    fun repairsUtf8ReadAsLatin1Title() {
        val repaired = TextRepair.repair(kyleTitle)
        assertEquals(EXPECTED_TITLE, repaired)
    }

    @Test
    fun repairsUtf8ReadAsLatin1Album() {
        assertEquals("你的名字。", TextRepair.repair(kyleAlbum))
    }

    @Test
    fun leavesPlainAsciiUntouched() {
        val ascii = "Kyle Xian"
        assertEquals(ascii, TextRepair.repair(ascii))
    }

    @Test
    fun leavesNormalChineseUntouched() {
        val clean = "你的名字6首BGM串烧"
        assertEquals(clean, TextRepair.repair(clean))
    }

    @Test
    fun leavesNormalJapaneseUntouched() {
        val clean = "リンゴ日和～The Wolf Whistling Song"
        assertEquals(clean, TextRepair.repair(clean))
    }

    @Test
    fun repairedTitleIsNoLongerSuspicious() {
        val repaired = TextRepair.repair(kyleTitle)
        assertFalse(repaired.contains('\uFFFD'))
        assertTrue(repaired.any { it in '\u4E00'..'\u9FFF' })
    }
}
