// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/rag/RagRouting.kt
package com.littlebridge.enrollplus.feature.tutor.rag

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Routes for RAG (Retrieval-Augmented Generation).
 *
 * Endpoints:
 *   GET /tutor/rag/search?query=...&topicId=...&limit=... — search knowledge chunks
 *   GET /tutor/rag/status — check if RAG pipeline is active
 *
 * Authorization:
 *   /search — parent (for child context) or school role
 *   /status — school role (admin)
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8.4
 */
fun Route.ragRouting() {

    get("/tutor/rag/search") {
        val uid = call.principalUserUuid() ?: return@get call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val query = call.request.queryParameters["query"]
            ?: return@get call.fail("query required", HttpStatusCode.BadRequest, "BAD_QUERY")
        val topicIdStr = call.request.queryParameters["topicId"]
        val topicId = topicIdStr?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5

        // Resolve schoolId from user (parent or school role)
        val schoolId = com.littlebridge.enrollplus.core.resolveSchoolIdForUser(uid)

        val service = RagService()
        val result = service.retrieve(schoolId, query, topicId, limit)

        call.ok(
            RagSearchResponse(
                chunks = result.chunks.map { c ->
                    RagChunkResponse(
                        id = c.id.toString(),
                        source = c.source,
                        board = c.board,
                        classLabel = c.classLabel,
                        subject = c.subject,
                        topicId = c.topicId?.toString(),
                        chunkText = c.chunkText,
                    )
                },
                query = result.query,
                providerUsed = result.providerUsed,
                note = result.note,
            ),
            "RAG search (${result.chunks.size} chunks)"
        )
    }

    get("/tutor/rag/status") {
        val ctx = call.requireSchoolContext() ?: return@get

        val service = RagService()
        val active = service.isRagActive(ctx.schoolId)

        call.ok(
            RagStatusResponse(
                ragActive = active,
                mode = if (active) "vector" else "text_search_stub",
            ),
            if (active) "RAG is active" else "RAG is in stub mode"
        )
    }
}

@Serializable
data class RagSearchResponse(
    val chunks: List<RagChunkResponse>,
    val query: String,
    val providerUsed: String,
    val note: String?,
)

@Serializable
data class RagChunkResponse(
    val id: String,
    val source: String,
    val board: String,
    val classLabel: String,
    val subject: String,
    val topicId: String?,
    val chunkText: String,
)

@Serializable
data class RagStatusResponse(
    val ragActive: Boolean,
    val mode: String,
)
