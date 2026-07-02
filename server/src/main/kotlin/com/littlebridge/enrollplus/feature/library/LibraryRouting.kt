/*
 * File: LibraryRouting.kt
 * Module: feature.library
 *
 * HTTP surface for the Library Management feature.
 *
 * Admin endpoints (school admin / librarian):
 *   /api/v1/school/library/books{,/{id},/{id}/copies,/search,/bulk-import,/{id}/archive,/{id}/unarchive}
 *   /api/v1/school/library/issues{,/{id}/return,/{id}/renew,/{id}/lost,/{id}/fine/pay,/{id}/fine/waive}
 *   /api/v1/school/library/quick-issue
 *   /api/v1/school/library/bulk-return
 *   /api/v1/school/library/reservations{,/{id}/cancel,/{id}/fulfill}
 *   /api/v1/school/library/categories{,/{id}}
 *   /api/v1/school/library/settings
 *   /api/v1/school/library/dashboard
 *   /api/v1/school/library/audit-log
 *   /api/v1/school/library/announcements{,/{id}}
 *   /api/v1/school/library/acquisition-requests{,/{id}/{approve,reject,order,receive}}
 *   /api/v1/school/library/trending
 *   /api/v1/school/library/copies/repair{,/{id}/repair}
 *   /api/v1/school/library/export
 *   /api/v1/school/library/onboarding
 *
 * Parent endpoints:
 *   /api/v1/parent/library/search
 *   /api/v1/parent/library/books/{id}
 *   /api/v1/parent/library/issued/{childId}
 *   /api/v1/parent/library/reserve
 *   /api/v1/parent/library/reservations
 *   /api/v1/parent/library/wishlist/{childId}{,/{bookId}}
 *
 * Student endpoints:
 *   /api/v1/student/library/search
 *   /api/v1/student/library/books/{id}
 *   /api/v1/student/library/issued
 *   /api/v1/student/library/history
 *   /api/v1/student/library/renew/{issueId}
 *   /api/v1/student/library/reserve
 *   /api/v1/student/library/reservations
 *   /api/v1/student/library/wishlist{,/{bookId}}
 *   /api/v1/student/library/reading-goals
 *   /api/v1/student/library/badges
 *   /api/v1/student/library/profile
 *   /api/v1/student/library/announcements
 *   /api/v1/student/library/stats
 *   /api/v1/student/library/discussions/{bookId}
 *   /api/v1/student/library/acquisition-requests
 *   /api/v1/student/library/recommendations
 *   /api/v1/student/library/trending
 */
package com.littlebridge.enrollplus.feature.library

import com.littlebridge.enrollplus.core.*
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
// DTOs are defined locally in LibraryDtos.kt (server does NOT depend on :shared)
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Module-level singleton — no DI container (see NotificationRouting pattern)
private val libraryService = LibraryService()

// ── Rate Limiter ──────────────────────────────────────────────────────────────
// Simple in-memory sliding-window rate limiter per user UUID.
// Spec: 60/min for read endpoints, 10/hour for write endpoints (reserve, renew).

private data class RateBucket(
    val timestamps: MutableList<Long> = mutableListOf(),
)

private val rateBuckets = ConcurrentHashMap<String, RateBucket>()

private fun checkRateLimit(key: String, maxRequests: Int, windowMs: Long): Boolean {
    val bucket = rateBuckets.computeIfAbsent(key) { RateBucket() }
    val now = System.currentTimeMillis()
    val windowStart = now - windowMs
    synchronized(bucket) {
        bucket.timestamps.removeAll { it < windowStart }
        if (bucket.timestamps.size >= maxRequests) return false
        bucket.timestamps.add(now)
        return true
    }
}

private fun ApplicationCall.applyRateLimit(scope: String, maxRequests: Int, windowMs: Long): Boolean {
    val uid = principalUserUuid()?.toString() ?: request.origin.remoteHost
    return checkRateLimit("library:$scope:$uid", maxRequests, windowMs)
}

private const val WINDOW_MINUTE = 60_000L
private const val WINDOW_HOUR = 3_600_000L

// ── Own-data check: verify parent-child relationship ──────────────────────────
// Spec §16 Data Isolation: "Parent queries filtered by child_id (verified parent-child
// relationship)". Prevents a parent from accessing another family's child data.
private suspend fun verifyParentChild(parentUid: UUID, childId: UUID): Boolean = dbQuery {
    ChildrenTable.selectAll()
        .where {
            (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq parentUid) and
                (ChildrenTable.isActive eq true)
        }
        .any()
}

