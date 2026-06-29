package com.littlebridge.enrollplus.feature.idcard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.idcard.domain.model.*
import com.littlebridge.enrollplus.feature.idcard.domain.repository.IdCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IdCardState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val templates: List<IdCardTemplateDto> = emptyList(),
    val cards: List<IdCardDto> = emptyList(),
    val currentCard: IdCardDto? = null,
    val generateResult: GenerateIdCardResponse? = null,
    val pdfUrl: String? = null,
    val error: String? = null,
    val infoMessage: String? = null,
)

class IdCardViewModel(
    private val repository: IdCardRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IdCardState())
    val state: StateFlow<IdCardState> = _state.asStateFlow()

    private suspend fun token(): String = prefs.getUserToken().first() ?: ""

    // ── Admin: Templates ────────────────────────────────────────────────

    fun loadTemplates() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getTemplates(token())) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, templates = result.data.data ?: emptyList())
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

    fun createTemplate(name: String, roleType: String, frontConfig: String, backConfig: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.createTemplate(token(), CreateTemplateRequest(name, roleType, frontConfig, backConfig))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Template created") }
                    loadTemplates()
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

    fun deactivateTemplate(templateId: String) {
        viewModelScope.launch {
            when (val result = repository.deactivateTemplate(token(), templateId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(infoMessage = "Template deactivated") }
                    loadTemplates()
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Admin: Generate Cards ───────────────────────────────────────────

    fun generateCards(templateId: String, scope: String, classId: String? = null) {
        _state.update { it.copy(isGenerating = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.generateCards(token(), GenerateIdCardRequest(templateId, scope, classId))) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            generateResult = data,
                            infoMessage = "Generated ${data?.count ?: 0} ID cards",
                        )
                    }
                    loadCards()
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isGenerating = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> _state.update {
                    it.copy(isGenerating = false, error = "Connection error")
                }
            }
        }
    }

    fun loadCards() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getCards(token())) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, cards = result.data.data ?: emptyList())
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

    // ── Parent: Digital ID Card ─────────────────────────────────────────

    fun loadChildIdCard(childId: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getChildIdCard(token(), childId)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, currentCard = result.data.data)
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

    // ── Teacher: Digital ID Card ────────────────────────────────────────

    fun loadTeacherIdCard() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getTeacherIdCard(token())) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, currentCard = result.data.data)
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

    // ── Staff: Digital ID Card ──────────────────────────────────────────

    fun loadStaffIdCard() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getStaffIdCard(token())) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, currentCard = result.data.data)
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

    // ── Admin: PDF Download ─────────────────────────────────────────────

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteCard(token(), cardId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(infoMessage = "Card deleted") }
                    loadCards()
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun loadPdfUrl(cardId: String) {
        viewModelScope.launch {
            when (val result = repository.getPdfUrl(token(), cardId)) {
                is NetworkResult.Success -> {
                    val url = result.data.data?.get("pdfUrl")
                    _state.update { it.copy(pdfUrl = url, error = if (url == null) "PDF not available" else null) }
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, infoMessage = null, pdfUrl = null) }
    }
}
