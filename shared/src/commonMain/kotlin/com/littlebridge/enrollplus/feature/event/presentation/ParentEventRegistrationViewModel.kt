/*
 * File: ParentEventRegistrationViewModel.kt
 * Module: feature.event.presentation
 *
 * ViewModel for the Parent Event Registration screen.
 * Manages event list, event detail, registration, cancellation, reschedule, and my registrations.
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

data class ParentEventRegistrationState(
    val events: List<ParentEventDto> = emptyList(),
    val eventDetail: ParentEventDetailResponse? = null,
    val myRegistrations: List<RegistrationDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRegistering: Boolean = false,
    val isCancelling: Boolean = false,
    val isRescheduling: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val registrationSuccess: Boolean = false,
)

class ParentEventRegistrationViewModel(
    private val repository: EventRegistrationRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ParentEventRegistrationState())
    val state: StateFlow<ParentEventRegistrationState> = _state.asStateFlow()

    init { loadEvents() }

    fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.listParentEvents(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        events = result.data.data?.events ?: emptyList(),
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ParentEventVM", "loadEvents error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun loadEventDetail(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.getParentEventDetail(token, eventId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        eventDetail = result.data.data,
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

    fun register(eventId: String, slotId: String?, studentId: String?, attendeeCount: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRegistering = true, errorMessage = null, registrationSuccess = false)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isRegistering = false)
                return@launch
            }
            val request = RegisterRequest(slotId = slotId, studentId = studentId, attendeeCount = attendeeCount)
            val clientRequestId = "reg-${System.currentTimeMillis()}-${eventId}"
            when (val result = repository.register(token, eventId, request, clientRequestId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isRegistering = false,
                        registrationSuccess = true,
                        infoMessage = "Registered successfully!",
                    )
                    loadEventDetail(eventId)
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isRegistering = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isRegistering = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun cancelRegistration(eventId: String, studentId: String? = null, reason: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCancelling = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCancelling = false)
                return@launch
            }
            when (val result = repository.cancelRegistration(token, eventId, CancelRegistrationRequest(studentId, reason))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isCancelling = false,
                        infoMessage = "Registration cancelled",
                    )
                    loadEventDetail(eventId)
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isCancelling = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCancelling = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun reschedule(eventId: String, newSlotId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRescheduling = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isRescheduling = false)
                return@launch
            }
            when (val result = repository.reschedule(token, eventId, RescheduleRequest(newSlotId))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isRescheduling = false,
                        infoMessage = "Slot changed successfully",
                    )
                    loadEventDetail(eventId)
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isRescheduling = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isRescheduling = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun loadMyRegistrations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.listMyRegistrations(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        myRegistrations = result.data.data?.registrations ?: emptyList(),
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

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null, registrationSuccess = false)
    }
}
