package com.littlebridge.enrollplus.feature.teacher.data.offline

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.offline.outbox.OutboxOperation
import com.littlebridge.enrollplus.core.offline.outbox.OutboxStatus
import com.littlebridge.enrollplus.core.offline.sync.OutboxOperationHandler
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.data.remote.TeacherApi
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssignHomeworkRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssignHomeworkResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.CreateTeacherLeaveRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.GrantExtensionRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkMutationResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.ReviewSubmissionRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherClassBroadcastRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherClassBroadcastResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherLeaveDecisionRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherSelfLeaveResponse
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

// ─────────────────────────────────────────────────────────────────────────────
// Offline write handlers for teacher mutations.
// Each handler replays one operation type via TeacherApi.
// ─────────────────────────────────────────────────────────────────────────────

private val json = Json { ignoreUnknownKeys = true }

class LeaveDecisionHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.LEAVE_DECIDE
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val parts = op.payloadJson.split("||", limit = 2)
        val leaveId = parts[0]
        val request = json.decodeFromString(TeacherLeaveDecisionRequest.serializer(), parts[1])
        return api.decideLeaveRequest(token, leaveId, request) is NetworkResult.Success
    }
}

class ApplyLeaveHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.LEAVE_APPLY
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val request = json.decodeFromString(CreateTeacherLeaveRequest.serializer(), op.payloadJson)
        return api.applyMyLeave(token, request) is NetworkResult.Success
    }
}

class AssignHomeworkHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.HOMEWORK_ASSIGN
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val request = json.decodeFromString(AssignHomeworkRequest.serializer(), op.payloadJson)
        return api.assignHomework(token, request) is NetworkResult.Success
    }
}

class HomeworkExtensionHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.HOMEWORK_EXTEND
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val parts = op.payloadJson.split("||", limit = 2)
        val homeworkId = parts[0]
        val request = json.decodeFromString(GrantExtensionRequest.serializer(), parts[1])
        return api.grantHomeworkExtension(token, homeworkId, request) is NetworkResult.Success
    }
}

class HomeworkReviewHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.HOMEWORK_REVIEW
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val parts = op.payloadJson.split("||", limit = 3)
        val homeworkId = parts[0]
        val studentId = parts[1]
        val request = json.decodeFromString(ReviewSubmissionRequest.serializer(), parts[2])
        return api.reviewHomeworkSubmission(token, homeworkId, studentId, request) is NetworkResult.Success
    }
}

class HomeworkCloseHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.HOMEWORK_CLOSE
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val parts = op.payloadJson.split("||", limit = 2)
        val homeworkId = parts[0]
        val assignmentId = parts[1]
        return api.closeHomework(token, homeworkId, assignmentId) is NetworkResult.Success
    }
}

class BroadcastToClassHandler(
    private val api: TeacherApi,
    private val prefs: PreferenceRepository,
) : OutboxOperationHandler {
    override val type = OutboxOps.BROADCAST_CLASS
    override suspend fun handle(op: OutboxOperation): Boolean {
        val token = prefs.getUserToken().first() ?: return false
        val request = json.decodeFromString(TeacherClassBroadcastRequest.serializer(), op.payloadJson)
        return api.broadcastToClass(token, request) is NetworkResult.Success
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OutboxOps — factory for all teacher write operations.
// ─────────────────────────────────────────────────────────────────────────────

object OutboxOps {
    const val LEAVE_DECIDE = "LEAVE_DECIDE"
    const val LEAVE_APPLY = "LEAVE_APPLY"
    const val HOMEWORK_ASSIGN = "HOMEWORK_ASSIGN"
    const val HOMEWORK_EXTEND = "HOMEWORK_EXTEND"
    const val HOMEWORK_REVIEW = "HOMEWORK_REVIEW"
    const val HOMEWORK_CLOSE = "HOMEWORK_CLOSE"
    const val BROADCAST_CLASS = "BROADCAST_CLASS"

    fun leaveDecide(leaveId: String, request: TeacherLeaveDecisionRequest, now: Long) = OutboxOperation(
        id = "leave-decide-$leaveId-${now}",
        idempotencyKey = "leave-decide-$leaveId-${now}",
        type = LEAVE_DECIDE,
        payloadJson = "$leaveId||${json.encodeToString(TeacherLeaveDecisionRequest.serializer(), request)}",
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )

    fun leaveApply(request: CreateTeacherLeaveRequest, now: Long) = OutboxOperation(
        id = "leave-apply-${now}",
        idempotencyKey = "leave-apply-${now}",
        type = LEAVE_APPLY,
        payloadJson = json.encodeToString(CreateTeacherLeaveRequest.serializer(), request),
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )

    fun homeworkAssign(request: AssignHomeworkRequest, now: Long) = OutboxOperation(
        id = "hw-assign-${request.assignmentId}-${now}",
        idempotencyKey = "hw-assign-${request.assignmentId}-${now}",
        type = HOMEWORK_ASSIGN,
        payloadJson = json.encodeToString(AssignHomeworkRequest.serializer(), request),
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )

    fun homeworkExtend(homeworkId: String, request: GrantExtensionRequest, now: Long) = OutboxOperation(
        id = "hw-extend-$homeworkId-${now}",
        idempotencyKey = "hw-extend-$homeworkId-${now}",
        type = HOMEWORK_EXTEND,
        payloadJson = "$homeworkId||${json.encodeToString(GrantExtensionRequest.serializer(), request)}",
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )

    fun homeworkReview(homeworkId: String, studentId: String, request: ReviewSubmissionRequest, now: Long) = OutboxOperation(
        id = "hw-review-$homeworkId-$studentId-${now}",
        idempotencyKey = "hw-review-$homeworkId-$studentId-${now}",
        type = HOMEWORK_REVIEW,
        payloadJson = "$homeworkId||$studentId||${json.encodeToString(ReviewSubmissionRequest.serializer(), request)}",
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )

    fun homeworkClose(homeworkId: String, assignmentId: String, now: Long) = OutboxOperation(
        id = "hw-close-$homeworkId-${now}",
        idempotencyKey = "hw-close-$homeworkId-${now}",
        type = HOMEWORK_CLOSE,
        payloadJson = "$homeworkId||$assignmentId",
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )

    fun broadcastClass(request: TeacherClassBroadcastRequest, now: Long) = OutboxOperation(
        id = "broadcast-${now}",
        idempotencyKey = "broadcast-${now}",
        type = BROADCAST_CLASS,
        payloadJson = json.encodeToString(TeacherClassBroadcastRequest.serializer(), request),
        status = OutboxStatus.PENDING,
        attempts = 0,
        nextAttemptAt = now,
        createdAt = now,
    )
}
