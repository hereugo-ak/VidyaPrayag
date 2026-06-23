package com.littlebridge.vidyaprayag

import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CalendarOverlayDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CheckInStatusDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ResolvedDayDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ResolvedPeriodDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ResolvedWeekDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherObligationsDto
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
}
