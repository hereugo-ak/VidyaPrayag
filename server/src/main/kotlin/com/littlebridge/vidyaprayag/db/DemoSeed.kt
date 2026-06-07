/*
 * File: DemoSeed.kt
 * Module: db
 *
 * Operational demo seeder (audit finding B, §2).
 *
 * The previous CmsSeed only inserted landing copy + app_config flags — ZERO
 * operational rows — so a brand-new database had no user with a working
 * password/OTP, every data screen was empty/errored, and several portals were
 * unreachable. This seeder fixes that by inserting ONE working credential per
 * profile type plus the minimum operational data the portals read:
 *
 *   - super_admin    super@vidyaprayag.demo   / Demo@1234
 *   - school_admin   admin@vidyaprayag.demo   / Demo@1234   (owns the demo school)
 *   - teacher        teacher@vidyaprayag.demo / Demo@1234   (in the demo school)
 *   - parent         parent@vidyaprayag.demo  / Demo@1234   (+ linked child)
 *   - a demo school (with lat/long for distance discovery)
 *   - one demo child linked to the parent
 *   - demo fee_records for the parent (so the Fees tab has real data)
 *   - one demo announcement scoped to the demo school
 *
 * Idempotency: every insert is guarded by a presence check (email / slug /
 * deterministic id) so repeated cold boots never duplicate rows and never
 * overwrite operator edits.
 *
 * Gate: APP_SEED_DEMO env var (default "true"). Set APP_SEED_DEMO=false in a
 * real production tenant that should not contain demo accounts.
 *
 * Passwords are written with the salted PasswordHasher (same KDF as signup).
 */
package com.littlebridge.vidyaprayag.db

import com.littlebridge.vidyaprayag.feature.auth.PasswordHasher
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

object DemoSeed {

    private const val DEMO_PASSWORD = "Demo@1234"

    // Deterministic ids so links between demo rows are stable across restarts.
    private val SCHOOL_ID  = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val SUPER_ID   = UUID.fromString("00000000-0000-0000-0000-0000000000b1")
    private val ADMIN_ID   = UUID.fromString("00000000-0000-0000-0000-0000000000b2")
    private val TEACHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b3")
    private val PARENT_ID  = UUID.fromString("00000000-0000-0000-0000-0000000000b4")
    private val CHILD_ID   = UUID.fromString("00000000-0000-0000-0000-0000000000c1")

    fun ensureDemoData() {
        transaction {
            seedSchool()
            seedUser(SUPER_ID, "Demo Super Admin", "super@vidyaprayag.demo", "super_admin", schoolId = null)
            seedUser(ADMIN_ID, "Demo School Admin", "admin@vidyaprayag.demo", "school_admin", schoolId = SCHOOL_ID)
            seedUser(TEACHER_ID, "Demo Teacher", "teacher@vidyaprayag.demo", "teacher", schoolId = SCHOOL_ID)
            seedUser(PARENT_ID, "Demo Parent", "parent@vidyaprayag.demo", "parent", schoolId = null)
            seedChild()
            seedFeeRecords()
            seedAnnouncement()
        }
    }

    private fun seedSchool() {
        val exists = SchoolsTable.selectAll()
            .where { SchoolsTable.id eq SCHOOL_ID }
            .any()
        if (exists) return
        val now = Instant.now()
        SchoolsTable.insert {
            it[id] = SCHOOL_ID
            it[name] = "VidyaPrayag Demo Public School"
            it[slug] = "vidyaprayag-demo-public-school"
            it[board] = "CBSE"
            it[medium] = "English"
            it[schoolGender] = "co_ed"
            it[contactPhone] = "+911234567890"
            it[contactEmail] = "admin@vidyaprayag.demo"
            it[principalName] = "Demo Principal"
            it[fullAddress] = "1 Demo Road, Lucknow"
            it[city] = "Lucknow"
            it[district] = "Lucknow"
            it[state] = "Uttar Pradesh"
            it[pincode] = "226001"
            it[latitude] = 26.8467
            it[longitude] = 80.9462
            it[isActive] = true
            it[onboardedAt] = now
            it[createdAt] = now
            it[updatedAt] = now
        }
    }

    private fun seedUser(uid: UUID, name: String, emailAddr: String, userRole: String, schoolId: UUID?) {
        val exists = AppUsersTable.selectAll()
            .where { (AppUsersTable.id eq uid) or (AppUsersTable.email eq emailAddr) }
            .any()
        if (exists) return
        val now = Instant.now()
        AppUsersTable.insert {
            it[id] = uid
            it[fullName] = name
            it[email] = emailAddr
            it[passwordHash] = PasswordHasher.hash(DEMO_PASSWORD)
            it[role] = userRole
            if (schoolId != null) it[AppUsersTable.schoolId] = schoolId
            it[isEmailVerified] = true
            it[profileCompleted] = true
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
    }

    private fun seedChild() {
        val exists = ChildrenTable.selectAll()
            .where { ChildrenTable.id eq CHILD_ID }
            .any()
        if (exists) return
        val now = Instant.now()
        ChildrenTable.insert {
            it[id] = CHILD_ID
            it[parentId] = PARENT_ID
            it[schoolId] = SCHOOL_ID
            it[studentCode] = "DEMO-S001"
            it[childName] = "Demo Child"
            it[dateOfBirth] = "2015-06-01"
            it[gender] = "MALE"
            it[currentGrade] = "Grade 4"
            it[overallProgress] = 0.72
            it[currentLevel] = 4
            it[attendanceStatus] = "PRESENT"
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
    }

    private fun seedFeeRecords() {
        val exists = FeeRecordsTable.selectAll()
            .where { FeeRecordsTable.parentId eq PARENT_ID }
            .any()
        if (exists) return
        val now = Instant.now()
        data class Fee(val title: String, val amount: Double, val status: String, val due: String, val category: String)
        val fees = listOf(
            Fee("Term 1 Tuition", 25000.0, "PAID", "2026-04-10", "Tuition"),
            Fee("Term 2 Tuition", 25000.0, "DUE", "2026-09-10", "Tuition"),
            Fee("Transport Q2", 6000.0, "OVERDUE", "2026-05-15", "Transport")
        )
        fees.forEach { fee ->
            FeeRecordsTable.insert {
                it[id] = UUID.randomUUID()
                it[parentId] = PARENT_ID
                it[childId] = CHILD_ID
                it[schoolId] = SCHOOL_ID
                it[title] = fee.title
                it[amount] = fee.amount
                it[currency] = "INR"
                it[dueDate] = fee.due
                it[status] = fee.status
                it[category] = fee.category
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    private fun seedAnnouncement() {
        val eventIdValue = "demo-announcement-001"
        val exists = AnnouncementsTable.selectAll()
            .where { AnnouncementsTable.eventId eq eventIdValue }
            .any()
        if (exists) return
        val now = Instant.now()
        AnnouncementsTable.insert {
            it[id] = UUID.randomUUID()
            it[schoolId] = SCHOOL_ID
            it[eventId] = eventIdValue
            it[type] = "Events"
            it[title] = "Welcome to VidyaPrayag"
            it[subTitle] = "Demo announcement"
            it[description] = "This is a seeded demo announcement so the Activity tab has real data on first boot."
            it[date] = "2026-06-07"
            it[audienceType] = "ALL_SCHOOL"
            it[authorRole] = "school_admin"
            it[syncedToWa] = false
            it[createdBy] = ADMIN_ID
            it[createdAt] = now
            it[updatedAt] = now
        }
    }
}
