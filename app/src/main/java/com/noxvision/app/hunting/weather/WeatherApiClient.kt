package com.noxvision.app.hunting.weather

import com.noxvision.app.hunting.database.entities.CachedWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.noxvision.app.BuildConfig

class WeatherApiClient(private val apiKey: String = DEFAULT_API_KEY) {

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

    private fun parseWeatherResponse(json: String, latitude: Double, longitude: Double): CachedWeather {
        val obj = JSONObject(json)

        val main = obj.getJSONObject("main")
        val wind = obj.getJSONObject("wind")
        val clouds = obj.getJSONObject("clouds")
        val sys = obj.getJSONObject("sys")
        val weather = obj.getJSONArray("weather").getJSONObject(0)

        return CachedWeather(
            id = 0,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            temperature = main.getDouble("temp"),
            feelsLike = main.getDouble("feels_like"),
            humidity = main.getInt("humidity"),
            pressure = main.getInt("pressure"),
            windSpeed = wind.getDouble("speed"),
            windDirection = wind.optInt("deg", 0),
            windGust = wind.optDouble("gust", 0.0).takeIf { it > 0 },
            cloudiness = clouds.getInt("all"),
            visibility = obj.optInt("visibility", 10000),
            description = weather.getString("description"),
            icon = weather.getString("icon"),
            sunrise = sys.getLong("sunrise") * 1000,
            sunset = sys.getLong("sunset") * 1000
        )
    }

    fun isApiKeyValid(): Boolean {
        return apiKey.isNotEmpty()
    }
}
