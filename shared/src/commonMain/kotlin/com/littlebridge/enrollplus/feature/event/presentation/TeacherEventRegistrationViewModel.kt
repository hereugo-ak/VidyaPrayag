/*
 * File: TeacherEventRegistrationViewModel.kt
 * Module: feature.event.presentation
 *
 * ViewModel for the Teacher PTM Event Registration screen.
 * Manages PTM event list, detail with slot-wise bookings, and parent check-in.
 */
package com.littlebridge.enrollplus.feature.event.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.event.domain.model.*
import com.littlebridge.enrollplus.feature.event.domain.repository.EventRegistrationRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherEventRegistrationState(
    val events: List<TeacherPtmEventDto> = emptyList(),
    val eventDetail: TeacherPtmEventDto? = null,
    val slots: List<TeacherSlotDto> = emptyList(),
    val isLoading: Boolean = false,
    val isCheckingIn: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class TeacherEventRegistrationViewModel(
    private val repository: EventRegistrationRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherEventRegistrationState())
    val state: StateFlow<TeacherEventRegistrationState> = _state.asStateFlow()

    init { loadPtmEvents() }

    fun loadPtmEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.getTeacherPtmEvents(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        events = result.data.data?.events ?: emptyList(),
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherEventVM", "loadPtmEvents error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun loadPtmDetail(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.getTeacherPtmDetail(token, eventId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    _state.value = _state.value.copy(
                        eventDetail = detail,
                        slots = detail?.slots ?: emptyList(),
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun checkinParent(eventId: String, registrationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingIn = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCheckingIn = false)
                return@launch
            }
            when (val result = repository.checkinParent(token, eventId, registrationId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isCheckingIn = false,
                        infoMessage = "Parent checked in",
                    )
                    loadPtmDetail(eventId)
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isCheckingIn = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCheckingIn = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }
}
