package com.example.ocr.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ocr.domain.repository.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = Ink900, // Chữ đen trên nền xanh Teal
    primaryContainer = Ink700,
    onPrimaryContainer = Teal300,
    secondary = Amber400,
    onSecondary = Ink900, // Chữ đen trên nền vàng Amber
    background = Ink900,
    onBackground = TextPrimary,
    surface = Ink800,
    onSurface = TextPrimary,
    surfaceVariant = Ink700,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = ErrorRed,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = Teal400,
    onPrimary = Ink900, // Chữ đen trên nền xanh Teal (quan trọng cho chế độ sáng)
    primaryContainer = Teal100,
    onPrimaryContainer = Ink900,
    secondary = Amber400,
    onSecondary = Ink900,
    background = White,
    onBackground = Ink900,
    surface = Color(0xFFF5F5F5),
    onSurface = Ink900,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Ink700,
    outline = Color(0xFFDDDDDD),
    error = ErrorRed,
    onError = White
)

@Composable
fun OCRTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
