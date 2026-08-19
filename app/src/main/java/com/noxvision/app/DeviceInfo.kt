package com.noxvision.app

/**
 * Data class representing device information from the camera.
 */
data class DeviceInfo(
    val deviceName: String,
    val cameraName: String,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoFps: Int,
    val measureGear: Int,
    val cameraLens: String,
    val measureRange: String
)

/**
 * Camera capabilities based on device model.
 */
data class CameraCapabilities(
    val hasFocus: Boolean,
    val hasGps: Boolean,
    val hasRadiometry: Boolean,
    val hasAudio: Boolean,
    val maxPaletteId: Int,
    val supportsRestApi: Boolean
)

/**
 * Shutter modes for NUC calibration.
 */
enum class ShutterMode(val value: Int) {
    MANUAL(0),
}

/**
 * Extension function to determine camera capabilities based on device name.
 */
fun DeviceInfo.getCapabilities(): CameraCapabilities {
    return when {
        // C-Series: High-end handheld with full features
        deviceName.contains("C640", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true,
            hasGps = true,
            hasRadiometry = true,
            hasAudio = true,
            maxPaletteId = 20,
            supportsRestApi = true
        )
        deviceName.contains("C400", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true,
            hasGps = true,
            hasRadiometry = true,
            hasAudio = true,
            maxPaletteId = 15,
            supportsRestApi = true
        )
        deviceName.contains("C800", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true,
            hasGps = true,
            hasRadiometry = true,
            hasAudio = true,
            maxPaletteId = 20,
            supportsRestApi = true
        )
        
        // D-Series: Industrial/OEM modules
        deviceName.contains("D400", ignoreCase = true) ||
        deviceName.contains("D384", ignoreCase = true) ||
        deviceName.contains("D192", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true,
            hasGps = false,
            hasRadiometry = true,
            hasAudio = false,
            maxPaletteId = 15,
            supportsRestApi = true
        )
        deviceName.contains("D160", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false,
            hasGps = false,
            hasRadiometry = true,
            hasAudio = false,
            maxPaletteId = 10,
            supportsRestApi = true
        )
        
        // B-Series: Budget modules
        deviceName.contains("B320", ignoreCase = true) ||
        deviceName.contains("B256", ignoreCase = true) ||
        deviceName.contains("B160", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false,
            hasGps = false,
            hasRadiometry = true,
            hasAudio = false,
            maxPaletteId = 10,
            supportsRestApi = true
        )
        
        // TE-Series: Monoculars (like TE211M)
        deviceName.contains("TE", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false,
            hasGps = false,
            hasRadiometry = true,
            hasAudio = false,
            maxPaletteId = 10,
            supportsRestApi = true
        )
        
        // PS-Series: Smartphone attachments
        deviceName.contains("PS", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false,
            hasGps = false,
            hasRadiometry = true,
            hasAudio = false,
            maxPaletteId = 10,
            supportsRestApi = true
        )
        
        // TB-Series: High-end monoculars/thermal binoculars
        deviceName.startsWith("TB", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true,
            hasGps = true,
            hasRadiometry = true,
            hasAudio = true,
            maxPaletteId = 15,
            supportsRestApi = true
        )
        
        // Unknown device - use conservative defaults
        else -> CameraCapabilities(
            hasFocus = false,
            hasGps = false,
            hasRadiometry = false,
            hasAudio = false,
            maxPaletteId = 10,
            supportsRestApi = false
        )
    }
}

