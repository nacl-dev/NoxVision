package com.noxvision.app

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.isActive

import android.content.Context
import android.graphics.Bitmap

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

object AppLogger {
    data class LogEntry(
        val timestamp: String,
        val message: String,
        val type: LogType
    )

    enum class LogType {
        INFO, SUCCESS, ERROR
    }

    private val logs = mutableStateListOf<LogEntry>()
    val logsList: List<LogEntry> get() = logs

    fun log(message: String, type: LogType = LogType.INFO) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, LogEntry(timestamp, message, type))
        if (logs.size > 100) {
            logs.removeAt(logs.size - 1)
        }
        Log.d("AppLogger", "[$type] $message")
    }

    fun clear() {
        logs.clear()
    }
}

data class CameraFile(
    val name: String,
    val size: Long,
    val date: String,
    val type: String
)

enum class GallerySource { CAMERA_DEVICE, PHONE }
enum class PhoneFolder { CAMERA, PICTURES }

data class PhoneMediaFile(
    val uri: android.net.Uri,
    val name: String,
    val size: Long,
    val dateAddedSec: Long,
    val mime: String,
    val isVideo: Boolean
)

class WiFiAutoConnect(
    private val context: Context,
    private val cameraSSID: String,
    private val cameraPassword: String
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    suspend fun connectToCamera(): Boolean = withContext(Dispatchers.IO) {
        try {
            AppLogger.log("Connecting to $cameraSSID...", AppLogger.LogType.INFO)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectModern()
            } else {
                AppLogger.log(
                    "Android 9: Please connect manually to $cameraSSID",
                    AppLogger.LogType.INFO
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Please connect manually to $cameraSSID",
                        Toast.LENGTH_LONG
                    ).show()
                }
                false
            }
        } catch (e: Exception) {
            AppLogger.log("WiFi Error: ${e.message}", AppLogger.LogType.ERROR)
            false
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun connectModern(): Boolean = withContext(Dispatchers.Main) {
        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(cameraSSID)
                .setWpa2Passphrase(cameraPassword)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            var connected = false

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.bindProcessToNetwork(network)
                    connected = true
                    AppLogger.log("WiFi connected: $cameraSSID", AppLogger.LogType.SUCCESS)
                }

                override fun onUnavailable() {
                    AppLogger.log("WiFi not available", AppLogger.LogType.ERROR)
                }
            }

            connectivityManager.requestNetwork(request, networkCallback!!)

            var attempts = 0
            while (!connected && attempts < 100) {
                delay(500)
                attempts++
                if (attempts % 4 == 0) {
                    AppLogger.log("Waiting for connection... (${attempts/2}s)", AppLogger.LogType.INFO)
                }
            }

            if (connected) {
                delay(1000)
            } else {
                AppLogger.log("Timeout after ${attempts/2}s", AppLogger.LogType.ERROR)
            }

            connected
        } catch (e: Exception) {
            AppLogger.log("WiFi Error: ${e.message}", AppLogger.LogType.ERROR)
            false
        }
    }

    fun disconnect() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                networkCallback?.let {
                    connectivityManager.unregisterNetworkCallback(it)
                    connectivityManager.bindProcessToNetwork(null)
                }
                networkCallback = null
            }
            AppLogger.log("WiFi disconnected", AppLogger.LogType.INFO)
        } catch (e: Exception) {
            AppLogger.log("Disconnect Error: ${e.message}", AppLogger.LogType.ERROR)
        }
    }
}
// Known object sizes in meters
data class ObjectInfo(val label: String, val heightMeters: Float)

val KNOWN_OBJECTS = mapOf(
    "Person" to ObjectInfo("Person", 1.7f),
    "bicycle" to ObjectInfo("Bicycle", 1.0f),
    "car" to ObjectInfo("Car", 1.5f),
    "motorcycle" to ObjectInfo("Motorcycle", 1.2f),
    "bus" to ObjectInfo("Bus", 3.0f),
    "truck" to ObjectInfo("Truck", 3.0f),
    "dog" to ObjectInfo("Dog", 0.6f),
    "cat" to ObjectInfo("Cat", 0.3f)
)

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF,
    val estimatedDistance: Float? = null
)

class ThermalObjectDetector(context: Context) {
    private var interpreter: org.tensorflow.lite.Interpreter? = null
    private val labels = mutableListOf<String>()
    private var isInitialized = false

    // IMPORTANT: YOLOv8 uses 640x640 by default
    private val INPUT_SIZE = 640
    private val FOCAL_LENGTH_PIXELS = 3350f

    init {
        try {
            val modelExists = try {
                context.assets.open("detect.tflite").use { true }
            } catch (e: Exception) {
                false
            }

            if (modelExists) {
                val model = loadModelFile(context, "detect.tflite")
                val options = org.tensorflow.lite.Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = org.tensorflow.lite.Interpreter(model, options)

                loadLabels(context)

                isInitialized = true
                AppLogger.log("Thermal detector (YOLOv8) ready", AppLogger.LogType.SUCCESS)
            } else {
                AppLogger.log("AI model 'detect.tflite' not found", AppLogger.LogType.INFO)
            }
        } catch (e: Exception) {
            AppLogger.log("AI init error: ${e.message}", AppLogger.LogType.ERROR)
            interpreter = null
            isInitialized = false
        }
    }

