/*
 * File: Tables.kt
 * Module: db
 *
 * Exposed table definitions mapped 1:1 to the Postgres schema the backend
 * actually uses (the `vidyasetu_schema.sql` model — NOT the root
 * /supabase_schema doc, which only covers 2 of these tables).
 *
 * CANONICAL PROVISIONING ORDER (the ONLY complete recipe — see
 * docs/db/PROVISION.sql which lists these in order):
 *   1. docs/db/vidyasetu_schema.sql
 *   2. docs/db/migration_001_faculty_and_holiday_list.sql
 *   3. docs/db/migration_002_segmentation_geo_assignments.sql   (segmentation + assignments;
 *      schools lat/long now live in the BASE schema per §1.3, so this only re-asserts them
 *      via ADD COLUMN IF NOT EXISTS — a harmless no-op)
 *   4. docs/backend/sql/02_teacher_schema.sql
 *   5. docs/db/migration_005_class_normalization_and_student_code_standard.sql
 *   6. docs/db/migration_006_parent_link_review_fields.sql
 *   7. docs/db/migration_007_child_link_robustness.sql
 *   8. docs/db/migration_008_enrollments.sql   (Teacher Portal Rebuild T-001:
 *      typed class membership — EnrollmentsTable)
 *   9. docs/db/migration_009_tsa_fks.sql        (Teacher Portal Rebuild T-002:
 *      TSA class_id/subject_id FKs + is_class_teacher)
 *
 * Run all in Supabase → SQL Editor before pointing the backend at
 * production. For local-dev SQLite fallback, Exposed auto-creates the tables
 * in the order declared in DatabaseFactory.allTables. In Postgres, boot-time
 * validation (DatabaseFactory.validateSchema) logs any of the registered tables that
 * are missing and refuses to start when AUTO_CREATE_TABLES is not enabled.
 *
 * IMPORTANT DESIGN CHOICES
 * ------------------------
 *  - We deliberately do NOT model `auth.users` (Supabase Auth) here.  Our
 *    flow uses `app_users` for phone-OTP-first signup.  Email-only users
 *    can still be created (password_hash column).
 *  - We use UUIDs everywhere to match Supabase.
 *  - JSONB columns are stored as `text` here; we marshal them with
 *    kotlinx.serialization on the way in/out.
 *
 * NOTE: Exposed's `uuid` column type maps to Postgres UUID natively when
 * the JDBC driver is org.postgresql.Driver.  On SQLite it falls back to
 * a BLOB (16 bytes) which is fine for local-dev.
 */
package com.littlebridge.enrollplus.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp

// =====================================================================
// app_users  (our user record; decoupled from Supabase Auth)
// =====================================================================
object AppUsersTable : UUIDTable("app_users", "id") {
    val linkedAuthUserId = uuid("linked_auth_user_id").nullable()
    val schoolId         = uuid("school_id").nullable()
    val role             = varchar("role", 32).default("parent")  // user_role enum
    val fullName         = text("full_name")
    val phone            = varchar("phone", 32).nullable().uniqueIndex()
    val email            = varchar("email", 255).nullable().uniqueIndex()
    val passwordHash     = text("password_hash").nullable()
    val profilePicUrl    = text("profile_pic_url").nullable()
    val languagePref     = varchar("language_pref", 8).default("hi")
    val isPhoneVerified  = bool("is_phone_verified").default(false)
    val isEmailVerified  = bool("is_email_verified").default(false)
    val profileCompleted = bool("profile_completed").default(false)
    // RA-54: server-side signal for the teacher first-login "force change initial
    // password" gate. Provisioned teachers are created with this = true; the
    // POST /auth/change-password endpoint clears it (and flips profile_completed)
    // once the teacher sets their own password. NavGraphV2 reads it via login.
    val mustChangePassword = bool("must_change_password").default(false)
    // Phase 6: server-side theme preference (stores VThemeMode storage value:
    // "system" | "light" | "dark" | "custom:<themeId>"). Synced from the client
    // via PUT /api/v1/user/theme-pref and read back in GET /api/v1/user/details.
    val themePref         = varchar("theme_pref", 64).nullable()
    val isActive         = bool("is_active").default(true)
    val lastLoginAt      = timestamp("last_login_at").nullable()
    val createdAt        = timestamp("created_at")
    val updatedAt        = timestamp("updated_at")
}

// =====================================================================
// auth_otps  (industrial-grade OTP store — see SQL doc for design notes)
// =====================================================================
object AuthOtpsTable : UUIDTable("auth_otps", "id") {
    val identifier        = text("identifier")
    val identifierType    = varchar("identifier_type", 8) // phone | email
    val purpose           = varchar("purpose", 24).default("login")
    val codeHash          = text("code_hash")
    val codeSalt          = text("code_salt")

    val sentAt            = timestamp("sent_at")
    val firstSentAt       = timestamp("first_sent_at")
    val expiresAt         = timestamp("expires_at")

    val resendCount       = short("resend_count").default(0.toShort())
    val attemptCount      = short("attempt_count").default(0.toShort())
    val maxAttempts       = short("max_attempts").default(5.toShort())
    val maxResends        = short("max_resends").default(5.toShort())
    val resendWindowSecs  = integer("resend_window_secs").default(3600)

    val isVerified        = bool("is_verified").default(false)
    val isLocked          = bool("is_locked").default(false)
    val verifiedAt        = timestamp("verified_at").nullable()

    val ipAddress         = text("ip_address").nullable()
    val userAgent         = text("user_agent").nullable()
    val deviceId          = text("device_id").nullable()
    val deliveryChannel   = varchar("delivery_channel", 16).nullable()
    val deliveryProvider  = varchar("delivery_provider", 32).nullable()
    val providerMessageId = text("provider_message_id").nullable()

    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")

    init {
        uniqueIndex("ux_auth_otps_identifier_purpose", identifier, purpose)
    }
}

// =====================================================================
// otp_delivery_attempts
//   FULL audit trail of every provider attempt during a single OTP send.
//   `auth_otps` only tracks the WINNING provider; this table records the
//   entire chain (sent / failed / skipped) so we can:
//     • debug delivery problems per-vendor
//     • compute per-provider success rate / latency dashboards
//     • prove DLT compliance during a TRAI audit
//
//   Rows are short-lived — purged ~7 days after the parent OTP expires.
//   See `01_supplementary_schema.sql` SECTION 2b for the SQL definition.
// =====================================================================
object OtpDeliveryAttemptsTable : UUIDTable("otp_delivery_attempts", "id") {
    val otpId             = uuid("otp_id").nullable()       // FK auth_otps.id (nullable so we don't crash if parent already purged)
    val identifier        = text("identifier")               // copied for forensics (parent may be gone)
    val purpose           = varchar("purpose", 24)
    val attemptIndex      = integer("attempt_index")         // 0 = first provider tried
    val providerName      = varchar("provider_name", 64)     // "fast2sms" / "msg91" / ...
    val channel           = varchar("channel", 16)           // "sms" / "whatsapp" / "email" / "voice" / "console"
    val status            = varchar("status", 16)            // "sent" / "failed" / "skipped"
    val providerMessageId = text("provider_message_id").nullable()
    val httpStatus        = integer("http_status").nullable()
    val latencyMs         = integer("latency_ms").default(0)
    val reason            = text("reason").nullable()
    val rawResponse       = text("raw_response").nullable()
    val createdAt         = timestamp("created_at")
}

// =====================================================================
// user_sessions  (rotating refresh-token store)
// =====================================================================
object UserSessionsTable : UUIDTable("user_sessions", "id") {
    val userId            = uuid("user_id")
    val refreshTokenHash  = text("refresh_token_hash").uniqueIndex()
    val deviceId          = text("device_id").nullable()
    val platform          = varchar("platform", 16).nullable()
    val ipAddress         = text("ip_address").nullable()
    val userAgent         = text("user_agent").nullable()
    val issuedAt          = timestamp("issued_at")
    val expiresAt         = timestamp("expires_at")
    val revokedAt         = timestamp("revoked_at").nullable()
    val lastUsedAt        = timestamp("last_used_at").nullable()
    val createdAt         = timestamp("created_at")
}

// =====================================================================
// CMS landing + app config (KV stores, JSONB)
// =====================================================================
object LandingContentTable : Table("cms_landing_content") {
    val key       = text("key")
    val value     = text("value")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(key)
}

object AppConfigTable : Table("app_config") {
    val key       = text("key")
    val value     = text("value")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(key)
}

// =====================================================================
// Schools  (subset of fields the API surface needs)
// =====================================================================
object SchoolsTable : UUIDTable("schools", "id") {
    val name           = text("name")
    val slug           = text("slug").uniqueIndex()
    val board          = varchar("board", 32)
    val medium         = varchar("medium", 32)
    val schoolGender   = varchar("school_gender", 16).default("co_ed")
    val contactPhone   = text("contact_phone").nullable()
    val contactEmail   = text("contact_email").nullable()
    val principalName  = text("principal_name").nullable()
    val principalPhone = text("principal_phone").nullable()
    val principalEmail = text("principal_email").nullable()
    val fullAddress    = text("full_address").nullable()
    val city           = text("city")
    val district       = text("district")
    val state          = text("state").default("Uttar Pradesh")
    val pincode        = text("pincode").nullable()
    val logoUrl        = text("logo_url").nullable()
    val brandColor     = text("brand_color").default("#2563EB")
    // Geo coordinates persisted during onboarding / profile update so the
    // parent side can discover onboarded schools by distance ("near me").
    // Nullable because legacy rows / address-only onboarding may not have them.
    val latitude       = double("latitude").nullable()
    val longitude      = double("longitude").nullable()
    val isActive       = bool("is_active").default(true)
    val onboardedAt    = timestamp("onboarded_at").nullable()
    // Explicit onboarding lifecycle mirror of onboarded_at: 'pending' until the
    // admin completes onboarding, then 'active'. Kept in lock-step by the server
    // (REVIEW final submit + POST /onboarding/complete). See
    // docs/db/schema-patch-school-onboarding.sql.
    val onboardingStatus = varchar("onboarding_status", 16).default("pending")
    // Richer onboarding fields the wizard collects (all nullable; promoted from
    // school_onboarding_drafts so the dashboard can read them directly).
    val schoolType         = text("school_type").nullable()
    val affiliationNumber  = text("affiliation_number").nullable()
    val yearEstablished    = integer("year_established").nullable()
    val website            = text("website").nullable()
    val totalStudents      = integer("total_students").nullable()
    val totalClasses       = integer("total_classes").nullable()
    val academicYearStartMonth = text("academic_year_start_month").nullable()
    val gradingSystem      = text("grading_system").nullable()
    // EXPLICIT per-wizard-step completion ledger (onboarding redirect fix).
    //
    // The bug: self-registration (`POST /auth/register-school`) pre-creates a
    // fully-named `schools` row at signup. The old status logic inferred
    // "BASIC step done" purely from "a named school row exists", so a BRAND-NEW
    // school read as having already finished Step 1 — the wizard then resumed at
    // ACADEMIC (frontend Step 3) and skipped the required earlier steps.
    //
    // We now record which wizard steps the admin ACTUALLY submitted, as a CSV of
    // step tokens (BASIC,BRANDING,ACADEMIC,REVIEW). It is written ONLY by
    // `POST /onboarding/submit` (and `/complete`), never by registration, so it
    // cleanly distinguishes a registration placeholder from genuine step
    // completion. NULL/empty for a freshly-registered school → resume at BASIC.
    val onboardingStepsDone = text("onboarding_steps_done").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}

