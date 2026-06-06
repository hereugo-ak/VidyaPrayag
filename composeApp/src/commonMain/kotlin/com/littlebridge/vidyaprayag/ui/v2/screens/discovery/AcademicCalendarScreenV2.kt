package com.littlebridge.vidyaprayag.ui.v2.screens.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarEventDto
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicCalendarViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * AcademicCalendarScreenV2 — faithful Compose translation of `Discovery.tsx → AcademicCalendar`.
 *
 * Layout mirrors the React design exactly:
 *  - [VBackHeader] "Academic calendar"
 *  - month nav row (‹ prev · month label · next ›)
 *  - a 7-column month grid card; days that carry an event get an **arctic** (teal) dot
 *  - an "Upcoming events" list — each card: big day number + month abbrev, title, chevron
 *
 * Binds the real [AcademicCalendarViewModel] (`GET /api/v1/school/calendar`). The server payload has
 * no per-event *type* (academic / holiday / deadline) — that was mock data in the React file — so per
 * the hard UI rule we render every event with the single arctic accent and never fabricate types.
 * The dark variant maps to the NIGHT tone supplied by the host via [VTheme].
 */
@Composable
fun AcademicCalendarScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AcademicCalendarViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateV2()
    LaunchedEffect(Unit) { vm.loadCalendar() }

    val c = VTheme.colors
    val d = VTheme.dimens

    Column(modifier.fillMaxSize().background(c.background)) {
        VBackHeader(title = "Academic calendar", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.lg, vertical = d.md),
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            // ── Month navigation row ──────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MonthPill(text = "‹ Prev", onClick = { vm.goToPreviousMonth() })
                Text(
                    s.currentMonth.ifBlank { "—" },
                    style = VTheme.type.h3.colored(c.ink),
                )
                MonthPill(text = "Next ›", onClick = { vm.goToNextMonth() })
            }

            // ── Month grid ────────────────────────────────────────────────────────
            VCard(modifier = Modifier.fillMaxWidth()) {
                // weekday header
                Row(Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { wd ->
                        Text(
                            wd,
                            style = VTheme.type.label.colored(c.ink3),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(d.sm))

                // event-day lookup (parsed day-of-month → has event)
                val eventDays: Set<Int> = remember(s.calendarEvents) {
                    s.calendarEvents.mapNotNull { dayOfMonth(it.date) }.toSet()
                }
                // 30-cell month (matches React's fixed grid; real per-month length can be wired later)
                val rows = (1..30).chunked(7)
                rows.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val hasEvent = eventDays.contains(day)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (hasEvent) c.teal else c.background),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toString(),
                                    style = VTheme.type.caption.colored(if (hasEvent) c.navy else c.ink),
                                )
                            }
                        }
                        // pad short final week so cells stay square-aligned
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            // ── Upcoming events ─────────────────────────────────────────────────────
            VSectionHeader("UPCOMING EVENTS")

            when {
                s.isLoading && s.calendarEvents.isEmpty() ->
                    VEmptyState(title = "Loading calendar", icon = VIcons.Calendar)

                s.errorMessage != null && s.calendarEvents.isEmpty() ->
                    VEmptyState(title = "Couldn't load calendar", icon = VIcons.Calendar, body = s.errorMessage)

                s.calendarEvents.isEmpty() ->
                    VEmptyState(title = "No events scheduled", icon = VIcons.Calendar, body = "This month has no calendar entries yet.")

                else -> s.calendarEvents.forEach { event -> EventRow(event) }
            }

            Spacer(Modifier.height(d.lg))
        }
    }
}

@Composable
private fun MonthPill(text: String, onClick: () -> Unit) {
    val c = VTheme.colors
    Box(
        Modifier
            .clip(CircleShape)
            .background(c.cream)
            .clickable { onClick() }
            .padding(horizontal = VTheme.dimens.md, vertical = VTheme.dimens.xs),
    ) {
        Text(text, style = VTheme.type.caption.colored(c.ink2))
    }
}

@Composable
private fun EventRow(event: CalendarEventDto) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    (dayOfMonth(event.date)?.toString() ?: "—"),
                    style = VTheme.type.dataLg.colored(c.ink),
                )
                Text(
                    monthAbbrev(event.date),
                    style = VTheme.type.label.colored(c.ink3),
                )
            }
            Spacer(Modifier.width(VTheme.dimens.md))
            Column(Modifier.weight(1f)) {
                Text(event.eventTitle, style = VTheme.type.bodyStrong.colored(c.ink))
                if (event.eventDescription.isNotBlank()) {
                    Text(event.eventDescription, style = VTheme.type.caption.colored(c.ink3))
                }
            }
            Icon(
                VIcons.ChevronRight,
                contentDescription = null,
                tint = c.ink3,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Parses the day-of-month (1..31) out of an ISO `YYYY-MM-DD` date, or null if unparseable. */
private fun dayOfMonth(iso: String): Int? =
    iso.split("-").getOrNull(2)?.take(2)?.toIntOrNull()

/** Maps the ISO month component to a 3-letter uppercase abbreviation (e.g. "JUN"). */
private fun monthAbbrev(iso: String): String {
    val m = iso.split("-").getOrNull(1)?.toIntOrNull() ?: return "—"
    val names = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    return names.getOrNull(m - 1) ?: "—"
}
