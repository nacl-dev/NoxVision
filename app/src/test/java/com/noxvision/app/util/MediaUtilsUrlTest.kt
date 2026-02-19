package com.noxvision.app.util

import org.junit.Test
import org.junit.Assert.assertTrue

class MediaUtilsUrlTest {
    @Test
    fun buildDownloadUrls_encodesSpaces() {
        val urls = buildDownloadUrls("http://192.168.1.1", "foo bar.mp4")

        // We expect encoded URL (either %20 or +)
        val hasEncoded = urls.any { it.contains("foo%20bar.mp4") || it.contains("foo+bar.mp4") }
        assertTrue("Should contain encoded filename", hasEncoded)

        // We expect NO unencoded spaces
        val hasUnencoded = urls.any { it.contains("foo bar.mp4") }
        assertTrue("Should NOT contain unencoded spaces", !hasUnencoded)
    }

    @Test
    fun buildDownloadUrls_encodesSpecialChars() {
        val urls = buildDownloadUrls("http://192.168.1.1", "foo#bar?.mp4")

        // # should be %23, ? should be %3F
        val hasEncoded = urls.any { it.contains("foo%23bar%3F.mp4") }
        assertTrue("Should encode special chars # and ?", hasEncoded)
    }
}
