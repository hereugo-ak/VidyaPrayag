package com.littlebridge.enrollplus.feature.teacher.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "teacher_day_cache")
data class TeacherDayCacheEntity(
    @PrimaryKey val date: String,
    val weekday: Int,
    val isHoliday: Boolean,
    val holidayName: String?,
    val periodsJson: String,
    val calendarJson: String,
    val nowIndex: Int?,
    val nextIndex: Int?,
    val cachedAt: Long,
)

@Dao
interface TeacherDayCacheDao {
    @Query("SELECT * FROM teacher_day_cache WHERE date = :date")
    suspend fun getByDate(date: String): TeacherDayCacheEntity?

    @Query("SELECT * FROM teacher_day_cache ORDER BY cachedAt DESC")
    fun observeAll(): Flow<List<TeacherDayCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TeacherDayCacheEntity)

    @Query("DELETE FROM teacher_day_cache WHERE date != :keepDate")
    suspend fun evictOlder(keepDate: String)

    @Query("DELETE FROM teacher_day_cache")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM teacher_day_cache")
    suspend fun count(): Int

    @Query("DELETE FROM teacher_day_cache WHERE date NOT IN (SELECT date FROM teacher_day_cache ORDER BY cachedAt DESC LIMIT :keepCount)")
    suspend fun evictOldest(keepCount: Int)
}
