package com.littlebridge.enrollplus.feature.admin.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "announcement_entity")
data class AnnouncementEntity(
    @PrimaryKey val eventId: String,
    val type: String,
    val title: String,
    val subTitle: String?,
    val description: String,
    val eventImage: String?,
    val date: String,
    val audienceType: String,
    val audienceFilterJson: String?,
)

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcement_entity ORDER BY date DESC")
    fun observeAll(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcement_entity ORDER BY date DESC")
    suspend fun getAll(): List<AnnouncementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AnnouncementEntity>)

    @Query("DELETE FROM announcement_entity")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM announcement_entity")
    suspend fun count(): Int

    @Query("DELETE FROM announcement_entity WHERE eventId NOT IN (SELECT eventId FROM announcement_entity ORDER BY date DESC LIMIT :keepCount)")
    suspend fun evictOldest(keepCount: Int)
}
