package com.littlebridge.enrollplus.feature.library.data.remote

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.library.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class LibraryApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin (school) endpoints ────────────────────────────────────────────

    suspend fun getDashboard(token: String): NetworkResult<LibraryResponse<LibraryDashboardDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/dashboard")) { header("Authorization", "Bearer $token") } }

    suspend fun searchBooks(
        token: String, query: String, category: String?, language: String?, tags: String?, sortBy: String, availability: String, page: Int, limit: Int,
    ): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/library/books")) {
            header("Authorization", "Bearer $token")
            parameter("query", query)
            category?.let { parameter("category", it) }
            language?.let { parameter("language", it) }
            tags?.let { parameter("tags", it) }
            parameter("sortBy", sortBy)
            parameter("availability", availability)
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    suspend fun createBook(token: String, req: CreateBookRequest): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/books")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun getBook(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/books/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun updateBook(token: String, bookId: String, req: UpdateBookRequest): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall {
            client.put(getUrl("api/v1/school/library/books/$bookId")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun deleteBook(token: String, bookId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.delete(getUrl("api/v1/school/library/books/$bookId")) { header("Authorization", "Bearer $token") } }

    // ── Copies ──────────────────────────────────────────────────────────────

    suspend fun listCopies(token: String, bookId: String): NetworkResult<LibraryListResponse<BookCopyDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/books/$bookId/copies")) { header("Authorization", "Bearer $token") } }

    suspend fun addCopy(token: String, bookId: String, condition: String): NetworkResult<LibraryResponse<BookCopyDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/books/$bookId/copies")) {
                header("Authorization", "Bearer $token")
                parameter("condition", condition)
            }
        }

    // ── Issues ──────────────────────────────────────────────────────────────

    suspend fun listIssues(token: String, status: String?, page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryIssueDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/school/library/issues")) {
                header("Authorization", "Bearer $token")
                status?.let { parameter("status", it) }
                parameter("page", page)
                parameter("limit", limit)
            }
        }

    suspend fun issueBook(token: String, req: IssueBookRequest): NetworkResult<LibraryResponse<LibraryIssueDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/issues")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun returnBook(token: String, req: ReturnBookRequest): NetworkResult<LibraryResponse<ReturnResultDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/issues/${req.issueId}/return")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun renewBook(token: String, issueId: String): NetworkResult<LibraryResponse<RenewResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/issues/$issueId/renew")) { header("Authorization", "Bearer $token") } }

    suspend fun markLost(token: String, issueId: String): NetworkResult<LibraryResponse<LostResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/issues/$issueId/lost")) { header("Authorization", "Bearer $token") } }

    suspend fun payFine(token: String, issueId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/issues/$issueId/fine/pay")) { header("Authorization", "Bearer $token") } }

    suspend fun waiveFine(token: String, issueId: String, req: WaiveFineRequest): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/issues/$issueId/fine/waive")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    // ── Quick Issue & Bulk Return ───────────────────────────────────────────

    suspend fun quickIssue(token: String, req: QuickIssueRequest): NetworkResult<LibraryResponse<LibraryIssueDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/quick-issue")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun bulkReturn(token: String, req: BulkReturnRequest): NetworkResult<LibraryListResponse<BulkReturnResultDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/bulk-return")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    // ── Reservations (admin) ────────────────────────────────────────────────

    suspend fun listReservationsForBook(token: String, bookId: String): NetworkResult<LibraryListResponse<LibraryReservationDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/school/library/reservations")) {
                header("Authorization", "Bearer $token")
                parameter("bookId", bookId)
            }
        }

    suspend fun fulfillReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/reservations/$reservationId/fulfill")) { header("Authorization", "Bearer $token") } }

    // ── Categories ──────────────────────────────────────────────────────────

    suspend fun listCategories(token: String): NetworkResult<LibraryListResponse<LibraryCategoryDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/categories")) { header("Authorization", "Bearer $token") } }

    suspend fun createCategory(token: String, req: CreateCategoryRequest): NetworkResult<LibraryResponse<LibraryCategoryDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/categories")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun updateCategory(token: String, categoryId: String, req: UpdateCategoryRequest): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall {
            client.put(getUrl("api/v1/school/library/categories/$categoryId")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun deleteCategory(token: String, categoryId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.delete(getUrl("api/v1/school/library/categories/$categoryId")) { header("Authorization", "Bearer $token") } }

    // ── Settings ────────────────────────────────────────────────────────────

    suspend fun getSettings(token: String): NetworkResult<LibraryResponse<LibrarySettingsDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/settings")) { header("Authorization", "Bearer $token") } }

    suspend fun updateSettings(token: String, req: UpdateSettingsRequest): NetworkResult<LibraryResponse<LibrarySettingsDto>> =
        safeApiCall {
            client.put(getUrl("api/v1/school/library/settings")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    // ── Audit Log ───────────────────────────────────────────────────────────

    suspend fun listAuditLog(token: String, page: Int, limit: Int): NetworkResult<LibraryListResponse<LibraryAuditLogDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/school/library/audit-log")) {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
        }

    // ── Announcements ───────────────────────────────────────────────────────

    suspend fun listAnnouncements(token: String, activeOnly: Boolean): NetworkResult<LibraryListResponse<LibraryAnnouncementDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/school/library/announcements")) {
                header("Authorization", "Bearer $token")
                parameter("active", activeOnly)
            }
        }

    suspend fun createAnnouncement(token: String, req: CreateAnnouncementRequest): NetworkResult<LibraryResponse<LibraryAnnouncementDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/announcements")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun updateAnnouncement(token: String, announcementId: String, req: UpdateAnnouncementRequest): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall {
            client.put(getUrl("api/v1/school/library/announcements/$announcementId")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun deleteAnnouncement(token: String, announcementId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall {
            client.delete(getUrl("api/v1/school/library/announcements/$announcementId")) {
                header("Authorization", "Bearer $token")
            }
        }

    // ── Acquisition Requests (admin) ────────────────────────────────────────

    suspend fun listAcquisitionRequests(token: String, status: String?): NetworkResult<LibraryListResponse<LibraryAcquisitionRequestDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/school/library/acquisition-requests")) {
                header("Authorization", "Bearer $token")
                status?.let { parameter("status", it) }
            }
        }

    suspend fun updateAcquisitionStatus(token: String, requestId: String, action: String, orderLink: String? = null): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/acquisition-requests/$requestId/$action")) {
                header("Authorization", "Bearer $token")
                orderLink?.let { parameter("orderLink", it) }
            }
        }

    // ── Archive / Unarchive / Trending / Repair / Import / Export / Onboarding ──

    suspend fun archiveBook(token: String, bookId: String): NetworkResult<LibraryResponse<ArchiveResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/books/$bookId/archive")) { header("Authorization", "Bearer $token") } }

    suspend fun unarchiveBook(token: String, bookId: String): NetworkResult<LibraryResponse<ArchiveResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/books/$bookId/unarchive")) { header("Authorization", "Bearer $token") } }

    suspend fun listTrending(token: String, limit: Int = 10): NetworkResult<LibraryListResponse<TrendingBookDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/school/library/trending")) {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
            }
        }

    suspend fun listCopiesInRepair(token: String): NetworkResult<LibraryListResponse<RepairCopyResultDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/copies/repair")) { header("Authorization", "Bearer $token") } }

    suspend fun repairCopy(token: String, copyId: String): NetworkResult<LibraryResponse<RepairCopyResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/copies/$copyId/repair")) { header("Authorization", "Bearer $token") } }

    suspend fun bulkImport(token: String, req: BulkImportRequest): NetworkResult<LibraryResponse<BulkImportResultDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/books/bulk-import")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun exportCatalog(token: String, type: String = "catalog"): NetworkResult<LibraryResponse<ExportResultDto>> =
        safeApiCall { client.get(getUrl("api/v1/school/library/export")) {
            header("Authorization", "Bearer $token")
            parameter("type", type)
        } }

    suspend fun runOnboarding(token: String): NetworkResult<LibraryResponse<OnboardingResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/onboarding")) { header("Authorization", "Bearer $token") } }

    suspend fun updateBookCover(token: String, bookId: String, coverUrl: String): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/books/$bookId/cover-url")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("coverUrl" to coverUrl))
            }
        }

    suspend fun reorderCategories(token: String, orders: List<Pair<String, Int>>): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall {
            client.post(getUrl("api/v1/school/library/categories/reorder")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("orders" to orders.map { mapOf("id" to it.first, "displayOrder" to it.second) }))
            }
        }

    suspend fun convertAcquisitionToBook(token: String, requestId: String): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall { client.post(getUrl("api/v1/school/library/acquisition-requests/$requestId/convert")) { header("Authorization", "Bearer $token") } }

    // ── Parent endpoints ────────────────────────────────────────────────────

    suspend fun parentSearchBooks(token: String, query: String, category: String? = null, language: String? = null, tags: String? = null, sortBy: String = "newest", page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/parent/library/search")) {
                header("Authorization", "Bearer $token")
                parameter("query", query)
                category?.let { parameter("category", it) }
                language?.let { parameter("language", it) }
                tags?.let { parameter("tags", it) }
                parameter("sortBy", sortBy)
                parameter("page", page)
                parameter("limit", limit)
            }
        }

    suspend fun parentGetBook(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall { client.get(getUrl("api/v1/parent/library/books/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun parentGetIssuedForChild(token: String, childId: String): NetworkResult<LibraryListResponse<LibraryIssueDto>> =
        safeApiCall { client.get(getUrl("api/v1/parent/library/issued/$childId")) { header("Authorization", "Bearer $token") } }

    suspend fun parentReserveBook(token: String, req: ReserveBookRequest): NetworkResult<LibraryResponse<LibraryReservationDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/parent/library/reserve")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun parentListReservations(token: String): NetworkResult<LibraryListResponse<LibraryReservationDto>> =
        safeApiCall { client.get(getUrl("api/v1/parent/library/reservations")) { header("Authorization", "Bearer $token") } }

    suspend fun parentCancelReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.delete(getUrl("api/v1/parent/library/reservations/$reservationId")) { header("Authorization", "Bearer $token") } }

    suspend fun parentGetWishlist(token: String, childId: String): NetworkResult<LibraryListResponse<LibraryWishlistDto>> =
        safeApiCall { client.get(getUrl("api/v1/parent/library/wishlist/$childId")) { header("Authorization", "Bearer $token") } }

    suspend fun parentAddToWishlist(token: String, childId: String, bookId: String): NetworkResult<LibraryResponse<LibraryWishlistDto>> =
        safeApiCall { client.post(getUrl("api/v1/parent/library/wishlist/$childId/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun parentRemoveFromWishlist(token: String, childId: String, bookId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.delete(getUrl("api/v1/parent/library/wishlist/$childId/$bookId")) { header("Authorization", "Bearer $token") } }

    // ── Student endpoints ───────────────────────────────────────────────────

    suspend fun studentSearchBooks(token: String, query: String, category: String? = null, language: String? = null, tags: String? = null, sortBy: String = "newest", page: Int, limit: Int): NetworkResult<LibraryPaginatedResponse<LibraryBookDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/student/library/search")) {
                header("Authorization", "Bearer $token")
                parameter("query", query)
                category?.let { parameter("category", it) }
                language?.let { parameter("language", it) }
                tags?.let { parameter("tags", it) }
                parameter("sortBy", sortBy)
                parameter("page", page)
                parameter("limit", limit)
            }
        }

    suspend fun studentGetBook(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryBookDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/books/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun studentGetIssued(token: String): NetworkResult<LibraryListResponse<LibraryIssueDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/issued")) { header("Authorization", "Bearer $token") } }

    suspend fun studentGetHistory(token: String): NetworkResult<LibraryListResponse<LibraryIssueDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/history")) { header("Authorization", "Bearer $token") } }

    suspend fun studentRenewBook(token: String, issueId: String): NetworkResult<LibraryResponse<RenewResultDto>> =
        safeApiCall { client.post(getUrl("api/v1/student/library/renew/$issueId")) { header("Authorization", "Bearer $token") } }

    suspend fun studentGetProfile(token: String): NetworkResult<LibraryResponse<StudentLibraryProfileDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/profile")) { header("Authorization", "Bearer $token") } }

    suspend fun studentReserveBook(token: String, req: ReserveBookRequest): NetworkResult<LibraryResponse<LibraryReservationDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/student/library/reserve")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    suspend fun studentListReservations(token: String): NetworkResult<LibraryListResponse<LibraryReservationDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/reservations")) { header("Authorization", "Bearer $token") } }

    suspend fun studentCancelReservation(token: String, reservationId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.delete(getUrl("api/v1/student/library/reservations/$reservationId")) { header("Authorization", "Bearer $token") } }

    suspend fun studentGetWishlist(token: String): NetworkResult<LibraryListResponse<LibraryWishlistDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/wishlist")) { header("Authorization", "Bearer $token") } }

    suspend fun studentAddToWishlist(token: String, bookId: String): NetworkResult<LibraryResponse<LibraryWishlistDto>> =
        safeApiCall { client.post(getUrl("api/v1/student/library/wishlist/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun studentRemoveFromWishlist(token: String, bookId: String): NetworkResult<LibraryResponse<Unit>> =
        safeApiCall { client.delete(getUrl("api/v1/student/library/wishlist/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun studentGetBadges(token: String): NetworkResult<LibraryListResponse<LibraryBadgeDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/badges")) { header("Authorization", "Bearer $token") } }

    suspend fun studentGetReadingGoal(token: String): NetworkResult<LibraryResponse<LibraryReadingGoalDto?>> =
        safeApiCall {
            client.get(getUrl("api/v1/student/library/reading-goals")) {
                header("Authorization", "Bearer $token")
                parameter("period", "monthly")
            }
        }

    suspend fun studentSetReadingGoal(token: String, req: CreateReadingGoalRequest): NetworkResult<LibraryResponse<LibraryReadingGoalDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/student/library/reading-goals")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    // ── Student Discussions ─────────────────────────────────────────────────

    suspend fun studentGetDiscussions(token: String, bookId: String): NetworkResult<LibraryListResponse<LibraryDiscussionMessageDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/discussions/$bookId")) { header("Authorization", "Bearer $token") } }

    suspend fun studentPostDiscussion(token: String, bookId: String, req: PostDiscussionRequest): NetworkResult<LibraryResponse<LibraryDiscussionMessageDto>> =
        safeApiCall {
            client.post(getUrl("api/v1/student/library/discussions/$bookId")) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
        }

    // ── Student Acquisition Requests ────────────────────────────────────────

    suspend fun studentListAcquisitionRequests(token: String): NetworkResult<LibraryListResponse<LibraryAcquisitionRequestDto>> =
        safeApiCall { client.get(getUrl("api/v1/student/library/acquisition-requests")) { header("Authorization", "Bearer $token") } }

    // ── Student Recommendations & Trending ──────────────────────────────────

    suspend fun studentGetRecommendations(token: String, limit: Int = 10): NetworkResult<LibraryListResponse<RecommendationDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/student/library/recommendations")) {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
            }
        }

    suspend fun studentGetTrending(token: String, limit: Int = 10): NetworkResult<LibraryListResponse<TrendingBookDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/student/library/trending")) {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
            }
        }

    suspend fun studentGetAnnouncements(token: String): NetworkResult<LibraryListResponse<LibraryAnnouncementDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/student/library/announcements")) {
                header("Authorization", "Bearer $token")
            }
        }

    suspend fun studentGetStats(token: String): NetworkResult<LibraryResponse<StudentLibraryProfileDto>> =
        safeApiCall {
            client.get(getUrl("api/v1/student/library/stats")) {
                header("Authorization", "Bearer $token")
            }
        }
}
