package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PTMBooking(
    val id: String,
    val subject: String,
    val teacher: String,
    val date: String,
    val time: String,
    val iconName: String = "science"
)

data class PTMTimeSlot(
    val time: String,
    val isAvailable: Boolean = true,
    val isSelected: Boolean = false
)

data class ParentSchedulePTMState(
    val selectedMonth: String = "",
    val selectedDate: Int = 0,
    val ptmWindowDays: List<Int> = emptyList(),
    val teacherName: String = "",
    val teacherSubject: String = "",
    val teacherImageUrl: String = "",
    val slots: List<PTMTimeSlot> = emptyList(),
    val bookings: List<PTMBooking> = emptyList(),
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ParentSchedulePTMViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ParentSchedulePTMState())
    val state: StateFlow<ParentSchedulePTMState> = _state.asStateFlow()

    init {
        loadPtmScheduling()
    }

    private fun loadPtmScheduling() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            preferenceRepository.getUserToken().collect { token ->
                if (token != null) {
                    when (val result = repository.getPtmScheduling(token)) {
                        is NetworkResult.Success -> {
                            val data = result.data.data
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    selectedMonth = data.selectedMonth,
                                    selectedDate = data.selectedDate,
                                    ptmWindowDays = data.ptmWindowDays,
                                    teacherName = data.teacherName,
                                    teacherSubject = data.teacherSubject,
                                    teacherImageUrl = data.teacherImageUrl,
                                    slots = data.slots.map { s ->
                                        PTMTimeSlot(s.time, s.isAvailable, s.isSelected)
                                    },
                                    bookings = data.bookings.map { b ->
                                        PTMBooking(b.teacher, b.subject, b.teacher, b.date, b.time, b.iconName)
                                    }
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(isLoading = false, error = result.message) }
                        }
                        is NetworkResult.ConnectionError -> {
                            _state.update { it.copy(isLoading = false, error = "Connection error") }
                        }
                    }
                }
            }
        }
    }

    fun selectDate(date: Int) {
        _state.value = _state.value.copy(selectedDate = date)
    }

    fun selectSlot(time: String) {
        val updatedSlots = _state.value.slots.map {
            it.copy(isSelected = it.time == time)
        }
        _state.value = _state.value.copy(slots = updatedSlots)
    }
}
