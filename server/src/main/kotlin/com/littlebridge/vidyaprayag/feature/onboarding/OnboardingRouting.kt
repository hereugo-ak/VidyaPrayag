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
import com.littlebridge.vidyaprayag.db.SchoolMediaTable
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
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

/**
 * Per-step completion flag for the status endpoint. A step reads as `done`
 * when its backing REAL data exists (not merely when a draft row was saved),
 * so the client can resume a returning admin at the first incomplete step.
 */
@Serializable
data class OnboardingStepStatus(
    @SerialName("step") val step: String,                 // BASIC|BRANDING|ACADEMIC|REVIEW
    @SerialName("current_step_count") val currentStepCount: Int,
    @SerialName("is_done") val isDone: Boolean
)

/**
 * Server-truth onboarding status for the calling admin. The client gate
 * (NavGraphV2 AuthedFlow) reads this on every login to decide dashboard vs
 * onboarding, and `resume_step` lets it drop a returning admin at the first
 * incomplete step instead of always restarting at BASIC.
 */
@Serializable
data class OnboardingStatusResponse(
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("is_complete") val isComplete: Boolean,
    @SerialName("completion_percent") val completionPercent: Int,
    @SerialName("resume_step") val resumeStep: String,    // first incomplete step
    @SerialName("total_step_count") val totalStepCount: Int,
    @SerialName("steps") val steps: List<OnboardingStepStatus>
)

@Serializable
data class CompletionResponse(
    @SerialName("school_id") val schoolId: String,
    @SerialName("is_complete") val isComplete: Boolean,
    @SerialName("onboarding_status") val onboardingStatus: String  // "active" once complete
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
    Triple("full_address", "Address", "multiline"),
    // Geo coordinates captured by the client's "use current location" / map
    // picker. Stored as plain strings in the draft, parsed to Double on commit.
    Triple("latitude", "Latitude", "geo"),
    Triple("longitude", "Longitude", "geo")
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

/**
 * Ensures a `schools` row exists for [uid] and returns its id, creating one from
 * the saved BASIC/BRANDING drafts when absent. Also stamps app_users.school_id
 * and promotes the user to school_admin. Does NOT set onboarded_at (that only
 * happens on the final REVIEW submit). Must be called inside a dbQuery {}.
 */
private fun ensureSchoolForUser(uid: UUID): UUID {
    val existing = AppUsersTable.selectAll().where { AppUsersTable.id eq uid }
        .singleOrNull()?.get(AppUsersTable.schoolId)
    if (existing != null) return existing

    val basics = OnboardingDraftsTable.selectAll()
        .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq "BASIC") }
        .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }
    val branding = OnboardingDraftsTable.selectAll()
        .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq "BRANDING") }
        .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }

    val schoolName = basics["school_name"]?.takeIf { it.isNotBlank() } ?: "Unnamed School"
    val now = Instant.now()
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
        it[latitude] = basics["latitude"]?.toDoubleOrNull()
        it[longitude] = basics["longitude"]?.toDoubleOrNull()
        it[logoUrl] = branding["logo_url"]
        it[brandColor] = branding["brand_color"] ?: "#2563EB"
        it[isActive] = true
        it[createdAt] = now
        it[updatedAt] = now
    }
    AppUsersTable.update({ AppUsersTable.id eq uid }) {
        it[schoolId] = newSchoolId
        it[role] = "school_admin"
        it[updatedAt] = now
    }
    return newSchoolId
}

/** Pushes BASIC/BRANDING draft values into the live `schools` row. */
private fun syncSchoolBasics(schoolId: UUID, uid: UUID) {
    val basics = OnboardingDraftsTable.selectAll()
        .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq "BASIC") }
        .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }
    val branding = OnboardingDraftsTable.selectAll()
        .where { (OnboardingDraftsTable.userId eq uid) and (OnboardingDraftsTable.stepType eq "BRANDING") }
        .associate { it[OnboardingDraftsTable.key] to it[OnboardingDraftsTable.value] }
    val now = Instant.now()
    SchoolsTable.update({ SchoolsTable.id eq schoolId }) {
        basics["school_name"]?.takeIf { v -> v.isNotBlank() }?.let { v -> it[name] = v }
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
        basics["latitude"]?.toDoubleOrNull()?.let { v -> it[latitude] = v }
        basics["longitude"]?.toDoubleOrNull()?.let { v -> it[longitude] = v }
        branding["logo_url"]?.let { v -> it[logoUrl] = v }
        branding["brand_color"]?.let { v -> it[brandColor] = v }
        it[updatedAt] = now
    }
}

