package com.littlebridge.vidyaprayag.ui.v2.data

/**
 * MockV2 — a faithful Kotlin mirror of the design source `UI screens/src/app/lib/mock.ts`.
 *
 * The Figma React prototype renders every portal screen from this single dataset so cross-references
 * (a child in Parent == a row in Admin) hold up. The `ui/v2` screens are pixel-faithful copies of
 * that prototype, so they render from the *same* shapes/values here. This keeps the rebuilt UI
 * looking identical to the design while the live `shared/` ViewModels mature behind the scenes.
 *
 * NOTE: This is presentation seed data only — it never touches the network/`shared/` layer.
 */
object MockV2 {

    // ── School ────────────────────────────────────────────────────────────────
    data class School(
        val id: String,
        val name: String,
        val shortName: String,
        val code: String,
        val board: String,
        val type: String,
        val medium: String,
        val address: String,
        val phone: String,
        val email: String,
        val principal: String,
        val founded: Int,
        val academicYear: String,
        val dayOfYear: Int,
        val totalDays: Int,
    )

    val school = School(
        id = "svm",
        name = "Saraswati Vidya Mandir",
        shortName = "SVM",
        code = "SVM001",
        board = "CBSE",
        type = "Private Unaided",
        medium = "English",
        address = "12 Civil Lines, Lucknow, UP 226001",
        phone = "+91 522 220 4421",
        email = "office@svm.edu.in",
        principal = "Dr. Anita Verma",
        founded = 1987,
        academicYear = "2025–26",
        dayOfYear = 47,
        totalDays = 220,
    )

    // ── Classes / subjects ──────────────────────────────────────────────────────
    data class SchoolClass(val id: String, val name: String, val section: String, val strength: Int)

    val classes = listOf(
        SchoolClass("9A", "Class 9", "A", 32),
        SchoolClass("9B", "Class 9", "B", 30),
        SchoolClass("10A", "Class 10", "A", 34),
        SchoolClass("10B", "Class 10", "B", 31),
        SchoolClass("12S", "Class 12", "Science", 28),
        SchoolClass("12C", "Class 12", "Commerce", 25),
    )

    data class Subject(val code: String, val name: String, val type: String)

    val subjects = listOf(
        Subject("MAT", "Mathematics", "Core"),
        Subject("SCI", "Science", "Core"),
        Subject("ENG", "English", "Core"),
        Subject("HIN", "Hindi", "Language"),
        Subject("SST", "Social Studies", "Core"),
        Subject("COMP", "Computer Applications", "Core"),
        Subject("PE", "Physical Education", "Co-curricular"),
    )

    // ── Teachers ────────────────────────────────────────────────────────────────
    data class Teacher(
        val id: String,
        val name: String,
        val subjects: List<String>,
        val classes: List<String>,
        val lastActive: String,
        val username: String,
        val active: Boolean,
        val photo: String = "",
    )

    val teachers = listOf(
        Teacher("T01", "Dr. Ramesh Sharma", listOf("Chemistry", "Science"), listOf("10A", "10B", "12S"), "2h ago", "SVM001.T01", true),
        Teacher("T02", "Mrs. Priya Iyer", listOf("Mathematics"), listOf("9A", "9B", "10A"), "15m ago", "SVM001.T02", true),
        Teacher("T03", "Mr. Arjun Mehta", listOf("English"), listOf("9A", "10A", "12S"), "1d ago", "SVM001.T03", true),
        Teacher("T04", "Ms. Kavita Singh", listOf("Hindi"), listOf("9A", "9B"), "3h ago", "SVM001.T04", true),
        Teacher("T05", "Mr. Suresh Pillai", listOf("Social Studies"), listOf("9A", "10A", "10B"), "9d ago", "SVM001.T05", false),
        Teacher("T06", "Mrs. Deepika Rao", listOf("Computer Applications"), listOf("10A", "12C"), "30m ago", "SVM001.T06", true),
        Teacher("T07", "Mr. Vikram Joshi", listOf("Physical Education"), listOf("9A", "9B", "10A", "10B"), "4h ago", "SVM001.T07", true),
        Teacher("T08", "Dr. Meera Kapoor", listOf("Physics"), listOf("12S"), "5h ago", "SVM001.T08", true),
    )

    // ── Students ──────────────────────────────────────────────────────────────
    enum class Presence { Present, Absent, Late }
    enum class Pews { Ok, Warn, Risk }

