package com.noxvision.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.noxvision.app.util.AppLogger
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.FileChannel

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
    val boundingBox: RectF,
    val estimatedDistance: Float? = null
)

class ThermalObjectDetector(context: Context) {
    private var interpreter: org.tensorflow.lite.Interpreter? = null
    private val labels = mutableListOf<String>()
    private var isInitialized = false

    private val inputSize = 640
    private val focalLengthPixels = 3350f
    private val numAnchors = 8400
    private val minConfidenceThreshold = 0.45f
    private val nmsIouThreshold = 0.45f

    // Reusable buffers to avoid allocations per frame
    private var imgData: ByteBuffer? = null
    private var floatBuffer: FloatBuffer? = null
    private val intValues = IntArray(inputSize * inputSize)
    private val floatValues = FloatArray(inputSize * inputSize * 3)
    private val anchorMaxScores = FloatArray(numAnchors)
    private val anchorMaxClassIndices = IntArray(numAnchors)
    private var outputArray: Array<Array<FloatArray>>? = null
    // Optimization: Removed intermediate enhancedBitmap/Canvas to save memory and draw calls
    private var scaledBitmap: Bitmap? = null
    private var scaledCanvas: Canvas? = null

    private val inputRect = Rect(0, 0, inputSize, inputSize)
    private val enhancementColorMatrix = android.graphics.ColorMatrix().apply {
        val contrast = 1.3f
        val translate = (1f - contrast) * 128f
        set(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        setSaturation(1.2f)
    }

    private val enhancementPaint = Paint().apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(enhancementColorMatrix)
        isFilterBitmap = true // Combine scaling and filtering
    }

    init {
        try {
            val modelExists = try {
                context.assets.open("detect.tflite").use { true }
            } catch (e: Exception) {
                false
            }

            if (modelExists) {
                val model = loadModelFile(context)
                val options = org.tensorflow.lite.Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = org.tensorflow.lite.Interpreter(model, options)

                loadLabels(context)

                // Initialize buffers
                initBuffers()

                val numClasses = labels.size
                val numElements = 4 + numClasses
                outputArray = Array(1) { Array(numElements) { FloatArray(numAnchors) } }

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

    private fun initBuffers() {
        imgData = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        imgData?.order(ByteOrder.nativeOrder())
        floatBuffer = imgData?.asFloatBuffer()
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val fileDescriptor = context.assets.openFd("detect.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context) {
        try {
            context.assets.open("labelmap.txt").bufferedReader().useLines { lines ->
                labels.addAll(lines.filter { it.isNotBlank() })
            }
        } catch (e: Exception) {
            labels.addAll(listOf("car", "cat", "dog", "Person"))
        }
    }

    @Synchronized
    fun detectObjects(bitmap: Bitmap): List<DetectedObject> {
        if (!isInitialized || interpreter == null) {
            return emptyList()
        }

        try {
            val startTime = System.currentTimeMillis()

            // Optimized: Combine enhancement and scaling into a single step
            if (scaledBitmap == null) {
                scaledBitmap = createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
                scaledCanvas = Canvas(scaledBitmap!!)
            }
            val currentScaled = scaledBitmap!!

            // Draw source bitmap directly to scaled canvas with enhancement paint (scaling + color filter)
            scaledCanvas!!.drawBitmap(bitmap, null, inputRect, enhancementPaint)

            // 3. Convert to Float Buffer
            if (imgData == null) {
                 // Should be initialized in init, but safe check
                 initBuffers()
            }
            convertBitmapToFloatBuffer(currentScaled)

            val numClasses = labels.size
            // numAnchors is class property

            val currentOutput = outputArray ?: return emptyList()

            interpreter?.run(imgData, currentOutput)

            val allDetections = mutableListOf<DetectedObject>()

            // Optimized: Inverted loop for better cache locality and fewer array lookups
            // Reset max arrays
            anchorMaxScores.fill(0f)
            anchorMaxClassIndices.fill(-1)

            val outputChannels = currentOutput[0]

            // Find max class for each anchor
            for (c in 0 until numClasses) {
                val classProps = outputChannels[4 + c]
                for (i in 0 until numAnchors) {
                    val score = classProps[i]
                    if (score > anchorMaxScores[i]) {
                        anchorMaxScores[i] = score
                        anchorMaxClassIndices[i] = c
                    }
                }
            }

            // Collect detections
            for (i in 0 until numAnchors) {
                val maxScore = anchorMaxScores[i]
                if (maxScore < minConfidenceThreshold) continue

                val maxClassIndex = anchorMaxClassIndices[i]
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
                        val cx = outputChannels[0][i] / inputSize.toFloat()
                        val cy = outputChannels[1][i] / inputSize.toFloat()
                        val w = outputChannels[2][i] / inputSize.toFloat()
                        val h = outputChannels[3][i] / inputSize.toFloat()

                        val x1 = (cx - w / 2) * bitmap.width
                        val y1 = (cy - h / 2) * bitmap.height
                        val x2 = (cx + w / 2) * bitmap.width
                        val y2 = (cy + h / 2) * bitmap.height

                        val rect = RectF(x1, y1, x2, y2)

                        if (rect.width() > 50 && rect.height() > 50) {
                            val distance = estimateDistance(rawLabel, rect)
                            allDetections.add(DetectedObject(displayLabel, maxScore, rect, distance))
                        }
                    }
                }
            }

            val finalDetections = applyNMS(allDetections)
            val topDetections = finalDetections.sortedByDescending { it.confidence }.take(5)

            val elapsed = System.currentTimeMillis() - startTime
            if (topDetections.isNotEmpty()) {
                AppLogger.log("${topDetections.size} objects (${elapsed}ms) - Top: ${topDetections[0].label}", AppLogger.LogType.INFO)
            }

            // Do not recycle bitmaps here, they are reused

            return topDetections

        } catch (e: Exception) {
            AppLogger.log("Detection Error: ${e.message}", AppLogger.LogType.ERROR)
            return emptyList()
        }
    }

    private fun convertBitmapToFloatBuffer(bitmap: Bitmap) {
        floatBuffer?.rewind() // Important!

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        var floatIdx = 0
        val inv255 = 1.0f / 255.0f

        // Flattened loop for performance
        val totalPixels = inputSize * inputSize
        for (i in 0 until totalPixels) {
            val value = intValues[pixel++]
            // Use multiplication instead of division for performance
            floatValues[floatIdx++] = ((value shr 16 and 0xFF) * inv255)
            floatValues[floatIdx++] = ((value shr 8 and 0xFF) * inv255)
            floatValues[floatIdx++] = ((value and 0xFF) * inv255)
        }

        floatBuffer?.put(floatValues)
    }

    private fun applyNMS(detections: List<DetectedObject>): List<DetectedObject> {
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
                if (iou > nmsIouThreshold) {
                    suppressed[j] = true
                }
            }
        }
        return keep
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
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

    private fun estimateDistance(label: String, bbox: RectF): Float? {
        val objInfo = KNOWN_OBJECTS[label] ?: return null
        val objectHeightPx = bbox.height()
        if (objectHeightPx < 10) return null
        val distanceMeters = (objInfo.heightMeters * focalLengthPixels) / objectHeightPx
        return if (distanceMeters in 1f..100f) distanceMeters else null
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            outputArray = null
            // enhancedBitmap cleanup removed
            scaledBitmap?.recycle()
            scaledBitmap = null
            scaledCanvas = null
        } catch (_: Exception) {
        }
    }
}
