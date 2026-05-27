package com.example.ocr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OCRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
