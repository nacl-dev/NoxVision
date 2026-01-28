package com.noxvision.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.CameraSettings
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.SettingsSectionHeader
import com.noxvision.app.ui.SettingsToggleRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    audioEnabled: Boolean,
    hotspotEnabled: Boolean,
    brightness: Int,
    contrast: Int,
    enhancementEnabled: Boolean,
    objectDetectionEnabled: Boolean,
    cameraIp: String,
    onClose: () -> Unit,
    onAudioChange: (Boolean) -> Unit,
    onHotspotChange: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onContrastChange: (Int) -> Unit,
    onEnhancementChange: (Boolean) -> Unit,
    onObjectDetectionChange: (Boolean) -> Unit,
    onCameraIpChange: (String) -> Unit,
    onShowLog: () -> Unit,
    onShowAbout: () -> Unit,
    onShowThermalSettings: () -> Unit,
    onWifiSsidChange: (String) -> Unit,
    onWifiPasswordChange: (String) -> Unit,
    onHttpPortChange: (Int) -> Unit,
    onAutoConnectChange: (Boolean) -> Unit,
    onShowWhatsNew: () -> Unit,
    onShowFeatureBounties: () -> Unit
) {
    var editingIp by remember { mutableStateOf(cameraIp) }
    var ipError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var editingSsid by remember { mutableStateOf(CameraSettings.getWifiSsid(context)) }
    var editingPassword by remember { mutableStateOf(CameraSettings.getWifiPassword(context)) }
    var editingPort by remember { mutableStateOf(CameraSettings.getHttpPort(context).toString()) }
    var autoConnectEnabled by remember { mutableStateOf(CameraSettings.isAutoConnectEnabled(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen", color = NightColors.onSurface) },
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
            // ═══════════════════════════════════════════════════════════
            // 🔌 VERBINDUNG
            // ═══════════════════════════════════════════════════════════
            SettingsSectionHeader(
                icon = { Icon(Icons.Filled.Router, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                title = "VERBINDUNG"
            )
            
            OutlinedTextField(
                value = editingIp,
                onValueChange = { newValue ->
                    editingIp = newValue
                    ipError = !CameraSettings.isValidIp(newValue)
                },
                label = { Text("Kamera IP-Adresse") },
                isError = ipError,
                supportingText = {
                    if (ipError) {
                        Text(text = "Ungültige IP-Adresse", color = NightColors.error)
                    }
                },
                trailingIcon = {
                    if (editingIp != cameraIp && !ipError) {
                        IconButton(onClick = { onCameraIpChange(editingIp) }) {
                            Icon(Icons.Filled.Check, contentDescription = "Speichern", tint = NightColors.success)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NightColors.primary,
                    unfocusedBorderColor = NightColors.onBackground,
                    focusedLabelColor = NightColors.primary,
                    unfocusedLabelColor = NightColors.onBackground,
                    cursorColor = NightColors.primary,
                    focusedTextColor = NightColors.onSurface,
                    unfocusedTextColor = NightColors.onSurface
                )
            )
            if (editingIp != CameraSettings.getDefaultIp()) {
                TextButton(
                    onClick = {
                        editingIp = CameraSettings.getDefaultIp()
                        ipError = false
                        onCameraIpChange(CameraSettings.getDefaultIp())
                    }
                ) {
                    Text(text = "Zurücksetzen auf Standard-IP", color = NightColors.primary, fontSize = 12.sp)
                }
            }

            // WiFi Settings
            SettingsToggleRow(
                icon = Icons.Filled.Wifi,
                label = "WiFi Auto-Connect",
                checked = autoConnectEnabled,
                onCheckedChange = { 
                    autoConnectEnabled = it
                    onAutoConnectChange(it)
                }
            )

            if (autoConnectEnabled) {
                OutlinedTextField(
                    value = editingSsid,
                    onValueChange = { 
                        editingSsid = it
                        onWifiSsidChange(it)
                    },
                    label = { Text("WiFi SSID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NightColors.primary,
                        unfocusedBorderColor = NightColors.onBackground,
                        focusedTextColor = NightColors.onSurface,
                        unfocusedTextColor = NightColors.onSurface
                    )
                )

                OutlinedTextField(
                    value = editingPassword,
                    onValueChange = { 
                        editingPassword = it
                        onWifiPasswordChange(it)
                    },
                    label = { Text("WiFi Passwort") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NightColors.primary,
                        unfocusedBorderColor = NightColors.onBackground,
                        focusedTextColor = NightColors.onSurface,
                        unfocusedTextColor = NightColors.onSurface
                    )
                )
            }

            OutlinedTextField(
                value = editingPort,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        editingPort = it
                        it.toIntOrNull()?.let { port -> onHttpPortChange(port) }
                    }
                },
                label = { Text("HTTP API Port (Standard: 80)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NightColors.primary,
                    unfocusedBorderColor = NightColors.onBackground,
                    focusedTextColor = NightColors.onSurface,
                    unfocusedTextColor = NightColors.onSurface
                )
            )

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // ═══════════════════════════════════════════════════════════
            // 📷 KAMERA
            // ═══════════════════════════════════════════════════════════
            SettingsSectionHeader(
                icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                title = "KAMERA"
            )

            SettingsToggleRow(
                icon = if (audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                label = "Audio",
                checked = audioEnabled,
                onCheckedChange = onAudioChange
            )

            SettingsToggleRow(
                icon = Icons.Filled.MyLocation,
                label = "Hotspot anzeigen",
                checked = hotspotEnabled,
                onCheckedChange = onHotspotChange
            )

            // Helligkeit
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Brightness6, contentDescription = null, tint = NightColors.onSurface)
                    Text(text = "Helligkeit", color = NightColors.onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { level ->
                        FilterChip(
                            selected = brightness == level,
                            onClick = { onBrightnessChange(level) },
                            label = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("$level")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Kontrast
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Contrast, contentDescription = null, tint = NightColors.onSurface)
                    Text(text = "Kontrast", color = NightColors.onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { level ->
                        FilterChip(
                            selected = contrast == level,
                            onClick = { onContrastChange(level) },
                            label = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("$level")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Thermal Settings Button
            Button(
                onClick = onShowThermalSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary)
            ) {
                Icon(Icons.Filled.Thermostat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thermische Einstellungen")
            }

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // ═══════════════════════════════════════════════════════════
            // 🤖 APP-FUNKTIONEN
            // ═══════════════════════════════════════════════════════════
            SettingsSectionHeader(
                icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                title = "APP-FUNKTIONEN"
            )

            SettingsToggleRow(
                icon = Icons.Filled.Visibility,
                label = "AI Objekterkennung",
                checked = objectDetectionEnabled,
                onCheckedChange = onObjectDetectionChange
            )

            SettingsToggleRow(
                icon = Icons.Filled.AutoFixHigh,
                label = "Bildverbesserung",
                checked = enhancementEnabled,
                onCheckedChange = onEnhancementChange
            )

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // ═══════════════════════════════════════════════════════════
            // ℹ️ SYSTEM
            // ═══════════════════════════════════════════════════════════
            SettingsSectionHeader(
                icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                title = "SYSTEM"
            )

            // System Buttons - Modern Outlined Style
            OutlinedButton(
                onClick = onShowLog,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NightColors.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NightColors.primary,
                    containerColor = Color.Transparent
                )
            ) {
                Icon(Icons.Filled.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("System Log anzeigen")
            }

            OutlinedButton(
                onClick = onShowAbout,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NightColors.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NightColors.primary,
                    containerColor = Color.Transparent
                )
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Über NoxVision")
            }

            OutlinedButton(
                onClick = onShowWhatsNew,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NightColors.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NightColors.primary,
                    containerColor = Color.Transparent
                )
            ) {
                Icon(Icons.Filled.NewReleases, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Was ist neu?")
            }

            HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

            // ═══════════════════════════════════════════════════════════
            // 🤝 COMMUNITY & SUPPORT
            // ═══════════════════════════════════════════════════════════
            SettingsSectionHeader(
                icon = { Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                title = "COMMUNITY"
            )

            Button(
                onClick = onShowFeatureBounties,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NightColors.primaryDim)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFD700)) // Gold star
                Spacer(modifier = Modifier.width(8.dp))
                Text("Feature Bounties / Support", color = NightColors.onSurface)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
