package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolDayConfigRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDaySlotDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolDayConfigRequest
import com.littlebridge.enrollplus.feature.admin.domain.repository.SchoolDayConfigRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SchoolDayConfigState(
    val configs: List<SchoolDayConfigDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class SchoolDayConfigViewModel(
    private val repository: SchoolDayConfigRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SchoolDayConfigState())
    val state: StateFlow<SchoolDayConfigState> = _state.asStateFlow()

    init {
        loadConfigs()
    }

    fun loadConfigs() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.list(token)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        configs = result.data.data?.configs ?: emptyList(),
                        isLoading = false,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("SchoolDayConfigVM", "list error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun createConfig(
        name: String,
        applicableDays: String,
        classLevel: String,
        slots: List<SchoolDaySlotDto>,
        onCreated: (() -> Unit)? = null,
    ) {
        if (name.isBlank() || applicableDays.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Name and applicable days are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            val request = CreateSchoolDayConfigRequest(
                name = name.trim(),
                applicableDays = applicableDays.trim(),
                classLevel = classLevel.trim(),
                slots = slots,
            )
            when (val r = repository.create(token, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Config created")
                    onCreated?.invoke()
                    loadConfigs()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun updateConfig(
        id: String,
        name: String,
        applicableDays: String,
        classLevel: String,
        slots: List<SchoolDaySlotDto>,
        isActive: Boolean = true,
        onUpdated: (() -> Unit)? = null,
    ) {
        if (name.isBlank() || applicableDays.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Name and applicable days are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            val request = UpdateSchoolDayConfigRequest(
                name = name.trim(),
                applicableDays = applicableDays.trim(),
                classLevel = classLevel.trim(),
                slots = slots,
                isActive = isActive,
            )
            when (val r = repository.update(token, id, request)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Config updated")
                    onUpdated?.invoke()
                    loadConfigs()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun deactivateConfig(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.deactivate(token, id)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Config deactivated")
                    loadConfigs()
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }
}
