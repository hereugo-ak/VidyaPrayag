package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CreateTestRequest
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherTest
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeacherMarksState(
    val tests: List<TeacherTest> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null
)

class TeacherMarksViewModel(
    private val repository: TeacherRepository,
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherMarksState())
    val state: StateFlow<TeacherMarksState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(className: String? = null, subject: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getTests(token, className, subject)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(tests = result.data.tests, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherMarksVM", "getTests failed: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
                }
            }
        }
    }

    fun createTest(
        title: String,
        subject: String,
        className: String,
        totalMarks: Int,
        testDate: String,
        onCreated: () -> Unit
    ) {
        if (title.isBlank() || subject.isBlank() || className.isBlank()) {
            _state.value = _state.value.copy(error = "Title, subject, and class are required")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isCreating = false, error = "Not signed in")
                return@launch
            }
            val request = CreateTestRequest(
                title = title,
                subject = subject,
                className = className,
                totalMarks = totalMarks,
                testDate = testDate
            )
            when (val result = repository.createTest(token, request)) {
                is NetworkResult.Success -> {
                    AppLogger.d("TeacherMarksVM", "Created test ${result.data.testId}")
                    _state.value = _state.value.copy(isCreating = false)
                    onCreated()
                    refresh()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("TeacherMarksVM", "createTest failed: ${result.message}")
                    _state.value = _state.value.copy(isCreating = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isCreating = false, error = "Connection error")
                }
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
