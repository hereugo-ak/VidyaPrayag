package com.littlebridge.vidyaprayag.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ParentDashboardViewModel(
    private val getSchoolsUseCase: GetSchoolsUseCase
) : ViewModel() {

    private val _schools = MutableStateFlow<UiState<List<School>>>(UiState.Loading)
    val schools: StateFlow<UiState<List<School>>> = _schools.asStateFlow()

    private val _shortlist = MutableStateFlow<Set<String>>(emptySet())
    val shortlist: StateFlow<Set<String>> = _shortlist.asStateFlow()

    private val _hasChildProfile = MutableStateFlow(false)
    val hasChildProfile: StateFlow<Boolean> = _hasChildProfile.asStateFlow()

    init {
        loadSchools()
    }

    private fun loadSchools() {
        viewModelScope.launch {
            _schools.value = UiState.Loading
            try {
                getSchoolsUseCase().collect { schoolList ->
                    _schools.value = UiState.Success(schoolList)
                }
            } catch (e: Exception) {
                _schools.value = UiState.Error(e.message ?: "Failed to load schools")
            }
        }
    }

    fun toggleShortlist(schoolId: String) {
        val current = _shortlist.value
        if (current.contains(schoolId)) {
            _shortlist.value = current - schoolId
        } else {
            if (current.size < 3) {
                _shortlist.value = current + schoolId
            }
        }
    }
}
