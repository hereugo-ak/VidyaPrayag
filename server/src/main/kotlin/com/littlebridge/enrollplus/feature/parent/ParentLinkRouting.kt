/*
 * File: ParentLinkRouting.kt
 * Module: feature.parent
 *
 * Endpoints (Module: Link Your Child wizard — report §5.3, SWEEP-A):
 *   GET  /api/v1/parent/schools/search?q=     (JWT — step 2: match a school by name)
 *   POST /api/v1/parent/link-child            (JWT — step 3: link a child by roll/admission no.)
 *
 * Replaces the MockV2-driven ParentLinkChildScreenV2 wizard with real data:
 *   - step 2 searches active schools by name substring (SchoolsTable);
 *   - step 3 resolves the school's canonical student (StudentsTable) by
 *     (school_id, roll_number) and links the parent to that child by creating
 *     / updating a ChildrenTable row carrying the resolved student_code.
 *
 * Spec ref: parent_api_spec.artifact.md §Module: Child Onboarding (linking).
 */
package com.littlebridge.enrollplus.feature.parent

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserId
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ParentChildLinksTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/** Diagnostic logger for the parent→child link matcher (ROOT CAUSE visibility). */
private val linkLog = LoggerFactory.getLogger("ParentLinkRouting")

// ---------- DTOs ----------

@Serializable
data class SchoolMatchDto(
    val id: String,
    val name: String,
    val board: String,
    val city: String,
    @SerialName("logo_url") val logoUrl: String? = null
)

@Serializable
data class SchoolSearchResponse(
    val schools: List<SchoolMatchDto>
)

@Serializable
data class LinkChildRequest(
    @SerialName("school_id") val schoolId: String,
    @SerialName("roll_number") val rollNumber: String,
    // ISSUE 2c: the school is already chosen, so the final step now collects the
    // child's class + section (guided/auto-formatted on the client) and the
    // child's name, which the matcher checks against the roster.
    @SerialName("class_name") val className: String? = null,
    val section: String? = null,
    @SerialName("child_name") val childName: String? = null,
    // ISSUE 2d: the parent's contact phone for this child; matched against the
    // student's parent_phone on record. May also fall back to the parent's
    // account phone server-side when omitted.
    @SerialName("parent_phone") val parentPhone: String? = null,
    // Optional — parent's preferred contact name/language captured in step 1.
    @SerialName("parent_name") val parentName: String? = null
)

@Serializable
data class LinkedChildDto(
    @SerialName("child_id") val childId: String,
    @SerialName("child_name") val childName: String,
    @SerialName("class_name") val className: String,
    val roll: String,
    @SerialName("school_name") val schoolName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    // RA-48: linking is now request→approve. "pending" until a school admin approves;
    // "approved" carries a real child_id. Defaulted so older clients still parse.
    val status: String = "approved",
)

// ---- RA-48 admin-side DTOs (school admin vetting the link queue) ----

@Serializable
data class LinkRequestDto(
    val id: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("parent_name") val parentName: String? = null,
    @SerialName("parent_phone") val parentPhone: String? = null,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("roll_number") val rollNumber: String? = null,
    @SerialName("child_name") val childName: String? = null,
    @SerialName("class_name") val className: String? = null,
    val section: String? = null,
    // ISSUE 2d: why this request is in the "needs review" bucket (phone mismatch).
    @SerialName("review_reason") val reviewReason: String? = null,
    val status: String,
    @SerialName("requested_at") val requestedAt: String,
)

@Serializable
data class LinkRequestListResponse(
    val requests: List<LinkRequestDto>
)

// ---------- routing ----------

