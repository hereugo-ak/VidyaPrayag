// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/rag/RagService.kt
package com.littlebridge.enrollplus.feature.tutor.rag

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorKnowledgeChunksTable
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * RAG Service — Retrieval-Augmented Generation on-ramp.
 *
 * Currently a STUB that returns text-matched chunks from
 * [TutorKnowledgeChunksTable]. When pgvector + embeddings are activated
 * (Phase 5), the `retrieve` method will switch to vector similarity search.
 *
 * The agent's `retrieve_knowledge` tool calls this service. The tool is
 * already registered in [com.littlebridge.enrollplus.feature.tutor.agent.TutorTools.RetrieveKnowledge].
 *
 * Kill-switched under module name "tutor_rag".
 *
 * SOLID:
 *   S → Single responsibility: knowledge chunk retrieval.
 *   D → Depends on Exposed table abstraction; swappable to pgvector.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8.4 (The RAG on-ramp)
 */
class RagService {
    private val log = LoggerFactory.getLogger("RagService")

    data class KnowledgeChunk(
        val id: UUID,
        val source: String,
        val board: String,
        val classLabel: String,
        val subject: String,
        val topicId: UUID?,
        val chunkText: String,
    )

    data class RetrievalResult(
        val chunks: List<KnowledgeChunk>,
        val query: String,
        val providerUsed: String,
        val note: String? = null,
    )

    /**
     * Retrieve knowledge chunks matching the query.
     *
     * Current implementation: simple text search (ILIKE-style).
     * Future: vector similarity search via pgvector.
     *
     * @param schoolId  tenant scope (null = shared NCERT corpus)
     * @param query     the search query
     * @param topicId   optional topic filter
     * @param limit     max chunks to return
     */
    suspend fun retrieve(
        schoolId: UUID?,
        query: String,
        topicId: UUID? = null,
        limit: Int = 5,
    ): RetrievalResult {
        TutorKillSwitch.require(TutorConstants.MODULE_RAG)

        val chunks = dbQuery {
            val rows = TutorKnowledgeChunksTable.selectAll().where {
                val conditions = mutableListOf<org.jetbrains.exposed.sql.Op<Boolean>>()
                if (schoolId != null) {
                    conditions.add(
                        (TutorKnowledgeChunksTable.schoolId eq schoolId) or
                        (TutorKnowledgeChunksTable.schoolId.isNull())
                    )
                }
                if (topicId != null) {
                    conditions.add(TutorKnowledgeChunksTable.topicId eq topicId)
                }
                conditions.reduce { acc, op -> acc and op }
            }.toList()

            // Text-based search (stub): filter by query substring in chunkText
            val lowerQuery = query.lowercase()
            rows.filter { it[TutorKnowledgeChunksTable.chunkText].lowercase().contains(lowerQuery) }
                .take(limit)
                .map { row ->
                    KnowledgeChunk(
                        id = row[TutorKnowledgeChunksTable.id].value,
                        source = row[TutorKnowledgeChunksTable.chunkSource],
                        board = row[TutorKnowledgeChunksTable.board],
                        classLabel = row[TutorKnowledgeChunksTable.classLabel],
                        subject = row[TutorKnowledgeChunksTable.subject],
                        topicId = row[TutorKnowledgeChunksTable.topicId],
                        chunkText = row[TutorKnowledgeChunksTable.chunkText],
                    )
                }
        }

        log.info("RagService: retrieved {} chunks for query='{}' (text search stub)",
            chunks.size, query.take(50))

        return RetrievalResult(
            chunks = chunks,
            query = query,
            providerUsed = "text_search_stub",
            note = if (chunks.isEmpty()) "No knowledge chunks found. RAG is in stub mode." else null,
        )
    }

    /**
     * Check if the RAG pipeline is active (has any embedded chunks).
     */
    suspend fun isRagActive(schoolId: UUID?): Boolean = dbQuery {
        val query = TutorKnowledgeChunksTable.selectAll()
        val filtered = if (schoolId != null) {
            query.where {
                (TutorKnowledgeChunksTable.schoolId eq schoolId) or
                (TutorKnowledgeChunksTable.schoolId.isNull())
            }
        } else query

        filtered.any { it[TutorKnowledgeChunksTable.embedding] != null }
    }
}
