package com.littlebridge.enrollplus.feature.library.data.local

import com.littlebridge.enrollplus.feature.library.domain.model.LibraryBookDto
import com.littlebridge.enrollplus.feature.library.domain.model.LibraryIssueDto
import com.littlebridge.enrollplus.feature.library.domain.model.LibraryDashboardDto
import com.littlebridge.enrollplus.feature.library.domain.model.LibraryCategoryDto
import com.littlebridge.enrollplus.feature.library.domain.model.LibraryAnnouncementDto
import com.littlebridge.enrollplus.feature.library.domain.model.TrendingBookDto
import kotlinx.coroutines.flow.Flow

/**
 * Local cache interface for library data.
 * Room-backed on Android/JVM, in-memory on JS/Wasm.
 */
interface LibraryLocalDataSource {

    // ── Books cache ──────────────────────────────────────────────────────────
    suspend fun saveBooks(books: List<LibraryBookDto>)
    suspend fun getBooks(): List<LibraryBookDto>
    suspend fun clearBooks()

    // ── Dashboard cache ──────────────────────────────────────────────────────
    suspend fun saveDashboard(dashboard: LibraryDashboardDto)
    suspend fun getDashboard(): LibraryDashboardDto?

    // ── Issues cache ─────────────────────────────────────────────────────────
    suspend fun saveIssues(issues: List<LibraryIssueDto>)
    suspend fun getIssues(): List<LibraryIssueDto>

    // ── Categories cache ─────────────────────────────────────────────────────
    suspend fun saveCategories(categories: List<LibraryCategoryDto>)
    suspend fun getCategories(): List<LibraryCategoryDto>

    // ── Announcements cache ──────────────────────────────────────────────────
    suspend fun saveAnnouncements(announcements: List<LibraryAnnouncementDto>)
    suspend fun getAnnouncements(): List<LibraryAnnouncementDto>

    // ── Trending cache ───────────────────────────────────────────────────────
    suspend fun saveTrending(trending: List<TrendingBookDto>)
    suspend fun getTrending(): List<TrendingBookDto>

    // ── Pending actions queue (offline replay) ───────────────────────────────
    suspend fun enqueueAction(action: LibraryPendingAction): Long
    suspend fun getPendingActions(): List<LibraryPendingAction>
    suspend fun deleteAction(id: Long)
    suspend fun clearAll()
}

/**
 * A queued library action to replay when back online.
 */
data class LibraryPendingAction(
    val id: Long = 0,
    val type: String,
    val bookId: String? = null,
    val issueId: String? = null,
    val payloadJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