    private fun loadModelFile(context: Context, filename: String): java.nio.ByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context) {
        try {
            context.assets.open("labelmap.txt").bufferedReader().useLines { lines ->
                labels.addAll(lines.filter { it.isNotBlank() })
            }
        } catch (e: Exception) {
            // Fallback if file is missing (matching your training)
            labels.addAll(listOf("car", "cat", "dog", "Person"))
        }
    }

    fun detectObjects(bitmap: Bitmap): List<DetectedObject> {
        if (!isInitialized || interpreter == null) {
            return emptyList()
        }

        try {
            val startTime = System.currentTimeMillis()

            // 1. Bild vorbereiten
            val processedBitmap = enhanceThermalImage(bitmap)
            val scaledBitmap = Bitmap.createScaledBitmap(processedBitmap, INPUT_SIZE, INPUT_SIZE, true)

            // 2. Input Buffer füllen
            val inputBuffer = convertBitmapToFloatBuffer(scaledBitmap)

            // 3. Output Buffer
            val numClasses = labels.size
            val numElements = 4 + numClasses
            val numAnchors = 8400

            val outputArray = Array(1) { Array(numElements) { FloatArray(numAnchors) } }

            // 4. Inferenz
            interpreter?.run(inputBuffer, outputArray)

            val allDetections = mutableListOf<DetectedObject>()

            for (i in 0 until numAnchors) {
                var maxScore = 0f
                var maxClassIndex = -1

                // Find best class for this anchor
                for (c in 0 until numClasses) {
                    val score = outputArray[0][4 + c][i]
                    if (score > maxScore) {
                        maxScore = score
                        maxClassIndex = c
                    }
                }

                if (maxClassIndex != -1) {
                    val rawLabel = labels[maxClassIndex]

                    val (displayLabel, minConf) = when (rawLabel) {
                        "Person" -> "Person" to 0.45f
                        "car", "truck", "bus" -> "Vehicle" to 0.50f
                        "bicycle", "motorcycle" -> "Bicycle" to 0.50f
                        "dog", "cat", "horse", "cow", "sheep", "bear" -> "Animal" to 0.45f
                        else -> rawLabel to 0.50f
                    }

                    if (maxScore >= minConf) {
                        // --- THIS WAS THE BUG ---
                        // Values come as 0..640. We need to divide by INPUT_SIZE!

                        val cx = outputArray[0][0][i] / INPUT_SIZE.toFloat()
                        val cy = outputArray[0][1][i] / INPUT_SIZE.toFloat()
                        val w = outputArray[0][2][i] / INPUT_SIZE.toFloat()
                        val h = outputArray[0][3][i] / INPUT_SIZE.toFloat()

                        // Now they are 0..1 and we can scale them to bitmap
                        val x1 = (cx - w / 2) * bitmap.width
                        val y1 = (cy - h / 2) * bitmap.height
                        val x2 = (cx + w / 2) * bitmap.width
                        val y2 = (cy + h / 2) * bitmap.height

                        val rect = android.graphics.RectF(x1, y1, x2, y2)

                        if (rect.width() > 50 && rect.height() > 50) {
                            val distance = estimateDistance(rawLabel, rect, bitmap.height)
                            allDetections.add(DetectedObject(displayLabel, maxScore, rect, distance))
                        }
                    }
                }
            }

            val finalDetections = applyNMS(allDetections, iouThreshold = 0.45f)
            val topDetections = finalDetections.sortedByDescending { it.confidence }.take(5)

            val elapsed = System.currentTimeMillis() - startTime
            if (topDetections.isNotEmpty()) {
                AppLogger.log("${topDetections.size} objects (${elapsed}ms) - Top: ${topDetections[0].label}", AppLogger.LogType.INFO)
            }

            scaledBitmap.recycle()
            processedBitmap.recycle()

            return topDetections

        } catch (e: Exception) {
            AppLogger.log("Detection Error: ${e.message}", AppLogger.LogType.ERROR)
            return emptyList()
        }
    }

    // Helper function: Float Buffer instead of Byte Buffer
    private fun convertBitmapToFloatBuffer(bitmap: Bitmap): java.nio.ByteBuffer {
        // 4 Bytes pro Float * H * B * 3 Channels
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(java.nio.ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                // Normalization to 0.0-1.0 (important for this model!)
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }

    // === DEINE BESTEHENDEN HILFSFUNKTIONEN BLEIBEN GLEICH ===

    private fun applyNMS(detections: List<DetectedObject>, iouThreshold: Float): List<DetectedObject> {
        if (detections.isEmpty()) return emptyList()
        val sorted = detections.sortedByDescending { it.confidence }
        val keep = mutableListOf<DetectedObject>()
        val suppressed = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (suppressed[i]) continue
            keep.add(sorted[i])
            for (j in (i + 1) until sorted.size) {
                if (suppressed[j]) continue
                val iou = calculateIoU(sorted[i].boundingBox, sorted[j].boundingBox)
                // Important: Only suppress if same label OR both are "Animal"
                if (iou > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }
        return keep
    }

    private fun calculateIoU(box1: android.graphics.RectF, box2: android.graphics.RectF): Float {
        val intersectLeft = maxOf(box1.left, box2.left)
        val intersectTop = maxOf(box1.top, box2.top)
        val intersectRight = minOf(box1.right, box2.right)
        val intersectBottom = minOf(box1.bottom, box2.bottom)

        if (intersectRight < intersectLeft || intersectBottom < intersectTop) return 0f

        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectArea
        return if (unionArea > 0) intersectArea / unionArea else 0f
    }

    private fun enhanceThermalImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(enhanced)
        val colorMatrix = android.graphics.ColorMatrix().apply {
            val contrast = 1.3f
            val translate = (1f - contrast) * 128f
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            setSaturation(1.2f)
        }
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return enhanced
    }

    private fun estimateDistance(label: String, bbox: android.graphics.RectF, imageHeight: Int): Float? {
        val objInfo = KNOWN_OBJECTS[label] ?: return null
        val objectHeightPx = bbox.height()
        if (objectHeightPx < 10) return null
        val distanceMeters = (objInfo.heightMeters * FOCAL_LENGTH_PIXELS) / objectHeightPx
        return if (distanceMeters in 1f..100f) distanceMeters else null
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {}
    }
}



class MainActivity : ComponentActivity() {

    internal var wifiAutoConnect: WiFiAutoConnect? = null

