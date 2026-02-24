package com.noxvision.app.detection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Random

class ThermalObjectDetectorTest {

    @Test
    fun benchmarkConversion() {
        val inputSize = 640
        val totalPixels = inputSize * inputSize
        val intValues = IntArray(totalPixels)
        val floatValues = FloatArray(totalPixels * 3)
        val random = Random()

        // Fill with random data
        for (i in 0 until totalPixels) {
            intValues[i] = random.nextInt()
        }

        // Warmup
        for (i in 0 until 10) {
            ThermalObjectDetector.convertPixelsToFloatBuffer(intValues, floatValues, totalPixels)
        }

        val iterations = 50
        val startTime = System.nanoTime()
        for (i in 0 until iterations) {
            ThermalObjectDetector.convertPixelsToFloatBuffer(intValues, floatValues, totalPixels)
        }
        val endTime = System.nanoTime()

        val avgTimeMs = (endTime - startTime) / iterations / 1_000_000.0
        println("Average conversion time: $avgTimeMs ms")

        // Verify correctness for a few pixels
        val pixelIndex = 0
        val pixelValue = intValues[pixelIndex]
        val expectedR = ((pixelValue shr 16 and 0xFF) / 255.0f)
        val expectedG = ((pixelValue shr 8 and 0xFF) / 255.0f)
        val expectedB = ((pixelValue and 0xFF) / 255.0f)

        assertEquals(expectedR, floatValues[0], 0.0001f)
        assertEquals(expectedG, floatValues[1], 0.0001f)
        assertEquals(expectedB, floatValues[2], 0.0001f)
    }
}