// =====================================================================
// Onboarding drafts
// =====================================================================
object OnboardingDraftsTable : UUIDTable("school_onboarding_drafts", "id") {
    val userId    = uuid("user_id")
    val stepType  = varchar("step_type", 16) // BASIC | BRANDING | ACADEMIC | REVIEW
    val key       = text("key")
    val value     = text("value")
    val updatedAt = timestamp("updated_at")
    init {
        uniqueIndex("ux_ob_drafts_user_step_key", userId, stepType, key)
    }
}

// =====================================================================
// Classes + subjects
// =====================================================================
object SchoolClassesTable : UUIDTable("school_classes", "id") {
    val schoolId  = uuid("school_id")
    val code      = text("code")
    val name      = text("name")
    val sections  = text("sections").default("[]") // JSONB stored as text
    val createdAt = timestamp("created_at")
    init {
        uniqueIndex("ux_classes_school_code", schoolId, code)
    }
}

object SchoolSubjectsTable : UUIDTable("school_subjects", "id") {
    val classId         = uuid("class_id")
    val subName         = text("sub_name")
    val subCode         = text("sub_code")
    // Legacy free-text teacher name kept for backwards compatibility with rows
    // written before the structured teacher_subject_assignments model existed.
    val teacherAssigned = text("teacher_assigned").nullable()
    val createdAt       = timestamp("created_at")
}

// =====================================================================
// teacher_subject_assignments
//   Structured replacement for the free-text `school_subjects.teacher_assigned`
//   column. Defines WHO (faculty/user) teaches WHAT (subject) to WHICH
//   class+section. This is the assignment graph that enables:
//     • per-class subject pools (instead of one global subject array)
//     • teacher broadcasts scoped to the classes/subjects they actually teach
//     • audience expansion for segmented announcements
//
//   Uniqueness: one (school, class, section, subject, teacher) tuple is unique
//   so re-saving an assignment updates rather than duplicates.
// =====================================================================
object TeacherSubjectAssignmentsTable : UUIDTable("teacher_subject_assignments", "id") {
    val schoolId  = uuid("school_id")
    // T-002: class_id / subject_id are now FK-backed and backfilled
    // (migration_009_tsa_fks.sql) — the AUTHORITATIVE typed scope. className /
    // section / subject below are DISPLAY-only denormalised columns; the scoping
    // core (TeacherAccess, T-003) is id-first and treats ClassNaming as fallback.
    val classId   = uuid("class_id").nullable()          // FK school_classes.id (fk_tsa_class)
    val className = text("class_name")                   // DISPLAY only (denormalised)
    val section   = varchar("section", 8).default("A")
    val subjectId = uuid("subject_id").nullable()        // FK school_subjects.id (fk_tsa_subject)
    val subject   = text("subject")                      // DISPLAY only (denormalised)
    val teacherId = uuid("teacher_id").nullable()        // FK faculty.id / app_users.id
    val teacherName = text("teacher_name").nullable()    // display fallback when no FK
    val isActive  = bool("is_active").default(true)
    // T-002: real "class teacher of this class+section" signal (B-CLS-3/F-CLS-3).
    val isClassTeacher = bool("is_class_teacher").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    init {
        uniqueIndex(
            "ux_tsa_unique",
            schoolId, className, section, subject, teacherName
        )
    }
}

