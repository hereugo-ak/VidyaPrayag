package com.littlebridge.enrollplus.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * DTOs that mirror the server contract defined in
 * server/src/main/kotlin/.../feature/onboarding/OnboardingRouting.kt
 *
 * Endpoints:
 *   GET  /api/v1/onboarding/step?obStepType={BASIC|BRANDING|ACADEMIC|REVIEW}
 *   GET  /api/v1/onboarding/academic/class-details?classId={code}
 *   POST /api/v1/onboarding/submit
 *
 * All endpoints require: Authorization: Bearer <jwt>
 */

// ---------- Step type constants ----------
object ObStepType {
    const val BASIC = "BASIC"
    const val BRANDING = "BRANDING"
    const val ACADEMIC = "ACADEMIC"
    const val REVIEW = "REVIEW"
}

// ---------- POST /submit request ----------
@Serializable
data class OnboardingSubmitRequest(
    @SerialName("ob_step_type") val obStepType: String,
    @SerialName("is_final_submission") val isFinalSubmission: Boolean = false,
    @SerialName("data_payload") val dataPayload: JsonObject = JsonObject(emptyMap())
)

// ---------- POST /submit response ----------
@Serializable
data class OnboardingSubmitResponse(
    @SerialName("next_step") val nextStep: String? = null,
    @SerialName("is_onboarding_complete") val isOnboardingComplete: Boolean = false,
    @SerialName("redirect_to_home") val redirectToHome: Boolean = false
)

// ---------- GET /step shared sub-DTOs ----------
@Serializable
data class OnboardingFieldDto(
    val key: String,
    val type: String,
    @SerialName("draft_exists") val draftExists: Boolean = false,
    @SerialName("draft_value") val draftValue: String? = null,
    @SerialName("input_type") val inputType: String
)

@Serializable
data class ClassSummaryDto(
    val id: String,
    val name: String,
    val sections: List<String> = emptyList()
)

@Serializable
data class ReviewComplianceDoc(
    @SerialName("doc_id") val docId: String,
    @SerialName("doc_name") val docName: String,
    @SerialName("is_verified") val isVerified: Boolean = false
)

@Serializable
data class ReviewModule(
    val name: String,
    @SerialName("isSelected") val isSelected: Boolean = false
)

@Serializable
data class ReviewIdentity(
    @SerialName("institution_name") val institutionName: String,
    @SerialName("is_verified") val isVerified: Boolean = false
)

// ---------- GET /step response ----------
@Serializable
data class OnboardingStepResponse(
    @SerialName("ob_step_type") val obStepType: String,
    @SerialName("current_step_count") val currentStepCount: Int = 0,
    @SerialName("total_step_count") val totalStepCount: Int = 4,
    @SerialName("step_name") val stepName: String? = null,
    @SerialName("step_icon") val stepIcon: String? = null,
    @SerialName("step_heading") val stepHeading: String? = null,
    @SerialName("list_of_data") val listOfData: List<OnboardingFieldDto>? = null,
    @SerialName("list_of_active_classes") val listOfActiveClasses: List<ClassSummaryDto>? = null,
    @SerialName("identity_details") val identityDetails: ReviewIdentity? = null,
    @SerialName("compliance_docs") val complianceDocs: List<ReviewComplianceDoc>? = null,
    @SerialName("list_of_selected_modules") val listOfSelectedModules: List<ReviewModule>? = null
)

// ---------- GET /academic/class-details DTOs ----------
@Serializable
data class SubjectDetailDto(
    @SerialName("sub_name") val subName: String,
    @SerialName("sub_code") val subCode: String,
    @SerialName("teacher_assigned") val teacherAssigned: String? = null
)

@Serializable
data class ClassDetailsResponse(
    @SerialName("class_id") val classId: String,
    @SerialName("class_name") val className: String,
    @SerialName("total_subjects") val totalSubjects: Int = 0,
    @SerialName("list_of_subjects") val listOfSubjects: List<SubjectDetailDto> = emptyList()
)

// ---------- GET /status DTOs ----------
@Serializable
data class OnboardingStepStatus(
    @SerialName("step") val step: String,
    @SerialName("current_step_count") val currentStepCount: Int = 0,
    @SerialName("is_done") val isDone: Boolean = false
)

@Serializable
data class OnboardingStatusResponse(
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("is_complete") val isComplete: Boolean = false,
    @SerialName("completion_percent") val completionPercent: Int = 0,
    @SerialName("resume_step") val resumeStep: String = ObStepType.BASIC,
    @SerialName("total_step_count") val totalStepCount: Int = 4,
    @SerialName("steps") val steps: List<OnboardingStepStatus> = emptyList()
)

// ---------- POST /complete response ----------
@Serializable
data class OnboardingCompletionResponse(
    @SerialName("school_id") val schoolId: String,
    @SerialName("is_complete") val isComplete: Boolean = true,
    @SerialName("onboarding_status") val onboardingStatus: String = "active"
)

// ---------- GET /setup-progress response ----------
@Serializable
data class SetupProgressStep(
    val step: String,
    val status: String,
    val count: Int,
)

@Serializable
data class SetupProgress(
    val setupComplete: Boolean = false,
    val steps: List<SetupProgressStep> = emptyList(),
    val completedSteps: Int = 0,
    val totalSteps: Int = 5,
    val completionPercent: Int = 0,
)

// ---------- Payload key constants (matches server field schemas) ----------
object ObPayloadKeys {
    // BASIC step
    const val SCHOOL_NAME = "school_name"
    const val SHORT_NAME = "short_name"
    const val BOARD = "board"
    const val SCHOOL_TYPE = "school_type"
    const val AFFILIATION_NUMBER = "affiliation_number"
    const val MEDIUM = "medium"
    const val SCHOOL_GENDER = "school_gender"
    const val CONTACT_EMAIL = "contact_email"
    const val CONTACT_PHONE = "contact_phone"
    const val PRINCIPAL_NAME = "principal_name"
    const val PRINCIPAL_PHONE = "principal_phone"
    const val CITY = "city"
    const val DISTRICT = "district"
    const val STATE = "state"
    const val PINCODE = "pincode"
    const val FULL_ADDRESS = "full_address"
    // Geo (server persists these on schools.latitude / schools.longitude).
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"

    // BRANDING step
    const val LOGO_URL = "logo_url"
    const val BRAND_COLOR = "brand_color"
    const val COVER_IMAGE_URL = "cover_image_url"

    // ACADEMIC step — merged onboarding flow academic year config
    const val ACADEMIC_YEAR_LABEL = "academic_year_label"
    const val ACADEMIC_YEAR_START_DATE = "academic_year_start_date"
    const val ACADEMIC_YEAR_END_DATE = "academic_year_end_date"
    const val WORKING_DAYS = "working_days"
    const val SCHOOL_START_TIME = "school_start_time"
    const val SCHOOL_END_TIME = "school_end_time"
    const val PERIODS_PER_DAY = "periods_per_day"
}
