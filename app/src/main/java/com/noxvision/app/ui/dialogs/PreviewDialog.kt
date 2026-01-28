package com.noxvision.app.ui.dialogs

import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.noxvision.app.data.CameraFile
import com.noxvision.app.ui.NightColors
import com.noxvision.app.util.AppLogger
import com.noxvision.app.util.deleteCacheVideo
import com.noxvision.app.util.downloadFile
import com.noxvision.app.util.downloadVideoToCache
import com.noxvision.app.util.formatFileSize
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

@Composable
fun PreviewDialog(
    file: CameraFile,
    baseUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoadingVideo by remember { mutableStateOf(false) }
    var cachedVideoFile by remember { mutableStateOf<File?>(null) }

    val libVLC = remember {
        LibVLC(
            context,
            arrayListOf(
                "--network-caching=1000",
                "--file-caching=1000",
                "--live-caching=1000"
            )
        )
    }

    val previewPlayer = remember {
        MediaPlayer(libVLC).apply {
            videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.stop()
            previewPlayer.release()
            libVLC.release()
            deleteCacheVideo(file.name, context)
        }
    }

    Dialog(
        onDismissRequest = {
            previewPlayer.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(
                containerColor = NightColors.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NightColors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatFileSize(file.size)} • ${file.type}",
                            fontSize = 12.sp,
                            color = NightColors.onBackground
                        )
                    }

                    IconButton(onClick = {
                        previewPlayer.stop()
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = NightColors.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    if (isLoadingVideo) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NightColors.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Video wird geladen...",
                                color = NightColors.onBackground,
                                fontSize = 13.sp
                            )
                        }
                    } else if (isPlaying && cachedVideoFile != null) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    holder.addCallback(object :
                                        android.view.SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                            previewPlayer.vlcVout.setVideoView(this@apply)
                                            previewPlayer.vlcVout.attachViews()

                                            scope.launch {
                                                try {
                                                    val media = Media(libVLC, cachedVideoFile!!.toUri())
                                                    previewPlayer.media = media
                                                    media.release()
                                                    previewPlayer.play()
                                                } catch (e: Exception) {
                                                    AppLogger.log(
                                                        "Preview error: ${e.message}",
                                                        AppLogger.LogType.ERROR
                                                    )
                                                }
                                            }
                                        }

                                        override fun surfaceChanged(
                                            holder: android.view.SurfaceHolder,
                                            format: Int,
                                            width: Int,
                                            height: Int
                                        ) {
                                            previewPlayer.vlcVout.setWindowSize(width, height)
                                        }

                                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                            previewPlayer.vlcVout.detachViews()
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        if (file.type == "image") {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("$baseUrl/api/v1/files/download/${file.name}")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = file.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoFile,
                                    contentDescription = null,
                                    tint = Color(0xFF1A1A1A),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Video bereit",
                                    color = NightColors.onBackground,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (file.type == "video") {
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    previewPlayer.stop()
                                    isPlaying = false
                                } else if (!isLoadingVideo) {
                                    scope.launch {
                                        isLoadingVideo = true
                                        try {
                                            val cached = downloadVideoToCache(baseUrl, file.name, context)
                                            if (cached != null && cached.exists()) {
                                                cachedVideoFile = cached
                                                isPlaying = true
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "Video konnte nicht geladen werden",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            AppLogger.log(
                                                "Video Cache error: ${e.message}",
                                                AppLogger.LogType.ERROR
                                            )
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Fehler beim Laden: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } finally {
                                            isLoadingVideo = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingVideo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NightColors.primaryDim
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "Stop" else if (isLoadingVideo) "Lädt..." else "Abspielen")
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isDownloading = true
                                try {
                                    downloadFile(baseUrl, file.name, context)
                                    AppLogger.log(
                                        "Download: ${file.name}",
                                        AppLogger.LogType.SUCCESS
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Saved to gallery",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    AppLogger.log(
                                        "Download error: ${e.message}",
                                        AppLogger.LogType.ERROR
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Download fehlgeschlagen",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                isDownloading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NightColors.primary
                        )
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Herunterladen")
                        }
                    }
                }
            }
        }
    }
}
