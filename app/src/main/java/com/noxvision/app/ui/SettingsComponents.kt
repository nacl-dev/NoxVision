package com.noxvision.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NightColors {
    val background = Color(0xFF000000)
    val surface = Color(0xFF0F0F0F)
    val primary = Color(0xFF43A047) // Subtle Hunting Green (Green 600)
    val primaryDim = Color(0xFF1B5E20) // Dark Forest Green (Green 900)
    val onBackground = Color(0xFFFFFFFF)
    val onSurface = Color(0xFFE0E0E0)
    val recording = Color(0xFFFF1744)
    val success = Color(0xFF43A047) // Match primary for success
    val error = Color(0xFFFF5252)
}

/**
 * Section header with icon and title
 */
@Composable
fun SettingsSectionHeader(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        icon()
        Text(
            text = title,
            color = NightColors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

/**
 * Settings toggle row with icon, label and switch
 */
@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = NightColors.onSurface)
            Text(text = label, color = NightColors.onSurface)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NightColors.primary,
                checkedTrackColor = NightColors.primary.copy(alpha = 0.5f)
            )
        )
    }
}
