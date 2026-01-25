package com.noxvision.app

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
        private const val ENDPOINT_BASE_MEASURE_PARAM = "/api/v1/measure/getBaseMeasureParam"
        private const val ENDPOINT_RTSP_CONFIG = "/api/v1/measure/getRtspConfigInfo"
        private const val ENDPOINT_GPS_INFO = "/api/v1/measure/getGpsInfo"
        
        private const val ENDPOINT_GET_PALETTE = "/api/v1/measure/getPaletteId"
        private const val ENDPOINT_SET_PALETTE = "/api/v1/measure/setPaletteId"
        
        private const val ENDPOINT_GET_EMISSION = "/api/v1/measure/getEmission"
        private const val ENDPOINT_SET_EMISSION = "/api/v1/measure/setEmission"
        
        private const val ENDPOINT_GET_DISTANCE = "/api/v1/measure/getDistance"
        private const val ENDPOINT_SET_DISTANCE = "/api/v1/measure/setDistance"
        
        private const val ENDPOINT_GET_HUMIDITY = "/api/v1/measure/getHumidity"
        private const val ENDPOINT_SET_HUMIDITY = "/api/v1/measure/setHumidity"
        
        private const val ENDPOINT_GET_REFLECT_TEMP = "/api/v1/measure/getReflectTemperature"
        private const val ENDPOINT_SET_REFLECT_TEMP = "/api/v1/measure/setReflectTemperature"
        
        private const val ENDPOINT_GET_ATMOSPHERIC = "/api/v1/measure/getAtmosphericTransmittance"
        private const val ENDPOINT_SET_ATMOSPHERIC = "/api/v1/measure/setAtmosphericTransmittance"
        
        private const val ENDPOINT_GET_OPTICAL = "/api/v1/measure/getOpticalTransmittance"
        private const val ENDPOINT_SET_OPTICAL = "/api/v1/measure/setOpticalTransmittance"
        
        private const val ENDPOINT_SET_FOCUS = "/api/v1/measure/setFocus"
        private const val ENDPOINT_SHUTTER = "/api/v1/measure/shutter"
        private const val ENDPOINT_SET_RTSP_SERVER = "/api/v1/measure/setRtspServerEnable"
    }
    
    // Track if REST API is available (some older cameras may not support it)
    private var restApiAvailable: Boolean? = null
    
    /**
     * Check if the REST API is available on this camera.
     */
    suspend fun isRestApiAvailable(): Boolean {
        if (restApiAvailable != null) return restApiAvailable!!
        
        // Try to get device info to check if REST API works
        val result = getDeviceInfo()
        restApiAvailable = result != null
        return restApiAvailable!!
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
     * Perform a POST request with URL-encoded form data.
     */
    private suspend fun httpPostForm(endpoint: String, params: Map<String, Any>): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
                doOutput = true
            }
            
            // Build form data
            val formData = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(formData)
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
    
    /**
     * Get RTSP configuration (channels, ports, etc.)
     */
    suspend fun getRtspConfigInfo(): JSONObject? {
        return httpGet(ENDPOINT_RTSP_CONFIG).getOrNull()
    }
    
    /**
     * Get GPS information if available.
     */
    suspend fun getGpsInfo(): JSONObject? {
        return httpGet(ENDPOINT_GPS_INFO).getOrNull()
    }
    
    // ==================== Palette Control ====================
    
    /**
     * Get current palette ID.
     */
    suspend fun getPaletteId(): Int? {
        val result = httpGet(ENDPOINT_GET_PALETTE)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optInt("palette_id", -1).takeIf { it >= 0 } else null
        }
    }
    
    /**
     * Set palette by ID.
     * Common palette IDs:
     * - 0: Whitehot
     * - 2: Iron
     * - 4: Bluehot
     * - 5: Greenhot
     * - 9: Blackhot
     * - 11: Redhot
     */
    suspend fun setPaletteId(id: Int): Boolean {
        val result = httpPostJson(ENDPOINT_SET_PALETTE, mapOf("palette_id" to id.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }
    
    // ==================== Thermal Measurement Settings ====================
    
    /**
     * Get current emissivity value.
     */
    suspend fun getEmission(): Float? {
        val result = httpGet(ENDPOINT_GET_EMISSION)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optDouble("emission", -1.0).toFloat().takeIf { it > 0 } else null
        }
    }
    
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
     * Get current measurement distance.
     */
    suspend fun getDistance(): Float? {
        val result = httpGet(ENDPOINT_GET_DISTANCE)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optDouble("distance", -1.0).toFloat().takeIf { it >= 0 } else null
        }
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
     * Get current humidity setting.
     */
    suspend fun getHumidity(): Float? {
        val result = httpGet(ENDPOINT_GET_HUMIDITY)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optDouble("humidity", -1.0).toFloat().takeIf { it >= 0 } else null
        }
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
     * Get current reflected temperature setting.
     */
    suspend fun getReflectTemperature(): Float? {
        val result = httpGet(ENDPOINT_GET_REFLECT_TEMP)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optDouble("reflect_temp", Double.MIN_VALUE).toFloat()
                .takeIf { it != Float.MIN_VALUE } else null
        }
    }
    
    /**
     * Set reflected temperature in Celsius.
     */
    suspend fun setReflectTemperature(celsius: Float): Boolean {
        val result = httpPostJson(ENDPOINT_SET_REFLECT_TEMP, mapOf("reflect_temp" to celsius.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }
    
    /**
     * Get atmospheric transmittance.
     */
    suspend fun getAtmosphericTransmittance(): Float? {
        val result = httpGet(ENDPOINT_GET_ATMOSPHERIC)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optDouble("atmosphericTransmittance", -1.0).toFloat()
                .takeIf { it >= 0 } else null
        }
    }
    
    /**
     * Set atmospheric transmittance (0-1).
     */
    suspend fun setAtmosphericTransmittance(value: Float): Boolean {
        require(value in 0f..1f) { "Atmospheric transmittance must be between 0 and 1" }
        val result = httpPostJson(ENDPOINT_SET_ATMOSPHERIC, mapOf("atmosphericTransmittance" to value.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }
    
    /**
     * Get optical transmittance.
     */
    suspend fun getOpticalTransmittance(): Float? {
        val result = httpGet(ENDPOINT_GET_OPTICAL)
        return result.getOrNull()?.let { json ->
            if (isSuccess(json)) json.optDouble("opticalTransmittance", -1.0).toFloat()
                .takeIf { it >= 0 } else null
        }
    }
    
    /**
     * Set optical transmittance (0-1).
     */
    suspend fun setOpticalTransmittance(value: Float): Boolean {
        require(value in 0f..1f) { "Optical transmittance must be between 0 and 1" }
        val result = httpPostJson(ENDPOINT_SET_OPTICAL, mapOf("opticalTransmittance" to value.toString()))
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
    
    /**
     * Set focus action (for cameras with motorized focus).
     */
    suspend fun setFocus(action: FocusAction): Boolean {
        val result = httpPostJson(ENDPOINT_SET_FOCUS, mapOf("focus_action" to action.value.toString()))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }
    
    /**
     * Enable or disable RTSP server.
     */
    suspend fun setRtspServerEnable(enable: Boolean): Boolean {
        val result = httpPostJson(ENDPOINT_SET_RTSP_SERVER, mapOf("enable_server" to if (enable) "1" else "0"))
        return result.getOrNull()?.let { isSuccess(it) } ?: false
    }
}
