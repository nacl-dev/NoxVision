package com.noxvision.app.hunting.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherApiClientTest {

    private val client = WeatherApiClient("test-key")

    @Test
    fun parseWeatherResponse_validJson_returnsCachedWeather() {
        val json = """
            {
              "coord": {
                "lon": 13.405,
                "lat": 52.52
              },
              "weather": [
                {
                  "id": 800,
                  "main": "Clear",
                  "description": "clear sky",
                  "icon": "01d"
                }
              ],
              "base": "stations",
              "main": {
                "temp": 289.92,
                "feels_like": 287.55,
                "temp_min": 288.71,
                "temp_max": 290.93,
                "pressure": 1012,
                "humidity": 40
              },
              "visibility": 10000,
              "wind": {
                "speed": 3.6,
                "deg": 350,
                "gust": 8.0
              },
              "clouds": {
                "all": 0
              },
              "dt": 1560350645,
              "sys": {
                "type": 1,
                "id": 1414,
                "message": 0.0103,
                "country": "DE",
                "sunrise": 1560341379,
                "sunset": 1560393310
              },
              "timezone": 7200,
              "id": 420006353,
              "name": "Berlin",
              "cod": 200
            }
        """.trimIndent()

        val latitude = 52.52
        val longitude = 13.405
        val result = client.parseWeatherResponse(json, latitude, longitude)

        assertEquals(latitude, result.latitude, 0.0001)
        assertEquals(longitude, result.longitude, 0.0001)
        assertEquals(289.92, result.temperature, 0.0001)
        assertEquals(287.55, result.feelsLike, 0.0001)
        assertEquals(40, result.humidity)
        assertEquals(1012, result.pressure)
        assertEquals(3.6, result.windSpeed, 0.0001)
        assertEquals(350, result.windDirection)
        assertEquals(8.0, result.windGust!!, 0.0001)
        assertEquals(0, result.cloudiness)
        assertEquals(10000, result.visibility)
        assertEquals("clear sky", result.description)
        assertEquals("01d", result.icon)
        assertEquals(1560341379000L, result.sunrise)
        assertEquals(1560393310000L, result.sunset)
    }

    @Test
    fun parseWeatherResponse_missingOptionalFields_usesDefaults() {
        val json = """
            {
              "weather": [
                {
                  "description": "cloudy",
                  "icon": "02d"
                }
              ],
              "main": {
                "temp": 15.0,
                "feels_like": 14.0,
                "humidity": 60,
                "pressure": 1000
              },
              "wind": {
                "speed": 5.0
              },
              "clouds": {
                "all": 20
              },
              "sys": {
                "sunrise": 1000,
                "sunset": 2000
              }
            }
        """.trimIndent()

        val result = client.parseWeatherResponse(json, 10.0, 20.0)

        // Default visibility is 10000
        assertEquals(10000, result.visibility)
        // Default wind deg is 0
        assertEquals(0, result.windDirection)
        // Default wind gust is null (because default value 0.0 is filtered out)
        assertEquals(null, result.windGust)
    }
}
