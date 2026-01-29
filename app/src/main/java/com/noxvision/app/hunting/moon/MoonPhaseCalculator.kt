package com.noxvision.app.hunting.moon

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

object MoonPhaseCalculator {
    // Synodic month in days (average time between new moons)
    private const val SYNODIC_MONTH = 29.530588853

    // Reference new moon: January 6, 2000, 18:14 UTC
    private const val REFERENCE_NEW_MOON_JD = 2451550.1

    enum class MoonPhase(val germanName: String, val icon: String, val illumination: String) {
        NEW_MOON("Neumond", "\uD83C\uDF11", "0%"),
        WAXING_CRESCENT("Zunehmender Sichelmond", "\uD83C\uDF12", "1-49%"),
        FIRST_QUARTER("Erstes Viertel", "\uD83C\uDF13", "50%"),
        WAXING_GIBBOUS("Zunehmender Dreiviertelmond", "\uD83C\uDF14", "51-99%"),
        FULL_MOON("Vollmond", "\uD83C\uDF15", "100%"),
        WANING_GIBBOUS("Abnehmender Dreiviertelmond", "\uD83C\uDF16", "99-51%"),
        LAST_QUARTER("Letztes Viertel", "\uD83C\uDF17", "50%"),
        WANING_CRESCENT("Abnehmender Sichelmond", "\uD83C\uDF18", "49-1%")
    }

    data class MoonInfo(
        val phase: MoonPhase,
        val daysSinceNewMoon: Double,
        val daysUntilNextNewMoon: Double,
        val illuminationPercent: Double,
        val activityPrediction: WildlifeActivityPrediction
    )

    enum class WildlifeActivityPrediction(val germanText: String, val level: Int) {
        VERY_HIGH("Sehr hohe Aktivitaet", 5),
        HIGH("Hohe Aktivitaet", 4),
        MEDIUM("Mittlere Aktivitaet", 3),
        LOW("Geringe Aktivitaet", 2),
        VERY_LOW("Sehr geringe Aktivitaet", 1)
    }

    fun calculateMoonPhase(timestamp: Long = System.currentTimeMillis()): MoonInfo {
        val julianDate = toJulianDate(timestamp)
        val daysSinceRef = julianDate - REFERENCE_NEW_MOON_JD
        val lunarCycles = daysSinceRef / SYNODIC_MONTH
        val currentCycleProgress = lunarCycles - floor(lunarCycles)
        val daysSinceNewMoon = currentCycleProgress * SYNODIC_MONTH
        val daysUntilNextNewMoon = SYNODIC_MONTH - daysSinceNewMoon

        val phase = getPhaseFromCycleProgress(currentCycleProgress)
        val illumination = calculateIllumination(currentCycleProgress)
        val activity = predictWildlifeActivity(phase, illumination)

        return MoonInfo(
            phase = phase,
            daysSinceNewMoon = daysSinceNewMoon,
            daysUntilNextNewMoon = daysUntilNextNewMoon,
            illuminationPercent = illumination,
            activityPrediction = activity
        )
    }

    private fun toJulianDate(timestamp: Long): Double {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timestamp

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val dayFraction = (hour + minute / 60.0 + second / 3600.0) / 24.0

        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3

        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        return jdn + dayFraction - 0.5
    }

    private fun getPhaseFromCycleProgress(progress: Double): MoonPhase {
        return when {
            progress < 0.0625 -> MoonPhase.NEW_MOON
            progress < 0.1875 -> MoonPhase.WAXING_CRESCENT
            progress < 0.3125 -> MoonPhase.FIRST_QUARTER
            progress < 0.4375 -> MoonPhase.WAXING_GIBBOUS
            progress < 0.5625 -> MoonPhase.FULL_MOON
            progress < 0.6875 -> MoonPhase.WANING_GIBBOUS
            progress < 0.8125 -> MoonPhase.LAST_QUARTER
            progress < 0.9375 -> MoonPhase.WANING_CRESCENT
            else -> MoonPhase.NEW_MOON
        }
    }

    private fun calculateIllumination(progress: Double): Double {
        // Using cosine function for illumination approximation
        // At new moon (progress = 0), illumination = 0
        // At full moon (progress = 0.5), illumination = 100
        val angle = progress * 2 * Math.PI
        return ((1 - kotlin.math.cos(angle)) / 2) * 100
    }

    private fun predictWildlifeActivity(phase: MoonPhase, illumination: Double): WildlifeActivityPrediction {
        // Wildlife activity tends to be higher during bright moonlit nights
        // and around the full moon period
        return when (phase) {
            MoonPhase.FULL_MOON -> WildlifeActivityPrediction.VERY_HIGH
            MoonPhase.WAXING_GIBBOUS, MoonPhase.WANING_GIBBOUS -> WildlifeActivityPrediction.HIGH
            MoonPhase.FIRST_QUARTER, MoonPhase.LAST_QUARTER -> WildlifeActivityPrediction.MEDIUM
            MoonPhase.WAXING_CRESCENT, MoonPhase.WANING_CRESCENT -> WildlifeActivityPrediction.LOW
            MoonPhase.NEW_MOON -> WildlifeActivityPrediction.VERY_LOW
        }
    }

    fun getNextFullMoon(fromTimestamp: Long = System.currentTimeMillis()): Long {
        val info = calculateMoonPhase(fromTimestamp)
        val daysUntilFull = when {
            info.daysSinceNewMoon < SYNODIC_MONTH / 2 -> (SYNODIC_MONTH / 2) - info.daysSinceNewMoon
            else -> SYNODIC_MONTH - info.daysSinceNewMoon + (SYNODIC_MONTH / 2)
        }
        return fromTimestamp + (daysUntilFull * 24 * 60 * 60 * 1000).toLong()
    }

    fun getNextNewMoon(fromTimestamp: Long = System.currentTimeMillis()): Long {
        val info = calculateMoonPhase(fromTimestamp)
        return fromTimestamp + (info.daysUntilNextNewMoon * 24 * 60 * 60 * 1000).toLong()
    }
}