    data class Student(
        val id: String,
        val name: String,
        val roll: String,
        val klass: String,
        val attendance: Int,
        val lastMarks: Int,
        val fees: Int,
        val parentMobile: String,
        val parentName: String,
        val dob: String,
        val gender: String,
        val today: Presence,
        val pews: Pews,
    )

    val students = listOf(
        Student("s1", "Aarav Khanna", "01", "10A", 94, 86, 0, "+91 98100 12345", "Rajeev Khanna", "12 Aug 2010", "M", Presence.Present, Pews.Ok),
        Student("s2", "Riya Sharma", "02", "10A", 71, 74, 12500, "+91 98100 22221", "Sneha Sharma", "03 Jan 2010", "F", Presence.Absent, Pews.Warn),
        Student("s3", "Kabir Singh", "03", "10A", 88, 92, 0, "+91 98100 33345", "Pratap Singh", "21 Feb 2010", "M", Presence.Present, Pews.Ok),
        Student("s4", "Ananya Verma", "04", "10A", 96, 91, 0, "+91 98100 44321", "Anita Verma", "07 May 2010", "F", Presence.Present, Pews.Ok),
        Student("s5", "Vihaan Kapoor", "05", "10A", 58, 61, 28500, "+91 98100 55512", "Mohan Kapoor", "18 Sep 2010", "M", Presence.Absent, Pews.Risk),
        Student("s6", "Ishaan Reddy", "06", "10A", 90, 79, 0, "+91 98100 66631", "Lakshmi Reddy", "11 Oct 2010", "M", Presence.Late, Pews.Ok),
        Student("s7", "Diya Patel", "07", "10A", 92, 88, 0, "+91 98100 77741", "Mahesh Patel", "04 Jul 2010", "F", Presence.Present, Pews.Ok),
        Student("s8", "Aryan Bose", "08", "10A", 81, 72, 5500, "+91 98100 88811", "Sanjay Bose", "29 Nov 2010", "M", Presence.Present, Pews.Ok),
        Student("s9", "Saanvi Joshi", "09", "10A", 97, 95, 0, "+91 98100 99921", "Reena Joshi", "15 Mar 2010", "F", Presence.Present, Pews.Ok),
        Student("s10", "Rohan Das", "10", "10A", 64, 58, 18200, "+91 98101 11111", "Bikash Das", "08 Jun 2010", "M", Presence.Absent, Pews.Warn),
        Student("s11", "Tara Menon", "01", "9A", 89, 84, 0, "+91 98102 22231", "Asha Menon", "13 Apr 2011", "F", Presence.Present, Pews.Ok),
        Student("s12", "Yash Agarwal", "02", "9A", 76, 68, 8200, "+91 98102 33341", "Sumit Agarwal", "30 Jan 2011", "M", Presence.Present, Pews.Ok),
    )

    /** The active parent's child — "Riya Sharma". */
    val childForParent = students[1]

    /** Riya + Tara as siblings for the child-switcher demo. */
    val siblings = listOf(students[1], students[10])

    val parentName = "Sneha Sharma"

    // ── Timetable ───────────────────────────────────────────────────────────────
    data class Period(val period: Int, val subject: String, val klass: String, val time: String, val teacher: String)

    val timetableToday = listOf(
        Period(1, "Mathematics", "10A", "08:00 – 08:45", "Mrs. Priya Iyer"),
        Period(2, "Science", "10A", "08:45 – 09:30", "Dr. Ramesh Sharma"),
        Period(3, "English", "10A", "09:30 – 10:15", "Mr. Arjun Mehta"),
        Period(4, "Hindi", "10A", "10:30 – 11:15", "Ms. Kavita Singh"),
        Period(5, "Computer", "10A", "11:15 – 12:00", "Mrs. Deepika Rao"),
        Period(6, "PE", "10A", "12:00 – 12:45", "Mr. Vikram Joshi"),
    )

    // ── Coverage / activity / pending ───────────────────────────────────────────
    data class ClassCoverage(val klass: String, val pct: Int)

    val classAttendanceGrid = classes.mapIndexed { i, c ->
        c to listOf(92, 87, 84, 71, 95, 88)[i]
    }

    val syllabusCoverage = listOf(
        ClassCoverage("9-A", 78),
        ClassCoverage("9-B", 94),
        ClassCoverage("10-A", 61),
        ClassCoverage("10-B", 72),
        ClassCoverage("12-S", 85),
        ClassCoverage("12-C", 80),
    )

    data class TeacherActivity(val who: String, val what: String, val whenAt: String)

