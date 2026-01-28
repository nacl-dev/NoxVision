package com.noxvision.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.components.AboutFeatureItem
import com.noxvision.app.ui.components.TechBadge

@Composable
fun AboutDialogContent(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = NightColors.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "NoxVision",
                    color = NightColors.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Version Info
                Surface(
                    color = NightColors.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Version 1.0",
                            color = NightColors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Open Source Android App fur Guide Warmebildkameras",
                            color = NightColors.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }

                HorizontalDivider(color = NightColors.surface)

                // Description
                Text(
                    text = "NoxVision ist eine leistungsstarke Alternative zur offiziellen Guide App, speziell entwickelt fur das Guide TE211M Warmebild-Monokular und kompatible TE-Serie Kameras.",
                    color = NightColors.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = NightColors.surface)

                // Features
                Text(
                    text = "Features",
                    color = NightColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutFeatureItem(Icons.Filled.Videocam, "Live RTSP Video Stream")
                    AboutFeatureItem(Icons.Filled.Visibility, "YOLO KI-Objekterkennung")
                    AboutFeatureItem(Icons.Filled.Palette, "Mehrere Farbpaletten")
                    AboutFeatureItem(Icons.Filled.Camera, "Screenshot & Video-Aufnahme")
                    AboutFeatureItem(Icons.Filled.PhotoLibrary, "Integrierte Galerie")
                    AboutFeatureItem(Icons.Filled.Wifi, "Auto-WiFi Verbindung")
                    AboutFeatureItem(Icons.Filled.DarkMode, "Nacht-optimiertes Design")
                }

                HorizontalDivider(color = NightColors.surface)

                // Supported Devices
                Text(
                    text = "Unterstutzte Gerate",
                    color = NightColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "• Guide TE211M (primar getestet)\n• Andere Guide TE-Serie Kameras\n• Kameras mit RTSP auf 192.168.42.1:8554",
                    color = NightColors.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = NightColors.surface)

                // License
                Surface(
                    color = NightColors.primaryDim.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = null,
                            tint = NightColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "MIT License",
                                color = NightColors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "© 2026 NoxVision Contributors",
                                color = NightColors.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Tech Stack
                Text(
                    text = "Technologien",
                    color = NightColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TechBadge("Kotlin 2.0")
                    TechBadge("Jetpack Compose")
                    TechBadge("TensorFlow Lite")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TechBadge("LibVLC")
                    TechBadge("Material 3")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schliessen", color = NightColors.primary)
            }
        },
        containerColor = NightColors.background,
        textContentColor = NightColors.onSurface
    )
}
