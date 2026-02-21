package com.noxvision.app.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.core.graphics.createBitmap
import com.noxvision.app.data.CameraFile
import com.noxvision.app.data.PhoneFolder
import com.noxvision.app.data.PhoneMediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val FOLDER_GUIDE_CAMERA = "GuideCamera"

fun sanitizeFilename(name: String): String {
    // Strip directory path (support both forward and backward slashes)
    val simpleName = name.substringAfterLast('/').substringAfterLast('\\')
    // Replace invalid characters
    val sanitized = simpleName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    // Prevent parent directory reference
    return if (sanitized == "..") "__" else sanitized
}

fun buildDownloadUrls(baseUrl: String, filename: String): List<String> {
    // Extract simple filename to prevent path traversal
    val safeName = filename.substringAfterLast('/').substringAfterLast('\\')

    val encodedFilename = try {
        URLEncoder.encode(safeName, "UTF-8").replace("+", "%20")
    } catch (e: Exception) {
        safeName
    }

    return listOf(
        "$baseUrl/api/v1/files/download/$encodedFilename",
        "$baseUrl/videos/$encodedFilename",
        "$baseUrl/api/v1/files/download/videos/$encodedFilename",
        "$baseUrl/api/v1/files/videos/$encodedFilename",
        "$baseUrl/$encodedFilename"
    )
}

fun buildPrimaryDownloadUrl(baseUrl: String, filename: String): String {
    return buildDownloadUrls(baseUrl, filename).first()
}

suspend fun checkUrlConnection(urlString: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }
    }
}

suspend fun findWorkingUrl(
    urls: List<String>,
    checkUrl: suspend (String) -> Boolean = { checkUrlConnection(it) }
): String? = coroutineScope {
    val resultChannel = Channel<String>(Channel.UNLIMITED)
    val failures = AtomicInteger(0)

    if (urls.isEmpty()) return@coroutineScope null

    urls.forEach { url ->
        launch {
            if (checkUrl(url)) {
                resultChannel.trySend(url)
            } else {
                if (failures.incrementAndGet() == urls.size) {
                    resultChannel.close()
                }
            }
        }
    }

    try {
        val winner = resultChannel.receive()
        coroutineContext.cancelChildren()
        winner
    } catch (e: ClosedReceiveChannelException) {
        null
    }
}

private fun createMediaContentValues(filename: String, mimeType: String): ContentValues {
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/" + FOLDER_GUIDE_CAMERA
            )
        }
    }
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

fun createVideoFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    return File(storageDir, "VID_$timestamp.mp4")
}

suspend fun saveVideoToGallery(context: Context, file: File) {
    withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "VID_$timestamp.mp4"

        val contentValues = createMediaContentValues(filename, "video/mp4")

        val uri =
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}

suspend fun downloadVideoToCache(baseUrl: String, filename: String, context: Context): File? {
    return withContext(Dispatchers.IO) {
        val urlsToTry = buildDownloadUrls(baseUrl, filename)

        val safeFilename = sanitizeFilename(filename)
        val cacheFile = File(context.cacheDir, "video_preview_$safeFilename")

        AppLogger.log("Suche schnellste Cache-Download URL...", AppLogger.LogType.INFO)
        val workingUrl = findWorkingUrl(urlsToTry) ?: run {
            AppLogger.log("Alle Cache-Download URLs fehlgeschlagen!", AppLogger.LogType.ERROR)
            return@withContext null
        }

        try {
            AppLogger.log("Cache-Download versuche: $workingUrl", AppLogger.LogType.INFO)
            val url = URL(workingUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 60000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                AppLogger.log("Cache-Download URL OK: $workingUrl", AppLogger.LogType.SUCCESS)
                conn.inputStream.use { input ->
                    cacheFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                        AppLogger.log("Cache-Download OK: ${totalBytes / 1024}KB", AppLogger.LogType.SUCCESS)
                    }
                }
                conn.disconnect()
                return@withContext cacheFile
            } else {
                AppLogger.log("HTTP $responseCode für $workingUrl", AppLogger.LogType.INFO)
                conn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.log("Cache-Download Fehler: ${e.message}", AppLogger.LogType.INFO)
        }

        // If we reached here, the chosen URL failed during download.
        // We could fallback, but for now we return null as per optimized design.
        return@withContext null
    }
}

fun deleteCacheVideo(filename: String, context: Context) {
    try {
        val safeFilename = sanitizeFilename(filename)
        val cacheFile = File(context.cacheDir, "video_preview_$safeFilename")
        if (cacheFile.exists()) {
            cacheFile.delete()
            AppLogger.log("Cache gelöscht: $filename", AppLogger.LogType.INFO)
        }
    } catch (e: Exception) {
        AppLogger.log("Cache löschen fehlgeschlagen: ${e.message}", AppLogger.LogType.ERROR)
    }
}

