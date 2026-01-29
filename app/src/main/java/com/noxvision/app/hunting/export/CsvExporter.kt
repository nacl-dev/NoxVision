package com.noxvision.app.hunting.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.noxvision.app.hunting.database.entities.HuntRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(private val context: Context) {

    suspend fun exportRecords(records: List<HuntRecord>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "jagdtagebuch_$timestamp.csv"

                val csvContent = buildCsvContent(records)

                val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(filename, csvContent)
                } else {
                    saveToExternalStorage(filename, csvContent)
                }

                Result.success(savedPath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildCsvContent(records: List<HuntRecord>): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("ID;Datum;Uhrzeit;Wildart;Geschlecht;Gewicht (kg);Breitengrad;Laengengrad;Bundesland;Mondphase;Notizen")

        // Data rows
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.GERMANY)

        for (record in records) {
            val date = Date(record.timestamp)
            sb.append(record.id).append(";")
            sb.append(dateFormat.format(date)).append(";")
            sb.append(timeFormat.format(date)).append(";")
            sb.append(escapeCsv(record.wildlifeType)).append(";")
            sb.append(escapeCsv(record.gender ?: "")).append(";")
            sb.append(record.estimatedWeight ?: "").append(";")
            sb.append(record.latitude?.toString() ?: "").append(";")
            sb.append(record.longitude?.toString() ?: "").append(";")
            sb.append(escapeCsv(record.bundesland ?: "")).append(";")
            sb.append(escapeCsv(record.moonPhase ?: "")).append(";")
            sb.append(escapeCsv(record.notes ?: ""))
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun saveToMediaStore(filename: String, content: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NoxVision")
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Failed to create file")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }

        return "Documents/NoxVision/$filename"
    }

    private fun saveToExternalStorage(filename: String, content: String): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NoxVision")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }

        return file.absolutePath
    }
}
