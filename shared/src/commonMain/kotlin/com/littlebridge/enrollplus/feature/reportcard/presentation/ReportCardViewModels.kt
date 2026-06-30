/*
 * File: ReportCardViewModels.kt
 * Module: feature.reportcard.presentation
 *
 * ViewModels for AI Report Card 2.0 screens:
 *   - TeacherReportReviewViewModel  (teacher review queue + approve/regenerate)
 *   - TeacherReportDraftEditorViewModel (teacher draft editing)
 *   - AdminReportPublishViewModel   (admin publish + oversight)
 *   - AdminReportEffectivenessViewModel (admin learn/effectiveness)
 *   - ParentReportViewModel         (parent published reports + conference pack)
 */
package com.littlebridge.enrollplus.feature.reportcard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels
import com.littlebridge.enrollplus.feature.reportcard.domain.repository.ReportCardRepository
import com.littlebridge.enrollplus.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ── Teacher: Review Queue ────────────────────────────────────────────────

data class TeacherReportReviewState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val drafts: List<ReportCardModels.DraftDto> = emptyList(),
    val className: String = "",
    val section: String = "A",
    val term: String = "",
    val approvingId: String? = null,
    val regeneratingId: String? = null,
    val message: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && drafts.isEmpty()
}

class TeacherReportReviewViewModel(
    private val repository: ReportCardRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherReportReviewState())
    val state: StateFlow<TeacherReportReviewState> = _state.asStateFlow()

    fun loadReviewQueue(className: String, section: String, term: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, className = className, section = section, term = term)
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val r = repository.getReviewQueue(t, className, section, term)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(isLoading = false, drafts = r.data.data ?: emptyList())
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
            }
        }
    }

    fun approveDraft(draftId: String) {
        _state.value = _state.value.copy(approvingId = draftId)
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.approveDraft(t, draftId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(approvingId = null, message = "Draft approved")
                    loadReviewQueue(_state.value.className, _state.value.section, _state.value.term)
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(approvingId = null, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(approvingId = null, error = "Connection error")
            }
        }
    }

    fun regenerateDraft(draftId: String) {
        _state.value = _state.value.copy(regeneratingId = draftId)
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.regenerateDraft(t, draftId)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(regeneratingId = null, message = "Draft regenerated")
                    loadReviewQueue(_state.value.className, _state.value.section, _state.value.term)
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(regeneratingId = null, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(regeneratingId = null, error = "Connection error")
            }
        }
    }

    fun bulkApprove(draftIds: List<String>) {
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.bulkApprove(t, draftIds)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(message = "${r.data.data?.approved ?: 0} drafts approved")
                    loadReviewQueue(_state.value.className, _state.value.section, _state.value.term)
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(error = "Connection error")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()
}

// ── Teacher: Draft Editor ────────────────────────────────────────────────

data class TeacherReportDraftEditorState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val draft: ReportCardModels.DraftDto? = null,
    val editedContent: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class TeacherReportDraftEditorViewModel(
    private val repository: ReportCardRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherReportDraftEditorState())
    val state: StateFlow<TeacherReportDraftEditorState> = _state.asStateFlow()

    fun loadDraft(draftId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val r = repository.getDraft(t, draftId)) {
                is NetworkResult.Success -> {
                    val d = r.data.data
                    _state.value = _state.value.copy(
                        isLoading = false,
                        draft = d,
                        editedContent = d?.aiDraft ?: "",
                    )
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
            }
        }
    }

    fun updateContent(content: String) {
        _state.value = _state.value.copy(editedContent = content, saved = false)
    }

    fun saveDraft() {
        val draftId = _state.value.draft?.id ?: return
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.editDraft(t, draftId, _state.value.editedContent)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(saving = false, saved = true)
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(saving = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(saving = false, error = "Connection error")
            }
        }
    }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()
}

// ── Admin: Publish + Oversight ───────────────────────────────────────────

