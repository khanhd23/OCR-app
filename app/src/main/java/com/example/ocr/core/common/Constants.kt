package com.example.ocr.core.common

import com.example.ocr.BuildConfig

object Constants {
    // Network
    // Cấu hình qua `ocr.baseUrl` trong local.properties (xem README)
    val BASE_URL: String = BuildConfig.BASE_URL
    const val OCR_ENDPOINT          = "api/ocr/"
    const val OCR_BATCH_ENDPOINT    = "api/ocr/batch"
    const val EXPORT_WORD_ENDPOINT  = "api/export/word"

    // Database
    const val DATABASE_NAME = "ocr_database"
    const val OCR_TABLE = "ocr_results"

    // Export
    const val EXPORT_DIR = "OCRExports"

    // Camera
    const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    const val PHOTO_EXTENSION = ".jpg"
}
