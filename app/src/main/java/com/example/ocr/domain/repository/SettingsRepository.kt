package com.example.ocr.domain.repository

import kotlinx.coroutines.flow.Flow

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class OCRModel {
    MLKIT, TROCR
}

interface SettingsRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)

    fun getLanguage(): Flow<String>
    suspend fun setLanguage(languageCode: String)

    fun getOCRModel(): Flow<OCRModel>
    suspend fun setOCRModel(model: OCRModel)

    fun isHistoryEnabled(): Flow<Boolean>
    suspend fun setHistoryEnabled(enabled: Boolean)

    suspend fun clearAllHistory()
}
