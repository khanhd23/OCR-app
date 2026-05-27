package com.example.ocr.core.di

import com.example.ocr.data.repository.OCRRepositoryImpl
import com.example.ocr.domain.repository.OCRRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOCRRepository(impl: OCRRepositoryImpl): OCRRepository
}
