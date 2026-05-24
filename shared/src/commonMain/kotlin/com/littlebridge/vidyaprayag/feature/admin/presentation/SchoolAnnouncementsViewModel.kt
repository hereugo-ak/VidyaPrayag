package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnnouncementDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateAnnouncementRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.AnnouncementsRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class Announcement(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "Holidays", "PTM", "Events", "Update", "Reminder"
    val date: String,
    val imageUrl: String? = null,
    val isFeatured: Boolean = false,
    val participants: List<String> = emptyList()
)

data class SchoolAnnouncementsState(
    /** Filtered list shown in the UI (after category + search). */
    val announcements: List<Announcement> = emptyList(),
    /**
     * Unfiltered cache so we can re-derive [announcements] without re-hitting
     * the network when the user toggles a category chip.
     */
    val allAnnouncements: List<Announcement> = emptyList(),
    /** null = "All" (show everything). */
    val selectedCategory: String? = null,
    val isWhatsAppSyncEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class SchoolAnnouncementsViewModel(
    private val announcementsRepository: AnnouncementsRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SchoolAnnouncementsState())
    val state: StateFlow<SchoolAnnouncementsState> = _state.asStateFlow()

    init {
        loadAnnouncements()
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                AppLogger.d("SchoolAnnouncementsVM", "No auth token; skipping load")
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            when (val result = announcementsRepository.getAnnouncements(token)) {
                is NetworkResult.Success -> {
                    val all = result.data.data?.announcements.orEmpty().map { it.toUiModel() }
                    _state.value = _state.value.copy(
                        allAnnouncements = all,
                        announcements = applyCategoryFilter(all, _state.value.selectedCategory),
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("SchoolAnnouncementsVM", "getAnnouncements error: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("SchoolAnnouncementsVM", "getAnnouncements connection error")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Connection error. Check your internet."
                    )
                }
            }
        }
    }

    fun searchAnnouncements(query: String) {
        if (query.isBlank()) {
            loadAnnouncements()
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            when (val result = announcementsRepository.searchAnnouncements(token, query)) {
                is NetworkResult.Success -> {
                    val all = result.data.data?.announcements.orEmpty().map { it.toUiModel() }
                    _state.value = _state.value.copy(
                        allAnnouncements = all,
                        announcements = applyCategoryFilter(all, _state.value.selectedCategory),
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error.")
                }
            }
        }
    }

    /**
     * Switch the active category filter. Pass null (or "All") to clear the
     * filter. Operates on the cached [SchoolAnnouncementsState.allAnnouncements]
     * to avoid an extra network round-trip.
     */
    fun setCategoryFilter(category: String?) {
        val normalized = category?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        val filtered = applyCategoryFilter(_state.value.allAnnouncements, normalized)
        _state.value = _state.value.copy(
            selectedCategory = normalized,
            announcements = filtered
        )
    }

    private fun applyCategoryFilter(
        items: List<Announcement>,
        category: String?
    ): List<Announcement> {
        if (category.isNullOrBlank()) return items
        return items.filter { it.category.equals(category, ignoreCase = true) }
    }

    /**
     * Create a new announcement and refresh the list on success. The screen
     * passes [onCreated] to dismiss the dialog only after the server
     * round-trip succeeds — preventing a stale local copy from sticking
     * around if the request fails.
     */
    fun createAnnouncement(
        type: String,
        title: String,
        description: String,
        date: String,
        subTitle: String? = null,
        eventImage: String? = null,
        onCreated: (() -> Unit)? = null
    ) {
        if (title.isBlank() || description.isBlank() || date.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Title, description and date are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCreating = false, errorMessage = "Not signed in")
                return@launch
            }
            val body = CreateAnnouncementRequest(
                type = type.ifBlank { "Update" },
                title = title.trim(),
                subTitle = subTitle?.trim()?.takeIf { it.isNotBlank() },
                description = description.trim(),
                eventImage = eventImage?.trim()?.takeIf { it.isNotBlank() },
                date = date.trim()
            )
            when (val r = announcementsRepository.createAnnouncement(token, body)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        infoMessage = "Announcement created"
                    )
                    onCreated?.invoke()
                    loadAnnouncements()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("SchoolAnnouncementsVM", "createAnnouncement error: ${r.message}")
                    _state.value = _state.value.copy(isCreating = false, errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        errorMessage = "Connection error. Check your internet."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }

    fun toggleWhatsAppSync(enabled: Boolean) {
        _state.value = _state.value.copy(isWhatsAppSyncEnabled = enabled)
        if (enabled) {
            viewModelScope.launch {
                val token = preferenceRepository.getUserToken().first() ?: return@launch
                announcementsRepository.syncWhatsApp(token)
            }
        }
    }

    private fun AnnouncementDto.toUiModel() = Announcement(
        id = eventId,
        title = title,
        description = description,
        category = type,
        date = date,
        imageUrl = eventImage,
        isFeatured = false
    )
}
