package com.littlebridge.vidyaprayag.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.domain.model.LinkChildRequest
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A school match surfaced in step 2 of the link wizard. */
data class SchoolMatch(
    val id: String,
    val name: String,
    val board: String,
    val city: String,
    val logoUrl: String? = null,
)

/** The child resolved + linked in step 3. */
data class LinkedChild(
    val childId: String,
    val childName: String,
    val className: String,
    val roll: String,
    val schoolName: String,
    val profilePhotoUrl: String? = null,
)

data class LinkChildState(
    val fullName: String = "",
    val language: String = "English",
    val schoolQuery: String = "",
    val rollNumber: String = "",
    // Step 2 — school search
    val isSearching: Boolean = false,
    val matches: List<SchoolMatch> = emptyList(),
    val selectedSchool: SchoolMatch? = null,
    val searchError: String? = null,
    // Step 3 — link result
    val isLinking: Boolean = false,
    val linkError: String? = null,
    val linkedChild: LinkedChild? = null,
)

/**
 * LinkChildViewModel — drives [ParentLinkChildScreenV2] off the real
 * `GET /api/v1/parent/schools/search` and `POST /api/v1/parent/link-child`
 * endpoints (report §5.3, SWEEP-A). Replaces MockV2.childForParent / MockV2.school.
 */
class LinkChildViewModel(
    private val repository: ParentRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LinkChildState())
    val state: StateFlow<LinkChildState> = _state.asStateFlow()

    fun onFullNameChange(v: String) = _state.update { it.copy(fullName = v) }
    fun onLanguageChange(v: String) = _state.update { it.copy(language = v) }
    fun onRollNumberChange(v: String) = _state.update { it.copy(rollNumber = v, linkError = null) }

    fun onSchoolQueryChange(v: String) {
        _state.update { it.copy(schoolQuery = v, selectedSchool = null) }
    }

    fun selectSchool(match: SchoolMatch) {
        _state.update { it.copy(selectedSchool = match) }
    }

    /** Run the school search for the current query (called on the step-2 search action). */
    fun searchSchools() {
        val query = _state.value.schoolQuery.trim()
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isSearching = false, searchError = "Not signed in") }
                return@launch
            }
            when (val result = repository.searchSchools(token, query)) {
                is NetworkResult.Success -> {
                    val schools = result.data.data.schools.map {
                        SchoolMatch(it.id, it.name, it.board, it.city, it.logoUrl)
                    }
                    _state.update {
                        it.copy(
                            isSearching = false,
                            matches = schools,
                            // Auto-select the single best match so the wizard's "Match" card lights up.
                            selectedSchool = schools.firstOrNull(),
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isSearching = false, searchError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isSearching = false, searchError = "Connection error") }
            }
        }
    }

    /** Link the child by roll number against the selected school (step 3). [onSuccess] opens the dashboard. */
    fun linkChild(onSuccess: () -> Unit) {
        val s = _state.value
        val school = s.selectedSchool
        if (school == null) {
            _state.update { it.copy(linkError = "Please select your child's school first") }
            return
        }
        if (s.rollNumber.isBlank()) {
            _state.update { it.copy(linkError = "Roll / admission number is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLinking = true, linkError = null) }
            val token = preferenceRepository.getUserToken().first()
            if (token == null) {
                _state.update { it.copy(isLinking = false, linkError = "Not signed in") }
                return@launch
            }
            val request = LinkChildRequest(
                schoolId = school.id,
                rollNumber = s.rollNumber.trim(),
                parentName = s.fullName.takeIf { it.isNotBlank() },
            )
            when (val result = repository.linkChild(token, request)) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    _state.update {
                        it.copy(
                            isLinking = false,
                            linkedChild = LinkedChild(
                                childId = d.childId,
                                childName = d.childName,
                                className = d.className,
                                roll = d.roll,
                                schoolName = d.schoolName,
                                profilePhotoUrl = d.profilePhotoUrl,
                            ),
                        )
                    }
                    onSuccess()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLinking = false, linkError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLinking = false, linkError = "Connection error") }
            }
        }
    }
}
