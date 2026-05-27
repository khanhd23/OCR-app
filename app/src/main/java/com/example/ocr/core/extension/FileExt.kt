package com.example.ocr.core.extension

import android.content.Context
import android.net.Uri
import com.example.ocr.core.common.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.createTempImageFile(): File {
    val ts = SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.getDefault()).format(Date())
    return File.createTempFile("IMG_${ts}_", Constants.PHOTO_EXTENSION, cacheDir)
}

fun Context.createExportFile(fileName: String): File {
    val dir = File(getExternalFilesDir(null), Constants.EXPORT_DIR).apply { if (!exists()) mkdirs() }
    return File(dir, fileName)
}

fun Uri.toFile(context: Context): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val tempFile = context.createTempImageFile()
        inputStream.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}
