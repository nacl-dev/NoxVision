package com.noxvision.app.ui.hunting

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.noxvision.app.hunting.database.entities.CachedWeather
import com.noxvision.app.hunting.location.HuntingLocationManager
import com.noxvision.app.hunting.moon.MoonPhaseCalculator
import com.noxvision.app.hunting.weather.WeatherCache
import com.noxvision.app.hunting.weather.WeatherIconHelper
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.SettingsSectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { HuntingLocationManager(context) }
    val weatherCache = remember { WeatherCache(context) }

    var weather by remember { mutableStateOf<CachedWeather?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf<Long?>(null) }

    val moonInfo = remember { MoonPhaseCalculator.calculateMoonPhase() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isLoading = true
            scope.launch {
                try {
                    val location = locationManager.getCurrentLocation()
                    if (location != null) {
                        val result = weatherCache.getWeather(location.latitude, location.longitude)
                        weather = result
                        lastUpdated = result?.timestamp
                        errorMessage = null
                    } else {
                        errorMessage = "Position konnte nicht ermittelt werden"
                    }
                } catch (e: Exception) {
                    errorMessage = e.message
                }
                isLoading = false
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        // Try to load cached weather first
        weather = weatherCache.getCachedWeather()
        lastUpdated = weather?.timestamp

        // Then try to refresh
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isLoading = true
            val location = locationManager.getCurrentLocation()
            if (location != null) {
                val result = weatherCache.getWeather(location.latitude, location.longitude)
                weather = result
                lastUpdated = result?.timestamp
            }
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Wetter",
                        color = NightColors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Zurueck",
                            tint = NightColors.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                isLoading = true
                                scope.launch {
                                    val location = locationManager.getCurrentLocation()
                                    if (location != null) {
                                        val result = weatherCache.getWeather(location.latitude, location.longitude, forceRefresh = true)
                                        weather = result
                                        lastUpdated = result?.timestamp
                                    }
                                    isLoading = false
                                }
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = NightColors.primary
                            )
                        } else {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Aktualisieren",
                                tint = NightColors.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightColors.background
                )
            )
        },
        containerColor = NightColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (weather == null && !isLoading) {
                // No weather data
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = NightColors.onBackground,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Keine Wetterdaten",
                            color = NightColors.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary)
                        ) {
                            Text("Wetter abrufen")
                        }
                    }
                }
            } else if (weather != null) {
                val w = weather!!

                // Main weather card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = WeatherIconHelper.getWeatherEmoji(w.icon),
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = WeatherIconHelper.formatTemperature(w.temperature),
                            color = NightColors.onSurface,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "Gefuehlt ${WeatherIconHelper.formatTemperature(w.feelsLike)}",
                            color = NightColors.onBackground,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = w.description.replaceFirstChar { it.uppercase() },
                            color = NightColors.onSurface,
                            fontSize = 16.sp
                        )

                        // Hunting conditions
                        val (isGood, conditionText) = WeatherIconHelper.isGoodHuntingWeather(w)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = conditionText,
                            color = if (isGood) NightColors.success else NightColors.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Wind compass rose
                SettingsSectionHeader(
                    icon = {
                        Icon(
                            Icons.Filled.Air,
                            contentDescription = null,
                            tint = NightColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    title = "WIND"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wind compass
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WindCompass(windDirection = w.windDirection.toFloat())
                        }

                        Column {
                            Text(
                                text = WeatherIconHelper.formatWindSpeed(w.windSpeed),
                                color = NightColors.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "aus ${w.getWindDirectionName()}",
                                color = NightColors.onBackground,
                                fontSize = 14.sp
                            )
                            Text(
                                text = WeatherIconHelper.getWindDescription(w.windSpeed),
                                color = NightColors.primary,
                                fontSize = 12.sp
                            )
                            if (w.windGust != null && w.windGust > w.windSpeed) {
                                Text(
                                    text = "Boeen: ${WeatherIconHelper.formatWindSpeed(w.windGust)}",
                                    color = NightColors.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Details
                SettingsSectionHeader(
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = NightColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    title = "DETAILS"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherDetailRow("Luftfeuchtigkeit", "${w.humidity}%")
                        WeatherDetailRow("Luftdruck", "${w.pressure} hPa")
                        WeatherDetailRow("Bewoelkung", "${w.cloudiness}%")
                        WeatherDetailRow("Sichtweite", "${w.visibility / 1000} km")

                        val timeFormat = SimpleDateFormat("HH:mm", Locale.GERMANY)
                        WeatherDetailRow("Sonnenaufgang", timeFormat.format(Date(w.sunrise)))
                        WeatherDetailRow("Sonnenuntergang", timeFormat.format(Date(w.sunset)))
                    }
                }

                // Moon phase
                SettingsSectionHeader(
                    icon = {
                        Icon(
                            Icons.Filled.NightsStay,
                            contentDescription = null,
                            tint = NightColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    title = "MONDPHASE"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = moonInfo.phase.icon,
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = moonInfo.phase.germanName,
                                color = NightColors.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Beleuchtung: ${moonInfo.illuminationPercent.toInt()}%",
                                color = NightColors.onBackground,
                                fontSize = 12.sp
                            )
                            Text(
                                text = moonInfo.activityPrediction.germanText,
                                color = NightColors.primary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Last updated
                if (lastUpdated != null) {
                    val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
                    Text(
                        text = "Zuletzt aktualisiert: ${format.format(Date(lastUpdated!!))}",
                        color = NightColors.onBackground,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (w.isExpired()) {
                        Text(
                            text = "Daten sind veraltet - bitte aktualisieren",
                            color = NightColors.error,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WeatherDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = NightColors.onBackground,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = NightColors.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WindCompass(windDirection: Float) {
    val primaryColor = NightColors.primary
    val backgroundColor = NightColors.onBackground

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Draw compass circle
        drawCircle(
            color = backgroundColor.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Draw cardinal directions
        val directions = listOf("N", "O", "S", "W")
        val angles = listOf(0f, 90f, 180f, 270f)

        // Draw direction markers
        for (i in 0 until 8) {
            val angle = i * 45f
            rotate(angle, center) {
                drawLine(
                    color = backgroundColor,
                    start = Offset(center.x, center.y - radius + 8),
                    end = Offset(center.x, center.y - radius + 16),
                    strokeWidth = if (i % 2 == 0) 3f else 1f
                )
            }
        }

        // Draw wind direction arrow
        rotate(windDirection, center) {
            // Arrow body
            drawLine(
                color = primaryColor,
                start = Offset(center.x, center.y + radius * 0.4f),
                end = Offset(center.x, center.y - radius * 0.6f),
                strokeWidth = 4f
            )
            // Arrow head
            drawLine(
                color = primaryColor,
                start = Offset(center.x, center.y - radius * 0.6f),
                end = Offset(center.x - 10, center.y - radius * 0.4f),
                strokeWidth = 4f
            )
            drawLine(
                color = primaryColor,
                start = Offset(center.x, center.y - radius * 0.6f),
                end = Offset(center.x + 10, center.y - radius * 0.4f),
                strokeWidth = 4f
            )
        }
    }
}
