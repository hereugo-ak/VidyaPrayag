package com.littlebridge.enrollplus.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.library.domain.model.*
import com.littlebridge.enrollplus.feature.library.domain.repository.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchoolLibraryState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val dashboard: LibraryDashboardDto? = null,
    val books: List<LibraryBookDto> = emptyList(),
    val booksTotal: Int = 0,
    val booksPage: Int = 1,
    val issues: List<LibraryIssueDto> = emptyList(),
    val issuesTotal: Int = 0,
    val issuesPage: Int = 1,
    val issuesStatusFilter: String? = null,
    val categories: List<LibraryCategoryDto> = emptyList(),
    val settings: LibrarySettingsDto? = null,
    val auditLog: List<LibraryAuditLogDto> = emptyList(),
    val announcements: List<LibraryAnnouncementDto> = emptyList(),
    val acquisitionRequests: List<LibraryAcquisitionRequestDto> = emptyList(),
    val reservations: List<LibraryReservationDto> = emptyList(),
    val searchQuery: String = "",
    val searchCategory: String? = null,
    val searchSortBy: String = "newest",
    val searchAvailability: String = "all",
    val isActionLoading: Boolean = false,
    val actionMessage: String? = null,
    val isOffline: Boolean = false,
    val isStaleData: Boolean = false,
    val trending: List<TrendingBookDto> = emptyList(),
    val repairCopies: List<RepairCopyResultDto> = emptyList(),
    val copies: List<BookCopyDto> = emptyList(),
    val bookHistory: List<LibraryIssueDto> = emptyList(),
    val selectedBookId: String? = null,
)

