package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.CreateLessonPlanRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkItemDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.InstantiateFromTemplateRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.LessonActivityDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.LessonCalendarDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.LessonPlanDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.LessonTemplateDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.SaveLessonTemplateRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.SyllabusNodeDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.UpdateLessonPlanRequest
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.enrollplus.util.todayIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LESSON_PLANNING_SPEC.md (P1-20) — the Lesson Plan state holder.
 *
 * Three modes:
 *   • LIST     — this assignment's lesson plans (filterable by status/date) + create composer
 *   • EDITOR   — single plan edit: title, objectives, activities, resources, assessment, duration, date, unit
 *   • CALENDAR — month-level view with plans grouped by day
 *   • TEMPLATES — list own+shared templates; instantiate or save new
 *
 * Reached PRE-SCOPED by [assignmentId] (X-1) — same pattern as Homework/Syllabus/Attendance.
 */

enum class LessonPlanMode { List, Editor, Calendar, Templates }

data class LessonPlanSummary(
    val id: String,
    val title: String,
    val status: String,
    val plannedDate: String?,
    val subjectName: String,
    val unitTitle: String?,
    val durationMinutes: Int,
)

data class LessonPlanEditorState(
    val planId: String? = null,
    val title: String = "",
    val objectives: List<String> = emptyList(),
    val activities: List<LessonActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    val assessmentMethod: String = "",
    val durationMinutes: Int = 45,
    val plannedDate: String = "",
    val curriculumUnitId: String? = null,
    val homeworkId: String? = null,
    val status: String = "planned",
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isNew: Boolean get() = planId == null
    val canSave: Boolean get() = title.isNotBlank()
    val isCompleted: Boolean get() = status == "completed"
    val isSkipped: Boolean get() = status == "skipped"
}

data class LessonCalendarState(
    val month: String = "",
    val days: List<LessonCalendarDay> = emptyList(),
)

data class LessonCalendarDay(
    val date: String,
    val plans: List<LessonPlanSummary> = emptyList(),
)

data class TeacherLessonPlanState(
    val assignmentId: String = "",
    val scopeLabel: String = "",
    val mode: LessonPlanMode = LessonPlanMode.List,
    // ── list ──
    val items: List<LessonPlanSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusFilter: String? = null,
    // ── editor ──
    val editor: LessonPlanEditorState = LessonPlanEditorState(),
    val syllabusUnits: List<SyllabusNodeDto> = emptyList(),
    val homeworkOptions: List<HomeworkItemDto> = emptyList(),
    // ── calendar ──
    val calendar: LessonCalendarState = LessonCalendarState(),
    val isCalendarLoading: Boolean = false,
    // ── templates ──
    val templates: List<LessonTemplateDto> = emptyList(),
    val isTemplatesLoading: Boolean = false,
    val templatesError: String? = null,
    val showSaveTemplateDialog: Boolean = false,
    val templateTitle: String = "",
    val templateIsShared: Boolean = false,
    val isSavingTemplate: Boolean = false,
    val showInstantiateDialog: Boolean = false,
    val instantiateTemplateId: String? = null,
    val instantiateDate: String = "",
    val isInstantiating: Boolean = false,
)

class TeacherLessonPlanViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherLessonPlanState())
    val state: StateFlow<TeacherLessonPlanState> = _state.asStateFlow()

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    // ── LIST ──────────────────────────────────────────────────────────────────

    fun load(assignmentId: String, scopeLabel: String = "") {
        if (assignmentId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(assignmentId = assignmentId, scopeLabel = scopeLabel, isLoading = true, error = null) }
            val t = token() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            val result = repository.listLessonPlans(t, assignmentId, status = _state.value.statusFilter)
            when (result) {
                is NetworkResult.Success -> {
                    val items = result.data.data.map { it.toSummary() }
                    _state.update { it.copy(items = items, isLoading = false) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message ?: "Failed to load") }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun retry() { load(_state.value.assignmentId, _state.value.scopeLabel) }

    fun setStatusFilter(status: String?) {
        _state.update { it.copy(statusFilter = status) }
        load(_state.value.assignmentId, _state.value.scopeLabel)
    }

    // ── EDITOR ─────────────────────────────────────────────────────────────────

    fun openNewEditor() {
        _state.update {
            it.copy(
                mode = LessonPlanMode.Editor,
                editor = LessonPlanEditorState(plannedDate = todayIso()),
            )
        }
        loadEditorOptions()
    }

    fun openExistingEditor(planId: String) {
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(mode = LessonPlanMode.Editor, editor = it.editor.copy(isSaving = true, error = null)) }
            loadEditorOptions()
            val result = repository.getLessonPlan(t, planId)
            when (result) {
                is NetworkResult.Success -> {
                    val p = result.data.data
                    _state.update {
                        it.copy(editor = LessonPlanEditorState(
                            planId = p.id,
                            title = p.title,
                            objectives = p.objectives,
                            activities = p.activities,
                            resources = p.resources,
                            assessmentMethod = p.assessmentMethod ?: "",
                            durationMinutes = p.durationMinutes,
                            plannedDate = p.plannedDate ?: "",
                            curriculumUnitId = p.curriculumUnitId,
                            homeworkId = p.homeworkId,
                            status = p.status,
                        ))
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(editor = it.editor.copy(isSaving = false, error = result.message ?: "Failed to load")) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun setEditorTitle(v: String) { _state.update { it.copy(editor = it.editor.copy(title = v)) } }
    fun setEditorDate(v: String) { _state.update { it.copy(editor = it.editor.copy(plannedDate = v)) } }
    fun setEditorDuration(v: Int) { _state.update { it.copy(editor = it.editor.copy(durationMinutes = v)) } }
    fun setEditorAssessment(v: String) { _state.update { it.copy(editor = it.editor.copy(assessmentMethod = v)) } }
    fun setEditorUnit(v: String?) { _state.update { it.copy(editor = it.editor.copy(curriculumUnitId = v)) } }
    fun setEditorHomework(v: String?) { _state.update { it.copy(editor = it.editor.copy(homeworkId = v)) } }

    private fun loadEditorOptions() {
        viewModelScope.launch {
            val t = token() ?: return@launch
            val asgId = _state.value.assignmentId
            // Load syllabus units for the unit picker
            val syllabusResult = repository.loadSyllabus(t, asgId)
            when (syllabusResult) {
                is NetworkResult.Success -> _state.update { it.copy(syllabusUnits = syllabusResult.data.data.units) }
                else -> {}
            }
            // Load homework list for the homework link picker
            val hwResult = repository.listHomework(t, asgId)
            when (hwResult) {
                is NetworkResult.Success -> _state.update { it.copy(homeworkOptions = hwResult.data.data.items) }
                else -> {}
            }
        }
    }

    fun addObjective(text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(editor = it.editor.copy(objectives = it.editor.objectives + text)) }
    }
    fun removeObjective(index: Int) {
        _state.update { it.copy(editor = it.editor.copy(objectives = it.editor.objectives.toMutableList().also { it.removeAt(index) })) }
    }
    fun addResource(text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(editor = it.editor.copy(resources = it.editor.resources + text)) }
    }
    fun removeResource(index: Int) {
        _state.update { it.copy(editor = it.editor.copy(resources = it.editor.resources.toMutableList().also { it.removeAt(index) })) }
    }
    fun addActivity(text: String, duration: Int = 15) {
        if (text.isBlank()) return
        _state.update { it.copy(editor = it.editor.copy(activities = it.editor.activities + LessonActivityDto(text, duration))) }
    }
    fun removeActivity(index: Int) {
        _state.update { it.copy(editor = it.editor.copy(activities = it.editor.activities.toMutableList().also { it.removeAt(index) })) }
    }

    fun savePlan() {
        val e = _state.value.editor
        if (!e.canSave) return
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(editor = it.editor.copy(isSaving = true, error = null)) }
            val result = if (e.isNew) {
                repository.createLessonPlan(t, CreateLessonPlanRequest(
                    assignmentId = _state.value.assignmentId,
                    curriculumUnitId = e.curriculumUnitId,
                    title = e.title.trim(),
                    objectives = e.objectives,
                    activities = e.activities,
                    resources = e.resources,
                    assessmentMethod = e.assessmentMethod.ifBlank { null },
                    durationMinutes = e.durationMinutes,
                    homeworkId = e.homeworkId,
                    plannedDate = e.plannedDate.ifBlank { null },
                ))
            } else {
                repository.updateLessonPlan(t, e.planId!!, UpdateLessonPlanRequest(
                    curriculumUnitId = e.curriculumUnitId,
                    title = e.title.trim(),
                    objectives = e.objectives,
                    activities = e.activities,
                    resources = e.resources,
                    assessmentMethod = e.assessmentMethod.ifBlank { null },
                    durationMinutes = e.durationMinutes,
                    homeworkId = e.homeworkId,
                    plannedDate = e.plannedDate.ifBlank { null },
                ))
            }
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(mode = LessonPlanMode.List, editor = LessonPlanEditorState()) }
                    load(_state.value.assignmentId, _state.value.scopeLabel)
                }
                is NetworkResult.Error -> _state.update { it.copy(editor = it.editor.copy(isSaving = false, error = result.message ?: "Save failed")) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun completePlan() {
        val planId = _state.value.editor.planId ?: return
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(editor = it.editor.copy(isSaving = true, error = null)) }
            val result = repository.completeLessonPlan(t, planId)
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(mode = LessonPlanMode.List, editor = LessonPlanEditorState()) }
                    load(_state.value.assignmentId, _state.value.scopeLabel)
                }
                is NetworkResult.Error -> _state.update { it.copy(editor = it.editor.copy(isSaving = false, error = result.message ?: "Complete failed")) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun skipPlan() {
        val planId = _state.value.editor.planId ?: return
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(editor = it.editor.copy(isSaving = true, error = null)) }
            val result = repository.skipLessonPlan(t, planId)
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(mode = LessonPlanMode.List, editor = LessonPlanEditorState()) }
                    load(_state.value.assignmentId, _state.value.scopeLabel)
                }
                is NetworkResult.Error -> _state.update { it.copy(editor = it.editor.copy(isSaving = false, error = result.message ?: "Skip failed")) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun deletePlan() {
        val planId = _state.value.editor.planId ?: return
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(editor = it.editor.copy(isSaving = true, error = null)) }
            val result = repository.deleteLessonPlan(t, planId)
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(mode = LessonPlanMode.List, editor = LessonPlanEditorState()) }
                    load(_state.value.assignmentId, _state.value.scopeLabel)
                }
                is NetworkResult.Error -> _state.update { it.copy(editor = it.editor.copy(isSaving = false, error = result.message ?: "Delete failed")) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun closeEditor() {
        _state.update { it.copy(mode = LessonPlanMode.List, editor = LessonPlanEditorState()) }
    }

    // ── CALENDAR ────────────────────────────────────────────────────────────────

    fun openCalendar() {
        val month = todayIso().substring(0, 7)
        loadCalendar(month)
        _state.update { it.copy(mode = LessonPlanMode.Calendar) }
    }

    fun loadCalendar(month: String) {
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(isCalendarLoading = true) }
            val result = repository.getLessonCalendar(t, _state.value.assignmentId, month)
            when (result) {
                is NetworkResult.Success -> {
                    val cal = result.data.data
                    _state.update {
                        it.copy(calendar = LessonCalendarState(
                            month = cal.month,
                            days = cal.days.map { d -> LessonCalendarDay(
                                date = d.date,
                                plans = d.plans.map { p -> p.toSummary() },
                            )},
                        ), isCalendarLoading = false)
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isCalendarLoading = false) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun closeCalendar() { _state.update { it.copy(mode = LessonPlanMode.List) } }

    // ── TEMPLATES ───────────────────────────────────────────────────────────────

    fun openTemplates() {
        _state.update { it.copy(mode = LessonPlanMode.Templates) }
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(isTemplatesLoading = true, templatesError = null) }
            val result = repository.listLessonTemplates(t, _state.value.assignmentId)
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(templates = result.data.data, isTemplatesLoading = false) }
                is NetworkResult.Error -> _state.update { it.copy(isTemplatesLoading = false, templatesError = result.message ?: "Failed to load") }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun closeTemplates() { _state.update { it.copy(mode = LessonPlanMode.List) } }

    fun openSaveTemplateDialog() {
        _state.update { it.copy(showSaveTemplateDialog = true, templateTitle = _state.value.editor.title) }
    }

    fun closeSaveTemplateDialog() {
        _state.update { it.copy(showSaveTemplateDialog = false, templateTitle = "", templateIsShared = false) }
    }

    fun setTemplateTitle(v: String) { _state.update { it.copy(templateTitle = v) } }
    fun setTemplateShared(v: Boolean) { _state.update { it.copy(templateIsShared = v) } }

    fun saveTemplate() {
        val e = _state.value.editor
        val title = _state.value.templateTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(isSavingTemplate = true) }
            val result = repository.saveLessonTemplate(t, SaveLessonTemplateRequest(
                assignmentId = _state.value.assignmentId,
                title = title,
                objectives = e.objectives,
                activities = e.activities,
                resources = e.resources,
                assessmentMethod = e.assessmentMethod.ifBlank { null },
                durationMinutes = e.durationMinutes,
                isShared = _state.value.templateIsShared,
            ))
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(isSavingTemplate = false, showSaveTemplateDialog = false, templateTitle = "", templateIsShared = false) }
                is NetworkResult.Error -> _state.update { it.copy(isSavingTemplate = false) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun openInstantiateDialog(templateId: String) {
        _state.update { it.copy(showInstantiateDialog = true, instantiateTemplateId = templateId, instantiateDate = todayIso()) }
    }

    fun closeInstantiateDialog() {
        _state.update { it.copy(showInstantiateDialog = false, instantiateTemplateId = null, instantiateDate = "") }
    }

    fun setInstantiateDate(v: String) { _state.update { it.copy(instantiateDate = v) } }

    fun instantiateFromTemplate() {
        val templateId = _state.value.instantiateTemplateId ?: return
        val date = _state.value.instantiateDate
        if (date.isBlank()) return
        viewModelScope.launch {
            val t = token() ?: return@launch
            _state.update { it.copy(isInstantiating = true) }
            val result = repository.instantiateLessonFromTemplate(t, templateId, InstantiateFromTemplateRequest(
                assignmentId = _state.value.assignmentId,
                plannedDate = date,
            ))
            when (result) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isInstantiating = false, showInstantiateDialog = false, instantiateTemplateId = null, mode = LessonPlanMode.List) }
                    load(_state.value.assignmentId, _state.value.scopeLabel)
                }
                is NetworkResult.Error -> _state.update { it.copy(isInstantiating = false) }
                is NetworkResult.ConnectionError -> {}
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            val t = token() ?: return@launch
            repository.deleteLessonTemplate(t, templateId)
            loadTemplates()
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private fun LessonPlanDto.toSummary() = LessonPlanSummary(
        id = id,
        title = title,
        status = status,
        plannedDate = plannedDate,
        subjectName = subjectName,
        unitTitle = curriculumUnitTitle,
        durationMinutes = durationMinutes,
    )
}
