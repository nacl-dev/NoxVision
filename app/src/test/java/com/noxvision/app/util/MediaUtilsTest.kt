package com.noxvision.app.util

import org.junit.Test
import org.junit.Assert.assertEquals

class MediaUtilsTest {

    @Test
    fun formatDuration_zeroSeconds() {
        assertEquals("00:00", formatDuration(0))
    }

    @Test
    fun formatDuration_lessThanOneMinute() {
        assertEquals("00:45", formatDuration(45))
    }

    @Test
    fun formatDuration_exactlyOneMinute() {
        assertEquals("01:00", formatDuration(60))
    }

    @Test
    fun formatDuration_moreThanOneMinute() {
        assertEquals("01:30", formatDuration(90))
    }

    @Test
    fun formatDuration_moreThanOneHour() {
        // 3665 seconds = 1 hour, 1 minute, 5 seconds = 61 minutes, 5 seconds
        assertEquals("61:05", formatDuration(3665))
    }

    @Test
    fun formatFileSize_bytes() {
        assertEquals("500 B", formatFileSize(500))
    }

    @Test
    fun formatFileSize_kilobytes() {
        assertEquals("1 KB", formatFileSize(1024))
        assertEquals("1 KB", formatFileSize(1500)) // 1500 / 1024 = 1
    }

    @Test
    fun formatFileSize_megabytes() {
        assertEquals("1 MB", formatFileSize(1024 * 1024))
        assertEquals("2 MB", formatFileSize(2 * 1024 * 1024 + 500))
    }

    @Test
    fun sanitizeFilename_validFilename() {
        assertEquals("test.mp4", sanitizeFilename("test.mp4"))
        assertEquals("IMG_20240101.jpg", sanitizeFilename("IMG_20240101.jpg"))
    }

    @Test
    fun sanitizeFilename_pathTraversal() {
        // Should strip path components
        assertEquals("test.mp4", sanitizeFilename("../test.mp4"))
        assertEquals("test.mp4", sanitizeFilename("foo/bar/test.mp4"))
        assertEquals("passwd", sanitizeFilename("/etc/passwd"))
    }

    @Test
    fun sanitizeFilename_specialChars() {
        // Should replace special characters with underscores
        assertEquals("bad_name.mp4", sanitizeFilename("bad*name.mp4"))
        assertEquals("bad_name.mp4", sanitizeFilename("bad?name.mp4"))
        assertEquals("bad_name.mp4", sanitizeFilename("bad:name.mp4"))
    }

    @Test
    fun sanitizeFilename_spaces() {
        // Should replace spaces with underscores
        assertEquals("My_Video.mp4", sanitizeFilename("My Video.mp4"))
    }
}
