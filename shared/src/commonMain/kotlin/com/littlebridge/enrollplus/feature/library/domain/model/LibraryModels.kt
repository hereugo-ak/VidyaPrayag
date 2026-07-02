/*
 * File: LibraryModels.kt
 * Module: feature.library.domain.model
 *
 * Serializable DTOs for the Library Management feature.
 * Mirrors server-side DTOs from feature/library/LibraryRouting.kt exactly
 * (same @SerialName), so the same JSON decodes on both sides.
 *
 * Server routes mirrored:
 *   Admin:  /api/v1/school/library/...
 *   Parent: /api/v1/parent/library/...
 *   Student: /api/v1/student/library/...
 */
package com.littlebridge.enrollplus.feature.library.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Core DTOs
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryBookDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("total_copies") val totalCopies: Int = 1,
    @SerialName("available_copies") val availableCopies: Int = 1,
    @SerialName("shelf_location") val shelfLocation: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("replacement_cost") val replacementCost: Double? = null,
    @SerialName("series_name") val seriesName: String? = null,
    @SerialName("series_number") val seriesNumber: Int? = null,
    val language: String = "en",
    @SerialName("is_archived") val isArchived: Boolean = false,
    val synopsis: String? = null,
    @SerialName("page_count") val pageCount: Int? = null,
    @SerialName("is_trending") val isTrending: Boolean = false,
    @SerialName("trend_issue_count") val trendIssueCount: Int = 0,
)

@Serializable
data class BookCopyDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("copy_number") val copyNumber: Int,
    val barcode: String? = null,
    val condition: String = "new",       // new | good | fair | poor | damaged
    val status: String = "available",    // available | issued | lost | repair
)

@Serializable
data class LibraryIssueDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("copy_id") val copyId: String? = null,
    @SerialName("copy_number") val copyNumber: Int? = null,
    @SerialName("borrower_id") val borrowerId: String,
    @SerialName("borrower_type") val borrowerType: String,   // student | teacher
    @SerialName("borrower_name") val borrowerName: String,
    @SerialName("issue_date") val issueDate: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("return_date") val returnDate: String? = null,
    @SerialName("return_condition") val returnCondition: String? = null,
    @SerialName("damage_notes") val damageNotes: String? = null,
    @SerialName("renewal_count") val renewalCount: Int = 0,
    @SerialName("fine_amount") val fineAmount: Double = 0.0,
    @SerialName("fine_status") val fineStatus: String = "none",  // none | pending | paid | waived
    @SerialName("fine_paid_at") val finePaidAt: String? = null,
    @SerialName("fine_waived_reason") val fineWaivedReason: String? = null,
    val status: String = "issued",      // issued | returned | lost
)

@Serializable
data class LibraryReservationDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("reserved_by") val reservedBy: String,
    @SerialName("reserved_by_name") val reservedByName: String,
    @SerialName("reserved_by_type") val reservedByType: String = "student", // student | teacher | parent
    val status: String = "pending",     // pending | notified | fulfilled | cancelled
    @SerialName("created_at") val createdAt: String,
    @SerialName("fulfilled_at") val fulfilledAt: String? = null,
    @SerialName("waitlist_position") val waitlistPosition: Int? = null,
)

@Serializable
data class LibraryDashboardDto(
    @SerialName("total_books") val totalBooks: Int = 0,
    @SerialName("total_copies") val totalCopies: Int = 0,
    @SerialName("available_copies") val availableCopies: Int = 0,
    @SerialName("issued_copies") val issuedCopies: Int = 0,
    @SerialName("overdue_books") val overdueBooks: Int = 0,
    @SerialName("active_reservations") val activeReservations: Int = 0,
    @SerialName("lost_books") val lostBooks: Int = 0,
    @SerialName("damaged_books") val damagedBooks: Int = 0,
    @SerialName("outstanding_fines_count") val outstandingFinesCount: Int = 0,
    @SerialName("outstanding_fines_amount") val outstandingFinesAmount: Double = 0.0,
    @SerialName("fines_collected_this_month") val finesCollectedThisMonth: Double = 0.0,
)

@Serializable
data class ReturnResultDto(
    @SerialName("issue_id") val issueId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("return_date") val returnDate: String,
    @SerialName("days_overdue") val daysOverdue: Int = 0,
    @SerialName("fine_amount") val fineAmount: Double = 0.0,
    @SerialName("fine_capped") val fineCapped: Boolean = false,
    @SerialName("return_condition") val returnCondition: String? = null,
)

@Serializable
data class RenewResultDto(
    @SerialName("issue_id") val issueId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("old_due_date") val oldDueDate: String,
    @SerialName("new_due_date") val newDueDate: String,
    @SerialName("renewal_count") val renewalCount: Int = 0,
    @SerialName("max_renewals") val maxRenewals: Int = 2,
)

