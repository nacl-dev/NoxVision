package com.noxvision.app

import android.util.Log
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
        private const val TAG = "CameraApiClient"
        private const val CONNECT_TIMEOUT = 3000
        private const val READ_TIMEOUT = 5000
        
        // API Endpoints
        private const val ENDPOINT_DEVICE_INFO = "/api/v1/measure/getDeviceInfo"

        private const val ENDPOINT_SET_EMISSION = "/api/v1/measure/setEmission"

        private const val ENDPOINT_SET_DISTANCE = "/api/v1/measure/setDistance"

        private const val ENDPOINT_SET_HUMIDITY = "/api/v1/measure/setHumidity"

        private const val ENDPOINT_SET_REFLECT_TEMP = "/api/v1/measure/setReflectTemperature"

        private const val ENDPOINT_SHUTTER = "/api/v1/measure/shutter"

        // Reticle / OSD endpoints (Guide camera REST API)
        private const val ENDPOINT_MISC_DEVICE_INFO = "/api/v1/misc/deviceinfo"
        private const val ENDPOINT_RETICLE_TYPE = "/api/v1/peripheral/dashtype"
        private const val ENDPOINT_RETICLE_COLOR = "/api/v1/peripheral/dashcolor"
        private const val ENDPOINT_RETICLE_BRIGHTNESS = "/api/v1/peripheral/dashlight"
        private const val ENDPOINT_RETICLE_COLOR_ALT = "/api/v1/peripheral/reticlecolor"
        private const val ENDPOINT_SIMULATE_RETICLE = "/api/v1/camera/simulatereticle"
        private const val ENDPOINT_OSD_STATE = "/api/v1/peripheral/osd_state"
        private const val ENDPOINT_OSD_GATE = "/api/v1/camera/osdgate"
        private const val ENDPOINT_RETICLE_LIST = "/api/v1/peripheral/dashlist"
    }

    // ==================== HTTP Helpers ====================

    private fun applyCommonHeaders(conn: HttpURLConnection) {
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Accept-Encoding", "gzip")
        conn.setRequestProperty("User-Agent", "okhttp/4.11.0")
    }

    private fun readResponseBody(conn: HttpURLConnection, success: Boolean): String {
        val stream = if (success) conn.inputStream else conn.errorStream ?: conn.inputStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            buildString {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    append(line)
                }
            }
        }
    }

    private fun formatHttpError(method: String, endpoint: String, code: Int, body: String): String {
        val detail = body.take(300).ifBlank { "(empty body)" }
        return "$method $endpoint failed: HTTP $code body=$detail"
    }

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
                applyCommonHeaders(this)
            }

            val responseCode = conn.responseCode
            val body = readResponseBody(conn, responseCode in 200..299)
            conn.disconnect()

            if (responseCode !in 200..299) {
                val error = formatHttpError("GET", endpoint, responseCode, body)
                Log.w(TAG, error)
                AppLogger.log(error, AppLogger.LogType.ERROR)
                return@withContext Result.failure(Exception(error))
            }

            val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()
            Result.success(json)
        } catch (e: Exception) {
            val error = "GET $endpoint failed: ${e.message}"
            Log.w(TAG, error, e)
            AppLogger.log(error, AppLogger.LogType.ERROR)
            Result.failure(e)
        }
    }
    
    /**
     * Perform a PUT request with JSON body (Guide camera peripheral APIs).
     */
    private suspend fun httpPutJson(endpoint: String, params: Map<String, Any>): Result<JSONObject> =
        writeJsonRequest(endpoint, params, "PUT")

    /**
     * Perform a POST request with JSON body.
     */
    private suspend fun httpPostJson(endpoint: String, params: Map<String, Any>): Result<JSONObject> =
        writeJsonRequest(endpoint, params, "POST")

    private suspend fun writeJsonRequest(
        endpoint: String,
        params: Map<String, Any>,
        method: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$endpoint")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = method
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                applyCommonHeaders(this)
                doOutput = true
            }

            val jsonBody = JSONObject(params).toString()
            Log.d(TAG, "$method $endpoint body=$jsonBody")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val body = readResponseBody(conn, responseCode in 200..299)
            conn.disconnect()

            if (responseCode !in 200..299) {
                val error = formatHttpError(method, endpoint, responseCode, body)
                Log.w(TAG, error)
                AppLogger.log(error, AppLogger.LogType.ERROR)
                return@withContext Result.failure(Exception(error))
            }

            Log.d(TAG, "$method $endpoint ok: ${body.take(200).ifBlank { "(empty)" }}")
            val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()
            Result.success(json)
        } catch (e: Exception) {
            val error = "$method $endpoint failed: ${e.message}"
            Log.w(TAG, error, e)
            AppLogger.log(error, AppLogger.LogType.ERROR)
            Result.failure(e)
        }
    }

    private fun parseValueResponse(json: JSONObject): String? {
        if (json.has("value") && !json.isNull("value")) {
            return json.opt("value")?.toString()?.takeIf { it.isNotBlank() }
        }
        return null
    }

    /**
     * Check if response indicates success.
     */
    private fun isSuccess(json: JSONObject): Boolean {
        return json.optString("retmsg", "") == "success"
    }

    private fun isPeripheralSuccess(json: JSONObject): Boolean {
        if (json.length() == 0) return true
        if (isSuccess(json)) return true
        if (json.has("value")) return true
        val retmsg = json.optString("retmsg", "")
        return retmsg.isBlank()
    }

    private suspend fun putValue(endpoint: String, value: String): Boolean {
        val result = httpPutJson(endpoint, mapOf("value" to value))
        return result.getOrNull()?.let { isPeripheralSuccess(it) } ?: false
    }

    private suspend fun tryPutValues(endpoint: String, values: List<String>): Boolean {
        for (value in values) {
            if (putValue(endpoint, value)) {
                Log.d(TAG, "tryPutValues $endpoint ok with value=$value")
                return true
            }
        }
        Log.w(TAG, "tryPutValues $endpoint failed for $values")
        return false
    }

    private suspend fun getValue(endpoint: String): String? {
        return httpGet(endpoint).getOrNull()?.let { json ->
            if (isSuccess(json) || json.has("value")) parseValueResponse(json) else null
        }
    }
    
    // ==================== Device Information ====================
    
    /**
     * Get device information including model, resolution, and capabilities.
     * Falls back to `/api/v1/misc/deviceinfo` when the measure endpoint is unavailable.
     */
    suspend fun getDeviceInfo(): DeviceInfo? {
        val fromMeasure = httpGet(ENDPOINT_DEVICE_INFO).getOrNull()?.let { parseMeasureDeviceInfo(it) }
        if (fromMeasure != null) return enrichWithMiscInfo(fromMeasure)
        return deviceInfoFromMiscOnly()
    }

    private fun parseMeasureDeviceInfo(json: JSONObject): DeviceInfo? {
        if (!isSuccess(json)) return null

        return try {
            DeviceInfo(
                deviceName = json.optString("device_name", "Unknown"),
                cameraName = json.optString("camera_name", "Unknown"),
                videoWidth = json.optInt("video_width", 256),
                videoHeight = json.optInt("video_height", 192),
                videoFps = json.optInt("video_fps", 25),
                measureGear = json.optInt("measure_gear", 0),
                cameraLens = json.optString("camera_lens", ""),
                measureRange = json.optString("measure_range", ""),
                projectCode = json.optString("project_code", json.optString("projectCode", "")),
                reservationCode = json.optString(
                    "device_identifier_code_reservation",
                    json.optString("reservation", "")
                )
            )
        } catch (e: Exception) {
            AppLogger.log("Failed to parse device info: ${e.message}", AppLogger.LogType.ERROR)
            null
        }
    }

    private suspend fun deviceInfoFromMiscOnly(): DeviceInfo? {
        val misc = getMiscDeviceInfo() ?: return null
        return DeviceInfo(
            deviceName = misc.projectCode.ifBlank { "Guide Camera" },
            cameraName = misc.projectCode,
            videoWidth = 256,
            videoHeight = 192,
            videoFps = 50,
            measureGear = 0,
            cameraLens = "",
            measureRange = "",
            projectCode = misc.projectCode,
            reservationCode = misc.reservationCode
        )
    }

    /**
     * Load project/reservation codes from /api/v1/misc/deviceinfo when missing.
     */
    suspend fun enrichWithMiscInfo(info: DeviceInfo): DeviceInfo {
        if (info.projectCode.isNotBlank() && info.reservationCode.isNotBlank()) return info
        val misc = getMiscDeviceInfo() ?: return info
        return info.copy(
            projectCode = info.projectCode.ifBlank { misc.projectCode },
            reservationCode = info.reservationCode.ifBlank { misc.reservationCode }
        )
    }

    suspend fun getMiscDeviceInfo(): MiscDeviceInfo? {
        val json = httpGet(ENDPOINT_MISC_DEVICE_INFO).getOrNull() ?: return null
        val hasKnownFields = json.has("project_code") || json.has("projectCode") ||
            json.has("value") || json.has("id")
        if (!isSuccess(json) && !hasKnownFields) return null

        val projectCode = when {
            json.has("project_code") -> json.optString("project_code")
            json.has("projectCode") -> json.optString("projectCode")
            json.has("value") -> json.optString("value")
            else -> ""
        }

        val reservationCode = when {
            json.has("device_identifier_code_reservation") ->
                json.optString("device_identifier_code_reservation")
            json.has("reservation") -> json.optString("reservation")
            json.has("id") -> {
                val id = json.optString("id", "")
                id.split("_").lastOrNull()?.takeIf { it.length == 4 } ?: ""
            }
            else -> ""
        }

        if (projectCode.isBlank() && reservationCode.isBlank()) return null
        return MiscDeviceInfo(projectCode = projectCode, reservationCode = reservationCode)
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

    // ==================== Device Reticle (Guide camera REST API) ====================

    /** Current reticle type index (1-based, from dashtype — TE211M dashlist: "1"…"5"). */
    suspend fun getReticleType(): Int? = getValue(ENDPOINT_RETICLE_TYPE)?.toIntOrNull()

    /** Set reticle type by index (PUT dashtype). Index is 1-based on real hardware. */
    suspend fun setReticleType(index: Int): Boolean {
        require(index in 1..5) { "Reticle type must be between 1 and 5" }
        return putValue(ENDPOINT_RETICLE_TYPE, index.toString())
    }

    /** Current reticle color (dashcolor). */
    suspend fun getReticleColor(): DeviceReticleColor? =
        DeviceReticleColor.fromApiValue(getValue(ENDPOINT_RETICLE_COLOR))

    /** Set reticle color (PUT dashcolor). */
    suspend fun setReticleColor(color: DeviceReticleColor): Boolean =
        putValue(ENDPOINT_RETICLE_COLOR, color.apiValue)

    /** Set reticle color via alternate endpoint (POST reticlecolor). */
    suspend fun setReticleColorAlt(color: DeviceReticleColor): Boolean {
        val result = httpPostJson(ENDPOINT_RETICLE_COLOR_ALT, mapOf("value" to color.apiValue))
        return result.getOrNull()?.let { isPeripheralSuccess(it) } ?: false
    }

    /** Current reticle brightness level (1-based, from dashlight). */
    suspend fun getReticleBrightness(): Int? = getValue(ENDPOINT_RETICLE_BRIGHTNESS)?.toIntOrNull()

    /** Set reticle brightness (1-based index, PUT dashlight). */
    suspend fun setReticleBrightness(level: Int): Boolean {
        require(level in 1..5) { "Brightness level must be between 1 and 5" }
        return putValue(ENDPOINT_RETICLE_BRIGHTNESS, level.toString())
    }

    /** Available reticle type indices from dashlist (may be empty if unsupported). */
    suspend fun getReticleTypeList(): List<Int> {
        val json = httpGet(ENDPOINT_RETICLE_LIST).getOrNull() ?: return emptyList()
        if (!isSuccess(json)) return emptyList()
        val list = json.optJSONArray("list") ?: json.optJSONArray("value") ?: return emptyList()
        return buildList {
            for (i in 0 until list.length()) {
                list.opt(i)?.toString()?.toIntOrNull()?.let { add(it) }
            }
        }
    }

    /** Mock reticle position (simulatereticle). Coordinates are pixel offsets as strings. */
    suspend fun getSimulateReticle(): SimulateReticle? {
        val json = httpGet(ENDPOINT_SIMULATE_RETICLE).getOrNull() ?: return null
        if (!isSuccess(json) && !json.has("x")) return null
        val x = json.optString("x", "").toIntOrNull() ?: return null
        val y = json.optString("y", "").toIntOrNull() ?: return null
        return SimulateReticle(x, y)
    }

    suspend fun setSimulateReticle(x: Int, y: Int): Boolean {
        val params = mapOf("x" to x.toString(), "y" to y.toString())
        val postResult = httpPostJson(ENDPOINT_SIMULATE_RETICLE, params)
        if (postResult.getOrNull()?.let { isPeripheralSuccess(it) } == true) return true
        val putResult = httpPutJson(ENDPOINT_SIMULATE_RETICLE, params)
        return putResult.getOrNull()?.let { isPeripheralSuccess(it) } ?: false
    }

    /** OSD overlay state string (GET osd_state). */
    suspend fun getOsdState(): String? = getValue(ENDPOINT_OSD_STATE)

    suspend fun setOsdState(value: String): Boolean = putValue(ENDPOINT_OSD_STATE, value)

    /**
     * OSD watermark gate (date/time/logo) — not the live reticle toggle.
     * Uses {"osd":"date"|"time"|"off"}, not {"value":"on"}.
     */
    suspend fun getOsdGate(): String? = getValue(ENDPOINT_OSD_GATE)

    suspend fun setOsdGate(value: String): Boolean = putValue(ENDPOINT_OSD_GATE, value)

    /** True if dashtype responds (fallback when projectCode is missing). */
    suspend fun probeReticleSupport(): Boolean = getReticleType() != null

    /**
     * Live device reticle uses dashtype/dashcolor/dashlight only.
     * There is no osdgate on/off for device reticle visibility.
     */
    suspend fun setReticleEnabled(enabled: Boolean): Boolean {
        if (enabled) {
            Log.w(TAG, "setReticleEnabled(true) requires type/color/brightness — use applyDeviceReticle()")
            return false
        }
        // Hide reticle by setting an out-of-range type index (device-specific fallback order).
        val hidden = tryPutValues(
            ENDPOINT_RETICLE_TYPE,
            listOf("255", "-1", "99")
        )
        Log.d(TAG, "setReticleEnabled(false) hidden=$hidden")
        return hidden
    }

    /** Apply type/color/brightness via dash* endpoints. */
    suspend fun applyDeviceReticleSettings(
        type: Int,
        color: DeviceReticleColor,
        brightness: Int
    ): Boolean {
        val typeOk = setReticleType(type)
        val colorOk = setReticleColor(color)
        val brightnessOk = setReticleBrightness(brightness)
        val applied = typeOk || colorOk || brightnessOk
        Log.d(
            TAG,
            "applyDeviceReticleSettings(type=$type, color=${color.apiValue}, brightness=$brightness) " +
                "typeOk=$typeOk colorOk=$colorOk brightnessOk=$brightnessOk"
        )
        return applied
    }

    suspend fun applyDeviceReticle(
        type: Int,
        color: DeviceReticleColor,
        brightness: Int
    ): Boolean {
        val typeOk = setReticleType(type)
        if (typeOk) {
            setReticleColor(color)
            setReticleBrightness(brightness)
        }
        Log.d(TAG, "applyDeviceReticle typeOk=$typeOk (type=$type)")
        return typeOk
    }
}

/** Position of the mock/simulated reticle overlay. */
data class SimulateReticle(val x: Int, val y: Int)

/** Project/reservation identifiers from misc deviceinfo. */
data class MiscDeviceInfo(
    val projectCode: String,
    val reservationCode: String
)