// =====================================================================
// Announcements + WhatsApp logs
// =====================================================================
object AnnouncementsTable : UUIDTable("announcements", "id") {
    val schoolId    = uuid("school_id")
    val eventId     = text("event_id").uniqueIndex()
    val type        = varchar("type", 16) // Holidays|PTM|Events|Special|Remainder
    val title       = text("title")
    val subTitle    = text("sub_title").nullable()
    val description = text("description")
    val eventImage  = text("event_image").nullable()
    val date        = varchar("date", 12) // YYYY-MM-DD, mapped from PG DATE
    // ---- Broadcast audience segmentation ----
    // audienceType: ALL_SCHOOL | CLASS | SECTION | SUBJECT | STUDENT | CUSTOM
    // audienceFilter: JSON describing the scope, e.g.
    //   {"class_name":"Grade 5","section":"A"} or {"subject":"Maths"} or
    //   {"student_codes":["S001","S002"]}. NULL/ALL_SCHOOL = everyone in school.
    val audienceType   = varchar("audience_type", 16).default("ALL_SCHOOL")
    val audienceFilter = text("audience_filter").nullable()
    // The teacher/admin user that owns this broadcast. For teacher broadcasts the
    // recipient expansion is constrained to the classes/subjects they teach.
    val authorRole     = varchar("author_role", 16).default("school_admin")
    val syncedToWa  = bool("synced_to_wa").default(false)
    val createdBy   = uuid("created_by").nullable()
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

object WhatsappLogsTable : UUIDTable("whatsapp_logs", "id") {
    val schoolId          = uuid("school_id")
    val announcementId    = text("announcement_id")
    val jobId             = text("job_id")
    val phone             = text("phone")
    val status            = varchar("status", 16).default("QUEUED")
    val providerMessageId = text("provider_message_id").nullable()
    val errorMessage      = text("error_message").nullable()
    val createdAt         = timestamp("created_at")
}

// =====================================================================
// Admission enquiries
// =====================================================================
object AdmissionEnquiriesTable : UUIDTable("admission_enquiries", "id") {
    val schoolId    = uuid("school_id")
    val studentName = text("student_name")
    val parentName  = text("parent_name")
    val parentPhone = text("parent_phone").nullable()
    val parentEmail = text("parent_email").nullable()
    val className   = text("class_name")
    val date        = varchar("date", 12) // YYYY-MM-DD
    val status      = varchar("status", 16).default("new")
    val profilePic  = text("profile_pic").nullable()
    val admissionSource = varchar("source", 32).nullable()
    val notes       = text("notes").nullable()
    val assignedTo  = uuid("assigned_to").nullable()
    val convertedAt = timestamp("converted_at").nullable()
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

// =====================================================================
// School profile (philosophy / media / storage)
// =====================================================================
object SchoolPhilosophyTable : Table("school_philosophy") {
    val schoolId        = uuid("school_id")
    val coreMission     = text("core_mission").nullable()
    val learningModel   = text("learning_model").nullable()
    val primaryLanguage = text("primary_language").nullable()
    val publicProfile   = bool("public_profile").default(true)
    val updatedAt       = timestamp("updated_at")
    override val primaryKey = PrimaryKey(schoolId)
}

object SchoolMediaTable : UUIDTable("school_media", "id") {
    val schoolId   = uuid("school_id")
    val kind       = varchar("kind", 8) // IMAGE | VIDEO
    val url        = text("url")
    val position   = integer("position").default(0)
    val sizeBytes  = long("size_bytes").default(0)
    val uploadedBy = uuid("uploaded_by").nullable()
    val createdAt  = timestamp("created_at")
}

object StorageMetricsTable : Table("storage_metrics") {
    val schoolId     = uuid("school_id")
    val totalStorage = text("total_storage").default("10 GB")
    val storageUsed  = text("storage_used").default("0 B")
    val bytesUsed    = long("bytes_used").default(0L)
    val updatedAt    = timestamp("updated_at")
    override val primaryKey = PrimaryKey(schoolId)
}

// =====================================================================
// Academic calendar / Holidays / Faculty / Attendance
// =====================================================================
object AcademicCalendarTable : UUIDTable("academic_calendar", "id") {
    val schoolId         = uuid("school_id")
    val eventId          = text("event_id").uniqueIndex()
    val date             = varchar("date", 12)
    val day              = varchar("day", 16)
    val eventTitle       = text("event_title")
    val eventDescription = text("event_description").nullable()
    val standard         = text("standard").nullable()
    val isHoliday        = bool("is_holiday").default(false)
    val createdAt        = timestamp("created_at")
}

/**
 * LEGACY holiday source. DEPRECATED by Teacher Portal Rebuild T-102 (Doc 05
 * §2.3, closes D-TT-5): `calendar_events(type=HOLIDAY, status=PUBLISHED)` is now
 * the SINGLE source of holiday truth, and the resolved-day computation (T-104)
 * reads holidays ONLY from there. migration_012_holidays_merge.sql forward-fills
 * every holiday_list row into calendar_events (idempotent via source_ref
 * 'HL:<id>').
 *
 * The table + this mapping are KEPT (not dropped) only so the two surviving
 * legacy readers compile+work in this release:
 *   - feature/parent/ParentAcademicsRouting.kt (parent academics holidays)
 *   - feature/school/SchoolRouting.kt          (school holidays screen)
 * A later phase repoints those readers at calendar_events and then drops this
 * table + mapping. Do NOT add new readers of this table — read calendar_events.
 */
@Deprecated("D-TT-5 / T-102: holidays now live in calendar_events(HOLIDAY,PUBLISHED). Read CalendarEventsTable instead; holiday_list is migrated by migration_012 and will be dropped once the two legacy readers are repointed.")
object HolidayListTable : UUIDTable("holiday_list", "id") {
    val schoolId  = uuid("school_id")
    val date      = varchar("date", 12)
    val title     = text("title")
    val type      = varchar("type", 16) // Public | School
    val frequency = varchar("frequency", 16) // weekly|monthly|yearly
    val createdAt = timestamp("created_at")
}

object FacultyTable : UUIDTable("faculty", "id") {
    val schoolId   = uuid("school_id")
    val externalId = text("external_id").uniqueIndex()
    val userId     = uuid("user_id").nullable()
    val name       = text("name")
    val profilePic = text("profile_pic").nullable()
    val department = text("department").nullable()
    val isActive   = bool("is_active").default(true)
    val createdAt  = timestamp("created_at")
}

// =====================================================================
// attendance_records  (Teacher Portal Rebuild — Doc 11 T-201 / Doc 06 §1.2)
//   The TYPED student/faculty attendance record. T-201 migrates the legacy
//   shape (packed `grade` "<class>-<section>" + free `person_id`, status
//   present|absent|late) to a provably-scoped one:
//     • student_id / enrollment_id   — typed FK identity of WHO + which exact
//       class membership (kills D-ATT-2 / X-1 / D-STU for attendance).
//     • faculty_id                   — the app_users id when type='faculty'.
//     • assignment_id                — the authorizing TSA (Doc 05 binding) so
//       the mark's class/section/subject scope is provable, not parsed from a
//       string (kills D-ATT-4 / X-1).
//     • status now includes `leave`  — an approved leave can be reflected
//       (D-ATT-1); leave approval auto-writes it (B-LV-2, T-204).
//     • source                       — manual | leave_auto | bulk | biometric.
//     • marked_at                    — when the mark was last written.
//   The packed `grade` column is DROPPED (class/section derive via
//   assignment_id → TSA). `person_id` is retained NULLABLE only so the T-201
//   migration can quarantine un-resolvable legacy rows without data loss; new
//   writes use the typed FKs. Applied by docs/db/migration_014_attendance.sql.
//   UNIQUE moves to (school, date, type, student_id, assignment_id): one mark
//   per student per class per day (Doc 06 §1.2).
// =====================================================================
object AttendanceRecordsTable : UUIDTable("attendance_records", "id") {
    val schoolId     = uuid("school_id")
    val date         = date("date")        // T-004: typed `date` (was varchar12)
    val type         = varchar("type", 16) // student | faculty
    // Legacy free identifier — retained NULLABLE for migration/quarantine only.
    val personId     = text("person_id").nullable()
    // DEPRECATED (T-201): the packed "<class>-<section>" grade string. Doc 06 §1.2
    // calls for it to be DROPPED (class/section derive via assignment_id → TSA).
    // It is kept here as a NULLABLE deprecated column ONLY so the pre-rebuild
    // readers (admin dashboards, parent academics, the legacy TeacherRouting
    // attendance path, and the T-104 resolver's attendanceMarked join) keep
    // compiling and working until they are migrated to the typed columns in
    // T-203/T-205. The migration (014) ADDS the typed columns + new UNIQUE but
    // does NOT physically drop `grade` yet — the DROP is deferred to a follow-up
    // once every reader is off it (flagged deviation, see migration_014 header).
    val grade        = text("grade").nullable()
    // T-201 typed identity (Doc 06 §1.2). All nullable: a student row fills
    // student_id/enrollment_id/assignment_id; a faculty row fills faculty_id.
    val studentId    = uuid("student_id").nullable()      // FK students.id (type=student)
    val enrollmentId = uuid("enrollment_id").nullable()   // FK enrollments.id (exact membership)
    val facultyId    = uuid("faculty_id").nullable()      // FK app_users.id (type=faculty)
    val assignmentId = uuid("assignment_id").nullable()   // FK teacher_subject_assignments.id
    val status       = varchar("status", 16)              // present | absent | late | leave
    // NOTE: property named `attSource` (not `source`) because Exposed's ColumnSet
    // now declares a `source` member; a column property of the same name would
    // hide it and fail to compile. DB column name stays "source".
    val attSource    = varchar("source", 16).default("manual") // manual|leave_auto|bulk|biometric
    val markedBy     = uuid("marked_by").nullable()
    val markedAt     = timestamp("marked_at").nullable()  // when last written (Doc 06 §1.2)
    val createdAt    = timestamp("created_at")
    init {
        // Doc 06 §1.2: one mark per student per class (assignment) per day. NULLs
        // in student_id/assignment_id are treated distinct by Postgres; the legacy
        // ux_att_records_unique (school,date,type,person_id) is replaced by the
        // migration. New student writes always carry both, so the key is real.
        uniqueIndex(
            "ux_att_records_typed_unique",
            schoolId, date, type, studentId, assignmentId
        )
    }
}

// =====================================================================
// Students (read-only mirror — operational writes happen elsewhere)
// =====================================================================
object StudentsTable : UUIDTable("students", "id") {
    val schoolId   = uuid("school_id")
    val studentCode = text("student_code").uniqueIndex()
    val fullName   = text("full_name")
    val className  = text("class_name")
    val section    = text("section").default("A")
    val rollNumber = text("roll_number")
    // ISSUE 2b: parent/guardian phone captured at admin student creation. Used by
    // the parent→child link matching logic (full match vs phone-mismatch review).
    // Nullable so pre-existing rows remain valid until backfilled.
    val parentPhone = text("parent_phone").nullable()
    val profilePhotoUrl = text("profile_photo_url").nullable()
    val isActive   = bool("is_active").default(true)
    val createdAt  = timestamp("created_at")
}

// =====================================================================
// enrollments  (Teacher Portal Rebuild — Doc 11 T-001 / Doc 09 §1)
//   The TYPED class-membership bridge: a student is enrolled in a
//   class+section for a period. This replaces the in-memory
//   ClassNaming.sameClassSection() heuristic (X-1/X-2) and the packed
//   attendance_records.grade "<class>-<section>" string (D-STU) as the
//   source of truth for "who is in class 7B".
//
//   FK: student_id -> students.id, class_id -> school_classes.id.
//   Uniqueness: (student_id, class_id, section, start_date) so re-running
//   the backfill is idempotent and historical/transfer rows coexist.
//
//   Created/applied by docs/db/migration_008_enrollments.sql. Registered in
//   DatabaseFactory.allTables — AUTO_CREATE_TABLES is OFF in production, so
//   that migration MUST be applied in Supabase before the matching deploy or
//   validateSchema() refuses to boot.
//
//   NOTE (per LAWS): Doc 01 §11.2 sketched (from_date/to_date/is_active) but
//   Doc 11 T-001 "Details" cites Doc 09 §1, whose shape (school_id, section,
//   roll_number, status, start_date, end_date) is authoritative and modelled
//   here. start_date/end_date are real DATE columns (not the varchar(12) date
//   convention used by pre-T-004 tables).
// =====================================================================
object EnrollmentsTable : UUIDTable("enrollments", "id") {
    val schoolId   = uuid("school_id")
    val studentId  = uuid("student_id")                 // FK students.id
    val classId    = uuid("class_id")                   // FK school_classes.id
    val section    = varchar("section", 8).default("A")
    val rollNumber = integer("roll_number").nullable()
    val status     = varchar("status", 16).default("active") // active|transferred|withdrawn
    val startDate  = date("start_date")
    val endDate    = date("end_date").nullable()        // transfers/withdrawals (Doc 06 E4/E5)
    val createdAt  = timestamp("created_at")
    init {
        uniqueIndex(
            "ux_enrollments_unique",
            studentId, classId, section, startDate
        )
    }
}

// =====================================================================
// Parent Ecosystem Tables (spec: parent_api_spec.artifact.md)
// =====================================================================

/**
 * Children registered by parents during onboarding (Module: Child Onboarding).
 * A child belongs to a parent (app_users.role = 'parent') and OPTIONALLY to a
 * school (set when the parent enrols them somewhere). `interests` and the
 * holistic progress JSON columns are persisted as plain text and parsed/
 * encoded with kotlinx.serialization at the route layer.
 *
 * Spec ref: parent_api_spec.artifact.md §Module: Child Onboarding
 *           parent_api_spec.artifact.md §Module: Core Dashboard & Progress
 */
object ChildrenTable : UUIDTable("children", "id") {
    val parentId         = uuid("parent_id")                            // FK app_users.id
    val schoolId         = uuid("school_id").nullable()                 // FK schools.id (optional)
    // Links a parent's child to the school's canonical students.student_code so
    // STUDENT-scoped announcements can target exact students (report §5.6).
    val studentCode      = text("student_code").nullable()
    val childName        = text("child_name")
    val dateOfBirth      = varchar("date_of_birth", 12).nullable()      // YYYY-MM-DD
    val gender           = varchar("gender", 16).nullable()             // MALE | FEMALE | OTHER
    val currentGrade     = varchar("current_grade", 32).nullable()      // e.g. "Grade 1"
    val interests        = text("interests").default("[]")              // JSON array of strings
    val profilePic       = text("profile_pic").nullable()
    val overallProgress  = double("overall_progress").default(0.0)      // 0..1
    val currentLevel     = integer("current_level").default(1)
    val attendanceStatus = varchar("attendance_status", 16).default("PRESENT")
    val isActive         = bool("is_active").default(true)
    val createdAt        = timestamp("created_at")
    val updatedAt        = timestamp("updated_at")
}

/**
 * Parent fee records — driven by GET /api/v1/parent/fees.
 *
 * Stores per-line-item fees for a (parent, child) pair.  The aggregate
 * `stats` block in the response is computed on-the-fly:
 *   total_collected  = SUM(amount) WHERE status='PAID'
 *   outstanding      = SUM(amount) WHERE status IN ('DUE','OVERDUE')
 *   overdue_count    = COUNT(*) WHERE status='OVERDUE'
 *   progress         = total_collected / (total_collected + outstanding)
 *
 * Spec ref: parent_api_spec.artifact.md §Module: School Management §Screen: Fees
 */
object FeeRecordsTable : UUIDTable("fee_records", "id") {
    val parentId    = uuid("parent_id")
    val childId     = uuid("child_id").nullable()
    val schoolId    = uuid("school_id").nullable()
    val title       = text("title")
    val description = text("description").nullable()
    val amount      = double("amount").default(0.0)
    val currency    = varchar("currency", 8).default("INR")             // India-first default (audit §11 L3)
    val dueDate     = varchar("due_date", 12).nullable()                // YYYY-MM-DD
    val status      = varchar("status", 16).default("DUE")              // PAID | DUE | OVERDUE
    val category    = varchar("category", 32).default("Tuition")        // Tuition | Transport | ...
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
    // Notification scheduler: throttle fee reminders to at most once per 24h.
    val lastRemindedAt = timestamp("last_reminded_at").nullable()
}

// =====================================================================
// School Ecosystem Tables  (spec: school_api_spec.artifact.md)
// =====================================================================

/**
 * Leave applications submitted by students or teachers.
 *
 * `request_type` is "student" or "teacher" so the same table powers both
 * tabs on the Leave Requests screen.  `image_url` is the avatar of the
 * requester (mirrors what the UI shows).
 *
 * Spec ref: school_api_spec.artifact.md §Module: Leave Requests
 */
object LeaveRequestsTable : UUIDTable("leave_requests", "id") {
    val schoolId      = uuid("school_id")
    val requesterId   = uuid("requester_id").nullable()                 // FK app_users.id (optional)
    val requesterName = text("requester_name")
    val requesterRole = varchar("requester_role", 16).default("student") // student | teacher
    val dateFrom      = varchar("date_from", 12)                        // YYYY-MM-DD
    val dateTo        = varchar("date_to", 12)                          // YYYY-MM-DD
    val reason        = text("reason")
    val imageUrl      = text("image_url").nullable()
    val status        = varchar("status", 16).default("Pending")        // Pending | Approved | Rejected
    val actionedBy    = uuid("actioned_by").nullable()
    val actionedAt    = timestamp("actioned_at").nullable()
    // RA-44: cross-role leave workflow. A parent applies on behalf of a child;
    // the request is routed to the child's class teacher(s) (resolved at apply
    // time from teacher_subject_assignments) and can be decided by that teacher
    // or overridden by an admin. All nullable so legacy teacher-leave rows and
    // pre-PATCH-103 data still load.
    val classId       = uuid("class_id").nullable()                     // FK school_classes.id (the class)
    val className     = varchar("class_name", 64).nullable()
    val section       = varchar("section", 16).nullable()
    val teacherId     = uuid("teacher_id").nullable()                   // FK app_users.id (routed-to teacher)
    val childId       = uuid("child_id").nullable()                     // FK children.id (the student)
    val parentId      = uuid("parent_id").nullable()                    // FK app_users.id (the applicant)
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")
}

/**
 * Parent-Teacher Meeting events — drives the Schedule PTM screen.
 * The "active event" is computed at request-time as either the next
 * upcoming row or, in absence, the most recent past row.
 *
 * Spec ref: school_api_spec.artifact.md §Module: PTM
 */
object PtmEventsTable : UUIDTable("ptm_events", "id") {
    val schoolId         = uuid("school_id")
    val title            = text("title")
    val date             = varchar("date", 12)                          // YYYY-MM-DD
    val slot             = text("slot")                                 // e.g. "09:00 - 13:00"
    val expectedParents  = integer("expected_parents").default(0)
    val checkedInParents = integer("checked_in_parents").default(0)
    val invitesDelivered = integer("invites_delivered").default(0)
    val readReceipts     = integer("read_receipts").default(0)
    val turnout          = integer("turnout").default(0)                // historical metric for past events
    val totalMet         = integer("total_met").default(0)
    val createdBy        = uuid("created_by").nullable()
    val createdAt        = timestamp("created_at")
    val updatedAt        = timestamp("updated_at")
}

/**
 * Per-class PTM rollup (met_count / total_count) belonging to a PTM event.
 * Two reasons we keep this as its own table instead of a JSON column:
 *   - Faculty teams update one row at a time as they hold meetings.
 *   - We want to index/filter by class_name later.
 *
 * Spec ref: school_api_spec.artifact.md §Module: PTM
 */
object PtmClassProgressTable : UUIDTable("ptm_class_progress", "id") {
    val ptmEventId  = uuid("ptm_event_id")                              // FK ptm_events.id
    val className   = text("class_name")
    val teacherName = text("teacher_name")
    val metCount    = integer("met_count").default(0)
    val totalCount  = integer("total_count").default(0)
    val updatedAt   = timestamp("updated_at")
    init {
        uniqueIndex("ux_ptm_class_progress_unique", ptmEventId, className)
    }
}

/**
 * Admin inbox: a thread is the conversation header (sender / preview /
 * unread counter), and `MessagesTable` rows are the individual messages.
 *
 * Spec ref: school_api_spec.artifact.md §Module: Messages
 */
object MessageThreadsTable : UUIDTable("message_threads", "id") {
    val schoolId       = uuid("school_id")
    val ownerUserId    = uuid("owner_user_id")                          // this row's inbox holder — JWT.sub
    // RA-51: a conversation between two users is represented by ONE row per
    // participant, all sharing the same conversation_id. Each row carries the
    // owner's view (unread/read), peer_user_id identifies the other party, and
    // MessagesTable is keyed by conversation_id so both sides see one history.
    // Legacy/system threads leave conversation_id = their own id and
    // peer_user_id null (single-owner inbox, e.g. system alerts).
    val conversationId = uuid("conversation_id").nullable()             // shared key across both participants' rows
    val peerUserId     = uuid("peer_user_id").nullable()                // the OTHER participant (null = system/self)
    val senderName     = text("sender_name")                            // display name of the peer, as seen by owner
    val senderRole     = text("sender_role")
    val senderImageUrl = text("sender_image_url").nullable()
    val iconName       = text("icon_name").nullable()                   // when no avatar (e.g. "payments")
    val lastMessage    = text("last_message").default("")
    val lastMessageAt  = timestamp("last_message_at")
    val unreadCount    = integer("unread_count").default(0)
    val isRead         = bool("is_read").default(true)
    // Phase 1 (MESSAGING_SYSTEM_SPEC §8.1): thread-level preferences.
    val isMuted        = bool("is_muted").default(false)
    val isPinned       = bool("is_pinned").default(false)
    val isArchived     = bool("is_archived").default(false)
    val draftBody      = text("draft_body").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}

object MessagesTable : UUIDTable("messages", "id") {
    val threadId       = uuid("thread_id")                              // FK message_threads.id (the sender's owning row)
    val conversationId = uuid("conversation_id").nullable()             // RA-51: shared key — both participants read by this
    val senderId       = uuid("sender_id").nullable()                   // null for system messages
    val body           = text("body").nullable()                        // nullable for soft-delete tombstones
    val createdAt      = timestamp("created_at")
    // Phase 1 (MESSAGING_SYSTEM_SPEC §8.1): ordering, idempotency, edit/delete, replies.
    val seq            = integer("seq").nullable()                      // server-assigned monotonic per conversation
    val clientMsgId    = uuid("client_msg_id").nullable()               // idempotency key (unique when non-null)
    val editedAt       = timestamp("edited_at").nullable()              // non-null = message was edited
    val deletedAt      = timestamp("deleted_at").nullable()             // non-null = tombstoned (body nulled)
    val replyToId      = uuid("reply_to_id").nullable()                 // FK messages.id (quoted original)

