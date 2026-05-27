package com.example.ocr.core.common

object Constants {
    // Network
    const val BASE_URL = "https://5476-34-50-177-248.ngrok-free.app/"   // http://192.168.0.104:8000/
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
