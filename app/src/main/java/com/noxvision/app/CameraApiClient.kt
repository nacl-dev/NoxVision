package com.noxvision.app

import com.noxvision.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * REST API client for Guide Sensmart thermal cameras.
 * 
 * This client provides access to the /api/v1/measure/ endpoints
 * available on Guide thermal cameras for device info, palette control,
 * thermal measurement settings, and camera control.
 * 
 * Usage:
 * ```
 * val client = CameraApiClient("http://192.168.42.1")
 * val deviceInfo = client.getDeviceInfo()
 * client.setPaletteId(2)
 * client.setEmission(0.95f)
 * ```
 */
class CameraApiClient(private val baseUrl: String) {
    
    companion object {
        private const val CONNECT_TIMEOUT = 3000
        private const val READ_TIMEOUT = 5000
        
        // API Endpoints
        private const val ENDPOINT_DEVICE_INFO = "/api/v1/measure/getDeviceInfo"

        private const val ENDPOINT_SET_EMISSION = "/api/v1/measure/setEmission"

        private const val ENDPOINT_SET_DISTANCE = "/api/v1/measure/setDistance"

        private const val ENDPOINT_SET_HUMIDITY = "/api/v1/measure/setHumidity"

        private const val ENDPOINT_SET_REFLECT_TEMP = "/api/v1/measure/setReflectTemperature"

        private const val ENDPOINT_SHUTTER = "/api/v1/measure/shutter"
    }

    // ==================== HTTP Helpers ====================
    
    /**
     * Perform a GET request and parse JSON response.
     */
    private suspend fun httpGet(endpoint: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                conn.disconnect()
                return@withContext Result.failure(Exception("HTTP $responseCode"))
            }
            
            val response = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
            }
            conn.disconnect()
            
            val json = JSONObject(response.toString())
            Result.success(json)
        } catch (e: Exception) {
            AppLogger.log("API GET $endpoint failed: ${e.message}", AppLogger.LogType.ERROR)
            Result.failure(e)
        }
    }
    
    /**
     * Perform a POST request with JSON body.
     */
    private suspend fun httpPostJson(endpoint: String, params: Map<String, Any>): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
            }
            
            // Build JSON body
            val jsonBody = JSONObject(params).toString()
            
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                conn.disconnect()
                return@withContext Result.failure(Exception("HTTP $responseCode"))
            }
            
            val response = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
            }
            conn.disconnect()
            
            val json = JSONObject(response.toString())
            Result.success(json)
        } catch (e: Exception) {
            AppLogger.log("API POST $endpoint failed: ${e.message}", AppLogger.LogType.ERROR)
            Result.failure(e)
        }
    }

    /**
     * Check if response indicates success.
     */
    private fun isSuccess(json: JSONObject): Boolean {
        return json.optString("retmsg", "") == "success"
    }
    
    // ==================== Device Information ====================
    
    /**
     * Get device information including model, resolution, and capabilities.
     */
    suspend fun getDeviceInfo(): DeviceInfo? {
        val result = httpGet(ENDPOINT_DEVICE_INFO)
        return result.getOrNull()?.let { json ->
            if (!isSuccess(json)) return null
            
            try {
                DeviceInfo(
                    deviceName = json.optString("device_name", "Unknown"),
                    cameraName = json.optString("camera_name", "Unknown"),
                    videoWidth = json.optInt("video_width", 256),
                    videoHeight = json.optInt("video_height", 192),
                    videoFps = json.optInt("video_fps", 25),
                    measureGear = json.optInt("measure_gear", 0),
                    cameraLens = json.optString("camera_lens", ""),
                    measureRange = json.optString("measure_range", "")
                )
            } catch (e: Exception) {
                AppLogger.log("Failed to parse device info: ${e.message}", AppLogger.LogType.ERROR)
                null
            }
        }
    }

    // ==================== Palette Control ====================

    // ==================== Thermal Measurement Settings ====================

    /**
     * Set emissivity (0.01 - 1.0).
     * @param value Emissivity value
     * @param index Measurement zone index (0 for global)
     */
    suspend fun setEmission(value: Float, index: Int = 0): Boolean {
        require(value in 0.01f..1.0f) { "Emissivity must be between 0.01 and 1.0" }
        val result = httpPostJson(ENDPOINT_SET_EMISSION, mapOf(
            "emission" to value.toString(),
            "index" to index.toString()
        ))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }

    /**
     * Set measurement distance in meters.
     */
    suspend fun setDistance(meters: Float): Boolean {
        require(meters >= 0) { "Distance must be non-negative" }
        val result = httpPostJson(ENDPOINT_SET_DISTANCE, mapOf("distance" to meters.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }

    /**
     * Set humidity percentage (0-100).
     */
    suspend fun setHumidity(percent: Float): Boolean {
        require(percent in 0f..100f) { "Humidity must be between 0 and 100" }
        val result = httpPostJson(ENDPOINT_SET_HUMIDITY, mapOf("humidity" to percent.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }

    /**
     * Set reflected temperature in Celsius.
     */
    suspend fun setReflectTemperature(celsius: Float): Boolean {
        val result = httpPostJson(ENDPOINT_SET_REFLECT_TEMP, mapOf("reflect_temp" to celsius.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }

    // ==================== Camera Control ====================
    
    /**
     * Trigger shutter (NUC calibration).
     * @param mode Shutter mode (0=manual, 1=auto)
     */
    suspend fun triggerShutter(mode: ShutterMode = ShutterMode.MANUAL): Boolean {
        val result = httpPostJson(ENDPOINT_SHUTTER, mapOf("shutter_mode" to mode.value.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }

}
