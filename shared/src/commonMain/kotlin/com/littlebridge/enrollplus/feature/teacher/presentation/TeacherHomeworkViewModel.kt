package com.littlebridge.enrollplus.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssignHomeworkRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.GrantExtensionRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkAttachmentDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkBoardData
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkItemDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkSubmissionRowDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkSubmissionStatus
import com.littlebridge.enrollplus.feature.teacher.domain.model.ReviewSubmissionRequest
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.enrollplus.util.todayIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T-406 — the Planner › Homework state holder, rebuilt from scratch (DELETE-don't-patch)
 * on the typed homework lifecycle plane (T-405). Replaces the legacy holder whose Assign
 * button was dead (F-HW-1) and whose `getHomework()/create()` had no scope, no board, no
 * extensions.
 *
 * Two modes (Doc 08 §6–§8):
 *   • LIST  — this assignment's active homework, each with a live submission ratio + status
 *             counts; an "Assign new homework" composer (title/desc/due-date(+time)/allow-late).
 *   • BOARD — one homework's roster-joined submissions board (every enrolled student, even
 *             NOT-SUBMITTED — B-HW-3/H7), with status columns/counts, grant-extension
 *             (whole-class or single-student), per-row review/grade, and close.
 *
 * Reached PRE-SCOPED by [assignmentId] (X-1) — never a free-text class picker (contrast the
 * legacy create(classId, …)). All numbers come from the backend; nothing is fabricated.
 */

/** Which surface the screen is showing. */
enum class HomeworkMode { List, Board }

/** A homework row in the active list (UI projection of [HomeworkItemDto]). */
data class HomeworkSummary(
    val id: String,
    val title: String,
    val description: String,
    val className: String,
    val section: String,
    val subject: String,
    val dueDate: String,
    val dueTime: String?,
    val allowLate: Boolean,
    val isPastDue: Boolean,
    val submittedCount: Int,
    val lateCount: Int,
    val gradedCount: Int,
    val notSubmittedCount: Int,
    val totalCount: Int,
    val attachments: List<HomeworkAttachmentDto>,
) {
    /** submitted + late + graded over total; honest 0 when no roster. */
    val turnedInCount: Int get() = submittedCount + lateCount + gradedCount
    val turnedInRatio: Float get() = if (totalCount == 0) 0f else turnedInCount.toFloat() / totalCount
}

/** One row on the submissions board (UI projection of [HomeworkSubmissionRowDto]). */
data class HomeworkBoardRow(
    val studentId: String,
    val studentCode: String,
    val name: String,
    val rollNo: Int?,
    val status: String,
    val submittedAt: String?,
    val grade: String?,
    val hasExtension: Boolean,
    val extendedTo: String?,
)

/** The full board for one homework. */
data class HomeworkBoard(
    val homeworkId: String = "",
    val title: String = "",
    val className: String = "",
    val section: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val dueTime: String? = null,
    val allowLate: Boolean = false,
    val isPastDue: Boolean = false,
    val rows: List<HomeworkBoardRow> = emptyList(),
    val submittedCount: Int = 0,
    val lateCount: Int = 0,
    val gradedCount: Int = 0,
    val notSubmittedCount: Int = 0,
    val totalCount: Int = 0,
)

