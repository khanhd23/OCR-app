package com.example.ocr.domain.repository

import android.graphics.Bitmap
import com.example.ocr.core.common.Resource
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.model.OCRResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface OCRRepository {

    suspend fun uploadBitmap(bitmap: Bitmap, pageIndex: Int = 0): Resource<OCRResult>

    suspend fun uploadImage(imageFile: File): Resource<OCRResult>

    suspend fun exportToWord(document: OCRDocument): Resource<File>
    fun getAllDocuments(): Flow<List<OCRDocument>>
    suspend fun saveDocument(document: OCRDocument): Long
    suspend fun getDocumentById(id: Long): OCRDocument?
    suspend fun deleteDocument(id: Long)
    suspend fun searchDocuments(query: String): List<OCRDocument>
}
