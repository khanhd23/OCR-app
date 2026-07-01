package com.example.ocr.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.ocr.core.common.Resource
import com.example.ocr.core.extension.createExportFile
import com.example.ocr.core.network.OCRApi
import com.example.ocr.data.local.db.OCRDao
import com.example.ocr.data.local.db.toDomain
import com.example.ocr.data.local.db.toEntity
import com.example.ocr.data.local.processor.LineImagePacker
import com.example.ocr.data.remote.dto.ExportWordRequest
import com.example.ocr.data.remote.mapper.toDomain
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.model.OCRResult
import com.example.ocr.domain.repository.OCRModel
import com.example.ocr.domain.repository.OCRRepository
import com.example.ocr.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class OCRRepositoryImpl @Inject constructor(
    private val api: OCRApi,
    private val dao: OCRDao,
    private val packer: LineImagePacker,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : OCRRepository {

    override suspend fun uploadBitmap(
        bitmap: Bitmap,
        pageIndex: Int
    ): Resource<OCRResult> = withContext(Dispatchers.Default) {
        try {
            val currentModel = settingsRepository.getOCRModel().first()
            val packed = packer.packForUpload(bitmap)

            if (packed.parts.isEmpty()) {
                return@withContext Resource.Error(
                    "Không phát hiện được dòng văn bản trong ảnh"
                )
            }

            val response = withContext(Dispatchers.IO) {
                api.uploadLineBatch(
                    lines = packed.parts,
                    pageIndex = pageIndex,
                    model = currentModel.name.lowercase()
                )
            }

            if (response.isSuccessful && response.body()?.success == true) {
                val result = response.body()!!.toDomain()
                
                // Kiểm tra cài đặt lưu lịch sử
                val shouldSave = settingsRepository.isHistoryEnabled().first()
                if (shouldSave) {
                    // Logic lưu vào DB sẽ được gọi từ ViewModel hoặc tại đây
                    // Ở đây mình trả về kết quả, ViewModel sẽ quyết định lưu hay không
                }
                
                Resource.Success(result)
            } else {
                Resource.Error(
                    response.body()?.message
                        ?: "Server lỗi ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Resource.Error("Lỗi: ${e.message}")
        }
    }
    override suspend fun uploadImage(imageFile: File): Resource<OCRResult> {
        return try {
            val part = MultipartBody.Part.createFormData(
                "file", imageFile.name,
                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            )
            val response = api.uploadImage(part)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.toDomain())
            } else {
                Resource.Error(response.body()?.message ?: "Không thể kết nối server")
            }
        } catch (e: Exception) {
            Resource.Error("Lỗi mạng: ${e.message}")
        }
    }

    override suspend fun exportToWord(document: OCRDocument): Resource<File> {
        return try {
            val contentList = document.fullText.split("\n")

            val response = api.exportWord(
                ExportWordRequest(
                    filename = document.title,
                    content = contentList
                )
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Resource.Error("Server trả về file rỗng")

                val fileName = response.headers()["Content-Disposition"]
                    ?.substringAfter("filename=")?.replace("\"", "")?.trim()
                    ?: "OCR_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.docx"

                val outputFile = context.createExportFile(fileName)
                body.byteStream().use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
                Resource.Success(outputFile)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Lỗi server ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Lỗi kết nối: ${e.message}")
        }
    }

    override fun getAllDocuments(): Flow<List<OCRDocument>> =
        dao.getAllDocuments().map { list -> list.map { it.toDomain() } }

    override suspend fun saveDocument(document: OCRDocument): Long =
        dao.insert(document.toEntity())

    override suspend fun getDocumentById(id: Long): OCRDocument? =
        dao.getById(id)?.toDomain()

    override suspend fun deleteDocument(id: Long) = dao.deleteById(id)

    override suspend fun searchDocuments(query: String): List<OCRDocument> =
        dao.search(query).map { it.toDomain() }
}
