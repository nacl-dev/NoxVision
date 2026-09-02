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
    val measureRange: String,
    val projectCode: String = "",
    val reservationCode: String = ""
)

/**
 * Device-side reticle capabilities detected from camera project code and reservation ID.
 */
data class ReticleCapabilities(
    val hasDeviceReticle: Boolean,
    val hasMockReticle: Boolean
)

/**
 * Camera capabilities based on device model and project code.
 */
data class CameraCapabilities(
    val hasFocus: Boolean,
    val hasGps: Boolean,
    val hasRadiometry: Boolean,
    val hasAudio: Boolean,
    val maxPaletteId: Int,
    val supportsRestApi: Boolean,
    val reticle: ReticleCapabilities = ReticleCapabilities(false, false)
)

/**
 * Device reticle colors (Guide camera REST API color names).
 */
enum class DeviceReticleColor(val apiValue: String) {
    BLACK("black"),
    WHITE("white"),
    YELLOW("yellow"),
    GREEN("green"),
    BLUE("blue"),
    RED("red");

    companion object {
        fun fromApiValue(value: String?): DeviceReticleColor? {
            return entries.find { it.apiValue.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * Shutter modes for NUC calibration.
 */
enum class ShutterMode(val value: Int) {
    MANUAL(0),
}

private val FULL_RETICLE_BASES = setOf(
    "ZG04", "ZG18", "ZG19", "ZG25", "ZG30", "ZG38", "ZG45", "ZG51", "ZG54",
    "ZG59", "ZG61", "ZG63", "ZG66", "ZG67", "ZG68", "ZG69", "ZG70"
)

private val MOCK_RETICLE_BASES = setOf("ZG34", "ZG59", "ZG61")

private val CONDITIONAL_RETICLE_BASES = setOf("ZG40")

private val CONDITIONAL_RETICLE_RESERVATIONS = setOf("0001", "0004")

/**
 * Extract the ZG project base from a full project code (e.g. ZG67A-01-HW-0000-0001 → ZG67A).
 */
fun projectCodeBase(projectCode: String): String {
    if (projectCode.isBlank()) return ""
    return projectCode.substringBefore("-").uppercase()
}

/**
 * Resolve reticle support from Guide internal project code and reservation ID.
 */
fun resolveReticleCapabilities(
    projectCode: String,
    reservationCode: String = "",
    deviceName: String = ""
): ReticleCapabilities {
    val base = projectCodeBase(projectCode)

    val hasMock = base.isNotBlank() && MOCK_RETICLE_BASES.any { base.startsWith(it) }

    val hasFull = base.isNotBlank() && (
        FULL_RETICLE_BASES.any { base.startsWith(it) } ||
            (CONDITIONAL_RETICLE_BASES.any { base.startsWith(it) } &&
                reservationCode in CONDITIONAL_RETICLE_RESERVATIONS)
        )

    if (hasFull) {
        return ReticleCapabilities(hasDeviceReticle = true, hasMockReticle = hasMock)
    }

    // Fallback when project_code is missing from deviceinfo.
    val teWithReticle = deviceName.contains("TE411", ignoreCase = true) ||
        deviceName.contains("TE421", ignoreCase = true)

    return ReticleCapabilities(
        hasDeviceReticle = teWithReticle,
        hasMockReticle = false
    )
}

/**
 * Models confirmed without hardware reticle (e.g. TE211M observation monocular).
 */
fun DeviceInfo.isKnownReticleUnsupported(): Boolean {
    if (deviceName.contains("TE211M", ignoreCase = true)) return true
    val base = projectCodeBase(projectCode)
    if (base.startsWith("ZG40") && reservationCode == "0000") return true
    if (projectCode.equals("ZG40C", ignoreCase = true) &&
        (reservationCode.isBlank() || reservationCode == "0000")
    ) {
        return true
    }
    return false
}

/**
 * Extension function to determine camera capabilities based on device name and project code.
 */
fun DeviceInfo.getCapabilities(): CameraCapabilities {
    val reticle = resolveReticleCapabilities(projectCode, reservationCode, deviceName)

    val baseCapabilities = when {
        deviceName.contains("C640", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true, hasGps = true, hasRadiometry = true, hasAudio = true,
            maxPaletteId = 20, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("C400", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true, hasGps = true, hasRadiometry = true, hasAudio = true,
            maxPaletteId = 15, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("C800", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true, hasGps = true, hasRadiometry = true, hasAudio = true,
            maxPaletteId = 20, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("D400", ignoreCase = true) ||
        deviceName.contains("D384", ignoreCase = true) ||
        deviceName.contains("D192", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true, hasGps = false, hasRadiometry = true, hasAudio = false,
            maxPaletteId = 15, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("D160", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false, hasGps = false, hasRadiometry = true, hasAudio = false,
            maxPaletteId = 10, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("B320", ignoreCase = true) ||
        deviceName.contains("B256", ignoreCase = true) ||
        deviceName.contains("B160", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false, hasGps = false, hasRadiometry = true, hasAudio = false,
            maxPaletteId = 10, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("TE", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false, hasGps = false, hasRadiometry = true, hasAudio = false,
            maxPaletteId = 10, supportsRestApi = true, reticle = reticle
        )
        deviceName.contains("PS", ignoreCase = true) -> CameraCapabilities(
            hasFocus = false, hasGps = false, hasRadiometry = true, hasAudio = false,
            maxPaletteId = 10, supportsRestApi = true, reticle = reticle
        )
        deviceName.startsWith("TB", ignoreCase = true) -> CameraCapabilities(
            hasFocus = true, hasGps = true, hasRadiometry = true, hasAudio = true,
            maxPaletteId = 15, supportsRestApi = true, reticle = reticle
        )
        else -> CameraCapabilities(
            hasFocus = false, hasGps = false, hasRadiometry = false, hasAudio = false,
            maxPaletteId = 10, supportsRestApi = false, reticle = reticle
        )
    }

    // Prefer project-code reticle flags when available (more accurate than device name alone).
    return if (projectCode.isNotBlank()) {
        baseCapabilities.copy(reticle = reticle)
    } else {
        baseCapabilities
    }
}
