package com.example.ocr.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr.domain.repository.AppTheme
import com.example.ocr.domain.repository.OCRModel
import com.example.ocr.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val theme: StateFlow<AppTheme> = repository.getTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val language: StateFlow<String> = repository.getLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "vi")

    val ocrModel: StateFlow<OCRModel> = repository.getOCRModel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OCRModel.MLKIT)

    val isHistoryEnabled: StateFlow<Boolean> = repository.isHistoryEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.setLanguage(languageCode)
        }
    }

    fun setOCRModel(model: OCRModel) {
        viewModelScope.launch {
            repository.setOCRModel(model)
        }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHistoryEnabled(enabled)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
