package com.example.ocr.core.di

import android.content.Context
import androidx.room.Room
import com.example.ocr.core.common.Constants
import com.example.ocr.data.local.db.AppDatabase
import com.example.ocr.data.local.db.OCRDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideOCRDao(db: AppDatabase): OCRDao = db.ocrDao()
}
