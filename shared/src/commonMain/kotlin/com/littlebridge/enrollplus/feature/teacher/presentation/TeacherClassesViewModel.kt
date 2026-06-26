package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassDetailData
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClassBroadcastRequest
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClassSummaryDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T-504 — Classes presentation. Wired to the rebuilt typed Classes plane:
 *   • list  → TeacherRepository.listClassesV2  (GET /teacher/classes-v2 — one aggregated query)
 *   • detail→ TeacherRepository.getClassDetailV2(assignmentId) (composite, real roster)
 *
 * The legacy `TeacherClass` / `getClasses` shape (looping N+1, hardcoded
 * isClassTeacher=false, grade-string attendance) is RETIRED — this VM speaks the
 * typed DTOs directly so the UI renders the real roster + server-computed flags
 * (no client recomputation, no drift from the backend's computeFlags).
 *
 * Detail is fetched on demand (tap a card) and cached per assignmentId for the
 * session so re-opening is instant; pull-to-refresh reloads both planes.
 */
data class TeacherClassesState(
    // ── list plane ───────────────────────────────────────────────────────────
    val classes: List<TeacherClassSummaryDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // filter: null = all, true = class-teacher only, false = subject-only
    val classTeacherFilter: Boolean? = null,
    val search: String = "",
    // ── detail plane ───────────────────────────────────────────────────────────
    val openAssignmentId: String? = null,    // non-null = detail open
    val detail: ClassDetailData? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    // ── RA-51: message class parents broadcast composer ──────────────────────
    val broadcastClassName: String? = null,
    val broadcasting: Boolean = false,
    val broadcastError: String? = null,
    val broadcastResultCount: Int? = null,
) {
    /** The list after the active class-teacher filter + search query are applied. */
    val visibleClasses: List<TeacherClassSummaryDto>
        get() = classes.filter { c ->
            (classTeacherFilter == null || c.isClassTeacher == classTeacherFilter) &&
                (search.isBlank() || "${c.className} ${c.section} ${c.subject}"
                    .contains(search.trim(), ignoreCase = true))
        }
}

class TeacherClassesViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherClassesState())
    val state: StateFlow<TeacherClassesState> = _state.asStateFlow()

    // Session cache of composite details (instant re-open; refresh clears it).
    private val detailCache = mutableMapOf<String, ClassDetailData>()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val result = repository.listClassesV2(token)) {
                is NetworkResult.Success ->
                    _state.update {
                        it.copy(isLoading = false, classes = result.data.data?.classes ?: emptyList())
                    }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun refresh() {
        detailCache.clear()
        load()
    }

    // ---------------- filters ----------------

    fun setSearch(q: String) = _state.update { it.copy(search = q) }

    /** Toggle the class-teacher filter through (all → class teacher → subject → all). */
    fun cycleFilter() = _state.update {
        it.copy(
            classTeacherFilter = when (it.classTeacherFilter) {
                null -> true
                true -> false
                false -> null
            }
        )
    }

    fun setFilter(value: Boolean?) = _state.update { it.copy(classTeacherFilter = value) }

    // ---------------- detail ----------------

    fun openClass(assignmentId: String) {
        _state.update { it.copy(openAssignmentId = assignmentId, detailError = null) }
        detailCache[assignmentId]?.let { cached ->
            _state.update { it.copy(detail = cached, detailLoading = false) }
            return
        }
        _state.update { it.copy(detail = null, detailLoading = true) }
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(detailLoading = false, detailError = "Not authenticated") }
                return@launch
            }
            when (val r = repository.getClassDetailV2(token, assignmentId)) {
                is NetworkResult.Success -> {
                    val data = r.data.data
                    if (data != null) detailCache[assignmentId] = data
                    _state.update { it.copy(detailLoading = false, detail = data) }
                }
                is NetworkResult.Error -> _state.update { it.copy(detailLoading = false, detailError = r.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(detailLoading = false, detailError = "Connection error") }
            }
        }
    }

    fun retryDetail() {
        val id = _state.value.openAssignmentId ?: return
        detailCache.remove(id)
        openClass(id)
    }

    fun closeClass() = _state.update {
        it.copy(openAssignmentId = null, detail = null, detailLoading = false, detailError = null)
    }

    // ---------------- RA-51: message class parents ----------------

    fun openBroadcast(className: String) = _state.update {
        it.copy(broadcastClassName = className, broadcastError = null, broadcastResultCount = null)
    }

    fun closeBroadcast() = _state.update {
        it.copy(broadcastClassName = null, broadcasting = false, broadcastError = null, broadcastResultCount = null)
    }

    fun sendBroadcast(body: String) {
        val className = _state.value.broadcastClassName ?: return
        if (body.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(broadcasting = true, broadcastError = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(broadcasting = false, broadcastError = "Not authenticated") }
                return@launch
            }
            val req = TeacherClassBroadcastRequest(className = className, body = body.trim())
            when (val r = repository.broadcastToClass(token, req)) {
                is NetworkResult.Success ->
                    _state.update {
                        it.copy(broadcasting = false, broadcastResultCount = r.data.data?.recipients ?: 0)
                    }
                is NetworkResult.Error -> _state.update { it.copy(broadcasting = false, broadcastError = r.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(broadcasting = false, broadcastError = "Connection error") }
            }
        }
    }
}
