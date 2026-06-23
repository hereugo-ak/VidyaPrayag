package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentStatus
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentType
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CreateAssessmentRequestV2
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.MarkSaveEntryDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.MarksSaveRequest
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TeacherGradebookViewModel — T-305 (Doc 07 §3/§5, Doc 10 §6.4).
 *
 * The clean rebuild of the gradebook state holder, replacing the legacy split of
 * TeacherAssessmentsViewModel (exam picker) + TeacherMarksViewModel (force-publish
 * marks), both deleted in T-305. It is **assignment-scoped** — the screen is reached
 * PRE-SCOPED from a Today /
 * Classes CTA carrying the pre-authorized `assignmentId`; there is no in-VM class picker
 * (F-SHELL-3).
 *
 * It drives a two-mode screen via [GradebookMode]:
 *   • [GradebookMode.List]  — scoped assessment list + inline create (scope pre-filled).
 *   • [GradebookMode.Marks] — the dense, validated marks grid for one assessment, with
 *     a result-driven SAVE (never publishes — the B-MK-1 fix) and an explicit PUBLISH
 *     (the ONLY parent-notify path, confirmed by a dialog that names the assessment).
 *
 * SAVE and PUBLISH are structurally distinct: [save] calls `PUT …/marks` (status stays
 * marks_pending, parents never notified); [publish] calls `POST …/publish` (the only path
 * that flips status→published and notifies parents). Editing a mark after a confirmed save
 * re-arms the result-driven Save button (RA-S18).
 */

/** Which face of the gradebook the screen shows. */
enum class GradebookMode { List, Marks }

/** One editable student row in the marks grid. `marks` is the live local edit. */
data class GradebookStudentMark(
    val studentId: String,
    val name: String,
    val rollNo: String,
    val marks: Float?,
    val isAbsent: Boolean = false,
    val remark: String? = null,
)

data class TeacherGradebookState(
    // Scope — server-resolved from the assignment (the launch context pre-fills it).
    val assignmentId: String = "",
    val scopeHint: String = "",
    val mode: GradebookMode = GradebookMode.List,

    // ── List mode ────────────────────────────────────────────────────────────
    val assessments: List<AssessmentDto> = emptyList(),
    val isListLoading: Boolean = false,
    val listError: String? = null,

    // Inline create (scope pre-filled — no shared picker, F-SHELL-3).
    val createName: String = "",
    val createType: String = AssessmentType.SCHEDULED,
    val createMaxMarks: String = "100",
    val createPassMarks: String = "",
    val createExamDate: String = "",       // "YYYY-MM-DD"; blank → server default
    val isCreating: Boolean = false,
    val createError: String? = null,

    // ── Marks mode ───────────────────────────────────────────────────────────
    val activeAssessment: AssessmentDto? = null,
    val students: List<GradebookStudentMark> = emptyList(),
    val isMarksLoading: Boolean = false,
    val marksError: String? = null,        // load-path (VStateHost)
    // Result-driven SAVE (never publishes).
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,         // save-path (inline)
    // Result-driven PUBLISH (the only notify path).
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val parentsNotified: Int? = null,      // set after a confirmed publish
) {
    val enteredCount: Int get() = students.count { it.marks != null || it.isAbsent }
    val rosterCount: Int get() = students.size
    val maxMarks: Int get() = activeAssessment?.maxMarks ?: 0
    /** Live class average over present (non-absent) entered marks (AB excluded, §5.2). */
    val liveAverage: Float?
        get() {
            val vals = students.filter { !it.isAbsent }.mapNotNull { it.marks }
            return if (vals.isEmpty()) null else vals.average().toFloat()
        }

    val isPublished: Boolean get() = activeAssessment?.isPublished == true
    /** Can publish once at least one student has a mark/AB recorded. */
    val canPublish: Boolean get() = enteredCount > 0 && !isPublished
}

class TeacherGradebookViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherGradebookState())
    val state: StateFlow<TeacherGradebookState> = _state.asStateFlow()

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    // ── List mode ──────────────────────────────────────────────────────────────

    /** Open the gradebook for an authorizing [assignmentId]; loads its scoped list. */
    fun load(assignmentId: String, scopeHint: String = "") {
        if (assignmentId.isBlank()) {
            _state.update {
                it.copy(isListLoading = false, assessments = emptyList(), assignmentId = "")
            }
            return
        }
        _state.update {
            it.copy(
                assignmentId = assignmentId,
                scopeHint = scopeHint.ifBlank { it.scopeHint },
                mode = GradebookMode.List,
            )
        }
        loadList()
    }

    private fun loadList() {
        val asg = _state.value.assignmentId
        if (asg.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isListLoading = true, listError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isListLoading = false, listError = "Not authenticated") }
                return@launch
            }
            when (val r = repository.listAssessments(t, asg)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(isListLoading = false, assessments = r.data.data.assessments) }
                is NetworkResult.Error ->
                    _state.update { it.copy(isListLoading = false, listError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isListLoading = false, listError = "Connection error") }
            }
        }
    }

    fun retryList() = loadList()

    // ── Inline create (scope pre-filled) ─────────────────────────────────────────

    fun setCreateName(v: String) = _state.update { it.copy(createName = v, createError = null) }
    fun setCreateType(v: String) {
        if (v in AssessmentType.ALL) _state.update { it.copy(createType = v) }
    }
    fun setCreateMaxMarks(v: String) =
        _state.update { it.copy(createMaxMarks = v.filter { ch -> ch.isDigit() }.take(4), createError = null) }
    fun setCreatePassMarks(v: String) =
        _state.update { it.copy(createPassMarks = v.filter { ch -> ch.isDigit() }.take(4), createError = null) }
    fun setCreateExamDate(v: String) = _state.update { it.copy(createExamDate = v) }

    /**
     * Create a scoped assessment. Validates client-side (name, max>0, 0≤pass≤max) — the
     * server re-validates (§3). On success the list refreshes and the form resets.
     */
    fun createAssessment() {
        val s = _state.value
        val asg = s.assignmentId
        if (asg.isBlank()) return
        val name = s.createName.trim()
        val max = s.createMaxMarks.toIntOrNull()
        val pass = s.createPassMarks.takeIf { it.isNotBlank() }?.toIntOrNull()
        when {
            name.isBlank() -> { _state.update { it.copy(createError = "Name is required") }; return }
            max == null || max <= 0 -> { _state.update { it.copy(createError = "Max marks must be greater than 0") }; return }
            pass != null && (pass < 0 || pass > max) -> { _state.update { it.copy(createError = "Pass marks must be between 0 and $max") }; return }
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isCreating = false, createError = "Not authenticated") }
                return@launch
            }
            val req = CreateAssessmentRequestV2(
                assignmentId = asg,
                name = name,
                type = s.createType,
                maxMarks = max!!,
                passMarks = pass,
                examDate = s.createExamDate.takeIf { it.isNotBlank() },
                linkToCalendar = false,
            )
            when (val r = repository.createAssessmentV2(t, req)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isCreating = false,
                            createName = "",
                            createType = AssessmentType.SCHEDULED,
                            createMaxMarks = "100",
                            createPassMarks = "",
                            createExamDate = "",
                        )
                    }
                    loadList()
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isCreating = false, createError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isCreating = false, createError = "Connection error") }
            }
        }
    }

    // ── Marks mode ───────────────────────────────────────────────────────────────

    /** Open the marks grid for one assessment (loads its roster + any existing marks). */
    fun openMarks(assessment: AssessmentDto) {
        _state.update {
            it.copy(
                mode = GradebookMode.Marks,
                activeAssessment = assessment,
                students = emptyList(),
                marksError = null,
                saveSuccess = false,
                saveError = null,
                publishError = null,
                parentsNotified = null,
            )
        }
        loadMarks(assessment.id)
    }

    private fun loadMarks(assessmentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isMarksLoading = true, marksError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isMarksLoading = false, marksError = "Not authenticated") }
                return@launch
            }
            when (val r = repository.getAssessmentMarks(t, assessmentId)) {
                is NetworkResult.Success -> {
                    val d = r.data.data
                    _state.update {
                        it.copy(
                            isMarksLoading = false,
                            activeAssessment = d.assessment,
                            students = d.students.map { e ->
                                GradebookStudentMark(
                                    studentId = e.studentId,
                                    name = e.name,
                                    rollNo = e.rollNo,
                                    marks = e.marks,
                                    isAbsent = e.isAbsent,
                                    remark = e.remark,
                                )
                            },
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isMarksLoading = false, marksError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isMarksLoading = false, marksError = "Connection error") }
            }
        }
    }

    fun retryMarks() {
        _state.value.activeAssessment?.let { loadMarks(it.id) }
    }

    /** Back from the marks grid to the scoped list (refreshes counts). */
    fun backToList() {
        _state.update {
            it.copy(
                mode = GradebookMode.List,
                activeAssessment = null,
                students = emptyList(),
                saveSuccess = false,
                saveError = null,
                publishError = null,
                parentsNotified = null,
            )
        }
        loadList()
    }

    /**
     * Local edit — set one student's mark. Clamped to [0, maxMarks]; pass null to clear.
     * Setting a non-null mark clears the AB flag (a real mark is not an absence).
     */
    fun setMark(studentId: String, marks: Float?) {
        _state.update { s ->
            val clamped = marks?.coerceIn(0f, s.maxMarks.toFloat())
            s.copy(
                students = s.students.map {
                    if (it.studentId == studentId)
                        it.copy(marks = clamped, isAbsent = if (clamped != null) false else it.isAbsent)
                    else it
                },
                saveSuccess = false,
            )
        }
    }

    /** Toggle a student's ABSENT (AB) flag — a state distinct from a real 0 (§5.2). */
    fun toggleAbsent(studentId: String) {
        _state.update { s ->
            s.copy(
                students = s.students.map {
                    if (it.studentId == studentId) {
                        val nextAbsent = !it.isAbsent
                        it.copy(isAbsent = nextAbsent, marks = if (nextAbsent) null else it.marks)
                    } else it
                },
                saveSuccess = false,
            )
        }
    }

    /**
     * SAVE marks — writes `assessment_marks`, status stays marks_pending. NEVER publishes
     * (the B-MK-1 fix). On success the active assessment's status/counts are echoed back so
     * the UI can prove "saved, NOT published".
     */
    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val assessment = s.activeAssessment ?: return@launch
            if (s.students.isEmpty()) return@launch
            _state.update { it.copy(isSaving = true, saveError = null, saveSuccess = false) }
            val t = token() ?: run {
                _state.update { it.copy(isSaving = false, saveError = "Not authenticated") }
                return@launch
            }
            val req = MarksSaveRequest(
                entries = s.students.map {
                    MarkSaveEntryDto(
                        studentId = it.studentId,
                        marks = if (it.isAbsent) null else it.marks,
                        isAbsent = it.isAbsent,
                        remark = it.remark,
                    )
                },
            )
            when (val r = repository.saveAssessmentMarks(t, assessment.id, req)) {
                is NetworkResult.Success -> {
                    val d = r.data.data
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            activeAssessment = it.activeAssessment?.copy(
                                status = d.status,
                                enteredCount = d.enteredCount,
                                rosterCount = d.rosterCount,
                            ),
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isSaving = false, saveError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isSaving = false, saveError = "Connection error") }
            }
        }
    }

    /**
     * PUBLISH — the ONLY path that flips status→published and notifies parents (Doc 07 §2).
     * The screen confirms this with a dialog that names the assessment BEFORE calling; the
     * response carries how many parents were notified.
     */
    fun publish() {
        viewModelScope.launch {
            val assessment = _state.value.activeAssessment ?: return@launch
            _state.update { it.copy(isPublishing = true, publishError = null, parentsNotified = null) }
            val t = token() ?: run {
                _state.update { it.copy(isPublishing = false, publishError = "Not authenticated") }
                return@launch
            }
            when (val r = repository.publishAssessment(t, assessment.id)) {
                is NetworkResult.Success -> {
                    val d = r.data.data
                    _state.update {
                        it.copy(
                            isPublishing = false,
                            parentsNotified = d.parentsNotified,
                            activeAssessment = it.activeAssessment?.copy(
                                status = d.status,
                                publishedAt = d.publishedAt,
                            ),
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isPublishing = false, publishError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isPublishing = false, publishError = "Connection error") }
            }
        }
    }

    /** Retract a published assessment (audited; no re-notify). */
    fun unpublish() {
        viewModelScope.launch {
            val assessment = _state.value.activeAssessment ?: return@launch
            _state.update { it.copy(isPublishing = true, publishError = null, parentsNotified = null) }
            val t = token() ?: run {
                _state.update { it.copy(isPublishing = false, publishError = "Not authenticated") }
                return@launch
            }
            when (val r = repository.unpublishAssessment(t, assessment.id)) {
                is NetworkResult.Success -> {
                    val d = r.data.data
                    _state.update {
                        it.copy(
                            isPublishing = false,
                            activeAssessment = it.activeAssessment?.copy(
                                status = d.status,
                                publishedAt = d.publishedAt,
                            ),
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(isPublishing = false, publishError = r.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isPublishing = false, publishError = "Connection error") }
            }
        }
    }
}
