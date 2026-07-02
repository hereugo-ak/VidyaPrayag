package com.littlebridge.enrollplus.feature.library

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryPluginsTest {

    // ── ISBN Validator ──────────────────────────────────────────────────────

    @Test
    fun `valid ISBN-10 is accepted`() {
        val validator = DefaultIsbnValidator()
        assertTrue(validator.validate("0306406152"))
    }

    @Test
    fun `valid ISBN-13 is accepted`() {
        val validator = DefaultIsbnValidator()
        assertTrue(validator.validate("9780306406157"))
    }

    @Test
    fun `ISBN with hyphens is accepted`() {
        val validator = DefaultIsbnValidator()
        assertTrue(validator.validate("0-306-40615-2"))
    }

    @Test
    fun `ISBN with spaces is accepted`() {
        val validator = DefaultIsbnValidator()
        assertTrue(validator.validate("978 0306 40615 7"))
    }

    @Test
    fun `invalid ISBN too short is rejected`() {
        val validator = DefaultIsbnValidator()
        assertFalse(validator.validate("12345"))
    }

    @Test
    fun `invalid ISBN with letters is rejected`() {
        val validator = DefaultIsbnValidator()
        assertFalse(validator.validate("030640615X"))
    }

    @Test
    fun `normalize strips hyphens and spaces`() {
        val validator = DefaultIsbnValidator()
        assertEquals("0306406152", validator.normalize("0-306-40615-2"))
        assertEquals("9780306406157", validator.normalize("978 0306 40615 7"))
    }

    // ── Fine Calculator ─────────────────────────────────────────────────────

    @Test
    fun `fine is zero when returned on time`() {
        val calc = DefaultFineCalculator()
        val due = LocalDate.of(2026, 1, 10)
        val returned = LocalDate.of(2026, 1, 10)
        assertEquals(0.0, calc.calculate(due, returned, 1.0, true))
    }

    @Test
    fun `fine is zero when returned early`() {
        val calc = DefaultFineCalculator()
        val due = LocalDate.of(2026, 1, 10)
        val returned = LocalDate.of(2026, 1, 5)
        assertEquals(0.0, calc.calculate(due, returned, 1.0, true))
    }

    @Test
    fun `fine is calculated correctly for 5 days overdue`() {
        val calc = DefaultFineCalculator()
        val due = LocalDate.of(2026, 1, 10)
        val returned = LocalDate.of(2026, 1, 15)
        assertEquals(5.0, calc.calculate(due, returned, 1.0, true))
    }

    @Test
    fun `fine uses custom fine per day`() {
        val calc = DefaultFineCalculator()
        val due = LocalDate.of(2026, 1, 10)
        val returned = LocalDate.of(2026, 1, 15)
        assertEquals(25.0, calc.calculate(due, returned, 5.0, true))
    }

    @Test
    fun `fine is capped when capEnabled`() {
        val calc = DefaultFineCalculator()
        val due = LocalDate.of(2026, 1, 1)
        val returned = LocalDate.of(2026, 6, 1)
        val daysOverdue = ChronoUnit.DAYS.between(due, returned).toInt()
        val expectedRaw = daysOverdue * 1.0
        val result = calc.calculate(due, returned, 1.0, true)
        assertTrue(result <= DefaultFineCalculator.FINE_CAP)
        assertEquals(expectedRaw.coerceAtMost(DefaultFineCalculator.FINE_CAP), result)
    }

    @Test
    fun `fine is uncapped when capDisabled`() {
        val calc = DefaultFineCalculator()
        val due = LocalDate.of(2026, 1, 1)
        val returned = LocalDate.of(2026, 6, 1)
        val daysOverdue = ChronoUnit.DAYS.between(due, returned).toInt()
        val expected = daysOverdue * 1.0
        assertEquals(expected, calc.calculate(due, returned, 1.0, false))
    }

    // ── Due Date Calculator ─────────────────────────────────────────────────

    @Test
    fun `due date adds loan days to issue date`() {
        val calc = DefaultDueDateCalculator()
        val issue = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2026, 1, 15), calc.calculate(issue, 14))
    }

    @Test
    fun `due date crosses month boundary`() {
        val calc = DefaultDueDateCalculator()
        val issue = LocalDate.of(2026, 1, 25)
        assertEquals(LocalDate.of(2026, 2, 8), calc.calculate(issue, 14))
    }

    // ── Barcode Generator ───────────────────────────────────────────────────

    @Test
    fun `barcode has correct format`() {
        val gen = DefaultBarcodeGenerator()
        val schoolId = java.util.UUID.fromString("aabbccdd-1234-5678-90ab-cdef01234567")
        val bookId = java.util.UUID.fromString("eeff0011-2233-4455-6677-8899aabbccdd")
        val barcode = gen.generate(schoolId, bookId, 1)
        assertTrue(barcode.startsWith("LIB-aabbccdd-"))
        assertTrue(barcode.endsWith("-001"))
    }

    @Test
    fun `barcode pads copy number to 3 digits`() {
        val gen = DefaultBarcodeGenerator()
        val schoolId = java.util.UUID.randomUUID()
        val bookId = java.util.UUID.randomUUID()
        val barcode = gen.generate(schoolId, bookId, 42)
        assertTrue(barcode.endsWith("-042"))
    }

    // ── Feature Flags ───────────────────────────────────────────────────────

    @Test
    fun `known feature flags are enabled`() {
        val flags = DefaultFeatureFlagService()
        assertTrue(flags.isEnabled("library_enabled"))
        assertTrue(flags.isEnabled("library.fines"))
        assertTrue(flags.isEnabled("library.reservations"))
        assertTrue(flags.isEnabled("library.wishlist"))
        assertTrue(flags.isEnabled("library.reading_goals"))
        assertTrue(flags.isEnabled("library.quick_issue"))
    }

    @Test
    fun `unknown feature flag is disabled`() {
        val flags = DefaultFeatureFlagService()
        assertFalse(flags.isEnabled("library.unknown_feature"))
    }
}
