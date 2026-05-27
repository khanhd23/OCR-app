package com.example.ocr.domain.model

data class OCRDocument(
    val id: Long = 0,
    val title: String,
    val fullText: String,
    val imagePath: String,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val language: String = "vi",
    val ocrResult: OCRResult? = null
)