    internal fun updateWifiAutoConnect() {
        val ssid = CameraSettings.getWifiSsid(this)
        val password = CameraSettings.getWifiPassword(this)
        wifiAutoConnect = WiFiAutoConnect(this, ssid, password)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateWifiAutoConnect()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = NightColors.background,
                    surface = NightColors.surface,
                    primary = NightColors.primary,
                    onBackground = NightColors.onBackground,
                    onSurface = NightColors.onSurface
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NightColors.background
                ) {
                    VideoStreamScreen()
                }
            }
        }
    }
}

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
    var audioEnabled by remember { mutableStateOf(true) }
    var selectedPalette by remember { mutableStateOf("whitehot") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showGalleryDialog by remember { mutableStateOf(false) }
    var galleryRefreshKey by remember { mutableIntStateOf(0) }
    
    // First Run / Whats New
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) { 1 }

        if (CameraSettings.isFirstRun(context)) {
            showWelcomeDialog = true
        } else {
            val lastVersion = CameraSettings.getLastVersionCode(context)
            if (lastVersion != -1 && lastVersion < currentVersionCode) {
                showWhatsNewDialog = true
            } else {
                // Keep version code updated even if no dialog is shown
                CameraSettings.setLastVersionCode(context, currentVersionCode)
            }
        }
    }

    var objectDetectionEnabled by remember { mutableStateOf(false) }
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    val detector = remember { ThermalObjectDetector(context) }

    // Camera IP settings - persisted via SharedPreferences
    var cameraIp by remember { mutableStateOf(CameraSettings.getCameraIp(context)) }
    val rtspUrl = remember(cameraIp) { CameraSettings.getRtspUrl(cameraIp) }
    val baseUrl = remember(cameraIp) { CameraSettings.getBaseUrl(context, cameraIp) }

    // Camera API Client for REST API access
    val apiClient = remember(baseUrl) { CameraApiClient(baseUrl) }
    
    // Device info and capabilities
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(CameraSettings.getCachedDeviceInfo(context)) }
    var cameraCapabilities by remember { mutableStateOf<CameraCapabilities?>(deviceInfo?.getCapabilities()) }
    
    // Thermal measurement settings - persisted via SharedPreferences
    var emissivity by remember { mutableFloatStateOf(CameraSettings.getEmissivity(context)) }
    var measureDistance by remember { mutableFloatStateOf(CameraSettings.getDistance(context)) }
    var humidity by remember { mutableFloatStateOf(CameraSettings.getHumidity(context)) }
    var reflectTemperature by remember { mutableFloatStateOf(CameraSettings.getReflectTemperature(context)) }
    
    // Thermal settings dialog
    var showThermalSettingsDialog by remember { mutableStateOf(false) }
    var isShutterInProgress by remember { mutableStateOf(false) }

    // Lifecycle state
    var wasPlayingBeforeStop by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current



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

    fun buildPaletteFrame(palletId: Int): ByteArray {
        val frameHead = byteArrayOf(0x55.toByte(), 0xAA.toByte())
        val cmd = byteArrayOf(0x02, 0x00, 0x04)  // CommandProtocol.PALLET

        // int2byteArray: Big-Endian 4 Bytes
        val value = byteArrayOf(
            ((palletId shr 24) and 0xFF).toByte(),
            ((palletId shr 16) and 0xFF).toByte(),
            ((palletId shr 8) and 0xFF).toByte(),
            (palletId and 0xFF).toByte()
        )

        val payloadLen: Byte = (cmd.size + value.size).toByte()  // 7

        // Payload: [LEN, CMD..., VALUE...]
        val payload = ByteArray(1 + cmd.size + value.size)
        payload[0] = payloadLen
        System.arraycopy(cmd, 0, payload, 1, cmd.size)
        System.arraycopy(value, 0, payload, 1 + cmd.size, value.size)

        // XOR checksum over all payload bytes
        var checksum = payload[0]
        for (i in 1 until payload.size) {
            checksum = (checksum.toInt() xor payload[i].toInt()).toByte()
        }

        // Frame: [HEAD(2), PAYLOAD(8), CHECKSUM(1), END(1)] = 12 Bytes
        val frame = ByteArray(frameHead.size + payload.size + 1 + 1)
        frame[0] = frameHead[0]
        frame[1] = frameHead[1]
        System.arraycopy(payload, 0, frame, 2, payload.size)
        frame[2 + payload.size] = checksum
        frame[3 + payload.size] = 0xF0.toByte()  // FRAME_END

        return frame
    }

    suspend fun setPalette(paletteName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Map palette names to IDs
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

                // Debug: Output frame as hex
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
                    AppLogger.log("✓ Palette: $paletteName", AppLogger.LogType.SUCCESS)
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

    fun buildHotspotFrame(hotspotMode: Int): ByteArray {
        val frameHead = byteArrayOf(0x55.toByte(), 0xAA.toByte())
        val cmd = byteArrayOf(0x02, 0x00, 0x09)  // CommandProtocol.HOTSPOT

        val value = byteArrayOf(
            ((hotspotMode shr 24) and 0xFF).toByte(),
            ((hotspotMode shr 16) and 0xFF).toByte(),
            ((hotspotMode shr 8) and 0xFF).toByte(),
            (hotspotMode and 0xFF).toByte()
        )

        val payloadLen: Byte = (cmd.size + value.size).toByte()
        val payload = ByteArray(1 + cmd.size + value.size)
        payload[0] = payloadLen
        System.arraycopy(cmd, 0, payload, 1, cmd.size)
        System.arraycopy(value, 0, payload, 1 + cmd.size, value.size)

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


    suspend fun setHotspot(enabled: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modeId = if (enabled) 1 else 0  // 0=off, 1=max (heißester Punkt)

                AppLogger.log("Setze Hotspot: ${if(enabled) "ON" else "OFF"}", AppLogger.LogType.INFO)

                val data = buildHotspotFrame(modeId)
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
                    AppLogger.log("✓ Hotspot: ${if(enabled) "EIN" else "AUS"}", AppLogger.LogType.SUCCESS)
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

                val cmd = byteArrayOf(0x02, 0x03, 0x09)  // LUMINANC
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
                    AppLogger.log("✓ Helligkeit: $clampedLevel", AppLogger.LogType.SUCCESS)
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

                val cmd = byteArrayOf(0x02, 0x03, 0x0A)  // CONTRAS
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
                    AppLogger.log("✓ Kontrast: $clampedLevel", AppLogger.LogType.SUCCESS)
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
                AppLogger.log("Setze Bildverbesserung: ${if(enabled) "EIN" else "AUS"}", AppLogger.LogType.INFO)

                val cmd = byteArrayOf(0x02, 0x03, 0x0B)  // ENHANCEMENT
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
                    AppLogger.log("✓ Bildverbesserung: ${if(enabled) "EIN" else "AUS"}", AppLogger.LogType.SUCCESS)
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


    suspend fun startRecordingViaVlc(libVLC: LibVLC) {
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

// AI Object Detection - Periodic Analysis
    LaunchedEffect(objectDetectionEnabled, isPlaying) {
        if (objectDetectionEnabled && isPlaying) {
            while (isActive && objectDetectionEnabled && isPlaying) {
                delay(1500) // Every 1.5 seconds - gentle for S25

                surfaceView?.let { view ->
                    try {
                        val bitmap = createBitmap(view.width, view.height)
                        PixelCopy.request(
                            view.holder.surface,
                            bitmap,
                            { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    // Detection in background thread
                                    scope.launch(Dispatchers.Default) {
                                        val objects = detector.detectObjects(bitmap)
                                        withContext(Dispatchers.Main) {
                                            detectedObjects = objects
                                        }
                                        bitmap.recycle() // Free memory
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
            // Clear detections when disabled
            detectedObjects = emptyList()
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

    val mediaPlayer = remember {
        MediaPlayer(libVLC).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> {
                        statusText = "Connecting..."
                    }

                    MediaPlayer.Event.Playing -> {
                        statusText = "● LIVE"
                        bufferPercent = 0f
                        isConnecting = false
                    }

                    MediaPlayer.Event.Buffering -> {
                        bufferPercent = event.buffering
                        if (event.buffering < 100f) {
                            statusText = "Puffert ${event.buffering.toInt()}%"
                        } else {
                            statusText = "● LIVE"
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
                        // Canvas overlay for AI detections - NEW
                        if (objectDetectionEnabled && detectedObjects.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                detectedObjects.forEach { obj ->
                                    // Draw bounding box
                                    drawRect(
                                        color = NightColors.primary,
                                        topLeft = Offset(obj.boundingBox.left, obj.boundingBox.top),
                                        size = Size(obj.boundingBox.width(), obj.boundingBox.height()),
                                        style = Stroke(width = 4f)
                                    )

                                    // Create label + distance text
                                    val germanLabel = KNOWN_OBJECTS[obj.label]?.label ?: obj.label
                                    val distanceText = obj.estimatedDistance?.let {
                                        " • %.1fm".format(it)
                                    } ?: ""
                                    val confidenceText = " (%.0f%%)".format(obj.confidence * 100)
                                    val fullText = "$germanLabel$distanceText$confidenceText"

                                    // Draw text with nativeCanvas
                                    val textPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.GREEN
                                        textSize = 40f
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    }
                                    val textBounds = android.graphics.Rect()
                                    textPaint.getTextBounds(fullText, 0, fullText.length, textBounds)

                                    val textX = obj.boundingBox.left.coerceAtLeast(0f)
                                    val textY = (obj.boundingBox.top - 12f).coerceAtLeast(textBounds.height().toFloat())

                                    // Black background for text
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        topLeft = Offset(textX, textY - textBounds.height()),
                                        size = Size(textBounds.width().toFloat() + 16f, textBounds.height().toFloat() + 8f)
                                    )

                                    // Text zeichnen mit drawIntoCanvas
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
                                text = "Offline",
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

                            // Try to detect device info via REST API
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
                                    AppLogger.log("Device-Info nicht verfügbar", AppLogger.LogType.INFO)
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
                            "WiFi Verbindung fehlgeschlagen",
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
                            // Only stop the player, but maybe keep WiFi? 
                            // User report says WiFi/Context is lost, so full disconnect might be safer to ensure clean state on return.
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
                            text = "Zoom",
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

            // === PALETTE SCHNELLWAHL ===
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
                            text = "Palette",
                            fontSize = 11.sp,
                            color = NightColors.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Palette-Buttons in scrollbarer Row
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
                        text = if (isPlaying) "Disconnect" else "Connect",
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
                        text = "Gallery",
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
                        text = if (isRecording) "Stop Rec" else "Record",
                        icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                        onClick = {
                            scope.launch {
                                if (isRecording) {
                                    stopRecordingViaVlc()
                                } else {
                                    startRecordingViaVlc(libVLC)
                                }
                            }
                        },
                        enabled = isPlaying,
                        modifier = Modifier.weight(1f),
                        isRecording = isRecording
                    )

                    DarkButton(
                        text = "Foto",
                        icon = Icons.Filled.Camera,
                        onClick = {
                            scope.launch {
                                surfaceView?.let { view ->
                                    captureScreenshot(context, view)
                                    galleryRefreshKey++
                                    Toast.makeText(
                                        context,
                                        "Photo saved",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = isPlaying && !isRecording,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (showSettingsDialog) {
            SettingsDialogContent(
                audioEnabled = audioEnabled,
                hotspotEnabled = hotspotEnabled,
                brightness = brightness,
                contrast = contrast,
                enhancementEnabled = enhancementEnabled,
                objectDetectionEnabled = objectDetectionEnabled,
                cameraIp = cameraIp,
                onDismiss = { showSettingsDialog = false },
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
                    AppLogger.log("Kamera IP geändert: $newIp", AppLogger.LogType.SUCCESS)
                },
                onShowLog = {
                    showSettingsDialog = false
                    showLogDialog = true
                },
                onShowAbout = {
                    showSettingsDialog = false
                    showAboutDialog = true
                },
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
                onShowWhatsNew = {
                    showSettingsDialog = false
                    showWhatsNewDialog = true
                }
            )
        }

        if (showAboutDialog) {
            AboutDialogContent(
                onDismiss = { showAboutDialog = false }
            )
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
                } catch (e: Exception) { 1 }

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
                } catch (e: Exception) { 1 }

                CameraSettings.setLastVersionCode(context, currentVersionCode)
                showWhatsNewDialog = false
            })
        }

        if (showLogDialog) {
            LogDialogContent(
                onDismiss = { showLogDialog = false }
            )
        }

        if (showGalleryDialog) {
            key(galleryRefreshKey) {  // NEU: Key für Refresh
                GalleryDialog(
                    baseUrl = baseUrl,
                    onDismiss = { showGalleryDialog = false }
                )
            }
        }
        
        if (showThermalSettingsDialog) {
            ThermalSettingsDialogContent(
                deviceInfo = deviceInfo,
                capabilities = cameraCapabilities,
                emissivity = emissivity,
                measureDistance = measureDistance,
                humidity = humidity,
                reflectTemperature = reflectTemperature,
                isShutterInProgress = isShutterInProgress,
                onDismiss = { showThermalSettingsDialog = false },
                onEmissivityChange = { value ->
                    emissivity = value
                    CameraSettings.setEmissivity(context, value)
                    AppLogger.log("Emissivität: ${"%.2f".format(value)}", AppLogger.LogType.INFO)
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
                    AppLogger.log("Reflexionstemperatur: ${"%.1f".format(value)} °C", AppLogger.LogType.INFO)
                },
                onShutterClick = {
                    scope.launch {
                        isShutterInProgress = true
                        AppLogger.log("Starte Shutter/NUC Kalibrierung...", AppLogger.LogType.INFO)
                        val success = apiClient.triggerShutter()
                        if (success) {
                            AppLogger.log("✓ Shutter/NUC erfolgreich", AppLogger.LogType.SUCCESS)
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
                            AppLogger.log("✓ $successCount/4 Einstellungen übertragen", AppLogger.LogType.SUCCESS)
                        } else {
                            AppLogger.log("Einstellungen konnten nicht übertragen werden", AppLogger.LogType.ERROR)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PaletteButton(
    imageRes: Int,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(NightColors.surface) // Always black/surface background
            .border(
                width = if (isSelected) 1.dp else 1.dp, // Always bordered? User said "umrandet like Settings"
                // Settings buttons are outlined. Unselected palette items usually don't have borders or have faint ones.
                // "Genau wie die Settings buttons umranded" -> Settings buttons have primary border.
                // "Schwarzer hintergrund bei auswahl".
                color = if (isSelected) NightColors.primary else Color.Transparent, 
                shape = RoundedCornerShape(8.dp)
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = name,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 9.sp,
            color = if (isSelected) NightColors.primary else NightColors.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
@Composable
fun GalleryDialog(
    baseUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(GallerySource.PHONE) }  // Geändert von CAMERA_DEVICE
    var phoneFolder by remember { mutableStateOf(PhoneFolder.PICTURES) }
    var cameraFiles by remember { mutableStateOf<List<CameraFile>>(emptyList()) }
    var phoneFiles by remember { mutableStateOf<List<PhoneMediaFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCameraFile by remember { mutableStateOf<CameraFile?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }  // NEU

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


    // Beim Öffnen des Dialogs neu laden - nicht nötig, da erster Run automatisch
    // LaunchedEffect(Unit) { refreshTrigger++ }

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

                if (source == GallerySource.PHONE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        FilterChip(
                            selected = phoneFolder == PhoneFolder.PICTURES,
                            onClick = { phoneFolder = PhoneFolder.PICTURES },
                            label = { Text("Bilder", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = phoneFolder == PhoneFolder.CAMERA,
                            onClick = { phoneFolder = PhoneFolder.CAMERA },
                            label = { Text("Kamera", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
@Composable
fun FileCard(
    file: CameraFile,
    onClick: () -> Unit,
    baseUrl: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = NightColors.primaryDim
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (file.type == "image") {
                // Echte Bildvorschau für Fotos
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("$baseUrl/api/v1/files/download/${file.name}")
                        .crossfade(true)
                        .build(),
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Video icon with filename
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoFile,
                        contentDescription = null,
                        tint = NightColors.onSurface,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = file.name,
                        fontSize = 9.sp,
                        color = NightColors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneFileCard(file: PhoneMediaFile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NightColors.primaryDim)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Echte Bildvorschau für Handy-Medien
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play-Icon für Videos
            if (file.isVideo) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

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
                    if (isPlaying) {
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
                                                    val videoUrl = "$baseUrl/api/v1/files/download/${file.name}"
                                                    val media = Media(libVLC, videoUrl.toUri())
                                                    previewPlayer.media = media
                                                    media.release()
                                                    previewPlayer.play()
                                                } catch (e: Exception) {
                                                    AppLogger.log("Preview error: ${e.message}", AppLogger.LogType.ERROR)
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
                        // GEÄNDERT: Echte Bildvorschau für Bilder
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
                            // Icon nur noch für Videos
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
                                } else {
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NightColors.primaryDim
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "Stop" else "Abspielen")
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
                                            "✓ Saved to gallery",
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

/**
 * Section header for settings categories
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

@Composable
fun SettingsDialogContent(
    audioEnabled: Boolean,
    hotspotEnabled: Boolean,
    brightness: Int,
    contrast: Int,
    enhancementEnabled: Boolean,
    objectDetectionEnabled: Boolean,
    cameraIp: String,
    onDismiss: () -> Unit,
    onAudioChange: (Boolean) -> Unit,
    onHotspotChange: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onContrastChange: (Int) -> Unit,
    onEnhancementChange: (Boolean) -> Unit,
    onObjectDetectionChange: (Boolean) -> Unit,
    onCameraIpChange: (String) -> Unit,
    onShowLog: () -> Unit,
    onShowAbout: () -> Unit,
    onShowThermalSettings: () -> Unit,
    onWifiSsidChange: (String) -> Unit,
    onWifiPasswordChange: (String) -> Unit,
    onHttpPortChange: (Int) -> Unit,
    onAutoConnectChange: (Boolean) -> Unit,
    onShowWhatsNew: () -> Unit
) {
    var editingIp by remember { mutableStateOf(cameraIp) }
    var ipError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var editingSsid by remember { mutableStateOf(CameraSettings.getWifiSsid(context)) }
    var editingPassword by remember { mutableStateOf(CameraSettings.getWifiPassword(context)) }
    var editingPort by remember { mutableStateOf(CameraSettings.getHttpPort(context).toString()) }
    var autoConnectEnabled by remember { mutableStateOf(CameraSettings.isAutoConnectEnabled(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Einstellungen",
                color = NightColors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══════════════════════════════════════════════════════════
                // 🔌 VERBINDUNG
                // ═══════════════════════════════════════════════════════════
                SettingsSectionHeader(
                    icon = { Icon(Icons.Filled.Router, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                    title = "VERBINDUNG"
                )
                
                OutlinedTextField(
                    value = editingIp,
                    onValueChange = { newValue ->
                        editingIp = newValue
                        ipError = !CameraSettings.isValidIp(newValue)
                    },
                    label = { Text("Kamera IP-Adresse") },
                    isError = ipError,
                    supportingText = {
                        if (ipError) {
                            Text(text = "Ungültige IP-Adresse", color = NightColors.error)
                        }
                    },
                    trailingIcon = {
                        if (editingIp != cameraIp && !ipError) {
                            IconButton(onClick = { onCameraIpChange(editingIp) }) {
                                Icon(Icons.Filled.Check, contentDescription = "Speichern", tint = NightColors.success)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NightColors.primary,
                        unfocusedBorderColor = NightColors.onBackground,
                        focusedLabelColor = NightColors.primary,
                        unfocusedLabelColor = NightColors.onBackground,
                        cursorColor = NightColors.primary,
                        focusedTextColor = NightColors.onSurface,
                        unfocusedTextColor = NightColors.onSurface
                    )
                )
                if (editingIp != CameraSettings.getDefaultIp()) {
                    TextButton(
                        onClick = {
                            editingIp = CameraSettings.getDefaultIp()
                            ipError = false
                            onCameraIpChange(CameraSettings.getDefaultIp())
                        }
                    ) {
                        Text(text = "Zurücksetzen auf Standard-IP", color = NightColors.primary, fontSize = 12.sp)
                    }
                }

                // WiFi Settings
                SettingsToggleRow(
                    icon = Icons.Filled.Wifi,
                    label = "WiFi Auto-Connect",
                    checked = autoConnectEnabled,
                    onCheckedChange = { 
                        autoConnectEnabled = it
                        onAutoConnectChange(it)
                    }
                )

                if (autoConnectEnabled) {
                    OutlinedTextField(
                        value = editingSsid,
                        onValueChange = { 
                            editingSsid = it
                            onWifiSsidChange(it)
                        },
                        label = { Text("WiFi SSID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NightColors.primary,
                            unfocusedBorderColor = NightColors.onBackground,
                            focusedTextColor = NightColors.onSurface,
                            unfocusedTextColor = NightColors.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = editingPassword,
                        onValueChange = { 
                            editingPassword = it
                            onWifiPasswordChange(it)
                        },
                        label = { Text("WiFi Passwort") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NightColors.primary,
                            unfocusedBorderColor = NightColors.onBackground,
                            focusedTextColor = NightColors.onSurface,
                            unfocusedTextColor = NightColors.onSurface
                        )
                    )
                }

                OutlinedTextField(
                    value = editingPort,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            editingPort = it
                            it.toIntOrNull()?.let { port -> onHttpPortChange(port) }
                        }
                    },
                    label = { Text("HTTP API Port (Standard: 80)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NightColors.primary,
                        unfocusedBorderColor = NightColors.onBackground,
                        focusedTextColor = NightColors.onSurface,
                        unfocusedTextColor = NightColors.onSurface
                    )
                )

                HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

                // ═══════════════════════════════════════════════════════════
                // 📷 KAMERA
                // ═══════════════════════════════════════════════════════════
                SettingsSectionHeader(
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                    title = "KAMERA"
                )

                SettingsToggleRow(
                    icon = if (audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    label = "Audio",
                    checked = audioEnabled,
                    onCheckedChange = onAudioChange
                )

                SettingsToggleRow(
                    icon = Icons.Filled.MyLocation,
                    label = "Hotspot anzeigen",
                    checked = hotspotEnabled,
                    onCheckedChange = onHotspotChange
                )

                // Helligkeit
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Brightness6, contentDescription = null, tint = NightColors.onSurface)
                        Text(text = "Helligkeit", color = NightColors.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { level ->
                            FilterChip(
                                selected = brightness == level,
                                onClick = { onBrightnessChange(level) },
                                label = {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("$level")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Kontrast
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Contrast, contentDescription = null, tint = NightColors.onSurface)
                        Text(text = "Kontrast", color = NightColors.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { level ->
                            FilterChip(
                                selected = contrast == level,
                                onClick = { onContrastChange(level) },
                                label = {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("$level")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Thermal Settings Button
                Button(
                    onClick = onShowThermalSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary)
                ) {
                    Icon(Icons.Filled.Thermostat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thermische Einstellungen")
                }

                HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

                // ═══════════════════════════════════════════════════════════
                // 🤖 APP-FUNKTIONEN
                // ═══════════════════════════════════════════════════════════
                SettingsSectionHeader(
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                    title = "APP-FUNKTIONEN"
                )

                SettingsToggleRow(
                    icon = Icons.Filled.Visibility,
                    label = "AI Objekterkennung",
                    checked = objectDetectionEnabled,
                    onCheckedChange = onObjectDetectionChange
                )

                SettingsToggleRow(
                    icon = Icons.Filled.AutoFixHigh,
                    label = "Bildverbesserung",
                    checked = enhancementEnabled,
                    onCheckedChange = onEnhancementChange
                )

                HorizontalDivider(color = NightColors.surface, modifier = Modifier.padding(vertical = 4.dp))

                // ═══════════════════════════════════════════════════════════
                // ℹ️ SYSTEM
                // ═══════════════════════════════════════════════════════════
                SettingsSectionHeader(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = NightColors.primary, modifier = Modifier.size(18.dp)) },
                    title = "SYSTEM"
                )

                // System Buttons - Modern Outlined Style
                OutlinedButton(
                    onClick = onShowLog,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NightColors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NightColors.primary,
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Log anzeigen")
                }

                OutlinedButton(
                    onClick = onShowAbout,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NightColors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NightColors.primary,
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Über NoxVision")
                }

                OutlinedButton(
                    onClick = onShowWhatsNew,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NightColors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NightColors.primary,
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(Icons.Filled.NewReleases, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Was ist neu?")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", color = NightColors.primary)
            }
        },
        containerColor = NightColors.surface,
        textContentColor = NightColors.onSurface
    )
}

@Composable
fun ThermalSettingsDialogContent(
    deviceInfo: DeviceInfo?,
    capabilities: CameraCapabilities?,
    emissivity: Float,
    measureDistance: Float,
    humidity: Float,
    reflectTemperature: Float,
    isShutterInProgress: Boolean,
    onDismiss: () -> Unit,
    onEmissivityChange: (Float) -> Unit,
    onDistanceChange: (Float) -> Unit,
    onHumidityChange: (Float) -> Unit,
    onReflectTempChange: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onApplySettings: () -> Unit
) {
    var localEmissivity by remember(emissivity) { mutableFloatStateOf(emissivity) }
    var localDistance by remember(measureDistance) { mutableFloatStateOf(measureDistance) }
    var localHumidity by remember(humidity) { mutableFloatStateOf(humidity) }
    var localReflectTemp by remember(reflectTemperature) { mutableFloatStateOf(reflectTemperature) }
    
    // Emissivity preset dropdown
    var showEmissivityPresets by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Thermische Einstellungen",
                    color = NightColors.onSurface
                )
                if (deviceInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${deviceInfo.deviceName} • ${deviceInfo.videoWidth}x${deviceInfo.videoHeight}",
                        fontSize = 12.sp,
                        color = NightColors.onBackground
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Info Section (if available)
                if (deviceInfo != null && capabilities != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NightColors.background)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Kamera-Features",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NightColors.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (capabilities.hasRadiometry) {
                                    Text("🌡️ Radiometrie", fontSize = 11.sp, color = NightColors.success)
                                }
                                if (capabilities.hasFocus) {
                                    Text("🔍 Fokus", fontSize = 11.sp, color = NightColors.success)
                                }
                                if (capabilities.hasGps) {
                                    Text("📍 GPS", fontSize = 11.sp, color = NightColors.success)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = NightColors.surface)
                }

                // Shutter / NUC Button
                Button(
                    onClick = onShutterClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isShutterInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightColors.primary
                    )
                ) {
                    if (isShutterInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kalibriere...")
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shutter / NUC Kalibrierung")
                    }
                }

                HorizontalDivider(color = NightColors.surface)

                // Emissivity
                Column {
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
                                Icons.Filled.Thermostat,
                                contentDescription = null,
                                tint = NightColors.onSurface
                            )
                            Text(
                                text = "Emissivität",
                                color = NightColors.onSurface
                            )
                        }
                        Text(
                            text = "%.2f".format(localEmissivity),
                            color = NightColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = localEmissivity,
                        onValueChange = { localEmissivity = it },
                        onValueChangeFinished = { onEmissivityChange(localEmissivity) },
                        valueRange = 0.01f..1.0f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = NightColors.primary,
                            activeTrackColor = NightColors.primary
                        )
                    )
                    
                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EmissivityPresets.presets.entries.take(3).forEach { (name, value) ->
                            FilterChip(
                                selected = kotlin.math.abs(localEmissivity - value) < 0.01f,
                                onClick = {
                                    localEmissivity = value
                                    onEmissivityChange(value)
                                },
                                label = { Text(name, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = NightColors.surface)

                // Distance
                Column {
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
                                Icons.Filled.Straighten,
                                contentDescription = null,
                                tint = NightColors.onSurface
                            )
                            Text(
                                text = "Entfernung",
                                color = NightColors.onSurface
                            )
                        }
                        Text(
                            text = "%.1f m".format(localDistance),
                            color = NightColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = localDistance,
                        onValueChange = { localDistance = it },
                        onValueChangeFinished = { onDistanceChange(localDistance) },
                        valueRange = 0.1f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = NightColors.primary,
                            activeTrackColor = NightColors.primary
                        )
                    )
                }

                HorizontalDivider(color = NightColors.surface)

                // Humidity
                Column {
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
                                Icons.Filled.WaterDrop,
                                contentDescription = null,
                                tint = NightColors.onSurface
                            )
                            Text(
                                text = "Luftfeuchtigkeit",
                                color = NightColors.onSurface
                            )
                        }
                        Text(
                            text = "%.0f %%".format(localHumidity),
                            color = NightColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = localHumidity,
                        onValueChange = { localHumidity = it },
                        onValueChangeFinished = { onHumidityChange(localHumidity) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = NightColors.primary,
                            activeTrackColor = NightColors.primary
                        )
                    )
                }

                HorizontalDivider(color = NightColors.surface)

                // Reflected Temperature
                Column {
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
                                Icons.Filled.DeviceThermostat,
                                contentDescription = null,
                                tint = NightColors.onSurface
                            )
                            Text(
                                text = "Reflexionstemperatur",
                                color = NightColors.onSurface
                            )
                        }
                        Text(
                            text = "%.1f °C".format(localReflectTemp),
                            color = NightColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = localReflectTemp,
                        onValueChange = { localReflectTemp = it },
                        onValueChangeFinished = { onReflectTempChange(localReflectTemp) },
                        valueRange = -40f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = NightColors.primary,
                            activeTrackColor = NightColors.primary
                        )
                    )
                }

                HorizontalDivider(color = NightColors.surface)

                // Apply to Camera Button
                Button(
                    onClick = onApplySettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightColors.success
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Einstellungen an Kamera senden")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", color = NightColors.primary)
            }
        },
        containerColor = NightColors.surface,
        textContentColor = NightColors.onSurface
    )
}


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

@Composable
fun DarkButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) {
                NightColors.recording
            } else if (enabled) {
                NightColors.primaryDim
            } else {
                NightColors.surface
            },
            contentColor = if (enabled) NightColors.onSurface else NightColors.onBackground.copy(
                alpha = 0.4f
            ),
            disabledContainerColor = NightColors.surface,
            disabledContentColor = NightColors.onBackground.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

private fun createVideoFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    return File(storageDir, "VID_$timestamp.mp4")
}

private fun saveVideoToGallery(context: Context, file: File) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "VID_$timestamp.mp4"

    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, filename)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/GuideCamera"
            )
        }
    }

    val uri =
        context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}

private suspend fun fetchCameraFiles(baseUrl: String): List<CameraFile> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/v1/files")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use {
                    it.readText()
                }

                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("value")
                val fileList = mutableListOf<CameraFile>()
                for (i in 0 until jsonArray.length()) {
                    val fileObj = jsonArray.getJSONObject(i)
                    val fileName = fileObj.optString("name", "unknown")
                    fileList.add(
                        CameraFile(
                            name = fileName,
                            size = fileObj.optLong("length", 0),
                            date = fileObj.optString("date", ""),
                            type = if (fileName.endsWith(".mp4")) "video" else "image"
                        )
                    )
                }
                conn.disconnect()
                fileList.sortedByDescending { it.date }
            } else {
                conn.disconnect()
                throw Exception("HTTP $responseCode")
            }
        } catch (e: Exception) {
            throw Exception("Connection error: ${e.message}")
        }
    }
}

private suspend fun downloadFile(baseUrl: String, filename: String, appContext: Context) {
    withContext(Dispatchers.IO) {
        val urlsToTry = listOf(
            "$baseUrl/videos/$filename",
            "$baseUrl/api/v1/files/download/videos/$filename",
            "$baseUrl/api/v1/files/videos/$filename",
            "$baseUrl/api/v1/files/download/$filename",
            "$baseUrl/$filename"
        )

        var lastError: Exception? = null

        for (downloadUrl in urlsToTry) {
            try {
                AppLogger.log("Versuche: $downloadUrl", AppLogger.LogType.INFO)
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 30000
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    AppLogger.log(
                        "✓ Korrekte URL: $downloadUrl",
                        AppLogger.LogType.SUCCESS
                    )
                    val inputStream = conn.inputStream

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(
                            MediaStore.MediaColumns.MIME_TYPE,
                            if (filename.endsWith(".mp4")) "video/mp4" else "image/jpeg"
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_DCIM + "/GuideCamera"
                            )
                        }
                    }

                    val uri = if (filename.endsWith(".mp4")) {
                        appContext.contentResolver.insert(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                    } else {
                        appContext.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                    }

                    uri?.let {
                        appContext.contentResolver.openOutputStream(it)?.use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0L
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                            AppLogger.log(
                                "Download OK: ${totalBytes / 1024}KB",
                                AppLogger.LogType.SUCCESS
                            )
                        }
                    }

                    conn.disconnect()
                    return@withContext
                } else {
                    AppLogger.log("✗ HTTP $responseCode", AppLogger.LogType.INFO)
                    conn.disconnect()
                    lastError = Exception("HTTP $responseCode")
                }
            } catch (e: Exception) {
                AppLogger.log("✗ ${e.message}", AppLogger.LogType.INFO)
                lastError = e
            }
        }

        AppLogger.log("Alle URLs fehlgeschlagen!", AppLogger.LogType.ERROR)
        throw lastError ?: Exception("Download fehlgeschlagen")
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

private suspend fun fetchPhoneMedia(context: Context, folder: PhoneFolder): List<PhoneMediaFile> {
    return withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val likePath = when (folder) {
            PhoneFolder.CAMERA -> "DCIM/Camera/%"
            PhoneFolder.PICTURES -> "Pictures/%"
        }

        val selection = buildString {
            append("(")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE}=?")
            append(")")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?")
            }
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                likePath
            )
        } else {
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
        }

        val sort = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val out = mutableListOf<PhoneMediaFile>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sort)
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "unknown"
                    val size = c.getLong(sizeCol)
                    val date = c.getLong(dateCol)
                    val mime = c.getString(mimeCol) ?: "application/octet-stream"
                    val mediaType = c.getInt(typeCol)

                    val uri = ContentUris.withAppendedId(collection, id)

                    out.add(
                        PhoneMediaFile(
                            uri = uri,
                            name = name,
                            size = size,
                            dateAddedSec = date,
                            mime = mime,
                            isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                        )
                    )
                }
            }
        out
    }
}

private fun captureScreenshot(context: Context, view: SurfaceView) {
    try {
        val bitmap = createBitmap(view.width, view.height)
        PixelCopy.request(
            view.holder.surface,
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    saveBitmapToGallery(context, bitmap)
                }
            },
            Handler(Looper.getMainLooper())
        )
    } catch (_: Exception) {
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "IMG_$timestamp.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/GuideCamera"
            )
        }
    }

    val uri =
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
    }
}

@Composable
fun AboutDialogContent(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
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
                            text = "Open Source Android App für Guide Wärmebildkameras",
                            color = NightColors.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }

                HorizontalDivider(color = NightColors.surface)

                // Description
                Text(
                    text = "NoxVision ist eine leistungsstarke Alternative zur offiziellen Guide App, speziell entwickelt für das Guide TE211M Wärmebild-Monokular und kompatible TE-Serie Kameras.",
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
                    text = "Unterstützte Geräte",
                    color = NightColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Text(
                    text = "• Guide TE211M (primär getestet)\n• Andere Guide TE-Serie Kameras\n• Kameras mit RTSP auf 192.168.42.1:8554",
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
                Text("Schließen", color = NightColors.primary)
            }
        },
        containerColor = NightColors.background,
        textContentColor = NightColors.onSurface
    )
}

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
fun WelcomeDialog(onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    
    Dialog(
        onDismissRequest = {}, // Force user to go through guide
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
                        imageVector = when(step) {
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
                        text = when(step) {
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
                    when(step) {
                        0 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Ihr Begleiter für Wärmibildaufnahmen.\n\nDiese App bietet erweiterte Funktionen für Ihre Guide Kamera.",
                                    color = NightColors.onBackground,
                                    fontSize = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        1 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Damit die App funktioniert, müssen Sie die korrekte SSID und das Passwort Ihrer Kamera in den Einstellungen hinterlegen.",
                                    color = NightColors.onBackground,
                                    fontSize = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NightColors.background)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Standard-Werte:", color = NightColors.onSurface, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("SSID: Siehe Kamera-Aufkleber", color = NightColors.onBackground, fontSize = 14.sp)
                                        Text("Passwort: Oft 12345678", color = NightColors.onBackground, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                WelcomeFeatureItem(Icons.Filled.Camera, "Galerie", "Bilder & Videos direkt in der App verwalten")
                                WelcomeFeatureItem(Icons.Filled.Thermostat, "Messungen", "Temperaturpunkte und Emissionsgrad anpassen")
                                WelcomeFeatureItem(Icons.Filled.Wifi, "Auto-Connect", "Automatische Verbindung beim Start")
                                WelcomeFeatureItem(Icons.Filled.Videocam, "Aufnahme", "Videos mit Ton aufzeichnen")
                            }
                        }
                        3 -> {
                            Text(
                                text = "Sie können diese Einstellungen später jederzeit über das Zahnrad-Symbol ändern.\n\nViel Spaß mit NoxVision!",
                                color = NightColors.onBackground,
                                fontSize = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            Text("Zurück", color = NightColors.onSurface)
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
                                    .clip(androidx.compose.foundation.shape.CircleShape)
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

@Composable
fun WhatsNewItem(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "• $title", fontWeight = FontWeight.Bold, color = NightColors.onSurface, fontSize = 16.sp)
        Text(text = desc, color = NightColors.onBackground, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp, top=4.dp))
    }
}
