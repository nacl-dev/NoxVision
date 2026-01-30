package com.noxvision.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.noxvision.app.CameraApiClient
import com.noxvision.app.CameraCapabilities
import com.noxvision.app.CameraSettings
import com.noxvision.app.DeviceInfo
import com.noxvision.app.MainActivity
import com.noxvision.app.getCapabilities
import com.noxvision.app.R
import com.noxvision.app.billing.BillingManager
import com.noxvision.app.billing.FeatureBountyRepository
import com.noxvision.app.billing.FeatureBountyScreen
import com.noxvision.app.detection.DetectedObject
import com.noxvision.app.detection.KNOWN_OBJECTS
import com.noxvision.app.detection.ThermalObjectDetector
import com.noxvision.app.ui.components.DarkButton
import com.noxvision.app.ui.components.PaletteButton
import com.noxvision.app.ui.dialogs.AboutDialogContent
import com.noxvision.app.ui.dialogs.GalleryDialog
import com.noxvision.app.ui.dialogs.LogDialogContent
import com.noxvision.app.ui.dialogs.WelcomeDialog
import com.noxvision.app.ui.dialogs.WhatsNewDialog
import com.noxvision.app.ui.hunting.HuntingHubScreen
import com.noxvision.app.util.AppLogger
import com.noxvision.app.util.captureScreenshot
import com.noxvision.app.util.createVideoFile
import com.noxvision.app.util.formatDuration
import com.noxvision.app.util.saveVideoToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

