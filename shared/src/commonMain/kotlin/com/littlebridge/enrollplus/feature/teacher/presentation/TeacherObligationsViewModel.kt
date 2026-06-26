package com.littlebridge.vidyaprayag.feature.teacher.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * TeacherObligationsViewModel — backs the Today "what needs me" strip (T-107,
 * Doc 04 §5.5). It owns ONLY the transport for the REAL obligations aggregate
 * (GET /api/v1/teacher/obligations), replacing the fabricated Today tasks
 * (B-HOME-4).
 *
 * Design rules honored:
 *  - **Honesty** (Doc 04 §5.5): [TeacherObligationsState.isAllCaughtUp] is true
 *    ONLY when every count is genuinely zero AND the load succeeded; a failed or
 *    in-flight load is never mistaken for "all caught up".
 *  - **Server-authoritative counts**: counts come straight from the server
 *    aggregate (scoped to the teacher's allocation at the query level); the
 *    client never invents or floors them.
 *  - **Refreshable**: [load] is public so the strip can be refreshed after an
 *    action that changes the counts (e.g. returning from marking attendance, a
 *    leave decision). Kept separate from the schedule VM so a strip refresh
 *    doesn't re-fetch the whole resolved day.
 */
data class TeacherObligationsState(
    val isLoading: Boolean = false,
    val loaded: Boolean = false,           // a successful load has completed at least once
    val unmarkedClasses: Int = 0,
    val classesTodayTotal: Int = 0,
    val unpublishedResults: Int = 0,
    val submissionsToReview: Int = 0,
    val pendingLeaveDecisions: Int = 0,
    val items: List<ObligationItemDto> = emptyList(),
    // Honest "we couldn't read it": the strip hides rather than pretending
    // everything is done (Doc 04 §5.5 — never fabricate "all caught up").
    val unavailable: Boolean = false,
) {
    /** True only when the load succeeded AND there is genuinely nothing outstanding. */
    val isAllCaughtUp: Boolean
        get() = loaded && !unavailable &&
            items.isEmpty() &&
            unmarkedClasses == 0 && unpublishedResults == 0 &&
            submissionsToReview == 0 && pendingLeaveDecisions == 0

    /** Total outstanding count — feeds the dock badge (F-SHELL-4). */
    val totalOutstanding: Int
        get() = unmarkedClasses + unpublishedResults + submissionsToReview + pendingLeaveDecisions
}

class TeacherObligationsViewModel(
    private val repository: TeacherRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherObligationsState())
    val state: StateFlow<TeacherObligationsState> = _state.asStateFlow()

    init {
        load()
    }

    /** Fetch the real obligations aggregate. Public so the strip can refresh. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, unavailable = false) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLoading = false, unavailable = true) }
                return@launch
            }
            when (val result = repository.getObligations(token)) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loaded = true,
                            unmarkedClasses = d.unmarkedClasses,
                            classesTodayTotal = d.classesTodayTotal,
                            unpublishedResults = d.unpublishedResults,
                            submissionsToReview = d.submissionsToReview,
                            pendingLeaveDecisions = d.pendingLeaveDecisions,
                            items = d.items,
                            unavailable = false,
                        )
                    }
                }
                // A read failure is non-fatal and must NOT read as "all caught up":
                // surface the unavailable flag so the strip simply hides.
                is NetworkResult.Error, is NetworkResult.ConnectionError ->
                    _state.update { it.copy(isLoading = false, unavailable = true) }
            }
        }
    }
}
