package com.example.ocr.core.network

import com.example.ocr.core.common.Constants
import com.example.ocr.data.remote.dto.ExportWordRequest
import com.example.ocr.data.remote.dto.OCRResponseDto
import com.example.ocr.data.remote.dto.PageOCRResponseDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface OCRApi {
    @Headers("ngrok-skip-browser-warning: 69420")
    @Multipart
    @POST(Constants.OCR_BATCH_ENDPOINT)
    suspend fun uploadLineBatch(
        @Part lines: List<MultipartBody.Part>,
        @Part("page_index") pageIndex: Int = 0,
        @Part("model") model: String = "mlkit"
    ): Response<PageOCRResponseDto>

    @Headers("ngrok-skip-browser-warning: 69420")
    @Multipart
    @POST(Constants.OCR_ENDPOINT)
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<OCRResponseDto>

    // Tải file Word từ server
    @POST(Constants.EXPORT_WORD_ENDPOINT)
    @Headers("Accept: application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    suspend fun exportWord(
        @Body request: ExportWordRequest
    ): Response<ResponseBody>
}