    init {
        // Delta sync + pagination: WHERE conversation_id=? AND seq>? ORDER BY seq
        index("idx_messages_conv_seq", isUnique = false, conversationId, seq)
    }
}

/**
 * Phase 1 (§7.1) — atomic per-conversation sequence counter.
 * Prevents duplicate seq values under concurrent inserts.
 * UPSERT: INSERT ... ON CONFLICT (conversation_id) DO UPDATE SET next_val = next_val + 1 RETURNING next_val.
 */
object ConversationSeqTable : UUIDTable("conversation_seq", "id") {
    val conversationId = uuid("conversation_id").uniqueIndex()
    val nextVal        = integer("next_val")
    val updatedAt      = timestamp("updated_at")
}

/**
 * Phase 1 (MESSAGING_SYSTEM_SPEC §8.2) — per-message per-user delivery status.
 * Tracks the SENT → DELIVERED → READ state machine (§4.2).
 * UNIQUE(message_id, user_id) prevents duplicate status rows.
 */
object MessageStatusTable : UUIDTable("message_status", "id") {
    val messageId      = uuid("message_id")                             // FK messages.id (CASCADE)
    val conversationId = uuid("conversation_id")
    val userId         = uuid("user_id")                               // the recipient whose status this tracks
    val status         = varchar("status", 16)                         // SENT | DELIVERED | READ
    val createdAt      = timestamp("created_at")

    init {
        uniqueIndex("ux_message_status_msg_user", messageId, userId)
        index("idx_msg_status_conv_user", isUnique = false, conversationId, userId)
    }
}

/**
 * Phase 1 (MESSAGING_SYSTEM_SPEC §8.2, §12) — media attachments on messages.
 * Reuses the Supabase Storage pipeline (SupabaseStorage.kt) with kind "MESSAGE".
 * Storage path: {schoolId}/message/{uuid}.{ext}
 */
object MessageAttachmentsTable : UUIDTable("message_attachments", "id") {
    val messageId      = uuid("message_id")                             // FK messages.id (CASCADE)
    val conversationId = uuid("conversation_id")
    val senderId       = uuid("sender_id")
    val schoolId       = uuid("school_id")
    val fileName       = text("file_name")
    val mimeType       = text("mime_type")
    val sizeBytes      = long("size_bytes")
    val storageUrl     = text("storage_url")
    val thumbnailUrl   = text("thumbnail_url").nullable()
    val attachmentType = varchar("attachment_type", 16).default("IMAGE") // IMAGE | VIDEO | DOCUMENT | AUDIO
    val width          = integer("width").nullable()
    val height         = integer("height").nullable()
    val durationMs     = integer("duration_ms").nullable()
    val createdAt      = timestamp("created_at")

    init {
        index("idx_attachments_message", isUnique = false, messageId)
    }
}

/**
 * Per-student exam results upserted via Results screen.
 * The (school, test, class, subject, student_id) tuple is unique so
 * publishing the same test twice updates rather than duplicates.
 *
 * Spec ref: school_api_spec.artifact.md §Module: Results
 */
/**
 * LEGACY, school-admin string-scored marks model. T-301 / X-6 / D-ASMT-1
 * designate [AssessmentsTable] + [AssessmentMarksTable] as the SINGLE
 * canonical marks model; this parallel table is DEPRECATED as a source of
 * truth.
 *
 * It is NOT dropped: 5 school-admin routing files (ResultsRouting,
 * SchoolAnalyticsRouting, SchoolStudentsRouting, AdminDashboard*Routing) still
 * read it (78 references). Per the constitution's "schema migrations are
 * non-destructive; don't break out-of-scope readers" rule (same pattern as the
 * T-201 attendance `grade`/`person_id` retention), migration_015 MIRRORS the
 * numeric-scored exam_results rows into `assessments`/`assessment_marks`
 * (idempotently, marked `type='exam'`, `created_by=NULL`) so the teacher
 * Gradebook history (T-304/T-306) is complete, while leaving exam_results
 * itself intact for the admin side. The admin readers will migrate onto the
 * canonical model in a future admin-portal pass; until then exam_results is
 * read-only legacy.
 */
@Deprecated("T-301 / X-6: superseded by AssessmentsTable + AssessmentMarksTable (the single canonical marks model). Retained read-only for the 5 school-admin readers; migration_015 mirrors numeric rows into assessments. Do NOT write new teacher marks here.")
object ExamResultsTable : UUIDTable("exam_results", "id") {
    val schoolId   = uuid("school_id")
    val test       = text("test")
    val className  = text("class_name")
    val subject    = text("subject")
    val studentId  = text("student_id")                                 // matches students.student_code
    val studentName = text("student_name")
    val imageUrl   = text("image_url").nullable()
    val attendance = varchar("attendance", 8).default("0%")             // e.g. "92%"
    val score      = varchar("score", 8).default("")                    // string keeps "98", "A+", "Pending"
    val status     = varchar("status", 16).default("Pending")           // Exceeding | Meeting | Below | Pending
    val trend      = varchar("trend", 8).default("0%")                  // e.g. "+2.4%"
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
    init {
        uniqueIndex("ux_exam_results_unique", schoolId, test, className, subject, studentId)
    }
}

// =====================================================================
// Teacher vertical (master doc Step 7 / §9 / gap G1)
//
// These tables back the `api/v1/teacher/*` routes consumed by the KMP
// client's `shared/feature/teacher` data layer. They are scoped through
// `teacher_subject_assignments` (who teaches what to which class+section)
// which already exists above. Attendance reuses `attendance_records`
// (student rows, marked_by = teacher). Marks are normalised here into a
// proper assessment → marks model (master doc G4: "normalized vs. the
// freeform academic_records / string-score exam_results").
//
// DDL: docs/backend/sql/02_teacher_schema.sql (idempotent, Supabase).
// =====================================================================

/**
 * A teacher-authored assessment definition (a "test"/"exam") for one
 * class+section+subject, with a max-marks denominator. Marks themselves
 * live in [AssessmentMarksTable]. This is the normalized counterpart to
 * the school-admin, string-scored [ExamResultsTable] — the teacher portal
 * needs a numeric max-marks denominator + an `exam_id` to enter scores
 * against (TeacherMarksData.maxMarks / SubmitMarksRequest.examId).
 *
 * `teacherId` is the assignment owner (app_users.id of the teacher).
 */
object AssessmentsTable : UUIDTable("assessments", "id") {
    val schoolId  = uuid("school_id")
    val teacherId = uuid("teacher_id").nullable()        // FK app_users.id (assessment author)
    val className = text("class_name")                   // denormalised for fast reads / display
    val section   = varchar("section", 8).default("A")
    val subject   = text("subject")
    val name       = text("name")                        // "Unit Test I", "Mid Term", …
    val maxMarks  = integer("max_marks").default(100)
    val examDate  = date("exam_date").nullable()         // T-004: typed `date` (was varchar12)
    val isActive  = bool("is_active").default(true)
    // RA-43: marks become parent-visible only once published. The teacher
    // marks-submit can publish; parent reads filter on isPublished = true.
    val isPublished = bool("is_published").default(false)
    val publishedAt = timestamp("published_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    // ---------------------------------------------------------------------
    // T-301 (Doc 07 §1.3) — canonical, typed, scope-bound assessment model.
    // These columns make `assessments` the SINGLE marks model (closes X-6,
    // D-ASMT-1..6). All nullable/defaulted so the migration (migration_015)
    // is non-destructive and the legacy columns above remain valid readers
    // (ParentAcademicsRouting, the legacy teacher /marks handler) until those
    // readers are repointed (T-303 / parent rebuild). Same additive pattern
    // as the T-201 attendance migration.
    // ---------------------------------------------------------------------
    val academicYearId  = uuid("academic_year_id").nullable()   // FK academic_years.id
    // X-1/D-ASMT-6: provable scope binding instead of free-text class/section/subject.
    val assignmentId    = uuid("assignment_id").nullable()      // FK teacher_subject_assignments.id
    val classId         = uuid("class_id").nullable()           // FK school_classes.id (canonical scope)
    val subjectId       = uuid("subject_id").nullable()         // FK school_subjects.id
    // D-ASMT-4: assessment type — scheduled|surprise|assignment|project|exam.
    val type            = varchar("type", 16).default("scheduled")
    // D-ASMT-4: pass mark (nullable; 0 <= pass_marks <= max_marks enforced by CHECK).
    val passMarks       = integer("pass_marks").nullable()
    // D-ASMT-5: calendar tie (an assessment can surface on the school calendar).
    val calendarEventId = uuid("calendar_event_id").nullable()  // FK calendar_events.id
    // The publish-discipline state machine (Doc 07 §2 / the B-MK-1 fix lives in T-303):
    //   draft → scheduled → marks_pending → published → archived.
    // `isPublished`/`publishedAt` above remain the legacy parent-visibility gate
    // until parent reads move onto `status='published'`; the migration keeps the
    // two in sync (status='published' ⇔ is_published=true).
    val status          = varchar("status", 16).default("draft")
    val createdBy       = uuid("created_by").nullable()         // FK app_users.id (author audit)
}

/**
 * Per-student score for an [AssessmentsTable] row. `marks` is a Double so a
 * teacher can enter half-marks; the route clamps it to [0, assessment.maxMarks].
 * One (assessment, student) pair is unique so re-submitting updates in place.
 */
object AssessmentMarksTable : UUIDTable("assessment_marks", "id") {
    val assessmentId = uuid("assessment_id")             // FK assessments.id
    @Deprecated("T-301 / D-ASMT-3: identity moves to studentRef (FK students.id). " +
        "This student_code text column is retained for the legacy /marks reader + " +
        "ParentAcademicsRouting until they are repointed (T-303 / parent rebuild).")
    val studentId    = text("student_id")                // students.student_code (legacy identity)
    @Deprecated("T-301 / X-4: name is joined from students for display, not stored as truth. " +
        "Retained nullable until legacy readers stop selecting it.")
    val studentName  = text("student_name")
    val marks        = double("marks").nullable()        // null = not yet entered
    val enteredBy    = uuid("entered_by").nullable()     // FK app_users.id (teacher)
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")

