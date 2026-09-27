package com.localmusic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies the pure-JVM ID3v2 fallback parser used when
 * [android.media.MediaMetadataRetriever] fails to read a file's tag.
 *
 * The sample is an ID3v2.3 tag with UTF-8 text frames and an embedded PNG APIC,
 * mirroring real files (e.g. a 036 song) that Android's retriever cannot parse.
 */
class Id3TagsTest {

    private fun sample(): File {
        val url = javaClass.classLoader!!.getResource("id3v23_cover.mp3")
        assertNotNull("test resource missing", url)
        return File(url!!.toURI())
    }

    @Test
    fun readsTextFramesAndPicture() {
        val tags = Id3Tags.read(sample())
        assertNotNull(tags)
        assertEquals("双人旁", tags!!.title)
        assertEquals("许嵩", tags.artist)
        assertEquals("苏格拉没有底", tags.album)
        assertEquals(2011, tags.year)
        assertEquals(6, tags.track)
        assertEquals("image/png", tags.pictureMime)
        assertNotNull(tags.picture)
        assertTrue(tags.picture!!.isNotEmpty())
        // PNG signature
        assertEquals(0x89.toByte(), tags.picture!![0])
        assertEquals('P'.code.toByte(), tags.picture!![1])
    }
}
