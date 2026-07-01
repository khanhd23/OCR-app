package com.example.ocr

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.ocr.domain.repository.AppTheme
import com.example.ocr.domain.repository.SettingsRepository
import com.example.ocr.presentation.navigation.OCRNavGraph
import com.example.ocr.presentation.theme.OCRTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            val appTheme by settingsRepository.getTheme().collectAsState(initial = AppTheme.SYSTEM)
            val languageCode by settingsRepository.getLanguage().collectAsState(initial = "en")
            
            val context = LocalContext.current
            
            // Create a localized context that still wraps the Activity context
            // This prevents Hilt from crashing because it can still find the Activity
            val localizedContext = remember(languageCode) {
                val locale = Locale(languageCode)
                Locale.setDefault(locale)
                
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                
                val baseLocalizedContext = context.createConfigurationContext(config)
                
                object : ContextWrapper(context) {
                    override fun getResources() = baseLocalizedContext.resources
                    override fun getAssets() = baseLocalizedContext.assets
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                OCRTheme(appTheme = appTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        OCRNavGraph()
                    }
                }
            }
        }
    }
}
