/*
 * File: ParentOnboardingRouting.kt
 * Module: feature.parent
 *
 * Endpoints (Module: Child Onboarding):
 *   GET  /api/v1/parent/onboarding/metadata             (JWT — Step 1 metadata)
 *   POST /api/v1/parent/onboarding/child-info           (JWT — Step 1 submit)
 *   GET  /api/v1/parent/onboarding/preference-options   (JWT — Step 2 metadata)
 *
 * Spec ref: parent_api_spec.artifact.md §Module: Child Onboarding
 *
 * Data sources:
 *   - app_config (KV) for screen_config / available_grades / available_interests /
 *     available_boards / available_focus_areas / budget_config — these are seeded
 *     by ParentCmsSeed so operators can edit them in Supabase without redeploys.
 *   - children table for the actual write on POST /child-info.
 */
package com.littlebridge.vidyaprayag.feature.parent

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppConfigTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.util.UUID

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

// ---------- Step 1: metadata ----------

@Serializable
data class ScreenConfig(
    @SerialName("header_title") val headerTitle: String,
    @SerialName("header_subtitle") val headerSubtitle: String,
    @SerialName("progress_label") val progressLabel: String,
    @SerialName("progress_value") val progressValue: Double
)

@Serializable
data class OnboardingMetadataResponse(
    @SerialName("screen_config") val screenConfig: ScreenConfig,
    @SerialName("available_grades") val availableGrades: List<String>,
    @SerialName("available_interests") val availableInterests: List<String>,
    @SerialName("footer_text") val footerText: String
)

// ---------- Step 1: submit ----------

@Serializable
data class ChildInfoRequest(
    @SerialName("child_name") val childName: String,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerialName("current_grade") val currentGrade: String? = null,
    val interests: List<String> = emptyList()
)

@Serializable
data class ChildInfoResponse(
    @SerialName("child_id") val childId: String,
    @SerialName("child_name") val childName: String,
    @SerialName("next_step") val nextStep: String
)

// ---------- Step 2: preference options ----------

@Serializable
data class FocusArea(
    val id: String,
    val title: String,
    val icon: String
)

@Serializable
data class BudgetConfig(
    @SerialName("min_value") val minValue: Int,
    @SerialName("max_value") val maxValue: Int,
    @SerialName("default_range") val defaultRange: List<Int>,
    @SerialName("currency_symbol") val currencySymbol: String
)

@Serializable
data class PreferenceOptionsResponse(
    @SerialName("available_boards") val availableBoards: List<String>,
    @SerialName("available_focus_areas") val availableFocusAreas: List<FocusArea>,
    @SerialName("budget_config") val budgetConfig: BudgetConfig
)

// ---------- helpers ----------

/** Reads a KV row from app_config and returns its raw JSON string, or null. */
private suspend fun readConfig(key: String): String? = dbQuery {
    AppConfigTable.selectAll()
        .where { AppConfigTable.key eq key }
        .singleOrNull()
        ?.get(AppConfigTable.value)
}

private fun parseStringArray(raw: String?, fallback: List<String>): List<String> {
    if (raw.isNullOrBlank()) return fallback
    return runCatching {
        lenientJson.parseToJsonElement(raw).jsonArray.map { (it as JsonPrimitive).content }
    }.getOrDefault(fallback)
}

// ---------- routing ----------

fun Route.parentOnboardingRouting() {
    authenticate("jwt") {
        route("/api/v1/parent/onboarding") {

            // ---- GET /metadata ----
            get("/metadata") {
                val screenCfgRaw = readConfig("parent_onboarding_step1_screen_config")
                val gradesRaw    = readConfig("parent_available_grades")
                val interestsRaw = readConfig("parent_available_interests")
                val footerRaw    = readConfig("parent_onboarding_footer_text")

                val screenConfig: ScreenConfig = screenCfgRaw?.let {
                    runCatching { lenientJson.decodeFromString(ScreenConfig.serializer(), it) }.getOrNull()
                } ?: ScreenConfig(
                    headerTitle = "Let's build a profile for your child.",
                    headerSubtitle = "We use this information to curate the best learning path.",
                    progressLabel = "Step 1 of 3",
                    progressValue = 0.33
                )

                val response = OnboardingMetadataResponse(
                    screenConfig = screenConfig,
                    availableGrades = parseStringArray(
                        gradesRaw,
                        listOf("Grade 1", "Grade 2", "Grade 3", "Nursery", "KG")
                    ),
                    availableInterests = parseStringArray(
                        interestsRaw,
                        listOf("Music", "Art", "STEM", "Sports", "Languages")
                    ),
                    footerText = footerRaw?.trim('"') ?: "Your data is encrypted and secure."
                )
                call.ok(response, message = "Onboarding metadata fetched")
            }

            // ---- POST /child-info ----
            post("/child-info") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = runCatching { call.receive<ChildInfoRequest>() }.getOrNull() ?: run {
                    call.fail("Invalid request body"); return@post
                }
                if (req.childName.isBlank()) {
                    call.fail("child_name is required"); return@post
                }

                val now = Instant.now()
                val newId = UUID.randomUUID()

                val interestsJson = buildJsonArray { req.interests.forEach { add(JsonPrimitive(it)) } }.toString()

                dbQuery {
                    ChildrenTable.insert {
                        it[id] = newId
                        it[parentId] = uid
                        it[childName] = req.childName
                        it[dateOfBirth] = req.dateOfBirth
                        it[gender] = req.gender?.uppercase()
                        it[currentGrade] = req.currentGrade
                        it[interests] = interestsJson
                        it[overallProgress] = 0.0
                        it[currentLevel] = 1
                        it[attendanceStatus] = "PRESENT"
                        it[isActive] = true
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }

                call.created(
                    ChildInfoResponse(
                        childId = newId.toString(),
                        childName = req.childName,
                        nextStep = "PREFERENCES"
                    ),
                    message = "Child profile created successfully"
                )
            }

            // ---- GET /preference-options ----
            get("/preference-options") {
                val boardsRaw  = readConfig("parent_available_boards")
                val focusRaw   = readConfig("parent_available_focus_areas")
                val budgetRaw  = readConfig("parent_budget_config")

                val boards = parseStringArray(
                    boardsRaw,
                    listOf("CBSE", "ICSE", "IB", "State Board")
                )
                val focusAreas: List<FocusArea> = focusRaw?.let {
                    runCatching {
                        lenientJson.decodeFromString(
                            kotlinx.serialization.builtins.ListSerializer(FocusArea.serializer()), it
                        )
                    }.getOrNull()
                } ?: listOf(
                    FocusArea("acad", "Academics", "school"),
                    FocusArea("sports", "Sports", "sports_soccer")
                )
                val budget: BudgetConfig = budgetRaw?.let {
                    runCatching { lenientJson.decodeFromString(BudgetConfig.serializer(), it) }.getOrNull()
                } ?: BudgetConfig(
                    minValue = 0,
                    maxValue = 10000,
                    defaultRange = listOf(2000, 5000),
                    currencySymbol = "$"
                )

                call.ok(
                    PreferenceOptionsResponse(
                        availableBoards = boards,
                        availableFocusAreas = focusAreas,
                        budgetConfig = budget
                    ),
                    message = "Preference options fetched"
                )
            }
        }
    }
}
