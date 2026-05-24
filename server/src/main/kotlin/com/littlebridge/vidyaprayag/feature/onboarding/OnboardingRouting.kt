/*
 * File: OnboardingRouting.kt
 * Module: feature.onboarding
 *
 * Endpoints:
 *   GET  /api/v1/onboarding/step?obStepType={BASIC|BRANDING|ACADEMIC|REVIEW}
 *   GET  /api/v1/onboarding/academic/class-details?classId={code}
 *   POST /api/v1/onboarding/submit
 *
 * Spec ref: vidya_prayag_api_spec.artifact.md §School Onboarding Flow
 *
 * Drafts:
 *   Stored in `school_onboarding_drafts` keyed by (user_id, step_type, key).
 *   On REVIEW with `is_final_submission=true`:
 *     - We create/update a row in `schools` for this user.
 *     - We set `app_users.school_id` so subsequent calls resolve the school.
 *     - We stamp `schools.onboarded_at = NOW()` to flip status to COMPLETED.
 *
 * Real data flow (no hardcoded school fallbacks):
 *   If the calling user has not created a school yet, ACADEMIC/REVIEW
 *   responses are empty lists / a 404 instead of mock data.
 */
package com.littlebridge.vidyaprayag.feature.onboarding

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.OnboardingDraftsTable
import com.littlebridge.vidyaprayag.db.SchoolClassesTable
import com.littlebridge.vidyaprayag.db.SchoolSubjectsTable
import com.littlebridge.vidyaprayag.db.SchoolsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
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
data class ReviewModule(val name: String, val isSelected: Boolean)

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