suspend fun fetchCameraFiles(baseUrl: String): List<CameraFile> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/v1/files")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use {
                    it.readText()
                }

                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("value")
                val fileList = mutableListOf<CameraFile>()
                for (i in 0 until jsonArray.length()) {
                    val fileObj = jsonArray.getJSONObject(i)
                    val fileName = fileObj.optString("name", "unknown")
                    fileList.add(
                        CameraFile(
                            name = fileName,
                            size = fileObj.optLong("length", 0),
                            date = fileObj.optString("date", ""),
                            type = if (fileName.endsWith(".mp4")) "video" else "image"
                        )
                    )
                }
                conn.disconnect()
                fileList.sortedByDescending { it.date }
            } else {
                conn.disconnect()
                throw Exception("HTTP $responseCode")
            }
        } catch (e: Exception) {
            throw Exception("Connection error: ${e.message}")
        }
    }
}

suspend fun downloadFile(baseUrl: String, filename: String, appContext: Context) {
    withContext(Dispatchers.IO) {
        // We use the same URLs as video cache, but order might be different in original code?
        // Original: videos/, download/videos/, files/videos/, download/, root
        // buildDownloadUrls: download/, videos/, download/videos/, files/videos/, root
        // The order shouldn't matter much as long as we try them all.
        // Let's reuse buildDownloadUrls for consistency and simplicity.
        val urlsToTry = buildDownloadUrls(baseUrl, filename)

        AppLogger.log("Suche schnellste Download-URL...", AppLogger.LogType.INFO)
        val workingUrl = findWorkingUrl(urlsToTry) ?: run {
             AppLogger.log("Alle URLs fehlgeschlagen!", AppLogger.LogType.ERROR)
             throw Exception("Download fehlgeschlagen")
        }

        try {
            AppLogger.log("Versuche Download: $workingUrl", AppLogger.LogType.INFO)
            val url = URL(workingUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 30000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                AppLogger.log("Korrekte URL: $workingUrl", AppLogger.LogType.SUCCESS)
                val inputStream = conn.inputStream

                val safeFilename = sanitizeFilename(filename)
                val mimeType = if (safeFilename.endsWith(".mp4")) "video/mp4" else "image/jpeg"
                val contentValues = createMediaContentValues(safeFilename, mimeType)

                val uri = if (safeFilename.endsWith(".mp4")) {
                    appContext.contentResolver.insert(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                } else {
                    appContext.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                }

                uri?.let {
                    appContext.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        while (inputStream.read(buffer).also { readCount -> bytesRead = readCount } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                        AppLogger.log("Download OK: ${totalBytes / 1024}KB", AppLogger.LogType.SUCCESS)
                    }
                }

                conn.disconnect()
                return@withContext
            } else {
                AppLogger.log("HTTP $responseCode", AppLogger.LogType.INFO)
                conn.disconnect()
                throw Exception("HTTP $responseCode")
            }
        } catch (e: Exception) {
            AppLogger.log("${e.message}", AppLogger.LogType.INFO)
            throw e
        }
    }
}

suspend fun fetchPhoneMedia(context: Context, folder: PhoneFolder): List<PhoneMediaFile> {
    return withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val likePath = when (folder) {
            PhoneFolder.CAMERA -> "DCIM/Camera/%"
            PhoneFolder.PICTURES -> "Pictures/%"
            PhoneFolder.NOXVISION -> "DCIM/$FOLDER_GUIDE_CAMERA/%"
        }

        val selection = buildString {
            append("(")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE}=?")
            append(")")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?")
            }
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                likePath
            )
        } else {
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
        }

        val sort = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val out = mutableListOf<PhoneMediaFile>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sort)
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "unknown"
                    val size = c.getLong(sizeCol)
                    val date = c.getLong(dateCol)
                    val mime = c.getString(mimeCol) ?: "application/octet-stream"
                    val mediaType = c.getInt(typeCol)

                    val uri = ContentUris.withAppendedId(collection, id)

                    out.add(
                        PhoneMediaFile(
                            uri = uri,
                            name = name,
                            size = size,
                            dateAddedSec = date,
                            mime = mime,
                            isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                        )
                    )
                }
            }
        out
    }
}

fun captureScreenshot(context: Context, view: SurfaceView) {
    try {
        val bitmap = createBitmap(view.width, view.height)
        PixelCopy.request(
            view.holder.surface,
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    saveBitmapToGallery(context, bitmap)
                }
            },
            Handler(Looper.getMainLooper())
        )
    } catch (_: Exception) {
    }
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "IMG_$timestamp.jpg"

    val contentValues = createMediaContentValues(filename, "image/jpeg")

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
    }
}
