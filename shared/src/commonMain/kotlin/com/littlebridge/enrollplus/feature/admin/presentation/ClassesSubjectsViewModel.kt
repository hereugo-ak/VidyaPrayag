package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.repository.SchoolClassesRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ClassesSubjectsState(
    val classes: List<SchoolClassDto> = emptyList(),
    val subjectsByClass: Map<String, List<SchoolSubjectDto>> = emptyMap(),
    val timetable: TimetableDto? = null,
    val selectedClassId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class ClassesSubjectsViewModel(
    private val repository: SchoolClassesRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClassesSubjectsState())
    val state: StateFlow<ClassesSubjectsState> = _state.asStateFlow()

    init {
        loadClasses()
    }

    // ── Classes ────────────────────────────────────────────────────────────────

    fun loadClasses() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.listClasses(token)) {
                is NetworkResult.Success -> {
                    val classes = result.data.data?.classes ?: emptyList()
                    _state.value = _state.value.copy(classes = classes, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ClassesVM", "list error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun createClass(code: String, name: String, sections: List<String>, onDone: (() -> Unit)? = null) {
        if (code.isBlank() || name.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Code and name are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.createClass(token, CreateSchoolClassRequest(code.trim(), name.trim(), sections))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Class created")
                    onDone?.invoke()
                    loadClasses()
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

    fun updateClass(id: String, code: String, name: String, sections: List<String>, onDone: (() -> Unit)? = null) {
        if (code.isBlank() || name.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Code and name are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.updateClass(token, id, UpdateSchoolClassRequest(code.trim(), name.trim(), sections))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Class updated")
                    onDone?.invoke()
                    loadClasses()
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

    fun deleteClass(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.deleteClass(token, id)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Class deleted")
                    loadClasses()
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

    // ── Subjects ───────────────────────────────────────────────────────────────

    fun loadSubjects(classId: String) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val result = repository.listSubjects(token, classId)) {
                is NetworkResult.Success -> {
                    val subjects = result.data.data?.subjects ?: emptyList()
                    _state.value = _state.value.copy(
                        subjectsByClass = _state.value.subjectsByClass + (classId to subjects),
                        selectedClassId = classId,
                    )
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ClassesVM", "subjects error: ${result.message}")
                    _state.value = _state.value.copy(errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun createSubject(classId: String, name: String, code: String, onDone: (() -> Unit)? = null) {
        if (name.isBlank() || code.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Subject name and code are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.createSubject(token, classId, CreateSchoolSubjectRequest(name.trim(), code.trim()))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Subject added")
                    onDone?.invoke()
                    loadSubjects(classId)
                    loadClasses()
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

    fun updateSubject(subjectId: String, classId: String, name: String, code: String, onDone: (() -> Unit)? = null) {
        if (name.isBlank() || code.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Subject name and code are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.updateSubject(token, subjectId, UpdateSchoolSubjectRequest(name.trim(), code.trim()))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Subject updated")
                    onDone?.invoke()
                    loadSubjects(classId)
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

    fun deleteSubject(subjectId: String, classId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.deleteSubject(token, subjectId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Subject deleted")
                    loadSubjects(classId)
                    loadClasses()
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

    // ── Timetable ──────────────────────────────────────────────────────────────

    fun loadTimetable(classFilter: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = repository.getTimetable(token, classFilter)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(timetable = result.data.data, isLoading = false)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ClassesVM", "timetable error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun selectClass(classId: String?) {
        _state.value = _state.value.copy(selectedClassId = classId)
        if (classId != null) loadSubjects(classId)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, infoMessage = null)
    }
}
