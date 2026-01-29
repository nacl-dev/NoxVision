package com.noxvision.app.hunting.weather

import android.content.Context
import com.noxvision.app.hunting.database.HuntingDatabase
import com.noxvision.app.hunting.database.entities.CachedWeather

class WeatherCache(context: Context) {
    private val weatherDao = HuntingDatabase.getDatabase(context).weatherDao()
    private val apiClient = WeatherApiClient()

    suspend fun getWeather(latitude: Double, longitude: Double, forceRefresh: Boolean = false): CachedWeather? {
        // Try to get cached weather first
        val cached = weatherDao.getCachedWeather()

        if (cached != null && !cached.isExpired() && !forceRefresh) {
            return cached
        }

        // Try to fetch fresh weather
        val result = apiClient.fetchWeather(latitude, longitude)
        if (result.isSuccess) {
            val fresh = result.getOrNull()
            if (fresh != null) {
                weatherDao.insert(fresh)
                return fresh
            }
        }

        // Return cached even if expired (offline mode)
        return cached
    }

    suspend fun getCachedWeather(): CachedWeather? {
        return weatherDao.getCachedWeather()
    }

    suspend fun clearCache() {
        weatherDao.clear()
    }

    suspend fun isOnline(latitude: Double, longitude: Double): Boolean {
        return apiClient.fetchWeather(latitude, longitude).isSuccess
    }
}

object WeatherIconHelper {
    fun getWeatherEmoji(iconCode: String): String {
        return when (iconCode) {
            "01d" -> "\u2600\uFE0F" // Clear day
            "01n" -> "\uD83C\uDF19" // Clear night
            "02d", "02n" -> "\u26C5" // Few clouds
            "03d", "03n" -> "\u2601\uFE0F" // Scattered clouds
            "04d", "04n" -> "\u2601\uFE0F" // Broken clouds
            "09d", "09n" -> "\uD83C\uDF27\uFE0F" // Shower rain
            "10d", "10n" -> "\uD83C\uDF26\uFE0F" // Rain
            "11d", "11n" -> "\u26C8\uFE0F" // Thunderstorm
            "13d", "13n" -> "\u2744\uFE0F" // Snow
            "50d", "50n" -> "\uD83C\uDF2B\uFE0F" // Mist
            else -> "\u2601\uFE0F"
        }
    }

    fun getWindDescription(speedMs: Double): String {
        return when {
            speedMs < 0.5 -> "Windstille"
            speedMs < 1.6 -> "Leiser Zug"
            speedMs < 3.4 -> "Leichte Brise"
            speedMs < 5.5 -> "Schwache Brise"
            speedMs < 8.0 -> "Maessige Brise"
            speedMs < 10.8 -> "Frische Brise"
            speedMs < 13.9 -> "Starker Wind"
            speedMs < 17.2 -> "Steifer Wind"
            speedMs < 20.8 -> "Stuermischer Wind"
            speedMs < 24.5 -> "Sturm"
            speedMs < 28.5 -> "Schwerer Sturm"
            speedMs < 32.7 -> "Orkanartiger Sturm"
            else -> "Orkan"
        }
    }

    fun formatTemperature(celsius: Double): String {
        return String.format("%.1f\u00B0C", celsius)
    }

    fun formatWindSpeed(speedMs: Double): String {
        val kmh = speedMs * 3.6
        return String.format("%.1f km/h", kmh)
    }

    fun isGoodHuntingWeather(weather: CachedWeather): Pair<Boolean, String> {
        val issues = mutableListOf<String>()

        if (weather.windSpeed > 8.0) {
            issues.add("Starker Wind")
        }
        if (weather.temperature < -10 || weather.temperature > 30) {
            issues.add("Extreme Temperatur")
        }
        if (weather.visibility < 1000) {
            issues.add("Schlechte Sicht")
        }
        if (weather.description.contains("gewitter", ignoreCase = true)) {
            issues.add("Gewitter")
        }
        if (weather.description.contains("stark", ignoreCase = true) &&
            weather.description.contains("regen", ignoreCase = true)) {
            issues.add("Starker Regen")
        }

        return if (issues.isEmpty()) {
            Pair(true, "Gute Jagdbedingungen")
        } else {
            Pair(false, issues.joinToString(", "))
        }
    }
}
