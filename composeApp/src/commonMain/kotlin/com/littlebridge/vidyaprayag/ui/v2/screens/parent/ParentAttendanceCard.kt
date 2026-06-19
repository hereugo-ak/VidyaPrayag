package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentAttendanceData
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentHolidayDto
import com.littlebridge.vidyaprayag.feature.parent.presentation.AttendanceDayState
import com.littlebridge.vidyaprayag.feature.parent.presentation.TodayAttendance
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.MONTH_LONG
import com.littlebridge.vidyaprayag.util.WEEKDAY_SHORT
import com.littlebridge.vidyaprayag.util.dayOfWeek
import com.littlebridge.vidyaprayag.util.daysInMonth
import com.littlebridge.vidyaprayag.util.isoOf
import com.littlebridge.vidyaprayag.util.parseIsoDate

/**
 * The dashboard's primary feature card — today's attendance verdict + this month's
 * attendance ring, with a **contained swipe gesture** that transforms the very same
 * card into a full month calendar (and back), expanding it vertically so the cards
 * below reflow fluidly.
 *
 * A faithful port of the website reference's hero attendance card, then elevated with
 * the engineered interaction the brief calls for:
 *   - swipe LEFT within the card → flips to the month calendar (the card grows tall,
 *     cards below slide down via the parent's spacedBy + this card's animateContentSize)
 *   - swipe RIGHT (or tap the back chevron) → flips back to the today summary
 *
 * LAW: the swipe is component-scoped (local [face] state) — it never navigates the page.
 * Every state handled; every number from the real backend.
 */
@Composable
fun ParentAttendanceCard(
    today: TodayAttendance,
    attendance: ParentAttendanceData?,
    modifier: Modifier = Modifier,
) {
    // 0 = today summary (front), 1 = month calendar (back). Component-scoped.
    var face by remember { mutableStateOf(0) }
    val hasMonth = attendance != null && attendance.records.isNotEmpty()

    // A horizontal-drag detector scoped to the card. A decisive swipe flips the face;
    // small drags are ignored so vertical scroll still wins.
    val swipe = if (hasMonth) {
        Modifier.pointerInput(Unit) {
            var dx = 0f
            detectHorizontalDragGestures(
                onDragStart = { dx = 0f },
                onDragEnd = {
                    if (dx <= -36f) face = 1   // swipe left → calendar
                    else if (dx >= 36f) face = 0 // swipe right → today
                },
            ) { _, delta -> dx += delta }
        }
    } else Modifier

    VCard(modifier = modifier.then(swipe), padding = 14.dp) {
        // animateContentSize on the card's content gives the fluid vertical expand +
        // reflow: when the calendar face mounts, the card grows and the dashboard's
        // spacedBy column pushes the cards below down smoothly.
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
            label = "attendanceFace",
        ) { f ->
            if (f == 0) {
                TodayFace(
                    today = today,
                    attendance = attendance,
                    onExpand = if (hasMonth) ({ face = 1 }) else null,
                )
            } else {
                CalendarFace(attendance = attendance, onCollapse = { face = 0 })
            }
        }
    }
}

/**
 * Front face — the today verdict + month ring. ALWAYS renders a rich, premium body:
 * a tinted status hero (icon + headline + subline) and a stat band. When the month has real
 * records the band is the live attendance ring + breakdown; when it's empty the band becomes a
 * polished "tracking from today" placeholder — never a collapsed, near-blank card.
 */
@Composable
private fun TodayFace(
    today: TodayAttendance,
    attendance: ParentAttendanceData?,
    onExpand: (() -> Unit)?,
) {
    val c = VTheme.colors
    val tone = toneFor(today.state)
    val hasMonth = attendance != null && attendance.totalDays > 0

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VStatusDot(color = tone.dot, size = 6.dp)
                Text(
                    "ATTENDANCE · TODAY",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                )
            }
            StatePill(label = tone.badge, bg = tone.badgeBg, fg = tone.badgeFg)
        }

        Spacer(Modifier.height(12.dp))

        // ── Status hero — a tinted plate with an icon + verdict, always present. ──
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tone.heroBg)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(tone.dot.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tone.icon, contentDescription = null, tint = tone.dot, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    tone.headline,
                    style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                )
                if (tone.subline.isNotBlank()) {
                    Text(tone.subline, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Stat band — ring + breakdown when there's a month; polished placeholder when not. ──
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (hasMonth) {
                AttendanceRing(percent = attendance!!.attendanceRate.coerceIn(0, 100), modifier = Modifier.size(56.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "This month",
                        style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                    )
                    Text(
                        "${attendance.attendanceRate}% present",
                        style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(monthBreakdown(attendance), style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
                }
            } else {
                // Empty month → an intentional, on-brand placeholder ring (dashed track) + copy.
                EmptyRing(modifier = Modifier.size(56.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "This month",
                        style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                    )
                    Text(
                        "Tracking from today",
                        style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "The month fills in as the class is marked",
                        style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp),
                    )
                }
            }

            if (onExpand != null) {
                val ix = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.1f))
                        .clickable(interactionSource = ix, indication = null) { onExpand() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Calendar, contentDescription = "Open calendar", tint = c.accentDeep, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (onExpand != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Swipe for the month calendar",
                style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp, letterSpacing = 0.4.sp),
            )
        }
    }
}