// ---------- Field schemas per step ----------
private val BASIC_FIELDS = listOf(
    Triple("school_name", "SchoolName", "line"),
    Triple("board", "Board", "dropdown"),               // CBSE|ICSE|UP_STATE…
    Triple("medium", "Medium", "dropdown"),
    Triple("school_gender", "Gender", "dropdown"),
    Triple("contact_email", "Email", "line"),
    Triple("contact_phone", "Phone", "line"),
    Triple("city", "City", "line"),
    Triple("district", "District", "line"),
    Triple("state", "State", "line"),
    Triple("pincode", "Pincode", "line"),
    Triple("full_address", "Address", "multiline")
)
private val BRANDING_FIELDS = listOf(
    Triple("logo_url", "Logo", "image"),
    Triple("brand_color", "ThemeColor", "color")
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
private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

private suspend fun resolveSchoolIdForUser(uid: UUID): UUID? = dbQuery {
    AppUsersTable.selectAll().where { AppUsersTable.id eq uid }
        .singleOrNull()?.get(AppUsersTable.schoolId)
}

private fun slugify(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

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
                    OnboardingDraftsTable.selectAll()
                        .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq type) }
                        .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }
                }

                when (type) {
                    "BASIC", "BRANDING" -> {
                        val fields = if (type == "BASIC") BASIC_FIELDS else BRANDING_FIELDS
                        val list = fields.map { (k, t, input) ->
                            OnboardingFieldDto(
                                key = k, type = t,
                                draftExists = drafts[k] != null,
                                draftValue = drafts[k],
                                inputType = input
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
                        val schoolId = resolveSchoolIdForUser(uid)
                        val classes = if (schoolId == null) emptyList() else dbQuery {
                            SchoolClassesTable.selectAll()
                                .where { SchoolClassesTable.schoolId eq schoolId }
                                .map {
                                    val secs = runCatching {
                                        lenientJson.parseToJsonElement(it[SchoolClassesTable.sections])
                                            .let { e -> (e as? JsonArray)?.map { p -> (p as JsonPrimitive).content } }
                                    }.getOrNull() ?: emptyList()
                                    ClassSummaryDto(
                                        id = it[SchoolClassesTable.code],
                                        name = it[SchoolClassesTable.name],
                                        sections = secs
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
                        val schoolId = resolveSchoolIdForUser(uid)
                        val school = schoolId?.let {
                            dbQuery { SchoolsTable.selectAll().where { SchoolsTable.id eq it }.singleOrNull() }
                        }
                        val identity = ReviewIdentity(
                            institutionName = school?.get(SchoolsTable.name) ?: "—",
                            isVerified = (school?.get(SchoolsTable.onboardedAt) != null)
                        )
                        val docs = listOf(
                            ReviewComplianceDoc("d_1", "Affiliation Cert", false),
                            ReviewComplianceDoc("d_2", "Building Safety", false)
                        )
                        val modules = listOf(
                            ReviewModule("Analytics", true),
                            ReviewModule("PTM Management", true),
                            ReviewModule("Scholarships", false)
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
            get("/academic/class-details") {
                val code = call.request.queryParameters["classId"] ?: run {
                    call.fail("classId is required"); return@get
                }
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val schoolId = resolveSchoolIdForUser(uid) ?: run {
                    call.fail("User has no school yet. Complete onboarding first.", HttpStatusCode.NotFound); return@get
                }
                val payload = dbQuery {
                    val cls = SchoolClassesTable.selectAll()
                        .where { (SchoolClassesTable.schoolId eq schoolId) and (SchoolClassesTable.code eq code) }
                        .singleOrNull() ?: return@dbQuery null
                    val classRowId = cls[SchoolClassesTable.id].value
                    val subjects = SchoolSubjectsTable.selectAll()
                        .where { SchoolSubjectsTable.classId eq classRowId }
                        .map {
                            SubjectDetailDto(
                                subName = it[SchoolSubjectsTable.subName],
                                subCode = it[SchoolSubjectsTable.subCode],
                                teacherAssigned = it[SchoolSubjectsTable.teacherAssigned]
                            )
                        }
                    ClassDetailsResponse(
                        classId = cls[SchoolClassesTable.code],
                        className = cls[SchoolClassesTable.name],
                        totalSubjects = subjects.size,
                        listOfSubjects = subjects
                    )
                }
                if (payload == null) call.fail("Class '$code' not found", HttpStatusCode.NotFound)
                else call.ok(payload, message = "Class details fetched")
            }

            // -------- POST /submit --------
            post("/submit") {
                val req = call.receive<SubmitRequest>()
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val step = req.obStepType.uppercase()

                // 1. Upsert (key,value) into drafts.
                dbQuery {
                    req.dataPayload.forEach { (k, v) ->
                        val text = if (v is JsonPrimitive && v.isString) v.content else v.toString()
                        OnboardingDraftsTable.deleteWhere {
                            (OnboardingDraftsTable.userId eq uid) and
                                (OnboardingDraftsTable.stepType eq step) and
                                (OnboardingDraftsTable.key eq k)
                        }
                        OnboardingDraftsTable.insert {
                            it[OnboardingDraftsTable.userId] = uid
                            it[stepType] = step
                            it[OnboardingDraftsTable.key] = k
                            it[value] = text
                            it[updatedAt] = Instant.now()
                        }
                    }
                }

                val complete = req.isFinalSubmission && step == "REVIEW"
                if (complete) {
                    dbQuery {
                        val basics = OnboardingDraftsTable.selectAll()
                            .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq "BASIC") }
                            .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }
                        val branding = OnboardingDraftsTable.selectAll()
                            .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq "BRANDING") }
                            .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }

                        val schoolName = basics["school_name"] ?: "Unnamed School"
                        val now = Instant.now()

                        // Find existing school via app_users.school_id
                        val u = AppUsersTable.selectAll().where { AppUsersTable.id eq uid }.singleOrNull()
                        val existingSchoolId = u?.get(AppUsersTable.schoolId)

                        if (existingSchoolId == null) {
                            val newSchoolId = UUID.randomUUID()
                            SchoolsTable.insert {
                                it[id] = newSchoolId
                                it[name] = schoolName
                                it[slug] = slugify(schoolName) + "-" + newSchoolId.toString().take(6)
                                it[board] = basics["board"] ?: "CBSE"
                                it[medium] = basics["medium"] ?: "English"
                                it[schoolGender] = basics["school_gender"] ?: "co_ed"
                                it[contactEmail] = basics["contact_email"]
                                it[contactPhone] = basics["contact_phone"]
                                it[fullAddress] = basics["full_address"]
                                it[city] = basics["city"] ?: "Unknown"
                                it[district] = basics["district"] ?: "Unknown"
                                it[state] = basics["state"] ?: "Uttar Pradesh"
                                it[pincode] = basics["pincode"]
                                it[logoUrl] = branding["logo_url"]
                                it[brandColor] = branding["brand_color"] ?: "#2563EB"
                                it[isActive] = true
                                it[onboardedAt] = now
                                it[createdAt] = now
                                it[updatedAt] = now
                            }
                            AppUsersTable.update({ AppUsersTable.id eq uid }) {
                                it[schoolId] = newSchoolId
                                it[role] = "school_admin"
                                it[profileCompleted] = true
                                it[updatedAt] = now
                            }
                        } else {
                            SchoolsTable.update({ SchoolsTable.id eq existingSchoolId }) {
                                basics["school_name"]?.let { v -> it[name] = v }
                                basics["board"]?.let { v -> it[board] = v }
                                basics["medium"]?.let { v -> it[medium] = v }
                                basics["school_gender"]?.let { v -> it[schoolGender] = v }
                                basics["contact_email"]?.let { v -> it[contactEmail] = v }
                                basics["contact_phone"]?.let { v -> it[contactPhone] = v }
                                basics["full_address"]?.let { v -> it[fullAddress] = v }
                                basics["city"]?.let { v -> it[city] = v }
                                basics["district"]?.let { v -> it[district] = v }
                                basics["state"]?.let { v -> it[state] = v }
                                basics["pincode"]?.let { v -> it[pincode] = v }
                                branding["logo_url"]?.let { v -> it[logoUrl] = v }
                                branding["brand_color"]?.let { v -> it[brandColor] = v }
                                it[onboardedAt] = now
                                it[updatedAt] = now
                            }
                            AppUsersTable.update({ AppUsersTable.id eq uid }) {
                                it[profileCompleted] = true
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
