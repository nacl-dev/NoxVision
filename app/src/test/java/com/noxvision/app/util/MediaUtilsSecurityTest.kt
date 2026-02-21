package com.noxvision.app.util

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

class MediaUtilsSecurityTest {

    @Test
    fun sanitizeFilename_removesParentDirectory() {
        // ".." should be sanitized to something safe
        val safeName = sanitizeFilename("..")
        assertFalse("Filename should not contain ..", safeName.contains(".."))
        assertEquals("__", safeName) // Expecting __ based on plan
    }

    @Test
    fun buildDownloadUrls_stripsPathComponents() {
        // Path traversal attempts
        val filenames = listOf(
            "../passwd",
            "..\\passwd",
            "/etc/passwd",
            "C:\\Windows\\System32\\calc.exe"
        )

        filenames.forEach { filename ->
            val urls = buildDownloadUrls("http://example.com", filename)
            urls.forEach { url ->
                assertFalse("URL should not contain path traversal: $url", url.contains(".."))
                assertFalse("URL should not contain path traversal: $url", url.contains("%2E%2E")) // Encoded ..
                assertFalse("URL should not contain absolute path: $url", url.contains("/etc/"))
                assertFalse("URL should not contain absolute path: $url", url.contains("C:"))
            }
        }
    }

    @Test
    fun buildDownloadUrls_preservesSafeFilename() {
        val filename = "video.mp4"
        val urls = buildDownloadUrls("http://example.com", filename)

        // At least one URL should end with the filename
        val hasFilename = urls.any { it.endsWith(filename) }
        assertEquals(true, hasFilename)
    }
}