@Serializable
data class LostResultDto(
    @SerialName("issue_id") val issueId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("fine_amount") val fineAmount: Double = 0.0,
    @SerialName("fine_status") val fineStatus: String = "pending",
    @SerialName("copy_status") val copyStatus: String = "lost",
)

@Serializable
data class SearchResultDto(
    val books: List<LibraryBookDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

@Serializable
data class BulkImportResultDto(
    @SerialName("total_rows") val totalRows: Int = 0,
    @SerialName("success_count") val successCount: Int = 0,
    @SerialName("failure_count") val failureCount: Int = 0,
    val warnings: List<ImportWarning> = emptyList(),
    val errors: List<ImportError> = emptyList(),
)

@Serializable
data class ImportWarning(
    val row: Int,
    val isbn: String? = null,
    val warning: String,
)

@Serializable
data class ImportError(
    val row: Int,
    val error: String,
)

@Serializable
data class FineSummaryDto(
    @SerialName("outstanding_count") val outstandingCount: Int = 0,
    @SerialName("outstanding_amount") val outstandingAmount: Double = 0.0,
    @SerialName("collected_this_month") val collectedThisMonth: Double = 0.0,
    @SerialName("waived_this_month") val waivedThisMonth: Double = 0.0,
)

@Serializable
data class LibraryCategoryDto(
    val id: String,
    val name: String,
    val color: String = "#6366f1",
    val icon: String = "book",
    @SerialName("display_order") val displayOrder: Int = 0,
)

@Serializable
data class LibrarySettingsDto(
    @SerialName("default_loan_days") val defaultLoanDays: Int = 14,
    @SerialName("fine_per_day") val finePerDay: Double = 1.0,
    @SerialName("max_books_per_student") val maxBooksPerStudent: Int = 3,
    @SerialName("max_renewals") val maxRenewals: Int = 2,
    @SerialName("reservation_timeout_days") val reservationTimeoutDays: Int = 7,
    @SerialName("due_reminder_days") val dueReminderDays: Int = 2,
    @SerialName("fine_cap_enabled") val fineCapEnabled: Boolean = true,
    @SerialName("quick_issue_enabled") val quickIssueEnabled: Boolean = true,
    @SerialName("bulk_return_enabled") val bulkReturnEnabled: Boolean = true,
    @SerialName("leaderboard_enabled") val leaderboardEnabled: Boolean = false,
)

@Serializable
data class LibraryAuditLogDto(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_name") val actorName: String = "",
    val action: String = "",
    @SerialName("entity_type") val entityType: String = "",
    @SerialName("entity_id") val entityId: String? = null,
    val metadata: String? = null,       // JSON string
    @SerialName("previous_state") val previousState: String? = null,
    @SerialName("new_state") val newState: String? = null,
    val hash: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class LibraryAnnouncementDto(
    val id: String,
    val title: String,
    val message: String,
    val audience: String = "all",       // all | students | parents
    @SerialName("created_by_name") val createdByName: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
// Student Profile & Stats
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class StudentLibraryProfileDto(
    @SerialName("total_books_read") val totalBooksRead: Int = 0,
    @SerialName("currently_issued") val currentlyIssued: Int = 0,
    @SerialName("overdue_count") val overdueCount: Int = 0,
    @SerialName("outstanding_fine") val outstandingFine: Double = 0.0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("badges_earned") val badgesEarned: Int = 0,
    @SerialName("max_borrowing_limit") val maxBorrowingLimit: Int = 0,
    @SerialName("most_read_categories") val mostReadCategories: List<CategoryStatDto> = emptyList(),
)

@Serializable
data class ReadingStatsDto(
    @SerialName("monthly_counts") val monthlyCounts: Map<String, Int> = emptyMap(),
    @SerialName("category_distribution") val categoryDistribution: Map<String, Int> = emptyMap(),
    @SerialName("daily_activity") val dailyActivity: Map<String, Int> = emptyMap(),
)

@Serializable
data class StreakDto(
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("daily_activity") val dailyActivity: Map<String, Int> = emptyMap(),
)

// ─────────────────────────────────────────────────────────────────────────────
// Wishlist
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryWishlistDto(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("book_author") val bookAuthor: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("available_copies") val availableCopies: Int = 0,
    @SerialName("added_at") val addedAt: String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
// Reading Goals
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryReadingGoalDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("goal_count") val goalCount: Int,
    val period: String,                 // monthly | quarterly | yearly
    @SerialName("target_year") val targetYear: Int,
    @SerialName("books_read") val booksRead: Int = 0,
    @SerialName("progress_percentage") val progressPercentage: Double = 0.0,
    @SerialName("is_achieved") val isAchieved: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// Acquisition Requests
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryAcquisitionRequestDto(
    val id: String,
    @SerialName("requested_by_name") val requestedByName: String = "",
    @SerialName("requested_by_type") val requestedByType: String = "student",
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val reason: String? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null,
    val status: String = "pending",     // pending | approved | rejected | ordered | received
    @SerialName("order_link") val orderLink: String? = null,
    @SerialName("converted_book_id") val convertedBookId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
// Badges & Leaderboard
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryBadgeDto(
    @SerialName("badge_type") val badgeType: String,
    @SerialName("badge_name") val badgeName: String,
    @SerialName("badge_icon") val badgeIcon: String,
    @SerialName("earned_at") val earnedAt: String? = null,
    @SerialName("is_earned") val isEarned: Boolean = false,
)

@Serializable
data class LeaderboardDto(
    val entries: List<LeaderboardEntry> = emptyList(),
)

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    @SerialName("student_name") val studentName: String,
    @SerialName("books_read") val booksRead: Int,
    val anonymized: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// Discussions
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryDiscussionMessageDto(
    val id: String,
    @SerialName("student_name") val studentName: String,
    val message: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// Featured Book
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class FeaturedBookDto(
    val book: LibraryBookDto,
    val type: String,                   // WEEK | MONTH
    val message: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// Quick Issue & Bulk Return
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class QuickIssueResultDto(
    @SerialName("issue_id") val issueId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("copy_number") val copyNumber: Int,
    @SerialName("borrower_name") val borrowerName: String,
    @SerialName("due_date") val dueDate: String,
)

@Serializable
data class BulkReturnResultDto(
    @SerialName("issue_id") val issueId: String,
    @SerialName("book_title") val bookTitle: String,
    @SerialName("return_date") val returnDate: String,
    @SerialName("fine_amount") val fineAmount: Double = 0.0,
    val success: Boolean = true,
    val error: String? = null,
)

@Serializable
data class BulkReturnSessionDto(
    @SerialName("session_id") val sessionId: String,
    val items: List<BulkReturnResultDto> = emptyList(),
    @SerialName("total_returned") val totalReturned: Int = 0,
    @SerialName("total_fines") val totalFines: Double = 0.0,
)

// ─────────────────────────────────────────────────────────────────────────────
// Request Models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CreateBookRequest(
    val isbn: String? = null,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
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
    @SerialName("borrower_type") val borrowerType: String,   // student | teacher
    @SerialName("borrower_name") val borrowerName: String,
)

@Serializable
data class ReturnBookRequest(
    @SerialName("issue_id") val issueId: String,
    @SerialName("return_condition") val returnCondition: String? = null,  // good | fair | damaged
    @SerialName("damage_notes") val damageNotes: String? = null,
)

@Serializable
data class ReserveBookRequest(
    @SerialName("book_id") val bookId: String,
)

@Serializable
data class WaiveFineRequest(
    val reason: String,
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val color: String = "#6366f1",
    val icon: String = "book",
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val color: String? = null,
    val icon: String? = null,
)

@Serializable
data class ReorderCategoriesRequest(
    val orders: List<CategoryOrder>,
)

@Serializable
data class CategoryOrder(
    val id: String,
    @SerialName("display_order") val displayOrder: Int,
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
data class CreateAcquisitionRequest(
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val reason: String? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null,
)

@Serializable
data class CreateReadingGoalRequest(
    @SerialName("goal_count") val goalCount: Int,
    val period: String,                 // monthly | quarterly | yearly
    @SerialName("target_year") val targetYear: Int,
)

@Serializable
data class UpdateReadingGoalRequest(
    @SerialName("goal_count") val goalCount: Int? = null,
)

@Serializable
data class PostDiscussionRequest(
    val message: String,
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

// ─────────────────────────────────────────────────────────────────────────────
// Response Wrappers
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LibraryResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class LibraryListResponse<T>(
    val success: Boolean = true,
    val data: List<T> = emptyList(),
)

@Serializable
data class LibraryPaginatedResponse<T>(
    val success: Boolean = true,
    val data: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

// ─────────────────────────────────────────────────────────────────────────────
// Trending / Recommendations / Archive / Repair / Import / Export / Onboarding
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TrendingBookDto(
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val issueCount: Int,
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
data class BulkImportRequest(
    val rows: List<CreateBookRequest> = emptyList(),
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
data class OnboardingResultDto(
    val categoriesSeeded: Int = 0,
    val settingsConfigured: Boolean = false,
    val ready: Boolean = false,
)

@Serializable
data class ExportResultDto(
    val downloadUrl: String,
    val format: String,
    val rowCount: Int,
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
    @SerialName("active_issues_count") val activeIssuesCount: Int = 0,
    @SerialName("borrowing_limit") val borrowingLimit: Int = 0,
    @SerialName("has_outstanding_fines") val hasOutstandingFines: Boolean = false,
)

@Serializable
data class QuickIssueSessionDto(
    @SerialName("scanned_barcode") val scannedBarcode: String? = null,
    @SerialName("found_copy") val foundCopy: BookCopyDto? = null,
    @SerialName("selected_borrower") val selectedBorrower: BorrowerSummaryDto? = null,
    @SerialName("borrowing_limit_check") val borrowingLimitCheck: Boolean = false,
)
