/*
 * File: ParentNudgeViewModel.kt
 * Module: feature.pews.presentation
 *
 * Drives the gentle, opt-in PEWS nudge on the PARENT dashboard
 * (GET /api/v1/parent/pews/{childId}). This is the read-only end of the loop:
 *
 *   • The server returns show=false unless there is a REAL concern AND the
 *     school has parent_share_enabled — so the card simply never appears
 *     otherwise (no "risk" word, no score, no fabrication).
 *   • The copy is label-free and supportive; actions deep-link into existing
 *     parent surfaces (attendance / message teacher).
 *
 * Honesty (LAW 6): every field is a real value served by the parent-scoped
 * endpoint; we never invent a headline, a number, or an action.
 */
package com.littlebridge.enrollplus.feature.pews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentNudgeDto
import com.littlebridge.enrollplus.feature.pews.domain.repository.PewsRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ParentNudgeState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val nudge: PewsParentNudgeDto? = null,
) {
    /** The card renders only when the server says there's a real, shareable concern. */
    val visible: Boolean get() = nudge?.show == true
}

class ParentNudgeViewModel(
    private val repository: PewsRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ParentNudgeState())
    val state: StateFlow<ParentNudgeState> = _state.asStateFlow()

    private var lastChildId: String? = null

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()

    /**
     * Load (or re-load) the nudge for the active child. Called whenever the
     * parent switches child. A blank/duplicate child id is ignored.
     */
    fun load(childId: String?) {
        if (childId.isNullOrBlank()) {
            _state.value = ParentNudgeState()
            lastChildId = null
            return
        }
        if (childId == lastChildId && _state.value.nudge != null) return
        lastChildId = childId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val t = token()
            if (t.isNullOrBlank()) {
                // Non-fatal: a logged-out parent simply sees no nudge.
                _state.value = ParentNudgeState()
                return@launch
            }
            when (val r = repository.getParentNudge(t, childId)) {
                is NetworkResult.Success ->
                    _state.value = ParentNudgeState(isLoading = false, error = null, nudge = r.data.data)
                is NetworkResult.Error -> {
                    AppLogger.e("ParentNudgeVM", "getParentNudge error: ${r.message}")
                    // The nudge is a supportive extra — never break the dashboard on error.
                    _state.value = ParentNudgeState(isLoading = false, error = null, nudge = null)
                }
                is NetworkResult.ConnectionError ->
                    _state.value = ParentNudgeState(isLoading = false, error = null, nudge = null)
            }
        }
    }
}
