package com.littlebridge.enrollplus

import com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceLoadDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceSaveMarkDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceSaveRequest
import com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceSaveResultDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceStudentDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.CalendarOverlayDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.CheckInStatusDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedPeriodDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedWeekDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherObligationsDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Teacher Portal Rebuild — T-103. The resolved-day / week / check-in / obligations
 * shared DTOs (Doc 05 §4, Doc 06 §2, Doc 04 §5.5) must serialize round-trip so the
 * KMP client and the server agree on the wire contract field-for-field.
 */
class TeacherTodayModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun resolvedDay_roundTrips() {
        val original = ResolvedDayDto(
            date = "2026-06-22",
            weekday = 1,
            isHoliday = false,
            holidayName = null,
            periods = listOf(
                ResolvedPeriodDto(
                    periodId = "p1",
                    assignmentId = "a1",
                    className = "Grade 7",
                    section = "B",
                    subject = "Maths",
                    room = "12",
                    startTime = "09:00",
                    endTime = "09:45",
                    status = "SCHEDULED",
                    attendanceMarked = true,
                ),
                ResolvedPeriodDto(
                    periodId = "p2",
                    assignmentId = "a2",
                    className = "Grade 7",
                    section = "B",
                    subject = "Science",
                    startTime = "10:00",
                    endTime = "10:45",
                    status = "CANCELLED",
                    note = "Teacher on duty",
                ),
            ),
            calendar = listOf(
                CalendarOverlayDto(
                    eventId = "CAL_AB12CD34",
                    type = "EXAM",
                    title = "Unit Test I",
                    assessmentId = "as1",
                ),
            ),
            nowIndex = 0,
            nextIndex = 1,
        )
        val encoded = json.encodeToString(ResolvedDayDto.serializer(), original)
        // wire field names are snake_case via @SerialName
        assertTrue(encoded.contains("\"is_holiday\""))
        assertTrue(encoded.contains("\"attendance_marked\""))
        assertTrue(encoded.contains("\"now_index\""))
        val decoded = json.decodeFromString(ResolvedDayDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun resolvedWeek_roundTrips() {
        val day = ResolvedDayDto(date = "2026-06-22", weekday = 1)
        val original = ResolvedWeekDto(weekStart = "2026-06-22", days = listOf(day))
        val encoded = json.encodeToString(ResolvedWeekDto.serializer(), original)
        assertTrue(encoded.contains("\"week_start\""))
        val decoded = json.decodeFromString(ResolvedWeekDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun checkInStatus_roundTrips() {
        val original = CheckInStatusDto(
            checkedIn = true,
            checkedInAt = "2026-06-22T08:42:00Z",
            method = "biometric",
            date = "2026-06-22",
        )
        val encoded = json.encodeToString(CheckInStatusDto.serializer(), original)
        assertTrue(encoded.contains("\"checked_in\""))
        assertTrue(encoded.contains("\"checked_in_at\""))
        val decoded = json.decodeFromString(CheckInStatusDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun obligations_allCaughtUp_isHonest() {
        val empty = TeacherObligationsDto()
        assertTrue(empty.isAllCaughtUp)

        val withWork = TeacherObligationsDto(
            unmarkedClasses = 3,
            classesTodayTotal = 6,
            items = listOf(
                ObligationItemDto(
                    id = "att-a1",
                    type = "attendance",
                    title = "3 of 6 classes unmarked",
                    count = 3,
                    assignmentId = "a1",
                ),
            ),
        )
        assertFalse(withWork.isAllCaughtUp)

        val encoded = json.encodeToString(TeacherObligationsDto.serializer(), withWork)
        assertTrue(encoded.contains("\"unmarked_classes\""))
        val decoded = json.decodeFromString(TeacherObligationsDto.serializer(), encoded)
        assertEquals(withWork, decoded)
    }

    // ── T-202: typed, scoped attendance load/save (Doc 06 §1.2/§3.8) ───────────

    @Test
    fun attendanceLoad_roundTrips_withLeaveDefaults() {
        val original = AttendanceLoadDto(
            assignmentId = "a1",
            date = "2026-06-23",
            scope = "7B · Mathematics",
            className = "Grade 7",
            section = "B",
            subject = "Mathematics",
            students = listOf(
                AttendanceStudentDto(studentId = "s1", name = "Asha", rollNo = "1", status = "present"),
                AttendanceStudentDto(
                    studentId = "s2",
                    name = "Bilal",
                    rollNo = "2",
                    status = "leave",
                    source = "leave_auto",
                    enrollmentId = "e2",
                ),
            ),
            alreadyMarked = true,
            lastMarkedBy = "Mr. Rao",
            lastMarkedAt = "2026-06-23T09:05:00Z",
            leaveDefaults = listOf("s2"),
            backDateWindowDays = 7,
        )
        val encoded = json.encodeToString(AttendanceLoadDto.serializer(), original)
        // snake_case wire names
        assertTrue(encoded.contains("\"assignment_id\""))
        assertTrue(encoded.contains("\"already_marked\""))
        assertTrue(encoded.contains("\"leave_defaults\""))
        assertTrue(encoded.contains("\"back_date_window_days\""))
        val decoded = json.decodeFromString(AttendanceLoadDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun attendanceLoad_decodesServerEnvelope_ignoringMessage() {
        // The server replies { success, message, data:{…} }; the shared response
        // wrapper has only { success, data } — ignoreUnknownKeys must drop message.
        val wire = """
            {"success":true,"message":"Attendance loaded",
             "data":{"assignment_id":"a1","date":"2026-06-23","students":[],
                     "already_marked":false,"leave_defaults":[]}}
        """.trimIndent()
        val decoded = json.decodeFromString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.AttendanceLoadResponse.serializer(),
            wire,
        )
        assertTrue(decoded.success)
        assertEquals("a1", decoded.data.assignmentId)
        assertFalse(decoded.data.alreadyMarked)
    }

    @Test
    fun attendanceSave_roundTrips() {
        val req = AttendanceSaveRequest(
            assignmentId = "a1",
            date = "2026-06-23",
            marks = listOf(
                AttendanceSaveMarkDto(studentId = "s1", status = "present"),
                AttendanceSaveMarkDto(studentId = "s2", status = "leave"),
            ),
        )
        val encoded = json.encodeToString(AttendanceSaveRequest.serializer(), req)
        assertTrue(encoded.contains("\"assignment_id\""))
        assertTrue(encoded.contains("\"student_id\""))
        val decoded = json.decodeFromString(AttendanceSaveRequest.serializer(), encoded)
        assertEquals(req, decoded)

        val result = AttendanceSaveResultDto(saved = 2, date = "2026-06-23")
        val re = json.encodeToString(AttendanceSaveResultDto.serializer(), result)
        assertEquals(result, json.decodeFromString(AttendanceSaveResultDto.serializer(), re))
    }

    // ── T-302: assessment + marks lifecycle (Doc 07 §1.3/§2/§5/§6) ─────────────

    @Test
    fun assessment_roundTrips_withScopeAndStatus() {
        val original = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentDto(
            id = "as1",
            assignmentId = "a1",
            classId = "c1",
            subjectId = "sub1",
            className = "Grade 7",
            section = "B",
            subject = "Mathematics",
            name = "Unit Test I",
            type = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentType.SCHEDULED,
            maxMarks = 25,
            passMarks = 10,
            examDate = "2026-06-25",
            status = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentStatus.MARKS_PENDING,
            enteredCount = 31,
            rosterCount = 38,
        )
        val encoded = json.encodeToString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentDto.serializer(),
            original,
        )
        assertTrue(encoded.contains("\"assignment_id\""))
        assertTrue(encoded.contains("\"max_marks\""))
        assertTrue(encoded.contains("\"pass_marks\""))
        assertTrue(encoded.contains("\"entered_count\""))
        val decoded = json.decodeFromString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentDto.serializer(),
            encoded,
        )
        assertEquals(original, decoded)
        // honesty helpers
        assertTrue(decoded.hasPassLine)
        assertFalse(decoded.isPublished)
    }

    @Test
    fun marksLoad_restoresEnteredValues_andAbsent() {
        val original = com.littlebridge.enrollplus.feature.teacher.domain.model.MarksLoadDto(
            assessment = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentDto(
                id = "as1", name = "Unit Test I", maxMarks = 25, passMarks = 10,
                status = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentStatus.MARKS_PENDING,
            ),
            students = listOf(
                com.littlebridge.enrollplus.feature.teacher.domain.model.MarkEntryDto(
                    studentId = "s1", name = "Asha", rollNo = "1", marks = 22f,
                ),
                // AB is a distinct state, not a 0 (§5.2)
                com.littlebridge.enrollplus.feature.teacher.domain.model.MarkEntryDto(
                    studentId = "s2", name = "Bilal", rollNo = "2", marks = null, isAbsent = true,
                    remark = "Sick",
                ),
            ),
            enteredCount = 1,
            rosterCount = 2,
        )
        val encoded = json.encodeToString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.MarksLoadDto.serializer(),
            original,
        )
        assertTrue(encoded.contains("\"is_absent\""))
        assertTrue(encoded.contains("\"roll_no\""))
        val decoded = json.decodeFromString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.MarksLoadDto.serializer(),
            encoded,
        )
        assertEquals(original, decoded)
    }

    @Test
    fun marksSave_carriesNoPublishFlag_andEchoesUnpublishedStatus() {
        val req = com.littlebridge.enrollplus.feature.teacher.domain.model.MarksSaveRequest(
            entries = listOf(
                com.littlebridge.enrollplus.feature.teacher.domain.model.MarkSaveEntryDto(
                    studentId = "s1", marks = 22f,
                ),
                com.littlebridge.enrollplus.feature.teacher.domain.model.MarkSaveEntryDto(
                    studentId = "s2", marks = null, isAbsent = true,
                ),
            ),
        )
        val encoded = json.encodeToString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.MarksSaveRequest.serializer(),
            req,
        )
        // The B-MK-1 fix is structural: a SAVE payload has no publish concept at all.
        assertFalse(encoded.contains("publish"))
        assertTrue(encoded.contains("\"student_id\""))
        val decoded = json.decodeFromString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.MarksSaveRequest.serializer(),
            encoded,
        )
        assertEquals(req, decoded)

        // SAVE result echoes the still-unpublished status so the UI can prove it.
        val result = com.littlebridge.enrollplus.feature.teacher.domain.model.MarksSaveResultDto(
            saved = 2, enteredCount = 2, rosterCount = 38,
        )
        assertEquals(
            com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentStatus.MARKS_PENDING,
            result.status,
        )
    }

    @Test
    fun assessmentHistory_roundTrips_andHonestEmpty() {
        val empty = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentHistoryDto()
        assertFalse(empty.hasData)

        val original = com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentHistoryDto(
            assignmentId = "a1",
            className = "Grade 7",
            subject = "Mathematics",
            timeline = listOf(
                com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentTrendPointDto(
                    assessmentId = "as1", name = "Unit Test I", examDate = "2026-05-01",
                    maxMarks = 25, average = 18.4f, passRate = 0.86f, enteredCount = 38, rosterCount = 38,
                ),
            ),
            distribution = listOf(
                com.littlebridge.enrollplus.feature.teacher.domain.model.MarkBucketDto(label = "0–24%", count = 2),
                com.littlebridge.enrollplus.feature.teacher.domain.model.MarkBucketDto(label = "75–100%", count = 9),
            ),
        )
        val encoded = json.encodeToString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentHistoryDto.serializer(),
            original,
        )
        assertTrue(encoded.contains("\"assignment_id\""))
        assertTrue(encoded.contains("\"pass_rate\""))
        val decoded = json.decodeFromString(
            com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentHistoryDto.serializer(),
            encoded,
        )
        assertEquals(original, decoded)
        assertTrue(decoded.hasData)
    }
}