@Composable
fun VideoStreamScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var bufferPercent by remember { mutableFloatStateOf(0f) }
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    var recordingPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var recordingTempFile by remember { mutableStateOf<File?>(null) }
    var hotspotEnabled by remember { mutableStateOf(false) }

    var brightness by remember { mutableIntStateOf(3) }
    var contrast by remember { mutableIntStateOf(3) }
    var enhancementEnabled by remember { mutableStateOf(false) }

    var isConnecting by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(10f) }
    var audioEnabled by rememberSaveable { mutableStateOf(true) }
    var selectedPalette by rememberSaveable { mutableStateOf("whitehot") }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showGalleryDialog by rememberSaveable { mutableStateOf(false) }
    var galleryRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    // First Run / Whats New
    var showWelcomeDialog by rememberSaveable { mutableStateOf(false) }
    var showWhatsNewDialog by rememberSaveable { mutableStateOf(false) }

    // Feature Bounties
    var showFeatureBountiesDialog by rememberSaveable { mutableStateOf(false) }

    // Hunting Hub
    var showHuntingHub by rememberSaveable { mutableStateOf(false) }

    val featureRepository = remember { FeatureBountyRepository(context) }

    // Create BillingManager with callback to repo
    val billingManager = remember {
        BillingManager(context) { productId ->
            val amount = productId.replace("credits_", "").toIntOrNull() ?: 0
            if (amount > 0) featureRepository.addCredits(amount)
        }
    }

    LaunchedEffect(Unit) {
        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            1
        }

        if (CameraSettings.isFirstRun(context)) {
            showWelcomeDialog = true
        } else {
            val lastVersion = CameraSettings.getLastVersionCode(context)
            if (lastVersion != -1 && lastVersion < currentVersionCode) {
                showWhatsNewDialog = true
            } else {
                CameraSettings.setLastVersionCode(context, currentVersionCode)
            }
        }
    }

    var objectDetectionEnabled by remember { mutableStateOf(false) }
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    val detector = remember { ThermalObjectDetector(context) }

    // Camera IP settings
    var cameraIp by remember { mutableStateOf(CameraSettings.getCameraIp(context)) }
    val rtspUrl = remember(cameraIp) { CameraSettings.getRtspUrl(cameraIp) }
    val baseUrl = remember(cameraIp) { CameraSettings.getBaseUrl(context, cameraIp) }

    // Camera API Client
    val apiClient = remember(baseUrl) { CameraApiClient(baseUrl) }

    // Device info and capabilities
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(CameraSettings.getCachedDeviceInfo(context)) }
    var cameraCapabilities by remember { mutableStateOf<CameraCapabilities?>(deviceInfo?.getCapabilities()) }

    // Thermal measurement settings
    var emissivity by remember { mutableFloatStateOf(CameraSettings.getEmissivity(context)) }
    var measureDistance by remember { mutableFloatStateOf(CameraSettings.getDistance(context)) }
    var humidity by remember { mutableFloatStateOf(CameraSettings.getHumidity(context)) }
    var reflectTemperature by remember { mutableFloatStateOf(CameraSettings.getReflectTemperature(context)) }

    // Thermal settings dialog
    var showThermalSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var isShutterInProgress by remember { mutableStateOf(false) }

    // Lifecycle state
    var wasPlayingBeforeStop by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera control functions
    suspend fun setZoom(level: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                AppLogger.log("Setze Zoom auf $level", AppLogger.LogType.INFO)
                val url = URL("$baseUrl/api/v1/camera/seekzoom")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept-Encoding", "gzip")
                conn.setRequestProperty("User-Agent", "okhttp/4.11.0")
                conn.doOutput = true

                val jsonInputString = """{"value":"$level"}"""
                OutputStreamWriter(conn.outputStream).use {
                    it.write(jsonInputString)
                    it.flush()
                }

                val responseCode = conn.responseCode
                val success = responseCode == 200

                if (success) {
                    val zoomDisplay = String.format(Locale.getDefault(), "%.1f", level / 10.0)
                    AppLogger.log("Zoom: ${zoomDisplay}x", AppLogger.LogType.SUCCESS)
                } else {
                    AppLogger.log("Zoom error: HTTP $responseCode", AppLogger.LogType.ERROR)
                }

                conn.disconnect()
                success
            } catch (e: Exception) {
                AppLogger.log("Zoom Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    suspend fun setAudio(status: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                AppLogger.log("Setze Audio auf $status", AppLogger.LogType.INFO)
                val url = URL("$baseUrl/api/v1/peripheral/audio_status")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept-Encoding", "gzip")
                conn.setRequestProperty("User-Agent", "okhttp/4.11.0")
                conn.doOutput = true

                val jsonInputString = """{"value":"$status"}"""
                OutputStreamWriter(conn.outputStream).use {
                    it.write(jsonInputString)
                    it.flush()
                }

                val responseCode = conn.responseCode
                val success = responseCode == 200
                if (success) {
                    AppLogger.log(
                        "Audio: ${if (status == "on") "Ein" else "Aus"}",
                        AppLogger.LogType.SUCCESS
                    )
                }

                conn.disconnect()
                success
            } catch (e: Exception) {
                AppLogger.log("Audio Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    fun buildGenericFrame(cmd: ByteArray, value: Int): ByteArray {
        val frameHead = byteArrayOf(0x55.toByte(), 0xAA.toByte())

        val valueBytes = byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )

        val payloadLen: Byte = (cmd.size + valueBytes.size).toByte()
        val payload = ByteArray(1 + cmd.size + valueBytes.size)
        payload[0] = payloadLen
        System.arraycopy(cmd, 0, payload, 1, cmd.size)
        System.arraycopy(valueBytes, 0, payload, 1 + cmd.size, valueBytes.size)

        var checksum = payload[0]
        for (i in 1 until payload.size) {
            checksum = (checksum.toInt() xor payload[i].toInt()).toByte()
        }

        val frame = ByteArray(frameHead.size + payload.size + 2)
        frame[0] = frameHead[0]
        frame[1] = frameHead[1]
        System.arraycopy(payload, 0, frame, 2, payload.size)
        frame[2 + payload.size] = checksum
        frame[3 + payload.size] = 0xF0.toByte()

        return frame
    }

    fun buildPaletteFrame(palletId: Int): ByteArray {
        val cmd = byteArrayOf(0x02, 0x00, 0x04)
        return buildGenericFrame(cmd, palletId)
    }

    suspend fun setPalette(paletteName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val paletteIds = mapOf(
                    "whitehot" to 0,
                    "blackhot" to 9,
                    "redhot" to 11,
                    "iron" to 2,
                    "bluehot" to 4,
                    "greenhot" to 5,
                    "darkbrown" to 20
                )

                val palletId = paletteIds[paletteName] ?: run {
                    AppLogger.log("Unbekannte Palette: $paletteName", AppLogger.LogType.ERROR)
                    return@withContext false
                }

                AppLogger.log("Setze Palette: $paletteName (ID=$palletId)", AppLogger.LogType.INFO)

                val data = buildPaletteFrame(palletId)
                val hexStr = data.joinToString(" ") { "%02x".format(it) }
                AppLogger.log("Frame: $hexStr", AppLogger.LogType.INFO)

                val url = URL("${baseUrl}/api/v1/files/customdata")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/customdata")
                    doOutput = true
                }

                conn.outputStream.use { it.write(data) }
                val code = conn.responseCode
                conn.disconnect()

                val success = code == 200
                if (success) {
                    AppLogger.log("Palette: $paletteName", AppLogger.LogType.SUCCESS)
                } else {
                    AppLogger.log("Palette error: HTTP $code", AppLogger.LogType.ERROR)
                }

                success
            } catch (e: Exception) {
                AppLogger.log("Palette Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    suspend fun setHotspot(enabled: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modeId = if (enabled) 1 else 0
                AppLogger.log("Setze Hotspot: ${if (enabled) "ON" else "OFF"}", AppLogger.LogType.INFO)

                val cmd = byteArrayOf(0x02, 0x00, 0x09)
                val data = buildGenericFrame(cmd, modeId)

                val url = URL("${baseUrl}/api/v1/files/customdata")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/customdata")
                    doOutput = true
                }

                conn.outputStream.use { it.write(data) }
                val code = conn.responseCode
                conn.disconnect()

                val success = code == 200
                if (success) {
                    AppLogger.log("Hotspot: ${if (enabled) "EIN" else "AUS"}", AppLogger.LogType.SUCCESS)
                } else {
                    AppLogger.log("Hotspot error: HTTP $code", AppLogger.LogType.ERROR)
                }

                success
            } catch (e: Exception) {
                AppLogger.log("Hotspot Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    suspend fun setBrightness(level: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val clampedLevel = level.coerceIn(1, 5)
                AppLogger.log("Setze Helligkeit: $clampedLevel", AppLogger.LogType.INFO)

                val cmd = byteArrayOf(0x02, 0x03, 0x09)
                val data = buildGenericFrame(cmd, clampedLevel)

                val url = URL("${baseUrl}/api/v1/files/customdata")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/customdata")
                    doOutput = true
                }

                conn.outputStream.use { it.write(data) }
                val code = conn.responseCode
                conn.disconnect()

                val success = code == 200
                if (success) {
                    AppLogger.log("Helligkeit: $clampedLevel", AppLogger.LogType.SUCCESS)
                } else {
                    AppLogger.log("Brightness error: HTTP $code", AppLogger.LogType.ERROR)
                }
                success
            } catch (e: Exception) {
                AppLogger.log("Helligkeit Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    suspend fun setContrast(level: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val clampedLevel = level.coerceIn(1, 5)
                AppLogger.log("Setze Kontrast: $clampedLevel", AppLogger.LogType.INFO)

                val cmd = byteArrayOf(0x02, 0x03, 0x0A)
                val data = buildGenericFrame(cmd, clampedLevel)

                val url = URL("${baseUrl}/api/v1/files/customdata")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/customdata")
                    doOutput = true
                }

                conn.outputStream.use { it.write(data) }
                val code = conn.responseCode
                conn.disconnect()

                val success = code == 200
                if (success) {
                    AppLogger.log("Kontrast: $clampedLevel", AppLogger.LogType.SUCCESS)
                } else {
                    AppLogger.log("Contrast error: HTTP $code", AppLogger.LogType.ERROR)
                }
                success
            } catch (e: Exception) {
                AppLogger.log("Kontrast Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    suspend fun setEnhancement(enabled: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val value = if (enabled) 1 else 0
                AppLogger.log("Setze Bildverbesserung: ${if (enabled) "EIN" else "AUS"}", AppLogger.LogType.INFO)

                val cmd = byteArrayOf(0x02, 0x03, 0x0B)
                val data = buildGenericFrame(cmd, value)

                val url = URL("${baseUrl}/api/v1/files/customdata")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/customdata")
                    doOutput = true
                }

                conn.outputStream.use { it.write(data) }
                val code = conn.responseCode
                conn.disconnect()

                val success = code == 200
                if (success) {
                    AppLogger.log("Bildverbesserung: ${if (enabled) "EIN" else "AUS"}", AppLogger.LogType.SUCCESS)
                } else {
                    AppLogger.log("Image enhancement error: HTTP $code", AppLogger.LogType.ERROR)
                }
                success
            } catch (e: Exception) {
                AppLogger.log("Bildverbesserung Exception: ${e.message}", AppLogger.LogType.ERROR)
                false
            }
        }
    }

    val libVLC = remember {
        LibVLC(
            context,
            arrayListOf(
                "--rtsp-tcp",
                "--network-caching=300",
                "--rtsp-frame-buffer-size=500000",
                "--live-caching=300",
                "--file-caching=300"
            )
        )
    }

    suspend fun startRecordingViaVlc() {
        if (recordingPlayer != null) return

        val file = createVideoFile(context)
        recordingTempFile = file

        val recPlayer = MediaPlayer(libVLC)

        val sout = ":sout=#std{access=file,mux=mp4,dst='${file.absolutePath}'}"

        val media = Media(libVLC, rtspUrl.toUri()).apply {
            addOption(":rtsp-tcp")
            addOption(":network-caching=300")
            addOption(sout)
            addOption(":sout-keep")
        }

        recPlayer.media = media
        media.release()

        recPlayer.play()

        recordingPlayer = recPlayer
        isRecording = true
    }

    suspend fun stopRecordingViaVlc() {
        val rec = recordingPlayer
        val file = recordingTempFile

        recordingPlayer = null
        recordingTempFile = null
        isRecording = false

        try {
            rec?.stop()
            rec?.release()
        } catch (_: Exception) {
        }

        delay(800)

        file?.let { videoFile ->
            if (videoFile.exists() && videoFile.length() > 1024) {
                saveVideoToGallery(context, videoFile)
                galleryRefreshKey++
                Toast.makeText(
                    context,
                    "Video saved (${videoFile.length() / 1024}KB)",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (videoFile.exists()) videoFile.delete()
                Toast.makeText(context, "Video zu klein/leer", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        } else {
            recordingDuration = 0
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Berechtigungen erforderlich", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        AppLogger.log("App gestartet", AppLogger.LogType.INFO)

        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.CHANGE_NETWORK_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val needPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needPermissions.isNotEmpty()) {
            permissionLauncher.launch(needPermissions.toTypedArray())
        }
    }

    // AI Object Detection
    LaunchedEffect(objectDetectionEnabled, isPlaying) {
        if (objectDetectionEnabled && isPlaying) {
            while (isActive && objectDetectionEnabled && isPlaying) {
                delay(1500)

                surfaceView?.let { view ->
                    try {
                        val bitmap = createBitmap(view.width, view.height)
                        PixelCopy.request(
                            view.holder.surface,
                            bitmap,
                            { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    scope.launch(Dispatchers.Default) {
                                        val objects = detector.detectObjects(bitmap)
                                        withContext(Dispatchers.Main) {
                                            detectedObjects = objects
                                        }
                                        bitmap.recycle()
                                    }
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )
                    } catch (e: Exception) {
                        AppLogger.log("Frame Capture Error: ${e.message}", AppLogger.LogType.ERROR)
                    }
                }
            }
        } else {
            detectedObjects = emptyList()
        }
    }

    val mediaPlayer = remember {
        MediaPlayer(libVLC).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> {
                        statusText = "Connecting..."
                    }

                    MediaPlayer.Event.Playing -> {
                        statusText = "LIVE"
                        bufferPercent = 0f
                        isConnecting = false
                    }

                    MediaPlayer.Event.Buffering -> {
                        bufferPercent = event.buffering
                        if (event.buffering < 100f) {
                            statusText = "Puffert ${event.buffering.toInt()}%"
                        } else {
                            statusText = "LIVE"
                        }
                    }

                    MediaPlayer.Event.EncounteredError -> {
                        statusText = "Error"
                        isPlaying = false
                        isConnecting = false
                    }

                    MediaPlayer.Event.Stopped -> {
                        statusText = ""
                        isConnecting = false
                    }
                }
            }
            this.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recordingPlayer?.stop()
            recordingPlayer?.release()
            mediaPlayer.release()
            libVLC.release()
            detector.close()
        }
    }

    // Connection Logic Helpers
    val disconnectCamera: () -> Unit = {
        scope.launch {
            if (isRecording) {
                stopRecordingViaVlc()
            }
            mediaPlayer.stop()
            isPlaying = false
            statusText = ""
            withContext(Dispatchers.IO) {
                (context as? MainActivity)?.wifiAutoConnect?.disconnect()
            }
            AppLogger.log("Camera disconnected", AppLogger.LogType.INFO)
        }
    }

    val connectCamera: () -> Unit = {
        isConnecting = true
        scope.launch {
            val connected = withContext(Dispatchers.IO) {
                (context as? MainActivity)?.wifiAutoConnect?.connectToCamera()
                    ?: false
            }

            if (connected || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                statusText = "Connecting..."
                try {
                    val media = Media(libVLC, rtspUrl.toUri())
                    media.addOption(":network-caching=300")
                    media.addOption(":rtsp-tcp")
                    mediaPlayer.media = media
                    media.release()
                    mediaPlayer.play()
                    isPlaying = true

                    scope.launch {
                        try {
                            val info = apiClient.getDeviceInfo()
                            if (info != null) {
                                deviceInfo = info
                                cameraCapabilities = info.getCapabilities()
                                CameraSettings.saveDeviceInfo(context, info)
                                AppLogger.log("Kamera erkannt: ${info.deviceName}", AppLogger.LogType.SUCCESS)
                            }
                        } catch (e: Exception) {
                            AppLogger.log("Device-Info nicht verfugbar", AppLogger.LogType.INFO)
                        }
                    }
                } catch (e: Exception) {
                    statusText = "Stream Error"
                    isConnecting = false
                    AppLogger.log(
                        "Stream Error: ${e.message}",
                        AppLogger.LogType.ERROR
                    )
                }
            } else {
                statusText = "WiFi Error"
                isConnecting = false
                Toast.makeText(
                    context,
                    context.getString(R.string.wifi_connection_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Lifecycle Observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (isPlaying) {
                    wasPlayingBeforeStop = true
                    disconnectCamera()
                }
            } else if (event == Lifecycle.Event.ON_START) {
                if (wasPlayingBeforeStop) {
                    wasPlayingBeforeStop = false
                    connectCamera()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightColors.background)
    ) {
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Einstellungen",
                tint = NightColors.onSurface
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 12.dp),
                    color = NightColors.primary,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Connecting to camera...",
                    fontSize = 12.sp,
                    color = NightColors.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(NightColors.recording, shape = RoundedCornerShape(50))
                    )
                    Text(
                        text = "REC ${formatDuration(recordingDuration)}",
                        fontSize = 12.sp,
                        color = NightColors.recording,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (statusText.isNotEmpty() && !isPlaying) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = NightColors.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .align(Alignment.Center)
                        .background(Color.Black)
                ) {
                    if (isPlaying) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).apply {
                                    surfaceView = this
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    holder.addCallback(object :
                                        android.view.SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                            mediaPlayer.vlcVout.setVideoView(this@apply)
                                            mediaPlayer.vlcVout.attachViews()
                                        }

                                        override fun surfaceChanged(
                                            holder: android.view.SurfaceHolder,
                                            format: Int,
                                            width: Int,
                                            height: Int
                                        ) {
                                            mediaPlayer.vlcVout.setWindowSize(width, height)
                                        }

                                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                            mediaPlayer.vlcVout.detachViews()
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Canvas overlay for AI detections
                        if (objectDetectionEnabled && detectedObjects.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                detectedObjects.forEach { obj ->
                                    drawRect(
                                        color = NightColors.primary,
                                        topLeft = Offset(obj.boundingBox.left, obj.boundingBox.top),
                                        size = Size(obj.boundingBox.width(), obj.boundingBox.height()),
                                        style = Stroke(width = 4f)
                                    )

                                    val germanLabel = KNOWN_OBJECTS[obj.label]?.label ?: obj.label
                                    val distanceText = obj.estimatedDistance?.let {
                                        " • %.1fm".format(it)
                                    } ?: ""
                                    val confidenceText = " (%.0f%%)".format(obj.confidence * 100)
                                    val fullText = "$germanLabel$distanceText$confidenceText"

                                    val textPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.GREEN
                                        textSize = 40f
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    }
                                    val textBounds = android.graphics.Rect()
                                    textPaint.getTextBounds(fullText, 0, fullText.length, textBounds)

                                    val textX = obj.boundingBox.left.coerceAtLeast(0f)
                                    val textY = (obj.boundingBox.top - 12f).coerceAtLeast(textBounds.height().toFloat())

                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        topLeft = Offset(textX, textY - textBounds.height()),
                                        size = Size(textBounds.width().toFloat() + 16f, textBounds.height().toFloat() + 8f)
                                    )

                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.drawText(
                                            fullText,
                                            textX + 8f,
                                            textY,
                                            textPaint
                                        )
                                    }
                                }
                            }
                        }

                    } else if (!isConnecting) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.offline),
                                fontSize = 13.sp,
                                color = NightColors.onBackground
                            )
                        }
                    }

                    if (bufferPercent > 0f && bufferPercent < 100f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    progress = { bufferPercent / 100f },
                                    modifier = Modifier.size(44.dp),
                                    color = NightColors.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${bufferPercent.toInt()}%",
                                    fontSize = 13.sp,
                                    color = NightColors.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
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
                        Icon(
                            Icons.Filled.ZoomIn,
                            contentDescription = null,
                            tint = NightColors.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.zoom),
                            fontSize = 12.sp,
                            color = NightColors.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.1fx", zoomLevel / 10),
                        fontSize = 12.sp,
                        color = NightColors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = zoomLevel,
                    onValueChange = { zoomLevel = it },
                    onValueChangeFinished = {
                        scope.launch {
                            setZoom(zoomLevel.toInt())
                        }
                    },
                    valueRange = 10f..40f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = NightColors.primaryDim,
                        activeTrackColor = NightColors.primaryDim,
                        inactiveTrackColor = NightColors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Palette selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = null,
                            tint = NightColors.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.palette),
                            fontSize = 11.sp,
                            color = NightColors.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val palettes = listOf(
                        Triple("whitehot", "White Hot", R.drawable.stream_palette_whitehot),
                        Triple("blackhot", "Black Hot", R.drawable.stream_palette_blackhot),
                        Triple("redhot", "Red Hot", R.drawable.stream_palette_tint2),
                        Triple("iron", "Iron", R.drawable.stream_palette_ironred),
                        Triple("bluehot", "Blue", R.drawable.stream_palette_bluehot),
                        Triple("greenhot", "Green", R.drawable.stream_palette_greenhot),
                        Triple("darkbrown", "Brown", R.drawable.stream_palette_dark_brown)
                    )

                    palettes.forEach { (id, name, imageRes) ->
                        PaletteButton(
                            imageRes = imageRes,
                            name = name,
                            isSelected = selectedPalette == id,
                            onClick = {
                                scope.launch {
                                    val success = setPalette(id)
                                    if (success) selectedPalette = id
                                }
                            },
                            modifier = Modifier.width(65.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkButton(
                        text = if (isPlaying) stringResource(R.string.disconnect) else stringResource(R.string.connect),
                        icon = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        onClick = {
                            if (isPlaying) {
                                disconnectCamera()
                            } else {
                                connectCamera()
                            }
                        },
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f)
                    )

                    DarkButton(
                        text = stringResource(R.string.gallery),
                        icon = Icons.Filled.PhotoLibrary,
                        onClick = {
                            showGalleryDialog = true
                        },
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkButton(
                        text = if (isRecording) stringResource(R.string.stop_rec) else stringResource(R.string.record),
                        icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                        onClick = {
                            scope.launch {
                                if (isRecording) {
                                    stopRecordingViaVlc()
                                } else {
                                    startRecordingViaVlc()
                                }
                            }
                        },
                        enabled = isPlaying,
                        modifier = Modifier.weight(1f),
                        isRecording = isRecording
                    )

                    DarkButton(
                        text = stringResource(R.string.photo),
                        icon = Icons.Filled.Camera,
                        onClick = {
                            scope.launch {
                                surfaceView?.let { view ->
                                    captureScreenshot(context, view)
                                    galleryRefreshKey++
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.photo_saved),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = isPlaying && !isRecording,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hunting Assistant Button (full width)
                DarkButton(
                    text = stringResource(R.string.hunting_assistant),
                    icon = Icons.Filled.Forest,
                    onClick = { showHuntingHub = true },
                    enabled = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showSettingsDialog) {
            Dialog(
                onDismissRequest = { showSettingsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                SettingsScreen(
                    audioEnabled = audioEnabled,
                    hotspotEnabled = hotspotEnabled,
                    brightness = brightness,
                    contrast = contrast,
                    enhancementEnabled = enhancementEnabled,
                    objectDetectionEnabled = objectDetectionEnabled,
                    cameraIp = cameraIp,
                    onClose = { showSettingsDialog = false },
                    onAudioChange = { enabled ->
                        scope.launch {
                            val success = setAudio(if (enabled) "on" else "off")
                            if (success) audioEnabled = enabled
                        }
                    },
                    onHotspotChange = { enabled ->
                        scope.launch {
                            val success = setHotspot(enabled)
                            if (success) hotspotEnabled = enabled
                        }
                    },
                    onBrightnessChange = { level ->
                        scope.launch {
                            val success = setBrightness(level)
                            if (success) brightness = level
                        }
                    },
                    onContrastChange = { level ->
                        scope.launch {
                            val success = setContrast(level)
                            if (success) contrast = level
                        }
                    },
                    onEnhancementChange = { enabled ->
                        scope.launch {
                            val success = setEnhancement(enabled)
                            if (success) enhancementEnabled = enabled
                        }
                    },
                    onObjectDetectionChange = { enabled ->
                        objectDetectionEnabled = enabled
                        AppLogger.log("AI Erkennung ${if (enabled) "EIN" else "AUS"}", AppLogger.LogType.INFO)
                    },
                    onCameraIpChange = { newIp ->
                        CameraSettings.setCameraIp(context, newIp)
                        cameraIp = newIp
                        AppLogger.log("Kamera IP geandert: $newIp", AppLogger.LogType.SUCCESS)
                    },
                    onShowLog = { /* Handled internally by SettingsScreen */ },
                    onShowAbout = { /* Handled internally by SettingsScreen */ },
                    onShowThermalSettings = {
                        showSettingsDialog = false
                        showThermalSettingsDialog = true
                    },
                    onWifiSsidChange = { newSsid ->
                        CameraSettings.setWifiSsid(context, newSsid)
                        (context as? MainActivity)?.updateWifiAutoConnect()
                    },
                    onWifiPasswordChange = { newPassword ->
                        CameraSettings.setWifiPassword(context, newPassword)
                        (context as? MainActivity)?.updateWifiAutoConnect()
                    },
                    onHttpPortChange = { newPort ->
                        CameraSettings.setHttpPort(context, newPort)
                    },
                    onAutoConnectChange = { enabled ->
                        CameraSettings.setAutoConnectEnabled(context, enabled)
                    },
                    onShowWhatsNew = { /* Handled internally by SettingsScreen */ },
                    onShowFeatureBounties = {
                        showSettingsDialog = false
                        showFeatureBountiesDialog = true
                    }
                )
            }
        }

        if (showFeatureBountiesDialog) {
            Dialog(
                onDismissRequest = { showFeatureBountiesDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                FeatureBountyScreen(
                    billingManager = billingManager,
                    repository = featureRepository,
                    onClose = { showFeatureBountiesDialog = false }
                )
            }
        }

        if (showWelcomeDialog) {
            WelcomeDialog(onDismiss = {
                val currentVersionCode = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                    }
                } catch (e: Exception) {
                    1
                }

                CameraSettings.setFirstRunCompleted(context)
                CameraSettings.setLastVersionCode(context, currentVersionCode)
                showWelcomeDialog = false
            })
        }

        if (showWhatsNewDialog) {
            WhatsNewDialog(onDismiss = {
                val currentVersionCode = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                    }
                } catch (e: Exception) {
                    1
                }

                CameraSettings.setLastVersionCode(context, currentVersionCode)
                showWhatsNewDialog = false
            })
        }

        if (showGalleryDialog) {
            key(galleryRefreshKey) {
                GalleryDialog(
                    baseUrl = baseUrl,
                    onDismiss = { showGalleryDialog = false }
                )
            }
        }

        if (showHuntingHub) {
            Dialog(
                onDismissRequest = { showHuntingHub = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                HuntingHubScreen(onClose = { showHuntingHub = false })
            }
        }

        if (showThermalSettingsDialog) {
            Dialog(
                onDismissRequest = { showThermalSettingsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                ThermalSettingsScreen(
                    deviceInfo = deviceInfo,
                    capabilities = cameraCapabilities,
                    emissivity = emissivity,
                    measureDistance = measureDistance,
                    humidity = humidity,
                    reflectTemperature = reflectTemperature,
                    isShutterInProgress = isShutterInProgress,
                    onClose = { showThermalSettingsDialog = false },
                    onEmissivityChange = { value ->
                        emissivity = value
                        CameraSettings.setEmissivity(context, value)
                        AppLogger.log("Emissivitat: ${"%.2f".format(value)}", AppLogger.LogType.INFO)
                    },
                    onDistanceChange = { value ->
                        measureDistance = value
                        CameraSettings.setDistance(context, value)
                        AppLogger.log("Entfernung: ${"%.1f".format(value)} m", AppLogger.LogType.INFO)
                    },
                    onHumidityChange = { value ->
                        humidity = value
                        CameraSettings.setHumidity(context, value)
                        AppLogger.log("Luftfeuchtigkeit: ${"%.0f".format(value)} %", AppLogger.LogType.INFO)
                    },
                    onReflectTempChange = { value ->
                        reflectTemperature = value
                        CameraSettings.setReflectTemperature(context, value)
                        AppLogger.log("Reflexionstemperatur: ${"%.1f".format(value)} C", AppLogger.LogType.INFO)
                    },
                    onShutterClick = {
                        scope.launch {
                            isShutterInProgress = true
                            AppLogger.log("Starte Shutter/NUC Kalibrierung...", AppLogger.LogType.INFO)
                            val success = apiClient.triggerShutter()
                            if (success) {
                                AppLogger.log("Shutter/NUC erfolgreich", AppLogger.LogType.SUCCESS)
                            } else {
                                AppLogger.log("Shutter/NUC Fehler", AppLogger.LogType.ERROR)
                            }
                            isShutterInProgress = false
                        }
                    },
                    onApplySettings = {
                        scope.launch {
                            AppLogger.log("Sende Einstellungen an Kamera...", AppLogger.LogType.INFO)
                            var successCount = 0

                            if (apiClient.setEmission(emissivity)) successCount++
                            if (apiClient.setDistance(measureDistance)) successCount++
                            if (apiClient.setHumidity(humidity)) successCount++
                            if (apiClient.setReflectTemperature(reflectTemperature)) successCount++

                            if (successCount > 0) {
                                AppLogger.log("$successCount/4 Einstellungen ubertragen", AppLogger.LogType.SUCCESS)
                            } else {
                                AppLogger.log("Einstellungen konnten nicht ubertragen werden", AppLogger.LogType.ERROR)
                            }
                        }
                    }
                )
            }
        }
    }
}
