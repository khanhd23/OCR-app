package com.example.ocr.domain.model

import android.graphics.Rect

data class OCRLine(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float = 1f
)

data class OCRResult(
    val lines: List<OCRLine>,
    val fullText: String = lines.joinToString("\n") { it.text },
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)
