package com.example.ocr.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.ocr.data.local.db.OCRDao
import com.example.ocr.domain.repository.AppTheme
import com.example.ocr.domain.repository.OCRModel
import com.example.ocr.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrDao: OCRDao
) : SettingsRepository {

    private val supportedLanguages = listOf("vi", "en", "zh", "ja", "ko", "fr")

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val LANGUAGE = stringPreferencesKey("app_language")
        val OCR_MODEL = stringPreferencesKey("ocr_model")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
    }

    override fun getTheme(): Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    override fun getLanguage(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE] ?: getSystemDefaultLanguage()
    }

    private fun getSystemDefaultLanguage(): String {
        val systemLocale = Locale.getDefault().language
        return if (supportedLanguages.contains(systemLocale)) {
            systemLocale
        } else {
            "en"
        }
    }

    override suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = languageCode
        }
    }

    override fun getOCRModel(): Flow<OCRModel> = context.dataStore.data.map { preferences ->
        val modelName = preferences[PreferencesKeys.OCR_MODEL] ?: OCRModel.MLKIT.name
        try {
            OCRModel.valueOf(modelName)
        } catch (e: Exception) {
            OCRModel.MLKIT
        }
    }

    override suspend fun setOCRModel(model: OCRModel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OCR_MODEL] = model.name
        }
    }

    override fun isHistoryEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVE_HISTORY] ?: true
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_HISTORY] = enabled
        }
    }

    override suspend fun clearAllHistory() {
        ocrDao.deleteAll()
    }
}
