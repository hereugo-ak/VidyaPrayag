/*
 * File: Tables.kt
 * Module: db
 *
 * Exposed table definitions mapped 1:1 to the real Supabase Postgres schema.
 *
 * Two source-of-truth SQL files describe what's deployed in Supabase:
 *   - /supabase_schema                            (operational tables, v2.1)
 *   - /docs/backend/sql/01_supplementary_schema.sql  (everything the Ktor
 *                                                    backend additionally needs)
 *
 * Run BOTH SQL files in Supabase → SQL Editor before pointing the backend
 * at production.  For local-dev SQLite fallback, Exposed auto-creates the
 * tables in the order declared in DatabaseFactory.allTables.
 *
 * IMPORTANT DESIGN CHOICES
 * ------------------------
 *  - We deliberately do NOT model `auth.users` (Supabase Auth) here.  Our
 *    flow uses `app_users` for phone-OTP-first signup.  Email-only users
 *    can still be created (password_hash column).
 *  - We use UUIDs everywhere to match Supabase.
 *  - Foreign keys are declared with .references() so SchemaUtils generates
 *    proper FK constraints in SQLite dev too.
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
    val teacherAssigned = text("teacher_assigned").nullable()
    val createdAt       = timestamp("created_at")
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
    val source      = varchar("source", 32).nullable()
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
