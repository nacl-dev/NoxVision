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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.R
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
                            text = stringResource(R.string.version, "1.0"),
                            color = NightColors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.about_description),
                            color = NightColors.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }

                HorizontalDivider(color = NightColors.surface)

                // Description
                Text(
                    text = stringResource(R.string.about_long_description),
                    color = NightColors.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = NightColors.surface)

                // Features
                Text(
                    text = stringResource(R.string.features),
                    color = NightColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutFeatureItem(Icons.Filled.Videocam, stringResource(R.string.feature_stream))
                    AboutFeatureItem(Icons.Filled.Visibility, stringResource(R.string.feature_ai))
                    AboutFeatureItem(Icons.Filled.Palette, stringResource(R.string.feature_palettes))
                    AboutFeatureItem(Icons.Filled.Camera, stringResource(R.string.feature_capture))
                    AboutFeatureItem(Icons.Filled.PhotoLibrary, stringResource(R.string.feature_gallery))
                    AboutFeatureItem(Icons.Filled.Wifi, stringResource(R.string.feature_wifi))
                    AboutFeatureItem(Icons.Filled.DarkMode, stringResource(R.string.feature_night))
                }

                HorizontalDivider(color = NightColors.surface)

                // Supported Devices
                Text(
                    text = stringResource(R.string.supported_devices),
                    color = NightColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = stringResource(R.string.supported_devices_list),
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
                    text = stringResource(R.string.technologies),
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
                Text(stringResource(R.string.close), color = NightColors.primary)
            }
        },
        containerColor = NightColors.background,
        textContentColor = NightColors.onSurface
    )
}
