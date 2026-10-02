package com.example.ocr.presentation.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr.R
import com.example.ocr.core.common.Resource
import com.example.ocr.core.extension.toFile
import com.example.ocr.data.local.processor.MLKitProcessorImpl
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.usecase.SaveDocumentUseCase
import com.example.ocr.domain.usecase.UploadBitmapUseCase
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
    data class Preview(val imageUris: List<Uri>) : CameraUiState()
    data class Processing(
        @StringRes val messageRes: Int,
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val progress: Float = 0f
    ) : CameraUiState()
    data class Success(val documentId: Long) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val uploadBitmapUseCase: UploadBitmapUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val mlKit: MLKitProcessorImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val selectedBitmaps = mutableListOf<Bitmap>()
    private val selectedUris = mutableListOf<Uri>()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            mlKit.warmup()
        }
    }

    fun onImagesSelected(context: Context, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clearPreviousSelection()
                selectedUris.addAll(uris)
                
                uris.forEach { uri ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }?.let { selectedBitmaps.add(it) }
                }

                _uiState.value = CameraUiState.Preview(selectedUris.toList())
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Lỗi tải ảnh")
            }
        }
    }

    fun onImageCaptured(context: Context, uri: Uri) {
        onImagesSelected(context, listOf(uri))
    }

    fun performOCR(context: Context) {
        if (selectedBitmaps.isEmpty()) return

        viewModelScope.launch {
            val totalPages = selectedBitmaps.size
            val fullTexts = mutableListOf<String>()
            val firstImagePath = selectedUris.firstOrNull()?.toFile(context)?.absolutePath ?: ""

            _uiState.value = CameraUiState.Processing(R.string.processing, 0, totalPages, 0f)

            var successCount = 0
            selectedBitmaps.forEachIndexed { index, bitmap ->
                val pageNum = index + 1
                _uiState.value = CameraUiState.Processing(
                    R.string.processing, 
                    pageNum, 
                    totalPages, 
                    index.toFloat() / totalPages
                )

                val result = withContext(Dispatchers.Default) {
                    uploadBitmapUseCase(bitmap, index)
                }

                if (result is Resource.Success) {
                    fullTexts.add(result.data?.fullText ?: "")
                    successCount++
                } else if (result is Resource.Error && totalPages == 1) {
                    _uiState.value = CameraUiState.Error(result.message ?: "OCR Error")
                    return@launch
                }
            }

            if (successCount > 0) {
                val combinedText = fullTexts.joinToString("\n\n----------------------------\n\n")
                val document = OCRDocument(
                    title = "Scan ${System.currentTimeMillis() / 1000}",
                    fullText = combinedText,
                    imagePath = firstImagePath,
                    pageCount = totalPages
                )
                val id = saveDocumentUseCase(document)
                clearPreviousSelection()
                _uiState.value = CameraUiState.Success(id)
            } else {
                _uiState.value = CameraUiState.Error("Failed to recognize any page")
            }
        }
    }

    fun retake() {
        clearPreviousSelection()
        _uiState.value = CameraUiState.Idle
    }

    private fun clearPreviousSelection() {
        selectedBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        selectedBitmaps.clear()
        selectedUris.clear()
    }

    override fun onCleared() {
        clearPreviousSelection()
        super.onCleared()
    }
}