/**
 * Default academic structure used when the client submits ACADEMIC without an
 * explicit `classes` payload (the legacy frontend sends an empty body). This
 * guarantees step 3 produces REAL school_classes/school_subjects rows so
 * completion logic and the dashboard reflect reality.
 */
private val DEFAULT_ACADEMIC_CLASSES: List<Triple<String, String, List<String>>> = listOf(
    Triple("c1", "Class 1", listOf("A")),
    Triple("c2", "Class 2", listOf("A")),
    Triple("c3", "Class 3", listOf("A"))
)
private val DEFAULT_ACADEMIC_SUBJECTS: List<Pair<String, String>> = listOf(
    "Mathematics" to "MATH",
    "Science" to "SCI",
    "English" to "ENG",
    "Social Studies" to "SST"
)

/**
 * Persists the academic structure for [schoolId] from the submit payload.
 * Payload contract (all optional, falls back to defaults):
 *   {
 *     "classes": [
 *       { "code":"c8", "name":"Class 8", "sections":["A","B"],
 *         "subjects":[ {"sub_name":"Maths","sub_code":"MATH","teacher_assigned":"..."} ] }
 *     ]
 *   }
 * Idempotent: classes are upserted by (school, code); subjects are replaced for
 * each touched class. Must be called inside a dbQuery {}.
 */
