package com.littlebridge.enrollplus.core.offline.outbox

data class OutboxOperation(
    val id: String,
    val idempotencyKey: String,
    val type: String,
    val payloadJson: String,
    val status: OutboxStatus,
    val attempts: Int,
    val nextAttemptAt: Long,
    val createdAt: Long,
    val lastError: String? = null,
)