    // ---------------------------------------------------------------------
    // T-301 (Doc 07 §1.3) — typed per-student score.
    //   • studentRef : FK students.id (D-ASMT-3) — the canonical WHO; the
    //     student_code text above is kept only for legacy readers.
    //   • isAbsent   : "AB" distinct from a real 0 (Doc 07 §1.3).
    //   • remark     : optional per-student note.
    //   • enteredAt  : when the score was last entered/edited (audit).
    // Backfilled best-effort from student_code; nullable so the ADD is
    // non-destructive.
    // ---------------------------------------------------------------------
    val studentRef   = uuid("student_ref").nullable()    // FK students.id (typed identity, D-ASMT-3)
    val isAbsent     = bool("is_absent").default(false)  // AB ≠ 0
    val remark       = text("remark").nullable()
    val enteredAt    = timestamp("entered_at").nullable()
    init {
        uniqueIndex("ux_assessment_marks_unique", assessmentId, studentId)
    }
}

/**
 * Syllabus unit (chapter/topic) for a class+section+subject, with a covered
 * flag + the date it was marked covered. `position` orders units within a subject.
 *
 * T-403: this LEGACY flat table no longer backs the teacher syllabus surface —
 * the teacher plane moved to the typed [CurriculumUnitsTable] template +
 * [SyllabusProgressTable] per-section progress (see TeacherSyllabusRouting.kt).
 * It is RETAINED (not dropped) because it still backs read-only consumers:
 * ParentAcademicsRouting (parent syllabus view), SchoolIntelligenceRouting
 * (school-wide coverage), and the TeacherRouting dashboard progress rollup.
 * A future migration can backfill those onto the typed tables and drop this.
 */
object SyllabusUnitsTable : UUIDTable("syllabus_units", "id") {
    val schoolId   = uuid("school_id")
    val className  = text("class_name")
    val section    = varchar("section", 8).default("A")
    val subject    = text("subject")
    val title      = text("title")
    val position   = integer("position").default(0)
    val isCovered  = bool("is_covered").default(false)
    val coveredOn  = date("covered_on").nullable()         // T-004: typed `date` (was varchar12)
    val coveredBy  = uuid("covered_by").nullable()          // FK app_users.id (teacher)
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
}

/**
 * T-401 (Doc 08 §1.2) — the syllabus **template** (split from per-section
 * progress). A curriculum unit is a chapter or topic for a class+subject; a
 * NULL [parentId] is a top-level chapter, a non-null parent is a topic under it
 * (the chapter ▸ topic hierarchy that fixes D-SYL-4). The template is authored
 * once (per class+subject) and shared by every section, so two sections of the
 * same class track coverage independently in [SyllabusProgressTable]. This
 * resolves the free-text scope defect (D-SYL-3 / X-1) by binding to typed
 * class_id/subject_id.
 *
 * T-403 repointed the TEACHER readers/writers onto this typed pair and deleted
 * the legacy `/syllabus` handler. The legacy [SyllabusUnitsTable] is still
 * RETAINED (not dropped) because parent/school read-only consumers remain on it;
 * a future migration can backfill + drop it.
 */
object CurriculumUnitsTable : UUIDTable("curriculum_units", "id") {
    val schoolId  = uuid("school_id")
    val classId   = uuid("class_id")                     // FK school_classes.id (typed scope, X-1)
    val subjectId = uuid("subject_id")                   // FK school_subjects.id
    val parentId  = uuid("parent_id").nullable()         // FK curriculum_units.id — chapter ▸ topic (D-SYL-4)
    val title     = text("title")
    val position  = integer("position").default(0)
    val isActive  = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * T-401 (Doc 08 §1.2) — per-section coverage **state** for a curriculum unit.
 * Keyed UNIQUE on (unit, section, assignment) so the same template unit can be
 * "covered" independently for each section a teacher owns. [coveredOn] is typed
 * `date` (D-SYL-2 / X-3). The one-tap toggle (T-402 PATCH /syllabus/progress)
 * upserts a row here; [assignmentId] binds the write to a TSA the teacher owns
 * (X-1, enforced via requireOwnedAssignment).
 */
object SyllabusProgressTable : UUIDTable("syllabus_progress", "id") {
    val unitId       = uuid("unit_id")                   // FK curriculum_units.id
    val section      = varchar("section", 8).default("A")
    val assignmentId = uuid("assignment_id")             // FK teacher_subject_assignments.id (scope, X-1)
    val isCovered    = bool("is_covered").default(false)
    val coveredOn    = date("covered_on").nullable()     // TYPED (D-SYL-2)
    val coveredBy    = uuid("covered_by").nullable()     // FK app_users.id (teacher who toggled)
    val note         = text("note").nullable()
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
    init {
        uniqueIndex("ux_syllabus_progress_unique", unitId, section, assignmentId)
    }
}

/**
 * A homework/assignment authored by a teacher for one class+section+subject.
 * `submittedCount` is derived live from [HomeworkSubmissionsTable] at read
 * time; `totalCount` is the headcount of the target class (computed from
 * enrollments).
 *
 * T-404 (Doc 08 §5.3) — added the TYPED SCOPE spine ([assignmentId]/[classId]/
 * [subjectId], X-1) so the T-405 backend can enforce allocation via
 * requireOwnedAssignment instead of free-text class/section/subject (D-HW-1);
 * an optional [dueTime] so "is it past due?" is real date+time math (D-HW-2);
 * and the [allowLate] teacher policy that drives the no-submit-past-due rule
 * (D-HW-4). The legacy display columns ([className]/[section]/[subject]) are
 * RETAINED (not dropped) so the existing /homework GET+POST handler/screen keep
 * compiling green until T-405/T-406 repoint them (DELETE-don't-patch); the typed
 * ids are nullable + best-effort backfilled in migration_017_homework.sql.
 */
object HomeworkTable : UUIDTable("homework", "id") {
    val schoolId    = uuid("school_id")
    val teacherId   = uuid("teacher_id").nullable()     // FK app_users.id (author)
    // T-404: typed scope (X-1) — authoritative once backfilled; nullable for
    // back-compat with rows written before the binding existed.
    val assignmentId = uuid("assignment_id").nullable() // FK teacher_subject_assignments.id
    val classId      = uuid("class_id").nullable()      // FK school_classes.id
    val subjectId    = uuid("subject_id").nullable()    // FK school_subjects.id
    // Legacy DISPLAY columns (demoted; retained until T-405/T-406 — Rule 4).
    val className   = text("class_name")
    val section     = varchar("section", 8).default("A")
    val subject     = text("subject")
    val title       = text("title")
    val description = text("description").default("")
    val dueDate     = date("due_date")                  // T-004: typed `date` (was varchar12)
    val dueTime     = time("due_time").nullable()       // T-404: optional cutoff time (D-HW-2)
    val allowLate   = bool("allow_late").default(false) // T-404: teacher policy (D-HW-4)
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

/**
 * T-404 (Doc 08 §5.3) — a file/image attached to a homework (D-HW-3 / B-HW-5).
 * One homework can carry several. [uploadedBy] is the author (app_users.id).
 */
object HomeworkAttachmentsTable : UUIDTable("homework_attachments", "id") {
    val homeworkId  = uuid("homework_id")               // FK homework.id (CASCADE)
    val url         = text("url")
    val filename    = text("filename").default("")
    val mime        = text("mime").default("")
    val sizeBytes   = long("size_bytes").default(0)
    val uploadedBy  = uuid("uploaded_by").nullable()    // FK app_users.id
    val createdAt   = timestamp("created_at")
}

/**
 * One student's submission against a [HomeworkTable] row. Used to compute the
 * submitted/total ratio AND the submissions board (T-405). Unique on
 * (homework, student).
 *
 * T-404 (Doc 08 §5.3) — added a TYPED [studentUuid] FK to students (B-HW-6;
 * the legacy text [studentId] = student_code is retained for back-compat until
 * T-405 repoints readers), plus the lifecycle columns the board reads/writes:
 * [grade], [reviewedBy], [reviewedAt]. `status` now includes 'not_submitted'
 * (the board mostly derives that set by roster LEFT JOIN at read time, but the
 * column tolerates a materialised value). [submittedAt] is nullable (a
 * not-submitted/late-pending row has no submission time).
 */
object HomeworkSubmissionsTable : UUIDTable("homework_submissions", "id") {
    val homeworkId  = uuid("homework_id")               // FK homework.id (CASCADE)
    val studentId   = text("student_id")                // legacy: students.student_code
    val studentUuid = uuid("student_uuid").nullable()   // T-404: typed FK students.id (B-HW-6)
    // submitted | late | graded | not_submitted  (T-404 widened to text + check).
    val status      = text("status").default("submitted")
    val submittedAt = timestamp("submitted_at").nullable()
    val grade       = text("grade").nullable()          // T-404: optional grade/feedback
    val reviewedBy  = uuid("reviewed_by").nullable()    // T-404: FK app_users.id
    val reviewedAt  = timestamp("reviewed_at").nullable()
    init {
        uniqueIndex("ux_homework_submissions_unique", homeworkId, studentId)
    }
}

/**
 * T-404 (Doc 08 §5.3 / §7) — a teacher override of a homework's cutoff (D-HW-5).
 * [studentId] NULL = a whole-class extension (moves the cutoff for everyone);
 * non-null = reopen submission for that one student (the "she was sick" case).
 * Logged with [grantedBy]/[createdAt]/[reason] and reflected on the board.
 */
object HomeworkExtensionsTable : UUIDTable("homework_extensions", "id") {
    val homeworkId  = uuid("homework_id")               // FK homework.id (CASCADE)
    val studentId   = uuid("student_id").nullable()     // NULL = whole class; else FK students.id
    val newDueDate  = date("new_due_date")
    val newDueTime  = time("new_due_time").nullable()
    val grantedBy   = uuid("granted_by").nullable()     // FK app_users.id
    val reason      = text("reason").nullable()
    val createdAt   = timestamp("created_at")
}

/**
 * Optional timetable: a teacher's periods on a given weekday. The teacher Home
 * "today's periods" strip reads this; when a school hasn't entered a timetable
 * the response is an honest empty list (never fabricated). `weekday` is 1..7
 * (Mon..Sun) to match java.time.DayOfWeek.value.
 */
object TeacherPeriodsTable : UUIDTable("teacher_periods", "id") {
    val schoolId   = uuid("school_id")
    val teacherId  = uuid("teacher_id")                 // FK app_users.id
    val weekday    = integer("weekday")                 // 1=Mon … 7=Sun (ISO, documented — D-TT-1)
    // T-101 (Doc 05 §2.1): start_time/end_time promoted varchar("HH:mm") → typed
    // `time` (LocalTime) so the resolved-day computation can compare without
    // string parsing (D-TT-2). The wire contract is preserved: every consumer
    // formats LocalTime back to "HH:mm" at the DTO boundary.
    val startTime  = time("start_time")                 // TYPED (was varchar8 "HH:mm")
    val endTime    = time("end_time")                   // TYPED (was varchar8 "HH:mm")
    // T-101 NEW (Doc 05 §2.1): term validity + the TSA this period implements.
    // academic_year_id / assignment_id are nullable for back-compat with rows
    // written before the binding existed; backfill is best-effort in the
    // migration. assignment_id is the linchpin (D-TT-4): "current period" →
    // assignment_id → requireOwnedAssignment → authorized attendance/marks scope.
    val academicYearId = uuid("academic_year_id").nullable()  // FK academic_years.id
    val assignmentId   = uuid("assignment_id").nullable()     // FK teacher_subject_assignments.id
    // DEVIATION (Doc 05 §2.1 says "DROP className/section/subject as truth"):
    // the columns are KEPT as display-only/legacy here (NOT dropped) so the
    // existing timetable readers (Parent/School/Teacher routing) keep compiling
    // green in this commit (Rule 4). assignment_id is now the source of truth;
    // the legacy columns are demoted to a denormalised display fallback and a
    // later phase drops them once all readers derive class/subject via the FK.
    val className  = text("class_name")                 // DISPLAY/legacy (demoted)
    val section    = varchar("section", 8).default("A") // DISPLAY/legacy (demoted)
    val subject    = text("subject")                    // DISPLAY/legacy (demoted)
    val room       = text("room").default("")
    val position   = integer("position").default(0)
    val validFrom  = date("valid_from").nullable()      // T-101: term-revision window
    val validTo    = date("valid_to").nullable()
    val isActive   = bool("is_active").default(true)    // T-101: soft-disable a period
    val createdAt  = timestamp("created_at")
    init {
        // Doc 05 §2.1: no double-booking a teacher in the same weekday slot.
        uniqueIndex("ux_periods_no_double_book", schoolId, teacherId, weekday, startTime)
    }
}

/**
 * T-101 (Doc 05 §2.2) — one-off overrides to the recurring weekly pattern, so
 * the resolved-day computation for a SPECIFIC date is correct: a normally
 * scheduled period can be CANCELLED (attendance not expected — Doc 06 edge
 * case), RESCHEDULED, have a ROOM_CHANGE, a SUBSTITUTION inserted, or an EXTRA
 * period added for one date only.
 */
object PeriodExceptionsTable : UUIDTable("period_exceptions", "id") {
    val schoolId            = uuid("school_id")
    val periodId            = uuid("period_id").nullable()   // FK teacher_periods.id (null for EXTRA)
    val date                = date("date")                   // the specific date this applies to
    val kind                = varchar("kind", 16)            // CANCELLED|RESCHEDULED|ROOM_CHANGE|SUBSTITUTION|EXTRA
    val newStart            = time("new_start").nullable()
    val newEnd              = time("new_end").nullable()
    val newRoom             = text("new_room").nullable()
    val substituteTeacherId = uuid("substitute_teacher_id").nullable()  // FK app_users.id
    // EXTRA periods aren't tied to a recurring row, so they carry their own
    // assignment binding + display fallback (same demotion rule as periods).
    val assignmentId        = uuid("assignment_id").nullable()          // FK teacher_subject_assignments.id
    val note                = text("note").default("")
    val createdAt           = timestamp("created_at")
    val updatedAt           = timestamp("updated_at")
    init {
        uniqueIndex("ux_period_exceptions_unique", schoolId, periodId, date, kind)
    }
}

// =====================================================================
// Parent Scholarships (parent_api_spec.artifact.md §Module: Scholarships)
// =====================================================================

/**
 * Scholarship opportunities surfaced on the parent Scholarships screen.
 *
 * Replaces the hardcoded `"$45,000 STEM Award"` fiction (audit §4.2/§5.2) the
 * `/scholarships` route used to return. Rows are operator-curated (seeded /
 * managed) opportunities; the endpoint reads real rows from here so adding or
 * retiring a scholarship is a DB write, not a redeploy. `isActive=false` hides
 * a row without deleting it. `position` controls display order.
 */
object ScholarshipsTable : UUIDTable("scholarships", "id") {
    val title       = text("title")
    val description = text("description")
    val amount      = text("amount")                    // display string e.g. "₹45,000"
    val timeLeft    = text("time_left").default("")      // display string e.g. "3d : 12h"
    val category    = varchar("category", 48).default("Merit Based")
    val isCritical  = bool("is_critical").default(false)
    val position    = integer("position").default(0)
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

/**
 * A parent's scholarship applications (the "applications" list on the same
 * screen). Scoped to the applying parent so each parent sees only their own.
 * `iconName` mirrors the UI's institution glyph.
 */
object ScholarshipApplicationsTable : UUIDTable("scholarship_applications", "id") {
    val parentId    = uuid("parent_id")                 // FK app_users.id
    val institution = text("institution")
    val program     = text("program")
    val status      = varchar("status", 24).default("Received") // Received | Under Review | Shortlisted
    val iconName    = varchar("icon_name", 32).default("school")
    val position    = integer("position").default(0)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

// =====================================================================
// notifications  (RA-41/42/46/50 — the cross-user notification spine)
// =====================================================================
/**
 * Recipient-scoped notification rows. Every row targets exactly one recipient
 * (`userId` = the app_users.id the bell belongs to) and carries the originating
 * `schoolId` for tenant isolation. The Notify helper writes here; the role-aware
 * GET /notifications reads by `userId = jwt.sub`; /summary returns the unread
 * count for the portal bells (kills the hardcoded red-dot RA-50 and the
 * parent-only synth that broke for admins/teachers RA-42).
 */
object NotificationsTable : UUIDTable("notifications", "id") {
    val schoolId   = uuid("school_id").nullable()
    val userId     = uuid("user_id")                    // recipient app_users.id
    val category   = varchar("category", 32).default("general") // attendance|marks|homework|announcement|leave|fees|link|general
    val title      = text("title")
    val body       = text("body").default("")
    val deepLink   = text("deep_link").nullable()
    val actorId    = uuid("actor_id").nullable()        // who triggered it (teacher/admin/parent)
    val refType    = varchar("ref_type", 32).nullable() // e.g. attendance_record|assessment|homework|announcement|leave_request|fee_record|parent_child_link
    val refId      = text("ref_id").nullable()
    val isRead     = bool("is_read").default(false)
    val createdAt  = timestamp("created_at")
    val readAt     = timestamp("read_at").nullable()
    // Idempotency key to prevent duplicate notifications for the same event.
    val idempotencyKey = varchar("idempotency_key", 128).nullable()
    // Soft-archive timestamp (null = active, non-null = archived).
    val archivedAt = timestamp("archived_at").nullable()

