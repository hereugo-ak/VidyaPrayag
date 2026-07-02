package com.littlebridge.enrollplus.feature.library.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "library_books")
data class LibraryBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val category: String?,
    val totalCopies: Int,
    val availableCopies: Int,
    val coverUrl: String?,
    val isbn: String?,
    val language: String,
    val isArchived: Boolean,
    val dataJson: String,
)

@Entity(tableName = "library_cache")
data class LibraryCacheEntity(
    @PrimaryKey val key: String,
    val dataJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "library_pending_actions")
data class LibraryPendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val bookId: String?,
    val issueId: String?,
    val payloadJson: String?,
    val createdAt: Long,
)

@Dao
interface LibraryBookDao {
    @Query("SELECT * FROM library_books")
    fun getAllBooks(): Flow<List<LibraryBookEntity>>

    @Query("SELECT * FROM library_books")
    suspend fun getAllBooksOnce(): List<LibraryBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<LibraryBookEntity>)

    @Query("DELETE FROM library_books")
    suspend fun clearBooks()
}

@Dao
interface LibraryCacheDao {
    @Query("SELECT * FROM library_cache WHERE key = :key")
    suspend fun get(key: String): LibraryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LibraryCacheEntity)

    @Query("DELETE FROM library_cache")
    suspend fun clearAll()
}

@Dao
interface LibraryPendingActionDao {
    @Insert
    suspend fun insert(action: LibraryPendingActionEntity): Long

    @Query("SELECT * FROM library_pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<LibraryPendingActionEntity>

    @Query("DELETE FROM library_pending_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM library_pending_actions")
    suspend fun clearAll()
}
