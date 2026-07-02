/*
 * File: LibraryDtos.kt
 * Module: feature.library
 *
 * Server-side DTOs for the Library Management feature. These mirror the
 * shared module DTOs in shared/.../library/domain/model/LibraryModels.kt
 * but live in the server module because :server does NOT depend on :shared
 * (see server/build.gradle.kts for rationale).
 */
package com.littlebridge.enrollplus.feature.library

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ── Core DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class LibraryBookDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val totalCopies: Int,
    val availableCopies: Int,
    val shelfLocation: String? = null,
    val coverUrl: String? = null,
    val replacementCost: Double? = null,
    val seriesName: String? = null,
    val seriesNumber: Int? = null,
    val language: String = "en",
    val isArchived: Boolean = false,
    val synopsis: String? = null,
    val pageCount: Int? = null,
)

@Serializable
data class BookCopyDto(
    val id: String,
    val bookId: String,
    val copyNumber: Int,
    val barcode: String? = null,
    val condition: String,
    val status: String,
)

@Serializable
data class LibraryIssueDto(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val copyId: String? = null,
    val copyNumber: Int? = null,
    val borrowerId: String,
    val borrowerType: String,
    val borrowerName: String,
    val issueDate: String,
    val dueDate: String,
    val returnDate: String? = null,
    val returnCondition: String? = null,
    val damageNotes: String? = null,
    val renewalCount: Int,
    val fineAmount: Double,
    val fineStatus: String,
    val finePaidAt: String? = null,
    val fineWaivedReason: String? = null,
    val status: String,
)

@Serializable
data class LibraryReservationDto(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val reservedBy: String,
    val reservedByName: String,
    val reservedByType: String,
    val status: String,
    val createdAt: String,
    val fulfilledAt: String? = null,
    val waitlistPosition: Int? = null,
)

@Serializable
data class LibraryCategoryDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val displayOrder: Int,
)

@Serializable
data class LibrarySettingsDto(
    val defaultLoanDays: Int = 14,
    val finePerDay: Double = 1.0,
    val maxBooksPerStudent: Int = 3,
    val maxRenewals: Int = 2,
    val reservationTimeoutDays: Int = 7,
    val dueReminderDays: Int = 2,
    val fineCapEnabled: Boolean = true,
    val quickIssueEnabled: Boolean = true,
    val bulkReturnEnabled: Boolean = true,
    val leaderboardEnabled: Boolean = true,
)

@Serializable
data class LibraryDashboardDto(
    val totalBooks: Int,
    val totalCopies: Int,
    val availableCopies: Int,
    val issuedCopies: Int,
    val overdueBooks: Int,
    val activeReservations: Int,
    val lostBooks: Int,
    val outstandingFinesCount: Int,
    val outstandingFinesAmount: Double,
)

@Serializable
data class LibraryAuditLogDto(
    val id: String,
    val actorId: String? = null,
    val actorName: String,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val metadata: String? = null,
    val previousState: String? = null,
    val newState: String? = null,
    val hash: String,
    val createdAt: String,
)

@Serializable
data class LibraryAnnouncementDto(
    val id: String,
    val title: String,
    val message: String,
    val audience: String,
    val createdByName: String,
    val expiresAt: String? = null,
    val isActive: Boolean,
    val createdAt: String,
)

@Serializable
data class LibraryWishlistDto(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String? = null,
    val coverUrl: String? = null,
    val availableCopies: Int,
    val addedAt: String,
)

@Serializable
data class LibraryReadingGoalDto(
    val id: String,
    val studentId: String,
    val goalCount: Int,
    val period: String,
    val targetYear: Int,
    val booksRead: Int,
    val progressPercentage: Double,
    val isAchieved: Boolean,
)

@Serializable
data class LibraryAcquisitionRequestDto(
    val id: String,
    val requestedByName: String,
    val requestedByType: String,
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val reason: String? = null,
    val estimatedCost: Double? = null,
    val status: String,
    val orderLink: String? = null,
    val convertedBookId: String? = null,
    val createdAt: String,
)

@Serializable
data class LibraryBadgeDto(
    val badgeType: String,
    val badgeName: String,
    val badgeIcon: String,
    val isEarned: Boolean,
    val earnedAt: String? = null,
)

@Serializable
data class LibraryDiscussionMessageDto(
    val id: String,
    val studentName: String,
    val message: String,
    val createdAt: String,
    val isDeleted: Boolean = false,
)

