package com.noxvision.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.components.WelcomeFeatureItem

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = NightColors.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (step) {
                            0 -> Icons.Filled.WbSunny
                            1 -> Icons.Filled.Wifi
                            2 -> Icons.Filled.Settings
                            else -> Icons.Filled.CheckCircle
                        },
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (step) {
                            0 -> "Willkommen bei NoxVision"
                            1 -> "Verbindung einrichten"
                            2 -> "Wichtige Funktionen"
                            else -> "Bereit!"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = NightColors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (step) {
                        0 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Ihr Begleiter fur Warmibildaufnahmen.\n\nDiese App bietet erweiterte Funktionen fur Ihre Guide Kamera.",
                                    color = NightColors.onBackground,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        1 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Damit die App funktioniert, mussen Sie die korrekte SSID und das Passwort Ihrer Kamera in den Einstellungen hinterlegen.",
                                    color = NightColors.onBackground,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NightColors.background)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Standard-Werte:",
                                            color = NightColors.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "SSID: Siehe Kamera-Aufkleber",
                                            color = NightColors.onBackground,
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                        )
                                        Text(
                                            "Passwort: Oft 12345678",
                                            color = NightColors.onBackground,
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                WelcomeFeatureItem(
                                    Icons.Filled.Camera,
                                    "Galerie",
                                    "Bilder & Videos direkt in der App verwalten"
                                )
                                WelcomeFeatureItem(
                                    Icons.Filled.Thermostat,
                                    "Messungen",
                                    "Temperaturpunkte und Emissionsgrad anpassen"
                                )
                                WelcomeFeatureItem(
                                    Icons.Filled.Wifi,
                                    "Auto-Connect",
                                    "Automatische Verbindung beim Start"
                                )
                                WelcomeFeatureItem(
                                    Icons.Filled.Videocam,
                                    "Aufnahme",
                                    "Videos mit Ton aufzeichnen"
                                )
                            }
                        }

                        3 -> {
                            Text(
                                text = "Sie konnen diese Einstellungen spater jederzeit uber das Zahnrad-Symbol andern.\n\nViel Spass mit NoxVision!",
                                color = NightColors.onBackground,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Footer / Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button / Indicator
                    if (step > 0) {
                        TextButton(onClick = { step-- }) {
                            Text("Zuruck", color = NightColors.onSurface)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    // Dots indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == step) NightColors.primary
                                        else NightColors.onSurface.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    // Next / Finish button
                    Button(
                        onClick = {
                            if (step < 3) step++ else onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary)
                    ) {
                        Text(if (step < 3) "Weiter" else "Starten")
                    }
                }
            }
        }
    }
}
