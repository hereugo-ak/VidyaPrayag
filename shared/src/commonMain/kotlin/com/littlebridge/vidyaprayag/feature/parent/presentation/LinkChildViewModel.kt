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
    // RA-48: "pending" until a school admin approves; "approved" once linked.
    val status: String = "approved",
)

data class LinkChildState(
    val fullName: String = "",
    val language: String = "English",
    val schoolQuery: String = "",
    val rollNumber: String = "",
    // Step 3 — ISSUE 2c guided inputs (school already chosen).
    val childName: String = "",
    val className: String = "",
    val section: String = "",
    val parentPhone: String = "",
    // Step 2 — school search
    val isSearching: Boolean = false,
    val matches: List<SchoolMatch> = emptyList(),
    val selectedSchool: SchoolMatch? = null,
    val searchError: String? = null,
    // Step 3 — link result
    val isLinking: Boolean = false,
    val linkError: String? = null,
    val linkedChild: LinkedChild? = null,
    // RA-48: true once a pending link request has been submitted for admin
    // approval (no child is on the dashboard yet — show the "awaiting approval"
    // confirmation instead of routing into the dashboard).
    val linkPending: Boolean = false,
    // ISSUE 2d: true when the request landed in the admin "needs review" bucket
    // (e.g. a phone mismatch) so the wizard can show a distinct message.
    val linkNeedsReview: Boolean = false,
) {
    /**
     * ISSUE 2c: step-3 is valid when the core matching fields are filled.
     * Parent phone is OPTIONAL — if blank, the backend will match by name+class+roll
     * and treat it as a normal pending request (not needs_review). A parent who
     * doesn't know their phone or is on a shared number is not blocked.
     */
    val step3Valid: Boolean
        get() = childName.isNotBlank() && className.isNotBlank() && rollNumber.isNotBlank()
}

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

    // ─── ISSUE 2c: guided, real-time-formatted, auto-correcting step-3 inputs ───

    /** Child's name — just trims leading space; kept as typed otherwise. */
    fun onChildNameChange(v: String) = _state.update { it.copy(childName = v.trimStart(), linkError = null) }

    /**
     * Class — auto-corrects as the parent types:
     *   • collapses runs of whitespace, strips an accidental section suffix
     *     (e.g. "4 A"/"4-A" moves the "A" into the Section field),
     *   • upper-cases a trailing roman numeral / keeps digits.
     * It is intentionally permissive (we don't reject), only tidy.
     */
    fun onClassNameChange(v: String) {
        val cleaned = v.replace(Regex("\\s+"), " ").trimStart()
        // If the parent typed "<class> <SECTION>" with a single trailing letter,
        // peel that letter into the Section field automatically.
        val m = Regex("^(.*?)[\\s-]+([A-Za-z])\\s*$").find(cleaned)
        if (m != null && m.groupValues[1].any { it.isDigit() }) {
            val cls = m.groupValues[1].trim()
            val sec = m.groupValues[2].uppercase()
            _state.update { it.copy(className = cls, section = sec, linkError = null) }
        } else {
            _state.update { it.copy(className = cleaned, linkError = null) }
        }
    }

    /** Section — single upper-case letter/short token; auto-upper, max 4 chars. */
    fun onSectionChange(v: String) {
        val cleaned = v.filter { it.isLetterOrDigit() }.uppercase().take(4)
        _state.update { it.copy(section = cleaned, linkError = null) }
    }

    /**
     * Roll — keeps digits + alnum; numeric rolls are left as typed (the server
     * normalises '001' ≡ '1'), capped to a sane length.
     */
    fun onRollNumberChange(v: String) {
        val cleaned = v.filter { it.isLetterOrDigit() || it == '-' }.take(16)
        _state.update { it.copy(rollNumber = cleaned, linkError = null) }
    }

    /** Parent phone — digits + a single leading '+', formatting separators kept. */
    fun onParentPhoneChange(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }.take(18)
        _state.update { it.copy(parentPhone = cleaned, linkError = null) }
    }

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
                            // ROOT FIX (link failed "No student found"): previously we
                            // auto-selected schools.firstOrNull() — the alphabetically
                            // FIRST result — and the result cards were not tappable, so
                            // a parent whose child is at the SECOND/THIRD match was
                            // silently locked onto the wrong school and EVERY roll
                            // lookup then 404'd against the wrong school's roster.
                            // Now we ONLY auto-select when the search returns exactly one
                            // school; with multiple results the parent must tap to choose
                            // (the cards are now clickable — see ParentLinkChildScreenV2).
                            // Preserve a prior valid selection if it is still in results.
                            selectedSchool = when {
                                schools.size == 1 -> schools.first()
                                it.selectedSchool != null &&
                                    schools.any { s -> s.id == it.selectedSchool.id } -> it.selectedSchool
                                else -> null
                            },
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isSearching = false, searchError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isSearching = false, searchError = "Connection error") }
            }
        }
    }

    /**
     * Link the child by roll number against the selected school (step 3).
     *
     * RA-48: the server now returns either an APPROVED link (legacy/auto path,
     * carries a real child_id) or a PENDING request that a school admin must
     * approve. [onSuccess] (route into the dashboard) is invoked ONLY for an
     * approved link; for a pending request we surface [LinkChildState.linkPending]
     * so the wizard can show an "awaiting approval" confirmation instead.
     */
    fun linkChild(onSuccess: () -> Unit) {
        val s = _state.value
        val school = s.selectedSchool
        if (school == null) {
            _state.update { it.copy(linkError = "Please select your child's school first") }
            return
        }
        // ISSUE 2c: every guided field is required for a precise match.
        if (s.childName.isBlank()) {
            _state.update { it.copy(linkError = "Please enter your child's name") }
            return
        }
        if (s.className.isBlank()) {
            _state.update { it.copy(linkError = "Please enter your child's class") }
            return
        }
        if (s.rollNumber.isBlank()) {
            _state.update { it.copy(linkError = "Roll / admission number is required") }
            return
        }
        // Phone is optional — only validate when the parent actually entered something.
        if (s.parentPhone.isNotBlank() && s.parentPhone.filter { it.isDigit() }.length < 10) {
            _state.update { it.copy(linkError = "That phone number doesn't look right — enter at least 10 digits, or leave it blank") }
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
                className = s.className.trim().takeIf { it.isNotBlank() },
                section = s.section.trim().takeIf { it.isNotBlank() },
                childName = s.childName.trim().takeIf { it.isNotBlank() },
                parentPhone = s.parentPhone.trim().takeIf { it.isNotBlank() },
                parentName = s.fullName.takeIf { it.isNotBlank() },
            )
            when (val result = repository.linkChild(token, request)) {
                is NetworkResult.Success -> {
                    val d = result.data.data
                    // pending OR needs_review both leave the parent on the
                    // confirmation screen awaiting an admin decision.
                    val isPending = d.status == "pending"
                    val needsReview = d.status == "needs_review"
                    _state.update {
                        it.copy(
                            isLinking = false,
                            linkPending = isPending || needsReview,
                            linkNeedsReview = needsReview,
                            linkedChild = LinkedChild(
                                childId = d.childId,
                                childName = d.childName,
                                className = d.className,
                                roll = d.roll,
                                schoolName = d.schoolName,
                                profilePhotoUrl = d.profilePhotoUrl,
                                status = d.status,
                            ),
                        )
                    }
                    // Only route into the dashboard for an APPROVED link; a pending
                    // or needs-review request leaves the parent on the confirmation
                    // screen until a school admin acts.
                    if (!isPending && !needsReview) onSuccess()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLinking = false, linkError = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLinking = false, linkError = "Connection error") }
            }
        }
    }
}
