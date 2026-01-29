package com.noxvision.app.hunting.calendar

import java.util.Calendar

object HuntingSeasonData {
    enum class Bundesland(val displayName: String) {
        BADEN_WUERTTEMBERG("Baden-Wuerttemberg"),
        BAYERN("Bayern"),
        BERLIN("Berlin"),
        BRANDENBURG("Brandenburg"),
        BREMEN("Bremen"),
        HAMBURG("Hamburg"),
        HESSEN("Hessen"),
        MECKLENBURG_VORPOMMERN("Mecklenburg-Vorpommern"),
        NIEDERSACHSEN("Niedersachsen"),
        NORDRHEIN_WESTFALEN("Nordrhein-Westfalen"),
        RHEINLAND_PFALZ("Rheinland-Pfalz"),
        SAARLAND("Saarland"),
        SACHSEN("Sachsen"),
        SACHSEN_ANHALT("Sachsen-Anhalt"),
        SCHLESWIG_HOLSTEIN("Schleswig-Holstein"),
        THUERINGEN("Thueringen")
    }

    data class HuntingSeason(
        val wildlifeType: String,
        val gender: String?,
        val startMonth: Int,
        val startDay: Int,
        val endMonth: Int,
        val endDay: Int,
        val notes: String? = null
    ) {
        fun isInSeason(month: Int, day: Int): Boolean {
            val start = startMonth * 100 + startDay
            val end = endMonth * 100 + endDay
            val current = month * 100 + day

            return if (start <= end) {
                current in start..end
            } else {
                current >= start || current <= end
            }
        }

        fun formatSeasonPeriod(): String {
            val startMonthName = getMonthName(startMonth)
            val endMonthName = getMonthName(endMonth)
            return "$startDay. $startMonthName - $endDay. $endMonthName"
        }

        private fun getMonthName(month: Int): String {
            return when (month) {
                1 -> "Jan"
                2 -> "Feb"
                3 -> "Maerz"
                4 -> "Apr"
                5 -> "Mai"
                6 -> "Juni"
                7 -> "Juli"
                8 -> "Aug"
                9 -> "Sep"
                10 -> "Okt"
                11 -> "Nov"
                12 -> "Dez"
                else -> ""
            }
        }
    }

    // Bundesjagdzeiten (Basis fuer alle Bundeslaender)
    private val federalSeasons = listOf(
        // Rehwild
        HuntingSeason("Rehwild", "Bock", 5, 1, 10, 15),
        HuntingSeason("Rehwild", "Ricke", 9, 1, 1, 31),
        HuntingSeason("Rehwild", "Kitz", 9, 1, 2, 28),

        // Rotwild
        HuntingSeason("Rotwild", "Hirsch", 8, 1, 1, 31),
        HuntingSeason("Rotwild", "Tier", 8, 1, 1, 31),
        HuntingSeason("Rotwild", "Kalb", 8, 1, 2, 28),

        // Damwild
        HuntingSeason("Damwild", "Hirsch", 9, 1, 1, 31),
        HuntingSeason("Damwild", "Tier", 9, 1, 1, 31),
        HuntingSeason("Damwild", "Kalb", 9, 1, 2, 28),

        // Schwarzwild - ganzjaehrig
        HuntingSeason("Schwarzwild", "Keiler", 1, 1, 12, 31),
        HuntingSeason("Schwarzwild", "Bache", 1, 1, 12, 31, "Fuehrende Bachen geschont"),
        HuntingSeason("Schwarzwild", "Frischling", 1, 1, 12, 31),
        HuntingSeason("Schwarzwild", "Ueberlaeufer", 1, 1, 12, 31),

        // Muffelwild
        HuntingSeason("Muffelwild", "Widder", 8, 1, 1, 31),
        HuntingSeason("Muffelwild", "Schaf", 9, 1, 1, 31),
        HuntingSeason("Muffelwild", "Lamm", 9, 1, 1, 31),

        // Raubwild
        HuntingSeason("Raubwild", "Fuchs", 7, 16, 2, 28),
        HuntingSeason("Raubwild", "Dachs", 8, 1, 10, 31),
        HuntingSeason("Raubwild", "Waschbaer", 1, 1, 12, 31),
        HuntingSeason("Raubwild", "Marderhund", 1, 1, 12, 31),
        HuntingSeason("Raubwild", "Marder", 10, 16, 2, 28),

        // Niederwild
        HuntingSeason("Niederwild", "Hase", 10, 1, 1, 15),
        HuntingSeason("Niederwild", "Wildkaninchen", 1, 1, 12, 31),

        // Federwild
        HuntingSeason("Federwild", "Fasan", 10, 1, 1, 15),
        HuntingSeason("Federwild", "Wildente", 9, 1, 1, 15),
        HuntingSeason("Federwild", "Wildgans", 9, 1, 1, 15),
        HuntingSeason("Federwild", "Taube", 11, 1, 2, 20),
        HuntingSeason("Federwild", "Kraehe", 8, 1, 2, 20)
    )

    fun getSeasonsForBundesland(bundesland: Bundesland): List<HuntingSeason> {
        // For now, return federal seasons
        // In a complete implementation, this would have state-specific overrides
        return federalSeasons
    }

    fun getSeasonsInEffect(bundesland: Bundesland): List<HuntingSeason> {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return getSeasonsForBundesland(bundesland).filter { it.isInSeason(month, day) }
    }

    fun getUpcomingSeasons(bundesland: Bundesland, withinDays: Int = 30): List<Pair<HuntingSeason, Int>> {
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar

        return getSeasonsForBundesland(bundesland)
            .filter { season ->
                val month = today.get(Calendar.MONTH) + 1
                val day = today.get(Calendar.DAY_OF_MONTH)
                !season.isInSeason(month, day)
            }
            .mapNotNull { season ->
                val daysUntil = calculateDaysUntilSeason(season)
                if (daysUntil in 1..withinDays) {
                    Pair(season, daysUntil)
                } else {
                    null
                }
            }
            .sortedBy { it.second }
    }

    private fun calculateDaysUntilSeason(season: HuntingSeason): Int {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)

        val seasonStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, season.startMonth - 1)
            set(Calendar.DAY_OF_MONTH, season.startDay)
        }

        if (seasonStart.before(today)) {
            seasonStart.add(Calendar.YEAR, 1)
        }

        val diff = seasonStart.timeInMillis - today.timeInMillis
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun isWildlifeInSeason(wildlifeType: String, gender: String?, bundesland: Bundesland): Boolean {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return getSeasonsForBundesland(bundesland).any { season ->
            season.wildlifeType == wildlifeType &&
                    (gender == null || season.gender == null || season.gender == gender) &&
                    season.isInSeason(month, day)
        }
    }
}
