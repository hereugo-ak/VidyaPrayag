package com.littlebridge.enrollplus.feature.pulse

import com.littlebridge.enrollplus.feature.pulse.ParentPulseService.Companion.currentWeekStart
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParentPulseServiceTest {

    private val service = ParentPulseService()

    // ── computeTrend ────────────────────────────────────────────────────────

    @Test
    fun `computeTrend returns null when either value is null`() {
        assertNull(service.computeTrend(null, 90.0))
        assertNull(service.computeTrend(90.0, null))
        assertNull(service.computeTrend(null, null))
    }

    @Test
    fun `computeTrend returns up when current exceeds previous by more than 0_5`() {
        assertEquals("up", service.computeTrend(95.0, 90.0))
        assertEquals("up", service.computeTrend(90.6, 90.0))
    }

    @Test
    fun `computeTrend returns down when current drops by more than 0_5`() {
        assertEquals("down", service.computeTrend(85.0, 90.0))
        assertEquals("down", service.computeTrend(89.4, 90.0))
    }

    @Test
    fun `computeTrend returns stable when difference is within 0_5`() {
        assertEquals("stable", service.computeTrend(90.0, 90.0))
        assertEquals("stable", service.computeTrend(90.3, 90.0))
        assertEquals("stable", service.computeTrend(89.7, 90.0))
    }

    // ── buildActionableItems ────────────────────────────────────────────────

    @Test
    fun `buildActionableItems returns empty list when nothing pending`() {
        val items = service.buildActionableItems(
            homeworkPending = 0,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertTrue(items.isEmpty())
    }

    @Test
    fun `buildActionableItems includes homework when pending`() {
        val items = service.buildActionableItems(
            homeworkPending = 3,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertTrue(items.any { it.contains("3 homework submission(s) pending") })
    }

    @Test
    fun `buildActionableItems includes unread messages`() {
        val items = service.buildActionableItems(
            homeworkPending = 0,
            unreadMessages = 5,
            upcomingEvents = emptyList(),
        )
        assertTrue(items.any { it.contains("5 unread message(s)") })
    }

    @Test
    fun `buildActionableItems includes at most 2 upcoming events`() {
        val events = listOf(
            UpcomingEvent("Sports Day", "2026-07-01"),
            UpcomingEvent("PTM", "2026-07-03"),
            UpcomingEvent("Annual Day", "2026-07-10"),
        )
        val items = service.buildActionableItems(
            homeworkPending = 0,
            unreadMessages = 0,
            upcomingEvents = events,
        )
        assertEquals(2, items.size)
        assertTrue(items.any { it.contains("Sports Day") })
        assertTrue(items.any { it.contains("PTM") })
        assertTrue(items.none { it.contains("Annual Day") })
    }

    @Test
    fun `buildActionableItems includes all items in correct order`() {
        val events = listOf(UpcomingEvent("Exam", "2026-07-05"))
        val items = service.buildActionableItems(
            homeworkPending = 2,
            unreadMessages = 1,
            upcomingEvents = events,
        )
        assertEquals(3, items.size)
        assertEquals("2 homework submission(s) pending", items[0])
        assertEquals("1 unread message(s)", items[1])
        assertEquals("Upcoming: Exam on 2026-07-05", items[2])
    }

    // ── buildFallbackNarrative ──────────────────────────────────────────────

    @Test
    fun `buildFallbackNarrative returns no-activity message when all empty`() {
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = null,
            prevAttendance = null,
            marks = emptyList(),
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertEquals("No activity recorded for Aarav this week.", narrative)
    }

    @Test
    fun `buildFallbackNarrative includes attendance with trend`() {
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = 95.0,
            prevAttendance = 90.0,
            marks = emptyList(),
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertTrue(narrative.contains("Aarav's attendance was 95%"))
        assertTrue(narrative.contains("up from 90% last week"))
    }

    @Test
    fun `buildFallbackNarrative includes attendance down trend`() {
        val narrative = service.buildFallbackNarrative(
            studentName = "Priya",
            attendancePct = 80.0,
            prevAttendance = 92.0,
            marks = emptyList(),
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertTrue(narrative.contains("down from 92% last week"))
    }

    @Test
    fun `buildFallbackNarrative includes attendance stable trend`() {
        val narrative = service.buildFallbackNarrative(
            studentName = "Priya",
            attendancePct = 90.0,
            prevAttendance = 90.0,
            marks = emptyList(),
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertTrue(narrative.contains("same as last week"))
    }

    @Test
    fun `buildFallbackNarrative includes marks highlight`() {
        val marks = listOf(
            MarkEntry("Maths", "Unit Test 2", 85.0, 100),
            MarkEntry("Science", "Quiz", 18.0, 20),
        )
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = null,
            prevAttendance = null,
            marks = marks,
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        // Maths 85/100 = 85% > Science 18/20 = 90% → Science is top
        assertTrue(narrative.contains("18/20 in Science"))
        assertTrue(narrative.contains("90%"))
    }

    @Test
    fun `buildFallbackNarrative includes homework stats`() {
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = null,
            prevAttendance = null,
            marks = emptyList(),
            homeworkPending = 2,
            homeworkCompleted = 3,
            unreadMessages = 0,
            upcomingEvents = emptyList(),
        )
        assertTrue(narrative.contains("3 homework completed, 2 pending"))
    }

    @Test
    fun `buildFallbackNarrative includes unread messages`() {
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = null,
            prevAttendance = null,
            marks = emptyList(),
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 7,
            upcomingEvents = emptyList(),
        )
        assertTrue(narrative.contains("7 unread message(s)"))
    }

    @Test
    fun `buildFallbackNarrative includes first upcoming event`() {
        val events = listOf(
            UpcomingEvent("Sports Day", "2026-07-01"),
            UpcomingEvent("PTM", "2026-07-03"),
        )
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = null,
            prevAttendance = null,
            marks = emptyList(),
            homeworkPending = 0,
            homeworkCompleted = 0,
            unreadMessages = 0,
            upcomingEvents = events,
        )
        assertTrue(narrative.contains("Sports Day on 2026-07-01"))
        assertTrue(!narrative.contains("PTM"))
    }

    @Test
    fun `buildFallbackNarrative combines all parts`() {
        val marks = listOf(MarkEntry("Maths", "Unit Test", 45.0, 50))
        val events = listOf(UpcomingEvent("Exam", "2026-07-05"))
        val narrative = service.buildFallbackNarrative(
            studentName = "Aarav",
            attendancePct = 92.0,
            prevAttendance = 88.0,
            marks = marks,
            homeworkPending = 1,
            homeworkCompleted = 4,
            unreadMessages = 2,
            upcomingEvents = events,
        )
        assertTrue(narrative.contains("92%"))
        assertTrue(narrative.contains("up from 88%"))
        assertTrue(narrative.contains("45/50 in Maths"))
        assertTrue(narrative.contains("4 homework completed, 1 pending"))
        assertTrue(narrative.contains("2 unread message(s)"))
        assertTrue(narrative.contains("Exam on 2026-07-05"))
        assertTrue(narrative.endsWith("."))
    }

    // ── currentWeekStart ────────────────────────────────────────────────────

    @Test
    fun `currentWeekStart returns Monday for a Wednesday`() {
        val wed = LocalDate.of(2026, 6, 24) // Wednesday
        val monday = currentWeekStart(wed)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 6, 22), monday)
    }

    @Test
    fun `currentWeekStart returns same date when already Monday`() {
        val monday = LocalDate.of(2026, 6, 22) // Monday
        assertEquals(monday, currentWeekStart(monday))
    }

    @Test
    fun `currentWeekStart returns Monday for a Sunday`() {
        val sunday = LocalDate.of(2026, 6, 28) // Sunday
        val monday = currentWeekStart(sunday)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 6, 22), monday)
    }

    // ── formatWeekRange ─────────────────────────────────────────────────────

    @Test
    fun `formatWeekRange formats same-month range`() {
        val start = LocalDate.of(2026, 6, 22)
        val end = LocalDate.of(2026, 6, 28)
        assertEquals("Jun 22 - 28, 2026", service.formatWeekRange(start, end))
    }

    @Test
    fun `formatWeekRange formats cross-month range`() {
        val start = LocalDate.of(2026, 6, 29)
        val end = LocalDate.of(2026, 7, 5)
        assertEquals("Jun 29 - Jul 5, 2026", service.formatWeekRange(start, end))
    }

    @Test
    fun `formatWeekRange formats cross-year range`() {
        val start = LocalDate.of(2026, 12, 28)
        val end = LocalDate.of(2027, 1, 3)
        assertEquals("Dec 28 - Jan 3, 2027", service.formatWeekRange(start, end))
    }
}