    val recentTeacherActivity = listOf(
        TeacherActivity("Dr. Ramesh Sharma", "updated Class 10-A Chemistry syllabus", "2h ago"),
        TeacherActivity("Mrs. Priya Iyer", "entered Class 9-A Math Unit Test marks", "3h ago"),
        TeacherActivity("Ms. Kavita Singh", "marked Class 9-B attendance", "5h ago"),
    )

    data class PendingAction(val title: String, val sub: String, val cta: String)

    val pendingActions = listOf(
        PendingAction("2 parent messages awaiting reply", "Overdue > 24 hrs", "Reply"),
        PendingAction("Fee reminder batch due tomorrow", "84 parents will be notified", "Review"),
        PendingAction("Exam timetable not uploaded for Class 12", "Half-yearly in 18 days", "Upload"),
    )

    // ── Announcements / messages / notifications ────────────────────────────────
    data class Announcement(
        val id: String,
        val title: String,
        val date: String,
        val body: String,
        val recipients: String,
        val opens: String,
    )

    val announcements = listOf(
        Announcement("a1", "Parent–Teacher Meeting — Class 10", "Tomorrow, 10:00 AM", "Half-yearly PTM for all Class 10 sections. Please carry the report card stub.", "Class 10 parents", "234 / 316"),
        Announcement("a2", "School closed on 12 June", "Sent 3 days ago", "Local civic holiday. Buses will not operate.", "All school", "1,084 / 1,210"),
        Announcement("a3", "Annual Sports Day fixtures", "Sent 6 days ago", "Trials begin Monday across all sections. Sports kit mandatory.", "All teachers + parents", "942 / 1,210"),
    )

    data class InboxMessage(
        val id: String,
        val parent: String,
        val child: String,
        val preview: String,
        val time: String,
        val overdue: Boolean,
    )

    val messagesInbox = listOf(
        InboxMessage("m1", "Sneha Sharma", "Riya Sharma — 10A", "Riya was unwell yesterday, can the homework be shared?", "26h ago", true),
        InboxMessage("m2", "Mohan Kapoor", "Vihaan Kapoor — 10A", "Following up on fee deferment request from last week.", "8h ago", false),
        InboxMessage("m3", "Asha Menon", "Tara Menon — 9A", "Thank you for the swift response on the PTM slot.", "2d ago", false),
    )

    enum class NotifCategory { Attendance, Academic, Fees, Announcement }

    data class Notification(
        val id: String,
        val category: NotifCategory,
        val title: String,
        val body: String,
        val time: String,
        val unread: Boolean,
    )

    val notifications = listOf(
        Notification("n1", NotifCategory.Attendance, "Riya was marked absent today", "By Mrs. Priya Iyer • Class 10-A", "2h ago", true),
        Notification("n2", NotifCategory.Academic, "Science syllabus updated", "Chapter 6 — Periodic Table covered today", "3h ago", true),
        Notification("n3", NotifCategory.Fees, "Term-2 fee due in 6 days", "₹12,500 — Tuition + Transport", "Yesterday", false),
        Notification("n4", NotifCategory.Announcement, "PTM scheduled — Class 10", "Tomorrow at 10:00 AM in the main hall", "Yesterday", false),
        Notification("n5", NotifCategory.Academic, "Mathematics Unit Test marks released", "Riya scored 74 / 100. Class avg 68.", "3 Jun", false),
    )

    // ── Marks / trends / attendance ─────────────────────────────────────────────
    data class MarkRow(
        val name: String,
        val date: String,
        val subject: String,
        val marks: Int,
        val total: Int,
        val avg: Int,
    )

    val marksHistory = listOf(
        MarkRow("Unit Test 1", "12 Apr", "Mathematics", 78, 100, 64),
        MarkRow("Unit Test 1", "12 Apr", "Science", 82, 100, 71),
        MarkRow("Mid Term", "18 May", "Mathematics", 71, 100, 66),
        MarkRow("Mid Term", "18 May", "Science", 75, 100, 70),
        MarkRow("Unit Test 2", "02 Jun", "Mathematics", 74, 100, 68),
        MarkRow("Unit Test 2", "02 Jun", "Science", 88, 100, 73),
    )

    data class TrendPoint(val x: String, val you: Int, val avg: Int)

    val subjectTrend = listOf(
        TrendPoint("UT1", 78, 64),
        TrendPoint("Mid", 71, 66),
        TrendPoint("UT2", 74, 68),
    )

    enum class DayStatus { Present, Absent, Late, Holiday, Future }