class SchoolLibraryViewModel(
    private val repository: LibraryRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SchoolLibraryState())
    val state: StateFlow<SchoolLibraryState> = _state.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            when (val result = repository.getDashboard(token)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.update { it.copy(isLoading = false, dashboard = data, isOffline = false, isStaleData = false) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.dashboard != null
                    _state.update { it.copy(isLoading = false, isOffline = true, isStaleData = hasCache, error = if (hasCache) null else "Connection error — showing cached data" ) }
                }
            }
        }
    }

    private var searchJob: Job? = null

    fun searchBooks(page: Int = 1) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce — wait for user to stop typing
            _state.update { it.copy(isLoading = true, error = null, booksPage = page) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            val s = _state.value
            when (val result = repository.searchBooks(token, s.searchQuery, s.searchCategory, null, null, s.searchSortBy, s.searchAvailability, page, 20)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, books = result.data.data, booksTotal = result.data.total, isOffline = false, isStaleData = false) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.books.isNotEmpty()
                    _state.update { it.copy(isLoading = false, isOffline = true, isStaleData = hasCache, error = if (hasCache) null else "Connection error") }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun updateSearchCategory(category: String?) {
        _state.update { it.copy(searchCategory = category) }
    }

    fun updateSearchSortBy(sortBy: String) {
        _state.update { it.copy(searchSortBy = sortBy) }
    }

    fun updateSearchAvailability(availability: String) {
        _state.update { it.copy(searchAvailability = availability) }
    }

    fun loadIssues(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, issuesPage = page) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            val s = _state.value
            when (val result = repository.listIssues(token, s.issuesStatusFilter, page, 20)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, issues = result.data.data, issuesTotal = result.data.total, isOffline = false, isStaleData = false) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.issues.isNotEmpty()
                    _state.update { it.copy(isLoading = false, isOffline = true, isStaleData = hasCache, error = if (hasCache) null else "Connection error") }
                }
            }
        }
    }

    fun updateIssuesStatusFilter(status: String?) {
        _state.update { it.copy(issuesStatusFilter = status) }
        loadIssues(1)
    }

    fun loadCategories() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listCategories(token)) {
                is NetworkResult.Success -> _state.update { it.copy(categories = result.data.data) }
                else -> {}
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.getSettings(token)) {
                is NetworkResult.Success -> _state.update { it.copy(settings = result.data.data) }
                else -> {}
            }
        }
    }

    fun returnBook(issueId: String, condition: String?, damageNotes: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.returnBook(token, ReturnBookRequest(issueId, condition, damageNotes))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book returned") }
                    loadIssues(_state.value.issuesPage)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun renewBook(issueId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.renewBook(token, issueId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book renewed") }
                    loadIssues(_state.value.issuesPage)
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun payFine(issueId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.payFine(token, issueId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Fine paid") }
                    loadIssues(_state.value.issuesPage)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    // ── Create / Edit Book ──────────────────────────────────────────────────

    fun createBook(req: CreateBookRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.createBook(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book created") }
                    searchBooks(1)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun issueBook(req: IssueBookRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.issueBook(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book issued") }
                    loadIssues(_state.value.issuesPage)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun markLost(issueId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.markLost(token, issueId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book marked as lost") }
                    loadIssues(_state.value.issuesPage)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun waiveFine(issueId: String, reason: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.waiveFine(token, issueId, WaiveFineRequest(reason))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Fine waived") }
                    loadIssues(_state.value.issuesPage)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Categories ──────────────────────────────────────────────────────────

    fun createCategory(req: CreateCategoryRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.createCategory(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Category created") }
                    loadCategories()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.deleteCategory(token, categoryId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Category deleted") }
                    loadCategories()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Settings ────────────────────────────────────────────────────────────

    fun updateSettings(req: UpdateSettingsRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.updateSettings(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Settings updated", settings = result.data.data) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Quick Issue & Bulk Return ───────────────────────────────────────────

    fun quickIssue(req: QuickIssueRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.quickIssue(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Quick issue successful") }
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun bulkReturn(barcodes: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.bulkReturn(token, BulkReturnRequest(barcodes))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Bulk return: ${result.data.data.size} books returned") }
                    loadIssues(_state.value.issuesPage)
                    loadDashboard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Audit Log ───────────────────────────────────────────────────────────

    fun loadAuditLog(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listAuditLog(token, page, 50)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, auditLog = result.data.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, isOffline = true) }
            }
        }
    }

    // ── Announcements ───────────────────────────────────────────────────────

    fun loadAnnouncements(activeOnly: Boolean = false) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listAnnouncements(token, activeOnly)) {
                is NetworkResult.Success -> _state.update { it.copy(announcements = result.data.data) }
                else -> {}
            }
        }
    }

    fun createAnnouncement(req: CreateAnnouncementRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.createAnnouncement(token, req)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Announcement created") }
                    loadAnnouncements(false)
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun toggleAnnouncement(announcementId: String, isActive: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.updateAnnouncement(token, announcementId, UpdateAnnouncementRequest(isActive = !isActive))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = if (!isActive) "Announcement activated" else "Announcement deactivated") }
                    loadAnnouncements(false)
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.deleteAnnouncement(token, announcementId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Announcement deleted") }
                    loadAnnouncements(false)
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Acquisition Requests ────────────────────────────────────────────────

    fun loadAcquisitionRequests(status: String? = null) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listAcquisitionRequests(token, status)) {
                is NetworkResult.Success -> _state.update { it.copy(acquisitionRequests = result.data.data) }
                else -> {}
            }
        }
    }

    fun updateAcquisitionStatus(requestId: String, action: String, orderLink: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.updateAcquisitionStatus(token, requestId, action, orderLink)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Acquisition request $action") }
                    loadAcquisitionRequests()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Reservations ────────────────────────────────────────────────────────

    fun loadReservationsForBook(bookId: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listReservationsForBook(token, bookId)) {
                is NetworkResult.Success -> _state.update { it.copy(reservations = result.data.data) }
                else -> {}
            }
        }
    }

    fun fulfillReservation(reservationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.fulfillReservation(token, reservationId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Reservation fulfilled") }
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Archive / Unarchive ──────────────────────────────────────────────────

    fun archiveBook(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.archiveBook(token, bookId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book archived") }
                    searchBooks()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun unarchiveBook(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.unarchiveBook(token, bookId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book unarchived") }
                    searchBooks()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Trending ─────────────────────────────────────────────────────────────

    fun loadTrending(limit: Int = 10) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listTrending(token, limit)) {
                is NetworkResult.Success -> _state.update { it.copy(trending = result.data.data) }
                else -> {}
            }
        }
    }

    // ── Repair ───────────────────────────────────────────────────────────────

    fun loadRepairCopies() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listCopiesInRepair(token)) {
                is NetworkResult.Success -> _state.update { it.copy(repairCopies = result.data.data) }
                else -> {}
            }
        }
    }

    fun repairCopy(copyId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.repairCopy(token, copyId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Copy repaired and marked available") }
                    loadRepairCopies()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Bulk Import / Export / Onboarding ────────────────────────────────────

    fun bulkImport(rows: List<CreateBookRequest>) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.bulkImport(token, BulkImportRequest(rows))) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Imported ${data?.successCount ?: 0}/${data?.totalRows ?: 0} books") }
                    searchBooks()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun exportCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.exportCatalog(token)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Catalog exported: ${result.data.data?.downloadUrl ?: ""}") }
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun runOnboarding() {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.runOnboarding(token)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Onboarding complete") }
                    loadCategories()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Cover Upload ─────────────────────────────────────────────────────────

    fun uploadCover(bookId: String, coverUrl: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.updateBookCover(token, bookId, coverUrl)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Cover updated") }
                    searchBooks()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Category Reorder ─────────────────────────────────────────────────────

    fun reorderCategories(orders: List<Pair<String, Int>>) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.reorderCategories(token, orders)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Categories reordered") }
                    loadCategories()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Convert Acquisition to Book ──────────────────────────────────────────

    fun convertAcquisitionToBook(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.convertAcquisitionToBook(token, requestId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Acquisition request converted to book") }
                    loadAcquisitionRequests()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Book Copies ──────────────────────────────────────────────────────────

    fun loadCopies(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedBookId = bookId) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listCopies(token, bookId)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, copies = result.data.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, isOffline = true) }
            }
        }
    }

    fun addCopy(bookId: String, condition: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.addCopy(token, bookId, condition)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Copy added") }
                    loadCopies(bookId)
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Book History ─────────────────────────────────────────────────────────

    fun loadBookHistory(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedBookId = bookId) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.listIssues(token, null, 1, 100)) {
                is NetworkResult.Success -> {
                    val filtered = result.data.data.filter { it.bookId == bookId }
                    _state.update { it.copy(isLoading = false, bookHistory = filtered) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, isOffline = true) }
            }
        }
    }

    fun clearSelectedBook() {
        _state.update { it.copy(selectedBookId = null, copies = emptyList(), bookHistory = emptyList()) }
    }
}
