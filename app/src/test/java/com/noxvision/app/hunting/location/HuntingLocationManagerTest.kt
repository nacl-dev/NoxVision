package com.noxvision.app.hunting.location

import org.junit.Assert.assertEquals
import org.junit.Test

class HuntingLocationManagerTest {

    @Test
    fun testCalculateDistance_SamePoint() {
        val distance = HuntingLocationManager.calculateDistance(52.52, 13.405, 52.52, 13.405)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testCalculateDistance_EquatorOneDegree() {
        val distance = HuntingLocationManager.calculateDistance(0.0, 0.0, 0.0, 1.0)
        // Earth radius 6371000.0 * Math.toRadians(1.0)
        val expected = 6371000.0 * Math.PI / 180.0
        assertEquals(expected, distance, 0.1)
    }

    @Test
    fun testCalculateDistance_ParisToLondon() {
        val lat1 = 48.8566
        val lon1 = 2.3522
        val lat2 = 51.5074
        val lon2 = -0.1278

        val distance = HuntingLocationManager.calculateDistance(lat1, lon1, lat2, lon2)
        // Paris to London is approx 343.56 km using the implemented Haversine formula
        assertEquals(343556.0, distance, 1.0)
    }

    @Test
    fun testCalculateBearing_North() {
        val bearing = HuntingLocationManager.calculateBearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0f, bearing, 0.001f)
    }

    @Test
    fun testCalculateBearing_East() {
        val bearing = HuntingLocationManager.calculateBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0f, bearing, 0.001f)
    }

    @Test
    fun testCalculateBearing_South() {
        val bearing = HuntingLocationManager.calculateBearing(0.0, 0.0, -1.0, 0.0)
        assertEquals(180.0f, bearing, 0.001f)
    }

    @Test
    fun testCalculateBearing_West() {
        val bearing = HuntingLocationManager.calculateBearing(0.0, 0.0, 0.0, -1.0)
        assertEquals(270.0f, bearing, 0.001f)
    }

    @Test
    fun testFormatDistance_Meters() {
        assertEquals("123 m", HuntingLocationManager.formatDistance(123.4))
        assertEquals("999 m", HuntingLocationManager.formatDistance(999.9))
    }

    @Test
    fun testFormatDistance_Kilometers() {
        assertEquals("1.0 km", HuntingLocationManager.formatDistance(1000.0))
        assertEquals("1.5 km", HuntingLocationManager.formatDistance(1500.0))
        assertEquals("10.2 km", HuntingLocationManager.formatDistance(10200.0))
    }

    @Test
    fun testFormatCoordinates_NorthEast() {
        assertEquals("52.520000N, 13.405000E", HuntingLocationManager.formatCoordinates(52.52, 13.405))
    }

    @Test
    fun testFormatCoordinates_SouthWest() {
        assertEquals("22.906800S, 43.172900W", HuntingLocationManager.formatCoordinates(-22.9068, -43.1729))
    }

    @Test
    fun testFormatCoordinates_Zero() {
        assertEquals("0.000000N, 0.000000E", HuntingLocationManager.formatCoordinates(0.0, 0.0))
    }
}
