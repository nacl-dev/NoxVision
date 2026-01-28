package com.noxvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.CameraCapabilities
import com.noxvision.app.DeviceInfo
import com.noxvision.app.EmissivityPresets
import com.noxvision.app.ui.NightColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalSettingsScreen(
    deviceInfo: DeviceInfo?,
    capabilities: CameraCapabilities?,
    emissivity: Float,
    measureDistance: Float,
    humidity: Float,
    reflectTemperature: Float,
    isShutterInProgress: Boolean,
    onClose: () -> Unit,
    onEmissivityChange: (Float) -> Unit,
    onDistanceChange: (Float) -> Unit,
    onHumidityChange: (Float) -> Unit,
    onReflectTempChange: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onApplySettings: () -> Unit
) {
    var localEmissivity by remember(emissivity) { mutableFloatStateOf(emissivity) }
    var localDistance by remember(measureDistance) { mutableFloatStateOf(measureDistance) }
    var localHumidity by remember(humidity) { mutableFloatStateOf(humidity) }
    var localReflectTemp by remember(reflectTemperature) { mutableFloatStateOf(reflectTemperature) }
    
    // Emissivity preset dropdown
    var showEmissivityPresets by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Thermische Einstellungen", color = NightColors.onSurface, fontSize = 18.sp)
                        if (deviceInfo != null) {
                            Text(
                                text = "${deviceInfo.deviceName} • ${deviceInfo.videoWidth}x${deviceInfo.videoHeight}",
                                fontSize = 12.sp,
                                color = NightColors.onBackground
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück", tint = NightColors.onSurface)
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
            // Device Info Section (if available)
            if (deviceInfo != null && capabilities != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightColors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Kamera-Features",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NightColors.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (capabilities.hasRadiometry) {
                                Text("🌡️ Radiometrie", fontSize = 11.sp, color = NightColors.success)
                            }
                            if (capabilities.hasFocus) {
                                Text("🔍 Fokus", fontSize = 11.sp, color = NightColors.success)
                            }
                            if (capabilities.hasGps) {
                                Text("📍 GPS", fontSize = 11.sp, color = NightColors.success)
                            }
                        }
                    }
                }
            }

            // Shutter / NUC Button
            Button(
                onClick = onShutterClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isShutterInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NightColors.primary
                )
            ) {
                if (isShutterInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kalibriere...")
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shutter / NUC Kalibrierung")
                }
            }

            HorizontalDivider(color = NightColors.surface)

            // Emissivity
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Emissivität: ${"%.2f".format(localEmissivity)}", color = NightColors.onSurface)
                    TextButton(onClick = { showEmissivityPresets = true }) {
                        Text("Presets", color = NightColors.primary)
                    }
                }
                Slider(
                    value = localEmissivity,
                    onValueChange = { localEmissivity = it },
                    onValueChangeFinished = { onEmissivityChange(localEmissivity) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NightColors.primary,
                        activeTrackColor = NightColors.primary
                    )
                )
                
                if (showEmissivityPresets) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Haut" to 0.98f,
                            "Holz" to 0.94f,
                            "Stahl" to 0.80f,
                            "Alu" to 0.30f
                        ).forEach { (name, value) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    localEmissivity = value
                                    onEmissivityChange(value)
                                    showEmissivityPresets = false
                                },
                                label = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // Distance
            Column {
                Text("Entfernung: ${"%.1f".format(localDistance)} m", color = NightColors.onSurface)
                Slider(
                    value = localDistance,
                    onValueChange = { localDistance = it },
                    onValueChangeFinished = { onDistanceChange(localDistance) },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NightColors.primary,
                        activeTrackColor = NightColors.primary
                    )
                )
            }

            // Humidity
            Column {
                Text("Luftfeuchtigkeit: ${"%.0f".format(localHumidity)} %", color = NightColors.onSurface)
                Slider(
                    value = localHumidity,
                    onValueChange = { localHumidity = it },
                    onValueChangeFinished = { onHumidityChange(localHumidity) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NightColors.primary,
                        activeTrackColor = NightColors.primary
                    )
                )
            }

            // Reflect Temperature
            Column {
                Text("Reflexionstemperatur: ${"%.1f".format(localReflectTemp)} °C", color = NightColors.onSurface)
                Slider(
                    value = localReflectTemp,
                    onValueChange = { localReflectTemp = it },
                    onValueChangeFinished = { onReflectTempChange(localReflectTemp) },
                    valueRange = -20f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = NightColors.primary,
                        activeTrackColor = NightColors.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onApplySettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Einstellungen anwenden")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