    init {
        // Fast lookup of a user's unread notifications (bell summary).
        index("ix_notifications_user_unread", isUnique = false, userId, isRead)
        // Fast lookup by category for rate-limiting checks.
        index("ix_notifications_user_category", isUnique = false, userId, category)
        // Idempotency dedup lookup.
        index("ix_notifications_idempotency", isUnique = false, idempotencyKey)
    }
}

// =====================================================================
// device_tokens  (RA-41 push — FCM/APNs registry; dispatch via Admin SDK)
// =====================================================================
//
// Notification foundation (feature/setup_notification):
//   A user may own MULTIPLE devices (phone + tablet + another phone). We
//   intentionally do NOT overwrite previous tokens — every active token
//   stays valid and is fanned out to independently. Registration is keyed
//   by the (token) value itself; re-registering an existing token merely
//   refreshes its metadata + last_seen_at and re-asserts is_active=true.
//
// Columns mirror database/migrations/setup_notification_foundation.sql
// (the canonical Supabase migration) EXACTLY so the Exposed mapping lines
// up with the real Postgres schema.
object DeviceTokensTable : UUIDTable("device_tokens", "id") {
    val schoolId    = uuid("school_id").nullable()       // optional tenant scope (admin/teacher tokens)
    val userId      = uuid("user_id")                     // owner app_users.id
    val token       = text("token")
    val platform    = varchar("platform", 16).default("android") // android | ios | web
    val appVersion  = varchar("app_version", 64).nullable()
    val deviceModel = varchar("device_model", 128).nullable()
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
    val lastSeenAt  = timestamp("last_seen_at").nullable()

    init {
        // A token string is globally unique (FCM issues one per install).
        // Re-registering the same token from a different user (rare, e.g. a
        // handed-down device) re-points the owner via an UPDATE, never a dup.
        uniqueIndex("ux_device_tokens_token", token)
        // Fast lookup of every active token for a user (multi-device fan-out).
        index(
            customIndexName = "ix_device_tokens_user_active",
            isUnique = false,
            userId,
            isActive
        )
    }
}

// =====================================================================
// otp_gateway_devices  (feature/setup_notification — OTPSender gateway registry)
// =====================================================================
//
// The OTPSender Android app is an SMS GATEWAY: instead of paying an SMS vendor,
// the backend creates an SMS request, sends an FCM data-message to a registered
// OTPSender phone, and that phone sends the SMS from its own SIM. This table is
// the registry of those gateway devices.
//
//   POST /api/v1/gateway/register   -> upsert a row (deviceId is the natural key)
//   POST /api/v1/gateway/heartbeat   -> refresh last_seen_at + battery/network
//
// Device selection (OtpService): is_active = true AND last_seen_at within the
// liveness window (5 min); prefer the most recently active device. Columns
// mirror database/migrations/setup_otp_gateway.sql EXACTLY.
object OtpGatewayDevicesTable : UUIDTable("otp_gateway_devices", "id") {
    val deviceId    = varchar("device_id", 128)                 // natural key — OTPSender install id
    val deviceName  = varchar("device_name", 128).nullable()    // human label (e.g. "Office Pixel")
    val fcmToken    = text("fcm_token")                          // OTPSender FCM token (separate project)
    val appVersion  = varchar("app_version", 64).nullable()     // OTPSender BuildConfig.VERSION_NAME
    val isActive    = bool("is_active").default(true)           // soft-deactivated when retired
    val batteryLevel = integer("battery_level").nullable()      // 0..100, from heartbeat
    val networkType = varchar("network_type", 32).nullable()    // wifi | cellular | …, from heartbeat
    val lastSeenAt  = timestamp("last_seen_at").nullable()      // bumped by register + heartbeat
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")

    init {
        // A device_id is globally unique (one OTPSender install). Re-registering
        // the same device_id refreshes its row via UPDATE — never a duplicate.
        uniqueIndex("ux_otp_gateway_devices_device_id", deviceId)
        // Fast selection of the freshest active gateway for dispatch.
        index(
            customIndexName = "ix_otp_gateway_devices_active_seen",
            isUnique = false,
            isActive,
            lastSeenAt
        )
    }
}

// =====================================================================
// sms_requests  (feature/setup_notification — OTPSender SMS request queue)
// =====================================================================
//
// One row per OTP-SMS the backend asks an OTPSender gateway to deliver. The
// OTP itself is generated/stored in auth_otps; this table is the DELIVERY
// request the gateway polls / receives via FCM and reports back on.
//
//   (OtpService) create row (status=pending) -> FCM data-message to gateway
//   GET  /api/v1/gateway/requests/{requestId}            -> gateway fetches detail
//   GET  /api/v1/gateway/pending                          -> recovery: list pending
//   POST /api/v1/gateway/requests/{requestId}/status      -> gateway reports SENT|FAILED
//
// `request_id` is the public, client-facing id (a UUID string returned to the
// auth caller and echoed in the FCM payload); `id` is the internal PK.
// Columns mirror database/migrations/setup_otp_gateway.sql EXACTLY.
object SmsRequestsTable : UUIDTable("sms_requests", "id") {
    val requestId    = varchar("request_id", 64)                 // public id (UUID string)
    val phoneNumber  = varchar("phone_number", 32)               // E.164 destination
    val otp          = varchar("otp", 12).nullable()             // plaintext code the gateway must send
    val message      = text("message")                           // full SMS body
    val status       = varchar("status", 16).default("pending")  // pending | dispatched | sent | failed
    val deviceId     = varchar("device_id", 128).nullable()      // assigned OTPSender device_id (null while pending)
    val errorMessage = text("error_message").nullable()          // populated when status=failed
    val purpose      = varchar("purpose", 24).default("login")   // login | signup | …
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
    val dispatchedAt = timestamp("dispatched_at").nullable()     // when FCM was sent to the gateway
    val sentAt       = timestamp("sent_at").nullable()           // when the gateway reported SENT

