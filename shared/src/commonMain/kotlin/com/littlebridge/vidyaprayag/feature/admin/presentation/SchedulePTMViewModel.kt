package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PtmActiveEventDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PtmClassProgressDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PtmHistoryDto
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.PtmRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PTMHistoryItem(
    val id: String,
    val date: String,
    val title: String,
    val turnout: Int,
    val totalMet: Int
)

data class ClassPTMProgress(
    val id: String,
    val className: String,
    val teacherName: String,
    val metCount: Int,
    val totalCount: Int,
    val progress: Float
)

data class SchedulePTMState(
    val activeEventTitle: String = "",
    val activeEventDate: String = "",
    val activeEventSlot: String = "",
    val expectedParents: Int = 0,
    val checkedInParents: Int = 0,
    val invitesDelivered: Int = 0,
    val readReceipts: Int = 0,
    val history: List<PTMHistoryItem> = emptyList(),
    val classProgress: List<ClassPTMProgress> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SchedulePTMViewModel(
    private val ptmRepository: PtmRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SchedulePTMState())
    val state: StateFlow<SchedulePTMState> = _state.asStateFlow()

    init {
        loadPtm()
    }

    fun loadPtm() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                AppLogger.d("SchedulePTMVM", "No auth token; skipping load")
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            when (val result = ptmRepository.getPtm(token)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    _state.value = _state.value.copy(
                        activeEventTitle = data?.activeEvent?.title ?: "",
                        activeEventDate = data?.activeEvent?.date ?: "",
                        activeEventSlot = data?.activeEvent?.slot ?: "",
                        expectedParents = data?.activeEvent?.expectedParents ?: 0,
                        checkedInParents = data?.activeEvent?.checkedInParents ?: 0,
                        invitesDelivered = data?.activeEvent?.invitesDelivered ?: 0,
                        readReceipts = data?.activeEvent?.readReceipts ?: 0,
                        history = data?.history?.map { it.toUiModel() } ?: emptyList(),
                        classProgress = data?.classProgress?.map { it.toUiModel() } ?: emptyList(),
                        isLoading = false
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("SchedulePTMVM", "getPtm error: ${result.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("SchedulePTMVM", "getPtm connection error")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Connection error. Check your internet."
                    )
                }
            }
        }
    }

    private fun PtmHistoryDto.toUiModel() = PTMHistoryItem(
        id = id,
        date = date,
        title = title,
        turnout = turnout,
        totalMet = totalMet
    )

    private fun PtmClassProgressDto.toUiModel() = ClassPTMProgress(
        id = id,
        className = className,
        teacherName = teacherName,
        metCount = metCount,
        totalCount = totalCount,
        progress = progress
    )
}
