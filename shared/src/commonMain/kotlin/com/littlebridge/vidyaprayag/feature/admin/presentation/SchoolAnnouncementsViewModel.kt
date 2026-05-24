package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnnouncementDto
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
    val announcements: List<Announcement> = emptyList(),
    val isWhatsAppSyncEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
                    val dtos = result.data.data?.announcements ?: emptyList()
                    _state.value = _state.value.copy(
                        announcements = dtos.map { it.toUiModel() },
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
                    val dtos = result.data.data?.announcements ?: emptyList()
                    _state.value = _state.value.copy(
                        announcements = dtos.map { it.toUiModel() },
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