data class AdminReportPublishState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val oversight: ReportCardModels.OversightSummary? = null,
    val publishing: Boolean = false,
    val publishedCount: Int? = null,
    val term: String = "",
    val termConfig: ReportCardModels.TermConfig? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && oversight == null
}

class AdminReportPublishViewModel(
    private val repository: ReportCardRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminReportPublishState())
    val state: StateFlow<AdminReportPublishState> = _state.asStateFlow()

    fun loadOversight(term: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, term = term)
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val r = repository.getOversight(t, term)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(isLoading = false, oversight = r.data.data)
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
            }
        }
    }

    fun publishClass(className: String, section: String, term: String) {
        _state.value = _state.value.copy(publishing = true)
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.publishClass(t, ReportCardModels.PublishRequest(className, section, term))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(publishing = false, publishedCount = r.data.data?.published)
                    loadOversight(term)
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(publishing = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(publishing = false, error = "Connection error")
            }
        }
    }

    fun loadTermConfig() {
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.getTermConfig(t)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(termConfig = r.data.data)
                else -> {}
            }
        }
    }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()
}

// ── Admin: Effectiveness (Learn) ─────────────────────────────────────────

data class AdminReportEffectivenessState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val effectiveness: List<ReportCardModels.EffectivenessReport> = emptyList(),
    val projectionAccuracy: ReportCardModels.ProjectionAccuracy? = null,
    val patterns: ReportCardModels.CohortPatternReport? = null,
    val runningFlywheel: Boolean = false,
    val flywheelResult: List<ReportCardModels.EffectivenessReport>? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && effectiveness.isEmpty()
}

class AdminReportEffectivenessViewModel(
    private val repository: ReportCardRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminReportEffectivenessState())
    val state: StateFlow<AdminReportEffectivenessState> = _state.asStateFlow()

    fun loadEffectiveness() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val r = repository.getEffectiveness(t)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(isLoading = false, effectiveness = r.data.data ?: emptyList())
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
            }
        }
    }

    fun runFlywheel(currentTerm: String, previousTerm: String) {
        _state.value = _state.value.copy(runningFlywheel = true)
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.runFlywheel(t, currentTerm, previousTerm)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(runningFlywheel = false, flywheelResult = r.data.data)
                    loadEffectiveness()
                }
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(runningFlywheel = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(runningFlywheel = false, error = "Connection error")
            }
        }
    }

    fun loadPatterns(term: String) {
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.getPatterns(t, term)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(patterns = r.data.data)
                else -> {}
            }
        }
    }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()
}

// ── Parent: Report Screen ────────────────────────────────────────────────

data class ParentReportState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val reports: List<ReportCardModels.ParentReport> = emptyList(),
    val conferencePack: ReportCardModels.ConferencePack? = null,
    val selectedReport: ReportCardModels.ParentReport? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && reports.isEmpty()
}

class ParentReportViewModel(
    private val repository: ReportCardRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ParentReportState())
    val state: StateFlow<ParentReportState> = _state.asStateFlow()

    fun loadReports(childId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val t = token() ?: run {
                _state.value = _state.value.copy(isLoading = false, error = "Not signed in")
                return@launch
            }
            when (val r = repository.getPublishedReports(t, childId)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(isLoading = false, reports = r.data.data ?: emptyList())
                is NetworkResult.Error ->
                    _state.value = _state.value.copy(isLoading = false, error = r.message)
                is NetworkResult.ConnectionError ->
                    _state.value = _state.value.copy(isLoading = false, error = "Connection error")
            }
        }
    }

    fun loadConferencePack(childId: String) {
        viewModelScope.launch {
            val t = token() ?: return@launch
            when (val r = repository.getConferencePack(t, childId)) {
                is NetworkResult.Success ->
                    _state.value = _state.value.copy(conferencePack = r.data.data)
                else -> {}
            }
        }
    }

    fun selectReport(report: ReportCardModels.ParentReport) {
        _state.value = _state.value.copy(selectedReport = report)
    }

    private suspend fun token(): String? = preferenceRepository.getUserToken().first()
}
