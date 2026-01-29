package com.noxvision.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.CameraSettings
import com.noxvision.app.R
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.dialogs.AboutDialogContent
import com.noxvision.app.ui.dialogs.LanguageDialog
import com.noxvision.app.ui.dialogs.LogDialogContent
import com.noxvision.app.ui.dialogs.WhatsNewDialog
import com.noxvision.app.ui.hunting.HuntingHubScreen
import com.noxvision.app.util.LocaleHelper

/**
 * Settings navigation state
 */
private enum class SettingsPage {
    MAIN,
    CONNECTION,
    CAMERA,
    APP_FEATURES
}

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
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    // Internal dialog states - shown on top of settings
    var showLogDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var showHuntingHub by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentPage) {
                            SettingsPage.MAIN -> stringResource(R.string.settings)
                            SettingsPage.CONNECTION -> stringResource(R.string.connection)
                            SettingsPage.CAMERA -> stringResource(R.string.camera)
                            SettingsPage.APP_FEATURES -> stringResource(R.string.app_features)
                        },
                        color = NightColors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPage == SettingsPage.MAIN) {
                            onClose()
                        } else {
                            currentPage = SettingsPage.MAIN
                        }
                    }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = NightColors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightColors.background
                )
            )
        },
        containerColor = NightColors.background
    ) { paddingValues ->
        when (currentPage) {
            SettingsPage.MAIN -> MainSettingsPage(
                paddingValues = paddingValues,
                onNavigateToConnection = { currentPage = SettingsPage.CONNECTION },
                onNavigateToCamera = { currentPage = SettingsPage.CAMERA },
                onNavigateToAppFeatures = { currentPage = SettingsPage.APP_FEATURES },
                onShowThermalSettings = onShowThermalSettings,
                onShowLog = { showLogDialog = true },
                onShowAbout = { showAboutDialog = true },
                onShowWhatsNew = { showWhatsNewDialog = true },
                onShowFeatureBounties = onShowFeatureBounties,
                onShowLanguage = { showLanguageDialog = true }
            )
            SettingsPage.CONNECTION -> ConnectionSettingsPage(
                paddingValues = paddingValues,
                cameraIp = cameraIp,
                onCameraIpChange = onCameraIpChange,
                onWifiSsidChange = onWifiSsidChange,
                onWifiPasswordChange = onWifiPasswordChange,
                onHttpPortChange = onHttpPortChange,
                onAutoConnectChange = onAutoConnectChange
            )
            SettingsPage.CAMERA -> CameraSettingsPage(
                paddingValues = paddingValues,
                audioEnabled = audioEnabled,
                hotspotEnabled = hotspotEnabled,
                brightness = brightness,
                contrast = contrast,
                onAudioChange = onAudioChange,
                onHotspotChange = onHotspotChange,
                onBrightnessChange = onBrightnessChange,
                onContrastChange = onContrastChange,
                onShowThermalSettings = onShowThermalSettings
            )
            SettingsPage.APP_FEATURES -> AppFeaturesPage(
                paddingValues = paddingValues,
                objectDetectionEnabled = objectDetectionEnabled,
                enhancementEnabled = enhancementEnabled,
                onObjectDetectionChange = onObjectDetectionChange,
                onEnhancementChange = onEnhancementChange,
                onShowHuntingHub = { showHuntingHub = true }
            )
        }
    }

    // Dialogs shown on top of settings (not replacing them)
    if (showLogDialog) {
        LogDialogContent(onDismiss = { showLogDialog = false })
    }

    if (showAboutDialog) {
        AboutDialogContent(onDismiss = { showAboutDialog = false })
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(onDismiss = { showWhatsNewDialog = false })
    }

    if (showHuntingHub) {
        HuntingHubScreen(onClose = { showHuntingHub = false })
    }

    if (showLanguageDialog) {
        LanguageDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageCode ->
                LocaleHelper.setLocale(languageCode)
            }
        )
    }
}

