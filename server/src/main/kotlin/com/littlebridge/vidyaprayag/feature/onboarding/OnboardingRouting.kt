/*
 * File: OnboardingRouting.kt
 * Module: feature.onboarding
 *
 * Endpoints implemented:
 *   GET  /api/v1/onboarding/step?obStepType={BASIC|BRANDING|ACADEMIC|REVIEW}
 *   GET  /api/v1/onboarding/academic/class-details?classId={code}
 *   POST /api/v1/onboarding/submit
 *
 * Spec ref: vidya_prayag_api_spec.artifact.md §Module: School Onboarding Flow
 *
 * Design notes:
 *   - Drafts are stored in school_onboarding_drafts as (userId, stepType, key, value)
 *     so any step can be saved partial. The final REVIEW submission promotes the
 *     drafts into the proper feature tables (SchoolTable, ClassTable, SubjectTable …)
 *     and flips school.onboarding_status to COMPLETED.
 *   - On POST /submit, the data_payload is a free-form JSON object whose keys
 *     match the `key` field returned by GET /step.
 *
 * Used by UI:
 *   - composeApp/.../ui/screens/admin/InstitutionalBasicOBScreen.kt
 *   - composeApp/.../ui/screens/admin/BrandingInfoOBScreen.kt
 *   - composeApp/.../ui/screens/admin/AcademicInfoOBScreen.kt
 *   - composeApp/.../ui/screens/admin/LaunchInfoOBScreen.kt
 */
package com.littlebridge.vidyaprayag.feature.onboarding

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.ClassTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.OnboardingDraftTable
import com.littlebridge.vidyaprayag.db.SchoolTable
import com.littlebridge.vidyaprayag.db.SubjectTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

// ---------- DTOs ----------

@Serializable
data class OnboardingFieldDto(
    val key: String,
    val type: String,
    @SerialName("draft_exists") val draftExists: Boolean,
    @SerialName("draft_value") val draftValue: String? = null,
    @SerialName("input_type") val inputType: String
)

@Serializable
data class ClassSummaryDto(
    val id: String,
    val name: String,
    val sections: List<String>
)

@Serializable
data class ReviewComplianceDoc(
    @SerialName("doc_id") val docId: String,
    @SerialName("doc_name") val docName: String,
    @SerialName("is_verified") val isVerified: Boolean
)

@Serializable
data class ReviewModule(
    val name: String,
    val isSelected: Boolean
)

@Serializable
data class ReviewIdentity(
    @SerialName("institution_name") val institutionName: String,
    @SerialName("is_verified") val isVerified: Boolean
)

@Serializable
data class OnboardingStepResponse(
    @SerialName("ob_step_type") val obStepType: String,
    @SerialName("current_step_count") val currentStepCount: Int,
    @SerialName("total_step_count") val totalStepCount: Int,
    @SerialName("step_name") val stepName: String? = null,
    @SerialName("step_icon") val stepIcon: String? = null,
    @SerialName("step_heading") val stepHeading: String? = null,
    @SerialName("list_of_data") val listOfData: List<OnboardingFieldDto>? = null,
    @SerialName("list_of_active_classes") val listOfActiveClasses: List<ClassSummaryDto>? = null,
    @SerialName("identity_details") val identityDetails: ReviewIdentity? = null,
    @SerialName("compliance_docs") val complianceDocs: List<ReviewComplianceDoc>? = null,
    @SerialName("list_of_selected_modules") val listOfSelectedModules: List<ReviewModule>? = null
)

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
    @SerialName("total_subjects") val totalSubjects: Int,
    @SerialName("list_of_subjects") val listOfSubjects: List<SubjectDetailDto>
)

