/*
 * File: LibraryPrivacyService.kt
 * Module: feature.library
 *
 * PII classification + GDPR rights (spec §16).
 *
 * PII Classification:
 *   - Tags library fields containing PII (borrowerName, actorName, etc.)
 *   - Used for data export and anonymization to know which fields to redact
 *
 * GDPR Rights:
 *   - Right to Access: export all library data for a borrower as JSON
 *   - Right to Erasure: anonymize borrower PII (delegated to LibraryService.anonymizeBorrower)
 *
 * Data Retention (enforced by LibraryJobScheduler monthly):
 *   - Audit log:      3 years (1095 days)
 *   - Issues:         5 years (1825 days) — only returned/closed
 *   - Announcements:  6 months (180 days) — already deactivated
 */
package com.littlebridge.enrollplus.feature.library

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import java.util.UUID

object LibraryPrivacyService {

    // ── PII Classification (spec §16) ──────────────────────────────────────
    // Fields tagged as PII are redacted in data exports when the requester
    // is not the data subject, and are anonymized on erasure.

    data class PiiField(val table: String, val column: String, val classification: PiiClassification)

    enum class PiiClassification {
        DIRECT_PII,     // name, email, phone — directly identifies a person
        INDIRECT_PII,   // borrower_id, actor_id — identifies via lookup
        SENSITIVE,      // fine amount, waiver reason — financial/behavioral
        NON_PII,        // book title, ISBN, dates — not personal data
    }

    val PII_REGISTRY: List<PiiField> = listOf(
        PiiField("library_issues", "borrower_name", PiiClassification.DIRECT_PII),
        PiiField("library_issues", "borrower_id", PiiClassification.INDIRECT_PII),
        PiiField("library_issues", "fine_amount", PiiClassification.SENSITIVE),
        PiiField("library_issues", "fine_status", PiiClassification.SENSITIVE),
        PiiField("library_reservations", "reserved_by_name", PiiClassification.DIRECT_PII),
        PiiField("library_reservations", "reserved_by", PiiClassification.INDIRECT_PII),
        PiiField("library_acquisition_requests", "requested_by_name", PiiClassification.DIRECT_PII),
        PiiField("library_acquisition_requests", "requested_by", PiiClassification.INDIRECT_PII),
        PiiField("library_book_discussions", "student_name", PiiClassification.DIRECT_PII),
        PiiField("library_book_discussions", "student_id", PiiClassification.INDIRECT_PII),
        PiiField("library_audit_log", "actor_name", PiiClassification.DIRECT_PII),
        PiiField("library_audit_log", "actor_id", PiiClassification.INDIRECT_PII),
        PiiField("library_audit_log", "metadata", PiiClassification.SENSITIVE),
        PiiField("library_wishlist", "student_id", PiiClassification.INDIRECT_PII),
        PiiField("library_reading_goals", "student_id", PiiClassification.INDIRECT_PII),
        PiiField("library_reading_badges", "student_id", PiiClassification.INDIRECT_PII),
    )

    fun classifyField(table: String, column: String): PiiClassification =
        PII_REGISTRY.firstOrNull { it.table == table && it.column == column }?.classification
            ?: PiiClassification.NON_PII

    // ── Data Retention Policy (spec §16) ────────────────────────────────────

    data class RetentionPolicy(val table: String, val retentionDays: Int, val description: String)

    val RETENTION_POLICIES: List<RetentionPolicy> = listOf(
        RetentionPolicy("library_audit_log", 1095, "Audit log: 3 years"),
        RetentionPolicy("library_issues", 1825, "Issues: 5 years (returned/closed only)"),
        RetentionPolicy("library_announcements", 180, "Announcements: 6 months (deactivated)"),
    )

    // ── GDPR Right to Access: Data Export ──────────────────────────────────
    // Returns a structured JSON representation of all library data associated
    // with a borrower. Used for GDPR Article 15 compliance.

    @Serializable
    data class GdprDataExport(
        val borrowerId: String,
        val exportedAt: String,
        val issues: List<IssueExport>,
        val reservations: List<ReservationExport>,
        val wishlist: List<WishlistExport>,
        val readingGoals: List<ReadingGoalExport>,
        val badges: List<BadgeExport>,
        val discussions: List<DiscussionExport>,
        val piiFieldsRedacted: List<String>,
    )

    @Serializable
    data class IssueExport(
        val bookTitle: String,
        val issueDate: String,
        val dueDate: String,
        val returnDate: String?,
        val status: String,
        val fineAmount: Double,
        val fineStatus: String,
    )

    @Serializable
    data class ReservationExport(
        val bookTitle: String,
        val status: String,
        val createdAt: String,
    )

    @Serializable
    data class WishlistExport(
        val bookTitle: String,
        val addedAt: String,
    )

    @Serializable
    data class ReadingGoalExport(
        val goalCount: Int,
        val period: String,
        val targetYear: Int,
    )

    @Serializable
    data class BadgeExport(
        val badgeType: String,
        val awardedAt: String,
    )

    @Serializable
    data class DiscussionExport(
        val bookTitle: String,
        val message: String,
        val createdAt: String,
    )
}
