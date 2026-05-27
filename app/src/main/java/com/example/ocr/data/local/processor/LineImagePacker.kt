package com.example.ocr.data.local.processor

import android.graphics.Bitmap
import android.graphics.Rect
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineImagePacker @Inject constructor(
    private val preprocessor: ImagePreprocessor,
    private val mlKit: MLKitProcessorImpl,
) {

    data class PackResult(
        val parts     : List<MultipartBody.Part>,
        val lineCount : Int,
        val lineRects : List<Rect>,
    )

    suspend fun packForUpload(srcBitmap: Bitmap): PackResult {
        val processedBitmap = preprocessor.process(srcBitmap)
        val lineResults = mlKit.detectAndWarpLines(processedBitmap)
        processedBitmap.recycle()

        if (lineResults.isEmpty()) {
            return PackResult(emptyList(), 0, emptyList())
        }
        val parts = lineResults.mapIndexed { index, lineResult ->
            val jpegBytes = lineResult.warpedBitmap.toJpegBytes(quality = 92)
            lineResult.warpedBitmap.recycle()

            val fileName    = "line_${index.toString().padStart(3, '0')}.jpg"
            val requestBody = jpegBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("lines", fileName, requestBody)
        }

        val lineRects = lineResults.map { it.boundingBox }

        return PackResult(parts = parts, lineCount = parts.size, lineRects = lineRects)
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return baos.toByteArray()
    }
}