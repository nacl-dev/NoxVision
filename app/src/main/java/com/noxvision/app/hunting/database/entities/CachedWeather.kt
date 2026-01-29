package com.noxvision.app.hunting.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_weather")
data class CachedWeather(
    @PrimaryKey val id: Int = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val windGust: Double?,
    val cloudiness: Int,
    val visibility: Int,
    val description: String,
    val icon: String,
    val sunrise: Long,
    val sunset: Long
) {
    fun isExpired(maxAgeMinutes: Int = 30): Boolean {
        val now = System.currentTimeMillis()
        val ageMinutes = (now - timestamp) / (1000 * 60)
        return ageMinutes > maxAgeMinutes
    }

    fun getWindDirectionName(): String {
        return when {
            windDirection < 23 -> "N"
            windDirection < 68 -> "NO"
            windDirection < 113 -> "O"
            windDirection < 158 -> "SO"
            windDirection < 203 -> "S"
            windDirection < 248 -> "SW"
            windDirection < 293 -> "W"
            windDirection < 338 -> "NW"
            else -> "N"
        }
    }
}