/** A soft dashed placeholder ring with a centred dash — used when the month has no records yet. */
@Composable
private fun EmptyRing(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            // dashed track via a series of short arcs
            val gaps = 16
            val seg = 360f / gaps
            for (i in 0 until gaps step 2) {
                drawArc(
                    color = c.accent.copy(alpha = 0.2f),
                    startAngle = -90f + i * seg,
                    sweepAngle = seg * 0.7f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text("—", style = VTheme.type.dataSm.colored(c.accentDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp))
    }
}

/**
 * Back face — the month calendar grid with month navigation, colour-coded by the real
 * records + holidays, with a back chevron to collapse to today.
 */
@Composable
private fun CalendarFace(attendance: ParentAttendanceData?, onCollapse: () -> Unit) {
    val c = VTheme.colors
    val records = attendance?.records.orEmpty()
    val holidays = attendance?.holidays.orEmpty()

    val byDate = remember(records) { records.associate { it.date to it.status.lowercase() } }
    val months = remember(records) {
        val set = records.mapNotNull { parseIsoDate(it.date)?.let { (y, m, _) -> y to m } }.toSortedSet(
            compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second },
        )
        if (set.isEmpty()) listOf(2026 to 1) else set.toList()
    }
    var monthIndex by remember(months) { mutableStateOf(0) }
    val (year, month) = months[monthIndex.coerceIn(0, months.lastIndex)]

    Column(Modifier.fillMaxWidth()) {
        // header: back chevron + month + pager
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleIcon(VIcons.ChevronLeft, enabled = true, tint = c.accentDeep, bg = c.accent.copy(alpha = 0.1f), onClick = onCollapse)
            Text("${MONTH_LONG.getOrNull(month - 1) ?: "—"} $year", style = VTheme.type.bodyStrong.colored(c.navyDeep))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CircleIcon(VIcons.ChevronLeft, monthIndex < months.lastIndex, c.ink2, c.cream) { if (monthIndex < months.lastIndex) monthIndex++ }
                CircleIcon(VIcons.ChevronRight, monthIndex > 0, c.ink2, c.cream) { if (monthIndex > 0) monthIndex-- }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            WEEKDAY_SHORT.forEach { wd ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(wd, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp), textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val firstWeekday = dayOfWeek(year, month, 1)
        val totalDays = daysInMonth(year, month)
        val rows = (firstWeekday + totalDays + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (dow in 0 until 7) {
                        val day = week * 7 + dow - firstWeekday + 1
                        Box(Modifier.weight(1f)) {
                            if (day in 1..totalDays) {
                                val iso = isoOf(year, month, day)
                                DayCell(
                                    day = day,
                                    status = byDate[iso],
                                    isHoliday = isHolidayCell(iso, year, month, day, holidays),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(c.successInk, "Present")
            LegendDot(c.warningInk, "Late")
            LegendDot(c.dangerInk, "Absent")
            LegendDot(c.accentDeep, "Holiday")
        }
    }
}

/** True when [iso] is a declared holiday/vacation (dated or weekly-recurring). */
private fun isHolidayCell(iso: String, year: Int, month: Int, day: Int, holidays: List<ParentHolidayDto>): Boolean {
    if (holidays.any { it.date == iso }) return true
    val dow = dayOfWeek(year, month, day) // 0=Sun..6=Sat
    val name = when (dow) {
        0 -> "sunday"; 1 -> "monday"; 2 -> "tuesday"; 3 -> "wednesday"
        4 -> "thursday"; 5 -> "friday"; 6 -> "saturday"; else -> ""
    }
    return holidays.any { it.frequency.equals("weekly", true) && it.title.contains(name, true) }
}

@Composable
private fun DayCell(day: Int, status: String?, isHoliday: Boolean) {
    val c = VTheme.colors
    val (bg, fg) = when (status) {
        "present" -> c.successInk to c.card
        "late" -> c.warningInk to c.card
        "absent" -> c.dangerInk to c.card
        else -> if (isHoliday) c.accent.copy(alpha = 0.16f) to c.accentDeep else c.cream.copy(alpha = 0.4f) to c.ink2
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(1.dp, if (status == null && !isHoliday) c.hairline else Color.Transparent, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("$day", style = VTheme.type.caption.colored(fg).copy(fontSize = 10.sp))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val c = VTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 10.sp))
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (enabled) bg else Color.Transparent)
            .then(if (enabled) Modifier.clickable(interactionSource = ix, indication = null, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else c.placeholder, modifier = Modifier.size(15.dp))
    }
}

/** A small rounded pill rendering the state badge. */
@Composable
private fun StatePill(label: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(label, style = VTheme.type.label.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
    }
}

/**
 * The month attendance ring — a violet sweep over a soft track, percent centred,
 * animated in with a spring so the card feels alive on load.
 */
@Composable
private fun AttendanceRing(percent: Int, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val sweep by animateFloatAsState(targetValue = percent / 100f, label = "attendanceSweep")
    // RA-PP-THEME: on-palette violet sweep matching the reference ring (was violet→teal).
    val gradient = Brush.linearGradient(listOf(c.accentSoft, c.accent))
    val trackColor = c.accent.copy(alpha = 0.12f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$percent%",
            style = VTheme.type.dataSm.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
        )
    }
}

/** A real, honest month breakdown line built from the backend counts. */
private fun monthBreakdown(a: ParentAttendanceData): String {
    val attended = a.presentDays + a.lateDays
    val parts = buildList {
        add("$attended of ${a.totalDays} school days")
        if (a.lateDays > 0) add("${a.lateDays} late")
        if (a.absentDays > 0) add("${a.absentDays} absent")
    }
    return parts.joinToString(" · ")
}

/** Visual + copy tokens for one resolved today-state — drives the whole card face. */
private data class AttendanceTone(
    val badge: String,
    val headline: String,
    val subline: String,
    val dot: Color,
    val badgeBg: Color,
    val badgeFg: Color,
    val heroBg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun toneFor(state: AttendanceDayState): AttendanceTone {
    val c = VTheme.colors
    return when (state) {
        AttendanceDayState.Present -> AttendanceTone(
            "Present", "Marked present today", "Your child is in school",
            c.successInk, c.success.copy(alpha = 0.42f), c.successInk,
            c.success.copy(alpha = 0.18f), VIcons.ShieldCheck,
        )
        AttendanceDayState.Late -> AttendanceTone(
            "Late", "Arrived late today", "Marked present, after the bell",
            c.warningInk, c.warning.copy(alpha = 0.55f), c.warningInk,
            c.warning.copy(alpha = 0.28f), VIcons.Clock,
        )
        AttendanceDayState.Absent -> AttendanceTone(
            "Absent", "Marked absent today", "No attendance recorded for today",
            c.dangerInk, c.danger.copy(alpha = 0.55f), c.dangerInk,
            c.danger.copy(alpha = 0.28f), VIcons.AlertCircle,
        )
        AttendanceDayState.Holiday -> AttendanceTone(
            "Holiday", "School holiday today", "Enjoy the day off",
            c.accentDeep, c.accent.copy(alpha = 0.14f), c.accentDeep,
            c.accent.copy(alpha = 0.1f), VIcons.Calendar,
        )
        AttendanceDayState.Vacation -> AttendanceTone(
            "Break", "On vacation", "Enjoy the break",
            c.accentDeep, c.accent.copy(alpha = 0.14f), c.accentDeep,
            c.accent.copy(alpha = 0.1f), VIcons.Sparkles,
        )
        AttendanceDayState.Sunday -> AttendanceTone(
            "Sunday", "No school today", "It's a Sunday",
            c.ink3, c.cream, c.ink2,
            c.cream, VIcons.Calendar,
        )
        AttendanceDayState.NoData -> AttendanceTone(
            "Awaiting", "Attendance not marked yet", "You'll see today's status once the class is marked",
            c.accentDeep, c.accent.copy(alpha = 0.12f), c.accentDeep,
            c.accent.copy(alpha = 0.08f), VIcons.Clock,
        )
    }
}