@Composable
private fun MainSettingsPage(
    paddingValues: PaddingValues,
    onNavigateToConnection: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAppFeatures: () -> Unit,
    onShowThermalSettings: () -> Unit,
    onShowLog: () -> Unit,
    onShowAbout: () -> Unit,
    onShowWhatsNew: () -> Unit,
    onShowFeatureBounties: () -> Unit,
    onShowLanguage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ═══════════════════════════════════════════════════════════
        // HAUPTKATEGORIEN
        // ═══════════════════════════════════════════════════════════

        SettingsCategoryCard(
            icon = Icons.Filled.Wifi,
            title = stringResource(R.string.connection),
            subtitle = stringResource(R.string.connection_subtitle),
            onClick = onNavigateToConnection
        )

        SettingsCategoryCard(
            icon = Icons.Filled.CameraAlt,
            title = stringResource(R.string.camera),
            subtitle = stringResource(R.string.camera_subtitle),
            onClick = onNavigateToCamera
        )

        SettingsCategoryCard(
            icon = Icons.Filled.Thermostat,
            title = stringResource(R.string.thermal_settings),
            subtitle = stringResource(R.string.thermal_settings_subtitle),
            onClick = onShowThermalSettings
        )

        SettingsCategoryCard(
            icon = Icons.Filled.AutoAwesome,
            title = stringResource(R.string.app_features),
            subtitle = stringResource(R.string.app_features_subtitle),
            onClick = onNavigateToAppFeatures
        )

        SettingsCategoryCard(
            icon = Icons.Filled.Language,
            title = stringResource(R.string.language),
            subtitle = LocaleHelper.getSelectedLanguageDisplayName(),
            onClick = onShowLanguage
        )

        HorizontalDivider(
            color = NightColors.surface,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // ═══════════════════════════════════════════════════════════
        // SYSTEM & INFO
        // ═══════════════════════════════════════════════════════════

        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.system_info)
        )

        SettingsActionItem(
            icon = Icons.Filled.Description,
            title = stringResource(R.string.system_log),
            onClick = onShowLog
        )

        SettingsActionItem(
            icon = Icons.Filled.NewReleases,
            title = stringResource(R.string.whats_new),
            onClick = onShowWhatsNew
        )

        SettingsActionItem(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.about_noxvision),
            onClick = onShowAbout
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Community Bounties - temporarily hidden during test phase
        // HorizontalDivider(...)
        // SettingsSectionHeader(...) COMMUNITY
        // Button(...) Feature Bounties / Support
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NightColors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NightColors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NightColors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = NightColors.onBackground,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = NightColors.onBackground
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NightColors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = NightColors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = NightColors.onBackground
        )
    }
}

// ═══════════════════════════════════════════════════════════
// CONNECTION SETTINGS PAGE
// ═══════════════════════════════════════════════════════════

