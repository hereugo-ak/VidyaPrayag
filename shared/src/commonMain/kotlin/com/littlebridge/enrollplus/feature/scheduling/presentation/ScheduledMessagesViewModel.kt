package com.littlebridge.enrollplus.feature.scheduling.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.scheduling.domain.model.CreateScheduledMessageRequest
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageDto
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageListResponse
import com.littlebridge.enrollplus.feature.scheduling.domain.repository.ScheduledMessageRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

data class ScheduledMessagesState(
    val messages: List<ScheduledMessageDto> = emptyList(),
    val statusFilter: String? = null,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class ScheduledMessagesViewModel(
    private val repository: ScheduledMessageRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledMessagesState())
    val state: StateFlow<ScheduledMessagesState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.getScheduledMessages(token, _state.value.statusFilter)) {
                is NetworkResult.Success -> {
                    val list = result.data.data?.messages.orEmpty()
                    _state.value = _state.value.copy(
                        messages = list,
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ScheduledMessagesVM", "load error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _state.value = _state.value.copy(statusFilter = status)
        load()
    }

    fun createScheduledMessage(
        messageType: String,
        scheduledAt: String,
        payload: JsonElement,
        addToCalendar: Boolean = false,
        audienceType: String = "ALL_SCHOOL",
        audienceLabel: String? = null,
        title: String? = null,
        bodyPreview: String? = null,
        clientMsgId: String? = null,
        onCreated: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCreating = false, errorMessage = "Not signed in")
                return@launch
            }
            val request = CreateScheduledMessageRequest(
                messageType = messageType,
                scheduledAt = scheduledAt,
                payload = payload,
                addToCalendar = addToCalendar,
                audienceType = audienceType,
                audienceLabel = audienceLabel,
                title = title,
                bodyPreview = bodyPreview,
                clientMsgId = clientMsgId,
            )
            when (val result = repository.createScheduledMessage(token, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isCreating = false, infoMessage = "Scheduled message created")
                    onCreated?.invoke()
                    load()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ScheduledMessagesVM", "create error: ${result.message}")
                    _state.value = _state.value.copy(isCreating = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCreating = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun cancelScheduledMessage(id: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.cancelScheduledMessage(token, id)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(infoMessage = "Scheduled message cancelled")
                    load()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error.")
                }
            }
        }
    }

    fun dispatchNow(id: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.dispatchNow(token, id)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(infoMessage = "Dispatch triggered")
                    load()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error.")
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }
}
