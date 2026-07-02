package com.littlebridge.enrollplus.feature.library.data.repository

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.library.data.local.LibraryLocalDataSource
import com.littlebridge.enrollplus.feature.library.data.local.LibraryPendingAction
import com.littlebridge.enrollplus.feature.library.data.remote.LibraryApi
import com.littlebridge.enrollplus.feature.library.domain.model.*
import com.littlebridge.enrollplus.feature.library.domain.repository.LibraryRepository

class LibraryRepositoryImpl(
    private val api: LibraryApi,
    private val local: LibraryLocalDataSource? = null,
) : LibraryRepository {
    override suspend fun getDashboard(token: String): NetworkResult<LibraryResponse<LibraryDashboardDto>> {
        val result = api.getDashboard(token)
        if (result is NetworkResult.Success) result.data.data?.let { local?.saveDashboard(it) }
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getDashboard()
            if (cached != null) return NetworkResult.Success(LibraryResponse(data = cached, message = "from cache"))
        }
        return result
    }
    override suspend fun searchBooks(token: String, query: String, category: String?, language: String?, tags: String?, sortBy: String, availability: String, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>> {
        val result = api.searchBooks(token, query, category, language, tags, sortBy, availability, page, limit)
        if (result is NetworkResult.Success && page == 1) local?.saveBooks(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getBooks().orEmpty()
            if (cached.isNotEmpty() && page == 1) return NetworkResult.Success(LibraryPaginatedResponse(data = cached, total = cached.size))
        }
        return result
    }
    override suspend fun createBook(token: String, req: CreateBookRequest) = api.createBook(token, req)
    override suspend fun getBook(token: String, bookId: String) = api.getBook(token, bookId)
    override suspend fun updateBook(token: String, bookId: String, req: UpdateBookRequest) = api.updateBook(token, bookId, req)
    override suspend fun deleteBook(token: String, bookId: String) = api.deleteBook(token, bookId)
    override suspend fun listCopies(token: String, bookId: String) = api.listCopies(token, bookId)
    override suspend fun addCopy(token: String, bookId: String, condition: String) = api.addCopy(token, bookId, condition)
    override suspend fun listIssues(token: String, status: String?, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryIssueDto>> {
        val result = api.listIssues(token, status, page, limit)
        if (result is NetworkResult.Success && page == 1) local?.saveIssues(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getIssues().orEmpty()
            if (cached.isNotEmpty() && page == 1) return NetworkResult.Success(LibraryPaginatedResponse(data = cached, total = cached.size))
        }
        return result
    }
    override suspend fun issueBook(token: String, req: IssueBookRequest) = api.issueBook(token, req)
    override suspend fun returnBook(token: String, req: ReturnBookRequest) = api.returnBook(token, req)
    override suspend fun renewBook(token: String, issueId: String) = api.renewBook(token, issueId)
    override suspend fun markLost(token: String, issueId: String) = api.markLost(token, issueId)
    override suspend fun payFine(token: String, issueId: String) = api.payFine(token, issueId)
    override suspend fun waiveFine(token: String, issueId: String, req: WaiveFineRequest) = api.waiveFine(token, issueId, req)
    override suspend fun quickIssue(token: String, req: QuickIssueRequest) = api.quickIssue(token, req)
    override suspend fun bulkReturn(token: String, req: BulkReturnRequest) = api.bulkReturn(token, req)
    override suspend fun listReservationsForBook(token: String, bookId: String) = api.listReservationsForBook(token, bookId)
    override suspend fun fulfillReservation(token: String, reservationId: String) = api.fulfillReservation(token, reservationId)
    override suspend fun listCategories(token: String): NetworkResult<LibraryListResponse<LibraryCategoryDto>> {
        val result = api.listCategories(token)
        if (result is NetworkResult.Success) local?.saveCategories(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getCategories().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }
    override suspend fun createCategory(token: String, req: CreateCategoryRequest) = api.createCategory(token, req)
    override suspend fun updateCategory(token: String, categoryId: String, req: UpdateCategoryRequest) = api.updateCategory(token, categoryId, req)
    override suspend fun deleteCategory(token: String, categoryId: String) = api.deleteCategory(token, categoryId)
    override suspend fun getSettings(token: String) = api.getSettings(token)
    override suspend fun updateSettings(token: String, req: UpdateSettingsRequest) = api.updateSettings(token, req)
    override suspend fun listAuditLog(token: String, page: Int, limit: Int) = api.listAuditLog(token, page, limit)
    override suspend fun listAnnouncements(token: String, activeOnly: Boolean): NetworkResult<LibraryListResponse<LibraryAnnouncementDto>> {
        val result = api.listAnnouncements(token, activeOnly)
        if (result is NetworkResult.Success) local?.saveAnnouncements(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getAnnouncements().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }
    override suspend fun createAnnouncement(token: String, req: CreateAnnouncementRequest) = api.createAnnouncement(token, req)
    override suspend fun updateAnnouncement(token: String, announcementId: String, req: UpdateAnnouncementRequest) = api.updateAnnouncement(token, announcementId, req)
    override suspend fun deleteAnnouncement(token: String, announcementId: String) = api.deleteAnnouncement(token, announcementId)
    override suspend fun listAcquisitionRequests(token: String, status: String?) = api.listAcquisitionRequests(token, status)
    override suspend fun updateAcquisitionStatus(token: String, requestId: String, action: String, orderLink: String?) = api.updateAcquisitionStatus(token, requestId, action, orderLink)
    override suspend fun archiveBook(token: String, bookId: String) = api.archiveBook(token, bookId)
    override suspend fun unarchiveBook(token: String, bookId: String) = api.unarchiveBook(token, bookId)
    override suspend fun listTrending(token: String, limit: Int): NetworkResult<LibraryListResponse<TrendingBookDto>> {
        val result = api.listTrending(token, limit)
        if (result is NetworkResult.Success) local?.saveTrending(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getTrending().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }
    override suspend fun listCopiesInRepair(token: String) = api.listCopiesInRepair(token)
    override suspend fun repairCopy(token: String, copyId: String) = api.repairCopy(token, copyId)
    override suspend fun bulkImport(token: String, req: BulkImportRequest) = api.bulkImport(token, req)
    override suspend fun exportCatalog(token: String, type: String) = api.exportCatalog(token, type)
    override suspend fun runOnboarding(token: String) = api.runOnboarding(token)
    override suspend fun updateBookCover(token: String, bookId: String, coverUrl: String) = api.updateBookCover(token, bookId, coverUrl)
    override suspend fun reorderCategories(token: String, orders: List<Pair<String, Int>>) = api.reorderCategories(token, orders)
    override suspend fun convertAcquisitionToBook(token: String, requestId: String) = api.convertAcquisitionToBook(token, requestId)
    // Parent
    override suspend fun parentSearchBooks(token: String, query: String, category: String?, language: String?, tags: String?, sortBy: String, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>> {
        val result = api.parentSearchBooks(token, query, category, language, tags, sortBy, page, limit)
        if (result is NetworkResult.Success && page == 1) local?.saveBooks(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getBooks().orEmpty()
            if (cached.isNotEmpty() && page == 1) return NetworkResult.Success(LibraryPaginatedResponse(data = cached, total = cached.size))
        }
        return result
    }
    override suspend fun parentGetBook(token: String, bookId: String) = api.parentGetBook(token, bookId)
    override suspend fun parentGetIssuedForChild(token: String, childId: String): NetworkResult<LibraryListResponse<LibraryIssueDto>> {
        val result = api.parentGetIssuedForChild(token, childId)
        if (result is NetworkResult.Success) local?.saveIssues(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getIssues().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }
    override suspend fun parentReserveBook(token: String, req: ReserveBookRequest): NetworkResult<LibraryResponse<LibraryReservationDto>> {
        val result = api.parentReserveBook(token, req)
        if (result is NetworkResult.ConnectionError) local?.enqueueAction(LibraryPendingAction(type = "parent_reserve", bookId = req.bookId))
        return result
    }
    override suspend fun parentListReservations(token: String) = api.parentListReservations(token)
    override suspend fun parentCancelReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>> {
        val result = api.parentCancelReservation(token, reservationId)
        if (result is NetworkResult.ConnectionError) local?.enqueueAction(LibraryPendingAction(type = "parent_cancel_reservation", issueId = reservationId))
        return result
    }
    // Student
    override suspend fun studentSearchBooks(token: String, query: String, category: String?, language: String?, tags: String?, sortBy: String, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>> {
        val result = api.studentSearchBooks(token, query, category, language, tags, sortBy, page, limit)
        if (result is NetworkResult.Success && page == 1) local?.saveBooks(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getBooks().orEmpty()
            if (cached.isNotEmpty() && page == 1) return NetworkResult.Success(LibraryPaginatedResponse(data = cached, total = cached.size))
        }
        return result
    }
    override suspend fun studentGetBook(token: String, bookId: String) = api.studentGetBook(token, bookId)
    override suspend fun studentGetIssued(token: String): NetworkResult<LibraryListResponse<LibraryIssueDto>> {
        val result = api.studentGetIssued(token)
        if (result is NetworkResult.Success) local?.saveIssues(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getIssues().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }
    override suspend fun studentGetHistory(token: String) = api.studentGetHistory(token)
    override suspend fun studentRenewBook(token: String, issueId: String) = api.studentRenewBook(token, issueId)
    override suspend fun studentGetProfile(token: String) = api.studentGetProfile(token)
    override suspend fun studentReserveBook(token: String, req: ReserveBookRequest): NetworkResult<LibraryResponse<LibraryReservationDto>> {
        val result = api.studentReserveBook(token, req)
        if (result is NetworkResult.ConnectionError) local?.enqueueAction(LibraryPendingAction(type = "student_reserve", bookId = req.bookId))
        return result
    }
    override suspend fun studentListReservations(token: String) = api.studentListReservations(token)
    override suspend fun studentCancelReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>> {
        val result = api.studentCancelReservation(token, reservationId)
        if (result is NetworkResult.ConnectionError) local?.enqueueAction(LibraryPendingAction(type = "student_cancel_reservation", issueId = reservationId))
        return result
    }
    override suspend fun studentGetWishlist(token: String) = api.studentGetWishlist(token)
    override suspend fun studentAddToWishlist(token: String, bookId: String) = api.studentAddToWishlist(token, bookId)
    override suspend fun studentRemoveFromWishlist(token: String, bookId: String) = api.studentRemoveFromWishlist(token, bookId)
    override suspend fun studentGetBadges(token: String) = api.studentGetBadges(token)
    override suspend fun studentGetReadingGoal(token: String) = api.studentGetReadingGoal(token)
    override suspend fun studentSetReadingGoal(token: String, req: CreateReadingGoalRequest) = api.studentSetReadingGoal(token, req)
    override suspend fun studentGetDiscussions(token: String, bookId: String) = api.studentGetDiscussions(token, bookId)
    override suspend fun studentPostDiscussion(token: String, bookId: String, req: PostDiscussionRequest) = api.studentPostDiscussion(token, bookId, req)
    override suspend fun studentListAcquisitionRequests(token: String) = api.studentListAcquisitionRequests(token)
    override suspend fun studentGetRecommendations(token: String, limit: Int) = api.studentGetRecommendations(token, limit)
    override suspend fun studentGetTrending(token: String, limit: Int): NetworkResult<LibraryListResponse<TrendingBookDto>> {
        val result = api.studentGetTrending(token, limit)
        if (result is NetworkResult.Success) local?.saveTrending(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getTrending().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }
    override suspend fun studentGetAnnouncements(token: String): NetworkResult<LibraryListResponse<LibraryAnnouncementDto>> {
        val result = api.studentGetAnnouncements(token)
        if (result is NetworkResult.Success) local?.saveAnnouncements(result.data.data)
        else if (result is NetworkResult.ConnectionError) {
            val cached = local?.getAnnouncements().orEmpty()
            if (cached.isNotEmpty()) return NetworkResult.Success(LibraryListResponse(data = cached))
        }
        return result
    }

    // ── Action queue helpers ───────────────────────────────────────────────────
    suspend fun getPendingActions(): List<LibraryPendingAction> = local?.getPendingActions() ?: emptyList()
    suspend fun deletePendingAction(id: Long) { local?.deleteAction(id) }
}
