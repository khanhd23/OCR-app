package com.example.ocr.data.remote.mapper

import android.graphics.Rect
import com.example.ocr.data.remote.dto.OCRResponseDto
import com.example.ocr.data.remote.dto.PageOCRResponseDto
import com.example.ocr.domain.model.OCRLine
import com.example.ocr.domain.model.OCRResult

fun OCRResponseDto.toDomain(): OCRResult {
    val mappedLines = lines?.map { lineDto ->
        OCRLine(
            text        = lineDto.text,
            confidence  = lineDto.confidence,
            boundingBox = lineDto.boundingBox?.let {
                Rect(it.left, it.top, it.right, it.bottom)
            }
        )
    } ?: emptyList()

    return OCRResult(
        lines       = mappedLines,
        fullText    = fullText ?: mappedLines.joinToString("\n") { it.text },
        imageWidth  = imageWidth,
        imageHeight = imageHeight
    )
}

fun PageOCRResponseDto.toDomain(): OCRResult {
    val mappedLines = results.map { lineText ->
        OCRLine(
            text        = lineText,
            confidence  = 1f,
            boundingBox = null
        )
    }

    return OCRResult(
        lines    = mappedLines,
        fullText = mappedLines.joinToString("\n") { it.text }
    )
}

