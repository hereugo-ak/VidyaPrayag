package com.littlebridge.enrollplus.feature.event.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventOutboxDao {
    @Query("SELECT * FROM event_outbox_entity WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<EventOutboxEntity>

    @Query("SELECT * FROM event_outbox_entity WHERE id = :id")
    suspend fun getById(id: String): EventOutboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EventOutboxEntity)

    @Query("UPDATE event_outbox_entity SET status = :status, attempts = :attempts, lastError = :lastError, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, attempts: Int, lastError: String?, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM event_outbox_entity WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int

    @Query("DELETE FROM event_outbox_entity WHERE status = 'SYNCED' AND updatedAt < :before")
    suspend fun cleanSynced(before: Long)
}

@androidx.room.Entity(tableName = "event_outbox_entity")
data class EventOutboxEntity(
    @androidx.room.PrimaryKey val id: String,
    val operation: String,
    val eventId: String,
    val slotId: String?,
    val studentId: String?,
    val attendeeCount: Int,
    val clientRequestId: String,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