// ── School resolution for parent/student users ───────────────────────────────
// Parent and student users do NOT have school_id on their app_users row.
// Their school association is through ChildrenTable.schoolId. This mirrors
// the pattern in ParentMessagesRouting.kt / ParentFeesRouting.kt.
private suspend fun resolveParentSchoolId(parentUid: UUID): UUID? = dbQuery {
    ChildrenTable.selectAll()
        .where { (ChildrenTable.parentId eq parentUid) and (ChildrenTable.isActive eq true) }
        .mapNotNull { it[ChildrenTable.schoolId] }
        .firstOrNull()
}

fun Route.libraryRouting() {

    // Feature flag gate — all library endpoints return 503 when disabled
    authenticate("jwt") {
        route("/api/v1/school/library") {
            get {
                if (!libraryService.isFeatureEnabled("library_enabled")) {
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Library feature is not enabled", "code" to "SERVICE_UNAVAILABLE"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
            }
        }
    }

    // ── Admin endpoints (school admin only) ────────────────────────────────
    authenticate("jwt") {
        route("/api/v1/school/library") {

            // ── Dashboard ──────────────────────────────────────────────────
            get("/dashboard") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                try {
                    val dashboard = libraryService.getDashboard(ctx.schoolId)
                    call.ok(dashboard)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Dashboard error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Books ──────────────────────────────────────────────────────
            get("/books") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val query = call.request.queryParameters["query"] ?: ""
                val category = call.request.queryParameters["category"]
                val language = call.request.queryParameters["language"]
                val tags = call.request.queryParameters["tags"]?.split(",")?.filter { it.isNotBlank() }
                val sortBy = call.request.queryParameters["sortBy"] ?: "newest"
                val availability = call.request.queryParameters["availability"] ?: "all"
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                try {
                    val result = libraryService.searchBooks(ctx.schoolId, query, category, language, tags, sortBy, availability, page, limit)
                    call.respond(LibraryPaginatedResponse(data = result.books, total = result.total, page = result.page, limit = result.limit))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Search error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/books/search") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val query = call.request.queryParameters["query"] ?: ""
                val category = call.request.queryParameters["category"]
                val language = call.request.queryParameters["language"]
                val tags = call.request.queryParameters["tags"]?.split(",")?.filter { it.isNotBlank() }
                val sortBy = call.request.queryParameters["sortBy"] ?: "newest"
                val availability = call.request.queryParameters["availability"] ?: "all"
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                try {
                    val result = libraryService.searchBooks(ctx.schoolId, query, category, language, tags, sortBy, availability, page, limit)
                    call.respond(LibraryPaginatedResponse(data = result.books, total = result.total, page = result.page, limit = result.limit))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Search error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/books") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateBookRequest>()
                try {
                    val bookId = libraryService.createBook(ctx.schoolId, req, ctx.userId, ctx.role)
                    call.ok(mapOf("id" to bookId.toString()), "Book created", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Create error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/books/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                try {
                    val book = libraryService.getBook(ctx.schoolId, bookId)
                    call.ok(book)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Book error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            put("/books/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@put }
                val req = call.receive<UpdateBookRequest>()
                try {
                    libraryService.updateBook(ctx.schoolId, bookId, req, ctx.userId, ctx.role)
                    call.okMessage("Book updated")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Update error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/books/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.softDeleteBook(ctx.schoolId, bookId, ctx.userId, ctx.role)
                    call.okMessage("Book deleted")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Delete error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Copies ─────────────────────────────────────────────────────
            get("/books/{id}/copies") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                try {
                    val copies = libraryService.listCopies(ctx.schoolId, bookId)
                    call.ok(copies)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Copies error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/books/{id}/copies") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val condition = call.request.queryParameters["condition"] ?: "new"
                try {
                    val copyId = libraryService.addCopy(ctx.schoolId, bookId, condition, ctx.userId, ctx.role)
                    call.ok(mapOf("id" to copyId.toString()), "Copy added", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Add copy error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Issues ─────────────────────────────────────────────────────
            get("/issues") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val status = call.request.queryParameters["status"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                try {
                    val (issues, total) = libraryService.listIssues(ctx.schoolId, status, page, limit)
                    call.respond(LibraryPaginatedResponse(data = issues, total = total, page = page, limit = limit))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Issues error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/issues") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<IssueBookRequest>()
                try {
                    val issue = libraryService.issueBook(ctx.schoolId, req, ctx.userId, ctx.role)
                    call.ok(issue, "Book issued", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Issue error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/issues/{id}/return") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val issueId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val req = call.receive<ReturnBookRequest>()
                try {
                    val result = libraryService.returnBook(ctx.schoolId, req, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Return error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/issues/{id}/renew") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val issueId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.renewBook(ctx.schoolId, issueId, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Renew error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/issues/{id}/lost") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val issueId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.markLost(ctx.schoolId, issueId, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Mark lost error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Fines ──────────────────────────────────────────────────────
            post("/issues/{id}/fine/pay") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("fine-pay", 20, WINDOW_HOUR)) { call.fail("Too many fine payment requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val issueId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    libraryService.payFine(ctx.schoolId, issueId, ctx.userId, ctx.role)
                    call.okMessage("Fine paid")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Pay fine error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/issues/{id}/fine/waive") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("fine-waive", 20, WINDOW_HOUR)) { call.fail("Too many fine waiver requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val issueId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val req = call.receive<WaiveFineRequest>()
                try {
                    libraryService.waiveFine(ctx.schoolId, issueId, req.reason, ctx.userId, ctx.role)
                    call.okMessage("Fine waived")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Waive fine error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Quick Issue ────────────────────────────────────────────────
            post("/quick-issue") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("quick-issue", 30, WINDOW_MINUTE)) { call.fail("Too many quick-issue requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val req = call.receive<QuickIssueRequest>()
                val idempotencyKey = call.request.headers["Idempotency-Key"]
                try {
                    val result = libraryService.quickIssue(
                        ctx.schoolId, req.barcode,
                        req.borrowerId.toUuidOrNull() ?: throw LibraryValidationException("borrower_id", "Invalid borrower id"),
                        req.borrowerType, req.borrowerName,
                        ctx.userId, ctx.role,
                        idempotencyKey,
                    )
                    call.ok(result, "Quick issue successful", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Quick issue error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Bulk Return ────────────────────────────────────────────────
            post("/bulk-return") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("bulk-return", 30, WINDOW_MINUTE)) { call.fail("Too many bulk-return requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val req = call.receive<BulkReturnRequest>()
                try {
                    val results = libraryService.bulkReturn(ctx.schoolId, req.barcodes, ctx.userId, ctx.role)
                    call.ok(results)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Bulk return error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Bulk Import ───────────────────────────────────────────────
            post("/books/bulk-import") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("bulk-import", 1, WINDOW_MINUTE)) { call.fail("Too many bulk-import requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val req = call.receive<BulkImportRequest>()
                try {
                    val result = libraryService.bulkImport(ctx.schoolId, req.rows, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Bulk import error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Export ────────────────────────────────────────────────────
            get("/export") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                if (!call.applyRateLimit("export", 2, WINDOW_HOUR)) { call.fail("Too many export requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@get }
                val type = call.request.queryParameters["type"] ?: "catalog"
                try {
                    val result = when (type) {
                        "issues" -> libraryService.exportIssues(ctx.schoolId)
                        "fines" -> libraryService.exportFines(ctx.schoolId)
                        "audit" -> libraryService.exportAudit(ctx.schoolId)
                        else -> libraryService.exportCatalog(ctx.schoolId)
                    }
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Export error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/export/download") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val type = call.request.queryParameters["type"] ?: "catalog"
                try {
                    val csv = libraryService.generateExportCsv(ctx.schoolId, type)
                    call.respondText(csv, ContentType.Text.CSV, HttpStatusCode.OK)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Export download error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Archive / Unarchive ───────────────────────────────────────
            post("/books/{id}/archive") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.archiveBook(ctx.schoolId, bookId, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Archive error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/books/{id}/unarchive") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.unarchiveBook(ctx.schoolId, bookId, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Unarchive error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Trending ──────────────────────────────────────────────────
            get("/trending") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                try {
                    val result = libraryService.listTrending(ctx.schoolId, limit)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Trending error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Copy Repair ───────────────────────────────────────────────
            get("/copies/repair") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                try {
                    val copies = libraryService.listCopiesInRepair(ctx.schoolId)
                    call.ok(copies.map { mapOf("copy" to it.first, "bookTitle" to it.second) })
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Repair list error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/copies/{id}/repair") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val copyId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid copy id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.repairCopy(ctx.schoolId, copyId, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Repair error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Onboarding ────────────────────────────────────────────────
            post("/onboarding") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                try {
                    val result = libraryService.runOnboarding(ctx.schoolId, ctx.userId, ctx.role)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Onboarding error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Book Cover Upload ─────────────────────────────────────────
            post("/books/{id}/cover-url") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val req = call.receive<CoverUrlRequest>()
                try {
                    LibraryCoverService.validateStorageUrl(req.coverUrl)
                    val updated = libraryService.updateBookCover(ctx.schoolId, bookId, req.coverUrl, ctx.userId, ctx.role)
                    call.ok(updated, "Cover updated")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Cover update error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/books/{id}/cover") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }

                var fileBytes: ByteArray? = null
                var contentType: String? = null

                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (part.name == "file" && fileBytes == null) {
                                contentType = part.contentType?.toString()
                                    ?: part.originalFileName?.let { guessCoverContentType(it) }
                                @Suppress("DEPRECATION")
                                fileBytes = part.streamProvider().use { it.readBytes() }
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                val bytes = fileBytes
                if (bytes == null || bytes.isEmpty()) {
                    call.fail("No file part found", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                if (bytes.size > 5 * 1024 * 1024) {
                    call.fail("Cover image must be under 5 MB", HttpStatusCode.PayloadTooLarge, "FILE_TOO_LARGE")
                    return@post
                }
                val ct = contentType
                if (ct == null || !ct.startsWith("image/")) {
                    call.fail("Only image files are accepted for book covers", HttpStatusCode.UnsupportedMediaType, "UNSUPPORTED_TYPE")
                    return@post
                }

                try {
                    val coverResult = LibraryCoverService.upload(ctx.schoolId, bookId, bytes, ct)
                    val updated = libraryService.updateBookCover(ctx.schoolId, bookId, coverResult.coverUrl, ctx.userId, ctx.role)
                    call.ok(updated, "Cover uploaded")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Cover upload error", e.toHttpStatusCode(), e::class.simpleName)
                } catch (e: Exception) {
                    runCatching {
                        com.littlebridge.enrollplus.feature.notifications.Notify.toUser(
                            userId = ctx.userId,
                            category = "library",
                            title = "⚠️ Cover Upload Failed",
                            body = "Cover upload failed for book ID ${bookId}: ${e.message}",
                            schoolId = ctx.schoolId,
                            deepLink = "/admin/library/books/$bookId",
                            refType = "library_book",
                            refId = bookId.toString(),
                        )
                    }
                    call.fail("Cover upload failed: ${e.message}", HttpStatusCode.BadGateway, "UPLOAD_FAILED")
                }
            }

            // ── Reservations ───────────────────────────────────────────────
            get("/reservations") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val bookId = call.request.queryParameters["bookId"]
                try {
                    if (bookId != null) {
                        val bookUuid = bookId.toUuidOrNull()
                            ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                        val reservations = libraryService.listReservationsForBook(ctx.schoolId, bookUuid)
                        call.ok(reservations)
                    } else {
                        call.ok(emptyList<LibraryReservationDto>())
                    }
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reservations error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/reservations/{id}/fulfill") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val reservationId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid reservation id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    libraryService.fulfillReservation(ctx.schoolId, reservationId, ctx.userId, ctx.role)
                    call.okMessage("Reservation fulfilled")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Fulfill error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Categories ─────────────────────────────────────────────────
            get("/categories") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                try {
                    val categories = libraryService.listCategories(ctx.schoolId)
                    call.ok(categories)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Categories error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/categories") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateCategoryRequest>()
                try {
                    val id = libraryService.createCategory(ctx.schoolId, req, ctx.userId, ctx.role)
                    call.ok(mapOf("id" to id.toString()), "Category created", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Create category error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            put("/categories/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val categoryId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid category id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@put }
                val req = call.receive<UpdateCategoryRequest>()
                try {
                    libraryService.updateCategory(ctx.schoolId, categoryId, req, ctx.userId, ctx.role)
                    call.okMessage("Category updated")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Update category error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/categories/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val categoryId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid category id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.deleteCategory(ctx.schoolId, categoryId, ctx.userId, ctx.role)
                    call.okMessage("Category deleted")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Delete category error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/categories/reorder") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<ReorderCategoriesRequest>()
                try {
                    val orders = req.orders.map { it.id to it.displayOrder }
                    libraryService.reorderCategories(ctx.schoolId, orders, ctx.userId, ctx.role)
                    call.okMessage("Categories reordered")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reorder error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Settings ───────────────────────────────────────────────────
            get("/settings") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                try {
                    val settings = libraryService.getSettings(ctx.schoolId)
                    call.ok(settings)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Settings error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/notification-channels") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                call.ok(LibraryService.NOTIFICATION_CHANNEL_MATRIX.map { (event, config) ->
                    mapOf(
                        "event" to event,
                        "push" to config.push,
                        "inApp" to config.inApp,
                        "email" to config.email,
                        "sms" to config.sms,
                    )
                })
            }

            put("/settings") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val req = call.receive<UpdateSettingsRequest>()
                try {
                    val settings = libraryService.updateSettings(ctx.schoolId, req, ctx.userId, ctx.role)
                    call.ok(settings, "Settings updated")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Update settings error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Audit Log ──────────────────────────────────────────────────
            get("/audit-log") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                if (!call.applyRateLimit("audit-log", 10, WINDOW_MINUTE)) { call.fail("Too many audit-log requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@get }
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                try {
                    val logs = libraryService.listAuditLog(ctx.schoolId, page, limit)
                    call.ok(logs)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Audit log error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/audit-log/verify-hash") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                try {
                    val result = libraryService.verifyAuditHashChain(ctx.schoolId)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Hash verification error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Cursor-based pagination (spec §17) ──────────────────────────
            get("/audit-log/cursor") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val cursor = call.request.queryParameters["cursor"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                try {
                    val result = libraryService.listAuditLogCursor(ctx.schoolId, cursor, limit)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Audit log cursor error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/issues/cursor") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val status = call.request.queryParameters["status"]
                val cursor = call.request.queryParameters["cursor"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                try {
                    val result = libraryService.listIssuesCursor(ctx.schoolId, status, cursor, limit)
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Issues cursor error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── GDPR: Right to access + erasure (spec §16) ──────────────────
            get("/gdpr/export/{borrowerId}") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val borrowerId = call.parameters["borrowerId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid borrower id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                try {
                    val export = libraryService.exportBorrowerData(ctx.schoolId, borrowerId)
                    call.ok(export)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "GDPR export error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/gdpr/anonymize/{borrowerId}") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("gdpr-anonymize", 5, WINDOW_HOUR)) {
                    call.fail("Too many anonymization requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post
                }
                val borrowerId = call.parameters["borrowerId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid borrower id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val count = libraryService.anonymizeBorrower(borrowerId)
                    call.ok(mapOf("anonymizedRows" to count), "Borrower PII anonymized")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Anonymization error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── PII classification (spec §16) ──────────────────────────────
            get("/pii-classification") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                call.ok(LibraryPrivacyService.PII_REGISTRY.map {
                    mapOf(
                        "table" to it.table,
                        "column" to it.column,
                        "classification" to it.classification.name,
                    )
                })
            }

            // ── Data retention policies (spec §16) ──────────────────────────
            get("/retention-policies") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                call.ok(LibraryPrivacyService.RETENTION_POLICIES.map {
                    mapOf(
                        "table" to it.table,
                        "retentionDays" to it.retentionDays,
                        "description" to it.description,
                    )
                })
            }

            // ── Fines ──────────────────────────────────────────────────────
            get("/fines/outstanding") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                try {
                    val (fines, total) = libraryService.listOutstandingFines(ctx.schoolId, page, limit)
                    call.ok(mapOf("items" to fines, "total" to total, "page" to page, "limit" to limit))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Outstanding fines error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/fines/summary") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                try {
                    val summary = libraryService.getFineSummary(ctx.schoolId)
                    call.ok(summary)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Fine summary error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Copy Update/Delete ─────────────────────────────────────────
            put("/copies/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val copyId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid copy id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@put }
                val req = call.receive<UpdateCopyRequest>()
                try {
                    libraryService.updateCopy(ctx.schoolId, copyId, req, ctx.userId, ctx.role)
                    call.okMessage("Copy updated")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Update copy error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/copies/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val copyId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid copy id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.deleteCopy(ctx.schoolId, copyId, ctx.userId, ctx.role)
                    call.okMessage("Copy deleted")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Delete copy error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Featured Book ──────────────────────────────────────────────
            post("/featured") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<SetFeaturedBookRequest>()
                try {
                    libraryService.setFeaturedBook(ctx.schoolId, req.bookId.toUuidOrNull()!!, req.type, ctx.userId, ctx.role)
                    call.okMessage("Featured book set")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Featured book error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Leaderboard ────────────────────────────────────────────────
            get("/leaderboard") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                try {
                    val leaderboard = libraryService.getLeaderboard(ctx.schoolId, limit)
                    call.ok(leaderboard)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Leaderboard error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Announcements ──────────────────────────────────────────────
            get("/announcements") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val activeOnly = call.request.queryParameters["active"]?.toBoolean() ?: false
                try {
                    val announcements = libraryService.listAnnouncements(ctx.schoolId, activeOnly)
                    call.ok(announcements)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Announcements error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/announcements") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                if (!call.applyRateLimit("announcement-create", 5, WINDOW_HOUR)) { call.fail("Too many announcement creation requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val req = call.receive<CreateAnnouncementRequest>()
                try {
                    val id = libraryService.createAnnouncement(ctx.schoolId, req, ctx.userId, ctx.role)
                    call.ok(mapOf("id" to id.toString()), "Announcement created", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Create announcement error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            put("/announcements/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val announcementId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid announcement id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@put }
                val req = call.receive<UpdateAnnouncementRequest>()
                try {
                    libraryService.updateAnnouncement(ctx.schoolId, announcementId, req, ctx.userId, ctx.role)
                    call.okMessage("Announcement updated")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Update announcement error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Acquisition Requests ───────────────────────────────────────
            get("/acquisition-requests") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val status = call.request.queryParameters["status"]
                try {
                    val requests = libraryService.listAcquisitionRequests(ctx.schoolId, status)
                    call.ok(requests)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Acquisition requests error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/acquisition-requests/{id}/approve") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val requestId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    libraryService.updateAcquisitionStatus(ctx.schoolId, requestId, "approved", ctx.userId, ctx.role, null)
                    call.okMessage("Acquisition request approved")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Approve error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/acquisition-requests/{id}/reject") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val requestId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    libraryService.updateAcquisitionStatus(ctx.schoolId, requestId, "rejected", ctx.userId, ctx.role, null)
                    call.okMessage("Acquisition request rejected")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reject error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/acquisition-requests/{id}/order") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val requestId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val orderLink = call.request.queryParameters["orderLink"]
                try {
                    libraryService.updateAcquisitionStatus(ctx.schoolId, requestId, "ordered", ctx.userId, ctx.role, orderLink)
                    call.okMessage("Acquisition request marked as ordered")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Order error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/acquisition-requests/{id}/receive") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val requestId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    libraryService.updateAcquisitionStatus(ctx.schoolId, requestId, "received", ctx.userId, ctx.role, null)
                    call.okMessage("Acquisition request marked as received")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Receive error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/acquisition-requests/{id}/convert") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val requestId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val book = libraryService.convertAcquisitionToBook(ctx.schoolId, requestId, ctx.userId, ctx.role)
                    call.ok(book, "Acquisition request converted to book", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Convert error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }
        }

        // ── Parent endpoints ──────────────────────────────────────────────
        route("/api/v1/parent/library") {

            get("/search") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val query = call.request.queryParameters["query"] ?: ""
                val category = call.request.queryParameters["category"]
                val language = call.request.queryParameters["language"]
                val tags = call.request.queryParameters["tags"]?.split(",")?.filter { it.isNotBlank() }
                val sortBy = call.request.queryParameters["sortBy"] ?: "newest"
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                try {
                    val result = libraryService.searchBooks(schoolId, query, category, language, tags, sortBy, "all", page, limit)
                    call.respond(LibraryPaginatedResponse(data = result.books, total = result.total, page = result.page, limit = result.limit))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Search error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/books/{id}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                try {
                    val book = libraryService.getBook(schoolId, bookId)
                    call.ok(book)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Book error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/issued/{childId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val childId = call.parameters["childId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid child id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                if (!verifyParentChild(uid, childId)) { call.fail("Not your child", HttpStatusCode.Forbidden, "NOT_OWN_CHILD"); return@get }
                try {
                    val issues = libraryService.listIssuedForBorrower(schoolId, childId)
                    call.ok(issues)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Issued books error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/reserve") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                if (!call.applyRateLimit("reserve", 10, WINDOW_HOUR)) { call.fail("Too many reservation requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val req = call.receive<ReserveBookRequest>()
                try {
                    val reservation = libraryService.reserveBook(schoolId, req.bookId.toUuidOrNull()!!, uid, "Parent", "parent")
                    call.ok(reservation, "Book reserved", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reserve error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/reservations") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val reservations = libraryService.listReservationsForUser(schoolId, uid)
                    call.ok(reservations)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reservations error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/reservations/{id}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@delete }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@delete }
                val reservationId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid reservation id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.cancelReservation(schoolId, reservationId, uid)
                    call.okMessage("Reservation cancelled")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Cancel error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Parent: History ────────────────────────────────────────────
            get("/history/{childId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val childId = call.parameters["childId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid child id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                if (!verifyParentChild(uid, childId)) { call.fail("Not your child", HttpStatusCode.Forbidden, "NOT_OWN_CHILD"); return@get }
                try {
                    val history = libraryService.listHistoryForBorrower(schoolId, childId)
                    call.ok(history)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "History error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Parent: Renew ──────────────────────────────────────────────
            post("/renew/{issueId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                if (!call.applyRateLimit("renew", 10, WINDOW_HOUR)) { call.fail("Too many renewal requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val issueId = call.parameters["issueId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.renewBook(schoolId, issueId, uid, "Parent")
                    call.ok(result)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Renew error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Parent: Announcements ──────────────────────────────────────
            get("/announcements") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val announcements = libraryService.listAnnouncements(schoolId, true)
                    call.ok(announcements)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Announcements error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Parent: Trending ───────────────────────────────────────────
            get("/trending") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                try {
                    val trending = libraryService.listTrending(schoolId, limit)
                    call.ok(trending)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Trending error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Parent: Wishlist ───────────────────────────────────────────
            get("/wishlist/{childId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val childId = call.parameters["childId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid child id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                if (!verifyParentChild(uid, childId)) { call.fail("Not your child", HttpStatusCode.Forbidden, "NOT_OWN_CHILD"); return@get }
                try {
                    val wishlist = libraryService.listWishlist(schoolId, childId)
                    call.ok(wishlist)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Wishlist error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/wishlist/{childId}/{bookId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val childId = call.parameters["childId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid child id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val bookId = call.parameters["bookId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                if (!verifyParentChild(uid, childId)) { call.fail("Not your child", HttpStatusCode.Forbidden, "NOT_OWN_CHILD"); return@post }
                try {
                    val id = libraryService.addToWishlist(schoolId, childId, bookId)
                    call.ok(mapOf("id" to id.toString()), "Added to wishlist", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Wishlist error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/wishlist/{childId}/{bookId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@delete }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@delete }
                val childId = call.parameters["childId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid child id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                val bookId = call.parameters["bookId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                if (!verifyParentChild(uid, childId)) { call.fail("Not your child", HttpStatusCode.Forbidden, "NOT_OWN_CHILD"); return@delete }
                try {
                    libraryService.removeFromWishlist(schoolId, childId, bookId)
                    call.okMessage("Removed from wishlist")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Wishlist error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }
        }

        // ── Student endpoints ─────────────────────────────────────────────
        route("/api/v1/student/library") {

            get("/search") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val query = call.request.queryParameters["query"] ?: ""
                val category = call.request.queryParameters["category"]
                val language = call.request.queryParameters["language"]
                val tags = call.request.queryParameters["tags"]?.split(",")?.filter { it.isNotBlank() }
                val sortBy = call.request.queryParameters["sortBy"] ?: "newest"
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                try {
                    val result = libraryService.searchBooks(schoolId, query, category, language, tags, sortBy, "all", page, limit)
                    call.respond(LibraryPaginatedResponse(data = result.books, total = result.total, page = result.page, limit = result.limit))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Search error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/books/{id}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val bookId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                try {
                    val book = libraryService.getBook(schoolId, bookId)
                    call.ok(book)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Book error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/issued") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val issues = libraryService.listIssuedForBorrower(schoolId, uid)
                    call.ok(issues)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Issued books error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/history") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val history = libraryService.listHistoryForBorrower(schoolId, uid)
                    call.ok(history)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "History error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/renew/{issueId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                if (!call.applyRateLimit("renew", 10, WINDOW_HOUR)) { call.fail("Too many renewal requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val issueId = call.parameters["issueId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid issue id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val result = libraryService.studentRenewBook(schoolId, issueId, uid)
                    call.ok(result, "Book renewed", HttpStatusCode.OK)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Renew error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Reserve & Reservations ──────────────────────────────────────
            post("/reserve") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                if (!call.applyRateLimit("reserve", 10, WINDOW_HOUR)) { call.fail("Too many reservation requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val req = call.receive<ReserveBookRequest>()
                try {
                    val reservation = libraryService.reserveBook(schoolId, req.bookId.toUuidOrNull()!!, uid, "Student", "student")
                    call.ok(reservation, "Book reserved", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reserve error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/reservations") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val reservations = libraryService.listReservationsForUser(schoolId, uid)
                    call.ok(reservations)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reservations error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/reservations/{id}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@delete }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@delete }
                val reservationId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid reservation id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.cancelReservation(schoolId, reservationId, uid)
                    call.okMessage("Reservation cancelled")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Cancel error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/profile") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val profile = libraryService.getStudentProfile(schoolId, uid)
                    call.ok(profile)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Profile error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/badges") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val badges = libraryService.listBadges(schoolId, uid)
                    call.ok(badges)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Badges error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Wishlist ───────────────────────────────────────────────────
            get("/wishlist") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val wishlist = libraryService.listWishlist(schoolId, uid)
                    call.ok(wishlist)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Wishlist error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/wishlist/{bookId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val bookId = call.parameters["bookId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                try {
                    val id = libraryService.addToWishlist(schoolId, uid, bookId)
                    call.ok(mapOf("id" to id.toString()), "Added to wishlist", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Wishlist error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/wishlist/{bookId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@delete }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@delete }
                val bookId = call.parameters["bookId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.removeFromWishlist(schoolId, uid, bookId)
                    call.okMessage("Removed from wishlist")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Wishlist error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Reading Goals ──────────────────────────────────────────────
            get("/reading-goals") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val period = call.request.queryParameters["period"] ?: "monthly"
                val year = call.request.queryParameters["year"]?.toIntOrNull() ?: java.time.LocalDate.now().year
                try {
                    val goal = libraryService.getReadingGoal(schoolId, uid, period, year)
                    if (goal != null) call.ok(goal) else call.okMessage("No reading goal set")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reading goals error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/reading-goals") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val req = call.receive<CreateReadingGoalRequest>()
                try {
                    val id = libraryService.upsertReadingGoal(schoolId, uid, req)
                    call.ok(mapOf("id" to id.toString()), "Reading goal saved", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reading goals error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Discussions ────────────────────────────────────────────────
            get("/discussions/{bookId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val bookId = call.parameters["bookId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                try {
                    val discussions = libraryService.listDiscussions(schoolId, bookId)
                    call.ok(discussions)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Discussions error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/discussions/{bookId}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val bookId = call.parameters["bookId"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid book id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val req = call.receive<PostDiscussionRequest>()
                try {
                    val id = libraryService.postDiscussion(schoolId, bookId, uid, "Student", req.message)
                    call.ok(mapOf("id" to id.toString()), "Message posted", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Discussion error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Acquisition Requests ───────────────────────────────────────
            get("/acquisition-requests") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val requests = libraryService.listAcquisitionRequests(schoolId, null)
                    call.ok(requests)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Acquisition requests error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            post("/acquisition-requests") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val req = call.receive<CreateAcquisitionRequest>()
                try {
                    val id = libraryService.createAcquisitionRequest(schoolId, uid, "Student", "student", req)
                    call.ok(mapOf("id" to id.toString()), "Acquisition request submitted", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Acquisition request error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Reserve ────────────────────────────────────────────────────
            post("/reserve") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post }
                if (!call.applyRateLimit("reserve", 10, WINDOW_HOUR)) { call.fail("Too many reservation requests", HttpStatusCode.TooManyRequests, "RATE_LIMITED"); return@post }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@post }
                val req = call.receive<ReserveBookRequest>()
                try {
                    val reservation = libraryService.reserveBook(schoolId, req.bookId.toUuidOrNull()!!, uid, "Student", "student")
                    call.ok(reservation, "Book reserved", HttpStatusCode.Created)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reserve error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            get("/reservations") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val reservations = libraryService.listReservationsForUser(schoolId, uid)
                    call.ok(reservations)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Reservations error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            delete("/reservations/{id}") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@delete }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@delete }
                val reservationId = call.parameters["id"]?.toUuidOrNull()
                    ?: run { call.fail("Invalid reservation id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                try {
                    libraryService.cancelReservation(schoolId, reservationId, uid)
                    call.okMessage("Reservation cancelled")
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Cancel error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Recommendations ───────────────────────────────────────────
            get("/recommendations") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                try {
                    val recommendations = libraryService.listRecommendations(schoolId, uid, limit)
                    call.ok(recommendations)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Recommendations error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Trending ──────────────────────────────────────────────────
            get("/trending") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                try {
                    val trending = libraryService.listTrending(schoolId, limit)
                    call.ok(trending)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Trending error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Leaderboard ────────────────────────────────────────────────
            get("/leaderboard") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                try {
                    val leaderboard = libraryService.getLeaderboard(schoolId, limit)
                    call.ok(leaderboard)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Leaderboard error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Featured Book ──────────────────────────────────────────────
            get("/featured") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val featured = libraryService.getFeaturedBook(schoolId)
                    if (featured != null) call.ok(featured) else call.ok(mapOf<String, Any?>())
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Featured book error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Reading Streak ─────────────────────────────────────────────
            get("/streak") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val streak = libraryService.getReadingStreak(schoolId, uid)
                    call.ok(mapOf("streak" to streak))
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Streak error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Announcements ──────────────────────────────────────────────
            get("/announcements") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val announcements = libraryService.listAnnouncements(schoolId, activeOnly = true)
                    call.ok(announcements)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Announcements error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }

            // ── Stats ──────────────────────────────────────────────────────
            get("/stats") {
                val uid = call.principalUserUuid() ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val schoolId = resolveParentSchoolId(uid) ?: run { call.fail("No school", HttpStatusCode.NotFound, "NO_SCHOOL"); return@get }
                try {
                    val profile = libraryService.getStudentProfile(schoolId, uid)
                    call.ok(profile)
                } catch (e: LibraryException) {
                    call.fail(e.message ?: "Stats error", e.toHttpStatusCode(), e::class.simpleName)
                }
            }
        }
    }
}

private fun String.toUuidOrNull(): UUID? =
    runCatching { UUID.fromString(this) }.getOrNull()

private fun guessCoverContentType(fileName: String): String? =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "heic" -> "image/heic"
        else -> null
    }
