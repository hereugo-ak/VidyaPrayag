package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.dayOfWeek
import com.littlebridge.vidyaprayag.util.daysInMonth
import com.littlebridge.vidyaprayag.util.isoOf
import com.littlebridge.vidyaprayag.util.parseIsoDate
import com.littlebridge.vidyaprayag.util.todayIso

// ─────────────────────────────────────────────────────────────────────────────
// P4-T1 — Week View Header
//
// The Planner's signature week strip: 7 columns Mon–Sun. Each column shows a day
// abbreviation (LabelCaps, TextTertiary) over the date number; TODAY gets a filled
// PrimaryIndigo circle behind the number (white text); days carrying events show a
// small AccentAmber dot below the number. Tapping a day selects it (host scrolls
// the plan list to that date). Previous/next week is a left/right swipe on the
// strip itself, driven by a [HorizontalPager] — no chevrons, no menu.
//
// All visuals resolve through the Enroll.* bridge → VTheme (the violet `primary`
// family stands in for the loop's PrimaryIndigo, the warning family for AccentAmber
// — no new hex, full Parents↔Teacher parity per the iteration IMPORTANT NOTE).
//
// Date math is string-first ("YYYY-MM-DD") to match the rest of the app
// (DateUtil / VDatePicker), so this composable is platform-agnostic and needs no
// kotlinx-datetime.
// ─────────────────────────────────────────────────────────────────────────────

/** A single day cell in the week strip. `iso` is the canonical "YYYY-MM-DD" key. */
data class PlannerWeekDay(
    val iso: String,            // "2026-06-22"
    val dayLabel: String,       // "MON"
    val dayNumber: Int,         // 22
    val isToday: Boolean,
    val hasEvents: Boolean,     // exam / lesson / homework on this day → amber dot
)

/** Mon-first weekday abbreviations, indexed 0=Mon … 6=Sun. */
private val WEEK_LABELS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

// A generous symmetric window of weeks so the pager can swipe far in either
// direction; the centre page is "this week".
private const val WEEK_PAGE_COUNT = 105   // ≈ ±1 year of weeks
private const val WEEK_PAGE_CENTER = WEEK_PAGE_COUNT / 2

/**
 * WeekViewHeader — the swipeable 7-day strip atop the Planner.
 *
 * @param selectedIso    the currently selected day ("YYYY-MM-DD"); its column is ringed.
 * @param eventDays      the set of ISO dates that carry an event (exam/lesson/HW) →
 *                       these render the amber dot. Sourced from the Planner VM.
 * @param onSelectDay    a day was tapped → host scrolls the plan list to it.
 * @param onWeekChanged  the visible week changed (swipe) → host can prefetch that
 *                       week's schedule. Receives the Monday ISO of the new week.
 */
@Composable
fun WeekViewHeader(
    selectedIso: String,
    eventDays: Set<String>,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    onWeekChanged: (mondayIso: String) -> Unit = {},
) {
    val todayIso = remember { todayIso() }
    // Monday of the current real week — page 0 anchor for the pager arithmetic.
    val thisMonday = remember(todayIso) { mondayOf(todayIso) }

    val pagerState = rememberPagerState(initialPage = WEEK_PAGE_CENTER) { WEEK_PAGE_COUNT }

    // Tell the host which week is now visible (for prefetching that week's data).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val monday = addDays(thisMonday, (page - WEEK_PAGE_CENTER) * 7)
            onWeekChanged(monday)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceBase)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.md),
    ) { page ->
        val monday = addDays(thisMonday, (page - WEEK_PAGE_CENTER) * 7)
        val days = remember(monday, todayIso, eventDays) {
            buildWeek(monday, todayIso, eventDays)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Enroll.space.xs),
        ) {
            days.forEach { day ->
                DayColumn(
                    day = day,
                    selected = day.iso == selectedIso,
                    onClick = { onSelectDay(day.iso) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    day: PlannerWeekDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ix = remember { MutableInteractionSource() }

    // The date "puck" — TODAY is a solid primary disc; a selected (non-today) day
    // gets a soft primary tint so the user can see where the list is scrolled to.
    val puckBg by animateColorAsState(
        targetValue = when {
            day.isToday -> Enroll.colors.primary
            selected -> Enroll.colors.primarySoft
            else -> Color.Transparent
        },
        label = "puckBg",
    )
    val numberInk = when {
        day.isToday -> Color.White
        selected -> Enroll.colors.primary
        else -> Enroll.colors.textPrimary
    }

    Column(
        modifier = modifier
            .clip(Enroll.shape.card)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(vertical = Enroll.space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.dayLabel,
            style = Enroll.type.labelCaps.colored(Enroll.colors.textTertiary),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Enroll.space.sm))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(Enroll.shape.pill)
                .background(puckBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayNumber.toString(),
                style = Enroll.type.labelBold.colored(numberInk),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Enroll.space.xs))
        // Event marker — a small amber dot, or an invisible spacer of the same size
        // so every column keeps an identical height (no jitter between rows).
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(Enroll.shape.pill)
                .background(if (day.hasEvents) Enroll.colors.accent else Color.Transparent),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date math — string-first ("YYYY-MM-DD"), built on the shared DateUtil helpers.
// ─────────────────────────────────────────────────────────────────────────────

/** Builds the 7 [PlannerWeekDay]s for the week starting on [mondayIso]. */
private fun buildWeek(
    mondayIso: String,
    todayIso: String,
    eventDays: Set<String>,
): List<PlannerWeekDay> = (0 until 7).map { offset ->
    val iso = addDays(mondayIso, offset)
    val (_, _, d) = parseIsoDate(iso) ?: Triple(0, 0, 0)
    PlannerWeekDay(
        iso = iso,
        dayLabel = WEEK_LABELS[offset],
        dayNumber = d,
        isToday = iso == todayIso,
        hasEvents = iso in eventDays,
    )
}

/** The Monday (ISO) of the week that contains [iso]. */
private fun mondayOf(iso: String): String {
    val (y, m, d) = parseIsoDate(iso) ?: return iso
    // dayOfWeek: 0=Sun..6=Sat → Mon-first offset 0..6 (Mon=0 … Sun=6).
    val dow = dayOfWeek(y, m, d)
    val monOffset = if (dow == 0) 6 else dow - 1
    return addDays(iso, -monOffset)
}

/** [iso] shifted by [delta] days (positive or negative), via plain Gregorian rollover. */
private fun addDays(iso: String, delta: Int): String {
    var (y, m, d) = parseIsoDate(iso) ?: return iso
    var remaining = delta
    while (remaining > 0) {
        val dim = daysInMonth(y, m)
        if (d < dim) { d++ } else { d = 1; if (m == 12) { m = 1; y++ } else m++ }
        remaining--
    }
    while (remaining < 0) {
        if (d > 1) { d-- } else {
            if (m == 1) { m = 12; y-- } else m--
            d = daysInMonth(y, m)
        }
        remaining++
    }
    return isoOf(y, m, d)
}
