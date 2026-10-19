package com.noxvision.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuideControlFramesTest {

    @Test
    fun whitehotPaletteFrameMatchesKnownBytes() {
        val frame = GuideControlFrames.paletteFrame(GuideControlFrames.paletteId("whitehot")!!)
        assertEquals(
            byteArrayOf(
                0x55, 0xAA.toByte(),
                0x07,
                0x02, 0x00, 0x04,
                0x00, 0x00, 0x00, 0x00,
                0x01,
                0xF0.toByte()
            ).toList(),
            frame.toList()
        )
    }

    @Test
    fun hotspotOnUsesHotspotCommandAndValueOne() {
        val frame = GuideControlFrames.hotspotFrame(true)
        assertEquals(
            byteArrayOf(
                0x55, 0xAA.toByte(),
                0x07,
                0x02, 0x00, 0x09,
                0x00, 0x00, 0x00, 0x01,
                0x0D,
                0xF0.toByte()
            ).toList(),
            frame.toList()
        )
    }

    @Test
    fun unknownPaletteNameIsRejected() {
        assertNull(GuideControlFrames.paletteId("lava"))
        assertEquals(0, GuideControlFrames.paletteId("whitehot"))
        assertEquals(20, GuideControlFrames.paletteId("darkbrown"))
    }
}