@Serializable
data class StudentLibraryProfileDto(
    val totalBooksRead: Int,
    val currentlyIssued: Int,
    val overdueCount: Int,
    val outstandingFine: Double,
    val badgesEarned: Int,
    val maxBorrowingLimit: Int = 0,
    val mostReadCategories: List<CategoryStatDto> = emptyList(),
)

// ── Request DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class CreateBookRequest(
    val isbn: String? = null,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    @SerialName("total_copies") val totalCopies: Int = 1,
    @SerialName("shelf_location") val shelfLocation: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("replacement_cost") val replacementCost: Double? = null,
    @SerialName("series_name") val seriesName: String? = null,
    @SerialName("series_number") val seriesNumber: Int? = null,
    val language: String = "en",
    val synopsis: String? = null,
    @SerialName("page_count") val pageCount: Int? = null,
)

@Serializable
data class UpdateBookRequest(
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    @SerialName("shelf_location") val shelfLocation: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("replacement_cost") val replacementCost: Double? = null,
    @SerialName("series_name") val seriesName: String? = null,
    @SerialName("series_number") val seriesNumber: Int? = null,
    val language: String? = null,
    val synopsis: String? = null,
    @SerialName("page_count") val pageCount: Int? = null,
)

@Serializable
data class IssueBookRequest(
    @SerialName("book_id") val bookId: String,
    @SerialName("copy_id") val copyId: String? = null,
    @SerialName("borrower_id") val borrowerId: String,
    @SerialName("borrower_type") val borrowerType: String,
    @SerialName("borrower_name") val borrowerName: String,
)

@Serializable
data class ReturnBookRequest(
    @SerialName("issue_id") val issueId: String,
    @SerialName("return_condition") val returnCondition: String? = null,
    @SerialName("damage_notes") val damageNotes: String? = null,
)

@Serializable
data class ReserveBookRequest(
    @SerialName("book_id") val bookId: String,
)

@Serializable
data class QuickIssueRequest(
    val barcode: String,
    @SerialName("borrower_id") val borrowerId: String,
    @SerialName("borrower_type") val borrowerType: String,
    @SerialName("borrower_name") val borrowerName: String,
)

@Serializable
data class BulkReturnRequest(
    val barcodes: List<String>,
)

@Serializable
data class WaiveFineRequest(
    val reason: String,
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val color: String = "#4CAF50",
    val icon: String = "menu_book",
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val color: String? = null,
    val icon: String? = null,
)

@Serializable
data class UpdateSettingsRequest(
    @SerialName("default_loan_days") val defaultLoanDays: Int? = null,
    @SerialName("fine_per_day") val finePerDay: Double? = null,
    @SerialName("max_books_per_student") val maxBooksPerStudent: Int? = null,
    @SerialName("max_renewals") val maxRenewals: Int? = null,
    @SerialName("reservation_timeout_days") val reservationTimeoutDays: Int? = null,
    @SerialName("due_reminder_days") val dueReminderDays: Int? = null,
    @SerialName("fine_cap_enabled") val fineCapEnabled: Boolean? = null,
    @SerialName("quick_issue_enabled") val quickIssueEnabled: Boolean? = null,
    @SerialName("bulk_return_enabled") val bulkReturnEnabled: Boolean? = null,
    @SerialName("leaderboard_enabled") val leaderboardEnabled: Boolean? = null,
)

