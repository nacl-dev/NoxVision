package com.noxvision.app.hunting.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.noxvision.app.hunting.database.entities.HuntRecord
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun exportRecords(records: List<HuntRecord>, title: String = "Jagdtagebuch"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "jagdtagebuch_$timestamp.pdf"

                val document = PDDocument()
                var currentPage = createNewPage(document)
                var contentStream = PDPageContentStream(document, currentPage)
                var yPosition = 750f

                val fontBold = PDType1Font.HELVETICA_BOLD
                val fontRegular = PDType1Font.HELVETICA

                // Title
                contentStream.beginText()
                contentStream.setFont(fontBold, 18f)
                contentStream.newLineAtOffset(50f, yPosition)
                contentStream.showText(title)
                contentStream.endText()
                yPosition -= 30f

                // Date range
                if (records.isNotEmpty()) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                    val oldest = records.minOf { it.timestamp }
                    val newest = records.maxOf { it.timestamp }

                    contentStream.beginText()
                    contentStream.setFont(fontRegular, 10f)
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Zeitraum: ${dateFormat.format(Date(oldest))} - ${dateFormat.format(Date(newest))}")
                    contentStream.endText()
                    yPosition -= 15f

                    contentStream.beginText()
                    contentStream.setFont(fontRegular, 10f)
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Anzahl Eintraege: ${records.size}")
                    contentStream.endText()
                    yPosition -= 30f
                }

                // Records
                val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)

                for (record in records.sortedByDescending { it.timestamp }) {
                    if (yPosition < 100f) {
                        contentStream.close()
                        currentPage = createNewPage(document)
                        contentStream = PDPageContentStream(document, currentPage)
                        yPosition = 750f
                    }

                    // Record header
                    contentStream.beginText()
                    contentStream.setFont(fontBold, 11f)
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("${record.wildlifeType} ${record.gender ?: ""}")
                    contentStream.endText()
                    yPosition -= 15f

                    // Date and time
                    contentStream.beginText()
                    contentStream.setFont(fontRegular, 9f)
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Datum: ${dateTimeFormat.format(Date(record.timestamp))}")
                    contentStream.endText()
                    yPosition -= 12f

                    // Weight
                    if (record.estimatedWeight != null) {
                        contentStream.beginText()
                        contentStream.setFont(fontRegular, 9f)
                        contentStream.newLineAtOffset(50f, yPosition)
                        contentStream.showText("Gewicht: ${record.estimatedWeight} kg")
                        contentStream.endText()
                        yPosition -= 12f
                    }

                    // Location
                    if (record.latitude != null && record.longitude != null) {
                        contentStream.beginText()
                        contentStream.setFont(fontRegular, 9f)
                        contentStream.newLineAtOffset(50f, yPosition)
                        contentStream.showText("Position: ${String.format("%.6f", record.latitude)}, ${String.format("%.6f", record.longitude)}")
                        contentStream.endText()
                        yPosition -= 12f
                    }

                    // Bundesland
                    if (record.bundesland != null) {
                        contentStream.beginText()
                        contentStream.setFont(fontRegular, 9f)
                        contentStream.newLineAtOffset(50f, yPosition)
                        contentStream.showText("Bundesland: ${record.bundesland}")
                        contentStream.endText()
                        yPosition -= 12f
                    }

                    // Moon phase
                    if (record.moonPhase != null) {
                        contentStream.beginText()
                        contentStream.setFont(fontRegular, 9f)
                        contentStream.newLineAtOffset(50f, yPosition)
                        contentStream.showText("Mondphase: ${record.moonPhase}")
                        contentStream.endText()
                        yPosition -= 12f
                    }

                    // Notes
                    if (!record.notes.isNullOrBlank()) {
                        contentStream.beginText()
                        contentStream.setFont(fontRegular, 9f)
                        contentStream.newLineAtOffset(50f, yPosition)
                        val truncatedNotes = if (record.notes.length > 80) {
                            record.notes.take(77) + "..."
                        } else {
                            record.notes
                        }
                        contentStream.showText("Notizen: $truncatedNotes")
                        contentStream.endText()
                        yPosition -= 12f
                    }

                    yPosition -= 15f // Space between records
                }

                contentStream.close()

                // Save document
                val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(document, filename)
                } else {
                    saveToExternalStorage(document, filename)
                }

                document.close()
                Result.success(savedPath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun createNewPage(document: PDDocument): PDPage {
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)
        return page
    }

    private fun saveToMediaStore(document: PDDocument, filename: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/NoxVision")
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Failed to create file")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            document.save(outputStream)
        }

        return "Documents/NoxVision/$filename"
    }

    private fun saveToExternalStorage(document: PDDocument, filename: String): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NoxVision")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, filename)
        FileOutputStream(file).use { fos ->
            document.save(fos)
        }

        return file.absolutePath
    }
}
