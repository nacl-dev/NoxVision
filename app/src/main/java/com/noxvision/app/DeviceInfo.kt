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
 * Focus actions for motorized focus cameras.
 */
enum class FocusAction(val value: Int) {
    AUTO(0),
    NEAR(1),
    FAR(2),
    STOP(3)
}

/**
 * Shutter modes for NUC calibration.
 */
enum class ShutterMode(val value: Int) {
    MANUAL(0),
    AUTO(1)
}

/**
 * Common emissivity presets for different materials.
 */
object EmissivityPresets {
    const val MATTE_SURFACE = 0.95f      // Human skin, fabric, paper
    const val SEMI_GLOSS = 0.80f         // Painted surfaces
    const val OXIDIZED_METAL = 0.60f     // Rusty iron, oxidized copper
    const val POLISHED_METAL = 0.30f     // Aluminum, stainless steel
    const val MIRROR = 0.05f             // Polished gold, silver
    
    val presets = mapOf(
        "Skin/Fabric" to MATTE_SURFACE,
        "Painted Surface" to SEMI_GLOSS,
        "Oxidized Metal" to OXIDIZED_METAL,
        "Polished Metal" to POLISHED_METAL,
        "Mirror/Reflective" to MIRROR
    )
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

/**
 * Get a user-friendly name for the camera series.
 */
fun DeviceInfo.getSeriesName(): String {
    return when {
        deviceName.contains("C640", ignoreCase = true) -> "Guide C640 Series"
        deviceName.contains("C400", ignoreCase = true) -> "Guide C400 Series"
        deviceName.contains("C800", ignoreCase = true) -> "Guide C800 Series"
        deviceName.contains("D400", ignoreCase = true) -> "Guide D400 Series"
        deviceName.contains("D384", ignoreCase = true) -> "Guide D384 Series"
        deviceName.contains("TE", ignoreCase = true) -> "Guide TE Series"
        deviceName.contains("PS", ignoreCase = true) -> "Guide PS Series"
        deviceName.contains("B", ignoreCase = true) -> "Guide B Series"
        else -> "Guide Camera"
    }
}