@Serializable
data class CreateAnnouncementRequest(
    val title: String,
    val message: String,
    val audience: String = "all",
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class UpdateAnnouncementRequest(
    val title: String? = null,
    val message: String? = null,
    val audience: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

@Serializable
data class CreateReadingGoalRequest(
    val goalCount: Int,
    val period: String = "monthly",
    val targetYear: Int = java.time.LocalDate.now().year,
)

@Serializable
data class CreateAcquisitionRequest(
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val reason: String? = null,
    val estimatedCost: Double? = null,
)

@Serializable
data class PostDiscussionRequest(
    val message: String,
)

// ── Response wrappers ────────────────────────────────────────────────────────

@Serializable
data class SearchResultDto(
    val books: List<LibraryBookDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
)

@Serializable
data class ReturnResultDto(
    val issueId: String,
    val bookTitle: String,
    val returnDate: String,
    val daysOverdue: Int,
    val fineAmount: Double,
    val fineCapped: Boolean,
    val returnCondition: String? = null,
)

@Serializable
data class RenewResultDto(
    val issueId: String,
    val bookTitle: String,
    val oldDueDate: String,
    val newDueDate: String,
    val renewalCount: Int,
    val maxRenewals: Int,
)

@Serializable
data class LostResultDto(
    val issueId: String,
    val bookTitle: String,
    val fineAmount: Double,
    val fineStatus: String,
    val copyStatus: String,
)

@Serializable
data class QuickIssueResultDto(
    val issueId: String,
    val bookTitle: String,
    val copyNumber: Int,
    val borrowerName: String,
    val dueDate: String,
)

@Serializable
data class BulkReturnResultDto(
    val issueId: String = "",
    val bookTitle: String = "",
    val returnDate: String,
    val fineAmount: Double = 0.0,
    val success: Boolean,
    val error: String? = null,
)

@Serializable
data class LibraryPaginatedResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
)

@Serializable
data class BulkImportResultDto(
    val totalRows: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String> = emptyList(),
)

@Serializable
data class TrendingBookDto(
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val issueCount: Int,
)

@Serializable
data class RepairCopyResultDto(
    val copyId: String,
    val bookTitle: String,
    val oldStatus: String,
    val newStatus: String,
)

@Serializable
data class ArchiveResultDto(
    val bookId: String,
    val title: String,
    val isArchived: Boolean,
)

@Serializable
data class RecommendationDto(
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val category: String? = null,
    val reason: String,
)

@Serializable
data class OnboardingResultDto(
    val categoriesSeeded: Int,
    val settingsConfigured: Boolean,
    val ready: Boolean,
)

@Serializable
data class ExportResultDto(
    val downloadUrl: String,
    val format: String,
    val rowCount: Int,
)

@Serializable
data class BulkImportRequest(
    val rows: List<CreateBookRequest>,
)

@Serializable
data class CoverUrlRequest(
    val coverUrl: String,
)

@Serializable
data class ReorderCategoriesRequest(
    val orders: List<CategoryOrderItem>,
)

@Serializable
data class CategoryOrderItem(
    val id: String,
    val displayOrder: Int,
)

@Serializable
data class CategoryStatDto(
    val category: String,
    val count: Int,
)

@Serializable
data class BorrowerSummaryDto(
    val id: String,
    val name: String,
    val type: String,
    val activeIssuesCount: Int = 0,
    val borrowingLimit: Int = 0,
    val hasOutstandingFines: Boolean = false,
)

@Serializable
data class QuickIssueSessionDto(
    val scannedBarcode: String? = null,
    val foundCopy: BookCopyDto? = null,
    val selectedBorrower: BorrowerSummaryDto? = null,
    val borrowingLimitCheck: Boolean = false,
)

@Serializable
data class FineSummaryDto(
    val outstandingCount: Int = 0,
    val outstandingAmount: Double = 0.0,
    val collectedThisMonth: Double = 0.0,
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val studentName: String,
    val booksRead: Int,
    val anonymized: Boolean = false,
)

@Serializable
data class LeaderboardDto(
    val entries: List<LeaderboardEntry> = emptyList(),
)

@Serializable
data class FeaturedBookDto(
    val book: LibraryBookDto,
    val type: String,
    val message: String? = null,
)

@Serializable
data class SetFeaturedBookRequest(
    val bookId: String,
    val type: String = "WEEK",
)

@Serializable
data class UpdateCopyRequest(
    val condition: String? = null,
    val status: String? = null,
)

@Serializable
data class AuditHashVerifyResultDto(
    val verified: Boolean,
    val brokenAt: String? = null,
    val totalEntries: Int = 0,
    val checkedEntries: Int = 0,
)

// ── Cursor-based pagination (spec §17 API Level) ─────────────────────────────

@Serializable
data class CursorPageResponse<T>(
    val data: List<T>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

object CursorCodec {
    /**
     * Encode a cursor from a timestamp + UUID pair.
     * Format: base64("timestamp|uuid")
     */
    fun encode(timestamp: java.time.Instant, id: java.util.UUID): String {
        val raw = "$timestamp|$id"
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /**
     * Decode a cursor back to a timestamp + UUID pair, or null if invalid.
     */
    fun decode(cursor: String): Pair<java.time.Instant, java.util.UUID>? {
        return runCatching {
            val raw = String(java.util.Base64.getUrlDecoder().decode(cursor))
            val parts = raw.split("|")
            if (parts.size != 2) return null
            java.time.Instant.parse(parts[0]) to java.util.UUID.fromString(parts[1])
        }.getOrNull()
    }
}
