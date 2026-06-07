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
 *   3. docs/db/migration_002_segmentation_geo_assignments.sql   (adds schools lat/long)
 *   4. docs/backend/sql/02_teacher_schema.sql
 *
 * Run all four in Supabase → SQL Editor before pointing the backend at
 * production. For local-dev SQLite fallback, Exposed auto-creates the tables
 * in the order declared in DatabaseFactory.allTables. In Postgres, boot-time
 * validation (DatabaseFactory.validateSchema) logs any of the 36 tables that
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
package com.littlebridge.vidyaprayag.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
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
    val classId   = uuid("class_id").nullable()          // FK school_classes.id (optional pre-migration)
    val className = text("class_name")                   // denormalised for fast reads / display
    val section   = varchar("section", 8).default("A")
    val subjectId = uuid("subject_id").nullable()        // FK school_subjects.id (optional)
    val subject   = text("subject")                      // denormalised subject name
    val teacherId = uuid("teacher_id").nullable()        // FK faculty.id / app_users.id
    val teacherName = text("teacher_name").nullable()    // display fallback when no FK
    val isActive  = bool("is_active").default(true)
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

object AttendanceRecordsTable : UUIDTable("attendance_records", "id") {
    val schoolId   = uuid("school_id")
    val date       = varchar("date", 12)
    val type       = varchar("type", 16) // student | faculty
    val personId   = text("person_id")
    val grade      = text("grade").nullable()
    val status     = varchar("status", 16)
    val markedBy   = uuid("marked_by").nullable()
    val createdAt  = timestamp("created_at")
    init {
        uniqueIndex("ux_att_records_unique", schoolId, date, type, personId)
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
    val profilePhotoUrl = text("profile_photo_url").nullable()
    val isActive   = bool("is_active").default(true)
    val createdAt  = timestamp("created_at")
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
    val currency    = varchar("currency", 8).default("USD")
    val dueDate     = varchar("due_date", 12).nullable()                // YYYY-MM-DD
    val status      = varchar("status", 16).default("DUE")              // PAID | DUE | OVERDUE
    val category    = varchar("category", 32).default("Tuition")        // Tuition | Transport | ...
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
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
    val ownerUserId    = uuid("owner_user_id")                          // recipient (admin) — JWT.sub
    val senderName     = text("sender_name")
    val senderRole     = text("sender_role")
    val senderImageUrl = text("sender_image_url").nullable()
    val iconName       = text("icon_name").nullable()                   // when no avatar (e.g. "payments")
    val lastMessage    = text("last_message").default("")
    val lastMessageAt  = timestamp("last_message_at")
    val unreadCount    = integer("unread_count").default(0)
    val isRead         = bool("is_read").default(true)
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}

object MessagesTable : UUIDTable("messages", "id") {
    val threadId  = uuid("thread_id")                                   // FK message_threads.id
    val senderId  = uuid("sender_id").nullable()                        // null for system messages
    val body      = text("body")
    val createdAt = timestamp("created_at")
}

/**
 * Per-student exam results upserted via Results screen.
 * The (school, test, class, subject, student_id) tuple is unique so
 * publishing the same test twice updates rather than duplicates.
 *
 * Spec ref: school_api_spec.artifact.md §Module: Results
 */
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
    val examDate  = varchar("exam_date", 12).nullable()  // YYYY-MM-DD
    val isActive  = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

/**
 * Per-student score for an [AssessmentsTable] row. `marks` is a Double so a
 * teacher can enter half-marks; the route clamps it to [0, assessment.maxMarks].
 * One (assessment, student) pair is unique so re-submitting updates in place.
 */
object AssessmentMarksTable : UUIDTable("assessment_marks", "id") {
    val assessmentId = uuid("assessment_id")             // FK assessments.id
    val studentId    = text("student_id")                // students.student_code
    val studentName  = text("student_name")
    val marks        = double("marks").nullable()        // null = not yet entered
    val enteredBy    = uuid("entered_by").nullable()     // FK app_users.id (teacher)
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
    init {
        uniqueIndex("ux_assessment_marks_unique", assessmentId, studentId)
    }
}

/**
 * Syllabus unit (chapter/topic) for a class+section+subject, with a covered
 * flag + the date it was marked covered. Backs TeacherSyllabusData / the
 * PATCH /teacher/syllabus toggle. `position` orders units within a subject.
 */
object SyllabusUnitsTable : UUIDTable("syllabus_units", "id") {
    val schoolId   = uuid("school_id")
    val className  = text("class_name")
    val section    = varchar("section", 8).default("A")
    val subject    = text("subject")
    val title      = text("title")
    val position   = integer("position").default(0)
    val isCovered  = bool("is_covered").default(false)
    val coveredOn  = varchar("covered_on", 12).nullable()  // YYYY-MM-DD
    val coveredBy  = uuid("covered_by").nullable()          // FK app_users.id (teacher)
    val createdAt  = timestamp("created_at")
    val updatedAt  = timestamp("updated_at")
}

/**
 * A homework/assignment authored by a teacher for one class+section+subject.
 * `submittedCount` is derived live from [HomeworkSubmissionsTable] at read
 * time; `totalCount` is the headcount of the target class (computed from
 * `students`). Backs TeacherHomeworkData / CreateHomeworkRequest.
 */
object HomeworkTable : UUIDTable("homework", "id") {
    val schoolId    = uuid("school_id")
    val teacherId   = uuid("teacher_id").nullable()     // FK app_users.id (author)
    val className   = text("class_name")
    val section     = varchar("section", 8).default("A")
    val subject     = text("subject")
    val title       = text("title")
    val description = text("description").default("")
    val dueDate     = varchar("due_date", 12)           // YYYY-MM-DD
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

/**
 * One student's submission against a [HomeworkTable] row. Used to compute the
 * submitted/total ratio shown on the teacher's homework cards. Unique on
 * (homework, student).
 */
object HomeworkSubmissionsTable : UUIDTable("homework_submissions", "id") {
    val homeworkId  = uuid("homework_id")               // FK homework.id
    val studentId   = text("student_id")                // students.student_code
    val status      = varchar("status", 16).default("submitted") // submitted | graded | late
    val submittedAt = timestamp("submitted_at")
    init {
        uniqueIndex("ux_homework_submissions_unique", homeworkId, studentId)
    }
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
    val weekday    = integer("weekday")                 // 1=Mon … 7=Sun
    val startTime  = varchar("start_time", 8)           // "HH:mm"
    val endTime    = varchar("end_time", 8)             // "HH:mm"
    val className  = text("class_name")
    val section    = varchar("section", 8).default("A")
    val subject    = text("subject")
    val room       = text("room").default("")
    val position   = integer("position").default(0)
    val createdAt  = timestamp("created_at")
}
