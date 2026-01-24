package com.noxvision.app

import android.content.Context

/**
 * Manages camera connection settings with SharedPreferences persistence.
 */
object CameraSettings {
    private const val PREFS_NAME = "noxvision_settings"
    private const val KEY_CAMERA_IP = "camera_ip"
    private const val DEFAULT_IP = "192.168.42.1"
    
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
     * Build the HTTP base URL from the given IP address.
     */
    fun getBaseUrl(ip: String): String {
        return "http://$ip"
    }
    
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
}
