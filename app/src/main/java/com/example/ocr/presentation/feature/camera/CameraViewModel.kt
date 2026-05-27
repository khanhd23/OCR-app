package com.example.ocr.presentation.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.ocr.data.local.processor.MLKitProcessorImpl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr.core.common.Resource
import com.example.ocr.core.extension.toFile
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.repository.OCRRepository
import com.example.ocr.domain.usecase.SaveDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class CameraUiState {
    object Idle : CameraUiState()

    data class Preview(
        val imageUri: Uri
    ) : CameraUiState()

    data class Processing(
        val message: String = "Đang nhận dạng văn bản..."
    ) : CameraUiState()

    data class Success(
        val documentId: Long
    ) : CameraUiState()

    data class Error(
        val message: String
    ) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val ocrRepository: OCRRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val mlKit: MLKitProcessorImpl
) : ViewModel() {

    private var capturedBitmap: Bitmap? = null

    private val _uiState =
        MutableStateFlow<CameraUiState>(CameraUiState.Idle)

    val uiState: StateFlow<CameraUiState> =
        _uiState.asStateFlow()
    init {
        viewModelScope.launch(Dispatchers.Default) {
            mlKit.warmup()
        }
    }
    fun onImageCaptured(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                val bitmap =
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    } ?: run {
                        _uiState.value =
                            CameraUiState.Error("Không đọc được ảnh")
                        return@launch
                    }

                recyclePreviousBitmap()

                capturedBitmap = bitmap

                _uiState.value =
                    CameraUiState.Preview(uri)

            } catch (e: Exception) {
                _uiState.value =
                    CameraUiState.Error(
                        e.message ?: "Lỗi không xác định"
                    )
            }
        }
    }

    fun onImageSelected(context: Context, uri: Uri) {
        onImageCaptured(context, uri)
    }

    fun performOCR(context: Context) {
        val current =
            _uiState.value as? CameraUiState.Preview ?: return

        val bitmap =
            capturedBitmap ?: return

        viewModelScope.launch {
            _uiState.value =
                CameraUiState.Processing(
                    "Đang phân tích bố cục ảnh..."
                )

            val result = withContext(Dispatchers.Default) {
                ocrRepository.uploadBitmap(bitmap, 0)
            }

            when (result) {
                is Resource.Success -> {
                    _uiState.value =
                        CameraUiState.Processing(
                            "Đang lưu kết quả..."
                        )

                    val ocrResult = result.data!!
                    val imageFile = current.imageUri.toFile(context)

                    val document = OCRDocument(
                        title = "Tài liệu ${System.currentTimeMillis() / 1000}",
                        fullText = ocrResult.fullText,
                        imagePath = imageFile?.absolutePath ?: "",
                        ocrResult = ocrResult
                    )

                    val id =
                        saveDocumentUseCase(document)

                    recyclePreviousBitmap()

                    _uiState.value =
                        CameraUiState.Success(id)
                }

                is Resource.Error -> {
                    recyclePreviousBitmap()

                    _uiState.value =
                        CameraUiState.Error(
                            result.message ?: "OCR thất bại"
                        )
                }

                else -> {}
            }
        }
    }

    fun retake() {
        recyclePreviousBitmap()
        _uiState.value = CameraUiState.Idle
    }

    private fun recyclePreviousBitmap() {
        capturedBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        capturedBitmap = null
    }

    override fun onCleared() {
        recyclePreviousBitmap()
        super.onCleared()
    }
}