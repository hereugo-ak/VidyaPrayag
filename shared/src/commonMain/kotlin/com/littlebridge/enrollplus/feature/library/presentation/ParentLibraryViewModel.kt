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

data class ParentLibraryState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val books: List<LibraryBookDto> = emptyList(),
    val booksTotal: Int = 0,
    val booksPage: Int = 1,
    val searchQuery: String = "",
    val searchCategory: String? = null,
    val searchSortBy: String = "newest",
    val issuedBooks: List<LibraryIssueDto> = emptyList(),
    val reservations: List<LibraryReservationDto> = emptyList(),
    val isActionLoading: Boolean = false,
    val actionMessage: String? = null,
    val isOffline: Boolean = false,
    val isStaleData: Boolean = false,
    val selectedChildId: String? = null,
)

class ParentLibraryViewModel(
    private val repository: LibraryRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentLibraryState())
    val state: StateFlow<ParentLibraryState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun updateSearchCategory(category: String?) {
        _state.update { it.copy(searchCategory = category) }
    }

    fun updateSearchSortBy(sortBy: String) {
        _state.update { it.copy(searchSortBy = sortBy) }
    }

    fun searchBooks(page: Int = 1) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(isLoading = true, error = null, booksPage = page) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            val s = _state.value
            when (val result = repository.parentSearchBooks(
                token, s.searchQuery, s.searchCategory, null, null, s.searchSortBy, page, 20
            )) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            books = result.data.data,
                            booksTotal = result.data.total,
                            isOffline = false,
                            isStaleData = false,
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.books.isNotEmpty()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isOffline = true,
                            isStaleData = hasCache,
                            error = if (hasCache) null else "Connection error",
                        )
                    }
                }
            }
        }
    }

    fun loadIssuedBooks(childId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedChildId = childId) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.parentGetIssuedForChild(token, childId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, issuedBooks = result.data.data) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> {
                    val hasCache = _state.value.issuedBooks.isNotEmpty()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isOffline = true,
                            isStaleData = hasCache,
                            error = if (hasCache) null else "Connection error",
                        )
                    }
                }
            }
        }
    }

    fun loadReservations() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.parentListReservations(token)) {
                is NetworkResult.Success -> _state.update { it.copy(reservations = result.data.data) }
                else -> {}
            }
        }
    }

    fun reserveBook(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.parentReserveBook(token, ReserveBookRequest(bookId))) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isActionLoading = false,
                            actionMessage = "Book reserved. You'll be notified when available.",
                        )
                    }
                    loadReservations()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionMessage = null) }
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = repository.parentCancelReservation(token, reservationId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isActionLoading = false, actionMessage = "Reservation cancelled") }
                    loadReservations()
                }
                is NetworkResult.Error -> _state.update { it.copy(isActionLoading = false, actionMessage = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isActionLoading = false, actionMessage = "Connection error") }
            }
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}
