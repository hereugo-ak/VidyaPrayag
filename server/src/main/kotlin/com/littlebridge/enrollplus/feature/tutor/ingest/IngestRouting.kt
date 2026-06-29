// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/ingest/IngestRouting.kt
package com.littlebridge.enrollplus.feature.tutor.ingest

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Ingest (OCR + Voice).
 *
 * Endpoints:
 *   POST /tutor/ingest/photo   — upload a photo, extract text via OCR
 *   POST /tutor/ingest/voice   — upload audio, transcribe via Whisper
 *
 * Both accept multipart/form-data with a "file" part and "childId" + "subjectId" fields.
 *
 * Authorization: parent must own the child.
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4 step 1
 */
fun Route.ingestRouting() {

    post("/tutor/ingest/photo") {
        val uid = call.principalUserUuid() ?: return@post call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )

        var childIdStr: String? = null
        var subjectIdStr: String? = null
        var fileBytes: ByteArray? = null
        var mimeType: String? = null

        val multipart = call.receiveMultipart()
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "childId" -> childIdStr = part.value
                        "subjectId" -> subjectIdStr = part.value
                    }
                }
                is PartData.FileItem -> {
                    if (part.name == "file" && fileBytes == null) {
                        mimeType = part.contentType?.toString() ?: "image/jpeg"
                        @Suppress("DEPRECATION")
                        fileBytes = part.streamProvider().use { it.readBytes() }
                    }
                }
                else -> {}
            }
            part.dispose()
        }

        val childId = runCatching { UUID.fromString(childIdStr) }.getOrNull()
            ?: return@post call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = runCatching { UUID.fromString(subjectIdStr) }.getOrNull()
            ?: return@post call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")
        val imageBytes = fileBytes
            ?: return@post call.fail("No file uploaded", HttpStatusCode.BadRequest, "NO_FILE")

        // Ownership check
        val ownsChild = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq uid) and
                (ChildrenTable.isActive eq true)
            }.any()
        }
        if (!ownsChild) return@post call.fail(
            "Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND"
        )

        val schoolId = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@post call.fail(
            "Child has no school link", HttpStatusCode.BadRequest, "NO_SCHOOL"
        )

        val service = OcrService()
        val result = service.extractText(schoolId, imageBytes, mimeType ?: "image/jpeg")

        call.ok(
            IngestResponse(
                text = result.text,
                providerUsed = result.providerUsed,
                modelUsed = result.modelUsed,
                error = result.error,
            ),
            if (result.text.isNotEmpty()) "OCR complete" else "OCR failed"
        )
    }

    post("/tutor/ingest/voice") {
        val uid = call.principalUserUuid() ?: return@post call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )

        var childIdStr: String? = null
        var subjectIdStr: String? = null
        var fileBytes: ByteArray? = null
        var mimeType: String? = null

        val multipart = call.receiveMultipart()
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "childId" -> childIdStr = part.value
                        "subjectId" -> subjectIdStr = part.value
                    }
                }
                is PartData.FileItem -> {
                    if (part.name == "file" && fileBytes == null) {
                        mimeType = part.contentType?.toString() ?: "audio/mpeg"
                        @Suppress("DEPRECATION")
                        fileBytes = part.streamProvider().use { it.readBytes() }
                    }
                }
                else -> {}
            }
            part.dispose()
        }

        val childId = runCatching { UUID.fromString(childIdStr) }.getOrNull()
            ?: return@post call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = runCatching { UUID.fromString(subjectIdStr) }.getOrNull()
            ?: return@post call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")
        val audioBytes = fileBytes
            ?: return@post call.fail("No file uploaded", HttpStatusCode.BadRequest, "NO_FILE")

        // Ownership check
        val ownsChild = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq uid) and
                (ChildrenTable.isActive eq true)
            }.any()
        }
        if (!ownsChild) return@post call.fail(
            "Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND"
        )

        val schoolId = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@post call.fail(
            "Child has no school link", HttpStatusCode.BadRequest, "NO_SCHOOL"
        )

        val service = VoiceService()
        val result = service.transcribe(schoolId, audioBytes, mimeType ?: "audio/mpeg")

        call.ok(
            IngestResponse(
                text = result.text,
                providerUsed = result.providerUsed,
                modelUsed = result.modelUsed,
                error = result.error,
            ),
            if (result.text.isNotEmpty()) "Transcription complete" else "Transcription failed"
        )
    }
}

@Serializable
data class IngestResponse(
    val text: String,
    val providerUsed: String?,
    val modelUsed: Boolean,
    val error: String?,
)
