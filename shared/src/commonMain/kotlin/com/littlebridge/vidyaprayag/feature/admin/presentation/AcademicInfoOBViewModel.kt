package com.littlebridge.vidyaprayag.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ObStepType
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingSubmitRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.OnboardingRepository
import com.littlebridge.vidyaprayag.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class Subject(
    val id: String,
    val name: String,
    val teacherName: String? = null,
    val teacherImageUrl: String? = null,
    val iconName: String
)

data class AcademicInfoState(
    val selectedClass: String = "Class 8",
    val availableClasses: List<String> = DEFAULT_CLASSES,
    val subjects: List<Subject> = DEFAULT_SUBJECTS,
    val syncActive: Boolean = true,
    val curriculumPrecision: Int = 92
) {
    companion object {
        val DEFAULT_CLASSES = listOf(
            "Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6"
        )
        // Kept as fallback when the backend returns no subjects yet (e.g. a
        // brand-new tenant). The UI looks empty otherwise.
        val DEFAULT_SUBJECTS = listOf(
            Subject(
                "1", "Mathematics",
                "Dr. Arpita Sharma",
                null,
                "functions"
            ),
            Subject("2", "Science", null, null, "science"),
            Subject(
                "3", "History",
                "Prof. Julian V.",
                null,
                "history_edu"
            )
        )
    }
}

/**
 * ViewModel for the **third** onboarding step: Academic Structure.
 *
 * - On init, loads the active class list via GET /api/v1/onboarding/step?obStepType=ACADEMIC.
 *   The server returns an empty list for fresh tenants (school not created yet),
 *   in which case we keep the hardcoded defaults so the user still sees something.
 * - When the user picks a class, optionally hits GET /academic/class-details to
 *   populate real subject + teacher info.
 * - On Continue, POSTs to /api/v1/onboarding/submit with ob_step_type = "ACADEMIC".
 *   The ACADEMIC step has no required body fields in the server schema today, but
 *   submitting is what allows the server to move the user to REVIEW.
 */
class AcademicInfoOBViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AcademicInfoState())
    val state: StateFlow<AcademicInfoState> = _state.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Maps human-readable class name shown in the UI -> server class code,
    // populated when /step?obStepType=ACADEMIC returns a non-empty list.
    private val classCodeByName = mutableMapOf<String, String>()

    init {
        loadAcademicStep()
    }

    /**
     * Fetch the live class list from the backend. Falls back silently to the
     * defaults if the user hasn't created a school yet (the server returns an
     * empty list in that case).
     */
    private fun loadAcademicStep() {
        viewModelScope.launch {
            _isLoading.value = true
            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _isLoading.value = false
                return@launch
            }

            when (val result = onboardingRepository.getStep(token, ObStepType.ACADEMIC)) {
                is NetworkResult.Success -> {
                    val classes = result.data.listOfActiveClasses.orEmpty()
                    if (classes.isNotEmpty()) {
                        classCodeByName.clear()
                        classes.forEach { classCodeByName[it.name] = it.id }
                        val names = classes.map { it.name }
                        _state.value = _state.value.copy(
                            availableClasses = names,
                            selectedClass = _state.value.selectedClass.takeIf { it in names } ?: names.first()
                        )
                        // Best-effort: try to load subject details for the first class.
                        loadClassDetailsInternal(token, _state.value.selectedClass)
                    }
                    AppLogger.d("OnboardingAcademic", "Loaded ${classes.size} classes from backend")
                }
                is NetworkResult.Error -> {
                    AppLogger.e("OnboardingAcademic", "Failed to load ACADEMIC step: ${result.message}")
                    if (result.code == 401) {
                        preferenceRepository.clearSession()
                        _errorMessage.value = "Your session expired. Please sign in again before continuing onboarding."
                    }
                    // Non-auth errors are not surfaced as blocking errors - the screen
                    // still works with the fallback class/subject list.
                }
                is NetworkResult.ConnectionError -> {
                    AppLogger.e("OnboardingAcademic", "Connection error loading ACADEMIC step")
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Select a class. If we have a server-side mapping for it, also refresh
     * the subjects from the backend.
     */
    fun selectClass(className: String) {
        _state.value = _state.value.copy(selectedClass = className)
        val code = classCodeByName[className] ?: return
        viewModelScope.launch {
            val token = preferenceRepository.getUserToken().first() ?: return@launch
            loadClassDetailsInternal(token, className, code)
        }
    }

    private suspend fun loadClassDetailsInternal(token: String, className: String, classCode: String? = null) {
        val code = classCode ?: classCodeByName[className] ?: return
        when (val res = onboardingRepository.getClassDetails(token, code)) {
            is NetworkResult.Success -> {
                val subjects = res.data.listOfSubjects.mapIndexed { idx, dto ->
                    Subject(
                        id = dto.subCode.ifBlank { (idx + 1).toString() },
                        name = dto.subName,
                        teacherName = dto.teacherAssigned,
                        teacherImageUrl = null,
                        iconName = iconForSubject(dto.subName)
                    )
                }
                if (subjects.isNotEmpty()) {
                    _state.value = _state.value.copy(subjects = subjects)
                }
                AppLogger.d("OnboardingAcademic", "Loaded ${subjects.size} subjects for $className")
            }
            is NetworkResult.Error -> {
                AppLogger.e("OnboardingAcademic", "class-details failed: ${res.message}")
                if (res.code == 401) {
                    preferenceRepository.clearSession()
                    _errorMessage.value = "Your session expired. Please sign in again before continuing onboarding."
                }
            }
            is NetworkResult.ConnectionError -> {
                AppLogger.e("OnboardingAcademic", "class-details connection error")
            }
        }
    }

    private fun iconForSubject(name: String): String {
        val lower = name.lowercase()
        return when {
            "math" in lower -> "functions"
            "science" in lower || "physics" in lower || "chem" in lower || "bio" in lower -> "science"
            "history" in lower || "civics" in lower || "social" in lower -> "history_edu"
            else -> "book"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Builds the ACADEMIC `data_payload` from the current UI state so the server
     * persists REAL school_classes + school_subjects rows (instead of seeding
     * defaults). Contract:
     *   { "classes": [ { "code","name","sections":[...],
     *                    "subjects":[ {"sub_name","sub_code","teacher_assigned"} ] } ] }
     * Every available class is sent with the configured subject set; the section
     * list defaults to ["A"] when unknown.
     */
    private fun buildAcademicPayload(): JsonObject {
        val s = _state.value
        val subjectsArray: JsonArray = buildJsonArray {
            s.subjects.forEach { subj ->
                add(buildJsonObject {
                    put("sub_name", subj.name)
                    put("sub_code", subj.id)
                    if (subj.teacherName.isNullOrBlank()) put("teacher_assigned", JsonNull)
                    else put("teacher_assigned", subj.teacherName)
                })
            }
        }
        return buildJsonObject {
            put("classes", buildJsonArray {
                s.availableClasses.forEach { className ->
                    add(buildJsonObject {
                        put("code", classCodeByName[className] ?: className.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'))
                        put("name", className)
                        put("sections", buildJsonArray { add(JsonPrimitive("A")) })
                        put("subjects", subjectsArray)
                    })
                }
            })
        }
    }

    /**
     * POST to /api/v1/onboarding/submit with ob_step_type = "ACADEMIC".
     * We now send the real class/subject structure so the backend persists it.
     */
    fun submit(onSuccess: () -> Unit) {
        if (_isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val token = preferenceRepository.getUserToken().first()
            if (token.isNullOrBlank()) {
                _errorMessage.value = "You are not signed in. Please log in again."
                _isSubmitting.value = false
                return@launch
            }

            val request = OnboardingSubmitRequest(
                obStepType = ObStepType.ACADEMIC,
                isFinalSubmission = false,
                dataPayload = buildAcademicPayload()
            )

            when (val result = onboardingRepository.submitStep(token, request)) {
                is NetworkResult.Success -> {
                    AppLogger.d(
                        "OnboardingAcademic",
                        "ACADEMIC step submitted. nextStep=${result.data.nextStep}"
                    )
                    _isSubmitting.value = false
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    AppLogger.e("OnboardingAcademic", "Submit failed: ${result.message} (code=${result.code})")
                    _errorMessage.value = if (result.code == 401) {
                        preferenceRepository.clearSession()
                        "Your session expired. Please sign in again before continuing onboarding."
                    } else {
                        result.message
                    }
                    _isSubmitting.value = false
                }
                is NetworkResult.ConnectionError -> {
                    _errorMessage.value = "No internet connection. Please try again."
                    _isSubmitting.value = false
                }
            }
        }
    }
}
