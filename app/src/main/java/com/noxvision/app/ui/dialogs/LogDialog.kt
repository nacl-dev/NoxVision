package com.noxvision.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.ui.NightColors
import com.noxvision.app.util.AppLogger

@Composable
fun LogDialogContent(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Log",
                    color = NightColors.onSurface
                )
                TextButton(
                    onClick = { AppLogger.clear() }
                ) {
                    Text("Clear", color = NightColors.primary, fontSize = 12.sp)
                }
            }
        },
        text = {
            val listState = rememberLazyListState()
            if (AppLogger.logsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No log entries",
                        color = NightColors.onBackground,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    items(AppLogger.logsList) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = log.timestamp,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NightColors.onBackground,
                                modifier = Modifier.width(60.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log.message,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = when (log.type) {
                                    AppLogger.LogType.SUCCESS -> NightColors.success
                                    AppLogger.LogType.ERROR -> NightColors.error
                                    else -> NightColors.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NightColors.primary)
            }
        },
        containerColor = NightColors.surface,
        textContentColor = NightColors.onSurface
    )
}
