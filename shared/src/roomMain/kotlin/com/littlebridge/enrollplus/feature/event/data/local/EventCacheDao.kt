package com.littlebridge.enrollplus.feature.event.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventCacheDao {
    @Query("SELECT * FROM event_cache_entity ORDER BY startDate ASC")
    suspend fun getAllEvents(): List<EventCacheEntity>

    @Query("SELECT * FROM event_cache_entity WHERE id = :id")
    suspend fun getEventById(id: String): EventCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventCacheEntity>)

    @Query("DELETE FROM event_cache_entity")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM event_cache_entity")
    suspend fun count(): Int
}

@androidx.room.Entity(tableName = "event_cache_entity")
data class EventCacheEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String?,
    val type: String,
    val venue: String?,
    val schoolId: String,
    val registrationEnabled: Boolean,
    val maxAttendees: Int?,
    val hasSlots: Boolean,
    val myStatus: String?,
    val mySlotId: String?,
    val myAttendeeCount: Int?,
    val cachedAt: Long,
)
