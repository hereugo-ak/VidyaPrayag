package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AttendanceSaveMarkDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AttendanceSaveRequest
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TeacherAttendanceViewModel — T-205 (Doc 06 §3, Doc 10 §6.3).
 *
 * The clean rebuild of the attendance plane's state holder. It is **assignment-scoped**
 * (never class_id+grade), loads the typed enrollment roster via
 * [TeacherRepository.loadAttendance], pre-applies approved-leave defaults from the
 * server, supports the 4-state space (present · absent · late · leave, D-ATT-1),
 * bulk "mark all present", an enabled/correctable date (fixes F-ATT-2), a live
 * running counter, the load-for-EDIT audit ("last marked by … at …", E3), and a
 * result-driven save ([isSaving]/[saveSuccess]/[saveError]) that never auto-publishes.
 *
 * The screen reaches this PRE-SCOPED from a Today/Classes CTA — there is no in-VM
 * class picker (F-ATT-1). Scope is shown back to the teacher via [scope] (the
 * wrong-class guard header, E15) and re-confirmed from the server load.
 */

/** The 4-state attendance space (Doc 06 §3.4 — adds `leave`, D-ATT-1). */
object AttendanceStatus {
    const val PRESENT = "present"
    const val ABSENT = "absent"
    const val LATE = "late"
    const val LEAVE = "leave"

    val ALL = listOf(PRESENT, ABSENT, LATE, LEAVE)
}

/** Origin of a student's current status, surfaced so the UI can badge auto values. */
object AttendanceSource {
    const val MANUAL = "manual"
    const val LEAVE_AUTO = "leave_auto"
    const val BULK = "bulk"
    const val BIOMETRIC = "biometric"
}

data class StudentAttendance(
    val studentId: String,
    val name: String,
    val rollNo: String,
    val status: String,
    /** manual | leave_auto | bulk | biometric — null when freshly defaulted. */
    val source: String? = null,
    val enrollmentId: String? = null,
) {
    /** True when this student is on an approved leave (pre-defaulted server-side, §3.5). */
    val isOnApprovedLeave: Boolean
        get() = source == AttendanceSource.LEAVE_AUTO
}

data class TeacherAttendanceState(
    // Scope (the wrong-class guard header, E15) — all server-resolved from the assignment.
    val assignmentId: String = "",
    val scope: String = "",
    val className: String = "",
    val section: String = "",
    val subject: String = "",
    val date: String = "",
    val students: List<StudentAttendance> = emptyList(),
    // Load-for-EDIT audit (E3): true when marks already exist for (school,date,assignment).
    val alreadyMarked: Boolean = false,
    val lastMarkedBy: String? = null,
    val lastMarkedAt: String? = null,
    // Edge-case flags from the server (§4). The date picker stays enabled (F-ATT-2);
    // back-date window is advertised so the UI can warn before a blocked save (E9).
    val isHoliday: Boolean = false,
    val holidayName: String? = null,
    val isCancelled: Boolean = false,
    val backDateWindowDays: Int = 7,
    // Transport / result states.
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    // RA-S18: load errors drive VStateHost (full-screen retry); save errors are surfaced
    // inline so a failed save never wipes the roster the teacher is mid-way through.
    val error: String? = null,      // load-path error (VStateHost)
    val saveError: String? = null,  // save-path error (inline)
) {
    val presentCount: Int get() = students.count { it.status == AttendanceStatus.PRESENT }
    val absentCount: Int get() = students.count { it.status == AttendanceStatus.ABSENT }
    val lateCount: Int get() = students.count { it.status == AttendanceStatus.LATE }
    val leaveCount: Int get() = students.count { it.status == AttendanceStatus.LEAVE }
    val total: Int get() = students.size

    /** A freshly defaulted "present" (no source) is "unmarked" for the running counter. */
    val unmarkedCount: Int
        get() = students.count { it.status == AttendanceStatus.PRESENT && it.source == null }
}

class TeacherAttendanceViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherAttendanceState())
    val state: StateFlow<TeacherAttendanceState> = _state.asStateFlow()

    /**
     * Load the typed roster for an authorizing [assignmentId] on [date]. A blank
     * [date] lets the server default to today (real `date`, server-resolved). The
     * screen supplies the pre-authorized assignment id (Doc 05 binding).
     */
    fun load(assignmentId: String, date: String? = null) {
        if (assignmentId.isBlank()) {
            _state.update { it.copy(isLoading = false, students = emptyList(), assignmentId = "") }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    assignmentId = assignmentId,
                    isLoading = true,
                    error = null,
                    saveSuccess = false,
                    saveError = null,
                )
            }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val result = repository.loadAttendance(token, assignmentId, date?.takeIf { d -> d.isNotBlank() })) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            assignmentId = d.assignmentId,
                            scope = d.scope,
                            className = d.className,
                            section = d.section,
                            subject = d.subject,
                            date = d.date,
                            students = d.students.map { s ->
                                StudentAttendance(
                                    studentId = s.studentId,
                                    name = s.name,
                                    rollNo = s.rollNo,
                                    status = s.status,
                                    source = s.source,
                                    enrollmentId = s.enrollmentId,
                                )
                            },
                            alreadyMarked = d.alreadyMarked,
                            lastMarkedBy = d.lastMarkedBy,
                            lastMarkedAt = d.lastMarkedAt,
                            isHoliday = d.isHoliday,
                            holidayName = d.holidayName,
                            isCancelled = d.isCancelled,
                            backDateWindowDays = d.backDateWindowDays,
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    /** Reload the current assignment for a different (correctable) date — the enabled picker (F-ATT-2). */
    fun changeDate(date: String) {
        val asg = _state.value.assignmentId
        if (asg.isNotBlank()) load(asg, date)
    }

    /** Retry the current load (VStateHost error → Retry). */
    fun retry() {
        val s = _state.value
        if (s.assignmentId.isNotBlank()) load(s.assignmentId, s.date.takeIf { it.isNotBlank() })
    }

    /** Local edit — set one student's status. Marks the source `manual` (an explicit teacher choice). */
    fun setStatus(studentId: String, status: String) {
        if (status !in AttendanceStatus.ALL) return
        _state.update { s ->
            s.copy(
                students = s.students.map {
                    if (it.studentId == studentId) it.copy(status = status, source = AttendanceSource.MANUAL) else it
                },
                // RA-S18: editing after a confirmed save re-enables the result-driven Save button.
                saveSuccess = false,
            )
        }
    }

    /**
     * Bulk "Mark all present" (Mr. Rao's 40-student reality, §3.6). Sets every student
     * NOT already explicitly marked to present (source=bulk); leaves manual overrides
     * AND approved-leave defaults intact so the teacher's exceptions survive.
     */
    fun markAllPresent() {
        _state.update { s ->
            s.copy(
                students = s.students.map {
                    val keep = it.source == AttendanceSource.MANUAL || it.isOnApprovedLeave
                    if (keep) it else it.copy(status = AttendanceStatus.PRESENT, source = AttendanceSource.BULK)
                },
                saveSuccess = false,
            )
        }
    }

    /** Result-driven save (§3.7). Upserts via the typed endpoint; never auto-publishes. */
    fun save() {
        viewModelScope.launch {
            val current = _state.value
            if (current.assignmentId.isBlank() || current.students.isEmpty()) return@launch
            _state.update { it.copy(isSaving = true, saveError = null, saveSuccess = false) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isSaving = false, saveError = "Not authenticated") }
                return@launch
            }
            val request = AttendanceSaveRequest(
                assignmentId = current.assignmentId,
                date = current.date,
                marks = current.students.map { AttendanceSaveMarkDto(it.studentId, it.status) },
            )
            when (val result = repository.saveAttendance(token, request)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(isSaving = false, saveSuccess = true, alreadyMarked = true) }
                is NetworkResult.Error ->
                    _state.update { it.copy(isSaving = false, saveError = result.message) }
                is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isSaving = false, saveError = "Connection error") }
            }
        }
    }
}
