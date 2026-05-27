package com.example.ocr.domain.usecase

import com.example.ocr.core.common.Resource
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.repository.OCRRepository
import java.io.File
import javax.inject.Inject

class ExportWordUseCase @Inject constructor(
    private val ocrRepository: OCRRepository
) {
    suspend operator fun invoke(document: OCRDocument): Resource<File> {
        return ocrRepository.exportToWord(document)
    }
}
