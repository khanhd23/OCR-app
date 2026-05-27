package com.example.ocr.core.di

import com.example.ocr.data.local.processor.ImagePreprocessor
import com.example.ocr.data.local.processor.LineImagePacker
import com.example.ocr.data.local.processor.MLKitProcessorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProcessorModule {

    @Provides @Singleton
    fun provideMLKitProcessor(): MLKitProcessorImpl = MLKitProcessorImpl()

    @Provides @Singleton
    fun provideImagePreprocessor(): ImagePreprocessor = ImagePreprocessor()

    @Provides @Singleton
    fun provideLineImagePacker(
        preprocessor: ImagePreprocessor,
        mlKit: MLKitProcessorImpl,
    ): LineImagePacker = LineImagePacker(preprocessor, mlKit)
}