@Composable
private fun ConnectionSettingsPage(
    paddingValues: PaddingValues,
    cameraIp: String,
    onCameraIpChange: (String) -> Unit,
    onWifiSsidChange: (String) -> Unit,
    onWifiPasswordChange: (String) -> Unit,
    onHttpPortChange: (Int) -> Unit,
    onAutoConnectChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var editingIp by remember { mutableStateOf(cameraIp) }
    var ipError by remember { mutableStateOf(false) }
    var editingSsid by remember { mutableStateOf(CameraSettings.getWifiSsid(context)) }
    var editingPassword by remember { mutableStateOf(CameraSettings.getWifiPassword(context)) }
    var editingPort by remember { mutableStateOf(CameraSettings.getHttpPort(context).toString()) }
    var autoConnectEnabled by remember { mutableStateOf(CameraSettings.isAutoConnectEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // IP-Adresse
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Router,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.camera_address)
        )

        OutlinedTextField(
            value = editingIp,
            onValueChange = { newValue ->
                editingIp = newValue
                ipError = !CameraSettings.isValidIp(newValue)
            },
            label = { Text(stringResource(R.string.ip_address)) },
            isError = ipError,
            supportingText = {
                if (ipError) {
                    Text(text = stringResource(R.string.invalid_ip), color = NightColors.error)
                }
            },
            trailingIcon = {
                if (editingIp != cameraIp && !ipError) {
                    IconButton(onClick = { onCameraIpChange(editingIp) }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save), tint = NightColors.success)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = nightTextFieldColors()
        )

        if (editingIp != CameraSettings.getDefaultIp()) {
            TextButton(
                onClick = {
                    editingIp = CameraSettings.getDefaultIp()
                    ipError = false
                    onCameraIpChange(CameraSettings.getDefaultIp())
                }
            ) {
                Text(text = stringResource(R.string.reset_to_default, CameraSettings.getDefaultIp()), color = NightColors.primary, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = editingPort,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    editingPort = it
                    it.toIntOrNull()?.let { port -> onHttpPortChange(port) }
                }
            },
            label = { Text(stringResource(R.string.http_api_port)) },
            supportingText = { Text(stringResource(R.string.default_port)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = nightTextFieldColors()
        )

        HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

        // WiFi Auto-Connect
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.wifi_auto_connect)
        )

        SettingsToggleRow(
            icon = Icons.Filled.Wifi,
            label = stringResource(R.string.auto_connect),
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
                label = { Text(stringResource(R.string.wifi_ssid)) },
                supportingText = { Text(stringResource(R.string.wifi_ssid_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = nightTextFieldColors()
            )

            OutlinedTextField(
                value = editingPassword,
                onValueChange = {
                    editingPassword = it
                    onWifiPasswordChange(it)
                },
                label = { Text(stringResource(R.string.wifi_password)) },
                supportingText = { Text(stringResource(R.string.wifi_password_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = nightTextFieldColors()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════════════════════
// CAMERA SETTINGS PAGE
// ═══════════════════════════════════════════════════════════

@Composable
private fun CameraSettingsPage(
    paddingValues: PaddingValues,
    audioEnabled: Boolean,
    hotspotEnabled: Boolean,
    brightness: Int,
    contrast: Int,
    onAudioChange: (Boolean) -> Unit,
    onHotspotChange: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onContrastChange: (Int) -> Unit,
    onShowThermalSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Audio & Anzeige
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.audio_display)
        )

        SettingsToggleRow(
            icon = if (audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            label = stringResource(R.string.enable_audio),
            checked = audioEnabled,
            onCheckedChange = onAudioChange
        )

        SettingsToggleRow(
            icon = Icons.Filled.MyLocation,
            label = stringResource(R.string.show_hotspot),
            checked = hotspotEnabled,
            onCheckedChange = onHotspotChange
        )

        HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

        // Bildeinstellungen
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Brightness6,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.image_settings)
        )

        // Helligkeit
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Brightness6, contentDescription = null, tint = NightColors.onSurface)
                Text(text = stringResource(R.string.brightness), color = NightColors.onSurface)
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
                Text(text = stringResource(R.string.contrast), color = NightColors.onSurface)
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

        HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

        // Thermal Settings Link
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Thermostat,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.advanced)
        )

        Button(
            onClick = onShowThermalSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Thermostat, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.thermal_settings))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════════════════════
// APP FEATURES PAGE
// ═══════════════════════════════════════════════════════════

@Composable
private fun AppFeaturesPage(
    paddingValues: PaddingValues,
    objectDetectionEnabled: Boolean,
    enhancementEnabled: Boolean,
    onObjectDetectionChange: (Boolean) -> Unit,
    onEnhancementChange: (Boolean) -> Unit,
    onShowHuntingHub: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hunting Assistant
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Forest,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.hunting)
        )

        SettingsCategoryCard(
            icon = Icons.Filled.Forest,
            title = stringResource(R.string.hunting_assistant),
            subtitle = stringResource(R.string.hunting_assistant_subtitle),
            onClick = onShowHuntingHub
        )

        HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

        // AI Features
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.ai_features)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = NightColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    icon = Icons.Filled.Visibility,
                    label = stringResource(R.string.ai_object_detection),
                    checked = objectDetectionEnabled,
                    onCheckedChange = onObjectDetectionChange
                )

                if (objectDetectionEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ai_detection_desc),
                        color = NightColors.onBackground,
                        fontSize = 12.sp
                    )
                }
            }
        }

        HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 8.dp))

        // Bildverbesserung
        SettingsSectionHeader(
            icon = {
                Icon(
                    Icons.Filled.AutoFixHigh,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            title = stringResource(R.string.image_enhancement)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = NightColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    icon = Icons.Filled.AutoFixHigh,
                    label = stringResource(R.string.enable_image_enhancement),
                    checked = enhancementEnabled,
                    onCheckedChange = onEnhancementChange
                )

                if (enhancementEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.image_enhancement_desc),
                        color = NightColors.onBackground,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════

@Composable
private fun nightTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NightColors.primary,
    unfocusedBorderColor = NightColors.onBackground,
    focusedLabelColor = NightColors.primary,
    unfocusedLabelColor = NightColors.onBackground,
    cursorColor = NightColors.primary,
    focusedTextColor = NightColors.onSurface,
    unfocusedTextColor = NightColors.onSurface,
    focusedSupportingTextColor = NightColors.onBackground,
    unfocusedSupportingTextColor = NightColors.onBackground
)
