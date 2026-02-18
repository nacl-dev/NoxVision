package com.noxvision.app.hunting.location

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassSensorTest {

    @Test
    fun testCalculateRelativeBearing_Same() {
        val compass = 90f
        val target = 90f
        val relative = CompassSensor.calculateRelativeBearing(compass, target)
        assertEquals(0f, relative, 0.001f)
    }

    @Test
    fun testCalculateRelativeBearing_RightTurn() {
        // Target is 90 degrees to the right of compass (e.g., Compass 0, Target 90)
        val compass = 0f
        val target = 90f
        val relative = CompassSensor.calculateRelativeBearing(compass, target)
        assertEquals(90f, relative, 0.001f)
    }

    @Test
    fun testCalculateRelativeBearing_LeftTurn() {
        // Target is 90 degrees to the left of compass (e.g., Compass 90, Target 0)
        val compass = 90f
        val target = 0f
        val relative = CompassSensor.calculateRelativeBearing(compass, target)
        assertEquals(-90f, relative, 0.001f)
    }

    @Test
    fun testCalculateRelativeBearing_WrapRight() {
        // Target is across 0 degrees to the right (e.g., Compass 350, Target 10)
        // Expected: Turn right 20 degrees
        val compass = 350f
        val target = 10f
        val relative = CompassSensor.calculateRelativeBearing(compass, target)
        assertEquals(20f, relative, 0.001f)
    }

    @Test
    fun testCalculateRelativeBearing_WrapLeft() {
        // Target is across 0 degrees to the left (e.g., Compass 10, Target 350)
        // Expected: Turn left 20 degrees -> -20
        val compass = 10f
        val target = 350f
        val relative = CompassSensor.calculateRelativeBearing(compass, target)
        assertEquals(-20f, relative, 0.001f)
    }

    @Test
    fun testCalculateRelativeBearing_ExactOpposite() {
        // Target is exactly opposite (180 degrees)
        val compass = 0f
        val target = 180f
        val relative = CompassSensor.calculateRelativeBearing(compass, target)
        assertEquals(180f, relative, 0.001f)
    }
}
