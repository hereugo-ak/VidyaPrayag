package com.littlebridge.enrollplus.core.offline.outbox

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "outbox_operation")
data class OutboxOperationEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val type: String,
    val payloadJson: String,
    val status: String,
    val attempts: Int,
    val nextAttemptAt: Long,
    val createdAt: Long,
    val lastError: String? = null,
)

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OutboxOperationEntity)

    @Query("SELECT * FROM outbox_operation WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun observePending(): Flow<List<OutboxOperationEntity>>

    @Query("SELECT * FROM outbox_operation WHERE status = 'PENDING' AND nextAttemptAt <= :now ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peekDueNow(now: Long, limit: Int): List<OutboxOperationEntity>

    @Query("UPDATE outbox_operation SET status = 'IN_FLIGHT' WHERE id = :id")
    suspend fun markInFlight(id: String)

    @Query("UPDATE outbox_operation SET status = 'DONE', lastError = NULL WHERE id = :id")
    suspend fun markDone(id: String)

    @Query("UPDATE outbox_operation SET status = 'FAILED', lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("UPDATE outbox_operation SET status = 'PENDING', attempts = attempts + 1, nextAttemptAt = :nextAttemptAt, lastError = :error WHERE id = :id")
    suspend fun markRetryable(id: String, nextAttemptAt: Long, error: String)

    @Query("UPDATE outbox_operation SET status = 'PENDING' WHERE status = 'IN_FLIGHT'")
    suspend fun resetInFlight()

    @Query("SELECT COUNT(*) FROM outbox_operation WHERE status = 'PENDING'")
    suspend fun countPending(): Int

    @Query("SELECT MIN(createdAt) FROM outbox_operation WHERE status = 'PENDING'")
    suspend fun oldestPendingCreatedAt(): Long?
}

fun OutboxOperationEntity.toDomain() = OutboxOperation(
    id = id,
    idempotencyKey = idempotencyKey,
    type = type,
    payloadJson = payloadJson,
    status = OutboxStatus.valueOf(status),
    attempts = attempts,
    nextAttemptAt = nextAttemptAt,
    createdAt = createdAt,
    lastError = lastError,
)

fun OutboxOperation.toEntity() = OutboxOperationEntity(
    id = id,
    idempotencyKey = idempotencyKey,
    type = type,
    payloadJson = payloadJson,
    status = status.name,
    attempts = attempts,
    nextAttemptAt = nextAttemptAt,
    createdAt = createdAt,
    lastError = lastError,
)
