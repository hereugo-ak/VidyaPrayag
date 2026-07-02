package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkCreatePeriodsRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkCreatePeriodsResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkPeriodItem
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateTeacherRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TeacherCardDto
import com.littlebridge.enrollplus.feature.admin.domain.model.ChangeRequestListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateChangeRequestRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateExceptionRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreatePeriodRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodDetailDto
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodExceptionDto
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodExceptionListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.ReviewRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdatePeriodRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.repository.SchoolClassesRepository
import com.littlebridge.enrollplus.feature.admin.domain.repository.TeachersRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ClassesSubjectsState(
    val classes: List<SchoolClassDto> = emptyList(),
    val subjectsByClass: Map<String, List<SchoolSubjectDto>> = emptyMap(),
    val teachers: List<TeacherCardDto> = emptyList(),
    val timetable: TimetableDto? = null,
    val selectedClassId: String? = null,
    val exceptions: List<PeriodExceptionDto> = emptyList(),
    val changeRequests: List<TimetableChangeRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

class ClassesSubjectsViewModel(
    private val repository: SchoolClassesRepository,
    private val preferenceRepository: PreferenceRepository,
    private val teachersRepository: TeachersRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClassesSubjectsState())
    val state: StateFlow<ClassesSubjectsState> = _state.asStateFlow()

    init {
        loadClasses()
        loadTeachers()
    }

    // ── Teachers (for timetable dropdowns) ─────────────────────────────────────

    fun loadTeachers() {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val r = teachersRepository.getTeachers(token, page = 1, pageSize = 100)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(teachers = r.data.data?.teachers ?: emptyList())
                }
                is NetworkResult.Error -> {
                    AppLogger.e("ClassesVM", "teachers error: ${r.message}")
                }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun createTeacherInline(name: String, identifier: String, onDone: (() -> Unit)? = null) {
        if (name.isBlank() || identifier.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Teacher name and identifier (email/phone) are required.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = teachersRepository.createTeacher(token, CreateTeacherRequest(name.trim(), identifier.trim()))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Teacher created")
                    onDone?.invoke()
                    loadTeachers()
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

    // ── Period CRUD ────────────────────────────────────────────────────────────

    fun createPeriod(teacherId: String, className: String, section: String, subject: String, weekday: Int, startTime: String, endTime: String, room: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.createPeriod(token, CreatePeriodRequest(teacherId, className, section, subject, weekday, startTime, endTime, room))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Period created")
                    onDone?.invoke()
                    loadTimetable()
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

    fun bulkCreatePeriods(weekday: Int, periods: List<BulkPeriodItem>, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.bulkCreatePeriods(token, BulkCreatePeriodsRequest(weekday, periods))) {
                is NetworkResult.Success -> {
                    val data = r.data.data
                    val msg = if (data != null && data.errorCount > 0) {
                        "${data.createdCount} periods created, ${data.errorCount} errors"
                    } else "${data?.createdCount ?: 0} periods created"
                    _state.value = _state.value.copy(isSaving = false, infoMessage = msg)
                    onDone?.invoke()
                    loadTimetable()
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

    fun updatePeriod(periodId: String, weekday: Int?, startTime: String?, endTime: String?, room: String?, isActive: Boolean?, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.updatePeriod(token, periodId, UpdatePeriodRequest(weekday, startTime, endTime, room, isActive))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Period updated")
                    onDone?.invoke()
                    loadTimetable()
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

    fun deletePeriod(periodId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.deletePeriod(token, periodId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Period deleted")
                    loadTimetable()
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

    // ── Period Exceptions ──────────────────────────────────────────────────────

    fun loadExceptions(date: String? = null) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val r = repository.listExceptions(token, date)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(exceptions = r.data.data?.exceptions ?: emptyList())
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun createException(req: CreateExceptionRequest, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.createException(token, req)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Exception created")
                    onDone?.invoke()
                    loadExceptions()
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

    fun deleteException(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.deleteException(token, id)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Exception deleted")
                    loadExceptions()
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

    // ── Change Requests (admin) ────────────────────────────────────────────────

    fun loadChangeRequests(status: String? = null) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) return@launch
            when (val r = repository.listChangeRequests(token, status)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(changeRequests = r.data.data?.requests ?: emptyList())
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = r.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(errorMessage = "Connection error. Check your internet.")
                }
            }
        }
    }

    fun approveChangeRequest(id: String, adminNote: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.approveChangeRequest(token, id, ReviewRequest(adminNote))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Request approved")
                    loadChangeRequests()
                    loadTimetable()
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

    fun rejectChangeRequest(id: String, adminNote: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Not signed in")
                return@launch
            }
            when (val r = repository.rejectChangeRequest(token, id, ReviewRequest(adminNote))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, infoMessage = "Request rejected")
                    loadChangeRequests()
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
