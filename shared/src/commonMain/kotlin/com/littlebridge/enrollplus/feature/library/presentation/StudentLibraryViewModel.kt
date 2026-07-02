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

data class StudentLibraryState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profile: StudentLibraryProfileDto? = null,
    val books: List<LibraryBookDto> = emptyList(),
    val booksTotal: Int = 0,
    val booksPage: Int = 1,
    val searchQuery: String = "",
    val badges: List<LibraryBadgeDto> = emptyList(),
    val readingGoal: LibraryReadingGoalDto? = null,
    val wishlist: List<LibraryWishlistDto> = emptyList(),
    val issuedBooks: List<LibraryIssueDto> = emptyList(),
    val history: List<LibraryIssueDto> = emptyList(),
    val reservations: List<LibraryReservationDto> = emptyList(),
    val discussions: List<LibraryDiscussionMessageDto> = emptyList(),
    val isActionLoading: Boolean = false,
    val actionMessage: String? = null,
    val isOffline: Boolean = false,
    val isStaleData: Boolean = false,
    val recommendations: List<RecommendationDto> = emptyList(),
    val trending: List<TrendingBookDto> = emptyList(),
    val acquisitionRequests: List<LibraryAcquisitionRequestDto> = emptyList(),
    val announcements: List<LibraryAnnouncementDto> = emptyList(),
)

class StudentLibraryViewModel(
    private val repository: LibraryRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StudentLibraryState())
    val state: StateFlow<StudentLibraryState> = _state.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            when (val result = repository.studentGetProfile(token)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, profile = result.data.data, isOffline = false, isStaleData = false) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.profile != null
                    _state.update { it.copy(isLoading = false, isOffline = true, isStaleData = hasCache, error = if (hasCache) null else "Connection error") }
                }
            }
        }
    }

    private var searchJob: Job? = null

    fun searchBooks(page: Int = 1) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _state.update { it.copy(isLoading = true, error = null, booksPage = page) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            val query = _state.value.searchQuery
            when (val result = repository.studentSearchBooks(token, query, page = page, limit = 20)) {
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

    fun loadBadges() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetBadges(token)) {
                is NetworkResult.Success -> _state.update { it.copy(badges = result.data.data) }
                else -> {}
            }
        }
    }

    fun loadReadingGoal() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetReadingGoal(token)) {
                is NetworkResult.Success -> _state.update { it.copy(readingGoal = result.data.data) }
                else -> {}
            }
        }
    }

    fun setReadingGoal(goalCount: Int, period: String, targetYear: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentSetReadingGoal(token, CreateReadingGoalRequest(goalCount, period, targetYear))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Reading goal set", readingGoal = result.data.data) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun loadWishlist() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetWishlist(token)) {
                is NetworkResult.Success -> _state.update { it.copy(wishlist = result.data.data) }
                else -> {}
            }
        }
    }

    fun reserveBook(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentReserveBook(token, ReserveBookRequest(bookId))) {
                is NetworkResult.Success -> _state.update { it.copy(isActionLoading = false, actionMessage = "Book reserved!") }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    // ── Issued Books & History ───────────────────────────────────────────────

    fun loadIssuedBooks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetIssued(token)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, issuedBooks = result.data.data, isOffline = false, isStaleData = false) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.issuedBooks.isNotEmpty()
                    _state.update { it.copy(isLoading = false, isOffline = true, isStaleData = hasCache, error = if (hasCache) null else "Connection error") }
                }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetHistory(token)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, history = result.data.data, isOffline = false, isStaleData = false) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.history.isNotEmpty()
                    _state.update { it.copy(isLoading = false, isOffline = true, isStaleData = hasCache, error = if (hasCache) null else "Connection error") }
                }
            }
        }
    }

    fun renewBook(issueId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentRenewBook(token, issueId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Book renewed!") }
                    loadIssuedBooks()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Wishlist ────────────────────────────────────────────────────────────

    fun addToWishlist(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentAddToWishlist(token, bookId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Added to wishlist") }
                    loadWishlist()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentRemoveFromWishlist(token, bookId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Removed from wishlist") }
                    loadWishlist()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Discussions ─────────────────────────────────────────────────────────

    fun loadDiscussions(bookId: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetDiscussions(token, bookId)) {
                is NetworkResult.Success -> _state.update { it.copy(discussions = result.data.data) }
                else -> {}
            }
        }
    }

    fun postDiscussion(bookId: String, message: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentPostDiscussion(token, bookId, PostDiscussionRequest(message))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Message posted") }
                    loadDiscussions(bookId)
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Recommendations & Trending ───────────────────────────────────────────

    fun loadRecommendations(limit: Int = 10) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetRecommendations(token, limit)) {
                is NetworkResult.Success -> _state.update { it.copy(recommendations = result.data.data) }
                else -> {}
            }
        }
    }

    fun loadTrending(limit: Int = 10) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetTrending(token, limit)) {
                is NetworkResult.Success -> _state.update { it.copy(trending = result.data.data) }
                else -> {}
            }
        }
    }

    // ── Reservations ────────────────────────────────────────────────────────

    fun loadReservations() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentListReservations(token)) {
                is NetworkResult.Success -> _state.update { it.copy(reservations = result.data.data) }
                else -> {}
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentCancelReservation(token, reservationId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Reservation cancelled") }
                    loadReservations()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    // ── Acquisition Requests ────────────────────────────────────────────────

    fun loadAcquisitionRequests() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentListAcquisitionRequests(token)) {
                is NetworkResult.Success -> _state.update { it.copy(acquisitionRequests = result.data.data) }
                else -> {}
            }
        }
    }

    // ── Announcements ───────────────────────────────────────────────────────

    fun loadAnnouncements() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.studentGetAnnouncements(token)) {
                is NetworkResult.Success -> _state.update { it.copy(announcements = result.data.data) }
                else -> {}
            }
        }
    }
}
