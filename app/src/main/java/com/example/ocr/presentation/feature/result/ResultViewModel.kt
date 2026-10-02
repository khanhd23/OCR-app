package com.example.ocr.presentation.feature.result

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr.core.common.Resource
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.usecase.DeleteDocumentUseCase
import com.example.ocr.domain.usecase.ExportWordUseCase
import com.example.ocr.domain.usecase.GetDocumentByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Success(val document: OCRDocument) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}

sealed class ExportState {
    object Idle : ExportState()
    object Downloading : ExportState()
    data class Done(val file: File) : ExportState()
    data class Failed(val message: String) : ExportState()
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val getDocumentByIdUseCase: GetDocumentByIdUseCase,
    private val exportWordUseCase: ExportWordUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _copySuccess = MutableStateFlow(false)
    val copySuccess: StateFlow<Boolean> = _copySuccess.asStateFlow()

    fun loadDocument(id: Long) {
        viewModelScope.launch {
            val doc = getDocumentByIdUseCase(id)
            _uiState.value = if (doc != null)
                ResultUiState.Success(doc)
            else
                ResultUiState.Error("Không tìm thấy tài liệu")
        }
    }

    // Download word file from server
    fun downloadWord() {
        val doc = (_uiState.value as? ResultUiState.Success)?.document ?: return
        viewModelScope.launch {
            _exportState.value = ExportState.Downloading
            when (val result = exportWordUseCase(doc)) {
                is Resource.Success -> _exportState.value = ExportState.Done(result.data!!)
                is Resource.Error   -> _exportState.value = ExportState.Failed(result.message ?: "Tải file thất bại")
                is Resource.Loading -> {}
            }
        }
    }

    fun copyText(context: Context) {
        val doc = (_uiState.value as? ResultUiState.Success)?.document ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OCR Text", doc.fullText))
        viewModelScope.launch {
            _copySuccess.value = true
            kotlinx.coroutines.delay(2000)
            _copySuccess.value = false
        }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        val doc = (_uiState.value as? ResultUiState.Success)?.document ?: return
        viewModelScope.launch {
            deleteDocumentUseCase(doc.id)
            onDeleted()
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}
