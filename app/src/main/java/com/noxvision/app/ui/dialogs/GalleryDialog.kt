package com.noxvision.app.ui.dialogs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.data.CameraFile
import com.noxvision.app.data.GallerySource
import com.noxvision.app.data.PhoneFolder
import com.noxvision.app.data.PhoneMediaFile
import com.noxvision.app.ui.NightColors
import com.noxvision.app.ui.components.FileCard
import com.noxvision.app.ui.components.PhoneFileCard
import com.noxvision.app.util.AppLogger
import com.noxvision.app.util.fetchCameraFiles
import com.noxvision.app.util.fetchPhoneMedia

@Composable
fun GalleryDialog(
    baseUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var source by remember { mutableStateOf(GallerySource.PHONE) }
    var phoneFolder by remember { mutableStateOf(PhoneFolder.NOXVISION) }
    var cameraFiles by remember { mutableStateOf<List<CameraFile>>(emptyList()) }
    var phoneFiles by remember { mutableStateOf<List<PhoneMediaFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCameraFile by remember { mutableStateOf<CameraFile?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(source, phoneFolder, refreshTrigger) {
        isLoading = true
        errorMessage = null
        try {
            if (source == GallerySource.CAMERA_DEVICE) {
                cameraFiles = fetchCameraFiles(baseUrl)
                AppLogger.log("${cameraFiles.size} files found", AppLogger.LogType.SUCCESS)
            } else {
                phoneFiles = fetchPhoneMedia(context, phoneFolder)
                AppLogger.log("${phoneFiles.size} phone files", AppLogger.LogType.SUCCESS)
            }
        } catch (e: Exception) {
            errorMessage = e.message
            AppLogger.log("Gallery error: ${e.message}", AppLogger.LogType.ERROR)
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gallery",
                    color = NightColors.onSurface
                )
                IconButton(onClick = { refreshTrigger++ }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = NightColors.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = source == GallerySource.PHONE,
                        onClick = { source = GallerySource.PHONE },
                        label = { Text("Handy") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = source == GallerySource.CAMERA_DEVICE,
                        onClick = { source = GallerySource.CAMERA_DEVICE },
                        label = { Text("Thermal") },
                        modifier = Modifier.weight(1f)
                    )
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = NightColors.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Lade...",
                                    color = NightColors.onBackground,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        errorMessage != null -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = NightColors.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Error loading",
                                    color = NightColors.error,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = NightColors.onBackground,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        source == GallerySource.CAMERA_DEVICE -> {
                            if (cameraFiles.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = NightColors.onBackground,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No files",
                                        color = NightColors.onBackground,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(cameraFiles) { file ->
                                        FileCard(
                                            file = file,
                                            baseUrl = baseUrl,
                                            onClick = { selectedCameraFile = file }
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            if (phoneFiles.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = NightColors.onBackground,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No files",
                                        color = NightColors.onBackground,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(phoneFiles) { f ->
                                        PhoneFileCard(file = f) {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(f.uri, f.mime)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Kein Viewer gefunden",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            selectedCameraFile?.let { file ->
                PreviewDialog(
                    file = file,
                    baseUrl = baseUrl,
                    onDismiss = { selectedCameraFile = null }
                )
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