    init {
        // request_id is the public lookup key for the gateway status callbacks.
        uniqueIndex("ux_sms_requests_request_id", requestId)
        // Recovery query: pending requests, oldest first.
        index(
            customIndexName = "ix_sms_requests_status_created",
            isUnique = false,
            status,
            createdAt
        )
    }
}

// =====================================================================
// parent_child_links  (RA-48 — parent→child link approval workflow)
// =====================================================================
/**
 * A pending/approved/rejected request from a parent to link a child. The live
 * link still materialises in `children` on approval; this table records the
 * approval state so a school admin can vet roll-number claims (RA-03/RA-48)
 * before a parent gains read access to a student's academic data.
 */
object ParentChildLinksTable : UUIDTable("parent_child_links", "id") {
    val parentId    = uuid("parent_id")
    val schoolId    = uuid("school_id").nullable()
    val studentCode = varchar("student_code", 64).nullable()
    val rollNumber  = varchar("roll_number", 64).nullable()
    val childName   = text("child_name").nullable()
    // ISSUE 2c: the class+section the parent provided for the matched student,
    // captured so the admin queue can show the full claim and the matcher can
    // compare against the roster. Nullable so existing rows parse unchanged.
    val className   = text("class_name").nullable()
    val section     = varchar("section", 16).nullable()
    // ISSUE 2d: the parent phone that was on the request, and a flag for the
    // "needs review" (phone-mismatch) bucket so it is never silently dropped.
    val parentPhone = varchar("parent_phone", 32).nullable()
    val reviewReason = text("review_reason").nullable()
    val childId     = uuid("child_id").nullable()       // children.id once approved
    // status: pending | approved | rejected | needs_review (ISSUE 2d)
    val status      = varchar("status", 24).default("pending")
    // RA-SP: first-class parent relationship metadata. `relation` describes the
    // guardian role (Father | Mother | Guardian | …) and `isPrimaryGuardian`
    // marks the single primary point-of-contact for a student. The aggregation
    // service enforces AT MOST ONE primary guardian per (school, student_code);
    // both are nullable/defaulted so existing rows keep parsing unchanged.
    val relation          = varchar("relation", 32).nullable()
    val isPrimaryGuardian = bool("is_primary_guardian").default(false)
    val requestedAt = timestamp("requested_at")
    val actionedBy  = uuid("actioned_by").nullable()
    val actionedAt  = timestamp("actioned_at").nullable()
}

// =====================================================================
// non_teaching_staff  (RA-S17 — Admin People "Non-teaching staff" vertical)
// =====================================================================
/**
 * RA-S17: a school's NON-teaching staff (admin office, accountant, librarian,
 * support, security, transport, etc.). These are *not* `app_users` — they do
 * not log in; they are roster records the admin manages alongside Teachers and
 * Students on the People tab. School-scoped and soft-deletable, mirroring the
 * `students` pattern. Deletion is performed from the staff profile behind a
 * confirm dialog (never a direct list-row button).
 */
object NonTeachingStaffTable : UUIDTable("non_teaching_staff", "id") {
    val schoolId    = uuid("school_id")                         // FK schools.id — tenant scope
    val fullName    = text("full_name")
    val role        = text("role")                              // e.g. "Accountant", "Librarian"
    val department  = text("department").nullable()             // e.g. "Office", "Transport"
    val phone       = varchar("phone", 32).nullable()
    val email       = text("email").nullable()
    val photoUrl    = text("photo_url").nullable()
    val isActive    = bool("is_active").default(true)           // soft-delete flag
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

// =====================================================================
// parent_achievements  (Parents Portal — Profile tab "Missions & Achievements")
// =====================================================================
/**
 * Per-child achievements / missions surfaced on the rebuilt Parents Portal **Profile tab**
 * (the swipe-down "Missions & Achievements" sheet) and the holistic Track-Progress view.
 *
 * BACKWARD-COMPATIBLE BY DESIGN: the `/api/v1/parent/track-progress` endpoint already works
 * WITHOUT this table — it falls back to the `app_config` CMS templates + locally-derived
 * achievements. This table is the OPTIONAL upgrade path for schools/operators that want to
 * store REAL, per-child earned achievements instead of CMS-wide templates.
 *
 * IDEMPOTENT PROVISIONING: the matching SQL (docs/db/migration_004_parent_achievements.sql)
 * is fully guarded with `CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`, so running
 * it against a Supabase that already has the table is a harmless no-op — never an error.
 *
 * `kind` discriminates the row so one table powers every section of the sheet:
 *   BADGE        — a collectible achievement badge (title + icon + earned/locked + colours)
 *   COMPETENCY   — a NEP-aligned academic competency bar (title + 0..1 progress)
 *   EI_METRIC    — an emotional-intelligence metric (title + 0..1 value)
 *   MISSION      — a play & discovery milestone (title + description + MET/IN_PROGRESS/LOCKED)
 */
object ParentAchievementsTable : UUIDTable("parent_achievements", "id") {
    val childId     = uuid("child_id")                          // FK children.id — whose achievement
    val schoolId    = uuid("school_id").nullable()             // FK schools.id (optional tenant scope)
    val kind        = varchar("kind", 16)                      // BADGE | COMPETENCY | EI_METRIC | MISSION
    val title       = text("title")
    val description = text("description").nullable()
    val icon        = varchar("icon", 64).nullable()          // Material symbol name (e.g. "verified")
    // Hex colour stops for badges, JSON array string e.g. ["#B6C7EB","#006C49"].
    val colors      = text("colors").default("[]")
    // 0..1 progress for COMPETENCY / EI_METRIC rows; null for BADGE / MISSION.
    val progress    = double("progress").nullable()
    // Lifecycle: BADGE/MISSION use status (EARNED|LOCKED|MET|IN_PROGRESS); bars ignore it.
    val status      = varchar("status", 16).default("LOCKED")
    val isLocked    = bool("is_locked").default(true)
    val sortOrder   = integer("sort_order").default(0)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")

    init {
        index("ix_parent_achievements_child", false, childId)
        index("ix_parent_achievements_child_kind", false, childId, kind)
    }
}

// =====================================================================
// Academic Calendar Platform (VP-CAL)
// =====================================================================
//
// The Academic Calendar module is the school's centralized planning &
// scheduling system. It supersedes the legacy read-only `academic_calendar`
// table (which only ever held simple title/date rows surfaced by the old
// GET /api/v1/school/calendar grid) with a first-class, mutable event model
// that supports types, status (draft/published lifecycle), audience targeting,
// notifications, source tracking (manual vs announcement) and multi-day ranges.
//
// We deliberately introduce a NEW table (`calendar_events`) instead of widening
// `academic_calendar` so that:
//   - the legacy month-grid endpoint keeps working unchanged (no behaviour drift)
//   - the new platform owns its richer schema without back-compat baggage
//
// All rows are school-scoped (multi-tenant) and soft-deletable.

/**
 * VP-CAL: a single planned item in the Academic Calendar (event / holiday /
 * exam / PTM / activity / administrative). This is the operational scheduling
 * record the Academic Calendar platform reads & writes.
 */
object CalendarEventsTable : UUIDTable("calendar_events", "id") {
    val schoolId        = uuid("school_id")                     // FK schools.id — tenant scope
    val eventCode       = text("event_code").uniqueIndex()      // stable external id, e.g. CAL_AB12CD34
    // Optional link to the academic year this event belongs to (nullable so an
    // event can exist before a year is formally created — defaults to active).
    val academicYearId  = uuid("academic_year_id").nullable()
    val title           = text("title")
    val description     = text("description").default("")
    // type: EXAM | HOLIDAY | PTM | SCHOOL_EVENT | ACTIVITY | ADMINISTRATIVE | MILESTONE
    val type            = varchar("type", 24).default("SCHOOL_EVENT")
    // status: DRAFT | PUBLISHED | CANCELLED | COMPLETED
    val status          = varchar("status", 16).default("DRAFT")
    // source: MANUAL | ANNOUNCEMENT  (where the event originated)
    val eventSource          = varchar("source", 16).default("MANUAL")
    // When source = ANNOUNCEMENT, the originating announcement's event_id so we
    // can keep the two in sync and avoid duplicate workflows.
    val sourceRef       = text("source_ref").nullable()
    val startDate       = date("start_date")                    // T-004: typed `date` (was varchar12)
    val endDate         = date("end_date")                      // T-004: typed `date` (== startDate for single-day)
    val allDay          = bool("all_day").default(true)
    val bannerUrl       = text("banner_url").nullable()
    val icon            = text("icon").nullable()
    // audience: ALL_SCHOOL | GRADES | CLASSES | SECTIONS | TEACHERS | PARENTS | STUDENTS
    val audience        = varchar("audience", 16).default("ALL_SCHOOL")
    val classIds        = text("class_ids").nullable()          // JSON array (string) of class/grade ids
    val sectionIds      = text("section_ids").nullable()        // JSON array (string) of section ids
    val notifyStudents  = bool("notify_students").default(false)
    val notifyParents   = bool("notify_parents").default(false)
    val notifyTeachers  = bool("notify_teachers").default(false)
    // Highlights a milestone (session start, quarters, finals, session end).
    val isMilestone     = bool("is_milestone").default(false)
    val isActive        = bool("is_active").default(true)       // soft-delete flag
    val createdBy       = uuid("created_by").nullable()
    val updatedBy       = uuid("updated_by").nullable()
    val createdAt       = timestamp("created_at")
    val updatedAt       = timestamp("updated_at")
    // Notification scheduler: marks whether a reminder has been sent for this event.
    val reminderSent    = bool("reminder_sent").default(false)
}

/**
 * VP-CAL: an Academic Year. Replaces the "Academic Year (Coming Soon)" settings
 * placeholder with a real, manageable record. A school can have many years but
 * only ONE active at a time (enforced in the routing layer). Calendar events
 * reference a year via `calendar_events.academic_year_id`.
 *
 * status: DRAFT | ACTIVE | ARCHIVED
 */
object AcademicYearsTable : UUIDTable("academic_years", "id") {
    val schoolId      = uuid("school_id")                       // FK schools.id — tenant scope
    val name          = text("name")                            // e.g. "2026-27"
    val startDate     = varchar("start_date", 12)               // YYYY-MM-DD
    val endDate       = varchar("end_date", 12)                 // YYYY-MM-DD
    val isActive      = bool("is_active").default(false)
    val status        = varchar("status", 16).default("DRAFT")  // DRAFT | ACTIVE | ARCHIVED
    // Planning numbers the admin sets (or that get computed); kept nullable so a
    // freshly created year doesn't have to specify them up-front.
    val academicDays  = integer("academic_days").nullable()
    val holidayDays   = integer("holiday_days").nullable()
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")
}

// =====================================================================
// teacher_check_ins  (Teacher Portal Rebuild — Doc 11 T-106a / Doc 06 §1.3)
//   The authoritative one-row-per-teacher-per-day self check-in record that
//   powers the Today-tab check-in band (Doc 04 §5.1) and the biometric ladder
//   (Doc 06 §2: biometric -> PIN -> manual, always-available fallback, never a
//   hard gate). Closes B-ATT-5 (teacher self check-in).
//
//   `checked_in_at` is SERVER-STAMPED by POST /api/v1/teacher/checkin (T-106b)
//   with the server clock — NOT the device clock (Doc 06 §2.4 clock-skew edge).
//   `method` records which rung of the ladder succeeded.
//   UNIQUE (school_id, teacher_id, date) is the idempotency key: a second POST
//   for the same (school, teacher, date) returns the existing row, it does not
//   duplicate.
//
//   FK: teacher_id -> app_users.id. `device_id` is optional (the DTO makes it
//   nullable and the prefs layer has no device-id accessor, so callers pass
//   null for now).
//
//   Created/applied by docs/db/migration_013_teacher_checkins.sql. Registered in
//   DatabaseFactory.allTables — AUTO_CREATE_TABLES is OFF in production, so
//   that migration MUST be applied in Supabase before the matching deploy or
//   validateSchema() refuses to boot.
//
//   NOTE (per LAWS): Doc 06 §1.3 specifies exactly the columns below — no
//   separate `created_at` audit column (the row IS the check-in event, its
//   authoritative timestamp is `checked_in_at`). This mapping is faithful to
//   the authority.
// =====================================================================
object TeacherCheckInsTable : UUIDTable("teacher_check_ins", "id") {
    val schoolId    = uuid("school_id")
    val teacherId   = uuid("teacher_id")                 // FK app_users.id
    val date        = date("date")
    val checkedInAt = timestamp("checked_in_at")         // server-stamped (authoritative clock)
    val method      = varchar("method", 16)              // biometric | pin | manual
    val deviceId    = text("device_id").nullable()
    init {
        uniqueIndex(
            "ux_teacher_checkins_unique",
            schoolId, teacherId, date
        )
    }
}

// =====================================================================
// notification_preferences  (per-user, per-category notification prefs)
// =====================================================================
//
// Allows each user to enable/disable notifications per category and select
// a custom sound. When a row does not exist for a (user_id, category) pair,
// the default is enabled=true. Notify.toUsers() checks this table before
// inserting a notification row and dispatching push.
object NotificationPreferencesTable : UUIDTable("notification_preferences", "id") {
    val userId     = uuid("user_id")
    val category   = varchar("category", 32)               // attendance|marks|announcement|leave|fees|...
    val enabled    = bool("enabled").default(true)
    val sound      = varchar("sound", 64).nullable()       // sound resource name or null for default
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")

