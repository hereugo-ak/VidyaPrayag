package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentAttendanceDayDto
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * RA-S19 — Parent attendance month-calendar.
 *
 * Replaces the flat 30-row list with a true month grid: each day cell is colour-coded by
 * its attendance status (present / late / absent), the month can be paged, and a legend
 * sits below. Built entirely with V* primitives + [VTheme] tokens — no Material defaults,
 * no external date library (manual ISO date math, mirroring the existing
 * AcademicCalendarScreenV2 convention so the codebase stays kotlinx-datetime-free).
 *
 * Input is the same `records: List<ParentAttendanceDayDto>` the endpoint already returns
 * (`date` = "YYYY-MM-DD", `status` = present|late|absent), so no backend change is needed.
 */
@Composable
internal fun ParentAttendanceCalendar(
    records: List<ParentAttendanceDayDto>,
) {
    val c = VTheme.colors

    // Index records by ISO date for O(1) day lookup. If a day has multiple rows
    // (it shouldn't — the upsert is per-day), the last one wins.
    val byDate = remember(records) { records.associate { it.date to it.status.lowercase() } }

    // Month list available in the data, newest first. If empty, fall back to a single
    // sensible month so the grid still renders (it will simply be all-blank).
    val months = remember(records) {
        val set = records.mapNotNull { parseIsoDate(it.date)?.let { (y, m, _) -> y to m } }.toSortedSet(
            compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second },
        )
        if (set.isEmpty()) listOf(2026 to 1) else set.toList()
    }

    // Default to the most recent month present in the data.
    var monthIndex by remember(months) { mutableStateOf(0) }
    val (year, month) = months[monthIndex.coerceIn(0, months.lastIndex)]

    VCard {
        // ── Header: month name + pager ──────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PagerArrow(
                icon = VIcons.ChevronLeft,
                enabled = monthIndex < months.lastIndex,
                onClick = { if (monthIndex < months.lastIndex) monthIndex++ },
            )
            Text(
                "${MONTH_LONG.getOrNull(month - 1) ?: "—"} $year",
                style = VTheme.type.bodyStrong.colored(c.ink),
            )
            PagerArrow(
                icon = VIcons.ChevronRight,
                enabled = monthIndex > 0,
                onClick = { if (monthIndex > 0) monthIndex-- },
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Weekday header row ──────────────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            WEEKDAY_SHORT.forEach { wd ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(wd, style = VTheme.type.caption.colored(c.ink3), textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Day grid ────────────────────────────────────────────────────────
        val firstWeekday = dayOfWeek(year, month, 1) // 0=Sun .. 6=Sat
        val totalDays = daysInMonth(year, month)
        val cells = firstWeekday + totalDays
        val rows = (cells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (dow in 0 until 7) {
                        val cellIndex = week * 7 + dow
                        val day = cellIndex - firstWeekday + 1
                        Box(Modifier.weight(1f)) {
                            if (day in 1..totalDays) {
                                val iso = isoOf(year, month, day)
                                DayCell(day = day, status = byDate[iso])
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Legend ──────────────────────────────────────────────────────────
        VLabel("Legend")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(c.success, "Present")
            LegendDot(c.teal, "Late")
            LegendDot(c.danger, "Absent")
        }
    }
}

@Composable
private fun PagerArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) c.cream else Color.Transparent)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) c.ink2 else c.placeholder,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DayCell(day: Int, status: String?) {
    val c = VTheme.colors
    val bg = when (status) {
        "present" -> c.success
        "late" -> c.teal
        "absent" -> c.danger
        else -> Color.Transparent
    }
    // High-contrast text when the cell is filled, ink otherwise.
    val fg = when (status) {
        null -> c.ink2
        else -> c.card
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (status == null) c.cream.copy(alpha = 0.4f) else bg)
            .border(
                1.dp,
                if (status == null) c.hairline else Color.Transparent,
                RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("$day", style = VTheme.type.caption.colored(fg))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val c = VTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = VTheme.type.caption.colored(c.ink2))
    }
}

// ── Local ISO date helpers (kotlinx-datetime-free, mirrors AcademicCalendarScreenV2) ─────

/** Parses "YYYY-MM-DD" into Triple(year, month, day). Returns null on bad input. */
private fun parseIsoDate(iso: String): Triple<Int, Int, Int>? {
    if (iso.length < 10) return null
    val y = iso.substring(0, 4).toIntOrNull() ?: return null
    val m = iso.substring(5, 7).toIntOrNull() ?: return null
    val d = iso.substring(8, 10).toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return Triple(y, m, d)
}

private fun isoOf(year: Int, month: Int, day: Int): String {
    val mm = month.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "$year-$mm-$dd"
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}

/**
 * Day of week for a given date using Sakamoto's algorithm.
 * Returns 0=Sunday .. 6=Saturday (matches [WEEKDAY_SHORT] ordering).
 */
private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month < 3) year - 1 else year
    return (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
}

private val WEEKDAY_SHORT = listOf("S", "M", "T", "W", "T", "F", "S")

private val MONTH_LONG = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
