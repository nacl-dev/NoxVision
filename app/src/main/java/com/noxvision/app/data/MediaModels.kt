package com.noxvision.app.data

import android.net.Uri

data class CameraFile(
    val name: String,
    val size: Long,
    val date: String,
    val type: String
)

enum class GallerySource { CAMERA_DEVICE, PHONE }

enum class PhoneFolder { CAMERA, PICTURES, NOXVISION }

data class PhoneMediaFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateAddedSec: Long,
    val mime: String,
    val isVideo: Boolean
)