    init {
        // One preference row per user per category.
        uniqueIndex("ux_notif_prefs_user_category", userId, category)
    }
}

// =====================================================================
// Lesson Planning (LESSON_PLANNING_SPEC.md — P1-20)
//
// Three tables back the teacher lesson-plan surface:
//   1. LessonPlansTable          — the plan itself (scoped to a TSA, X-1)
//   2. LessonPlanTemplatesTable  — reusable templates (separate shape/lifecycle)
//   3. LessonPlanAttachmentsTable — URL-based file/link attachments
//
// Created/applied by docs/db/migration_025_lesson_planning.sql. Registered
// in DatabaseFactory.allTables — AUTO_CREATE_TABLES is OFF in production, so
// that migration MUST be applied in Supabase before the matching deploy or
// validateSchema() refuses to boot.
// =====================================================================

/**
 * A lesson plan authored by a teacher for one class+section+subject, scoped
 * to a [TeacherSubjectAssignmentsTable] row (X-1 ownership). Every read/write
 * is guarded by `requireOwnedAssignment()`. `teacher_id`, `class_id`,
 * `section`, `subject_id`, `subject_name` are denormalised from the TSA for
 * query convenience (same pattern as [HomeworkTable]).
 *
 * On completion (`status = "completed"`), if `curriculumUnitId` is set, the
 * handler upserts [SyllabusProgressTable] (isCovered=true, coveredOn=today,
 * coveredBy=teacher) keyed on `(unitId, section, assignmentId)`.
 *
 * `deleted_at` soft-delete: all queries filter `WHERE deleted_at IS NULL`.
 */
object LessonPlansTable : UUIDTable("lesson_plans", "id") {
    val schoolId           = uuid("school_id")
    val teacherId          = uuid("teacher_id")
    val assignmentId       = uuid("assignment_id")             // FK teacher_subject_assignments.id (X-1 scope)
    val classId            = uuid("class_id")                   // denormalised from TSA
    val section            = varchar("section", 8).default("A") // denormalised from TSA
    val subjectId          = uuid("subject_id").nullable()      // denormalised from TSA
    val subjectName        = text("subject_name")               // display column
    val curriculumUnitId   = uuid("curriculum_unit_id").nullable() // FK curriculum_units.id (optional)
    val title              = text("title")
    val objectives         = text("objectives")                 // JSON array: ["Objective 1", ...]
    val activities         = text("activities").nullable()      // JSON: [{"activity":"...","duration_min":15}]
    val resources          = text("resources").nullable()       // JSON: ["Textbook pg 45", ...]
    val assessmentMethod   = text("assessment_method").nullable()
    val durationMinutes    = integer("duration_minutes").default(45)
    val homeworkId         = uuid("homework_id").nullable()     // FK homework.id (optional)
    val plannedDate        = date("planned_date").nullable()
    val completedAt        = timestamp("completed_at").nullable()
    val status             = varchar("status", 16).default("planned") // planned | completed | skipped
    val isTemplate         = bool("is_template").default(false)
    val templateSourceId   = uuid("template_source_id").nullable() // FK lesson_plans.id (if from a template)
    val deletedAt          = timestamp("deleted_at").nullable() // soft delete
    val createdAt          = timestamp("created_at")
    val updatedAt          = timestamp("updated_at")
}

/**
 * A reusable lesson-plan template. Separate table because templates have no
 * `planned_date`, no `status`, no `homework_id`, no `completed_at` — a
 * different shape and lifecycle. `is_shared` enables cross-teacher reuse
 * within a school (future co-planning foundation).
 */
object LessonPlanTemplatesTable : UUIDTable("lesson_plan_templates", "id") {
    val schoolId           = uuid("school_id")
    val teacherId          = uuid("teacher_id")
    val assignmentId       = uuid("assignment_id")             // FK teacher_subject_assignments.id (scope)
    val subjectName        = text("subject_name")
    val title              = text("title")
    val objectives         = text("objectives")                 // JSON array
    val activities         = text("activities").nullable()      // JSON array
    val resources          = text("resources").nullable()       // JSON array
    val assessmentMethod   = text("assessment_method").nullable()
    val durationMinutes    = integer("duration_minutes").default(45)
    val isShared           = bool("is_shared").default(false)
    val deletedAt          = timestamp("deleted_at").nullable()
    val createdAt          = timestamp("created_at")
    val updatedAt          = timestamp("updated_at")
}

/**
 * A file/link attached to a lesson plan (URL-based). Follows the
 * [HomeworkAttachmentsTable] pattern exactly.
 */
object LessonPlanAttachmentsTable : UUIDTable("lesson_plan_attachments", "id") {
    val lessonPlanId       = uuid("lesson_plan_id")            // FK lesson_plans.id (CASCADE)
    val url                = text("url")
    val filename           = text("filename").default("")
    val mime               = text("mime").default("")
    val sizeBytes          = long("size_bytes").default(0)
    val uploadedBy         = uuid("uploaded_by").nullable()    // FK app_users.id
    val createdAt          = timestamp("created_at")
}

// =====================================================================
// Student Health Records (HEALTH_RECORDS_SPEC.md — P1-12)
//
// Three tables back the student health surface:
//   1. StudentHealthProfilesTable   — per-student health profile (1:1)
//   2. StudentImmunizationsTable    — immunization/vaccine tracking
//   3. StudentHealthIncidentsTable  — health incident log
//
// Created/applied by docs/db/migration_050_health_records.sql. Registered
// in DatabaseFactory.allTables — AUTO_CREATE_TABLES is OFF in production,
// so that migration MUST be applied in Supabase before the matching deploy
// or validateSchema() refuses to boot.
// =====================================================================

/**
 * A student's health profile — one row per student (UNIQUE on student_id).
 * Stores blood group, physical measurements, allergies, chronic conditions,
 * medications (JSON-as-text), and emergency contact / doctor information.
 *
 * JSON columns follow the existing convention (see Tables.kt header):
 * `allergies` is a JSON array string e.g. ["Peanuts","Penicillin"];
 * `chronicConditions` is a JSON array of objects e.g.
 *   [{"condition":"Asthma","notes":"..."}];
 * `medications` is a JSON array of objects e.g.
 *   [{"name":"Inhaler","dose":"2 puffs","frequency":"as needed"}].
 */
object StudentHealthProfilesTable : UUIDTable("student_health_profiles", "id") {
    val schoolId              = uuid("school_id")
    val studentId             = uuid("student_id").uniqueIndex()   // FK students.id — 1:1
    val bloodGroup            = varchar("blood_group", 8).nullable()
    val heightCm              = double("height_cm").nullable()
    val weightKg              = double("weight_kg").nullable()
    val allergies             = text("allergies").default("[]")           // JSON array
    val chronicConditions     = text("chronic_conditions").default("[]")  // JSON array of objects
    val medications           = text("medications").default("[]")         // JSON array of objects
    val emergencyContactName  = text("emergency_contact_name").nullable()
    val emergencyContactPhone = varchar("emergency_contact_phone", 32).nullable()
    val doctorName            = text("doctor_name").nullable()
    val doctorPhone           = varchar("doctor_phone", 32).nullable()
    val updatedAt             = timestamp("updated_at")
    val createdAt             = timestamp("created_at")
}

/**
 * An immunization record for a student — one row per dose administered.
 * Tracks vaccine name, dose number, date administered, next due date, and
 * the administering doctor/clinic. Indexed on student_id for fast lookups.
 */
object StudentImmunizationsTable : UUIDTable("student_immunizations", "id") {
    val studentId         = uuid("student_id")               // FK students.id
    val vaccineName       = text("vaccine_name")             // "BCG", "DPT", "MMR", "COVID-19", …
    val doseNumber        = integer("dose_number").default(1)
    val dateAdministered  = date("date_administered")
    val nextDueDate       = date("next_due_date").nullable()
    val administeredBy    = text("administered_by").nullable()  // doctor/clinic name
    val createdAt         = timestamp("created_at")

    init {
        index("idx_immunizations_student", false, studentId)
    }
}

/**
 * A health incident log entry — what happened, treatment, medication given,
 * whether the parent was notified, and which staff member attended. Indexed
 * on (student_id, date DESC) for the incident history view.
 *
 * `severity` is a controlled vocabulary: minor | moderate | major.
 */
object StudentHealthIncidentsTable : UUIDTable("student_health_incidents", "id") {
    val schoolId          = uuid("school_id")
    val studentId         = uuid("student_id")               // FK students.id
    val date              = date("date")
    val time              = varchar("time", 8).nullable()    // "HH:mm"
    val description       = text("description")              // "Fell during recess, scraped knee"
    val treatment         = text("treatment").nullable()     // "Cleaned with antiseptic, bandage applied"
    val medicationGiven   = text("medication_given").nullable()
    val parentNotified    = bool("parent_notified").default(false)
    val parentNotifiedAt  = timestamp("parent_notified_at").nullable()
    val attendedBy        = uuid("attended_by").nullable()   // FK app_users.id (staff)
    val attendedByName    = text("attended_by_name").nullable()
    val severity          = varchar("severity", 16).default("minor")  // minor | moderate | major
    val createdAt         = timestamp("created_at")

    init {
        index("idx_health_incidents_student", false, studentId, date)
    }
}
