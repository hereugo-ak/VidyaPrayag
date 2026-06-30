package com.littlebridge.enrollplus.feature.teacher.data.offline

import com.littlebridge.enrollplus.core.offline.outbox.OutboxOperation
import com.littlebridge.enrollplus.core.offline.sync.OutboxOperationHandler
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.data.remote.TeacherApi
import com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceSaveRequest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class AttendanceSaveHandler(
    private val api: TeacherApi,
    private val preferenceRepository: PreferenceRepository,
) : OutboxOperationHandler {

    override val type: String = "ATTENDANCE_SAVE"

    private val json = Json { ignoreUnknownKeys = true }
    private val requestSerializer = AttendanceSaveRequest.serializer()

    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = preferenceRepository.getUserToken().first() ?: return false
        val request = json.decodeFromString(requestSerializer, op.payloadJson)
        val result = api.saveAttendance(token, request)
        return result is com.littlebridge.enrollplus.core.network.NetworkResult.Success
    }
}

object AttendanceOutboxOps {
    const val TYPE = "ATTENDANCE_SAVE"

    fun create(
        request: AttendanceSaveRequest,
        idempotencyKey: String,
        now: Long,
    ): OutboxOperation {
        val json = Json { ignoreUnknownKeys = true }
        return OutboxOperation(
            id = idempotencyKey,
            idempotencyKey = idempotencyKey,
            type = TYPE,
            payloadJson = json.encodeToString(AttendanceSaveRequest.serializer(), request),
            status = com.littlebridge.enrollplus.core.offline.outbox.OutboxStatus.PENDING,
            attempts = 0,
            nextAttemptAt = now,
            createdAt = now,
        )
    }
}
