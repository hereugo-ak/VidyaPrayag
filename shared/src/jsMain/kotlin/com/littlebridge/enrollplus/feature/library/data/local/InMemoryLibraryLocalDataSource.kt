package com.littlebridge.enrollplus.feature.library.data.local

import com.littlebridge.enrollplus.feature.library.domain.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryLibraryLocalDataSource : LibraryLocalDataSource {
    private val mutex = Mutex()
    private var books: List<LibraryBookDto> = emptyList()
    private var dashboard: LibraryDashboardDto? = null
    private var issues: List<LibraryIssueDto> = emptyList()
    private var categories: List<LibraryCategoryDto> = emptyList()
    private var announcements: List<LibraryAnnouncementDto> = emptyList()
    private var trending: List<TrendingBookDto> = emptyList()
    private val actionQueue = mutableListOf<LibraryPendingAction>()
    private var actionIdCounter = 0L

    override suspend fun saveBooks(books: List<LibraryBookDto>) = mutex.withLock { this.books = books }
    override suspend fun getBooks(): List<LibraryBookDto> = mutex.withLock { books }
    override suspend fun clearBooks() = mutex.withLock { books = emptyList() }

    override suspend fun saveDashboard(dashboard: LibraryDashboardDto) = mutex.withLock { this.dashboard = dashboard }
    override suspend fun getDashboard(): LibraryDashboardDto? = mutex.withLock { dashboard }

    override suspend fun saveIssues(issues: List<LibraryIssueDto>) = mutex.withLock { this.issues = issues }
    override suspend fun getIssues(): List<LibraryIssueDto> = mutex.withLock { issues }

    override suspend fun saveCategories(categories: List<LibraryCategoryDto>) = mutex.withLock { this.categories = categories }
    override suspend fun getCategories(): List<LibraryCategoryDto> = mutex.withLock { categories }

    override suspend fun saveAnnouncements(announcements: List<LibraryAnnouncementDto>) = mutex.withLock { this.announcements = announcements }
    override suspend fun getAnnouncements(): List<LibraryAnnouncementDto> = mutex.withLock { announcements }

    override suspend fun saveTrending(trending: List<TrendingBookDto>) = mutex.withLock { this.trending = trending }
    override suspend fun getTrending(): List<TrendingBookDto> = mutex.withLock { trending }

    override suspend fun enqueueAction(action: LibraryPendingAction): Long = mutex.withLock {
        val id = ++actionIdCounter
        actionQueue.add(action.copy(id = id))
        id
    }

    override suspend fun getPendingActions(): List<LibraryPendingAction> = mutex.withLock { actionQueue.toList() }

    override suspend fun deleteAction(id: Long) = mutex.withLock { actionQueue.removeAll { it.id == id } }

    override suspend fun clearAll() = mutex.withLock {
        books = emptyList()
        dashboard = null
        issues = emptyList()
        categories = emptyList()
        announcements = emptyList()
        trending = emptyList()
        actionQueue.clear()
    }
}
