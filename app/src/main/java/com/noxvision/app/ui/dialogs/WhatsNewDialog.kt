package com.noxvision.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noxvision.app.WhatsNewRepository
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.components.WhatsNewItem

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
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
                        imageVector = Icons.Filled.NewReleases,
                        contentDescription = null,
                        tint = NightColors.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Das ist neu!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NightColors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WhatsNewRepository.features.forEach { feature ->
                        WhatsNewItem(feature.title, feature.description)
                    }
                }

                // Finish button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verstanden")
                }
            }
        }
    }
}
