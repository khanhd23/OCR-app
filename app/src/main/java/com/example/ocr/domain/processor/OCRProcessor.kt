package com.example.ocr.domain.processor

import android.graphics.Bitmap
import android.graphics.Rect
import okhttp3.MultipartBody

interface OCRProcessor {
    suspend fun detectLineRects(bitmap: Bitmap): List<Rect>
    suspend fun processForUpload(bitmap: Bitmap): List<MultipartBody.Part>
}
