package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentTimetableData
import com.littlebridge.vidyaprayag.feature.parent.presentation.LivePeriod
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.formatClock12h
import com.littlebridge.vidyaprayag.util.parseHourMinute
import com.littlebridge.vidyaprayag.util.todayWeekday

/**
 * Today's schedule card — the child's live class timetable for today, with a contained
 * swipe that transforms the same card into the full weekly timetable (and back).
 *
 * Live: each period carries a [LivePeriod.relation] (-1 finished, 0 now, 1 upcoming),
 * re-derived from the device clock by the ViewModel's per-minute refresh, so the
 * "now" highlight and the finished dimming update through the school day on their own.
 *
 * LAW: the swipe is component-scoped (local [face] state), never page nav; every period
 * is real (from the /timetable endpoint reading the canonical TeacherPeriodsTable).
 */
@Composable
fun ParentScheduleCard(
    todayPeriods: List<LivePeriod>,
    timetable: ParentTimetableData?,
    modifier: Modifier = Modifier,
) {
    val hasWeek = timetable != null && timetable.weekdays.any { it.periods.isNotEmpty() }
    var face by remember { mutableStateOf(0) } // 0 = today, 1 = week

    val swipe = if (hasWeek) {
        Modifier.pointerInput(Unit) {
            var dx = 0f
            detectHorizontalDragGestures(
                onDragStart = { dx = 0f },
                onDragEnd = { if (dx <= -36f) face = 1 else if (dx >= 36f) face = 0 },
            ) { _, delta -> dx += delta }
        }
    } else Modifier

    VCard(modifier = modifier.then(swipe), padding = 14.dp) {
        AnimatedContent(
            targetState = face,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(160)))
                } else {
                    (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(160)))
                }
            },
            label = "scheduleFace",
        ) { f ->
            if (f == 0) {
                TodayScheduleFace(
                    periods = todayPeriods,
                    onExpand = if (hasWeek) ({ face = 1 }) else null,
                )
            } else {
                WeeklyTimetableFace(timetable = timetable, onCollapse = { face = 0 })
            }
        }
    }
}

@Composable
private fun TodayScheduleFace(periods: List<LivePeriod>, onExpand: (() -> Unit)?) {
    val c = VTheme.colors

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Clock, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(14.dp))
                Text(
                    "TODAY'S SCHEDULE",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                )
            }
            if (onExpand != null) {
                val ix = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.1f))
                        .clickable(interactionSource = ix, indication = null) { onExpand() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Calendar, contentDescription = "Weekly timetable", tint = c.accentDeep, modifier = Modifier.size(15.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (periods.isEmpty()) {
            Text("No classes scheduled today", style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontSize = 13.sp))
            Text("Enjoy the day off", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { p -> PeriodRow(p) }
            }
            if (onExpand != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Swipe for the weekly timetable",
                    style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp, letterSpacing = 0.4.sp),
                )
            }
        }
    }
}

/** A single timetable period row, styled by its live relation to the clock. */
@Composable
private fun PeriodRow(p: LivePeriod) {
    val c = VTheme.colors
    val isNow = p.relation == 0
    val isFinished = p.relation == -1

    val timeColor = if (isFinished) c.placeholder else if (isNow) c.accentDeep else c.ink2
    val subjectColor = if (isFinished) c.ink3 else c.navyDeep
    val rowBg = if (isNow) c.accent.copy(alpha = 0.08f) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBg)
            .padding(horizontal = if (isNow) 8.dp else 0.dp, vertical = if (isNow) 6.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // time column
        Column(Modifier.width(58.dp)) {
            Text(
                formatClockSafe(p.startTime),
                style = VTheme.type.caption.colored(timeColor).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
            Text(
                formatClockSafe(p.endTime),
                style = VTheme.type.caption.colored(c.placeholder).copy(fontSize = 9.5.sp),
            )
        }
        // status rail
        VStatusDot(
            // RA-PP-THEME: on-palette schedule rail — now = strong violet accent,
            // upcoming = soft violet, finished = muted (was green teal for upcoming).
            color = if (isNow) c.accent else if (isFinished) c.placeholder else c.accentSoft,
            size = 7.dp,
        )
        // subject + meta
        Column(Modifier.weight(1f)) {
            Text(
                p.subject,
                style = VTheme.type.bodyStrong.colored(subjectColor).copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            )
            val meta = listOfNotNull(
                p.teacherName.takeIf { it.isNotBlank() },
                p.room.takeIf { it.isNotBlank() }?.let { "Room $it" },
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
            }
        }
        if (isNow) {
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(c.accent.copy(alpha = 0.16f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text("Now", style = VTheme.type.label.colored(c.accentDeep).copy(fontWeight = FontWeight.Bold, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun WeeklyTimetableFace(timetable: ParentTimetableData?, onCollapse: () -> Unit) {
    val c = VTheme.colors
    val today = todayWeekday()
    val weekdays = timetable?.weekdays.orEmpty().sortedBy { it.weekday }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val ix = remember { MutableInteractionSource() }
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.1f))
                    .clickable(interactionSource = ix, indication = null) { onCollapse() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ChevronLeft, contentDescription = "Back", tint = c.accentDeep, modifier = Modifier.size(15.dp))
            }
            Text("Weekly timetable", style = VTheme.type.bodyStrong.colored(c.navyDeep))
            Spacer(Modifier.width(30.dp))
        }

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            weekdays.forEach { day ->
                val isToday = day.weekday == today
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            weekdayName(day.weekday),
                            style = VTheme.type.labelStrong.colored(if (isToday) c.accentDeep else c.ink2).copy(fontSize = 11.sp),
                        )
                        if (isToday) {
                            Box(
                                Modifier.clip(RoundedCornerShape(999.dp)).background(c.accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text("Today", style = VTheme.type.label.colored(c.accentDeep).copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (day.periods.isEmpty()) {
                        Text("No classes", style = VTheme.type.caption.colored(c.placeholder).copy(fontSize = 11.sp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            day.periods.sortedBy { parseHourMinute(it.startTime) ?: 0 }.forEach { p ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        formatClockSafe(p.startTime),
                                        style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp),
                                        modifier = Modifier.width(56.dp),
                                    )
                                    Text(
                                        p.subject,
                                        style = VTheme.type.body.colored(c.navyDeep).copy(fontSize = 12.sp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Format an "HH:mm" wire time to a friendly 12h clock, falling back to the raw string. */
private fun formatClockSafe(hm: String): String =
    parseHourMinute(hm)?.let { formatClock12h(it) } ?: hm

private fun weekdayName(weekday: Int): String = when (weekday) {
    1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
    5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"; else -> "—"
}
