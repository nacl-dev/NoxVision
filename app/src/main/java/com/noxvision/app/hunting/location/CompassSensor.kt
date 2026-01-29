package com.noxvision.app.hunting.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CompassSensor(context: Context) {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val magnetometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isAvailable: Boolean
        get() = accelerometer != null && magnetometer != null

    fun getCompassUpdates(): Flow<CompassData> = callbackFlow {
        if (!isAvailable) {
            close()
            return@callbackFlow
        }

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPassFilter(event.values.clone(), gravity)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPassFilter(event.values.clone(), geomagnetic)
                        hasMagnetic = true
                    }
                }

                if (hasGravity && hasMagnetic) {
                    val rotationMatrix = FloatArray(9)
                    val inclinationMatrix = FloatArray(9)

                    if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        val azimuthRadians = orientation[0]
                        var azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
                        azimuthDegrees = (azimuthDegrees + 360) % 360

                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                        trySend(
                            CompassData(
                                azimuth = azimuthDegrees,
                                pitch = pitch,
                                roll = roll,
                                direction = getDirectionFromAzimuth(azimuthDegrees)
                            )
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun lowPassFilter(input: FloatArray, output: FloatArray): FloatArray {
        val alpha = 0.15f
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
        return output
    }

    private fun getDirectionFromAzimuth(azimuth: Float): String {
        return when {
            azimuth < 22.5 -> "N"
            azimuth < 67.5 -> "NO"
            azimuth < 112.5 -> "O"
            azimuth < 157.5 -> "SO"
            azimuth < 202.5 -> "S"
            azimuth < 247.5 -> "SW"
            azimuth < 292.5 -> "W"
            azimuth < 337.5 -> "NW"
            else -> "N"
        }
    }

    companion object {
        fun calculateRelativeBearing(compassAzimuth: Float, targetBearing: Float): Float {
            var relative = targetBearing - compassAzimuth
            if (relative < 0) relative += 360
            if (relative > 180) relative -= 360
            return relative
        }
    }
}

data class CompassData(
    val azimuth: Float,
    val pitch: Float,
    val roll: Float,
    val direction: String
)