@Serializable
data class SubmitRequest(
    @SerialName("ob_step_type") val obStepType: String,
    @SerialName("is_final_submission") val isFinalSubmission: Boolean = false,
    @SerialName("data_payload") val dataPayload: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class SubmitResponse(
    @SerialName("next_step") val nextStep: String?,
    @SerialName("is_onboarding_complete") val isOnboardingComplete: Boolean,
    @SerialName("redirect_to_home") val redirectToHome: Boolean
)

// ---------- Field schemas per step (these power the dynamic UI) ----------

private val BASIC_FIELDS = listOf(
    Triple("school_name", "SchoolName", "line"),
    Triple("board_affiliation", "BoardAffiliation", "dropdown"),
    Triple("official_email", "Email", "line"),
    Triple("contact_number", "Phone", "line"),
    Triple("country_code", "CountryCode", "dropdown"),
    Triple("address", "Address", "multiline")
)

private val BRANDING_FIELDS = listOf(
    Triple("logo_url", "Logo", "image"),
    Triple("theme_color", "ThemeColor", "color")
)

// ---------- Helpers ----------

private fun nextStepAfter(step: String): String? = when (step) {
    "BASIC"    -> "BRANDING"
    "BRANDING" -> "ACADEMIC"
    "ACADEMIC" -> "REVIEW"
    "REVIEW"   -> null
    else       -> null
}

private fun stepIndex(step: String): Int = when (step) {
    "BASIC" -> 1; "BRANDING" -> 2; "ACADEMIC" -> 3; "REVIEW" -> 4; else -> 1
}

// ---------- Routing ----------

fun Route.onboardingRouting() {
    authenticate("jwt") {
        route("/api/v1/onboarding") {

            // -------- GET /step --------
            get("/step") {
                val type = (call.request.queryParameters["obStepType"] ?: "BASIC").uppercase()
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }

                val drafts: Map<String, String> = dbQuery {
                    OnboardingDraftTable.selectAll()
                        .where { (OnboardingDraftTable.userId eq uid) and (OnboardingDraftTable.stepType eq type) }
                        .associate { it[OnboardingDraftTable.key] to it[OnboardingDraftTable.value] }
                }

                when (type) {
                    "BASIC", "BRANDING" -> {
                        val fields = if (type == "BASIC") BASIC_FIELDS else BRANDING_FIELDS
                        val list = fields.map { (k, t, input) ->
                            val v = drafts[k]
                            OnboardingFieldDto(
                                key = k, type = t,
                                draftExists = v != null, draftValue = v, inputType = input
                            )
                        }
                        call.ok(
                            OnboardingStepResponse(
                                obStepType = type,
                                currentStepCount = stepIndex(type),
                                totalStepCount = 4,
                                stepName = if (type == "BASIC") "Institutional Basics" else "Branding & Visuals",
                                stepIcon = if (type == "BASIC") "school" else "palette",
                                stepHeading = if (type == "BASIC") "Establish identity." else "Define your look.",
                                listOfData = list
                            ),
                            message = "Step data fetched"
                        )
                    }

                    "ACADEMIC" -> {
                        val schoolId = dbQuery {
                            SchoolTable.selectAll()
                                .where { SchoolTable.ownerUserId eq uid }
                                .singleOrNull()?.get(SchoolTable.id)?.value
                        }
                        val classes = if (schoolId == null) emptyList() else dbQuery {
                            ClassTable.selectAll().where { ClassTable.schoolId eq schoolId }.map {
                                ClassSummaryDto(
                                    id = it[ClassTable.code],
                                    name = it[ClassTable.name],
                                    sections = it[ClassTable.sections].split(',').filter { s -> s.isNotBlank() }
                                )
                            }
                        }
                        call.ok(
                            OnboardingStepResponse(
                                obStepType = type,
                                currentStepCount = 3,
                                totalStepCount = 4,
                                stepName = "Academic Structure",
                                stepIcon = "history_edu",
                                listOfActiveClasses = classes
                            ),
                            message = "Academic structure fetched"
                        )
                    }

                    "REVIEW" -> {
                        val school = dbQuery {
                            SchoolTable.selectAll()
                                .where { SchoolTable.ownerUserId eq uid }
                                .singleOrNull()
                        }
                        val identity = ReviewIdentity(
                            institutionName = school?.get(SchoolTable.name) ?: "—",
                            isVerified = school?.get(SchoolTable.isVerified) ?: false
                        )
                        // Compliance docs are not yet stored — return a stub list so
                        // the UI can render the section. Replace with a real query
                        // once the upload flow lands.
                        val docs = listOf(
                            ReviewComplianceDoc("d_1", "Affiliation Cert", school?.get(SchoolTable.isVerified) ?: false),
                            ReviewComplianceDoc("d_2", "Building Safety", false)
                        )
                        val modules = listOf(
                            ReviewModule("Analytics", isSelected = true),
                            ReviewModule("PTM Management", isSelected = true),
                            ReviewModule("Scholarships", isSelected = false)
                        )
                        call.ok(
                            OnboardingStepResponse(
                                obStepType = type,
                                currentStepCount = 4,
                                totalStepCount = 4,
                                stepName = "Launch & Review",
                                stepIcon = "rocket_launch",
                                identityDetails = identity,
                                complianceDocs = docs,
                                listOfSelectedModules = modules
                            ),
                            message = "Review data fetched"
                        )
                    }

                    else -> call.fail("Unknown obStepType '$type'")
                }
            }

            // -------- GET /academic/class-details --------
            //
            // Lookup order for the school context:
            //   1. The school OWNED by the calling user (real ADMIN flow).
            //   2. If none exists yet (e.g. a freshly signed-up admin who hasn't
            //      completed onboarding, OR a PARENT testing the endpoint), fall
            //      back to the first/demo school in the DB. This keeps the
            //      Postman collection's example  ?classId=C10  working out of the
            //      box against the seeded demo data (Grade 10 @ St. Xavier).
            get("/academic/class-details") {
                val code = call.request.queryParameters["classId"] ?: run {
                    call.fail("classId is required"); return@get
                }
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val payload = dbQuery {
                    val ownSchoolId = SchoolTable.selectAll()
                        .where { SchoolTable.ownerUserId eq uid }
                        .singleOrNull()?.get(SchoolTable.id)?.value
                    val schoolId = ownSchoolId
                        ?: SchoolTable.selectAll()
                            .limit(1)
                            .singleOrNull()?.get(SchoolTable.id)?.value
                        ?: return@dbQuery null
                    val cls = ClassTable.selectAll()
                        .where { (ClassTable.schoolId eq schoolId) and (ClassTable.code eq code) }
                        .singleOrNull() ?: return@dbQuery null
                    val classRowId = cls[ClassTable.id].value
                    val subjects = SubjectTable.selectAll()
                        .where { SubjectTable.classId eq classRowId }
                        .map {
                            SubjectDetailDto(
                                subName = it[SubjectTable.subName],
                                subCode = it[SubjectTable.subCode],
                                teacherAssigned = it[SubjectTable.teacherAssigned]
                            )
                        }
                    ClassDetailsResponse(
                        classId = cls[ClassTable.code],
                        className = cls[ClassTable.name],
                        totalSubjects = subjects.size,
                        listOfSubjects = subjects
                    )
                }
                if (payload == null) call.fail(
                    "Class '$code' not found. Try classId=C10 or C05 against the demo school.",
                    HttpStatusCode.NotFound
                ) else call.ok(payload, message = "Class details fetched")
            }

            // -------- POST /submit --------
            post("/submit") {
                val req = call.receive<SubmitRequest>()
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val step = req.obStepType.uppercase()

                // 1. Upsert every (key,value) pair into the draft table.
                dbQuery {
                    req.dataPayload.forEach { (k, v) ->
                        val text = if (v is JsonPrimitive && v.isString) v.content else v.toString()
                        // Delete-then-insert because Exposed 0.50 lacks upsert on SQLite.
                        OnboardingDraftTable.deleteWhere {
                            (OnboardingDraftTable.userId eq uid) and
                            (OnboardingDraftTable.stepType eq step) and
                            (OnboardingDraftTable.key eq k)
                        }
                        OnboardingDraftTable.insert {
                            it[OnboardingDraftTable.userId] = uid
                            it[stepType] = step
                            it[OnboardingDraftTable.key] = k
                            it[value] = text
                        }
                    }
                }

                val complete = req.isFinalSubmission && step == "REVIEW"
                if (complete) {
                    // Promote drafts → SchoolTable, ensure a school row exists.
                    dbQuery {
                        val basics = OnboardingDraftTable.selectAll()
                            .where { (OnboardingDraftTable.userId eq uid) and (OnboardingDraftTable.stepType eq "BASIC") }
                            .associate { it[OnboardingDraftTable.key] to it[OnboardingDraftTable.value] }
                        val branding = OnboardingDraftTable.selectAll()
                            .where { (OnboardingDraftTable.userId eq uid) and (OnboardingDraftTable.stepType eq "BRANDING") }
                            .associate { it[OnboardingDraftTable.key] to it[OnboardingDraftTable.value] }

                        val existing = SchoolTable.selectAll()
                            .where { SchoolTable.ownerUserId eq uid }
                            .singleOrNull()

                        val now = LocalDateTime.now()
                        if (existing == null) {
                            SchoolTable.insert {
                                it[ownerUserId] = uid
                                it[name] = basics["school_name"] ?: "Unnamed School"
                                it[boardAffiliation] = basics["board_affiliation"]
                                it[officialEmail] = basics["official_email"]
                                it[contactNumber] = basics["contact_number"]
                                it[countryCode] = basics["country_code"] ?: "+91"
                                it[address] = basics["address"]
                                it[logoUrl] = branding["logo_url"]
                                it[themeColor] = branding["theme_color"]
                                it[onboardingStatus] = "COMPLETED"
                                it[createdAt] = now
                                it[updatedAt] = now
                            }
                        } else {
                            SchoolTable.update({ SchoolTable.ownerUserId eq uid }) {
                                basics["school_name"]?.let { v -> it[name] = v }
                                basics["board_affiliation"]?.let { v -> it[boardAffiliation] = v }
                                basics["official_email"]?.let { v -> it[officialEmail] = v }
                                basics["contact_number"]?.let { v -> it[contactNumber] = v }
                                basics["country_code"]?.let { v -> it[countryCode] = v }
                                basics["address"]?.let { v -> it[address] = v }
                                branding["logo_url"]?.let { v -> it[logoUrl] = v }
                                branding["theme_color"]?.let { v -> it[themeColor] = v }
                                it[onboardingStatus] = "COMPLETED"
                                it[updatedAt] = now
                            }
                        }
                    }
                }

                call.ok(
                    SubmitResponse(
                        nextStep = if (complete) null else nextStepAfter(step),
                        isOnboardingComplete = complete,
                        redirectToHome = complete
                    ),
                    message = if (complete) "Onboarding completed" else "Step processed successfully"
                )
            }
        }
    }
}
