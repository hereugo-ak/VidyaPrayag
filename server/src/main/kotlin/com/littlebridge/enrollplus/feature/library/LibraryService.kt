/*
 * File: LibraryService.kt
 * Module: feature.library
 *
 * Business logic for the library feature. Orchestrates repository calls,
 * enforces invariants (max books, max renewals, copy availability), applies
 * plugins (ISBN validation, barcode generation, fine/duedate calculation),
 * and maps row projections to shared DTOs.
 *
 * Pattern follows GatewaySmsService / ScholarshipService:
 *   - constructor-injected dependencies with defaults (no DI container)
 *   - suspend functions for coroutine-friendly routing calls
 *   - throws LibraryException subclasses for error cases
 */
package com.littlebridge.enrollplus.feature.library

// DTOs are defined locally in LibraryDtos.kt (server does NOT depend on :shared)
import com.littlebridge.enrollplus.feature.notifications.Notify
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LibraryService(
    private val repo: LibraryRepository = LibraryRepository(),
    private val isbnValidator: IsbnValidator = DefaultIsbnValidator(),
    private val barcodeGenerator: BarcodeGenerator = DefaultBarcodeGenerator(),
    private val fineCalculator: FineCalculator = DefaultFineCalculator(),
    private val dueDateCalculator: DueDateCalculator = DefaultDueDateCalculator(),
    private val featureFlags: FeatureFlagService = DefaultFeatureFlagService(),
    private val searchProvider: SearchProvider = PostgresSearchProvider(repo),
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val quickIssueIdempotencyCache = ConcurrentHashMap<String, QuickIssueResultDto>()

    fun isFeatureEnabled(flag: String): Boolean = featureFlags.isEnabled(flag)

    // ── Books ──────────────────────────────────────────────────────────────

    suspend fun createBook(schoolId: UUID, req: CreateBookRequest, actorId: UUID, actorName: String): UUID {
        if (req.title.isBlank()) throw LibraryValidationException("title", "Title is required")
        if (req.title.length > 500) throw LibraryValidationException("title", "Title must be at most 500 characters")
        if (req.totalCopies < 1) throw LibraryValidationException("total_copies", "Must have at least 1 copy")
        if (req.totalCopies > 1000) throw LibraryValidationException("total_copies", "Cannot exceed 1000 copies")
        req.author?.let { if (it.length > 200) throw LibraryValidationException("author", "Author must be at most 200 characters") }
        req.replacementCost?.let { if (it < 0 || it > 100000) throw LibraryValidationException("replacement_cost", "Must be between 0 and 100000") }
        req.tags?.let { if (it.size > 20) throw LibraryValidationException("tags", "Maximum 20 tags allowed") }

        val sanitizedTitle = sanitizeHtml(req.title)
        val sanitizedAuthor = req.author?.let { sanitizeHtml(it) }

        val normalizedIsbn = req.isbn?.takeIf { it.isNotBlank() }?.let {
            if (!isbnValidator.validate(it)) throw LibraryValidationException("isbn", "Invalid ISBN format")
            isbnValidator.normalize(it)
        }

        val bookId = repo.createBook(
            schoolId = schoolId, isbn = normalizedIsbn, title = sanitizedTitle, author = sanitizedAuthor,
            publisher = req.publisher, category = req.category, tags = req.tags,
            totalCopies = req.totalCopies, shelfLocation = req.shelfLocation,
            coverUrl = req.coverUrl, replacementCost = req.replacementCost,
            seriesName = req.seriesName, seriesNumber = req.seriesNumber,
            language = req.language, synopsis = req.synopsis, pageCount = req.pageCount,
        )

        // Auto-create copies
        for (i in 1..req.totalCopies) {
            val barcode = barcodeGenerator.generate(schoolId, bookId, i)
            repo.createCopy(schoolId, bookId, i, barcode, "new")
        }

        repo.appendAuditLog(schoolId, actorId, actorName, "CREATE_BOOK", "book", bookId,
            metadata = mapOf("title" to req.title))

        LibraryEventBus.publish(BookCreated(schoolId, bookId, sanitizedTitle, actorId, actorName))

        return bookId
    }

    suspend fun searchBooks(
        schoolId: UUID, query: String, category: String?, language: String?, tags: List<String>?,
        sortBy: String, availability: String, page: Int, limit: Int,
    ): SearchResultDto {
        val cacheKey = LibraryCacheKeys.bookSearch(schoolId, query, category, language, tags, sortBy, availability, page, limit)
        return LibraryCache.getOrPut(cacheKey, LibraryCacheKeys.SEARCH_TTL_MINUTES) {
            val filters = SearchFilters(category = category, language = language, tags = tags, sortBy = sortBy, availability = availability)
            val (rows, total) = searchProvider.search(schoolId, query, filters, page, limit)
            SearchResultDto(
                books = rows.map { it.toDto() },
                total = total,
                page = page,
                limit = limit,
            )
        }
    }

    suspend fun getBook(schoolId: UUID, bookId: UUID): LibraryBookDto {
        val row = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        return row.toDto()
    }

    suspend fun updateBook(schoolId: UUID, bookId: UUID, req: UpdateBookRequest, actorId: UUID, actorName: String) {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())

        val updates = mutableMapOf<String, Any?>()
        req.title?.let { updates["title"] = it }
        req.author?.let { updates["author"] = it }
        req.publisher?.let { updates["publisher"] = it }
        req.category?.let { updates["category"] = it }
        req.tags?.let { updates["tags"] = it }
        req.shelfLocation?.let { updates["shelfLocation"] = it }
        req.coverUrl?.let { updates["coverUrl"] = it }
        req.replacementCost?.let { updates["replacementCost"] = it }
        req.seriesName?.let { updates["seriesName"] = it }
        req.seriesNumber?.let { updates["seriesNumber"] = it }
        req.language?.let { updates["language"] = it }
        req.synopsis?.let { updates["synopsis"] = it }
        req.pageCount?.let { updates["pageCount"] = it }

        repo.updateBook(schoolId, bookId, updates)
        repo.appendAuditLog(schoolId, actorId, actorName, "UPDATE_BOOK", "book", bookId)
        LibraryEventBus.publish(BookUpdated(schoolId, bookId, actorId, actorName))
    }

    suspend fun softDeleteBook(schoolId: UUID, bookId: UUID, actorId: UUID, actorName: String) {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        if (book.availableCopies != book.totalCopies) {
            throw LibraryConflictException("Cannot delete book with issued copies")
        }
        repo.softDeleteBook(schoolId, bookId)
        repo.appendAuditLog(schoolId, actorId, actorName, "DELETE_BOOK", "book", bookId)
        LibraryEventBus.publish(BookDeleted(schoolId, bookId, actorId, actorName))
    }

    // ── Copies ─────────────────────────────────────────────────────────────

    suspend fun listCopies(schoolId: UUID, bookId: UUID): List<BookCopyDto> {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        return repo.listCopiesForBook(schoolId, bookId).map { it.toDto() }
    }

    suspend fun addCopy(schoolId: UUID, bookId: UUID, condition: String, actorId: UUID, actorName: String): UUID {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        val copyNum = repo.nextCopyNumber(bookId)
        val barcode = barcodeGenerator.generate(schoolId, bookId, copyNum)
        val copyId = repo.createCopy(schoolId, bookId, copyNum, barcode, condition)
        repo.updateBookAvailability(schoolId, bookId, +1)
        // Also bump totalCopies — we need a separate update for that
        repo.updateBook(schoolId, bookId, mapOf("totalCopies" to (book.totalCopies + 1)))
        repo.appendAuditLog(schoolId, actorId, actorName, "ADD_COPY", "copy", copyId)
        LibraryEventBus.publish(CopyAdded(schoolId, bookId, copyId, actorId, actorName))
        return copyId
    }

    // ── Issue / Return / Renew ──────────────────────────────────────────────

    suspend fun issueBook(schoolId: UUID, req: IssueBookRequest, actorId: UUID, actorName: String): LibraryIssueDto {
        val book = repo.findBookById(schoolId, UUID.fromString(req.bookId))
            ?: throw LibraryNotFoundException("Book", req.bookId)
        if (book.availableCopies <= 0) throw LibraryConflictException("No copies available")

        val settings = repo.getSettings(schoolId)
        val loanDays = settings?.defaultLoanDays ?: 14
        val maxBooks = settings?.maxBooksPerStudent ?: 3

        val borrowerId = UUID.fromString(req.borrowerId)
        val activeCount = repo.countActiveIssuesForBorrower(schoolId, borrowerId)
        if (req.borrowerType == "student" && activeCount >= maxBooks) {
            throw LibraryConflictException("Student has reached max books limit ($maxBooks)")
        }

        val copyId = req.copyId?.let { UUID.fromString(it) }
        if (copyId != null) {
            val copy = repo.findCopyById(schoolId, copyId)
                ?: throw LibraryNotFoundException("Copy", copyId.toString())
            if (copy.status != "available") throw LibraryConflictException("Copy is not available")
            val activeIssue = repo.findActiveIssueForCopy(schoolId, copyId)
            if (activeIssue != null) throw LibraryConflictException("Copy already issued")
        }

        val today = LocalDate.now()
        val dueDate = dueDateCalculator.calculate(today, loanDays)
        val issueId = repo.createIssue(
            schoolId, UUID.fromString(req.bookId), copyId, borrowerId,
            req.borrowerType, req.borrowerName, today, dueDate,
        )

        if (copyId != null) {
            val updated = repo.updateCopyStatusConditional(schoolId, copyId, "available", "issued")
            if (updated == 0) throw LibraryConflictException("COPY_ALREADY_ISSUED")
        }
        repo.updateBookAvailability(schoolId, UUID.fromString(req.bookId), -1)

        repo.appendAuditLog(schoolId, actorId, actorName, "ISSUE_BOOK", "issue", issueId,
            metadata = mapOf("bookId" to req.bookId, "borrowerName" to req.borrowerName))

        LibraryEventBus.publish(BookIssued(schoolId, UUID.fromString(req.bookId), copyId, borrowerId, req.borrowerName, dueDate, actorId, actorName))

        return repo.findIssueById(schoolId, issueId)!!.toDto(book.title)
    }

    suspend fun returnBook(schoolId: UUID, req: ReturnBookRequest, actorId: UUID, actorName: String): ReturnResultDto {
        val issueId = UUID.fromString(req.issueId)
        val issue = repo.findIssueById(schoolId, issueId)
            ?: throw LibraryNotFoundException("Issue", req.issueId)
        if (issue.status != "issued") throw LibraryConflictException("Book already returned")

        val settings = repo.getSettings(schoolId)
        val finePerDay = settings?.finePerDay ?: 1.0
        val fineCapEnabled = settings?.fineCapEnabled ?: true

        val book = repo.findBookById(schoolId, issue.bookId)
        val today = LocalDate.now()
        val fine = fineCalculator.calculate(issue.dueDate, today, finePerDay, fineCapEnabled, book?.replacementCost)
        val fineStatus = if (fine > 0) "pending" else "none"

        repo.returnIssue(schoolId, issueId, today, req.returnCondition, req.damageNotes, fine, fineStatus)

        if (issue.copyId != null) {
            val condition = req.returnCondition ?: "good"
            if (condition == "damaged") {
                repo.updateCopyStatus(schoolId, issue.copyId, "repair")
            } else {
                repo.updateCopyStatus(schoolId, issue.copyId, "available")
                repo.updateBookAvailability(schoolId, issue.bookId, +1)
            }
        } else {
            repo.updateBookAvailability(schoolId, issue.bookId, +1)
        }

        val daysOverdue = ChronoUnit.DAYS.between(issue.dueDate, today).coerceAtLeast(0).toInt()

        repo.appendAuditLog(schoolId, actorId, actorName, "RETURN_BOOK", "issue", issueId,
            metadata = mapOf("fine" to fine, "daysOverdue" to daysOverdue))

        LibraryEventBus.publish(BookReturned(schoolId, issue.bookId, issueId, issue.borrowerId, fine, req.returnCondition, actorId, actorName))
        if (req.returnCondition == "damaged") {
            LibraryEventBus.publish(BookDamaged(schoolId, issue.bookId, issueId, issue.copyId, req.damageNotes, actorId, actorName))
        }

        // Check reading badges
        if (issue.borrowerType == "student") {
            checkAndAwardBadges(schoolId, issue.borrowerId)

            // Check reading goal achievement
            runCatching { checkReadingGoalAchievement(schoolId, issue.borrowerId) }
        }

        // Notify first pending reserver that the book is now available
        runCatching { notifyAvailable(schoolId, issue.bookId) }
            .onFailure { /* don't fail the return if notification fails */ }

        val actualCap = if (fineCapEnabled) (book?.replacementCost ?: DefaultFineCalculator.FINE_CAP) else Double.MAX_VALUE

        return ReturnResultDto(
            issueId = issueId.toString(),
            bookTitle = book?.title ?: "",
            returnDate = today.toString(),
            daysOverdue = daysOverdue,
            fineAmount = fine,
            fineCapped = fineCapEnabled && fine >= actualCap,
            returnCondition = req.returnCondition,
        )
    }

    suspend fun renewBook(schoolId: UUID, issueId: UUID, actorId: UUID, actorName: String): RenewResultDto {
        val issue = repo.findIssueById(schoolId, issueId)
            ?: throw LibraryNotFoundException("Issue", issueId.toString())
        if (issue.status != "issued") throw LibraryConflictException("Book already returned")

        val settings = repo.getSettings(schoolId)
        val maxRenewals = settings?.maxRenewals ?: 2
        val loanDays = settings?.defaultLoanDays ?: 14

        if (issue.renewalCount >= maxRenewals) {
            throw LibraryConflictException("Max renewals reached ($maxRenewals)")
        }

        val pendingReservations = repo.listReservationsForBook(schoolId, issue.bookId, "pending")
        if (pendingReservations.isNotEmpty()) {
            throw LibraryConflictException("Cannot renew — book has pending reservations")
        }

        val oldDueDate = issue.dueDate
        val newDueDate = dueDateCalculator.calculate(LocalDate.now(), loanDays)
        repo.renewIssue(schoolId, issueId, newDueDate)

        val book = repo.findBookById(schoolId, issue.bookId)
        repo.appendAuditLog(schoolId, actorId, actorName, "RENEW_BOOK", "issue", issueId)

        LibraryEventBus.publish(BookRenewed(schoolId, issue.bookId, issueId, issue.borrowerId, newDueDate, issue.renewalCount + 1, actorId, actorName))

        runCatching {
            Notify.toUser(
                userId = issue.borrowerId,
                category = "library",
                title = "📖 Book Renewed: ${book?.title ?: "Book"}",
                body = "Your due date has been extended to $newDueDate. Renewal ${issue.renewalCount + 1} of $maxRenewals.",
            )
        }

        return RenewResultDto(
            issueId = issueId.toString(),
            bookTitle = book?.title ?: "",
            oldDueDate = oldDueDate.toString(),
            newDueDate = newDueDate.toString(),
            renewalCount = issue.renewalCount + 1,
            maxRenewals = maxRenewals,
        )
    }

    suspend fun markLost(schoolId: UUID, issueId: UUID, actorId: UUID, actorName: String): LostResultDto {
        val issue = repo.findIssueById(schoolId, issueId)
            ?: throw LibraryNotFoundException("Issue", issueId.toString())
        if (issue.status != "issued") throw LibraryConflictException("Book not currently issued")

        val book = repo.findBookById(schoolId, issue.bookId)
            ?: throw LibraryNotFoundException("Book", issue.bookId.toString())
        val fine = book.replacementCost ?: 100.0

        repo.markIssueLost(schoolId, issueId, fine)
        if (issue.copyId != null) {
            repo.updateCopyStatus(schoolId, issue.copyId, "lost")
        }
        repo.decrementBookTotalCopies(schoolId, issue.bookId)

        repo.appendAuditLog(schoolId, actorId, actorName, "MARK_LOST", "issue", issueId,
            metadata = mapOf("fine" to fine))

        LibraryEventBus.publish(BookMarkedLost(schoolId, issue.bookId, issueId, issue.borrowerId, fine, actorId, actorName))

        runCatching {
            Notify.toUser(
                userId = issue.borrowerId,
                category = "library",
                title = "📕 Book Marked Lost: ${book.title}",
                body = "Your book has been marked as lost. A fine of ₹$fine has been charged.",
            )
        }

        return LostResultDto(
            issueId = issueId.toString(),
            bookTitle = book.title,
            fineAmount = fine,
            fineStatus = "pending",
            copyStatus = "lost",
        )
    }

    // ── Fines ──────────────────────────────────────────────────────────────

    suspend fun payFine(schoolId: UUID, issueId: UUID, actorId: UUID, actorName: String) {
        val updated = repo.payFine(schoolId, issueId)
        if (updated == 0) throw LibraryNotFoundException("Issue with pending fine", issueId.toString())
        repo.appendAuditLog(schoolId, actorId, actorName, "PAY_FINE", "issue", issueId)

        val issue = repo.findIssueById(schoolId, issueId)
        val book = issue?.let { repo.findBookById(schoolId, it.bookId) }

        if (issue != null && book != null) {
            LibraryEventBus.publish(FinePaid(schoolId, issueId, issue.bookId, issue.borrowerId, issue.fineAmount, actorId, actorName))
        }

        runCatching {
            Notify.toUser(
                userId = issue?.borrowerId ?: actorId,
                category = "library",
                title = "💰 Fine Paid: ${book?.title ?: "Book"}",
                body = "Your fine of ₹${issue?.fineAmount ?: 0.0} has been paid.",
            )
        }
    }

    suspend fun waiveFine(schoolId: UUID, issueId: UUID, reason: String, actorId: UUID, actorName: String) {
        if (reason.isBlank() || reason.length < 5) throw LibraryValidationException("reason", "Waiver reason must be at least 5 characters")
        if (reason.length > 500) throw LibraryValidationException("reason", "Waiver reason must be at most 500 characters")
        val sanitizedReason = sanitizeHtml(reason)
        val issue = repo.findIssueById(schoolId, issueId)
            ?: throw LibraryNotFoundException("Issue", issueId.toString())
        val fineAmount = issue.fineAmount
        val updated = repo.waiveFine(schoolId, issueId, actorId, sanitizedReason)
        if (updated == 0) throw LibraryNotFoundException("Issue with pending fine", issueId.toString())
        repo.appendAuditLog(schoolId, actorId, actorName, "WAIVE_FINE", "issue", issueId,
            metadata = mapOf("reason" to sanitizedReason, "amount" to fineAmount))

        val book = repo.findBookById(schoolId, issue.bookId)

        LibraryEventBus.publish(FineWaived(schoolId, issueId, issue.bookId, issue.borrowerId, issue.fineAmount, sanitizedReason, actorId, actorName))

        runCatching {
            Notify.toUser(
                userId = issue.borrowerId,
                category = "library",
                title = "✅ Fine Waived: ${book?.title ?: "Book"}",
                body = "Your fine of ₹${issue.fineAmount} has been waived. Reason: $sanitizedReason",
            )
        }
    }

    // ── Reservations ────────────────────────────────────────────────────────

    suspend fun reserveBook(schoolId: UUID, bookId: UUID, userId: UUID, userName: String, userType: String): LibraryReservationDto {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        if (book.availableCopies > 0) {
            throw LibraryConflictException("Book is available — no need to reserve")
        }
        val reservationId = repo.createReservation(schoolId, bookId, userId, userName, userType)
        LibraryEventBus.publish(BookReserved(schoolId, bookId, reservationId, userId, userName, userId, userName))
        val reservations = repo.listReservationsForBook(schoolId, bookId, "pending")
        val position = reservations.indexOfFirst { it.id == reservationId } + 1
        val row = reservations.first { it.id == reservationId }
        return LibraryReservationDto(
            id = reservationId.toString(),
            bookId = bookId.toString(),
            bookTitle = book.title,
            reservedBy = userId.toString(),
            reservedByName = userName,
            reservedByType = userType,
            status = row.status,
            createdAt = row.createdAt.toString(),
            fulfilledAt = row.fulfilledAt?.toString(),
            waitlistPosition = position,
        )
    }

    suspend fun listReservationsForUser(schoolId: UUID, userId: UUID): List<LibraryReservationDto> {
        return repo.listReservationsForUser(schoolId, userId).map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            LibraryReservationDto(
                id = row.id.toString(),
                bookId = row.bookId.toString(),
                bookTitle = book?.title ?: "",
                reservedBy = row.reservedBy.toString(),
                reservedByName = row.reservedByName,
                reservedByType = row.reservedByType,
                status = row.status,
                createdAt = row.createdAt.toString(),
                fulfilledAt = row.fulfilledAt?.toString(),
            )
        }
    }

    suspend fun cancelReservation(schoolId: UUID, reservationId: UUID, userId: UUID) {
        val updated = repo.cancelReservation(schoolId, reservationId, userId)
        if (updated == 0) throw LibraryNotFoundException("Reservation", reservationId.toString())
        LibraryEventBus.publish(ReservationCancelled(schoolId, java.util.UUID(0, 0), reservationId, userId, "user_cancelled", userId, "user"))
    }

    suspend fun listReservationsForBook(schoolId: UUID, bookId: UUID): List<LibraryReservationDto> {
        return repo.listReservationsForBook(schoolId, bookId, null).map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            LibraryReservationDto(
                id = row.id.toString(),
                bookId = row.bookId.toString(),
                bookTitle = book?.title ?: "",
                reservedBy = row.reservedBy.toString(),
                reservedByName = row.reservedByName,
                reservedByType = row.reservedByType,
                status = row.status,
                createdAt = row.createdAt.toString(),
                fulfilledAt = row.fulfilledAt?.toString(),
            )
        }
    }

    suspend fun fulfillReservation(schoolId: UUID, reservationId: UUID, actorId: UUID, actorName: String) {
        val updated = repo.updateReservationStatus(schoolId, reservationId, "fulfilled")
        if (updated == 0) throw LibraryNotFoundException("Reservation", reservationId.toString())
        repo.appendAuditLog(schoolId, actorId, actorName, "FULFILL_RESERVATION", "reservation", reservationId)
    }

    suspend fun listIssuedForBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryIssueDto> {
        return repo.listIssuesForBorrower(schoolId, borrowerId, "issued").map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            row.toDto(book?.title ?: "")
        }
    }

    suspend fun listHistoryForBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryIssueDto> {
        return repo.listIssuesForBorrower(schoolId, borrowerId, null).map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            row.toDto(book?.title ?: "")
        }
    }

    suspend fun studentRenewBook(schoolId: UUID, issueId: UUID, borrowerId: UUID): RenewResultDto {
        val issue = repo.findIssueById(schoolId, issueId)
            ?: throw LibraryNotFoundException("Issue", issueId.toString())
        if (issue.status != "issued") throw LibraryConflictException("Book already returned")
        if (issue.borrowerId != borrowerId) throw LibraryPermissionException("Cannot renew another user's book")

        val settings = repo.getSettings(schoolId)
        val maxRenewals = settings?.maxRenewals ?: 2
        val loanDays = settings?.defaultLoanDays ?: 14

        if (issue.renewalCount >= maxRenewals) {
            throw LibraryConflictException("Max renewals reached ($maxRenewals)")
        }

        val pendingReservations = repo.listReservationsForBook(schoolId, issue.bookId, "pending")
        if (pendingReservations.isNotEmpty()) {
            throw LibraryConflictException("Cannot renew — book has pending reservations")
        }

        val newDueDate = dueDateCalculator.calculate(LocalDate.now(), loanDays)
        repo.renewIssue(schoolId, issueId, newDueDate)
        val book = repo.findBookById(schoolId, issue.bookId)
        return RenewResultDto(
            issueId = issueId.toString(),
            bookTitle = book?.title ?: "",
            oldDueDate = issue.dueDate.toString(),
            newDueDate = newDueDate.toString(),
            renewalCount = issue.renewalCount + 1,
            maxRenewals = maxRenewals,
        )
    }

    /**
     * When a book becomes available (e.g. after return), check for pending
     * reservations and notify the first reserver. Updates reservation status
     * from "pending" to "notified".
     */
    suspend fun notifyAvailable(schoolId: UUID, bookId: UUID) {
        val reservations = repo.listReservationsForBook(schoolId, bookId, "pending")
        if (reservations.isNotEmpty()) {
            val first = reservations.first()
            val book = repo.findBookById(schoolId, bookId)
            val settings = repo.getSettings(schoolId)
            val timeoutDays = settings?.reservationTimeoutDays ?: 7

            repo.updateReservationStatus(schoolId, first.id, "notified")

            Notify.toUser(
                userId = first.reservedBy,
                category = "library",
                title = "📚 Book Available: ${book?.title ?: "Book"}",
                body = "Your reserved book is now available. Please pick it up within $timeoutDays days.",
                schoolId = schoolId,
                deepLink = "/student/library",
                refType = "library_reservation",
                refId = first.id.toString(),
            )
        }

        // Notify users who have this book on their wishlist (no reservation)
        runCatching {
            val book = repo.findBookById(schoolId, bookId) ?: return@runCatching
            if (book.availableCopies > 0) {
                val wishlistedBy = repo.listWishlistForBook(schoolId, bookId)
                for (wishlistEntry in wishlistedBy) {
                    // Skip if this user already has a pending reservation
                    val hasReservation = reservations.any { it.reservedBy == wishlistEntry.studentId }
                    if (!hasReservation) {
                        Notify.toUser(
                            userId = wishlistEntry.studentId,
                            category = "library",
                            title = "📚 Wishlist Book Available: ${book.title}",
                            body = "'${book.title}' from your wishlist is now available with ${book.availableCopies} copies.",
                            schoolId = schoolId,
                            deepLink = "/student/library/book/${bookId}",
                            refType = "library_wishlist",
                            refId = bookId.toString(),
                        )
                    }
                }
            }
        }
    }

    // ── Categories ──────────────────────────────────────────────────────────

    suspend fun listCategories(schoolId: UUID): List<LibraryCategoryDto> {
        val cacheKey = LibraryCacheKeys.categories(schoolId)
        return LibraryCache.getOrPut(cacheKey, LibraryCacheKeys.CATEGORIES_TTL_MINUTES) {
            repo.listCategories(schoolId).map { it.toDto() }
        }
    }

    suspend fun createCategory(schoolId: UUID, req: CreateCategoryRequest, actorId: UUID, actorName: String): UUID {
        if (req.name.isBlank()) throw LibraryValidationException("name", "Category name is required")
        val id = repo.createCategory(schoolId, req.name, req.color, req.icon)
        repo.appendAuditLog(schoolId, actorId, actorName, "CREATE_CATEGORY", "category", id)
        LibraryEventBus.publish(CategoryCreated(schoolId, id, req.name, actorId, actorName))
        return id
    }

    suspend fun updateCategory(schoolId: UUID, categoryId: UUID, req: UpdateCategoryRequest, actorId: UUID, actorName: String) {
        repo.updateCategory(schoolId, categoryId, req.name, req.color, req.icon)
        repo.appendAuditLog(schoolId, actorId, actorName, "UPDATE_CATEGORY", "category", categoryId)
        LibraryEventBus.publish(CategoryUpdated(schoolId, categoryId, req.name ?: "", actorId, actorName))
    }

    suspend fun deleteCategory(schoolId: UUID, categoryId: UUID, actorId: UUID, actorName: String) {
        val deleted = repo.deleteCategory(schoolId, categoryId)
        if (deleted == 0) throw LibraryNotFoundException("Category", categoryId.toString())
        repo.appendAuditLog(schoolId, actorId, actorName, "DELETE_CATEGORY", "category", categoryId)
        LibraryEventBus.publish(CategoryDeleted(schoolId, categoryId, "", actorId, actorName))
    }

    suspend fun reorderCategories(schoolId: UUID, orders: List<Pair<String, Int>>, actorId: UUID, actorName: String) {
        val uuidOrders = orders.map { UUID.fromString(it.first) to it.second }
        repo.reorderCategories(schoolId, uuidOrders)
        repo.appendAuditLog(schoolId, actorId, actorName, "REORDER_CATEGORIES", "category", null)
    }

    // ── Settings ────────────────────────────────────────────────────────────

    suspend fun getSettings(schoolId: UUID): LibrarySettingsDto {
        val cacheKey = LibraryCacheKeys.settings(schoolId)
        return LibraryCache.getOrPut(cacheKey, LibraryCacheKeys.SETTINGS_TTL_MINUTES) {
            val row = repo.getSettings(schoolId)
                ?: return@getOrPut LibrarySettingsDto()
            LibrarySettingsDto(
                defaultLoanDays = row.defaultLoanDays,
                finePerDay = row.finePerDay,
                maxBooksPerStudent = row.maxBooksPerStudent,
                maxRenewals = row.maxRenewals,
                reservationTimeoutDays = row.reservationTimeoutDays,
                dueReminderDays = row.dueReminderDays,
                fineCapEnabled = row.fineCapEnabled,
                quickIssueEnabled = row.quickIssueEnabled,
                bulkReturnEnabled = row.bulkReturnEnabled,
                leaderboardEnabled = row.leaderboardEnabled,
            )
        }
    }

    suspend fun updateSettings(schoolId: UUID, req: UpdateSettingsRequest, actorId: UUID, actorName: String): LibrarySettingsDto {
        val updates = mutableMapOf<String, Any?>()
        req.defaultLoanDays?.let { updates["defaultLoanDays"] = it }
        req.finePerDay?.let { updates["finePerDay"] = it }
        req.maxBooksPerStudent?.let { updates["maxBooksPerStudent"] = it }
        req.maxRenewals?.let { updates["maxRenewals"] = it }
        req.reservationTimeoutDays?.let { updates["reservationTimeoutDays"] = it }
        req.dueReminderDays?.let { updates["dueReminderDays"] = it }
        req.fineCapEnabled?.let { updates["fineCapEnabled"] = it }
        req.quickIssueEnabled?.let { updates["quickIssueEnabled"] = it }
        req.bulkReturnEnabled?.let { updates["bulkReturnEnabled"] = it }
        req.leaderboardEnabled?.let { updates["leaderboardEnabled"] = it }

        val row = repo.upsertSettings(schoolId, updates)
        repo.appendAuditLog(schoolId, actorId, actorName, "UPDATE_SETTINGS", "settings", row?.id)
        LibraryEventBus.publish(SettingsUpdated(schoolId, actorId, actorName))
        return getSettings(schoolId)
    }

    // ── Dashboard ───────────────────────────────────────────────────────────

    suspend fun getDashboard(schoolId: UUID): LibraryDashboardDto {
        val cacheKey = LibraryCacheKeys.dashboard(schoolId)
        return LibraryCache.getOrPut(cacheKey, LibraryCacheKeys.DASHBOARD_TTL_MINUTES) {
            val totalBooks = repo.countBooks(schoolId)
            val totalCopies = repo.sumTotalCopies(schoolId)
            val availableCopies = repo.sumAvailableCopies(schoolId)
            val issuedCopies = repo.countIssued(schoolId)
            val overdueBooks = repo.countOverdue(schoolId)
            val activeReservations = repo.countActiveReservations(schoolId)
            val lostBooks = repo.countLost(schoolId)
            val (fineCount, fineAmount) = repo.countOutstandingFines(schoolId)

            LibraryDashboardDto(
                totalBooks = totalBooks,
                totalCopies = totalCopies,
                availableCopies = availableCopies,
                issuedCopies = issuedCopies,
                overdueBooks = overdueBooks,
                activeReservations = activeReservations,
                lostBooks = lostBooks,
                outstandingFinesCount = fineCount,
                outstandingFinesAmount = fineAmount,
            )
        }
    }

    // ── Audit Log ───────────────────────────────────────────────────────────

    suspend fun listAuditLog(schoolId: UUID, page: Int, limit: Int): List<LibraryAuditLogDto> {
        val (rows, _) = repo.listAuditLog(schoolId, page, limit)
        return rows.map { it.toDto() }
    }

    // ── Cursor-based pagination (spec §17) ──────────────────────────────────
    suspend fun listAuditLogCursor(schoolId: UUID, cursor: String?, limit: Int): CursorPageResponse<LibraryAuditLogDto> {
        val decoded = cursor?.let { CursorCodec.decode(it) }
        val (rows, hasMore) = repo.listAuditLogCursor(schoolId, decoded, limit)
        val dtos = rows.map { it.toDto() }
        val nextCursor = if (hasMore && rows.isNotEmpty()) {
            CursorCodec.encode(rows.last().createdAt, rows.last().id)
        } else null
        return CursorPageResponse(data = dtos, nextCursor = nextCursor, hasMore = hasMore)
    }

    suspend fun listIssuesCursor(schoolId: UUID, status: String?, cursor: String?, limit: Int): CursorPageResponse<LibraryIssueDto> {
        val decoded = cursor?.let { CursorCodec.decode(it) }
        val (rows, hasMore) = repo.listIssuesCursor(schoolId, status, decoded, limit)
        val dtos = rows.map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            row.toDto(book?.title ?: "")
        }
        val nextCursor = if (hasMore && rows.isNotEmpty()) {
            CursorCodec.encode(rows.last().createdAt, rows.last().id)
        } else null
        return CursorPageResponse(data = dtos, nextCursor = nextCursor, hasMore = hasMore)
    }

    suspend fun verifyAuditHashChain(schoolId: UUID): AuditHashVerifyResultDto {
        val entries = repo.listAllAuditHashes(schoolId)
        if (entries.isEmpty()) return AuditHashVerifyResultDto(verified = true, totalEntries = 0, checkedEntries = 0)

        // First entry should be the GENESIS entry with a deterministic hash.
        val genesisPayload = "GENESIS|$schoolId"
        val expectedGenesisHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(genesisPayload.toByteArray())
            .joinToString("") { "%02x".format(it) }

        var prevHash: String
        val firstEntry = entries.first()

        if (firstEntry.second == expectedGenesisHash) {
            // Genesis entry verified — chain starts from it
            prevHash = firstEntry.second
        } else {
            // No genesis entry (legacy data) — fall back to empty-string anchor
            prevHash = ""
        }

        val startIndex = if (firstEntry.second == expectedGenesisHash) 1 else 0
        for (i in startIndex until entries.size) {
            val (id, hash, payload) = entries[i]
            val expected = java.security.MessageDigest.getInstance("SHA-256")
                .digest((prevHash + "|" + payload).toByteArray())
                .joinToString("") { "%02x".format(it) }
            if (hash != expected) {
                return AuditHashVerifyResultDto(verified = false, brokenAt = id.toString(), totalEntries = entries.size, checkedEntries = i + 1)
            }
            prevHash = hash
        }
        return AuditHashVerifyResultDto(verified = true, totalEntries = entries.size, checkedEntries = entries.size)
    }

    // ── Outstanding Fines ───────────────────────────────────────────────────

    suspend fun listOutstandingFines(schoolId: UUID, page: Int, limit: Int): Pair<List<LibraryIssueDto>, Int> {
        val (rows, total) = repo.listOutstandingFines(schoolId, page, limit)
        val dtos = rows.map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            row.toDto(book?.title ?: "")
        }
        return dtos to total
    }

    suspend fun getFineSummary(schoolId: UUID): FineSummaryDto {
        val (count, amount) = repo.countOutstandingFines(schoolId)
        val collected = repo.finesCollectedThisMonth(schoolId)
        return FineSummaryDto(
            outstandingCount = count,
            outstandingAmount = amount,
            collectedThisMonth = collected,
        )
    }

    // ── Copy Update/Delete ──────────────────────────────────────────────────

    suspend fun updateCopy(schoolId: UUID, copyId: UUID, req: UpdateCopyRequest, actorId: UUID, actorName: String) {
        if (req.condition != null) {
            val validConditions = listOf("new", "good", "fair", "poor", "damaged")
            if (req.condition !in validConditions) throw LibraryValidationException("condition", "Invalid condition")
            repo.updateCopyCondition(schoolId, copyId, req.condition)
        }
        if (req.status != null) {
            val validStatuses = listOf("available", "issued", "repair", "lost", "reserved")
            if (req.status !in validStatuses) throw LibraryValidationException("status", "Invalid status")
            repo.updateCopyStatus(schoolId, copyId, req.status)
        }
        repo.appendAuditLog(schoolId, actorId, actorName, "UPDATE_COPY", "copy", copyId,
            metadata = mapOf("condition" to req.condition, "status" to req.status))
    }

    suspend fun deleteCopy(schoolId: UUID, copyId: UUID, actorId: UUID, actorName: String) {
        val deleted = repo.deleteCopy(schoolId, copyId)
        if (deleted == 0) throw LibraryConflictException("Copy not found or not in available status")
        repo.appendAuditLog(schoolId, actorId, actorName, "DELETE_COPY", "copy", copyId)
    }

    // ── Leaderboard ─────────────────────────────────────────────────────────

    suspend fun getLeaderboard(schoolId: UUID, limit: Int = 50): LeaderboardDto {
        val settings = repo.getSettings(schoolId)
        if (settings?.leaderboardEnabled != true) return LeaderboardDto(emptyList())

        val borrowers = repo.listIssuesForBorrower(schoolId, java.util.UUID(0, 0), null)
        val allIssues = repo.listIssues(schoolId, "returned", 1, 10000).first
        val counts = allIssues.groupingBy { it.borrowerId }.eachCount()
        val entries = counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapIndexed { index, (borrowerId, count) ->
                LeaderboardEntry(
                    rank = index + 1,
                    studentName = allIssues.firstOrNull { it.borrowerId == borrowerId }?.borrowerName ?: "Anonymous Reader",
                    booksRead = count,
                    anonymized = false,
                )
            }
        return LeaderboardDto(entries)
    }

    // ── Featured Book ───────────────────────────────────────────────────────

    suspend fun setFeaturedBook(schoolId: UUID, bookId: UUID, type: String, actorId: UUID, actorName: String) {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        if (type !in listOf("WEEK", "MONTH")) throw LibraryValidationException("type", "Type must be WEEK or MONTH")
        repo.setFeaturedBook(schoolId, bookId, type)
        repo.appendAuditLog(schoolId, actorId, actorName, "SET_FEATURED_BOOK", "book", bookId,
            metadata = mapOf("type" to type))
    }

    suspend fun getFeaturedBook(schoolId: UUID): FeaturedBookDto? {
        val settings = repo.getSettings(schoolId) ?: return null
        val bookId = settings.featuredBookId ?: return null
        val type = settings.featuredType ?: return null
        val updatedAt = settings.featuredUpdatedAt ?: return null
        val maxAgeDays = if (type == "WEEK") 7 else 30
        if (updatedAt.isBefore(Instant.now().minusSeconds(maxAgeDays.toLong() * 86400))) return null
        val book = repo.findBookById(schoolId, bookId) ?: return null
        val message = if (type == "WEEK") "Book of the Week" else "Book of the Month"
        return FeaturedBookDto(book = book.toDto(), type = type, message = message)
    }

    // ── Reading Streak ──────────────────────────────────────────────────────

    suspend fun getReadingStreak(schoolId: UUID, studentId: UUID): Int {
        val issues = repo.listIssuesForBorrower(schoolId, studentId, null)
        val activeDates = issues.mapNotNull { issue ->
            issue.issueDate
        }.toSet()
        if (activeDates.isEmpty()) return 0
        var streak = 0
        var date = LocalDate.now()
        while (date in activeDates || date == LocalDate.now()) {
            if (date in activeDates) {
                streak++
                date = date.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    // ── Announcements ───────────────────────────────────────────────────────

    suspend fun listAnnouncements(schoolId: UUID, activeOnly: Boolean): List<LibraryAnnouncementDto> {
        if (activeOnly) {
            val cacheKey = LibraryCacheKeys.activeAnnouncements(schoolId)
            return LibraryCache.getOrPut(cacheKey, LibraryCacheKeys.ANNOUNCEMENTS_TTL_MINUTES) {
                repo.listAnnouncements(schoolId, true).map { it.toDto() }
            }
        }
        return repo.listAnnouncements(schoolId, false).map { it.toDto() }
    }

    suspend fun createAnnouncement(schoolId: UUID, req: CreateAnnouncementRequest, actorId: UUID, actorName: String): UUID {
        val expiresAt = req.expiresAt?.let { Instant.parse(it) }
        val id = repo.createAnnouncement(schoolId, req.title, req.message, req.audience, actorId, actorName, expiresAt)
        repo.appendAuditLog(schoolId, actorId, actorName, "CREATE_ANNOUNCEMENT", "announcement", id)
        LibraryEventBus.publish(AnnouncementCreated(schoolId, id, req.title, actorId, actorName))

        runCatching {
            Notify.toUser(
                userId = actorId,
                category = "library",
                title = "📢 Announcement Published: ${req.title}",
                body = req.message.take(100),
                schoolId = schoolId,
                deepLink = "/library/announcements",
                refType = "library_announcement",
                refId = id.toString(),
            )
        }

        return id
    }

    suspend fun updateAnnouncement(schoolId: UUID, announcementId: UUID, req: UpdateAnnouncementRequest, actorId: UUID, actorName: String) {
        val expiresAt = req.expiresAt?.let { Instant.parse(it) }
        repo.updateAnnouncement(schoolId, announcementId, req.title, req.message, req.audience, expiresAt, req.isActive)
        repo.appendAuditLog(schoolId, actorId, actorName, "UPDATE_ANNOUNCEMENT", "announcement", announcementId)
    }

    // ── Wishlist ────────────────────────────────────────────────────────────

    suspend fun listWishlist(schoolId: UUID, studentId: UUID): List<LibraryWishlistDto> {
        return repo.listWishlist(schoolId, studentId).map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            LibraryWishlistDto(
                id = row.id.toString(),
                bookId = row.bookId.toString(),
                bookTitle = book?.title ?: "",
                bookAuthor = book?.author,
                coverUrl = book?.coverUrl,
                availableCopies = book?.availableCopies ?: 0,
                addedAt = row.createdAt.toString(),
            )
        }
    }

    suspend fun addToWishlist(schoolId: UUID, studentId: UUID, bookId: UUID): UUID {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        val count = repo.countWishlist(schoolId, studentId)
        if (count >= 50) throw LibraryConflictException("Wishlist limit reached (max 50)")
        val id = repo.addToWishlist(schoolId, studentId, bookId)
        LibraryEventBus.publish(WishlistAdded(schoolId, bookId, studentId, studentId, "student"))
        return id
    }

    suspend fun removeFromWishlist(schoolId: UUID, studentId: UUID, bookId: UUID) {
        repo.removeFromWishlist(schoolId, studentId, bookId)
        LibraryEventBus.publish(WishlistRemoved(schoolId, bookId, studentId, studentId, "student"))
    }

    // ── Reading Goals ───────────────────────────────────────────────────────

    suspend fun getReadingGoal(schoolId: UUID, studentId: UUID, period: String, targetYear: Int): LibraryReadingGoalDto? {
        val row = repo.getReadingGoal(schoolId, studentId, period, targetYear) ?: return null
        val booksRead = repo.listIssuesForBorrower(schoolId, studentId, "returned")
            .count { it.returnDate?.year == targetYear }
        val progress = if (row.goalCount > 0) (booksRead.toDouble() / row.goalCount * 100).coerceAtMost(100.0) else 0.0
        return LibraryReadingGoalDto(
            id = row.id.toString(),
            studentId = studentId.toString(),
            goalCount = row.goalCount,
            period = row.period,
            targetYear = row.targetYear,
            booksRead = booksRead,
            progressPercentage = progress,
            isAchieved = booksRead >= row.goalCount,
        )
    }

    suspend fun upsertReadingGoal(schoolId: UUID, studentId: UUID, req: CreateReadingGoalRequest): UUID {
        if (req.goalCount < 1) throw LibraryValidationException("goal_count", "Goal must be at least 1")
        val id = repo.upsertReadingGoal(schoolId, studentId, req.goalCount, req.period, req.targetYear)
        LibraryEventBus.publish(ReadingGoalSet(schoolId, studentId, req.goalCount, studentId, "student"))
        return id
    }

    // ── Acquisition Requests ────────────────────────────────────────────────

    suspend fun listAcquisitionRequests(schoolId: UUID, status: String?): List<LibraryAcquisitionRequestDto> {
        return repo.listAcquisitionRequests(schoolId, status).map { it.toDto() }
    }

    suspend fun createAcquisitionRequest(
        schoolId: UUID, requestedBy: UUID, requestedByName: String, requestedByType: String,
        req: CreateAcquisitionRequest,
    ): UUID {
        if (req.title.isBlank()) throw LibraryValidationException("title", "Title is required")
        val id = repo.createAcquisitionRequest(
            schoolId, requestedBy, requestedByName, requestedByType,
            req.title, req.author, req.isbn, req.publisher, req.reason, req.estimatedCost,
        )
        LibraryEventBus.publish(AcquisitionRequestSubmitted(schoolId, id, req.title, requestedBy, requestedBy, requestedByName))
        return id
    }

    suspend fun updateAcquisitionStatus(
        schoolId: UUID, requestId: UUID, status: String, actorId: UUID, actorName: String, orderLink: String?,
    ) {
        val validStatuses = listOf("pending", "approved", "rejected", "ordered", "received")
        if (status !in validStatuses) throw LibraryValidationException("status", "Invalid status")
        val oldStatus = repo.findAcquisitionRequest(schoolId, requestId)?.status ?: "pending"
        val updated = repo.updateAcquisitionStatus(schoolId, requestId, status, actorId, orderLink)
        if (updated == 0) throw LibraryNotFoundException("Acquisition request", requestId.toString())
        repo.appendAuditLog(schoolId, actorId, actorName, "UPDATE_ACQUISITION", "acquisition", requestId,
            metadata = mapOf("status" to status))
        LibraryEventBus.publish(AcquisitionStatusChanged(schoolId, requestId, oldStatus, status, actorId, actorName))

        val acq = repo.findAcquisitionRequest(schoolId, requestId)
        if (acq != null) {
            when (status) {
                "approved" -> runCatching {
                    Notify.toUser(
                        userId = acq.requestedBy,
                        category = "library",
                        title = "📚 Acquisition Approved: ${acq.title}",
                        body = "Your book request '${acq.title}' has been approved and will be ordered.",
                        schoolId = schoolId,
                        deepLink = "/student/library/acquisitions",
                        refType = "library_acquisition",
                        refId = requestId.toString(),
                    )
                }
                "received" -> runCatching {
                    Notify.toUser(
                        userId = acq.requestedBy,
                        category = "library",
                        title = "📚 Acquisition Received: ${acq.title}",
                        body = "Your requested book '${acq.title}' has been received and will be added to the catalog.",
                        schoolId = schoolId,
                        deepLink = "/student/library/acquisitions",
                        refType = "library_acquisition",
                        refId = requestId.toString(),
                    )
                }
            }
        }
    }

    suspend fun convertAcquisitionToBook(schoolId: UUID, requestId: UUID, actorId: UUID, actorName: String): LibraryBookDto {
        val acq = repo.findAcquisitionRequest(schoolId, requestId)
            ?: throw LibraryNotFoundException("Acquisition request", requestId.toString())
        if (acq.status != "received") throw LibraryConflictException("Acquisition request must be in 'received' status to convert")
        if (acq.convertedBookId != null) throw LibraryConflictException("Acquisition request already converted to a book")

        val createReq = CreateBookRequest(
            isbn = acq.isbn,
            title = acq.title,
            author = acq.author,
            publisher = acq.publisher,
            totalCopies = 1,
        )
        val bookId = createBook(schoolId, createReq, actorId, actorName)
        repo.setConvertedBookId(schoolId, requestId, bookId)
        repo.appendAuditLog(schoolId, actorId, actorName, "CONVERT_ACQUISITION", "acquisition", requestId,
            metadata = mapOf("bookId" to bookId.toString()))

        return repo.findBookById(schoolId, bookId)?.toDto()
            ?: throw LibraryNotFoundException("Book", bookId.toString())
    }

    // ── Badges ──────────────────────────────────────────────────────────────

    suspend fun listBadges(schoolId: UUID, studentId: UUID): List<LibraryBadgeDto> {
        val earned = repo.listBadges(schoolId, studentId).map { it.badgeType }
        return ALL_BADGES.map { (type, name, icon) ->
            LibraryBadgeDto(
                badgeType = type,
                badgeName = name,
                badgeIcon = icon,
                isEarned = type in earned,
                earnedAt = null,
            )
        }
    }

    suspend fun checkAndAwardBadges(schoolId: UUID, studentId: UUID) {
        val returnedCount = repo.listIssuesForBorrower(schoolId, studentId, "returned").size
        val badgeMap = mapOf(
            1 to "first_book",
            5 to "5_books",
            10 to "10_books",
            25 to "25_books",
            50 to "50_books",
            100 to "100_books",
        )
        val badgeNames = mapOf(
            "first_book" to "First Book",
            "5_books" to "5 Books Read",
            "10_books" to "10 Books Read",
            "25_books" to "25 Books Read",
            "50_books" to "50 Books Read",
            "100_books" to "100 Books Read",
        )
        badgeMap.forEach { (threshold, badgeType) ->
            if (returnedCount >= threshold) {
                val awarded = repo.awardBadge(schoolId, studentId, badgeType)
                if (awarded) {
                    val badgeName = badgeNames[badgeType] ?: badgeType
                    LibraryEventBus.publish(BadgeAwarded(schoolId, studentId, badgeType, badgeName))

                    runCatching {
                        Notify.toUser(
                            userId = studentId,
                            category = "library",
                            title = "🏆 Badge Earned: $badgeName",
                            body = "Congratulations! You earned the '$badgeName' badge for reading $returnedCount books.",
                            schoolId = schoolId,
                            deepLink = "/student/library/profile",
                            refType = "library_badge",
                            refId = badgeType,
                        )
                    }
                }
            }
        }
    }

    private suspend fun checkReadingGoalAchievement(schoolId: UUID, studentId: UUID) {
        val year = LocalDate.now().year
        val goal = repo.getReadingGoal(schoolId, studentId, "yearly", year) ?: return
        val booksRead = repo.listIssuesForBorrower(schoolId, studentId, "returned")
            .count { it.returnDate?.year == year }
        if (booksRead >= goal.goalCount) {
            Notify.toUser(
                userId = studentId,
                category = "library",
                title = "🎯 Reading Goal Achieved!",
                body = "Congratulations! You've read $booksRead books this year, reaching your goal of ${goal.goalCount}.",
                schoolId = schoolId,
                deepLink = "/student/library/profile",
                refType = "library_reading_goal",
                refId = goal.id.toString(),
            )
        }
    }

    // ── Discussions ─────────────────────────────────────────────────────────

    suspend fun listDiscussions(schoolId: UUID, bookId: UUID): List<LibraryDiscussionMessageDto> {
        return repo.listDiscussions(schoolId, bookId).map { it.toDto() }
    }

    suspend fun postDiscussion(schoolId: UUID, bookId: UUID, studentId: UUID, studentName: String, message: String): UUID {
        if (message.isBlank()) throw LibraryValidationException("message", "Message is required")
        return repo.postDiscussion(schoolId, bookId, studentId, studentName, message)
    }

    suspend fun deleteDiscussion(schoolId: UUID, discussionId: UUID, deletedBy: UUID) {
        val updated = repo.deleteDiscussion(schoolId, discussionId, deletedBy)
        if (updated == 0) throw LibraryNotFoundException("Discussion", discussionId.toString())
    }

    // ── Quick Issue ─────────────────────────────────────────────────────────

    suspend fun quickIssue(schoolId: UUID, barcode: String, borrowerId: UUID, borrowerType: String, borrowerName: String, actorId: UUID, actorName: String, idempotencyKey: String? = null): QuickIssueResultDto {
        if (idempotencyKey != null) {
            val cacheKey = "$schoolId:$idempotencyKey"
            quickIssueIdempotencyCache[cacheKey]?.let { return it }
        }

        val copy = repo.findCopyByBarcode(schoolId, barcode)
            ?: throw LibraryNotFoundException("Copy with barcode", barcode)
        if (copy.status != "available") throw LibraryConflictException("Copy not available")

        val book = repo.findBookById(schoolId, copy.bookId)
            ?: throw LibraryNotFoundException("Book", copy.bookId.toString())

        val settings = repo.getSettings(schoolId)
        val loanDays = settings?.defaultLoanDays ?: 14
        val maxBooks = settings?.maxBooksPerStudent ?: 3

        if (borrowerType == "student") {
            val activeCount = repo.countActiveIssuesForBorrower(schoolId, borrowerId)
            if (activeCount >= maxBooks) {
                throw LibraryConflictException("Student has reached max books limit ($maxBooks)")
            }
        }

        val today = LocalDate.now()
        val dueDate = dueDateCalculator.calculate(today, loanDays)
        val issueId = repo.createIssue(schoolId, copy.bookId, copy.id, borrowerId, borrowerType, borrowerName, today, dueDate)
        val updated = repo.updateCopyStatusConditional(schoolId, copy.id, "available", "issued")
        if (updated == 0) throw LibraryConflictException("COPY_ALREADY_ISSUED")
        repo.updateBookAvailability(schoolId, copy.bookId, -1)

        repo.appendAuditLog(schoolId, actorId, actorName, "QUICK_ISSUE", "issue", issueId)

        val result = QuickIssueResultDto(
            issueId = issueId.toString(),
            bookTitle = book.title,
            copyNumber = copy.copyNumber,
            borrowerName = borrowerName,
            dueDate = dueDate.toString(),
        )

        if (idempotencyKey != null) {
            quickIssueIdempotencyCache["$schoolId:$idempotencyKey"] = result
        }

        return result
    }

    // ── Bulk Return ─────────────────────────────────────────────────────────

    suspend fun bulkReturn(schoolId: UUID, barcodes: List<String>, actorId: UUID, actorName: String): List<BulkReturnResultDto> {
        val results = mutableListOf<BulkReturnResultDto>()
        for (barcode in barcodes) {
            try {
                val copy = repo.findCopyByBarcode(schoolId, barcode)
                if (copy == null) {
                    results.add(BulkReturnResultDto(issueId = "", bookTitle = "", returnDate = LocalDate.now().toString(), success = false, error = "Barcode not found: $barcode"))
                    continue
                }
                val issue = repo.findActiveIssueForCopy(schoolId, copy.id)
                if (issue == null) {
                    results.add(BulkReturnResultDto(issueId = "", bookTitle = "", returnDate = LocalDate.now().toString(), success = false, error = "No active issue for barcode: $barcode"))
                    continue
                }
                val result = returnBook(schoolId, ReturnBookRequest(issueId = issue.id.toString()), actorId, actorName)
                results.add(BulkReturnResultDto(
                    issueId = result.issueId,
                    bookTitle = result.bookTitle,
                    returnDate = result.returnDate,
                    fineAmount = result.fineAmount,
                    success = true,
                ))
            } catch (e: Exception) {
                results.add(BulkReturnResultDto(issueId = "", bookTitle = "", returnDate = LocalDate.now().toString(), success = false, error = e.message))
            }
        }
        return results
    }

    // ── Student Profile ─────────────────────────────────────────────────────

    suspend fun getStudentProfile(schoolId: UUID, studentId: UUID): StudentLibraryProfileDto {
        val issues = repo.listIssuesForBorrower(schoolId, studentId, null)
        val returned = issues.count { it.status == "returned" }
        val currentlyIssued = issues.count { it.status == "issued" }
        val overdue = issues.count { it.status == "issued" && it.dueDate.isBefore(LocalDate.now()) }
        val outstandingFine = issues.filter { it.fineStatus == "pending" }.sumOf { it.fineAmount }
        val badges = repo.listBadges(schoolId, studentId).size

        val settings = repo.getSettings(schoolId)
        val maxBorrowingLimit = settings?.maxBooksPerStudent ?: 3

        val mostReadCategories = issues
            .filter { it.status == "returned" }
            .mapNotNull { repo.findBookById(schoolId, it.bookId)?.category }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { CategoryStatDto(category = it.key, count = it.value) }

        return StudentLibraryProfileDto(
            totalBooksRead = returned,
            currentlyIssued = currentlyIssued,
            overdueCount = overdue,
            outstandingFine = outstandingFine,
            badgesEarned = badges,
            maxBorrowingLimit = maxBorrowingLimit,
            mostReadCategories = mostReadCategories,
        )
    }

    // ── List Issues (admin) ─────────────────────────────────────────────────

    suspend fun listIssues(schoolId: UUID, status: String?, page: Int, limit: Int): Pair<List<LibraryIssueDto>, Int> {
        val (rows, total) = repo.listIssues(schoolId, status, page, limit)
        val dtos = rows.map { row ->
            val book = repo.findBookById(schoolId, row.bookId)
            row.toDto(book?.title ?: "")
        }
        return dtos to total
    }

    // ── Archive / Unarchive ─────────────────────────────────────────────────

    suspend fun updateBookCover(schoolId: UUID, bookId: UUID, coverUrl: String, actorId: UUID, actorName: String): LibraryBookDto {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        repo.updateBook(schoolId, bookId, mapOf("coverUrl" to coverUrl))
        repo.appendAuditLog(schoolId, actorId, actorName, "UPLOAD_COVER", "book", bookId)
        LibraryEventBus.publish(CoverUploaded(schoolId, bookId, coverUrl, actorId, actorName))
        return repo.findBookById(schoolId, bookId)?.toDto()
            ?: throw LibraryNotFoundException("Book", bookId.toString())
    }

    suspend fun archiveBook(schoolId: UUID, bookId: UUID, actorId: UUID, actorName: String): ArchiveResultDto {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        if (book.isArchived) throw LibraryConflictException("Book is already archived")
        repo.updateBook(schoolId, bookId, mapOf("isArchived" to true))
        repo.appendAuditLog(schoolId, actorId, actorName, "ARCHIVE_BOOK", "book", bookId)
        LibraryEventBus.publish(BookArchived(schoolId, bookId, actorId, actorName))

        runCatching {
            Notify.toUser(
                userId = actorId,
                category = "library",
                title = "📚 Book Archived: ${book.title}",
                body = "Book '${book.title}' has been archived and is no longer visible to students.",
                schoolId = schoolId,
                deepLink = "/admin/library/books",
                refType = "library_book",
                refId = bookId.toString(),
            )
        }

        return ArchiveResultDto(bookId = bookId.toString(), title = book.title, isArchived = true)
    }

    suspend fun unarchiveBook(schoolId: UUID, bookId: UUID, actorId: UUID, actorName: String): ArchiveResultDto {
        val book = repo.findBookById(schoolId, bookId)
            ?: throw LibraryNotFoundException("Book", bookId.toString())
        repo.updateBook(schoolId, bookId, mapOf("isArchived" to false))
        repo.appendAuditLog(schoolId, actorId, actorName, "UNARCHIVE_BOOK", "book", bookId)
        LibraryEventBus.publish(BookUnarchived(schoolId, bookId, actorId, actorName))
        return ArchiveResultDto(bookId = bookId.toString(), title = book.title, isArchived = false)
    }

    // ── Trending ────────────────────────────────────────────────────────────

    suspend fun listTrending(schoolId: UUID, limit: Int = 10): List<TrendingBookDto> {
        val cacheKey = LibraryCacheKeys.trending(schoolId)
        return LibraryCache.getOrPut(cacheKey, LibraryCacheKeys.TRENDING_TTL_MINUTES) {
            val since = LocalDate.now().minusDays(30)
            val counts = repo.countIssuesSince(schoolId, since).take(limit)
            counts.mapNotNull { (bookId, count) ->
                val book = repo.findBookById(schoolId, bookId) ?: return@mapNotNull null
                TrendingBookDto(
                    bookId = bookId.toString(),
                    title = book.title,
                    author = book.author,
                    coverUrl = book.coverUrl,
                    issueCount = count,
                )
            }
        }
    }

    // ── Recommendations ─────────────────────────────────────────────────────

    suspend fun listRecommendations(schoolId: UUID, studentId: UUID, limit: Int = 10): List<RecommendationDto> {
        val readBookIds = repo.listReturnedBooksByBorrower(schoolId, studentId).toSet()
        val readBooks = readBookIds.mapNotNull { repo.findBookById(schoolId, it) }
        val preferredCategories = readBooks.mapNotNull { it.category }.distinct()

        val recommendations = mutableListOf<RecommendationDto>()
        for (category in preferredCategories) {
            val books = repo.listBooksByCategory(schoolId, category, limit)
            for (book in books) {
                if (book.id !in readBookIds) {
                    recommendations.add(RecommendationDto(
                        bookId = book.id.toString(),
                        title = book.title,
                        author = book.author,
                        coverUrl = book.coverUrl,
                        category = book.category,
                        reason = "Because you read books in $category",
                    ))
                }
            }
            if (recommendations.size >= limit) break
        }
        return recommendations.take(limit)
    }

    // ── Copy Repair ─────────────────────────────────────────────────────────

    suspend fun repairCopy(schoolId: UUID, copyId: UUID, actorId: UUID, actorName: String): RepairCopyResultDto {
        val copies = repo.listCopiesByStatus(schoolId, "repair")
        val copy = copies.find { it.id == copyId }
            ?: throw LibraryNotFoundException("Copy in repair", copyId.toString())
        val book = repo.findBookById(schoolId, copy.bookId)
            ?: throw LibraryNotFoundException("Book", copy.bookId.toString())
        repo.updateCopyStatus(schoolId, copyId, "available")
        repo.updateCopyCondition(schoolId, copyId, "good")
        repo.updateBookAvailability(schoolId, copy.bookId, +1)
        repo.appendAuditLog(schoolId, actorId, actorName, "REPAIR_COPY", "copy", copyId)
        LibraryEventBus.publish(CopyRepaired(schoolId, copy.bookId, copyId, actorId, actorName))
        return RepairCopyResultDto(
            copyId = copyId.toString(),
            bookTitle = book.title,
            oldStatus = "repair",
            newStatus = "available",
        )
    }

    suspend fun listCopiesInRepair(schoolId: UUID): List<Pair<BookCopyDto, String>> {
        return repo.listCopiesByStatus(schoolId, "repair").map { copy ->
            val book = repo.findBookById(schoolId, copy.bookId)
            copy.toDto() to (book?.title ?: "")
        }
    }

    // ── Bulk Import ─────────────────────────────────────────────────────────

    suspend fun bulkImport(schoolId: UUID, rows: List<CreateBookRequest>, actorId: UUID, actorName: String): BulkImportResultDto {
        val errors = mutableListOf<String>()
        var successCount = 0
        rows.forEachIndexed { index, req ->
            try {
                createBook(schoolId, req, actorId, actorName)
                successCount++
            } catch (e: Exception) {
                errors.add("Row ${index + 1}: ${e.message}")
            }
        }
        val result = BulkImportResultDto(
            totalRows = rows.size,
            successCount = successCount,
            failureCount = errors.size,
            errors = errors,
        )
        LibraryEventBus.publish(BulkImportCompleted(schoolId, successCount, errors.size, actorId, actorName))

        runCatching {
            Notify.toUser(
                userId = actorId,
                category = "library",
                title = "📚 Bulk Import Complete",
                body = "$successCount books imported successfully" + if (errors.isNotEmpty()) ", ${errors.size} errors" else "",
                schoolId = schoolId,
                deepLink = "/admin/library/books",
                refType = "library_bulk_import",
            )
        }

        return result
    }

    // ── Export ──────────────────────────────────────────────────────────────

    suspend fun exportCatalog(schoolId: UUID): ExportResultDto {
        val (_, total) = repo.searchBooks(schoolId, "", null, null, null, "newest", "all", 1, 1)
        return ExportResultDto(
            downloadUrl = "/api/v1/school/library/export/download?type=catalog",
            format = "csv",
            rowCount = total,
        )
    }

    suspend fun exportIssues(schoolId: UUID): ExportResultDto {
        val (issues, total) = repo.listIssues(schoolId, null, 1, 1)
        return ExportResultDto(
            downloadUrl = "/api/v1/school/library/export/download?type=issues",
            format = "csv",
            rowCount = total,
        )
    }

    suspend fun exportFines(schoolId: UUID): ExportResultDto {
        val issues = repo.listIssuesWithFines(schoolId)
        return ExportResultDto(
            downloadUrl = "/api/v1/school/library/export/download?type=fines",
            format = "csv",
            rowCount = issues.size,
        )
    }

    suspend fun exportAudit(schoolId: UUID): ExportResultDto {
        val logs = repo.listAuditLog(schoolId, 1, 1)
        return ExportResultDto(
            downloadUrl = "/api/v1/school/library/export/download?type=audit",
            format = "csv",
            rowCount = logs.second,
        )
    }

    suspend fun generateExportCsv(schoolId: UUID, type: String): String {
        val sb = StringBuilder()
        when (type) {
            "catalog" -> {
                sb.appendLine("id,title,author,isbn,category,language,total_copies,available_copies,is_archived")
                val (rows, _) = repo.searchBooks(schoolId, "", null, null, null, "newest", "all", 1, 10000)
                for (r in rows) {
                    sb.appendLine("${r.id},${escapeCsv(r.title)},${escapeCsv(r.author ?: "")},${escapeCsv(r.isbn ?: "")},${escapeCsv(r.category ?: "")},${r.language},${r.totalCopies},${r.availableCopies},${r.isArchived}")
                }
            }
            "issues" -> {
                sb.appendLine("id,book_id,borrower_name,borrower_type,issue_date,due_date,return_date,status,fine_amount,fine_status")
                val (rows, _) = repo.listIssues(schoolId, null, 1, 10000)
                for (r in rows) {
                    sb.appendLine("${r.id},${r.bookId},${escapeCsv(r.borrowerName)},${r.borrowerType},${r.issueDate},${r.dueDate},${r.returnDate ?: ""},${r.status},${r.fineAmount},${r.fineStatus}")
                }
            }
            "fines" -> {
                sb.appendLine("issue_id,book_id,borrower_name,fine_amount,fine_status,due_date,return_date")
                val rows = repo.listIssuesWithFines(schoolId)
                for (r in rows) {
                    sb.appendLine("${r.id},${r.bookId},${escapeCsv(r.borrowerName)},${r.fineAmount},${r.fineStatus},${r.dueDate},${r.returnDate ?: ""}")
                }
            }
            "audit" -> {
                sb.appendLine("id,actor_id,actor_name,action,entity_type,entity_id,created_at")
                val (rows, _) = repo.listAuditLog(schoolId, 1, 10000)
                for (r in rows) {
                    sb.appendLine("${r.id},${r.actorId},${escapeCsv(r.actorName)},${r.action},${r.entityType},${r.entityId},${r.createdAt}")
                }
            }
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    // ── Onboarding ──────────────────────────────────────────────────────────

    suspend fun runOnboarding(schoolId: UUID, actorId: UUID, actorName: String): OnboardingResultDto {
        val existingCategories = repo.listCategories(schoolId)
        val seededCount = if (existingCategories.isEmpty()) {
            val defaults = listOf(
                Triple("Fiction", "#E91E63", "menu_book"),
                Triple("Science", "#2196F3", "science"),
                Triple("History", "#FF9800", "history_edu"),
                Triple("Biography", "#4CAF50", "person"),
                Triple("Reference", "#9C27B0", "library_books"),
            )
            defaults.forEach { (name, color, icon) ->
                repo.createCategory(schoolId, name, color, icon)
            }
            defaults.size
        } else 0

        val settings = repo.getSettings(schoolId)
        if (settings == null) {
            repo.upsertSettings(schoolId, mapOf(
                "defaultLoanDays" to 14,
                "finePerDay" to 1.0,
                "maxBooksPerStudent" to 3,
                "maxRenewals" to 2,
            ))
        }

        repo.appendAuditLog(schoolId, actorId, actorName, "ONBOARDING", "library", schoolId,
            metadata = mapOf("categoriesSeeded" to seededCount))

        return OnboardingResultDto(
            categoriesSeeded = seededCount,
            settingsConfigured = settings == null,
            ready = true,
        )
    }

    // ── Row → DTO mappers ───────────────────────────────────────────────────

    private fun LibraryBookRow.toDto(): LibraryBookDto = LibraryBookDto(
        id = id.toString(),
        isbn = isbn,
        title = title,
        author = author,
        publisher = publisher,
        category = category,
        tags = tags?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList()) } ?: emptyList(),
        totalCopies = totalCopies,
        availableCopies = availableCopies,
        shelfLocation = shelfLocation,
        coverUrl = coverUrl,
        replacementCost = replacementCost,
        seriesName = seriesName,
        seriesNumber = seriesNumber,
        language = language,
        isArchived = isArchived,
        synopsis = synopsis,
        pageCount = pageCount,
    )

    private fun LibraryCopyRow.toDto(): BookCopyDto = BookCopyDto(
        id = id.toString(),
        bookId = bookId.toString(),
        copyNumber = copyNumber,
        barcode = barcode,
        condition = condition,
        status = status,
    )

    private fun LibraryIssueRow.toDto(bookTitle: String): LibraryIssueDto = LibraryIssueDto(
        id = id.toString(),
        bookId = bookId.toString(),
        bookTitle = bookTitle,
        copyId = copyId?.toString(),
        copyNumber = null,
        borrowerId = borrowerId.toString(),
        borrowerType = borrowerType,
        borrowerName = borrowerName,
        issueDate = issueDate.toString(),
        dueDate = dueDate.toString(),
        returnDate = returnDate?.toString(),
        returnCondition = returnCondition,
        damageNotes = damageNotes,
        renewalCount = renewalCount,
        fineAmount = fineAmount,
        fineStatus = fineStatus,
        finePaidAt = finePaidAt?.toString(),
        fineWaivedReason = fineWaivedReason,
        status = status,
    )

    private fun LibraryCategoryRow.toDto(): LibraryCategoryDto = LibraryCategoryDto(
        id = id.toString(),
        name = name,
        color = color,
        icon = icon,
        displayOrder = displayOrder,
    )

    private fun LibraryAuditLogRow.toDto(): LibraryAuditLogDto = LibraryAuditLogDto(
        id = id.toString(),
        actorId = actorId?.toString(),
        actorName = actorName,
        action = action,
        entityType = entityType,
        entityId = entityId?.toString(),
        metadata = metadata,
        previousState = previousState,
        newState = newState,
        hash = hash,
        createdAt = createdAt.toString(),
    )

    private fun LibraryAnnouncementRow.toDto(): LibraryAnnouncementDto = LibraryAnnouncementDto(
        id = id.toString(),
        title = title,
        message = message,
        audience = audience,
        createdByName = createdByName,
        expiresAt = expiresAt?.toString(),
        isActive = isActive,
        createdAt = createdAt.toString(),
    )

    private fun LibraryAcquisitionRow.toDto(): LibraryAcquisitionRequestDto = LibraryAcquisitionRequestDto(
        id = id.toString(),
        requestedByName = requestedByName,
        requestedByType = requestedByType,
        title = title,
        author = author,
        isbn = isbn,
        publisher = publisher,
        reason = reason,
        estimatedCost = estimatedCost,
        status = status,
        orderLink = orderLink,
        convertedBookId = convertedBookId?.toString(),
        createdAt = createdAt.toString(),
    )

    private fun LibraryDiscussionRow.toDto(): LibraryDiscussionMessageDto = LibraryDiscussionMessageDto(
        id = id.toString(),
        studentName = studentName,
        message = message,
        createdAt = createdAt.toString(),
        isDeleted = deletedAt != null,
    )

    companion object {
        val ALL_BADGES = listOf(
            Triple("first_book", "First Book", "menu_book"),
            Triple("5_books", "Bookworm", "auto_stories"),
            Triple("10_books", "Avid Reader", "library_books"),
            Triple("25_books", "Page Turner", "bookmark"),
            Triple("50_books", "Book Champion", "emoji_events"),
            Triple("100_books", "Century Reader", "military_tech"),
            Triple("speed_reader", "Speed Reader", "rocket_launch"),
            Triple("genre_explorer", "Genre Explorer", "explore"),
            Triple("streak_7", "7-Day Streak", "local_fire_department"),
            Triple("streak_30", "30-Day Streak", "whatshot"),
        )

        data class NotificationChannelConfig(
            val push: Boolean,
            val inApp: Boolean,
            val email: Boolean,
            val sms: Boolean,
        )

        val NOTIFICATION_CHANNEL_MATRIX: Map<String, NotificationChannelConfig> = mapOf(
            "book_issued"           to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "book_returned"         to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "book_renewed"          to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "overdue_reminder"      to NotificationChannelConfig(push = true,  inApp = true,  email = true,  sms = false),
            "due_date_reminder"     to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "reservation_available" to NotificationChannelConfig(push = true,  inApp = true,  email = true,  sms = false),
            "fine_paid"             to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "fine_waived"           to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "book_lost"             to NotificationChannelConfig(push = true,  inApp = true,  email = true,  sms = false),
            "book_damaged"          to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "announcement_posted"   to NotificationChannelConfig(push = false, inApp = true,  email = false, sms = false),
            "acquisition_approved"  to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "badge_earned"          to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "reading_goal_reached"  to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "bulk_import_completed" to NotificationChannelConfig(push = false, inApp = true,  email = false, sms = false),
            "wishlist_available"    to NotificationChannelConfig(push = true,  inApp = true,  email = false, sms = false),
            "cover_upload_failed"   to NotificationChannelConfig(push = false, inApp = true,  email = false, sms = false),
            "book_archived"         to NotificationChannelConfig(push = false, inApp = true,  email = false, sms = false),
        )
    }

    // ── GDPR: Right to be forgotten ──────────────────────────────────────────
    // Spec §16: Anonymize library PII when a student is deactivated.
    // Reading history is retained for school records but PII (names) is removed.
    suspend fun anonymizeBorrower(borrowerId: UUID): Int {
        return repo.anonymizeBorrower(borrowerId)
    }

    // ── GDPR: Right to access (spec §16) ─────────────────────────────────────
    // Returns all library data associated with a borrower as a structured export.
    suspend fun exportBorrowerData(schoolId: UUID, borrowerId: UUID): LibraryPrivacyService.GdprDataExport {
        val issues = repo.listIssuesByBorrower(schoolId, borrowerId)
        val reservations = repo.listReservationsByBorrower(schoolId, borrowerId)
        val wishlist = repo.listWishlistByBorrower(schoolId, borrowerId)
        val readingGoals = repo.listReadingGoalsByBorrower(schoolId, borrowerId)
        val badges = repo.listBadgesByBorrower(schoolId, borrowerId)
        val discussions = repo.listDiscussionsByBorrower(schoolId, borrowerId)

        return LibraryPrivacyService.GdprDataExport(
            borrowerId = borrowerId.toString(),
            exportedAt = java.time.Instant.now().toString(),
            issues = issues.map { row ->
                val book = repo.findBookById(schoolId, row.bookId)
                LibraryPrivacyService.IssueExport(
                    bookTitle = book?.title ?: "Unknown",
                    issueDate = row.issueDate.toString(),
                    dueDate = row.dueDate.toString(),
                    returnDate = row.returnDate?.toString(),
                    status = row.status,
                    fineAmount = row.fineAmount,
                    fineStatus = row.fineStatus,
                )
            },
            reservations = reservations.map { row ->
                val book = repo.findBookById(schoolId, row.bookId)
                LibraryPrivacyService.ReservationExport(
                    bookTitle = book?.title ?: "Unknown",
                    status = row.status,
                    createdAt = row.createdAt.toString(),
                )
            },
            wishlist = wishlist.map { row ->
                val book = repo.findBookById(schoolId, row.bookId)
                LibraryPrivacyService.WishlistExport(
                    bookTitle = book?.title ?: "Unknown",
                    addedAt = row.createdAt.toString(),
                )
            },
            readingGoals = readingGoals.map { row ->
                LibraryPrivacyService.ReadingGoalExport(
                    goalCount = row.goalCount,
                    period = row.period,
                    targetYear = row.targetYear,
                )
            },
            badges = badges.map { row ->
                LibraryPrivacyService.BadgeExport(
                    badgeType = row.badgeType,
                    awardedAt = row.earnedAt.toString(),
                )
            },
            discussions = discussions.map { row ->
                val book = repo.findBookById(schoolId, row.bookId)
                LibraryPrivacyService.DiscussionExport(
                    bookTitle = book?.title ?: "Unknown",
                    message = row.message,
                    createdAt = row.createdAt.toString(),
                )
            },
            piiFieldsRedacted = LibraryPrivacyService.PII_REGISTRY
                .filter { it.classification == LibraryPrivacyService.PiiClassification.DIRECT_PII }
                .map { "${it.table}.${it.column}" },
        )
    }
}

private fun sanitizeHtml(input: String): String {
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
