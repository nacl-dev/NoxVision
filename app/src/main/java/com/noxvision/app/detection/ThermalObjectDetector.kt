package com.noxvision.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.noxvision.app.util.AppLogger
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
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

    fun detectObjects(bitmap: Bitmap): List<DetectedObject> {
        if (!isInitialized || interpreter == null) {
            return emptyList()
        }

        try {
            val startTime = System.currentTimeMillis()

            val processedBitmap = enhanceThermalImage(bitmap)
            val scaledBitmap = Bitmap.createScaledBitmap(processedBitmap, INPUT_SIZE, INPUT_SIZE, true)

            val inputBuffer = convertBitmapToFloatBuffer(scaledBitmap)

            val numClasses = labels.size
            val numElements = 4 + numClasses
            val numAnchors = 8400

            val outputArray = Array(1) { Array(numElements) { FloatArray(numAnchors) } }

            interpreter?.run(inputBuffer, outputArray)

            val allDetections = mutableListOf<DetectedObject>()

            for (i in 0 until numAnchors) {
                var maxScore = 0f
                var maxClassIndex = -1

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
                        val cx = outputArray[0][0][i] / INPUT_SIZE.toFloat()
                        val cy = outputArray[0][1][i] / INPUT_SIZE.toFloat()
                        val w = outputArray[0][2][i] / INPUT_SIZE.toFloat()
                        val h = outputArray[0][3][i] / INPUT_SIZE.toFloat()

                        val x1 = (cx - w / 2) * bitmap.width
                        val y1 = (cy - h / 2) * bitmap.height
                        val x2 = (cx + w / 2) * bitmap.width
                        val y2 = (cy + h / 2) * bitmap.height

                        val rect = RectF(x1, y1, x2, y2)

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

    private fun convertBitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }

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
                if (iou > iouThreshold) {
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

    private fun enhanceThermalImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(enhanced)
        val colorMatrix = android.graphics.ColorMatrix().apply {
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
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return enhanced
    }

    private fun estimateDistance(label: String, bbox: RectF, imageHeight: Int): Float? {
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
        } catch (e: Exception) {
        }
    }
}