private fun persistAcademicStructure(schoolId: UUID, payload: JsonObject) {
    val now = Instant.now()
    val classesJson = (payload["classes"] as? JsonArray)

    data class ParsedSubject(val name: String, val code: String, val teacher: String?)
    data class ParsedClass(val code: String, val name: String, val sections: List<String>, val subjects: List<ParsedSubject>)

    val parsedClasses: List<ParsedClass> = if (classesJson != null && classesJson.isNotEmpty()) {
        classesJson.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val name = o["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val code = o["code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: slugify(name)
            val sections = (o["sections"] as? JsonArray)
                ?.mapNotNull { s -> (s as? JsonPrimitive)?.contentOrNull }
                ?.ifEmpty { listOf("A") } ?: listOf("A")
            val subjects = (o["subjects"] as? JsonArray)?.mapNotNull { se ->
                val so = se as? JsonObject ?: return@mapNotNull null
                val sn = so["sub_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val sc = so["sub_code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: slugify(sn).uppercase()
                val tch = so["teacher_assigned"]?.jsonPrimitive?.contentOrNull
                ParsedSubject(sn, sc, tch)
            } ?: emptyList()
            ParsedClass(code, name, sections, subjects)
        }
    } else {
        // Legacy/empty payload -> seed sensible defaults so step 3 is real.
        DEFAULT_ACADEMIC_CLASSES.map { (code, name, sections) ->
            ParsedClass(
                code, name, sections,
                DEFAULT_ACADEMIC_SUBJECTS.map { (sn, sc) -> ParsedSubject(sn, sc, null) }
            )
        }
    }

    parsedClasses.forEach { pc ->
        val sectionsText = Json.encodeToString(JsonArray.serializer(), JsonArray(pc.sections.map { JsonPrimitive(it) }))
        val existing = SchoolClassesTable.selectAll()
            .where { (SchoolClassesTable.schoolId eq schoolId) and (SchoolClassesTable.code eq pc.code) }
            .singleOrNull()
        val classRowId: UUID = if (existing == null) {
            val newId = UUID.randomUUID()
            SchoolClassesTable.insert {
                it[id] = newId
                it[SchoolClassesTable.schoolId] = schoolId
                it[code] = pc.code
                it[name] = pc.name
                it[sections] = sectionsText
                it[createdAt] = now
            }
            newId
        } else {
            val rid = existing[SchoolClassesTable.id].value
            SchoolClassesTable.update({ SchoolClassesTable.id eq rid }) {
                it[name] = pc.name
                it[sections] = sectionsText
            }
            rid
        }
        // Replace subjects for this class (idempotent re-submit).
        SchoolSubjectsTable.deleteWhere { SchoolSubjectsTable.classId eq classRowId }
        pc.subjects.forEach { sub ->
            SchoolSubjectsTable.insert {
                it[classId] = classRowId
                it[subName] = sub.name
                it[subCode] = sub.code
                it[teacherAssigned] = sub.teacher
                it[createdAt] = now
            }
        }
    }
}

/**
 * Computes server-truth onboarding status for [uid] from REAL persisted data:
 *   - BASIC    done when a `schools` row exists for the user (name persisted).
 *   - BRANDING done when the school has a logo OR a non-default brand color
 *              (best-effort; never blocks completion).
 *   - ACADEMIC done when the school has at least one class.
 *   - REVIEW   done when `schools.onboarded_at` is stamped (final submit).
 *
 * IMPORTANT (the wrong-state bug): `is_complete` is NOT taken from a single
 * `onboarded_at` timestamp alone. That column can be hand-set on a manually
 * inserted row or carried by a seed (`DemoSeed`/`profile_completed=true`), which
 * previously made a school with NO classes and NO real setup report as fully
 * onboarded — the admin then landed on an empty dashboard with the wizard wrongly
 * skipped. Completion now requires the SUBSTANTIVE steps to genuinely exist
 * (a named school + at least one class) IN ADDITION TO the final stamp. If the
 * stamp is set but the substantive data is missing, the school is reported
 * incomplete and `resume_step` points at the first real step still missing.
 *
 * Completion percent is derived from the four steps; `resume_step` is the first
 * step that is not yet done (BRANDING is best-effort and never blocks). Must be
 * called inside a dbQuery {} via [DatabaseFactory.dbQuery] by the caller.
 */
private suspend fun computeOnboardingStatus(uid: UUID): OnboardingStatusResponse = dbQuery {
    val sid = AppUsersTable.selectAll().where { AppUsersTable.id eq uid }
        .singleOrNull()?.get(AppUsersTable.schoolId)
    val schoolRow = sid?.let {
        SchoolsTable.selectAll().where { SchoolsTable.id eq it }.singleOrNull()
    }

    val schoolId = schoolRow?.get(SchoolsTable.id)?.value
    val basicDone = schoolRow != null && schoolRow[SchoolsTable.name].isNotBlank()
    val brandingDone = schoolRow != null && (
        !schoolRow[SchoolsTable.logoUrl].isNullOrBlank() ||
            schoolRow[SchoolsTable.brandColor] != "#2563EB"
        )
    val academicDone = schoolId != null &&
        SchoolClassesTable.selectAll().where { SchoolClassesTable.schoolId eq schoolId }.count() > 0L
    val stampPresent = schoolRow?.get(SchoolsTable.onboardedAt) != null

    // The final REVIEW step only reads as done when the school was ACTUALLY taken
    // through onboarding: the stamp must be backed by the substantive prerequisite
    // data (a named school + at least one class). A bare stamp on an otherwise
    // empty row is treated as stale and does NOT mark the step done.
    val reviewDone = stampPresent && basicDone && academicDone

    val steps = listOf(
        OnboardingStepStatus("BASIC", 1, basicDone),
        OnboardingStepStatus("BRANDING", 2, brandingDone),
        OnboardingStepStatus("ACADEMIC", 3, academicDone),
        OnboardingStepStatus("REVIEW", 4, reviewDone)
    )
    val doneCount = steps.count { it.isDone }

    // Completion requires the substantive steps to genuinely exist, not just the
    // timestamp. BRANDING is best-effort (logo/brand color), so it never blocks.
    val isComplete = basicDone && academicDone && reviewDone

    // Resume at the first NON-branding step that is still missing; if only the
    // final stamp is missing, resume at REVIEW.
    val resume = when {
        !basicDone -> "BASIC"
        !academicDone -> "ACADEMIC"
        !reviewDone -> "REVIEW"
        else -> "REVIEW"
    }

    OnboardingStatusResponse(
        schoolId = schoolId?.toString(),
        isComplete = isComplete,
        completionPercent = (doneCount * 100) / steps.size,
        resumeStep = resume,
        totalStepCount = steps.size,
        steps = steps
    )
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
                        // RA-60: the REVIEW step previously rendered FABRICATED
                        // compliance docs ("Affiliation Cert", "Building Safety")
                        // and a fixed module list with no backing table. There is
                        // no document-upload step in onboarding and no compliance
                        // table, so compliance_docs is now an honest empty list
                        // (the UI shows a zero-state instead of fake verified rows).
                        // The module list is DERIVED from the school's REAL
                        // onboarding state: a module reads as configured only when
                        // the school actually set up its prerequisite data.
                        val schoolId = resolveSchoolIdForUser(uid)
                        val school = schoolId?.let {
                            dbQuery { SchoolsTable.selectAll().where { SchoolsTable.id eq it }.singleOrNull() }
                        }
                        val identity = ReviewIdentity(
                            institutionName = school?.get(SchoolsTable.name) ?: "—",
                            isVerified = (school?.get(SchoolsTable.onboardedAt) != null)
                        )

                        // Real onboarding signals (school-scoped). All counts are
                        // 0 when the school has not been created yet.
                        val (hasClasses, hasSubjects, hasMedia) = if (schoolId == null) {
                            Triple(false, false, false)
                        } else dbQuery {
                            val classCount = SchoolClassesTable.selectAll()
                                .where { SchoolClassesTable.schoolId eq schoolId }.count()
                            val classIds = SchoolClassesTable.selectAll()
                                .where { SchoolClassesTable.schoolId eq schoolId }
                                .map { it[SchoolClassesTable.id].value }
                            val subjectCount = if (classIds.isEmpty()) 0L else
                                SchoolSubjectsTable.selectAll()
                                    .where {
                                        classIds.map { cid -> SchoolSubjectsTable.classId eq cid }
                                            .reduce { acc, op -> acc or op }
                                    }.count()
                            val mediaCount = SchoolMediaTable.selectAll()
                                .where { SchoolMediaTable.schoolId eq schoolId }.count()
                            Triple(classCount > 0, subjectCount > 0, mediaCount > 0)
                        }

                        // compliance_docs: no backing source → honest empty.
                        val docs = emptyList<ReviewComplianceDoc>()
                        // modules: derived from real setup state, not fabricated.
                        //  - Analytics is configured once the school has classes
                        //    (the analytics surface needs class/exam data).
                        //  - Branding is configured once any media is uploaded.
                        //  - Academics is configured once subjects exist.
                        val modules = listOf(
                            ReviewModule("Academic structure", hasSubjects),
                            ReviewModule("Analytics", hasClasses),
                            ReviewModule("Branding & media", hasMedia)
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

                // 2. Step-specific persistence into REAL tables (not just drafts).
                //    - BASIC/BRANDING: create the school early (so ACADEMIC has a
                //      school to attach classes to) and sync its fields.
                //    - ACADEMIC: persist school_classes + school_subjects.
                //    - REVIEW(final): stamp onboarded_at to flip status to COMPLETED.
                val complete = req.isFinalSubmission && step == "REVIEW"
                dbQuery {
                    when (step) {
                        "BASIC", "BRANDING" -> {
                            val sid = ensureSchoolForUser(uid)
                            syncSchoolBasics(sid, uid)
                        }
                        "ACADEMIC" -> {
                            val sid = ensureSchoolForUser(uid)
                            syncSchoolBasics(sid, uid)
                            persistAcademicStructure(sid, req.dataPayload)
                        }
                        "REVIEW" -> {
                            val sid = ensureSchoolForUser(uid)
                            syncSchoolBasics(sid, uid)
                            // Safety net: if the client skipped persisting classes,
                            // seed defaults so a "completed" school is never empty.
                            val hasClasses = SchoolClassesTable.selectAll()
                                .where { SchoolClassesTable.schoolId eq sid }
                                .count() > 0L
                            if (!hasClasses) persistAcademicStructure(sid, JsonObject(emptyMap()))

                            if (complete) {
                                val now = Instant.now()
                                SchoolsTable.update({ SchoolsTable.id eq sid }) {
                                    it[onboardedAt] = now
                                    it[onboardingStatus] = "active"
                                    it[updatedAt] = now
                                }
                                AppUsersTable.update({ AppUsersTable.id eq uid }) {
                                    it[profileCompleted] = true
                                    it[updatedAt] = now
                                }
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

            // -------- GET /status --------
            // Server-truth onboarding state for the calling admin. The client
            // post-login gate reads this to decide dashboard vs onboarding and
            // to resume a returning admin at the first incomplete step. Honest:
            // an account that has not created a school yet reads as 0% with
            // resume_step = BASIC (no fabricated progress).
            get("/status") {
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                call.ok(computeOnboardingStatus(uid), message = "Onboarding status fetched")
            }

            // -------- POST /complete --------
            // Idempotently finalizes onboarding: ensures the school exists, seeds
            // a default academic structure if the admin skipped it, stamps
            // schools.onboarded_at and app_users.profile_completed=true so the
            // gate resolves to the dashboard permanently. Mirrors the REVIEW
            // final-submit path so a client can complete from either entry point.
            post("/complete") {
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val sid = dbQuery {
                    val schoolId = ensureSchoolForUser(uid)
                    syncSchoolBasics(schoolId, uid)
                    val hasClasses = SchoolClassesTable.selectAll()
                        .where { SchoolClassesTable.schoolId eq schoolId }
                        .count() > 0L
                    if (!hasClasses) persistAcademicStructure(schoolId, JsonObject(emptyMap()))
                    val now = Instant.now()
                    SchoolsTable.update({ SchoolsTable.id eq schoolId }) {
                        it[onboardedAt] = now
                        it[onboardingStatus] = "active"
                        it[updatedAt] = now
                    }
                    AppUsersTable.update({ AppUsersTable.id eq uid }) {
                        it[profileCompleted] = true
                        it[updatedAt] = now
                    }
                    schoolId
                }
                call.ok(
                    CompletionResponse(
                        schoolId = sid.toString(),
                        isComplete = true,
                        onboardingStatus = "active"
                    ),
                    message = "Onboarding completed"
                )
            }
        }
    }
}
