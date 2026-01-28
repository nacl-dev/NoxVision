package com.noxvision.app

import android.content.Context

/**
 * Manages camera connection and thermal measurement settings with SharedPreferences persistence.
 */
object CameraSettings {
    private const val PREFS_NAME = "noxvision_settings"
    
    // Connection settings
    private const val KEY_CAMERA_IP = "camera_ip"
    private const val DEFAULT_IP = "192.168.42.1"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"
    
    // Thermal measurement settings
    private const val KEY_EMISSIVITY = "emissivity"
    private const val KEY_DISTANCE = "distance"
    private const val KEY_HUMIDITY = "humidity"
    private const val KEY_REFLECT_TEMP = "reflect_temp"
    
    // Device info cache
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_CAMERA_NAME = "camera_name"
    private const val KEY_VIDEO_WIDTH = "video_width"
    private const val KEY_VIDEO_HEIGHT = "video_height"
    
    // WiFi and Connection settings
    private const val KEY_WIFI_SSID = "wifi_ssid"
    private const val KEY_WIFI_PASSWORD = "wifi_password"
    private const val KEY_HTTP_PORT = "http_port"
    private const val KEY_AUTOCONNECT_ENABLED = "autoconnect_enabled"
    
    // Default thermal values
    private const val DEFAULT_EMISSIVITY = 0.95f
    private const val DEFAULT_DISTANCE = 1.0f
    private const val DEFAULT_HUMIDITY = 50.0f
    private const val DEFAULT_REFLECT_TEMP = 23.0f
    
    // Default WiFi/Port values
    private const val DEFAULT_WIFI_SSID = "TE Mini-089F"
    private const val DEFAULT_WIFI_PASSWORD = "12345678"
    private const val DEFAULT_HTTP_PORT = 80
    private const val DEFAULT_AUTOCONNECT_ENABLED = true
    
    // ==================== Connection Settings ====================
    
