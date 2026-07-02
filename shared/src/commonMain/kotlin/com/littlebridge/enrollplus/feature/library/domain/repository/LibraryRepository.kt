package com.littlebridge.enrollplus.feature.library.domain.repository

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.library.domain.model.*

interface LibraryRepository {
    // Admin
    suspend fun getDashboard(token: String): NetworkResult<LibraryResponse<LibraryDashboardDto>>
    suspend fun searchBooks(token: String, query: String, category: String?, language: String?, tags: String?, sortBy: String, availability: String, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>>
    suspend fun createBook(token: String, req: CreateBookRequest): NetworkResult<LibraryResponse<LibraryBookDto>>
    suspend fun getBook(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryBookDto>>
    suspend fun updateBook(token: String, bookId: String, req: UpdateBookRequest): NetworkResult<LibraryResponse<LibraryBookDto>>
    suspend fun deleteBook(token: String, bookId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun listCopies(token: String, bookId: String): NetworkResult<LibraryListResponse<BookCopyDto>>
    suspend fun addCopy(token: String, bookId: String, condition: String): NetworkResult<LibraryResponse<BookCopyDto>>
    suspend fun listIssues(token: String, status: String?, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryIssueDto>>
    suspend fun issueBook(token: String, req: IssueBookRequest): NetworkResult<LibraryResponse<LibraryIssueDto>>
    suspend fun returnBook(token: String, req: ReturnBookRequest): NetworkResult<LibraryResponse<ReturnResultDto>>
    suspend fun renewBook(token: String, issueId: String): NetworkResult<LibraryResponse<RenewResultDto>>
    suspend fun markLost(token: String, issueId: String): NetworkResult<LibraryResponse<LostResultDto>>
    suspend fun payFine(token: String, issueId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun waiveFine(token: String, issueId: String, req: WaiveFineRequest): NetworkResult<LibraryResponse<Unit>>
    suspend fun quickIssue(token: String, req: QuickIssueRequest): NetworkResult<LibraryResponse<LibraryIssueDto>>
    suspend fun bulkReturn(token: String, req: BulkReturnRequest): NetworkResult<LibraryListResponse<BulkReturnResultDto>>
    suspend fun listReservationsForBook(token: String, bookId: String): NetworkResult<LibraryListResponse<LibraryReservationDto>>
    suspend fun fulfillReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun listCategories(token: String): NetworkResult<LibraryListResponse<LibraryCategoryDto>>
    suspend fun createCategory(token: String, req: CreateCategoryRequest): NetworkResult<LibraryResponse<LibraryCategoryDto>>
    suspend fun updateCategory(token: String, categoryId: String, req: UpdateCategoryRequest): NetworkResult<LibraryResponse<Unit>>
    suspend fun deleteCategory(token: String, categoryId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun getSettings(token: String): NetworkResult<LibraryResponse<LibrarySettingsDto>>
    suspend fun updateSettings(token: String, req: UpdateSettingsRequest): NetworkResult<LibraryResponse<LibrarySettingsDto>>
    suspend fun listAuditLog(token: String, page: Int, limit: Int): NetworkResult<LibraryListResponse<LibraryAuditLogDto>>
    suspend fun listAnnouncements(token: String, activeOnly: Boolean): NetworkResult<LibraryListResponse<LibraryAnnouncementDto>>
    suspend fun createAnnouncement(token: String, req: CreateAnnouncementRequest): NetworkResult<LibraryResponse<LibraryAnnouncementDto>>
    suspend fun updateAnnouncement(token: String, announcementId: String, req: UpdateAnnouncementRequest): NetworkResult<LibraryResponse<Unit>>
    suspend fun deleteAnnouncement(token: String, announcementId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun listAcquisitionRequests(token: String, status: String?): NetworkResult<LibraryListResponse<LibraryAcquisitionRequestDto>>
    suspend fun updateAcquisitionStatus(token: String, requestId: String, action: String, orderLink: String? = null): NetworkResult<LibraryResponse<Unit>>
    suspend fun archiveBook(token: String, bookId: String): NetworkResult<LibraryResponse<ArchiveResultDto>>
    suspend fun unarchiveBook(token: String, bookId: String): NetworkResult<LibraryResponse<ArchiveResultDto>>
    suspend fun listTrending(token: String, limit: Int = 10): NetworkResult<LibraryListResponse<TrendingBookDto>>
    suspend fun listCopiesInRepair(token: String): NetworkResult<LibraryListResponse<RepairCopyResultDto>>
    suspend fun repairCopy(token: String, copyId: String): NetworkResult<LibraryResponse<RepairCopyResultDto>>
    suspend fun bulkImport(token: String, req: BulkImportRequest): NetworkResult<LibraryResponse<BulkImportResultDto>>
    suspend fun exportCatalog(token: String, type: String = "catalog"): NetworkResult<LibraryResponse<ExportResultDto>>
    suspend fun runOnboarding(token: String): NetworkResult<LibraryResponse<OnboardingResultDto>>
    suspend fun updateBookCover(token: String, bookId: String, coverUrl: String): NetworkResult<LibraryResponse<LibraryBookDto>>
    suspend fun reorderCategories(token: String, orders: List<Pair<String, Int>>): NetworkResult<LibraryResponse<Unit>>
    suspend fun convertAcquisitionToBook(token: String, requestId: String): NetworkResult<LibraryResponse<LibraryBookDto>>
    // Parent
    suspend fun parentSearchBooks(token: String, query: String, category: String? = null, language: String? = null, tags: String? = null, sortBy: String = "newest", page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>>
    suspend fun parentGetBook(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryBookDto>>
    suspend fun parentGetIssuedForChild(token: String, childId: String): NetworkResult<LibraryListResponse<LibraryIssueDto>>
    suspend fun parentReserveBook(token: String, req: ReserveBookRequest): NetworkResult<LibraryResponse<LibraryReservationDto>>
    suspend fun parentListReservations(token: String): NetworkResult<LibraryListResponse<LibraryReservationDto>>
    suspend fun parentCancelReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>>
    // Student
    suspend fun studentSearchBooks(token: String, query: String, category: String? = null, language: String? = null, tags: String? = null, sortBy: String = "newest", page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>>
    suspend fun studentGetBook(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryBookDto>>
    suspend fun studentGetIssued(token: String): NetworkResult<LibraryListResponse<LibraryIssueDto>>
    suspend fun studentGetHistory(token: String): NetworkResult<LibraryListResponse<LibraryIssueDto>>
    suspend fun studentRenewBook(token: String, issueId: String): NetworkResult<LibraryResponse<RenewResultDto>>
    suspend fun studentGetProfile(token: String): NetworkResult<LibraryResponse<StudentLibraryProfileDto>>
    suspend fun studentReserveBook(token: String, req: ReserveBookRequest): NetworkResult<LibraryResponse<LibraryReservationDto>>
    suspend fun studentListReservations(token: String): NetworkResult<LibraryListResponse<LibraryReservationDto>>
    suspend fun studentCancelReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun studentGetWishlist(token: String): NetworkResult<LibraryListResponse<LibraryWishlistDto>>
    suspend fun studentAddToWishlist(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryWishlistDto>>
    suspend fun studentRemoveFromWishlist(token: String, bookId: String): NetworkResult<LibraryResponse<Unit>>
    suspend fun studentGetBadges(token: String): NetworkResult<LibraryListResponse<LibraryBadgeDto>>
    suspend fun studentGetReadingGoal(token: String): NetworkResult<LibraryResponse<LibraryReadingGoalDto?>>
    suspend fun studentSetReadingGoal(token: String, req: CreateReadingGoalRequest): NetworkResult<LibraryResponse<LibraryReadingGoalDto>>
    suspend fun studentGetDiscussions(token: String, bookId: String): NetworkResult<LibraryListResponse<LibraryDiscussionMessageDto>>
    suspend fun studentPostDiscussion(token: String, bookId: String, req: PostDiscussionRequest): NetworkResult<LibraryResponse<LibraryDiscussionMessageDto>>
    suspend fun studentListAcquisitionRequests(token: String): NetworkResult<LibraryListResponse<LibraryAcquisitionRequestDto>>
    suspend fun studentGetRecommendations(token: String, limit: Int = 10): NetworkResult<LibraryListResponse<RecommendationDto>>
    suspend fun studentGetTrending(token: String, limit: Int = 10): NetworkResult<LibraryListResponse<TrendingBookDto>>
    suspend fun studentGetAnnouncements(token: String): NetworkResult<LibraryListResponse<LibraryAnnouncementDto>>
}