data class TeacherHomeworkState(
    val assignmentId: String = "",
    val scopeLabel: String = "",
    val mode: HomeworkMode = HomeworkMode.List,
    // ── list ──
    val items: List<HomeworkSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // ── assign composer ──
    val isComposerOpen: Boolean = false,
    val composerTitle: String = "",
    val composerDescription: String = "",
    val composerDueDate: String = "",
    val composerDueTime: String = "",
    val composerAllowLate: Boolean = false,
    val isAssigning: Boolean = false,
    val composerError: String? = null,
    // ── board ──
    val isBoardLoading: Boolean = false,
    val board: HomeworkBoard? = null,
    val boardError: String? = null,
    // A row currently being persisted (review/extend) — drives a row spinner / disables re-tap.
    val updatingStudentId: String? = null,
    // ── extension composer (board) ──
    val isExtensionOpen: Boolean = false,
    // null = whole class; else the single student we're extending for.
    val extensionStudentId: String? = null,
    val extensionStudentName: String = "",
    val extensionDate: String = "",
    val extensionTime: String = "",
    val extensionReason: String = "",
    val isGrantingExtension: Boolean = false,
    val extensionError: String? = null,
    // ── close confirm (board) ──
    val isClosing: Boolean = false,
) {
    val hasItems: Boolean get() = items.isNotEmpty()
    val canAssign: Boolean get() = composerTitle.isNotBlank() && composerDueDate.isNotBlank()
}

class TeacherHomeworkViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherHomeworkState())
    val state: StateFlow<TeacherHomeworkState> = _state.asStateFlow()

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    // ── LIST ──────────────────────────────────────────────────────────────────

    /** Load this assignment's active homework. The Planner host supplies the assignmentId. */
    fun load(assignmentId: String) {
        if (assignmentId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(assignmentId = assignmentId, isLoading = true, error = null) }
            val t = token() ?: run {
                _state.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }
            when (val result = repository.listHomework(t, assignmentId)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(isLoading = false, items = result.data.data.items.map { d -> d.toUi() }) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun retry() = load(_state.value.assignmentId)

    // ── ASSIGN composer ─────────────────────────────────────────────────────────

    fun openComposer() = _state.update {
        it.copy(
            isComposerOpen = true,
            composerTitle = "",
            composerDescription = "",
            composerDueDate = todayIso(),   // honest default: due today (teacher can move it forward)
            composerDueTime = "",
            composerAllowLate = false,
            composerError = null,
        )
    }

    fun closeComposer() = _state.update { it.copy(isComposerOpen = false, composerError = null) }

    fun setComposerTitle(v: String) = _state.update { it.copy(composerTitle = v, composerError = null) }
    fun setComposerDescription(v: String) = _state.update { it.copy(composerDescription = v) }
    fun setComposerDueDate(v: String) = _state.update { it.copy(composerDueDate = v, composerError = null) }
    fun setComposerDueTime(v: String) = _state.update { it.copy(composerDueTime = v) }
    fun setComposerAllowLate(v: Boolean) = _state.update { it.copy(composerAllowLate = v) }

    /** Assign homework via the typed POST. On success closes the composer + reloads the list. */
    fun assign() {
        val s0 = _state.value
        if (s0.assignmentId.isBlank()) return
        val title = s0.composerTitle.trim()
        if (title.isBlank()) {
            _state.update { it.copy(composerError = "Give the homework a title") }
            return
        }
        if (s0.composerDueDate.isBlank()) {
            _state.update { it.copy(composerError = "Pick a due date") }
            return
        }
        // H1: due date must be today or later (server is the authority; we pre-guard for UX).
        if (s0.composerDueDate < todayIso()) {
            _state.update { it.copy(composerError = "Due date can't be in the past") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAssigning = true, composerError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isAssigning = false, composerError = "Not authenticated") }
                return@launch
            }
            val request = AssignHomeworkRequest(
                assignmentId = s0.assignmentId,
                title = title,
                description = s0.composerDescription.trim(),
                dueDate = s0.composerDueDate,
                dueTime = s0.composerDueTime.takeIf { it.isNotBlank() },
                allowLate = s0.composerAllowLate,
            )
            when (val result = repository.assignHomework(t, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isAssigning = false, isComposerOpen = false) }
                    load(s0.assignmentId)
                }
                is NetworkResult.Error -> _state.update { it.copy(isAssigning = false, composerError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isAssigning = false, composerError = "Connection error") }
            }
        }
    }

    // ── BOARD ────────────────────────────────────────────────────────────────────

    /** Open the roster-joined submissions board for one homework. */
    fun openBoard(homeworkId: String) {
        val s0 = _state.value
        if (s0.assignmentId.isBlank() || homeworkId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(mode = HomeworkMode.Board, isBoardLoading = true, board = null, boardError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isBoardLoading = false, boardError = "Not authenticated") }
                return@launch
            }
            when (val result = repository.getHomeworkBoard(t, homeworkId, s0.assignmentId)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(isBoardLoading = false, board = result.data.data.toUi()) }
                is NetworkResult.Error -> _state.update { it.copy(isBoardLoading = false, boardError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isBoardLoading = false, boardError = "Connection error") }
            }
        }
    }

    /** Leave the board → back to the list (refresh so counts reflect any review/extend). */
    fun closeBoard() {
        val s0 = _state.value
        _state.update {
            it.copy(
                mode = HomeworkMode.List,
                board = null,
                boardError = null,
                isExtensionOpen = false,
                extensionStudentId = null,
            )
        }
        if (s0.assignmentId.isNotBlank()) load(s0.assignmentId)
    }

    private fun reloadBoard() {
        val b = _state.value.board ?: return
        openBoard(b.homeworkId)
    }

    // ── REVIEW (per-row) ──────────────────────────────────────────────────────────

    /**
     * Mark a student's submission reviewed/graded. Optimistically updates the row, then persists;
     * reverts to a full board reload on failure (authoritative counts).
     */
    fun reviewSubmission(studentId: String, status: String, grade: String? = null) {
        val s0 = _state.value
        val board = s0.board ?: return
        if (s0.assignmentId.isBlank()) return

        viewModelScope.launch {
            _state.update { st ->
                val b = st.board ?: return@update st
                st.copy(
                    updatingStudentId = studentId,
                    boardError = null,
                    board = b.copy(rows = b.rows.map { if (it.studentId == studentId) it.copy(status = status, grade = grade ?: it.grade) else it }),
                )
            }
            val t = token() ?: run {
                _state.update { it.copy(updatingStudentId = null, boardError = "Not authenticated") }
                reloadBoard()
                return@launch
            }
            val request = ReviewSubmissionRequest(assignmentId = s0.assignmentId, status = status, grade = grade)
            when (val result = repository.reviewHomeworkSubmission(t, board.homeworkId, studentId, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(updatingStudentId = null) }
                    reloadBoard()   // pull authoritative status counts
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(updatingStudentId = null, boardError = result.message) }
                    reloadBoard()
                }
                is NetworkResult.ConnectionError -> {
                    _state.update { it.copy(updatingStudentId = null, boardError = "Connection error") }
                    reloadBoard()
                }
            }
        }
    }

    // ── EXTENSION composer (board) ─────────────────────────────────────────────────

    /** Open the extension composer. studentId null = whole class (H5); else that one student (H4). */
    fun openExtension(studentId: String?, studentName: String = "") {
        val board = _state.value.board ?: return
        _state.update {
            it.copy(
                isExtensionOpen = true,
                extensionStudentId = studentId,
                extensionStudentName = studentName,
                // Default the new cutoff to the current due date so the picker starts somewhere sane.
                extensionDate = board.dueDate.ifBlank { todayIso() },
                extensionTime = board.dueTime ?: "",
                extensionReason = "",
                extensionError = null,
            )
        }
    }

    fun closeExtension() = _state.update { it.copy(isExtensionOpen = false, extensionError = null) }

    fun setExtensionDate(v: String) = _state.update { it.copy(extensionDate = v, extensionError = null) }
    fun setExtensionTime(v: String) = _state.update { it.copy(extensionTime = v) }
    fun setExtensionReason(v: String) = _state.update { it.copy(extensionReason = v) }

    /** Grant the extension via the typed POST. On success reloads the board (new cutoff/badges). */
    fun grantExtension() {
        val s0 = _state.value
        val board = s0.board ?: return
        if (s0.assignmentId.isBlank()) return
        if (s0.extensionDate.isBlank()) {
            _state.update { it.copy(extensionError = "Pick a new due date") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isGrantingExtension = true, extensionError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isGrantingExtension = false, extensionError = "Not authenticated") }
                return@launch
            }
            val request = GrantExtensionRequest(
                assignmentId = s0.assignmentId,
                studentId = s0.extensionStudentId,
                newDueDate = s0.extensionDate,
                newDueTime = s0.extensionTime.takeIf { it.isNotBlank() },
                reason = s0.extensionReason.takeIf { it.isNotBlank() },
            )
            when (val result = repository.grantHomeworkExtension(t, board.homeworkId, request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isGrantingExtension = false, isExtensionOpen = false, extensionStudentId = null) }
                    reloadBoard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isGrantingExtension = false, extensionError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isGrantingExtension = false, extensionError = "Connection error") }
            }
        }
    }

    // ── CLOSE ──────────────────────────────────────────────────────────────────────

    /** Close (archive) the open homework, then return to the list. */
    fun closeHomework() {
        val s0 = _state.value
        val board = s0.board ?: return
        if (s0.assignmentId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isClosing = true, boardError = null) }
            val t = token() ?: run {
                _state.update { it.copy(isClosing = false, boardError = "Not authenticated") }
                return@launch
            }
            when (val result = repository.closeHomework(t, board.homeworkId, s0.assignmentId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isClosing = false) }
                    closeBoard()
                }
                is NetworkResult.Error -> _state.update { it.copy(isClosing = false, boardError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isClosing = false, boardError = "Connection error") }
            }
        }
    }
}

