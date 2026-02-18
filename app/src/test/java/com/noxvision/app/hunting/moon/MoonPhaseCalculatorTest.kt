package com.noxvision.app.hunting.moon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MoonPhaseCalculatorTest {

    // Reference New Moon: January 6, 2000, 18:14 UTC
    private val REFERENCE_TIMESTAMP: Long = run {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(2000, Calendar.JANUARY, 6, 18, 14, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

    private val SYNODIC_MONTH = 29.530588853 // in days
    private val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    private fun getTimestampAfterDays(days: Double): Long {
        return REFERENCE_TIMESTAMP + (days * MILLIS_PER_DAY).toLong()
    }

    @Test
    fun testReferenceNewMoon() {
        val info = MoonPhaseCalculator.calculateMoonPhase(REFERENCE_TIMESTAMP)
        assertEquals(MoonPhaseCalculator.MoonPhase.NEW_MOON, info.phase)
        assertEquals(0.0, info.illuminationPercent, 0.1)
    }

    @Test
    fun testCalculatedFullMoon() {
        // Full Moon is at 0.5 of the cycle
        val timestamp = getTimestampAfterDays(SYNODIC_MONTH * 0.5)
        val info = MoonPhaseCalculator.calculateMoonPhase(timestamp)

        assertEquals(MoonPhaseCalculator.MoonPhase.FULL_MOON, info.phase)
        assertEquals(100.0, info.illuminationPercent, 0.1)
    }

    @Test
    fun testFirstQuarter() {
        // First Quarter is at 0.25 of the cycle
        val timestamp = getTimestampAfterDays(SYNODIC_MONTH * 0.25)
        val info = MoonPhaseCalculator.calculateMoonPhase(timestamp)

        assertEquals(MoonPhaseCalculator.MoonPhase.FIRST_QUARTER, info.phase)
        // Illumination should be around 50%
        assertEquals(50.0, info.illuminationPercent, 0.1)
    }

    @Test
    fun testLastQuarter() {
        // Last Quarter is at 0.75 of the cycle
        val timestamp = getTimestampAfterDays(SYNODIC_MONTH * 0.75)
        val info = MoonPhaseCalculator.calculateMoonPhase(timestamp)

        assertEquals(MoonPhaseCalculator.MoonPhase.LAST_QUARTER, info.phase)
        // Illumination should be around 50%
        assertEquals(50.0, info.illuminationPercent, 0.1)
    }

    @Test
    fun testWildlifeActivityPrediction() {
        // Full Moon -> VERY_HIGH
        val fullMoonTs = getTimestampAfterDays(SYNODIC_MONTH * 0.5)
        val fullMoonInfo = MoonPhaseCalculator.calculateMoonPhase(fullMoonTs)
        assertEquals(MoonPhaseCalculator.WildlifeActivityPrediction.VERY_HIGH, fullMoonInfo.activityPrediction)

        // New Moon -> VERY_LOW
        val newMoonInfo = MoonPhaseCalculator.calculateMoonPhase(REFERENCE_TIMESTAMP)
        assertEquals(MoonPhaseCalculator.WildlifeActivityPrediction.VERY_LOW, newMoonInfo.activityPrediction)

        // First Quarter -> MEDIUM
        val firstQuarterTs = getTimestampAfterDays(SYNODIC_MONTH * 0.25)
        val firstQuarterInfo = MoonPhaseCalculator.calculateMoonPhase(firstQuarterTs)
        assertEquals(MoonPhaseCalculator.WildlifeActivityPrediction.MEDIUM, firstQuarterInfo.activityPrediction)
    }

    @Test
    fun testNextFullMoon() {
        // Start from New Moon. Next Full Moon should be ~14.7 days later
        val startTs = REFERENCE_TIMESTAMP
        val nextFullMoonTs = MoonPhaseCalculator.getNextFullMoon(startTs)

        val diff = nextFullMoonTs - startTs
        val expectedDiff = (SYNODIC_MONTH * 0.5 * MILLIS_PER_DAY).toLong()

        // Allow some margin because getNextFullMoon might round to nearest day or have logic quirks
        // The implementation calculates daysUntilFull based on phase info
        assertEquals(expectedDiff.toDouble(), diff.toDouble(), (1000 * 60).toDouble()) // within 1 minute
    }

    @Test
    fun testNextNewMoon() {
        // Start from New Moon + 1 day. Next New Moon should be ~28.5 days later
        val startTs = getTimestampAfterDays(1.0)
        val nextNewMoonTs = MoonPhaseCalculator.getNextNewMoon(startTs)

        val diff = nextNewMoonTs - startTs
        val expectedDiff = ((SYNODIC_MONTH - 1.0) * MILLIS_PER_DAY).toLong()

        assertEquals(expectedDiff.toDouble(), diff.toDouble(), (1000 * 60).toDouble()) // within 1 minute
    }
}
