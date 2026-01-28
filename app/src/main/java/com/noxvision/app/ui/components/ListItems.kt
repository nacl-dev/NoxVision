package com.noxvision.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.ui.NightColors

@Composable
fun AboutFeatureItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = NightColors.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            color = NightColors.onSurface,
            fontSize = 13.sp
        )
    }
}

@Composable
fun TechBadge(text: String) {
    Surface(
        color = NightColors.primaryDim.copy(alpha = 0.3f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, NightColors.primary.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = NightColors.primary
        )
    }
}

@Composable
fun WelcomeFeatureItem(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NightColors.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = NightColors.onSurface)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = NightColors.onBackground)
        }
    }
}

@Composable
fun WhatsNewItem(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "• $title",
            fontWeight = FontWeight.Bold,
            color = NightColors.onSurface,
            fontSize = 16.sp
        )
        Text(
            text = desc,
            color = NightColors.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
        )
    }
}
