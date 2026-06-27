/*
 * File: MessageAttachmentUpload.kt
 * Module: feature.school
 *
 * Phase 1 (MESSAGING_SYSTEM_SPEC §12, §9.4) — shared attachment upload handler.
 *
 * The upload flow (§12.1):
 *   1. Client picks file → POST multipart to /api/v1/{role}/messages/attachments
 *   2. Server validates JWT + role + school + size + MIME
 *   3. Server uploads to Supabase Storage: {schoolId}/message/{uuid}.{ext}
 *   4. Returns {storage_url, file_name, mime_type, size_bytes, attachment_type}
 *   5. Client includes attachment metadata in the send message request
 *   6. Server INSERTs into message_attachments (in sendInConversation)
 *
 * This file provides the shared [handleAttachmentUpload] function that each
 * role-specific routing file calls with its resolved (userId, schoolId) context.
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.feature.media.SupabaseStorage
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AttachmentUploadResponse(
    @SerialName("storage_url") val storageUrl: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("attachment_type") val attachmentType: String,
)

// Per-spec §12.2 size limits by type.
private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024    // 10 MB
private const val MAX_VIDEO_BYTES = 25L * 1024 * 1024    // 25 MB
private const val MAX_DOCUMENT_BYTES = 25L * 1024 * 1024 // 25 MB
private const val MAX_AUDIO_BYTES = 10L * 1024 * 1024    // 10 MB

private val VALID_ATTACHMENT_TYPES = setOf("IMAGE", "VIDEO", "DOCUMENT", "AUDIO")

private fun maxBytesForType(type: String): Long = when (type) {
    "IMAGE" -> MAX_IMAGE_BYTES
    "VIDEO" -> MAX_VIDEO_BYTES
    "DOCUMENT" -> MAX_DOCUMENT_BYTES
    "AUDIO" -> MAX_AUDIO_BYTES
    else -> MAX_IMAGE_BYTES
}

/**
 * Shared multipart attachment upload handler. Called by each role's routing
 * file after resolving the caller's (userId, schoolId).
 *
 * Returns the [AttachmentUploadResponse] on success, or null if the call was
 * already responded to with an error (caller should just return).
 */
suspend fun ApplicationCall.handleAttachmentUpload(
    userId: UUID,
    schoolId: UUID,
): AttachmentUploadResponse? {
    if (!SupabaseStorage.isConfigured()) {
        fail(
            "Media storage is not configured on the server. " +
                "Set SUPABASE_URL and SUPABASE_SERVICE_KEY env vars.",
            status = HttpStatusCode.ServiceUnavailable,
            errorCode = "STORAGE_NOT_CONFIGURED",
        )
        return null
    }

    var attachmentType = "IMAGE"
    var fileBytes: ByteArray? = null
    var contentType: String? = null
    var fileName: String? = null

    val multipart = receiveMultipart()
    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                if (part.name == "attachment_type") {
                    val v = part.value.trim().uppercase()
                    if (v in VALID_ATTACHMENT_TYPES) attachmentType = v
                }
            }
            is PartData.FileItem -> {
                if (part.name == "file" && fileBytes == null) {
                    contentType = part.contentType?.toString()
                        ?: part.originalFileName?.let { guessContentTypeForAttachment(it) }
                    fileName = part.originalFileName ?: "attachment"
                    @Suppress("DEPRECATION")
                    fileBytes = part.streamProvider().use { it.readBytes() }
                }
            }
            else -> {}
        }
        part.dispose()
    }

    val bytes = fileBytes
    if (bytes == null || bytes.isEmpty()) {
        fail("No file part found (expected multipart field 'file').")
        return null
    }

    val maxBytes = maxBytesForType(attachmentType)
    if (bytes.size > maxBytes) {
        fail(
            "File too large (${bytes.size / (1024 * 1024)} MB). Max ${maxBytes / (1024 * 1024)} MB for type $attachmentType.",
            status = HttpStatusCode.PayloadTooLarge,
            errorCode = "ATTACHMENT_TOO_LARGE",
        )
        return null
    }

    val ct = contentType
    if (ct == null || SupabaseStorage.extensionFor(ct, attachmentType) == null) {
        fail(
            "Unsupported file type '${ct ?: "unknown"}' for attachment_type $attachmentType.",
            status = HttpStatusCode.UnsupportedMediaType,
            errorCode = "ATTACHMENT_TYPE_UNSUPPORTED",
        )
        return null
    }

    val result = SupabaseStorage.upload(schoolId, attachmentType, bytes, ct)
    if (result == null) {
        fail(
            "Upload to storage failed. Please try again.",
            status = HttpStatusCode.BadGateway,
            errorCode = "STORAGE_UPLOAD_FAILED",
        )
        return null
    }

    return AttachmentUploadResponse(
        storageUrl = result.url,
        fileName = fileName ?: "attachment",
        mimeType = ct.substringBefore(';').trim(),
        sizeBytes = result.sizeBytes,
        attachmentType = attachmentType,
    )
}

private fun guessContentTypeForAttachment(fileName: String): String? =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "heic" -> "image/heic"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt" -> "text/plain"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        else -> null
    }
