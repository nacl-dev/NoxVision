package com.noxvision.app.hunting.weather

import com.noxvision.app.hunting.database.entities.CachedWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import com.noxvision.app.BuildConfig

class WeatherApiClient(private val apiKey: String = DEFAULT_API_KEY) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    companion object {
        // OpenWeatherMap Free Tier API Key loaded from local.properties via BuildConfig
        private val DEFAULT_API_KEY = BuildConfig.OPENWEATHER_API_KEY
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): Result<CachedWeather> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric&lang=de")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val weather = parseWeatherResponse(response, latitude, longitude)
                    Result.success(weather)
                } else {
                    connection.disconnect()
                    Result.failure(Exception("HTTP Error: $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    internal fun parseWeatherResponse(json: String, latitude: Double, longitude: Double): CachedWeather {
        val response = jsonParser.decodeFromString<WeatherResponse>(json)

        val main = response.main
        val wind = response.wind
        val clouds = response.clouds
        val sys = response.sys
        val weather = response.weather[0]

        return CachedWeather(
            id = 0,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            temperature = main.temp,
            feelsLike = main.feelsLike,
            humidity = main.humidity,
            pressure = main.pressure,
            windSpeed = wind.speed,
            windDirection = wind.deg ?: 0,
            windGust = wind.gust?.takeIf { it > 0.0 },
            cloudiness = clouds.all,
            visibility = response.visibility ?: 10000,
            description = weather.description,
            icon = weather.icon,
            sunrise = sys.sunrise * 1000,
            sunset = sys.sunset * 1000
        )
    }
}
