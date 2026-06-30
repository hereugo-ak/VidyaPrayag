package com.littlebridge.enrollplus.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.admin.domain.model.LinkRequestDto
import com.littlebridge.enrollplus.feature.admin.domain.repository.LinkRequestsRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * RA-48: drives the school-admin link-request queue
 * ([com.littlebridge.enrollplus.ui.v2.screens.school.LinkRequestsScreenV2]) off
 * the real GET /api/v1/school/link-requests + approve/reject endpoints. Every
 * request shown is scoped server-side to the admin's own school.
 */
/**
 * ISSUE 2d: the admin link queue is split into two tabs:
 *  • [PENDING] — clean full matches (name + class/section/roll + phone) waiting
 *    for a routine approve/reject.
 *  • [NEEDS_REVIEW] — partial matches where only the phone number mismatched.
 *    These are flagged (never silently dropped) so an admin can manually verify.
 */
enum class LinkRequestTab(val status: String) {
    PENDING("pending"),
    NEEDS_REVIEW("needs_review"),
}

data class LinkRequestsState(
    // The currently selected tab's requests.
    val requests: List<LinkRequestDto> = emptyList(),
    val tab: LinkRequestTab = LinkRequestTab.PENDING,
    // How many requests sit in the "needs review" bucket (drives the tab badge),
    // refreshed whenever that tab is loaded.
    val needsReviewCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    // ids currently being approved/rejected (per-row spinner / disable).
    val actingIds: Set<String> = emptySet(),
)

class LinkRequestsViewModel(
    private val repository: LinkRequestsRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LinkRequestsState())
    val state: StateFlow<LinkRequestsState> = _state.asStateFlow()

    init {
        load()
    }

    /** Switch tabs and (re)load that tab's queue. */
    fun selectTab(tab: LinkRequestTab) {
        if (_state.value.tab == tab) return
        _state.value = _state.value.copy(tab = tab, requests = emptyList())
        load()
    }

    fun load() {
        viewModelScope.launch {
            val tab = _state.value.tab
            _state.value = _state.value.copy(isLoading = true, error = null)
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val result = repository.getLinkRequests(token, tab.status)) {
                is NetworkResult.Success -> {
                    val list = result.data.data?.requests ?: emptyList()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        requests = list,
                        // Keep the needs-review badge in sync when we're on that tab.
                        needsReviewCount = if (tab == LinkRequestTab.NEEDS_REVIEW) list.size else _state.value.needsReviewCount,
                    )
                    // If we're on the pending tab, opportunistically refresh the
                    // needs-review count so the badge is accurate without a switch.
                    if (tab == LinkRequestTab.PENDING) refreshNeedsReviewCount(token)
                }
                is NetworkResult.Error -> {
                    AppLogger.e("LinkRequestsVM", "getLinkRequests error: ${result.message}")
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.ConnectionError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error. Check your internet.")
                }
            }
        }
    }

    /** Fetch just the count of needs-review requests to drive the tab badge. */
    private suspend fun refreshNeedsReviewCount(token: String) {
        when (val result = repository.getLinkRequests(token, LinkRequestTab.NEEDS_REVIEW.status)) {
            is NetworkResult.Success ->
                _state.value = _state.value.copy(needsReviewCount = result.data.data?.requests?.size ?: 0)
            else -> { /* leave the previous count on a transient failure */ }
        }
    }

    fun approve(id: String) = decide(id, approve = true)
    fun reject(id: String) = decide(id, approve = false)

    private fun decide(id: String, approve: Boolean) {
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            _state.value = _state.value.copy(actingIds = _state.value.actingIds + id)
            val result = if (approve) repository.approve(token, id) else repository.reject(token, id)
            _state.value = _state.value.copy(actingIds = _state.value.actingIds - id)
            when (result) {
                is NetworkResult.Success -> {
                    // Optimistically drop the decided row; it leaves the current queue.
                    val onNeedsReview = _state.value.tab == LinkRequestTab.NEEDS_REVIEW
                    _state.value = _state.value.copy(
                        requests = _state.value.requests.filterNot { it.id == id },
                        needsReviewCount = if (onNeedsReview)
                            (_state.value.needsReviewCount - 1).coerceAtLeast(0)
                        else _state.value.needsReviewCount,
                    )
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(error = result.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(error = "Connection error. Check your internet.")
            }
        }
    }
}