// ── mappers ──────────────────────────────────────────────────────────────────────

private fun HomeworkItemDto.toUi() = HomeworkSummary(
    id = id,
    title = title,
    description = description,
    className = className,
    section = section,
    subject = subject,
    dueDate = dueDate,
    dueTime = dueTime,
    allowLate = allowLate,
    isPastDue = isPastDue,
    submittedCount = submittedCount,
    lateCount = lateCount,
    gradedCount = gradedCount,
    notSubmittedCount = notSubmittedCount,
    totalCount = totalCount,
    attachments = attachments,
)

private fun HomeworkBoardData.toUi() = HomeworkBoard(
    homeworkId = homeworkId,
    title = title,
    className = className,
    section = section,
    subject = subject,
    dueDate = dueDate,
    dueTime = dueTime,
    allowLate = allowLate,
    isPastDue = isPastDue,
    rows = rows.map { it.toUi() },
    submittedCount = submittedCount,
    lateCount = lateCount,
    gradedCount = gradedCount,
    notSubmittedCount = notSubmittedCount,
    totalCount = totalCount,
)

private fun HomeworkSubmissionRowDto.toUi() = HomeworkBoardRow(
    studentId = studentId,
    studentCode = studentCode,
    name = name,
    rollNo = rollNo,
    status = status,
    submittedAt = submittedAt,
    grade = grade,
    hasExtension = hasExtension,
    extendedTo = extendedTo,
)

// Re-export the status constants for screen-side `when` exhaustiveness without a second import.
// Must be `public` (not `internal`): they are consumed cross-module from :composeApp
// (TeacherHomeworkScreenV2). `internal` is module-scoped, which fails the Android build with
// "Cannot access 'val HOMEWORK_STATUS_*': it is internal in ...".
val HOMEWORK_STATUS_SUBMITTED = HomeworkSubmissionStatus.SUBMITTED
val HOMEWORK_STATUS_LATE = HomeworkSubmissionStatus.LATE
val HOMEWORK_STATUS_GRADED = HomeworkSubmissionStatus.GRADED
val HOMEWORK_STATUS_NOT_SUBMITTED = HomeworkSubmissionStatus.NOT_SUBMITTED