    /**
     * Get the saved camera IP address or return the default.
     */
    fun getCameraIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CAMERA_IP, DEFAULT_IP) ?: DEFAULT_IP
    }
    
    /**
     * Save the camera IP address.
     */
    fun setCameraIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CAMERA_IP, ip).apply()
    }
    
    /**
     * Build the RTSP URL from the given IP address.
     */
    fun getRtspUrl(ip: String): String {
        return "rtsp://$ip:8554/video"
    }
    
    /**
     * Build the HTTP base URL from the given IP address and port.
     */
    fun getBaseUrl(context: Context, ip: String): String {
        val port = getHttpPort(context)
        return if (port == 80) "http://$ip" else "http://$ip:$port"
    }
    
    /**
     * Get the saved WiFi SSID.
     */
    fun getWifiSsid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WIFI_SSID, DEFAULT_WIFI_SSID) ?: DEFAULT_WIFI_SSID
    }
    
    /**
     * Set the WiFi SSID.
     */
    fun setWifiSsid(context: Context, ssid: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WIFI_SSID, ssid).apply()
    }
    
    /**
     * Get the saved WiFi Password.
     */
    fun getWifiPassword(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WIFI_PASSWORD, DEFAULT_WIFI_PASSWORD) ?: DEFAULT_WIFI_PASSWORD
    }
    
    /**
     * Set the WiFi Password.
     */
    fun setWifiPassword(context: Context, password: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WIFI_PASSWORD, password).apply()
    }
    
    /**
     * Get the saved HTTP Port.
     */
    fun getHttpPort(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HTTP_PORT, DEFAULT_HTTP_PORT)
    }
    
    /**
     * Set the HTTP Port.
     */
    fun setHttpPort(context: Context, port: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_HTTP_PORT, port).apply()
    }
    
    /**
     * Check if WiFi Auto-Connect is enabled.
     */
    fun isAutoConnectEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTOCONNECT_ENABLED, DEFAULT_AUTOCONNECT_ENABLED)
    }
    
    /**
     * Set WiFi Auto-Connect enabled state.
     */
    fun setAutoConnectEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTOCONNECT_ENABLED, enabled).apply()
    }
    
    /**
     * Get the default SSID.
     */
    fun getDefaultSsid(): String = DEFAULT_WIFI_SSID
    
    /**
     * Get the default port.
     */
    fun getDefaultPort(): Int = DEFAULT_HTTP_PORT
    
    /**
     * Validate IP address format using simple regex.
     * Returns true if the IP format is valid (basic check for x.x.x.x pattern).
     */
    fun isValidIp(ip: String): Boolean {
        val ipPattern = Regex(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        return ipPattern.matches(ip)
    }
    
    /**
     * Get the default IP address.
     */
    fun getDefaultIp(): String = DEFAULT_IP
    
    // ==================== Thermal Measurement Settings ====================
    
    /**
     * Get saved emissivity value (0.01 - 1.0).
     */
    fun getEmissivity(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_EMISSIVITY, DEFAULT_EMISSIVITY)
    }
    
    /**
     * Save emissivity value.
     */
    fun setEmissivity(context: Context, value: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_EMISSIVITY, value.coerceIn(0.01f, 1.0f)).apply()
    }
    
    /**
     * Get saved measurement distance in meters.
     */
    fun getDistance(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_DISTANCE, DEFAULT_DISTANCE)
    }
    
    /**
     * Save measurement distance.
     */
    fun setDistance(context: Context, meters: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_DISTANCE, meters.coerceAtLeast(0f)).apply()
    }
    
    /**
     * Get saved humidity percentage (0-100).
     */
    fun getHumidity(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_HUMIDITY, DEFAULT_HUMIDITY)
    }
    
    /**
     * Save humidity percentage.
     */
    fun setHumidity(context: Context, percent: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_HUMIDITY, percent.coerceIn(0f, 100f)).apply()
    }
    
    /**
     * Get saved reflected temperature in Celsius.
     */
    fun getReflectTemperature(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_REFLECT_TEMP, DEFAULT_REFLECT_TEMP)
    }
    
    /**
     * Save reflected temperature.
     */
    fun setReflectTemperature(context: Context, celsius: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_REFLECT_TEMP, celsius).apply()
    }
    
    /**
     * Get default emissivity value.
     */
    fun getDefaultEmissivity(): Float = DEFAULT_EMISSIVITY
    
    /**
     * Get default distance value.
     */
    fun getDefaultDistance(): Float = DEFAULT_DISTANCE
    
    /**
     * Get default humidity value.
     */
    fun getDefaultHumidity(): Float = DEFAULT_HUMIDITY
    
    /**
     * Get default reflected temperature.
     */
    fun getDefaultReflectTemperature(): Float = DEFAULT_REFLECT_TEMP
    
    // ==================== Device Info Cache ====================
    
    /**
     * Save device info for offline access.
     */
    fun saveDeviceInfo(context: Context, deviceInfo: DeviceInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DEVICE_NAME, deviceInfo.deviceName)
            .putString(KEY_CAMERA_NAME, deviceInfo.cameraName)
            .putInt(KEY_VIDEO_WIDTH, deviceInfo.videoWidth)
            .putInt(KEY_VIDEO_HEIGHT, deviceInfo.videoHeight)
            .apply()
    }
    
    /**
     * Get cached device info (may be null if never connected).
     */
    fun getCachedDeviceInfo(context: Context): DeviceInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceName = prefs.getString(KEY_DEVICE_NAME, null) ?: return null
        
        return DeviceInfo(
            deviceName = deviceName,
            cameraName = prefs.getString(KEY_CAMERA_NAME, "") ?: "",
            videoWidth = prefs.getInt(KEY_VIDEO_WIDTH, 256),
            videoHeight = prefs.getInt(KEY_VIDEO_HEIGHT, 192),
            videoFps = 25,
            measureGear = 0,
            cameraLens = "",
            measureRange = ""
        )
    }
    
    /**
     * Clear cached device info.
     */
    fun clearDeviceInfo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_DEVICE_NAME)
            .remove(KEY_CAMERA_NAME)
            .remove(KEY_VIDEO_WIDTH)
            .remove(KEY_VIDEO_HEIGHT)
            .apply()
    }

    /**
     * Check if this is the first run of the app.
     */
    fun isFirstRun(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_RUN, true)
    }

    /**
     * Mark the first run guide as completed.
     */
    fun setFirstRunCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    /**
     * Get the last version code that was run.
     */
    fun getLastVersionCode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_VERSION_CODE, -1)
    }

    /**
     * Save the current version code.
     */
    fun setLastVersionCode(context: Context, versionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_VERSION_CODE, versionCode).apply()
    }
}
