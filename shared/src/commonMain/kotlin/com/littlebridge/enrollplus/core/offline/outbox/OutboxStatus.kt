package com.littlebridge.enrollplus.core.offline.outbox

enum class OutboxStatus {
    PENDING,
    IN_FLIGHT,
    FAILED,
    DONE,
}
