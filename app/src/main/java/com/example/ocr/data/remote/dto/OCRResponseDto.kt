package com.example.ocr.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OCRResponseDto(
    @SerializedName("success")       val success: Boolean,
    @SerializedName("full_text")     val fullText: String?,
    @SerializedName("lines")         val lines: List<LineDto>?,
    @SerializedName("message")       val message: String?,
    @SerializedName("image_width")   val imageWidth: Int = 0,
    @SerializedName("image_height")  val imageHeight: Int = 0
)

data class LineDto(
    @SerializedName("text")         val text: String,
    @SerializedName("confidence")   val confidence: Float = 1f,
    @SerializedName("bounding_box") val boundingBox: BoundingBoxDto?
)

data class BoundingBoxDto(
    @SerializedName("left")   val left: Int,
    @SerializedName("top")    val top: Int,
    @SerializedName("right")  val right: Int,
    @SerializedName("bottom") val bottom: Int
)

data class PageOCRResponseDto(
    @SerializedName("success")     val success: Boolean,
    @SerializedName("pageIndex")   val pageIndex: Int = 0,
    @SerializedName("total")       val lineCount: Int = 0,
    @SerializedName("results")     val results: List<String>,
    @SerializedName("message")     val message: String?
)

data class ExportWordRequest(
    @SerializedName("filename") val filename: String,
    @SerializedName("content")  val content: List<String>,
)
