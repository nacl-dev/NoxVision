package com.noxvision.app.hunting.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val main: Main,
    val wind: Wind,
    val clouds: Clouds,
    val sys: Sys,
    val weather: List<WeatherCondition>,
    val visibility: Int? = null
)

@Serializable
data class Main(
    val temp: Double,
    @SerialName("feels_like")
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int
)

@Serializable
data class Wind(
    val speed: Double,
    val deg: Int? = null,
    val gust: Double? = null
)

@Serializable
data class Clouds(
    val all: Int
)

@Serializable
data class Sys(
    val sunrise: Long,
    val sunset: Long
)

@Serializable
data class WeatherCondition(
    val description: String,
    val icon: String
)