    data class AttendanceDay(val day: Int, val status: DayStatus)

    val attendanceMonth: List<AttendanceDay> = (1..30).map { d ->
        val status = when {
            d > 24 -> DayStatus.Future
            d in listOf(1, 8, 15, 22) -> DayStatus.Holiday
            d in listOf(5, 12) -> DayStatus.Absent
            d == 17 -> DayStatus.Late
            else -> DayStatus.Present
        }
        AttendanceDay(d, status)
    }

    // ── Fees ────────────────────────────────────────────────────────────────────
    data class FeeHead(val head: String, val amount: Int, val status: String, val dueDate: String)

    val feeBreakdown = listOf(
        FeeHead("Tuition Fee", 8500, "Due", "11 Jun 2026"),
        FeeHead("Transport", 2200, "Due", "11 Jun 2026"),
        FeeHead("Lab Fee", 800, "Due", "11 Jun 2026"),
        FeeHead("Activity Fee", 1000, "Due", "11 Jun 2026"),
    )

    data class FeePayment(val date: String, val amount: Int, val head: String, val receipt: String)

    val feeHistory = listOf(
        FeePayment("12 Apr 2026", 12500, "Term-1 Composite", "RCP-00482"),
        FeePayment("10 Jan 2026", 12500, "Term-3 (2025-26) Composite", "RCP-00321"),
        FeePayment("08 Oct 2025", 12500, "Term-2 (2025-26) Composite", "RCP-00198"),
    )

    // ── Discovery marketplace ───────────────────────────────────────────────────
    data class DiscoverySchool(
        val id: String,
        val name: String,
        val board: String,
        val type: String,
        val coed: Boolean,
        val medium: String,
        val distance: String,
        val feeRange: String,
        val sri: Double,
        val result: String,
        val photo: String,
    )

    val discoverySchools = listOf(
        DiscoverySchool("ds1", "Delhi Public School — Lucknow", "CBSE", "Private", true, "English", "1.8 km", "₹ 48,000 – ₹ 72,000", 8.7, "96%", "https://images.unsplash.com/photo-1728206348193-9b5ae74a7d32?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080"),
        DiscoverySchool("ds2", "City Montessori School", "ICSE", "Private", true, "English", "2.3 km", "₹ 38,000 – ₹ 56,000", 8.4, "94%", "https://images.unsplash.com/photo-1719159381916-062fa9f435a6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080"),
        DiscoverySchool("ds3", "Saraswati Vidya Mandir", "CBSE", "Private", true, "English", "0.6 km", "₹ 25,000 – ₹ 45,000", 7.9, "91%", "https://images.unsplash.com/photo-1613897728606-6ccdee638d66?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080"),
        DiscoverySchool("ds4", "St. Francis' College", "ICSE", "Private", false, "English", "3.7 km", "₹ 42,000 – ₹ 60,000", 8.1, "93%", "https://images.unsplash.com/photo-1651847162993-e7d4fe011eee?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080"),
        DiscoverySchool("ds5", "Loreto Convent", "ICSE", "Private", false, "English", "4.1 km", "₹ 36,000 – ₹ 52,000", 8.0, "92%", "https://images.unsplash.com/photo-1698360296111-98d7d6a23d6f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080"),
        DiscoverySchool("ds6", "Kendriya Vidyalaya Cantt", "CBSE", "Central Govt", true, "English + Hindi", "5.2 km", "₹ 6,000 – ₹ 12,000", 7.6, "89%", "https://images.unsplash.com/photo-1568667256549-094345857637?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080"),
    )

    // ── Calendar ────────────────────────────────────────────────────────────────
    enum class EventType { Academic, Event, Deadline, Holiday }

    data class CalendarEvent(val day: Int, val label: String, val type: EventType)

    val calendarEvents = listOf(
        CalendarEvent(6, "PTM Class 10", EventType.Academic),
        CalendarEvent(9, "Sports Trials", EventType.Event),
        CalendarEvent(11, "Fee Due", EventType.Deadline),
        CalendarEvent(12, "Civic Holiday", EventType.Holiday),
        CalendarEvent(18, "Half-Yearly Begins", EventType.Academic),
    )

    /** Format "10A" → "10 - A" (matches React's `.replace(/(\d+)/, "$1 - ")`-style display). */
    fun classDisplay(klass: String): String {
        val idx = klass.indexOfFirst { it.isLetter() }
        return if (idx > 0) "${klass.substring(0, idx)} - ${klass.substring(idx)}" else klass
    }
}
