/*
 * File: AdminEventRegistrationViewModel.kt
 * Module: feature.event.presentation
 *
 * ViewModel for the Admin Event Registration management screen.
 * Manages registration list, slot creation, auto-generation, config updates, and event cancellation.
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

data class AdminEventRegistrationState(
    val events: List<AdminEventDto> = emptyList(),
    val registrations: List<AdminRegistrationDto> = emptyList(),
    val slots: List<SlotResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isCancelling: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val csvData: String? = null,
)

class AdminEventRegistrationViewModel(
    private val repository: EventRegistrationRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminEventRegistrationState())
    val state: StateFlow<AdminEventRegistrationState> = _state.asStateFlow()

    init { loadEvents() }

    fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.listAdminEvents(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        events = result.data.data?.events ?: emptyList(),
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AdminEventVM", "loadEvents error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun loadRegistrations(status: String? = null, eventId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.listAllRegistrations(token, status, eventId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        registrations = result.data.data?.registrations ?: emptyList(),
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("AdminEventVM", "loadRegistrations error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun loadEventRegistrations(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.listEventRegistrations(token, eventId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        registrations = result.data.data?.registrations ?: emptyList(),
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

    fun loadSlots(eventId: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.listEventSlots(token, eventId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        slots = result.data.data ?: emptyList(),
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun createSlot(eventId: String, startTime: String, endTime: String, capacity: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCreating = false)
                return@launch
            }
            when (val result = repository.createSlot(token, eventId, CreateSlotRequest(startTime, endTime, capacity))) {
                is NetworkResult.Success -> {
                    val newSlot = result.data.data
                    _state.value = _state.value.copy(
                        isCreating = false,
                        infoMessage = "Slot created",
                        slots = if (newSlot != null) _state.value.slots + newSlot else _state.value.slots,
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isCreating = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCreating = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun autoGenerateSlots(eventId: String, request: AutoGenerateSlotsRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCreating = false)
                return@launch
            }
            when (val result = repository.autoGenerateSlots(token, eventId, request)) {
                is NetworkResult.Success -> {
                    val newSlots = result.data.data?.slots ?: emptyList()
                    _state.value = _state.value.copy(
                        isCreating = false,
                        infoMessage = "${newSlots.size} slots generated",
                        slots = _state.value.slots + newSlots,
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isCreating = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCreating = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun deleteSlot(eventId: String, slotId: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.deleteSlot(token, eventId, slotId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        slots = _state.value.slots.filterNot { it.id == slotId },
                        infoMessage = "Slot deleted",
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun updateRegistrationConfig(eventId: String, request: UpdateRegistrationConfigRequest) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.updateRegistrationConfig(token, eventId, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(infoMessage = "Registration settings updated")
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun cancelEvent(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCancelling = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCancelling = false)
                return@launch
            }
            when (val result = repository.cancelEvent(token, eventId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isCancelling = false,
                        infoMessage = "Event cancelled",
                    )
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

    fun exportCsv(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.exportRegistrationsCsv(token, eventId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        csvData = result.data,
                        isLoading = false,
                        infoMessage = "CSV exported",
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
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null, csvData = null)
    }
}
