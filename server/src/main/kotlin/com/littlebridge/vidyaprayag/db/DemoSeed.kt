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
 * T-005 (X-5 fix) — realistic teacher dataset so the rebuilt teacher portal
 * surfaces land with real rows instead of empty/error states:
 *   - demo classes (school_classes) + per-class subjects (school_subjects) so
 *     teacher_subject_assignments can carry the typed class_id/subject_id FKs
 *   - teacher_subject_assignments for the demo teacher (incl. 1 class-teacher)
 *   - a full class roster (students + enrollments) for the class the demo
 *     teacher is class-teacher of (so Classes shows a real roster)
 *   - teacher_periods for a full Mon–Fri week (so Today shows periods)
 *   - syllabus_units per subject (so the Planner shows units)
 *   - a couple of assessments (so Results has data)
 *   - sample homework + submissions (so Homework shows a submitted/total ratio)
 *   - a few approved leaves (so leave defaults have something to show)
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
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
    private val STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2")

    // RA-61: the canonical school-side student row the parent's child links to.
    // attendance_records / assessment_marks / analytics all key off
    // students.student_code, so the demo child needs a matching students row or
    // every academic read for the demo child resolves to nothing.
    private const val DEMO_STUDENT_CODE = "DEMO-S001"
    private const val DEMO_CLASS = "Grade 4"
    private const val DEMO_SECTION = "A"

    // Deterministic anchor ids so the scholarship seed is idempotent across boots.
    private val SCHOLARSHIP_ANCHOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1")
    private val APPLICATION_ANCHOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1")

    // ── T-005: deterministic anchors for the teacher operational dataset ──────
    // The demo teacher (TEACHER_ID) is class-teacher of DEMO_CLASS/DEMO_SECTION
    // ("Grade 4" / "A") and additionally teaches two more class+subject combos so
    // Today/Classes/Planner/Results/Homework all have non-trivial rows.
    private val CLASS_G4_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1") // Grade 4
    private val CLASS_G5_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f2") // Grade 5

    // Per-class subject anchors (school_subjects.id) the TSA FKs point at.
    private val SUBJ_G4_MATH_ID    = UUID.fromString("00000000-0000-0000-0000-000000000201")
    private val SUBJ_G4_SCIENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000202")
    private val SUBJ_G4_ENGLISH_ID = UUID.fromString("00000000-0000-0000-0000-000000000203")
    private val SUBJ_G5_MATH_ID    = UUID.fromString("00000000-0000-0000-0000-000000000204")

    // TSA assignment anchors (teacher_subject_assignments.id).
    private val TSA_G4_MATH_ID    = UUID.fromString("00000000-0000-0000-0000-000000000301")
    private val TSA_G4_SCIENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000302")
    private val TSA_G5_MATH_ID    = UUID.fromString("00000000-0000-0000-0000-000000000303")

    // Assessment anchors (assessments.id).
    private val ASSESS_G4_MATH_ID    = UUID.fromString("00000000-0000-0000-0000-000000000401")
    private val ASSESS_G4_SCIENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000402")

    // Homework anchors (homework.id).
    private val HW_G4_MATH_ID    = UUID.fromString("00000000-0000-0000-0000-000000000501")
    private val HW_G4_SCIENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000502")

    // Demo teacher display name (matches seedUser above) — used for the legacy
    // teacherName denormalised column on TSA rows.
    private const val DEMO_TEACHER_NAME = "Demo Teacher"

    // Roster size for the class the demo teacher is class-teacher of. The first
    // student is the existing demo child (DEMO-S001); the rest are generated.
    private const val DEMO_ROSTER_SIZE = 38

    fun ensureDemoData() {
        transaction {
            seedSchool()
            seedUser(SUPER_ID, "Demo Super Admin", "super@vidyaprayag.demo", "super_admin", schoolId = null)
            seedUser(ADMIN_ID, "Demo School Admin", "admin@vidyaprayag.demo", "school_admin", schoolId = SCHOOL_ID)
            seedUser(TEACHER_ID, "Demo Teacher", "teacher@vidyaprayag.demo", "teacher", schoolId = SCHOOL_ID)
            seedUser(PARENT_ID, "Demo Parent", "parent@vidyaprayag.demo", "parent", schoolId = null)
            seedStudent()   // RA-61: school-side student row the child links to
            seedChild()
            seedFeeRecords()
            seedAnnouncement()
            seedScholarships()
            // ── T-005: realistic teacher operational dataset (X-5 fix) ──────
            // Ordering is FK-dependency-driven: classes+subjects first (TSA needs
            // their ids), then assignments, then the roster the assignments scope
            // to, then the day/week/planner/results/homework/leave data on top.
            seedClasses()
            seedSubjects()
            seedTeacherAssignments()
            seedRoster()
            seedTeacherPeriods()
            seedSyllabus()
            seedAssessments()
            seedHomework()
            seedLeaves()
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

    /**
     * RA-61: the school's canonical students row for the demo child. Without
     * this, the parent's child (student_code = DEMO-S001) had no counterpart in
     * `students`, so the admin daily-attendance roster, per-student analytics,
     * and any read that joins on students.student_code found nothing for the
     * demo child. school_id matches the child + the demo school so the row is
     * tenant-consistent (closes the school_id=null/SCHOOL_ID mismatch note).
     */
    private fun seedStudent() {
        val exists = StudentsTable.selectAll()
            .where { (StudentsTable.id eq STUDENT_ID) or (StudentsTable.studentCode eq DEMO_STUDENT_CODE) }
            .any()
        if (exists) return
        val now = Instant.now()
        StudentsTable.insert {
            it[id] = STUDENT_ID
            it[schoolId] = SCHOOL_ID
            it[studentCode] = DEMO_STUDENT_CODE
            it[fullName] = "Demo Child"
            it[className] = DEMO_CLASS
            it[section] = DEMO_SECTION
            it[rollNumber] = "1"
            it[isActive] = true
            it[createdAt] = now
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
            it[studentCode] = DEMO_STUDENT_CODE   // RA-61: matches the students row
            it[childName] = "Demo Child"
            it[dateOfBirth] = "2015-06-01"
            it[gender] = "MALE"
            it[currentGrade] = DEMO_CLASS
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

    /**
     * Seeds a small set of operator-curated scholarship opportunities plus a few
     * applications for the demo parent, so the parent Scholarships screen shows
     * real DB-backed rows on first boot (audit §4.2/§5.2). The hardcoded
     * "$45,000 STEM Award" fiction used to live in the /scholarships handler;
     * now it lives here as honest, editable seed data.
     *
     * Idempotency: the opportunity seed is guarded on a deterministic anchor id
     * (first row) and the application seed on a deterministic anchor id, so
     * repeated cold boots never duplicate rows.
     */
    private fun seedScholarships() {
        val now = Instant.now()

        val scholarshipsSeeded = ScholarshipsTable.selectAll()
            .where { ScholarshipsTable.id eq SCHOLARSHIP_ANCHOR_ID }
            .any()
        if (!scholarshipsSeeded) {
            data class Opportunity(
                val anchorId: UUID?,
                val title: String,
                val description: String,
                val amount: String,
                val timeLeft: String,
                val category: String,
                val critical: Boolean,
                val position: Int
            )
            val opportunities = listOf(
                Opportunity(
                    SCHOLARSHIP_ANCHOR_ID,
                    "Global Excellence STEM Award 2026",
                    "For students with a strong focus on Engineering or Mathematics.",
                    "₹45,000", "3d : 12h", "Full Funding", true, 0
                ),
                Opportunity(
                    null,
                    "Social Impact Grant",
                    "Supports students leading community initiatives.",
                    "₹5,000", "24h left", "Merit Based", true, 1
                ),
                Opportunity(
                    null,
                    "Bridge-to-Learning Fund",
                    "First-generation college student aid.",
                    "₹12,000", "14 days", "Need Based", false, 2
                )
            )
            opportunities.forEach { opp ->
                ScholarshipsTable.insert {
                    it[id] = opp.anchorId ?: UUID.randomUUID()
                    it[title] = opp.title
                    it[description] = opp.description
                    it[amount] = opp.amount
                    it[timeLeft] = opp.timeLeft
                    it[category] = opp.category
                    it[isCritical] = opp.critical
                    it[position] = opp.position
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }

        val applicationsSeeded = ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq APPLICATION_ANCHOR_ID }
            .any()
        if (!applicationsSeeded) {
            data class Application(
                val anchorId: UUID?,
                val institution: String,
                val program: String,
                val status: String,
                val icon: String,
                val position: Int
            )
            val applications = listOf(
                Application(
                    APPLICATION_ANCHOR_ID,
                    "University of Applied Sciences",
                    "B.Arch - Sustainable Urbanism",
                    "Shortlisted", "architecture", 0
                ),
                Application(
                    null,
                    "Tech Institute of Innovation",
                    "M.Sc - AI",
                    "Under Review", "biotech", 1
                ),
                Application(
                    null,
                    "Royal Academy of Arts",
                    "BFA - Digital Media Design",
                    "Received", "history_edu", 2
                )
            )
            applications.forEach { app ->
                ScholarshipApplicationsTable.insert {
                    it[id] = app.anchorId ?: UUID.randomUUID()
                    it[parentId] = PARENT_ID
                    it[institution] = app.institution
                    it[program] = app.program
                    it[status] = app.status
                    it[iconName] = app.icon
                    it[position] = app.position
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }

    // =====================================================================
    // T-005 — realistic teacher operational dataset (X-5 fix)
    //
    // Every function below follows the same idempotency contract as the rest of
    // this file: guard on a deterministic anchor id (or a natural key) and
    // return early if the row(s) already exist, so repeated cold boots never
    // duplicate and never clobber operator edits.
    // =====================================================================

    /**
     * Two demo classes. `school_classes.code` is the natural class key; `name`
     * is the display label that the denormalised className columns across the
     * schema (TSA, enrollments-via-section, periods, syllabus, …) mirror.
     */
    private fun seedClasses() {
        val now = Instant.now()
        data class Klass(val cid: UUID, val code: String, val name: String)
        val classes = listOf(
            Klass(CLASS_G4_ID, "G4", DEMO_CLASS),   // "Grade 4"
            Klass(CLASS_G5_ID, "G5", "Grade 5")
        )
        classes.forEach { k ->
            val exists = SchoolClassesTable.selectAll()
                .where { SchoolClassesTable.id eq k.cid }
                .any()
            if (exists) return@forEach
            SchoolClassesTable.insert {
                it[id] = k.cid
                it[schoolId] = SCHOOL_ID
                it[code] = k.code
                it[name] = k.name
                it[sections] = "[\"A\",\"B\"]"
                it[createdAt] = now
            }
        }
    }

    /**
     * Per-class subjects. `school_subjects.class_id` binds each subject to its
     * class; the structured `teacher_subject_assignments` rows then reference
     * these by `subject_id`.
     */
    private fun seedSubjects() {
        val now = Instant.now()
        data class Subj(val sid: UUID, val classId: UUID, val name: String, val code: String)
        val subjects = listOf(
            Subj(SUBJ_G4_MATH_ID,    CLASS_G4_ID, "Mathematics", "G4-MATH"),
            Subj(SUBJ_G4_SCIENCE_ID, CLASS_G4_ID, "Science",     "G4-SCI"),
            Subj(SUBJ_G4_ENGLISH_ID, CLASS_G4_ID, "English",     "G4-ENG"),
            Subj(SUBJ_G5_MATH_ID,    CLASS_G5_ID, "Mathematics", "G5-MATH")
        )
        subjects.forEach { s ->
            val exists = SchoolSubjectsTable.selectAll()
                .where { SchoolSubjectsTable.id eq s.sid }
                .any()
            if (exists) return@forEach
            SchoolSubjectsTable.insert {
                it[id] = s.sid
                it[classId] = s.classId
                it[subName] = s.name
                it[subCode] = s.code
                it[teacherAssigned] = DEMO_TEACHER_NAME
                it[createdAt] = now
            }
        }
    }

    /**
     * teacher_subject_assignments for the demo teacher. The demo teacher:
     *   • is CLASS TEACHER of Grade 4 / A and teaches Mathematics there
     *   • also teaches Science to Grade 4 / A
     *   • teaches Mathematics to Grade 5 / A (not class teacher)
     * class_id / subject_id carry the typed FKs (T-002) — className/section/
     * subject are display-only denormalised mirrors.
     */
    private fun seedTeacherAssignments() {
        val now = Instant.now()
        data class Tsa(
            val aid: UUID,
            val classId: UUID,
            val className: String,
            val subjectId: UUID,
            val subject: String,
            val isClassTeacher: Boolean
        )
        val rows = listOf(
            Tsa(TSA_G4_MATH_ID,    CLASS_G4_ID, DEMO_CLASS, SUBJ_G4_MATH_ID,    "Mathematics", true),
            Tsa(TSA_G4_SCIENCE_ID, CLASS_G4_ID, DEMO_CLASS, SUBJ_G4_SCIENCE_ID, "Science",     false),
            Tsa(TSA_G5_MATH_ID,    CLASS_G5_ID, "Grade 5",  SUBJ_G5_MATH_ID,    "Mathematics", false)
        )
        rows.forEach { r ->
            val exists = TeacherSubjectAssignmentsTable.selectAll()
                .where { TeacherSubjectAssignmentsTable.id eq r.aid }
                .any()
            if (exists) return@forEach
            TeacherSubjectAssignmentsTable.insert {
                it[id] = r.aid
                it[schoolId] = SCHOOL_ID
                it[classId] = r.classId
                it[className] = r.className
                it[section] = DEMO_SECTION
                it[subjectId] = r.subjectId
                it[subject] = r.subject
                it[teacherId] = TEACHER_ID
                it[teacherName] = DEMO_TEACHER_NAME
                it[isActive] = true
                it[isClassTeacher] = r.isClassTeacher
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    /**
     * A full roster for Grade 4 / A — the class the demo teacher is class-teacher
     * of — so Classes shows a real roster and attendance/marks have students.
     *
     * The first roster member is the EXISTING demo child (STUDENT_ID /
     * DEMO-S001), so the parent's child appears in the class. The remaining
     * (DEMO_ROSTER_SIZE - 1) are generated with deterministic ids/codes so the
     * seed is idempotent. Each student gets a matching `enrollments` row
     * (typed class_id FK, T-001) for the active term.
     */
    private fun seedRoster() {
        val now = Instant.now()
        val enrollStart = LocalDate.of(2026, 4, 1)   // start of the demo academic term

        // Student #1 = the demo child (already in `students` via seedStudent()).
        // Ensure its enrollment exists, then generate the rest.
        ensureEnrollment(STUDENT_ID, rollNumber = 1, startDate = enrollStart, now = now)

        for (n in 2..DEMO_ROSTER_SIZE) {
            // Deterministic per-student id: 00000000-0000-0000-0000-0000000010NN
            val sid = UUID.fromString("00000000-0000-0000-0000-0000000010%02d".format(n))
            val code = "DEMO-S%03d".format(n)
            val studentExists = StudentsTable.selectAll()
                .where { (StudentsTable.id eq sid) or (StudentsTable.studentCode eq code) }
                .any()
            if (!studentExists) {
                StudentsTable.insert {
                    it[id] = sid
                    it[schoolId] = SCHOOL_ID
                    it[studentCode] = code
                    it[fullName] = "Demo Student %02d".format(n)
                    it[className] = DEMO_CLASS
                    it[section] = DEMO_SECTION
                    it[rollNumber] = n.toString()
                    it[isActive] = true
                    it[createdAt] = now
                }
            }
            ensureEnrollment(sid, rollNumber = n, startDate = enrollStart, now = now)
        }
    }

    /** Idempotent enrollment insert for the demo Grade 4 / A roster. */
    private fun ensureEnrollment(studentId: UUID, rollNumber: Int, startDate: LocalDate, now: Instant) {
        val exists = EnrollmentsTable.selectAll()
            .where {
                (EnrollmentsTable.studentId eq studentId) and
                    (EnrollmentsTable.classId eq CLASS_G4_ID) and
                    (EnrollmentsTable.section eq DEMO_SECTION) and
                    (EnrollmentsTable.startDate eq startDate)
            }
            .any()
        if (exists) return
        EnrollmentsTable.insert {
            it[schoolId] = SCHOOL_ID
            it[EnrollmentsTable.studentId] = studentId
            it[classId] = CLASS_G4_ID
            it[section] = DEMO_SECTION
            it[EnrollmentsTable.rollNumber] = rollNumber
            it[status] = "active"
            it[EnrollmentsTable.startDate] = startDate
            it[createdAt] = now
        }
    }

    /**
     * teacher_periods for a full Mon–Fri week so the Today strip is non-empty on
     * any weekday. Periods map onto the demo teacher's actual assignments
     * (Grade 4 Math/Science, Grade 5 Math). weekday is 1..7 (Mon..Sun) per
     * java.time.DayOfWeek.value. Idempotency: guarded on (teacher, weekday,
     * startTime).
     */
    private fun seedTeacherPeriods() {
        val now = Instant.now()
        data class Period(
            val weekday: Int,
            val start: String,
            val end: String,
            val className: String,
            val subject: String,
            val room: String,
            val position: Int
        )
        // A compact but realistic Mon–Fri timetable for the demo teacher.
        val periods = mutableListOf<Period>()
        for (wd in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value) {
            periods += Period(wd, "09:00", "09:45", DEMO_CLASS, "Mathematics", "R-101", 0)
            periods += Period(wd, "10:00", "10:45", DEMO_CLASS, "Science",     "R-101", 1)
            periods += Period(wd, "11:30", "12:15", "Grade 5",  "Mathematics", "R-205", 2)
        }
        periods.forEach { p ->
            val exists = TeacherPeriodsTable.selectAll()
                .where {
                    (TeacherPeriodsTable.teacherId eq TEACHER_ID) and
                        (TeacherPeriodsTable.weekday eq p.weekday) and
                        (TeacherPeriodsTable.startTime eq p.start)
                }
                .any()
            if (exists) return@forEach
            TeacherPeriodsTable.insert {
                it[schoolId] = SCHOOL_ID
                it[teacherId] = TEACHER_ID
                it[weekday] = p.weekday
                it[startTime] = p.start
                it[endTime] = p.end
                it[className] = p.className
                it[section] = DEMO_SECTION
                it[subject] = p.subject
                it[room] = p.room
                it[position] = p.position
                it[createdAt] = now
            }
        }
    }

    /**
     * syllabus_units (curriculum units) per subject the demo teacher owns, so the
     * Planner lands with content. A handful are pre-marked covered (with a
     * covered_on date in the past) to make progress non-trivial. Idempotency:
     * guarded on (school, className, section, subject, title).
     */
    private fun seedSyllabus() {
        val now = Instant.now()
        val coveredDate = LocalDate.of(2026, 5, 15)
        data class SylUnit(
            val className: String,
            val subject: String,
            val title: String,
            val position: Int,
            val covered: Boolean
        )
        val units = listOf(
            SylUnit(DEMO_CLASS, "Mathematics", "Numbers up to 10,000",        0, true),
            SylUnit(DEMO_CLASS, "Mathematics", "Addition & Subtraction",      1, true),
            SylUnit(DEMO_CLASS, "Mathematics", "Multiplication",              2, false),
            SylUnit(DEMO_CLASS, "Mathematics", "Division",                    3, false),
            SylUnit(DEMO_CLASS, "Science",     "Living & Non-living Things",  0, true),
            SylUnit(DEMO_CLASS, "Science",     "Plants Around Us",            1, false),
            SylUnit(DEMO_CLASS, "Science",     "The Human Body",              2, false),
            SylUnit("Grade 5",  "Mathematics", "Fractions",                   0, true),
            SylUnit("Grade 5",  "Mathematics", "Decimals",                    1, false)
        )
        units.forEach { u ->
            val exists = SyllabusUnitsTable.selectAll()
                .where {
                    (SyllabusUnitsTable.schoolId eq SCHOOL_ID) and
                        (SyllabusUnitsTable.className eq u.className) and
                        (SyllabusUnitsTable.section eq DEMO_SECTION) and
                        (SyllabusUnitsTable.subject eq u.subject) and
                        (SyllabusUnitsTable.title eq u.title)
                }
                .any()
            if (exists) return@forEach
            SyllabusUnitsTable.insert {
                it[schoolId] = SCHOOL_ID
                it[className] = u.className
                it[section] = DEMO_SECTION
                it[subject] = u.subject
                it[title] = u.title
                it[position] = u.position
                it[isCovered] = u.covered
                if (u.covered) {
                    it[coveredOn] = coveredDate
                    it[coveredBy] = TEACHER_ID
                }
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    /**
     * A couple of assessments (Grade 4 Math + Science) with marks for the full
     * roster, so the Results screen has real published data. Marks are
     * deterministic (derived from roll number) so cold boots are stable.
     * Idempotency: guarded on the assessment anchor id; marks guarded on
     * (assessment, student_code).
     */
    private fun seedAssessments() {
        val now = Instant.now()
        val examDate = LocalDate.of(2026, 5, 20)
        data class Exam(val aid: UUID, val subject: String, val name: String, val maxMarks: Int)
        val exams = listOf(
            Exam(ASSESS_G4_MATH_ID,    "Mathematics", "Unit Test I", 50),
            Exam(ASSESS_G4_SCIENCE_ID, "Science",     "Unit Test I", 50)
        )
        // Roster (student_code, name, roll) the marks attach to — student #1 is
        // the demo child, the rest are the generated roster.
        val roster = buildRosterRefs()
        exams.forEach { exam ->
            val assessmentExists = AssessmentsTable.selectAll()
                .where { AssessmentsTable.id eq exam.aid }
                .any()
            if (!assessmentExists) {
                AssessmentsTable.insert {
                    it[id] = exam.aid
                    it[schoolId] = SCHOOL_ID
                    it[teacherId] = TEACHER_ID
                    it[className] = DEMO_CLASS
                    it[section] = DEMO_SECTION
                    it[subject] = exam.subject
                    it[name] = exam.name
                    it[maxMarks] = exam.maxMarks
                    it[examDate] = examDate
                    it[isActive] = true
                    it[isPublished] = true
                    it[publishedAt] = now
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            roster.forEach { ref ->
                val markExists = AssessmentMarksTable.selectAll()
                    .where {
                        (AssessmentMarksTable.assessmentId eq exam.aid) and
                            (AssessmentMarksTable.studentId eq ref.code)
                    }
                    .any()
                if (markExists) return@forEach
                // Deterministic, plausible score in [60%, 95%] of maxMarks.
                val pct = 60 + (ref.roll * 7) % 36
                val score = (exam.maxMarks * pct / 100.0)
                AssessmentMarksTable.insert {
                    it[id] = UUID.randomUUID()
                    it[assessmentId] = exam.aid
                    it[studentId] = ref.code
                    it[studentName] = ref.name
                    it[marks] = score
                    it[enteredBy] = TEACHER_ID
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }

    /**
     * Sample homework for Grade 4 Math + Science with partial submissions, so the
     * Homework cards show a real submitted/total ratio. About 60% of the roster
     * has submitted (deterministic by roll). Idempotency: guarded on the
     * homework anchor id; submissions guarded on (homework, student_code).
     */
    private fun seedHomework() {
        val now = Instant.now()
        val dueDate = LocalDate.now().plusDays(2)   // due soon so it shows as active
        data class Hw(val hid: UUID, val subject: String, val title: String, val desc: String)
        val homeworks = listOf(
            Hw(HW_G4_MATH_ID,    "Mathematics", "Multiplication Worksheet", "Complete exercises 1–20 on page 34."),
            Hw(HW_G4_SCIENCE_ID, "Science",     "Plant Observation",        "Observe a plant at home and note 5 features.")
        )
        val roster = buildRosterRefs()
        homeworks.forEach { hw ->
            val hwExists = HomeworkTable.selectAll()
                .where { HomeworkTable.id eq hw.hid }
                .any()
            if (!hwExists) {
                HomeworkTable.insert {
                    it[id] = hw.hid
                    it[schoolId] = SCHOOL_ID
                    it[teacherId] = TEACHER_ID
                    it[className] = DEMO_CLASS
                    it[section] = DEMO_SECTION
                    it[subject] = hw.subject
                    it[title] = hw.title
                    it[description] = hw.desc
                    it[dueDate] = dueDate
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            roster.forEach { ref ->
                // ~60% submitted, deterministic by roll.
                if (ref.roll % 5 >= 3) return@forEach
                val subExists = HomeworkSubmissionsTable.selectAll()
                    .where {
                        (HomeworkSubmissionsTable.homeworkId eq hw.hid) and
                            (HomeworkSubmissionsTable.studentId eq ref.code)
                    }
                    .any()
                if (subExists) return@forEach
                HomeworkSubmissionsTable.insert {
                    it[id] = UUID.randomUUID()
                    it[homeworkId] = hw.hid
                    it[studentId] = ref.code
                    it[status] = "submitted"
                    it[submittedAt] = now
                }
            }
        }
    }

    /**
     * A few approved leaves for students in the demo class, so leave-default
     * logic (later phases) has something to surface and the teacher leave inbox
     * is non-empty. Idempotency: guarded on (school, requesterName, dateFrom).
     */
    private fun seedLeaves() {
        val now = Instant.now()
        data class Leave(
            val requesterName: String,
            val childId: UUID,
            val from: String,
            val to: String,
            val reason: String,
            val status: String
        )
        val leaves = listOf(
            Leave("Demo Child", CHILD_ID, "2026-06-18", "2026-06-19", "Fever — advised rest by doctor.", "Approved"),
            Leave("Demo Student 02", STUDENT_ID, "2026-06-15", "2026-06-15", "Family function.", "Approved")
        )
        leaves.forEach { lv ->
            val exists = LeaveRequestsTable.selectAll()
                .where {
                    (LeaveRequestsTable.schoolId eq SCHOOL_ID) and
                        (LeaveRequestsTable.requesterName eq lv.requesterName) and
                        (LeaveRequestsTable.dateFrom eq lv.from)
                }
                .any()
            if (exists) return@forEach
            LeaveRequestsTable.insert {
                it[id] = UUID.randomUUID()
                it[schoolId] = SCHOOL_ID
                it[requesterId] = PARENT_ID
                it[requesterName] = lv.requesterName
                it[requesterRole] = "student"
                it[dateFrom] = lv.from
                it[dateTo] = lv.to
                it[reason] = lv.reason
                it[status] = lv.status
                it[actionedBy] = TEACHER_ID
                it[actionedAt] = now
                it[classId] = CLASS_G4_ID
                it[className] = DEMO_CLASS
                it[section] = DEMO_SECTION
                it[teacherId] = TEACHER_ID
                it[childId] = lv.childId
                it[parentId] = PARENT_ID
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    /**
     * The (student_code, name, roll) references for the demo Grade 4 / A roster,
     * matching exactly what seedRoster() inserts. Used by the assessment + the
     * homework seeders so marks/submissions line up with real students.
     */
    private data class RosterRef(val code: String, val name: String, val roll: Int)

    private fun buildRosterRefs(): List<RosterRef> {
        val refs = mutableListOf(RosterRef(DEMO_STUDENT_CODE, "Demo Child", 1))
        for (n in 2..DEMO_ROSTER_SIZE) {
            refs += RosterRef("DEMO-S%03d".format(n), "Demo Student %02d".format(n), n)
        }
        return refs
    }
}
