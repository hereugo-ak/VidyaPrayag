package com.littlebridge.enrollplus.feature.library.data.local

import com.littlebridge.enrollplus.feature.library.domain.model.*
import kotlinx.serialization.json.Json

class RoomLibraryLocalDataSource(
    private val bookDao: LibraryBookDao,
    private val cacheDao: LibraryCacheDao,
    private val actionDao: LibraryPendingActionDao,
) : LibraryLocalDataSource {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    override suspend fun saveBooks(books: List<LibraryBookDto>) {
        bookDao.insertBooks(books.map { it.toEntity() })
    }

    override suspend fun getBooks(): List<LibraryBookDto> {
        return bookDao.getAllBooksOnce().map { it.toDomain() }
    }

    override suspend fun clearBooks() = bookDao.clearBooks()

    override suspend fun saveDashboard(dashboard: LibraryDashboardDto) {
        cacheDao.put(LibraryCacheEntity("dashboard", json.encodeToString(dashboard), System.currentTimeMillis()))
    }

    override suspend fun getDashboard(): LibraryDashboardDto? {
        return cacheDao.get("dashboard")?.let { json.decodeFromString<LibraryDashboardDto>(it.dataJson) }
    }

    override suspend fun saveIssues(issues: List<LibraryIssueDto>) {
        cacheDao.put(LibraryCacheEntity("issues", json.encodeToString(issues), System.currentTimeMillis()))
    }

    override suspend fun getIssues(): List<LibraryIssueDto> {
        return cacheDao.get("issues")?.let { json.decodeFromString<List<LibraryIssueDto>>(it.dataJson) } ?: emptyList()
    }

    override suspend fun saveCategories(categories: List<LibraryCategoryDto>) {
        cacheDao.put(LibraryCacheEntity("categories", json.encodeToString(categories), System.currentTimeMillis()))
    }

    override suspend fun getCategories(): List<LibraryCategoryDto> {
        return cacheDao.get("categories")?.let { json.decodeFromString<List<LibraryCategoryDto>>(it.dataJson) } ?: emptyList()
    }

    override suspend fun saveAnnouncements(announcements: List<LibraryAnnouncementDto>) {
        cacheDao.put(LibraryCacheEntity("announcements", json.encodeToString(announcements), System.currentTimeMillis()))
    }

    override suspend fun getAnnouncements(): List<LibraryAnnouncementDto> {
        return cacheDao.get("announcements")?.let { json.decodeFromString<List<LibraryAnnouncementDto>>(it.dataJson) } ?: emptyList()
    }

    override suspend fun saveTrending(trending: List<TrendingBookDto>) {
        cacheDao.put(LibraryCacheEntity("trending", json.encodeToString(trending), System.currentTimeMillis()))
    }

    override suspend fun getTrending(): List<TrendingBookDto> {
        return cacheDao.get("trending")?.let { json.decodeFromString<List<TrendingBookDto>>(it.dataJson) } ?: emptyList()
    }

    override suspend fun enqueueAction(action: LibraryPendingAction): Long {
        return actionDao.insert(
            LibraryPendingActionEntity(
                type = action.type,
                bookId = action.bookId,
                issueId = action.issueId,
                payloadJson = action.payloadJson,
                createdAt = action.createdAt,
            )
        )
    }

    override suspend fun getPendingActions(): List<LibraryPendingAction> {
        return actionDao.getAll().map {
            LibraryPendingAction(
                id = it.id,
                type = it.type,
                bookId = it.bookId,
                issueId = it.issueId,
                payloadJson = it.payloadJson,
                createdAt = it.createdAt,
            )
        }
    }

    override suspend fun deleteAction(id: Long) = actionDao.delete(id)

    override suspend fun clearAll() {
        bookDao.clearBooks()
        cacheDao.clearAll()
        actionDao.clearAll()
    }

    private fun LibraryBookDto.toEntity() = LibraryBookEntity(
        id = id,
        title = title,
        author = author,
        category = category,
        totalCopies = totalCopies,
        availableCopies = availableCopies,
        coverUrl = coverUrl,
        isbn = isbn,
        language = language,
        isArchived = isArchived,
        dataJson = json.encodeToString(this),
    )

    private fun LibraryBookEntity.toDomain(): LibraryBookDto {
        return json.decodeFromString<LibraryBookDto>(dataJson)
    }
}
