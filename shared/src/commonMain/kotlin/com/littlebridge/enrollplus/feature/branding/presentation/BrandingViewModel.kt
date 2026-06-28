package com.littlebridge.enrollplus.feature.branding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.branding.domain.model.*
import com.littlebridge.enrollplus.feature.branding.domain.repository.BrandingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrandingState(
    val isLoading: Boolean = false,
    val branding: SchoolBranding? = null,
    val subdomainAvailable: Boolean? = null,
    val error: String? = null,
    val infoMessage: String? = null,
)

class BrandingViewModel(
    private val repository: BrandingRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrandingState())
    val state: StateFlow<BrandingState> = _state.asStateFlow()

    private suspend fun token(): String = prefs.getUserToken().first() ?: ""

    fun loadBranding() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getBranding(token())) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, branding = result.data.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun updateBranding(request: UpdateBrandingRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.updateBranding(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(isLoading = false, branding = result.data.data, infoMessage = "Branding updated")
                    }
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun resetBranding() {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.resetBranding(token())) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(isLoading = false, branding = result.data.data, infoMessage = "Branding reset to defaults")
                    }
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun checkSubdomain(subdomain: String) {
        _state.update { it.copy(isLoading = true, error = null, subdomainAvailable = null) }
        viewModelScope.launch {
            when (val result = repository.checkSubdomain(token(), subdomain)) {
                is NetworkResult.Success -> {
                    val available = result.data.data?.available ?: false
                    _state.update { it.copy(isLoading = false, subdomainAvailable = available) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun updateSubdomain(subdomain: String) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.updateSubdomain(token(), SubdomainRequest(subdomain))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Subdomain assigned") }
                    reloadBranding(preserveMessage = true)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun removeSubdomain() {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.removeSubdomain(token())) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Subdomain removed") }
                    reloadBranding(preserveMessage = true)
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun clearInfoMessage() {
        _state.update { it.copy(infoMessage = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSubdomainCheck() {
        _state.update { it.copy(subdomainAvailable = null) }
    }

    private fun reloadBranding(preserveMessage: Boolean = false) {
        val msg = if (preserveMessage) _state.value.infoMessage else null
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getBranding(token())) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, branding = result.data.data, infoMessage = msg)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }
}
