package com.noxvision.app.hunting.weather

import com.noxvision.app.hunting.database.entities.CachedWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherIconHelperTest {

    @Test
    fun getWeatherEmoji_KnownCodes() {
        assertEquals("\u2600\uFE0F", WeatherIconHelper.getWeatherEmoji("01d"))
        assertEquals("\uD83C\uDF19", WeatherIconHelper.getWeatherEmoji("01n"))
        assertEquals("\u26C5", WeatherIconHelper.getWeatherEmoji("02d"))
        assertEquals("\u26C5", WeatherIconHelper.getWeatherEmoji("02n"))
        assertEquals("\u2601\uFE0F", WeatherIconHelper.getWeatherEmoji("03d"))
        assertEquals("\u2601\uFE0F", WeatherIconHelper.getWeatherEmoji("03n"))
        assertEquals("\u2601\uFE0F", WeatherIconHelper.getWeatherEmoji("04d"))
        assertEquals("\u2601\uFE0F", WeatherIconHelper.getWeatherEmoji("04n"))
        assertEquals("\uD83C\uDF27\uFE0F", WeatherIconHelper.getWeatherEmoji("09d"))
        assertEquals("\uD83C\uDF27\uFE0F", WeatherIconHelper.getWeatherEmoji("09n"))
        assertEquals("\uD83C\uDF26\uFE0F", WeatherIconHelper.getWeatherEmoji("10d"))
        assertEquals("\uD83C\uDF26\uFE0F", WeatherIconHelper.getWeatherEmoji("10n"))
        assertEquals("\u26C8\uFE0F", WeatherIconHelper.getWeatherEmoji("11d"))
        assertEquals("\u26C8\uFE0F", WeatherIconHelper.getWeatherEmoji("11n"))
        assertEquals("\u2744\uFE0F", WeatherIconHelper.getWeatherEmoji("13d"))
        assertEquals("\u2744\uFE0F", WeatherIconHelper.getWeatherEmoji("13n"))
        assertEquals("\uD83C\uDF2B\uFE0F", WeatherIconHelper.getWeatherEmoji("50d"))
        assertEquals("\uD83C\uDF2B\uFE0F", WeatherIconHelper.getWeatherEmoji("50n"))
    }

    @Test
    fun getWeatherEmoji_UnknownCode() {
        assertEquals("\u2601\uFE0F", WeatherIconHelper.getWeatherEmoji("unknown"))
        assertEquals("\u2601\uFE0F", WeatherIconHelper.getWeatherEmoji(""))
    }

    @Test
    fun getWindDescription_Boundaries() {
        assertEquals("Windstille", WeatherIconHelper.getWindDescription(0.0))
        assertEquals("Windstille", WeatherIconHelper.getWindDescription(0.49))
        assertEquals("Leiser Zug", WeatherIconHelper.getWindDescription(0.5))
        assertEquals("Leiser Zug", WeatherIconHelper.getWindDescription(1.5))
        assertEquals("Leichte Brise", WeatherIconHelper.getWindDescription(1.6))
        assertEquals("Sturm", WeatherIconHelper.getWindDescription(24.0))
        assertEquals("Orkan", WeatherIconHelper.getWindDescription(33.0))
    }

    @Test
    fun formatTemperature_Values() {
        // Use String.format to match the implementation's locale dependency
        val expectedPositive = String.format("%.1f\u00B0C", 20.5)
        assertEquals(expectedPositive, WeatherIconHelper.formatTemperature(20.5))

        val expectedZero = String.format("%.1f\u00B0C", 0.0)
        assertEquals(expectedZero, WeatherIconHelper.formatTemperature(0.0))

        val expectedNegative = String.format("%.1f\u00B0C", -5.23)
        // Note: formatTemperature implementation uses %.1f, so it rounds/truncates
        assertEquals(expectedNegative, WeatherIconHelper.formatTemperature(-5.23))
    }

    @Test
    fun formatWindSpeed_Conversion() {
        // 10 m/s = 36 km/h
        val expected36 = String.format("%.1f km/h", 36.0)
        assertEquals(expected36, WeatherIconHelper.formatWindSpeed(10.0))

        // 0 m/s = 0 km/h
        val expected0 = String.format("%.1f km/h", 0.0)
        assertEquals(expected0, WeatherIconHelper.formatWindSpeed(0.0))
    }

    private fun createWeather(
        windSpeed: Double = 5.0,
        temperature: Double = 20.0,
        visibility: Int = 10000,
        description: String = "Clear sky"
    ): CachedWeather {
        return CachedWeather(
            timestamp = System.currentTimeMillis(),
            latitude = 0.0,
            longitude = 0.0,
            temperature = temperature,
            feelsLike = temperature,
            humidity = 50,
            pressure = 1013,
            windSpeed = windSpeed,
            windDirection = 0,
            windGust = null,
            cloudiness = 0,
            visibility = visibility,
            description = description,
            icon = "01d",
            sunrise = 0,
            sunset = 0
        )
    }

    @Test
    fun isGoodHuntingWeather_GoodConditions() {
        val weather = createWeather()
        val (isGood, message) = WeatherIconHelper.isGoodHuntingWeather(weather)
        assertTrue(isGood)
        assertEquals("Gute Jagdbedingungen", message)
    }

    @Test
    fun isGoodHuntingWeather_BadConditions() {
        // Strong wind
        val windy = createWeather(windSpeed = 9.0)
        val (isGoodWind, msgWind) = WeatherIconHelper.isGoodHuntingWeather(windy)
        assertFalse(isGoodWind)
        assertTrue(msgWind.contains("Starker Wind"))

        // Extreme temperature
        val hot = createWeather(temperature = 31.0)
        val (isGoodHot, msgHot) = WeatherIconHelper.isGoodHuntingWeather(hot)
        assertFalse(isGoodHot)
        assertTrue(msgHot.contains("Extreme Temperatur"))

        val cold = createWeather(temperature = -11.0)
        val (isGoodCold, msgCold) = WeatherIconHelper.isGoodHuntingWeather(cold)
        assertFalse(isGoodCold)
        assertTrue(msgCold.contains("Extreme Temperatur"))

        // Poor visibility
        val foggy = createWeather(visibility = 500)
        val (isGoodFog, msgFog) = WeatherIconHelper.isGoodHuntingWeather(foggy)
        assertFalse(isGoodFog)
        assertTrue(msgFog.contains("Schlechte Sicht"))

        // Thunderstorm
        val storm = createWeather(description = "gewitter mit regen")
        val (isGoodStorm, msgStorm) = WeatherIconHelper.isGoodHuntingWeather(storm)
        assertFalse(isGoodStorm)
        assertTrue(msgStorm.contains("Gewitter"))

        // Heavy rain
        val rain = createWeather(description = "starker regen")
        val (isGoodRain, msgRain) = WeatherIconHelper.isGoodHuntingWeather(rain)
        assertFalse(isGoodRain)
        assertTrue(msgRain.contains("Starker Regen"))
    }

    @Test
    fun isGoodHuntingWeather_MultipleIssues() {
        val badWeather = createWeather(
            windSpeed = 10.0,
            temperature = 35.0,
            visibility = 200,
            description = "gewitter"
        )
        val (isGood, message) = WeatherIconHelper.isGoodHuntingWeather(badWeather)
        assertFalse(isGood)
        assertTrue(message.contains("Starker Wind"))
        assertTrue(message.contains("Extreme Temperatur"))
        assertTrue(message.contains("Schlechte Sicht"))
        assertTrue(message.contains("Gewitter"))
    }
}
