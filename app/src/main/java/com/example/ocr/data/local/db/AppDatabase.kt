package com.example.ocr.data.local.db

import android.graphics.Rect
import androidx.room.*
import com.example.ocr.core.common.Constants
import com.example.ocr.domain.model.OCRDocument
import kotlinx.coroutines.flow.Flow

// Entity
@Entity(tableName = Constants.OCR_TABLE)
data class OCREntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fullText: String,
    val imagePath: String,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val language: String = "vi"
)

// DAO
@Dao
interface OCRDao {
    @Query("SELECT * FROM ${Constants.OCR_TABLE} ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<OCREntity>>

    @Query("SELECT * FROM ${Constants.OCR_TABLE} WHERE id = :id")
    suspend fun getById(id: Long): OCREntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OCREntity): Long

    @Query("DELETE FROM ${Constants.OCR_TABLE} WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ${Constants.OCR_TABLE}")
    suspend fun deleteAll()

    @Query("SELECT * FROM ${Constants.OCR_TABLE} WHERE fullText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<OCREntity>
}

// Database
@Database(entities = [OCREntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ocrDao(): OCRDao
}

// Mappers
fun OCREntity.toDomain() = OCRDocument(
    id = id,
    title = title,
    fullText = fullText,
    imagePath = imagePath,
    pageCount = pageCount,
    createdAt = createdAt,
    language = language
)

fun OCRDocument.toEntity() = OCREntity(
    id = id,
    title = title,
    fullText = fullText,
    imagePath = imagePath,
    pageCount = pageCount,
    createdAt = createdAt,
    language = language
)
