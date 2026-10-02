package com.example.ocr.domain.usecase

import android.graphics.Bitmap
import com.example.ocr.core.common.Resource
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.model.OCRResult
import com.example.ocr.domain.repository.OCRRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Send bitmap to server
class UploadBitmapUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, pageIndex: Int = 0): Resource<OCRResult> =
        ocrRepository.uploadBitmap(bitmap, pageIndex)
}

class SaveDocumentUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    suspend operator fun invoke(document: OCRDocument): Long =
        ocrRepository.saveDocument(document)
}

class GetAllDocumentsUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    operator fun invoke(): Flow<List<OCRDocument>> =
        ocrRepository.getAllDocuments()
}

class GetDocumentByIdUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    suspend operator fun invoke(id: Long): OCRDocument? =
        ocrRepository.getDocumentById(id)
}

class DeleteDocumentUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    suspend operator fun invoke(id: Long) =
        ocrRepository.deleteDocument(id)
}

class SearchDocumentsUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    suspend operator fun invoke(query: String): List<OCRDocument> =
        ocrRepository.searchDocuments(query)
}
