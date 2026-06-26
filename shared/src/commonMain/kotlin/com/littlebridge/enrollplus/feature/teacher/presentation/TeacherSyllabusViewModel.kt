package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.CreateSyllabusUnitRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.SyllabusNodeDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ToggleSyllabusProgressRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.UpdateSyllabusUnitRequest
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T-403 — the Planner › Syllabus state holder, rebuilt from scratch (DELETE-don't-patch)
 * on the typed, assignment-scoped plane (T-402). The core gesture is a SINGLE TAP on a unit
 * row → optimistic coverage toggle (no form, no save button — Doc 08 §2/F-SYL-1). Hierarchy
 * (chapter ▸ topic) comes pre-flattened from the server (each node carries depth/parentId).
 *
 * Reached PRE-SCOPED by [assignmentId] (X-1) — never a free-text class/subject (contrast the
 * legacy getSyllabus(classId, subject)). The legacy UpdateSyllabusRequest toggle is gone;
 * this uses the typed ToggleSyllabusProgressRequest with a server-stamped covered_on.
 */

/** A flattened syllabus node (chapter or topic) with this section's coverage state. */
data class SyllabusUnit(
    val id: String,
    val parentId: String?,
    val title: String,
    val depth: Int,
    val isChapter: Boolean,
    val isCovered: Boolean,
    val coveredOn: String?,
    val note: String?,
)

data class TeacherSyllabusState(
    val assignmentId: String = "",
    val className: String = "",
    val section: String = "",
    val subject: String = "",
    val units: List<SyllabusUnit> = emptyList(),
    val coveredCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    // The unit currently being persisted (drives a row spinner / disables re-tap).
    val updatingUnitId: String? = null,
    val error: String? = null,
    // Edit mode surfaces the rare, deliberate affordances (add / rename) behind a toggle so
    // they never clutter the one-tap path (Doc 08 §2 "secondary, deliberate").
    val isEditing: Boolean = false,
    // Add-unit composer: null = closed; "" = a chapter, else a topic under that chapter id.
    val addingUnderParentId: String? = null,
    val addTitle: String = "",
    val isAdding: Boolean = false,
    val addError: String? = null,
) {
    /** 0..1; 0 when nothing to cover yet (honest, never NaN). */
    val progress: Float get() = if (totalCount == 0) 0f else coveredCount.toFloat() / totalCount
    val hasUnits: Boolean get() = units.isNotEmpty()
    /** Chapters only (for the "add topic under…" parent choices). */
    val chapters: List<SyllabusUnit> get() = units.filter { it.isChapter }
}

class TeacherSyllabusViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherSyllabusState())
    val state: StateFlow<TeacherSyllabusState> = _state.asStateFlow()

    /** Load the hierarchical units + this assignment's coverage. The screen supplies the assignmentId. */
    fun load(assignmentId: String) {
        if (assignmentId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(assignmentId = assignmentId, isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val result = repository.loadSyllabus(token, assignmentId)) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            className = d.className,
                            section = d.section,
                            subject = d.subject,
                            units = d.units.map { u -> u.toUi() },
                            coveredCount = d.coveredCount,
                            totalCount = d.totalCount,
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun retry() = load(_state.value.assignmentId)

    /**
     * The one-tap toggle. Optimistically flips local coverage + the covered count, then persists
     * via the typed PATCH /progress (server stamps covered_on=today, covered_by=me); reverts on
     * failure. Idempotent — a re-tap un-covers (clears covered_on).
     */
    fun toggleUnit(unitId: String) {
        val s0 = _state.value
        if (s0.assignmentId.isBlank()) return
        val target = s0.units.firstOrNull { it.id == unitId } ?: return
        val newCovered = !target.isCovered

        viewModelScope.launch {
            val before = s0.units
            val beforeCovered = s0.coveredCount

            // Optimistic local flip (covered_on shown once the server confirms).
            _state.update { s ->
                s.copy(
                    updatingUnitId = unitId,
                    error = null,
                    units = s.units.map { if (it.id == unitId) it.copy(isCovered = newCovered) else it },
                    coveredCount = (s.coveredCount + if (newCovered) 1 else -1).coerceIn(0, s.totalCount),
                )
            }

            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(updatingUnitId = null, units = before, coveredCount = beforeCovered, error = "Not authenticated") }
                return@launch
            }
            val request = ToggleSyllabusProgressRequest(
                assignmentId = s0.assignmentId,
                unitId = unitId,
                isCovered = newCovered,
            )
            when (val result = repository.toggleSyllabusProgress(token, request)) {
                is NetworkResult.Success -> {
                    // Reconcile to the server's authoritative node (covered_on/note).
                    val node = result.data.data?.toUi()
                    _state.update { s ->
                        s.copy(
                            updatingUnitId = null,
                            units = if (node != null) s.units.map { if (it.id == unitId) node else it } else s.units,
                        )
                    }
                }
                is NetworkResult.Error ->
                    _state.update { it.copy(updatingUnitId = null, units = before, coveredCount = beforeCovered, error = result.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(updatingUnitId = null, units = before, coveredCount = beforeCovered, error = "Connection error") }
            }
        }
    }

    // ── Edit mode (deliberate, behind a toggle) ──────────────────────────────

    fun toggleEditing() = _state.update { it.copy(isEditing = !it.isEditing, addingUnderParentId = null, addError = null) }

    /** Open the add composer. parentId "" / null → a chapter; a chapter id → a topic under it. */
    fun openAdd(parentId: String?) = _state.update {
        it.copy(addingUnderParentId = parentId ?: "", addTitle = "", addError = null)
    }

    fun closeAdd() = _state.update { it.copy(addingUnderParentId = null, addTitle = "", addError = null) }

    fun setAddTitle(value: String) = _state.update { it.copy(addTitle = value, addError = null) }

    /** Create a unit through the typed POST. On success reloads so positions/hierarchy stay authoritative. */
    fun submitAdd() {
        val s0 = _state.value
        if (s0.assignmentId.isBlank()) return
        val title = s0.addTitle.trim()
        if (title.isBlank()) {
            _state.update { it.copy(addError = "Give the unit a title") }
            return
        }
        val parentRaw = s0.addingUnderParentId
        val parentId = parentRaw?.takeIf { it.isNotBlank() }

        viewModelScope.launch {
            _state.update { it.copy(isAdding = true, addError = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isAdding = false, addError = "Not authenticated") }
                return@launch
            }
            val request = CreateSyllabusUnitRequest(
                assignmentId = s0.assignmentId,
                title = title,
                parentId = parentId,
            )
            when (val result = repository.createSyllabusUnit(token, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isAdding = false, addingUnderParentId = null, addTitle = "") }
                    load(s0.assignmentId)   // reload for authoritative ordering/hierarchy
                }
                is NetworkResult.Error -> _state.update { it.copy(isAdding = false, addError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isAdding = false, addError = "Connection error") }
            }
        }
    }

    /** Rename a unit (edit mode). Optimistic; reverts on failure. */
    fun renameUnit(unitId: String, newTitle: String) {
        val s0 = _state.value
        val title = newTitle.trim()
        if (s0.assignmentId.isBlank() || title.isBlank()) return
        val before = s0.units

        viewModelScope.launch {
            _state.update { s ->
                s.copy(units = s.units.map { if (it.id == unitId) it.copy(title = title) else it })
            }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(units = before, error = "Not authenticated") }
                return@launch
            }
            val result = repository.updateSyllabusUnit(
                token, s0.assignmentId, unitId, UpdateSyllabusUnitRequest(title = title),
            )
            when (result) {
                is NetworkResult.Success -> Unit
                is NetworkResult.Error -> _state.update { it.copy(units = before, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(units = before, error = "Connection error") }
            }
        }
    }
}

private fun SyllabusNodeDto.toUi() = SyllabusUnit(
    id = id,
    parentId = parentId,
    title = title,
    depth = depth,
    isChapter = isChapter,
    isCovered = isCovered,
    coveredOn = coveredOn,
    note = note,
)
