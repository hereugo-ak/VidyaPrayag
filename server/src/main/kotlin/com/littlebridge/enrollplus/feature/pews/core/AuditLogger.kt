// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/AuditLogger.kt
package com.littlebridge.enrollplus.feature.pews.core

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.NotificationsTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID



/**
 * Shared audit-logging utility for all PEWS 2.0 modules. Writes a structured
 * record to the notifications table (category="pews_audit") so PEWS actions
 * are traceable end-to-end — who triggered what, when, and against which entity.
 *
 * This lives in pews-core so every module uses the same logging shape without
 * duplicating boilerplate (REUSABILITY mandate).
 *
 * SOLID:
 *   - S: one responsibility — audit logging.
 *   - D: depends on the DB layer, not on any PEWS module.
 */
object AuditLogger {
    private val log = LoggerFactory.getLogger("PewsAudit")
    private val json = Json { encodeDefaults = true }

    /**
     * Record a PEWS audit event.
     *
     * @param schoolId     tenant scope
     * @param actorUserId  who triggered the action (null = system/scheduler)
     * @param module       PEWS module name (e.g. "sense", "caseworker", "act")
     * @param action       what happened (e.g. "recompute", "open_intervention", "draft_message")
     * @param entityType   the kind of entity affected (e.g. "snapshot", "intervention")
     * @param entityId     the entity's identifier (student code, intervention UUID, etc.)
     * @param details      additional structured context (serialized to JSON)
     */
    suspend fun log(
        schoolId: UUID,
        actorUserId: UUID? = null,
        module: String,
        action: String,
        entityType: String,
        entityId: String,
        details: Map<String, Any?> = emptyMap(),
    ) {
        val detailsJson = if (details.isEmpty()) "" else json.encodeToString(details)
        runCatching {
            dbQuery {
                NotificationsTable.insert {
                    it[NotificationsTable.schoolId] = schoolId
                    it[NotificationsTable.userId] = actorUserId ?: schoolId
                    it[NotificationsTable.actorId] = actorUserId
                    it[NotificationsTable.category] = "pews_audit"
                    it[NotificationsTable.title] = "[$module] $action"
                    it[NotificationsTable.body] = if (detailsJson.isNotBlank()) "$entityType:$entityId $detailsJson" else "$entityType:$entityId"
                    it[NotificationsTable.refType] = entityType
                    it[NotificationsTable.refId] = entityId
                    it[NotificationsTable.createdAt] = Instant.now()
                    it[NotificationsTable.isRead] = false
                }
            }
        }.onFailure { log.warn("AuditLogger write failed: {}", it.message) }
    }
}