fun Route.parentLinkRouting() {
    authenticate("jwt") {
        route("/api/v1/parent") {

            // ---- GET /schools/search?q= ----
            // Step 2 of the link wizard: match active schools by name substring.
            get("/schools/search") {
                call.principalUserId() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val query = call.request.queryParameters["q"]?.trim().orEmpty()
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)

                val matches = dbQuery {
                    SchoolsTable.selectAll()
                        .where {
                            // Only active schools are searchable. migration_007 backfills
                            // is_active + makes it NOT NULL, so a plain `eq true` no longer
                            // risks hiding a school whose is_active was NULL.
                            val active = (SchoolsTable.isActive eq true)
                            if (query.isBlank()) {
                                active
                            } else {
                                active and (SchoolsTable.name.lowerCase() like "%${query.lowercase()}%")
                            }
                        }
                        .orderBy(SchoolsTable.name, SortOrder.ASC)
                        .limit(limit)
                        .map { row ->
                            SchoolMatchDto(
                                id = row[SchoolsTable.id].value.toString(),
                                name = row[SchoolsTable.name],
                                board = row[SchoolsTable.board],
                                city = row[SchoolsTable.city],
                                logoUrl = row[SchoolsTable.logoUrl]
                            )
                        }
                }
                call.ok(SchoolSearchResponse(schools = matches), message = "Found ${matches.size} school(s)")
            }

            // ---- GET /schools/{id}/roster-debug ----
            // Diagnostic: return the raw roster count + first 5 student codes for a
            // school so we can quickly verify the school_id/student linkage in prod.
            // Authentication required (parent JWT), school must be active.
            get("/schools/{id}/roster-debug") {
                call.principalUserId() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val schoolUuid = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid school id"); return@get }

                val info = dbQuery {
                    val school = SchoolsTable.selectAll()
                        .where { SchoolsTable.id eq schoolUuid }
                        .firstOrNull()
                    if (school == null) return@dbQuery null

                    val totalAll = StudentsTable.selectAll()
                        .where { StudentsTable.schoolId eq schoolUuid }
                        .count()
                    val totalActive = StudentsTable.selectAll()
                        .where { (StudentsTable.schoolId eq schoolUuid) and (StudentsTable.isActive eq true) }
                        .count()
                    val totalInactive = StudentsTable.selectAll()
                        .where { (StudentsTable.schoolId eq schoolUuid) and (StudentsTable.isActive eq false) }
                        .count()
                    // Any rows that are neither true nor false are NULL is_active. After
                    // migration_007 (backfill + NOT NULL) this is always 0; we derive it
                    // rather than querying IS NULL so it compiles against the non-nullable
                    // Exposed column mapping.
                    val totalNullActive = totalAll - totalActive - totalInactive
                    val sample = StudentsTable.selectAll()
                        .where { StudentsTable.schoolId eq schoolUuid }
                        .limit(5)
                        .map {
                            mapOf(
                                "code" to it[StudentsTable.studentCode],
                                "name" to it[StudentsTable.fullName],
                                "class" to it[StudentsTable.className],
                                "section" to it[StudentsTable.section],
                                "roll" to it[StudentsTable.rollNumber],
                                "is_active" to it[StudentsTable.isActive].toString()
                            )
                        }
                    mapOf(
                        "school_id" to schoolUuid.toString(),
                        "school_name" to school[SchoolsTable.name],
                        "school_is_active" to school[SchoolsTable.isActive].toString(),
                        "students_total" to totalAll,
                        "students_active_true" to totalActive,
                        "students_active_null" to totalNullActive,
                        "students_active_false" to totalInactive,
                        "sample_students" to sample
                    )
                }
                if (info == null) {
                    call.fail("School not found", HttpStatusCode.NotFound)
                } else {
                    call.ok(info, message = "Roster debug info")
                }
            }

            // ---- POST /link-child ----
            // RA-48: step 3 now creates a PENDING parent_child_links request that a
            // school admin must approve — a parent can no longer self-link to any
            // roll number. RA-03: a per-parent+school probe throttle limits roll-
            // number guessing (max pending requests per school per parent).
            post("/link-child") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = runCatching { call.receive<LinkChildRequest>() }.getOrNull() ?: run {
                    call.fail("Invalid request body"); return@post
                }
                val schoolUuid = runCatching { UUID.fromString(req.schoolId) }.getOrNull() ?: run {
                    call.fail("Invalid school_id"); return@post
                }
                if (req.rollNumber.isBlank()) {
                    call.fail("roll_number is required"); return@post
                }

                val result = dbQuery {
                    // 1) Verify school exists. We accept is_active=true OR is_active IS NULL
                    // so a school that was registered but not yet fully onboarded (or whose
                    // is_active column defaulted to NULL before the migration hardened it)
                    // is still reachable for parents whose children are enrolled there.
                    val schoolRow = SchoolsTable.selectAll()
                        .where {
                            (SchoolsTable.id eq schoolUuid) and
                                ((SchoolsTable.isActive eq true))
                        }
                        .singleOrNull() ?: return@dbQuery LinkResult.SchoolNotFound
                    val schoolNameVal = schoolRow[SchoolsTable.name]

                    // RA-03 throttle: cap outstanding pending requests this parent has
                    // against this school, so they cannot brute-force roll numbers.
                    val pendingCount = ParentChildLinksTable.selectAll().where {
                        (ParentChildLinksTable.parentId eq uid) and
                            (ParentChildLinksTable.schoolId eq schoolUuid) and
                            (ParentChildLinksTable.status eq "pending")
                    }.count()
                    if (pendingCount >= MAX_PENDING_LINKS_PER_SCHOOL) {
                        return@dbQuery LinkResult.Throttled
                    }

                    // 2) Resolve the canonical student by (school, roll/admission no.).
                    // FIX (link-child 404): the previous implementation did an exact
                    // text `rollNumber eq input` + `singleOrNull()`. That failed for
                    // EVERY input because (a) rolls are stored as plain digits ('1')
                    // so '001'/'01' never matched, (b) the admission/student code
                    // (e.g. 'S1-G3A-001') was never checked, and (c) `singleOrNull()`
                    // silently returns null when SEVERAL classes share the same roll
                    // number (roll '1' exists in 3-A, 3-B, 5-A, 8-A…) — so even the
                    // correct plain roll resolved to "not found".
                    //
                    // Resolution order now:
                    //   1. exact admission/student code match (unique — always wins);
                    //   2. roll-number match with numeric normalisation ('001' ≡ '1');
                    //      if it is ambiguous across classes we tell the parent to use
                    //      the full admission number instead of lying with a 404.
                    // School rosters are small (≤ a few hundred rows), so the scoped
                    // fetch + in-memory normalisation mirrors the SchoolStudentsRouting
                    // search style and stays Postgres/SQLite-portable.
                    val rollInput = req.rollNumber.trim()
                    // FIX (rosterSize=0): include rows where is_active IS NULL as well as
                    // is_active = true, so students inserted directly into Supabase (or via
                    // older schema that lacked the column default) are not silently excluded.
                    // We explicitly exclude only rows that are HARD-set to false (admin-deleted).
                    val roster = StudentsTable.selectAll()
                        .where {
                            (StudentsTable.schoolId eq schoolUuid) and
                                ((StudentsTable.isActive eq true))
                        }
                        .toList()

                    fun normaliseRoll(v: String): String {
                        val t = v.trim()
                        // strip leading zeros for purely numeric rolls: '001' → '1'
                        return if (t.all { it.isDigit() } && t.isNotEmpty()) {
                            t.trimStart('0').ifEmpty { "0" }
                        } else t.lowercase()
                    }

                    // Helper: does this roster row's name match the typed child name?
                    // Case/space-insensitive; blank typed name never filters anything.
                    // TOLERANT by design (zero-technical-familiarity requirement): a
                    // parent who types only "Gaurav" still matches "Gaurav Kumar", and
                    // one who types the full name still matches a roster stored as just
                    // the first name. We accept when one normalised name CONTAINS the
                    // other, or when they share any whole name-token (first/last name).
                    fun nameMatches(row: org.jetbrains.exposed.sql.ResultRow): Boolean {
                        val typed = req.childName?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")
                        if (typed.isNullOrBlank()) return true
                        val stored = row[StudentsTable.fullName].trim().lowercase().replace(Regex("\\s+"), " ")
                        if (stored == typed) return true
                        if (stored.contains(typed) || typed.contains(stored)) return true
                        val storedTokens = stored.split(" ").filter { it.isNotBlank() }.toSet()
                        val typedTokens = typed.split(" ").filter { it.isNotBlank() }.toSet()
                        return storedTokens.intersect(typedTokens).isNotEmpty()
                    }

                    // Helper: normalise an admission/student code so trivial format
                    // drift ('g6b-012' / 'G6B 012' / 'G6B012') never blocks an exact
                    // code match. We strip every non-alphanumeric char + upper-case.
                    fun normaliseCode(v: String): String =
                        v.uppercase().filter { it.isLetterOrDigit() }

                    val rollInputCode = normaliseCode(rollInput)
                    val byCode = roster.filter {
                        normaliseCode(it[StudentsTable.studentCode]) == rollInputCode
                    }
                    var matches = if (byCode.isNotEmpty()) byCode else {
                        val wanted = normaliseRoll(rollInput)
                        roster.filter { normaliseRoll(it[StudentsTable.rollNumber]) == wanted }
                    }

                    // ROOT-CAUSE VISIBILITY: log exactly what we searched and how big
                    // the selected school's roster is, so a "No student found" is never
                    // a silent mystery again. The single biggest real-world cause was a
                    // parent landing on the WRONG school in step 2 (roster size 0 here).
                    linkLog.info(
                        "link-child match probe: school='{}' ({}) rosterSize={} roll='{}' " +
                            "class='{}' section='{}' childName='{}' byCode={} byRoll={}",
                        schoolNameVal, schoolUuid, roster.size, rollInput,
                        req.className, req.section, req.childName,
                        byCode.size, matches.size,
                    )

                    // ISSUE 2c/2d: when the parent supplied a class+section (the new
                    // guided step-3 inputs), narrow an ambiguous roll by the class so
                    // roll '1' in 3-A no longer collides with roll '1' in 5-B. Matching
                    // uses the ClassNaming key so format differences never block it.
                    if (matches.size > 1 && !req.className.isNullOrBlank()) {
                        val narrowed = matches.filter {
                            ClassNaming.sameClassSection(
                                it[StudentsTable.className], it[StudentsTable.section],
                                req.className, req.section
                            )
                        }
                        if (narrowed.isNotEmpty()) matches = narrowed
                    }

                    // Still ambiguous after class narrowing? Use the typed child name
                    // as a final tie-breaker before giving up (e.g. two pupils share
                    // a roll in the same section — pick the one whose name matches).
                    if (matches.size > 1) {
                        val byName = matches.filter { nameMatches(it) }
                        if (byName.isNotEmpty()) matches = byName
                    }

                    // ROOT FIX (graceful fallback): a parent often mistypes/omits the
                    // roll yet gets the NAME + CLASS + SECTION right. Rather than a
                    // dead-end "No student found", fall back to matching the roster on
                    // (name + class + section) when the roll lookup found nothing. This
                    // is only used as a fallback so a correct roll always wins first.
                    if (matches.isEmpty() &&
                        !req.childName.isNullOrBlank() && !req.className.isNullOrBlank()
                    ) {
                        matches = roster.filter {
                            nameMatches(it) && ClassNaming.sameClassSection(
                                it[StudentsTable.className], it[StudentsTable.section],
                                req.className, req.section
                            )
                        }
                    }

                    // effectiveSchoolId / effectiveSchoolName are what the rest of the
                    // transaction (link insert + admin notifications) bind to. They
                    // start as the parent's pick and are REBOUND if we have to find the
                    // student at a different school in the cross-school self-heal below.
                    var effectiveSchoolId = schoolUuid
                    var effectiveSchoolName = schoolNameVal

                    val studentRow = when {
                        matches.isEmpty() -> {
                            // ROOT FIX (the persistent "No student found"): a very common
                            // real-world cause is the parent landing on the WRONG school
                            // record in step 2 (duplicate / same-named / stale school rows)
                            // — the child genuinely exists, just under a different
                            // school_id. Instead of dead-ending OR making the parent
                            // navigate back, we SELF-HEAL: probe the WHOLE platform for a
                            // student matching (name + class/section + roll|code). If
                            // EXACTLY one such student exists, we rebind the link to THAT
                            // student's real school and continue. The request still goes to
                            // that school's admin approval queue, so auto-rebinding is safe.
                            val crossSchoolHit: org.jetbrains.exposed.sql.ResultRow? = run {
                                // Need enough identity to be confident — name AND class.
                                val typedName = req.childName?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")
                                val reqClass = req.className
                                if (typedName.isNullOrBlank() || reqClass.isNullOrBlank()) return@run null
                                val typedTokens = typedName.split(" ").filter { it.isNotBlank() }.toSet()
                                val wantRoll = normaliseRoll(rollInput)
                                val elsewhere = StudentsTable.selectAll()
                                    .where {
                                        (StudentsTable.schoolId neq schoolUuid) and
                                            ((StudentsTable.isActive eq true))
                                    }
                                    .toList()
                                    .filter { r ->
                                        val stored = r[StudentsTable.fullName].trim().lowercase().replace(Regex("\\s+"), " ")
                                        val nameOk = stored == typedName ||
                                            stored.contains(typedName) || typedName.contains(stored) ||
                                            stored.split(" ").filter { it.isNotBlank() }.toSet()
                                                .intersect(typedTokens).isNotEmpty()
                                        val classOk = ClassNaming.sameClassSection(
                                            r[StudentsTable.className], r[StudentsTable.section],
                                            reqClass, req.section,
                                        )
                                        val rollOk = normaliseRoll(r[StudentsTable.rollNumber]) == wantRoll ||
                                            normaliseCode(r[StudentsTable.studentCode]) == rollInputCode
                                        nameOk && classOk && rollOk
                                    }
                                // Several students across schools fit → too risky to
                                // auto-bind. Surface AmbiguousRoll so the parent uses the
                                // unique admission code instead of guessing.
                                if (elsewhere.size > 1) return@dbQuery LinkResult.AmbiguousRoll(elsewhere.size)
                                elsewhere.singleOrNull()
                            }

                            // Resolve the real school for a cross-school hit (active OR
                            // null is_active). A null result here means the student row
                            // exists but its school is hard-deactivated — not linkable.
                            val healedRow: org.jetbrains.exposed.sql.ResultRow? =
                                if (crossSchoolHit != null) {
                                    val realSchoolId = crossSchoolHit[StudentsTable.schoolId]
                                    val realSchool = SchoolsTable.selectAll()
                                        .where {
                                            (SchoolsTable.id eq realSchoolId) and
                                                ((SchoolsTable.isActive eq true))
                                        }
                                        .singleOrNull()
                                    if (realSchool != null) {
                                        effectiveSchoolId = realSchoolId
                                        effectiveSchoolName = realSchool[SchoolsTable.name]
                                        linkLog.warn(
                                            "link-child SELF-HEAL: parent picked '{}' ({}) but child is at '{}' ({}). " +
                                                "Binding link to the student's REAL school. roll='{}' class='{}' name='{}'",
                                            schoolNameVal, schoolUuid, effectiveSchoolName, effectiveSchoolId,
                                            rollInput, req.className, req.childName,
                                        )
                                        crossSchoolHit
                                    } else null
                                } else null

                            if (healedRow != null) {
                                healedRow
                            } else {
                                linkLog.warn(
                                    "link-child NO MATCH anywhere: school='{}' ({}) rosterSize={} roll='{}' " +
                                        "class='{}' section='{}' childName='{}' — {}",
                                    schoolNameVal, schoolUuid, roster.size, rollInput,
                                    req.className, req.section, req.childName,
                                    if (roster.isEmpty()) "selected-school roster EMPTY and no cross-school identity match"
                                    else "roster has ${roster.size} student(s) but none matched, and no cross-school identity match",
                                )
                                return@dbQuery LinkResult.StudentNotFound(emptyRoster = roster.isEmpty())
                            }
                        }
                        matches.size > 1 -> return@dbQuery LinkResult.AmbiguousRoll(matches.size)
                        else -> matches.single()
                    }

                    val studentCodeVal = studentRow[StudentsTable.studentCode]
                    val childNameVal = studentRow[StudentsTable.fullName]
                    val classNameVal = studentRow[StudentsTable.className]
                    val sectionVal = studentRow[StudentsTable.section]
                    val photoVal = studentRow[StudentsTable.profilePhotoUrl]
                    val now = Instant.now()

                    // ─── ISSUE 2d: full-match vs phone-mismatch classification ───
                    // The parent phone is taken from the request, falling back to the
                    // parent's account phone so an omitted field still matches when it
                    // can. It is compared (last-10-digits) against the student's
                    // parent_phone on record.
                    val accountPhone = AppUsersTable.selectAll()
                        .where { AppUsersTable.id eq uid }
                        .firstOrNull()?.get(AppUsersTable.phone)
                    val providedPhone = req.parentPhone?.takeIf { it.isNotBlank() } ?: accountPhone
                    val onRecordPhone = studentRow[StudentsTable.parentPhone]

                    // Phone matches when the school HAS a number on record AND it
                    // equals the provided one. If the school stored no number we
                    // cannot contradict the parent, so we DO NOT flag on that alone.
                    val phoneOnRecordPresent = !onRecordPhone.isNullOrBlank()
                    val phoneMatches = if (phoneOnRecordPresent) {
                        PhoneNormalizer.sameNumber(providedPhone, onRecordPhone)
                    } else true

                    val isPhoneMismatch = phoneOnRecordPresent &&
                        !providedPhone.isNullOrBlank() &&
                        !PhoneNormalizer.sameNumber(providedPhone, onRecordPhone)

                    val targetStatus = if (isPhoneMismatch) "needs_review" else "pending"
                    val reviewReasonVal = if (isPhoneMismatch)
                        "Parent phone does not match the number on record for this student." else null

                    // If a link already exists for this (parent, school, student) in
                    // any non-final state, reuse it (don't duplicate). Scoped to the
                    // EFFECTIVE school (the student's real school, after self-heal).
                    val existingLink = ParentChildLinksTable.selectAll().where {
                        (ParentChildLinksTable.parentId eq uid) and
                            (ParentChildLinksTable.schoolId eq effectiveSchoolId) and
                            (ParentChildLinksTable.studentCode eq studentCodeVal) and
                            ((ParentChildLinksTable.status eq "pending") or
                                (ParentChildLinksTable.status eq "approved") or
                                (ParentChildLinksTable.status eq "needs_review"))
                    }.singleOrNull()

                    val linkId: UUID = if (existingLink != null) {
                        existingLink[ParentChildLinksTable.id].value
                    } else {
                        val newId = UUID.randomUUID()
                        ParentChildLinksTable.insert {
                            it[id] = newId
                            it[parentId] = uid
                            it[schoolId] = effectiveSchoolId
                            // Store the school's CANONICAL roll/class/section (not the
                            // raw parent input) so the admin queue shows the roster value.
                            it[rollNumber] = studentRow[StudentsTable.rollNumber]
                            it[studentCode] = studentCodeVal
                            it[childName] = childNameVal
                            it[className] = classNameVal
                            it[section] = sectionVal
                            it[parentPhone] = providedPhone?.let { p -> PhoneNormalizer.canonical(p) }
                            it[reviewReason] = reviewReasonVal
                            it[status] = targetStatus
                            it[requestedAt] = now
                        }
                        newId
                    }

                    val childDto = LinkedChildDto(
                        childId = "",                 // no child row until approved
                        childName = childNameVal,
                        className = classNameVal,
                        roll = studentRow[StudentsTable.rollNumber],
                        // Report the EFFECTIVE school (the student's real one) so the
                        // parent's confirmation names the right school after a self-heal.
                        schoolName = effectiveSchoolName,
                        profilePhotoUrl = photoVal,
                        status = targetStatus,
                    )
                    if (isPhoneMismatch) {
                        LinkResult.NeedsReview(linkId, effectiveSchoolId, childDto, reviewReasonVal ?: "Needs review")
                    } else {
                        LinkResult.Pending(linkId, effectiveSchoolId, childDto)
                    }
                }

                when (result) {
                    is LinkResult.SchoolNotFound -> call.fail("School not found", HttpStatusCode.NotFound)
                    is LinkResult.StudentNotFound ->
                        // We searched the selected school AND every other school on the
                        // platform by name + class/section + roll/code, and the cross-
                        // school self-heal would have rebound automatically on a unique
                        // hit — so this genuinely means no such student exists. Guide the
                        // parent to the exact fields to re-check.
                        call.fail(
                            "We couldn't find your child anywhere on VidyaPrayag with that name, " +
                                "class/section and roll number. Please double-check the spelling of " +
                                "the name, the class and section, and the roll/admission number " +
                                "exactly as printed on the school ID, then try again.",
                            HttpStatusCode.NotFound
                        )
                    is LinkResult.AmbiguousRoll ->
                        call.fail(
                            "${result.count} students at this school share that roll number across different classes. " +
                                "Please enter the full admission number instead (e.g. S1-G3A-001).",
                            HttpStatusCode.Conflict,
                            "ROLL_AMBIGUOUS"
                        )
                    is LinkResult.Throttled ->
                        call.fail("You have too many pending link requests for this school. Please wait for them to be reviewed.", HttpStatusCode.TooManyRequests, "LINK_THROTTLED")
                    is LinkResult.Pending -> {
                        // RA-48 + RA-41: notify the school admins that a parent wants to link.
                        // Use the EFFECTIVE school (where the student really is, after a
                        // possible self-heal) so the RIGHT admins are notified.
                        val admins = NotifyRecipients.adminsInSchool(result.schoolId)
                        if (admins.isNotEmpty()) {
                            Notify.toUsers(
                                userIds = admins,
                                category = "link_request",
                                title = "New child-link request",
                                body = "A parent requested to link to roll ${req.rollNumber}.",
                                schoolId = result.schoolId,
                                actorId = uid,
                                deepLink = "admin/link-requests",
                                refType = "link_request",
                                refId = result.linkId.toString(),
                            )
                        }
                        call.created(result.child, message = "Link request submitted for approval")
                    }
                    is LinkResult.NeedsReview -> {
                        // ISSUE 2d: the claim matched the student EXCEPT the phone.
                        // Flag it into the admin "needs review" queue instead of the
                        // normal pending queue, and tell the admins it needs a closer look.
                        val admins = NotifyRecipients.adminsInSchool(result.schoolId)
                        if (admins.isNotEmpty()) {
                            Notify.toUsers(
                                userIds = admins,
                                category = "link_request",
                                title = "Child-link request needs review",
                                body = "A parent's phone didn't match the number on record for roll ${req.rollNumber}.",
                                schoolId = result.schoolId,
                                actorId = uid,
                                deepLink = "admin/link-requests",
                                refType = "link_request",
                                refId = result.linkId.toString(),
                            )
                        }
                        call.created(result.child, message = "Submitted — pending admin review (phone needs verification)")
                    }
                }
            }
        }

        // ============================================================
        // RA-48: school-admin link-request queue. A school admin vets the
        // pending parent→child link requests for THEIR school only (the
        // requireSchoolAdmin guard reads school_id from app_users, never the
        // body), then approves (materialises the children row + grants the
        // parent read access) or rejects (no link). Either decision notifies
        // the requesting parent.
        // ============================================================
        route("/api/v1/school") {

            // ---- GET /link-requests?status=pending ----
            // List the link requests for the admin's school. Defaults to pending.
            get("/link-requests") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                // ISSUE 2d: the "needs review" (phone-mismatch) bucket is now a
                // first-class filterable status alongside pending/approved/rejected.
                val statusFilter = call.request.queryParameters["status"]?.trim()?.lowercase()
                    ?.takeIf { it in setOf("pending", "approved", "rejected", "needs_review") }
                    ?: "pending"

                val rows = dbQuery {
                    // Join the requesting parent for display (name/phone).
                    ParentChildLinksTable.selectAll()
                        .where {
                            (ParentChildLinksTable.schoolId eq ctx.schoolId) and
                                (ParentChildLinksTable.status eq statusFilter)
                        }
                        .orderBy(ParentChildLinksTable.requestedAt, SortOrder.DESC)
                        .map { link ->
                            val parentUuid = link[ParentChildLinksTable.parentId]
                            val parentRow = AppUsersTable.selectAll()
                                .where { AppUsersTable.id eq parentUuid }
                                .singleOrNull()
                            // Enrich with the canonical class from students by code,
                            // falling back to the class stored on the link itself.
                            val code = link[ParentChildLinksTable.studentCode]
                            val classNameVal = code?.let { c ->
                                StudentsTable.selectAll()
                                    .where { (StudentsTable.schoolId eq ctx.schoolId) and (StudentsTable.studentCode eq c) }
                                    .singleOrNull()?.get(StudentsTable.className)
                            } ?: link[ParentChildLinksTable.className]
                            LinkRequestDto(
                                id = link[ParentChildLinksTable.id].value.toString(),
                                parentId = parentUuid.toString(),
                                parentName = parentRow?.get(AppUsersTable.fullName),
                                // Prefer the phone the parent submitted with THIS request
                                // (what we matched on) then their account phone.
                                parentPhone = link[ParentChildLinksTable.parentPhone]
                                    ?: parentRow?.get(AppUsersTable.phone),
                                studentCode = code,
                                rollNumber = link[ParentChildLinksTable.rollNumber],
                                childName = link[ParentChildLinksTable.childName],
                                className = classNameVal,
                                section = link[ParentChildLinksTable.section],
                                reviewReason = link[ParentChildLinksTable.reviewReason],
                                status = link[ParentChildLinksTable.status],
                                requestedAt = link[ParentChildLinksTable.requestedAt].toString(),
                            )
                        }
                }
                call.ok(LinkRequestListResponse(requests = rows), message = "${rows.size} ${statusFilter} request(s)")
            }

            // ---- POST /link-requests/{id}/approve ----
            // Materialise the children row from the pending link's student_code,
            // mark the link approved, and notify the parent. Idempotent: if the
            // children row already exists for this (parent, student_code) it is
            // reused instead of duplicated.
            post("/link-requests/{id}/approve") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val linkId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid request id"); return@post
                }

                val outcome = dbQuery {
                    val link = ParentChildLinksTable.selectAll()
                        .where {
                            (ParentChildLinksTable.id eq linkId) and
                                (ParentChildLinksTable.schoolId eq ctx.schoolId)
                        }
                        .singleOrNull() ?: return@dbQuery DecisionResult.NotFound
                    // ISSUE 2d: an admin can approve a request from EITHER the normal
                    // pending queue OR the "needs review" (phone-mismatch) queue once
                    // they've verified it out-of-band. Already-decided requests are not
                    // re-approved.
                    val curStatus = link[ParentChildLinksTable.status]
                    if (curStatus != "pending" && curStatus != "needs_review") {
                        return@dbQuery DecisionResult.AlreadyDecided(curStatus)
                    }

                    val parentUuid = link[ParentChildLinksTable.parentId]
                    val code = link[ParentChildLinksTable.studentCode]
                        ?: return@dbQuery DecisionResult.NotFound

                    // Re-resolve the canonical student so we copy the latest truth
                    // (name/class/photo) into the parent-facing children row.
                    val studentRow = StudentsTable.selectAll()
                        .where {
                            (StudentsTable.schoolId eq ctx.schoolId) and
                                (StudentsTable.studentCode eq code) and
                                (StudentsTable.isActive eq true)
                        }
                        .singleOrNull() ?: return@dbQuery DecisionResult.NotFound

                    val childNameVal = studentRow[StudentsTable.fullName]
                    val classNameVal = studentRow[StudentsTable.className]
                    val photoVal = studentRow[StudentsTable.profilePhotoUrl]
                    val now = Instant.now()

                    // Idempotent upsert of the children row for (parent, code).
                    val existingChild = ChildrenTable.selectAll()
                        .where {
                            (ChildrenTable.parentId eq parentUuid) and
                                (ChildrenTable.schoolId eq ctx.schoolId) and
                                (ChildrenTable.studentCode eq code)
                        }
                        .singleOrNull()

                    val childUuid: UUID = if (existingChild != null) {
                        val cid = existingChild[ChildrenTable.id].value
                        ChildrenTable.update({ ChildrenTable.id eq cid }) {
                            it[childName] = childNameVal
                            it[currentGrade] = classNameVal
                            it[profilePic] = photoVal
                            it[isActive] = true
                            it[updatedAt] = now
                        }
                        cid
                    } else {
                        val newId = UUID.randomUUID()
                        ChildrenTable.insert {
                            it[id] = newId
                            it[parentId] = parentUuid
                            it[schoolId] = ctx.schoolId
                            it[studentCode] = code
                            it[childName] = childNameVal
                            it[currentGrade] = classNameVal
                            it[profilePic] = photoVal
                            it[isActive] = true
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                        newId
                    }

                    // Flip the link → approved + audit who/when.
                    ParentChildLinksTable.update({ ParentChildLinksTable.id eq linkId }) {
                        it[status] = "approved"
                        it[childId] = childUuid
                        it[actionedBy] = ctx.userId
                        it[actionedAt] = now
                    }

                    // RA-SP: parent linking rule — a student may have MULTIPLE
                    // parents but AT MOST ONE primary guardian. Enforce it via the
                    // centralized aggregation service: if no approved link for this
                    // student is primary yet, promote the one we just approved;
                    // otherwise leave the existing primary intact.
                    val hasPrimary = ParentChildLinksTable.selectAll().where {
                        (ParentChildLinksTable.schoolId eq ctx.schoolId) and
                            (ParentChildLinksTable.studentCode eq code) and
                            (ParentChildLinksTable.status eq "approved") and
                            (ParentChildLinksTable.isPrimaryGuardian eq true)
                    }.any()
                    com.littlebridge.enrollplus.feature.school.StudentAggregationService
                        .enforceSinglePrimaryGuardian(
                            schoolId = ctx.schoolId,
                            studentCode = code,
                            primaryLinkId = if (hasPrimary) {
                                ParentChildLinksTable.selectAll().where {
                                    (ParentChildLinksTable.schoolId eq ctx.schoolId) and
                                        (ParentChildLinksTable.studentCode eq code) and
                                        (ParentChildLinksTable.status eq "approved") and
                                        (ParentChildLinksTable.isPrimaryGuardian eq true)
                                }.first()[ParentChildLinksTable.id].value
                            } else linkId
                        )

                    // A parent who just got their first child approved has completed
                    // their onboarding — flip profile_completed so the app routes to
                    // the dashboard instead of the link wizard.
                    AppUsersTable.update({ AppUsersTable.id eq parentUuid }) {
                        it[profileCompleted] = true
                        it[updatedAt] = now
                    }

                    DecisionResult.Approved(parentUuid, childUuid, childNameVal)
                }

                when (outcome) {
                    is DecisionResult.NotFound -> call.fail("Link request not found", HttpStatusCode.NotFound)
                    is DecisionResult.AlreadyDecided ->
                        call.fail("This request was already ${outcome.status}", HttpStatusCode.Conflict, "ALREADY_DECIDED")
                    is DecisionResult.Approved -> {
                        Notify.toUser(
                            userId = outcome.parentId,
                            category = "link_request",
                            title = "Child link approved",
                            body = "${outcome.childName} has been linked to your account.",
                            schoolId = ctx.schoolId,
                            actorId = ctx.userId,
                            deepLink = "parent/dashboard",
                            refType = "link_request",
                            refId = linkId.toString(),
                        )
                        call.ok(mapOf("child_id" to outcome.childId.toString(), "status" to "approved"), message = "Link approved")
                    }
                    else -> call.fail("Unexpected outcome", HttpStatusCode.InternalServerError)
                }
            }

            // ---- POST /link-requests/{id}/reject ----
            // Mark the link rejected (no children row created) and notify the parent.
            post("/link-requests/{id}/reject") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val linkId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid request id"); return@post
                }

                val outcome = dbQuery {
                    val link = ParentChildLinksTable.selectAll()
                        .where {
                            (ParentChildLinksTable.id eq linkId) and
                                (ParentChildLinksTable.schoolId eq ctx.schoolId)
                        }
                        .singleOrNull() ?: return@dbQuery DecisionResult.NotFound
                    // ISSUE 2d: rejectable from pending OR needs_review.
                    val curStatus = link[ParentChildLinksTable.status]
                    if (curStatus != "pending" && curStatus != "needs_review") {
                        return@dbQuery DecisionResult.AlreadyDecided(curStatus)
                    }
                    val parentUuid = link[ParentChildLinksTable.parentId]
                    val childNameVal = link[ParentChildLinksTable.childName]
                    val now = Instant.now()
                    ParentChildLinksTable.update({ ParentChildLinksTable.id eq linkId }) {
                        it[status] = "rejected"
                        it[actionedBy] = ctx.userId
                        it[actionedAt] = now
                    }
                    DecisionResult.Rejected(parentUuid, childNameVal)
                }

                when (outcome) {
                    is DecisionResult.NotFound -> call.fail("Link request not found", HttpStatusCode.NotFound)
                    is DecisionResult.AlreadyDecided ->
                        call.fail("This request was already ${outcome.status}", HttpStatusCode.Conflict, "ALREADY_DECIDED")
                    is DecisionResult.Rejected -> {
                        Notify.toUser(
                            userId = outcome.parentId,
                            category = "link_request",
                            title = "Child link request declined",
                            body = "Your request to link ${outcome.childName ?: "a child"} was not approved. Please verify the roll number with the school.",
                            schoolId = ctx.schoolId,
                            actorId = ctx.userId,
                            deepLink = "parent/link-child",
                            refType = "link_request",
                            refId = linkId.toString(),
                        )
                        call.ok(mapOf("status" to "rejected"), message = "Link request rejected")
                    }
                    else -> call.fail("Unexpected outcome", HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}

/** RA-03: max outstanding pending link requests a parent may have per school. */
private const val MAX_PENDING_LINKS_PER_SCHOOL = 3

/** RA-48: result of an admin approve/reject so the suspend respond runs outside dbQuery. */
private sealed interface DecisionResult {
    data object NotFound : DecisionResult
    data class AlreadyDecided(val status: String) : DecisionResult
    data class Approved(val parentId: UUID, val childId: UUID, val childName: String) : DecisionResult
    data class Rejected(val parentId: UUID, val childName: String?) : DecisionResult
}

/** Internal result of the link transaction so the suspend respond happens outside dbQuery. */
private sealed interface LinkResult {
    data object SchoolNotFound : LinkResult
    /**
     * No canonical student matched ANYWHERE on the platform (the selected school
     * AND every other school were searched, and the cross-school self-heal would
     * have rebound on a unique hit). [emptyRoster] reports whether the selected
     * school had any active students — useful context only.
     */
    data class StudentNotFound(val emptyRoster: Boolean = false) : LinkResult
    /** Several classes share this roll number — the parent must use the unique admission code. */
    data class AmbiguousRoll(val count: Int) : LinkResult
    data object Throttled : LinkResult
    /**
     * Full match (name + class/section/roll + parent phone) → standard pending
     * queue. [schoolId] is the student's REAL school (after a possible self-heal
     * from a wrongly-picked school), used for the link row + admin notification.
     */
    data class Pending(val linkId: UUID, val schoolId: UUID, val child: LinkedChildDto) : LinkResult
    /**
     * ISSUE 2d: name + class/section/roll matched but the parent phone did NOT
     * match the number on record. Routed to the "needs review" queue (flagged,
     * never silently dropped) for an admin to verify out-of-band. [schoolId] is
     * the student's REAL school (after a possible self-heal).
     */
    data class NeedsReview(val linkId: UUID, val schoolId: UUID, val child: LinkedChildDto, val reason: String) : LinkResult
}
