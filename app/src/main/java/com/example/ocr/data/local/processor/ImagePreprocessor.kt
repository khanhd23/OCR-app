package com.example.ocr.data.local.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePreprocessor @Inject constructor() {

    fun process(src: Bitmap): Bitmap {
        val out = createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(floatArrayOf(
                    1.2f,    0f,    0f, 0f, -26f,
                    0f, 1.2f,    0f, 0f, -26f,
                    0f,    0f, 1.2f, 0f, -26f,
                    0f,    0f,    0f, 1f,   0f,
                ))
            )
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }
}