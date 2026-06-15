/*
 * File: DateUtil.kt
 * Module: util
 *
 * Tiny cross-platform date helpers used by the VDatePicker calendar component
 * and any screen that needs "today" without pulling in kotlinx-datetime.
 *
 * All dates in this app are ISO "YYYY-MM-DD" strings on the wire, so these
 * helpers stay string-first to match the existing calendar grids
 * (AcademicCalendarScreenV2 / ParentAttendanceCalendar).
 */
package com.littlebridge.vidyaprayag.util

/** Today's local date as an ISO "YYYY-MM-DD" string. Platform-provided. */
expect fun todayIso(): String

/** Parses "YYYY-MM-DD" into Triple(year, month, day); null on bad input. */
fun parseIsoDate(iso: String): Triple<Int, Int, Int>? {
    if (iso.length < 10) return null
    val y = iso.substring(0, 4).toIntOrNull() ?: return null
    val m = iso.substring(5, 7).toIntOrNull() ?: return null
    val d = iso.substring(8, 10).toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return Triple(y, m, d)
}

/** Formats y/m/d into an ISO "YYYY-MM-DD" string (zero-padded). */
fun isoOf(year: Int, month: Int, day: Int): String {
    val mm = month.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "$year-$mm-$dd"
}

fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}

/**
 * Day of week for a given date using Sakamoto's algorithm.
 * Returns 0=Sunday .. 6=Saturday.
 */
fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month < 3) year - 1 else year
    return (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
}

val MONTH_LONG: List<String> = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

val WEEKDAY_SHORT: List<String> = listOf("S", "M", "T", "W", "T", "F", "S")
