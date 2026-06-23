package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.CalendarOverlayUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedDayUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.formatClock12h
import com.littlebridge.vidyaprayag.util.parseHourMinute

/**
 * TeacherScheduleCard — the teacher's live daily schedule, mirroring the parent
 * ParentScheduleCard's contained 3-face swipe (Doc 05 §5, Doc 10 §6.2) but fed by
 * the SERVER-RESOLVED day (T-104 `GET /teacher/day` + `/teacher/week`) surfaced as
 * [ResolvedDayUi] / [ResolvedPeriodUi] / [CalendarOverlayUi].
 *
 *   face 0 — NOW / NEXT (compact): the live verdict ("Teaching now" / "Covering now"
 *            / "Up next" / "Day complete") driven by the server's authoritative
 *            [ResolvedDayUi.nowIndex] / [ResolvedDayUi.nextIndex] — NOT the device
 *            clock (Doc 05 §6, "device clock wrong"). Carries pre-scoped CTAs
 *            ([Mark attendance] [Syllabus] [Homework]) which only appear when the
 *            focused period is one the teacher actually owns (assignmentId != null).
 *            Holiday short-circuits to a banner; an unseeded day to a "not set up" plate.
 *   face 1 — TODAY TIMELINE (expanded): every period on a connected rail, marked done
 *            (tick) / now (pulsing node) / upcoming, with cancelled struck-through,
 *            substitution / room-change / note meta, and an overlap warning chip.
 *   face 2 — WEEKLY (Mon–Sat): the resolved week grid, today highlighted, holidays
 *            badged, cancelled periods struck-through.
 *
 * LAW (carried from the parent card): the swipe is component-scoped (local [face]
 * state), never page nav. Everything shown is real, resolved server-side, and scoped
 * to the teacher's allocation — a period without an [ResolvedPeriodUi.assignmentId]
 * (e.g. a foreign overlap row) renders read-only with NO action CTAs.
 */
@Composable
fun TeacherScheduleCard(
    day: ResolvedDayUi,
    week: List<ResolvedDayUi>,
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasToday = day.periods.isNotEmpty()
    val hasWeek = week.any { it.periods.isNotEmpty() }
    // 0 = now/next (compact), 1 = today timeline (expanded), 2 = weekly grid.
    var face by remember { mutableStateOf(0) }
    val maxFace = when {
        hasToday && hasWeek -> 2
        hasToday || hasWeek -> 1
        else -> 0
    }

    val swipe = if (maxFace > 0) {
        Modifier.pointerInput(maxFace) {
            var dx = 0f
            detectHorizontalDragGestures(
                onDragStart = { dx = 0f },
                onDragEnd = {
                    if (dx <= -36f) face = (face + 1).coerceAtMost(maxFace)   // swipe left → expand
                    else if (dx >= 36f) face = (face - 1).coerceAtLeast(0)    // swipe right → collapse
                },
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
            label = "teacherScheduleFace",
        ) { f ->
            when (f) {
                0 -> NowNextFace(
                    day = day,
                    onExpand = if (maxFace > 0) ({ face = 1 }) else null,
                    expandLabel = if (hasToday) "Swipe to see today's full schedule" else "Swipe for the weekly timetable",
                    onMarkAttendance = onMarkAttendance,
                    onOpenSyllabus = onOpenSyllabus,
                    onOpenHomework = onOpenHomework,
                )
                1 -> if (hasToday) {
                    TodayTimelineFace(
                        day = day,
                        onCollapse = { face = 0 },
                        onExpand = if (hasWeek) ({ face = 2 }) else null,
                    )
                } else {
                    WeeklyFace(week = week, onCollapse = { face = 0 })
                }
                else -> WeeklyFace(
                    week = week,
                    onCollapse = { face = if (hasToday) 1 else 0 },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Face 0 — NOW / NEXT (compact)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The resting face: a single live verdict + pre-scoped CTAs for the focused period
 * + day-progress + calendar overlay chips. Holiday and unseeded days short-circuit
 * to their own distinct plates (Doc 10 §8 — honest, distinct states).
 */
@Composable
private fun NowNextFace(
    day: ResolvedDayUi,
    onExpand: (() -> Unit)?,
    expandLabel: String,
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
) {
    val c = VTheme.colors

    Column(Modifier.fillMaxWidth()) {
        // ── Eyebrow + expand chevron ──────────────────────────────────────────
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
                    Icon(VIcons.ChevronRight, contentDescription = "Expand schedule", tint = c.accentDeep, modifier = Modifier.size(15.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Holiday short-circuit — a CONTENT state, not "empty". ──────────────
        if (day.isHoliday) {
            HolidayPlate(name = day.holidayName)
            if (day.calendar.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                CalendarChips(day.calendar)
            }
            return
        }

        // ── Unseeded — genuinely no timetable for the day. ────────────────────
        if (day.periods.isEmpty()) {
            NotSetUpPlate()
            if (onExpand != null) {
                Spacer(Modifier.height(8.dp))
                SwipeHint(expandLabel)
            }
            return
        }

        // ── The focused period: server-authoritative now, else next. ──────────
        val total = day.periods.size
        val done = day.periods.indices.count { idx ->
            val ni = if (day.nowIndex >= 0) day.nowIndex else total
            idx < ni && !day.periods[idx].isCancelled
        }
        val nowPeriod = day.periods.getOrNull(day.nowIndex)
        val nextPeriod = day.periods.getOrNull(day.nextIndex)
        val focus = nowPeriod ?: nextPeriod

        val verdict = when {
            nowPeriod != null && nowPeriod.isSubstituteForMe -> Verdict(
                VIcons.ShieldCheck, c.accentDeep, c.accent.copy(alpha = 0.10f),
                "Covering now",
                "${nowPeriod.subject} · ${nowPeriod.classLabel}  ·  until ${clock(nowPeriod.endTime)}",
            )
            nowPeriod != null -> Verdict(
                VIcons.BookOpen, c.accentDeep, c.accent.copy(alpha = 0.10f),
                "Teaching now",
                "${nowPeriod.subject} · ${nowPeriod.classLabel}  ·  until ${clock(nowPeriod.endTime)}",
            )
            nextPeriod != null -> Verdict(
                VIcons.Clock, c.warningInk, c.warning.copy(alpha = 0.18f),
                "Up next",
                "${nextPeriod.subject} · ${nextPeriod.classLabel}  ·  at ${clock(nextPeriod.startTime)}",
            )
            else -> Verdict(
                VIcons.ShieldCheck, c.successInk, c.success.copy(alpha = 0.20f),
                "Day complete",
                "All of today's classes are done",
            )
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(verdict.heroBg).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(verdict.tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(verdict.icon, contentDescription = null, tint = verdict.tint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    verdict.headline,
                    style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                )
                Text(verdict.subline, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp), maxLines = 1)
            }
            if (focus != null && focus.assignmentId != null) {
                AttendanceBadge(marked = focus.attendanceMarked)
            }
        }

        // ── Pre-scoped CTAs — only when the focused period is genuinely ours. ──
        if (focus != null && focus.assignmentId != null && !focus.isCancelled) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopedCta(
                    icon = VIcons.ClipboardList,
                    label = if (focus.attendanceMarked) "Edit attendance" else "Mark attendance",
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkAttendance(focus) },
                )
                ScopedCta(
                    icon = VIcons.BookOpen,
                    label = "Syllabus",
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSyllabus(focus) },
                )
                ScopedCta(
                    icon = VIcons.FileText,
                    label = null, // icon-only
                    primary = false,
                    onClick = { onOpenHomework(focus) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        DayProgress(done = done, total = total)

        if (day.calendar.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            CalendarChips(day.calendar)
        }

        if (onExpand != null) {
            Spacer(Modifier.height(10.dp))
            SwipeHint(expandLabel)
        }
    }
}

/** A small immutable verdict tuple so the hero can be built cleanly. */
private data class Verdict(
    val icon: ImageVector,
    val tint: Color,
    val heroBg: Color,
    val headline: String,
    val subline: String,
)

/** The attendance state pill on the verdict hero — Marked (green) / Pending (amber). */
@Composable
private fun AttendanceBadge(marked: Boolean) {
    val c = VTheme.colors
    val (label, bg, fg, icon) = if (marked) {
        Quad("Marked", c.success.copy(alpha = 0.30f), c.successInk, VIcons.Check)
    } else {
        Quad("Pending", c.warning.copy(alpha = 0.22f), c.warningInk, VIcons.AlertTriangle)
    }
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(10.dp))
        Text(label, style = VTheme.type.label.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
    }
}

/** A tiny immutable 4-tuple for the attendance badge. */
private data class Quad(
    val label: String,
    val bg: Color,
    val fg: Color,
    val icon: ImageVector,
)

/**
 * A pre-scoped action button. Because the focused period already carries a
 * pre-authorized assignmentId, tapping needs no class picker — it deep-links
 * straight into the scoped action (Doc 04 §4, "attendance is an action").
 * [label] = null renders an icon-only square (for Homework).
 */
@Composable
private fun ScopedCta(
    icon: ImageVector,
    label: String?,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val bg = if (primary) c.accent else c.accent.copy(alpha = 0.10f)
    val fg = if (primary) Color.White else c.accentDeep
    val ix = remember { MutableInteractionSource() }
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .padding(horizontal = if (label == null) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (label == null) Arrangement.Center else Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(15.dp))
        if (label != null) {
            Text(label, style = VTheme.type.labelStrong.colored(fg).copy(fontSize = 11.sp), maxLines = 1)
        }
    }
}

/** A labelled day-progress bar: "N of M classes done" with an animated violet fill. */
@Composable
private fun DayProgress(done: Int, total: Int) {
    val c = VTheme.colors
    val ratio = if (total > 0) done.toFloat() / total else 0f
    val animated by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "teacherDayProgress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Today's progress",
                style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
            )
            Text(
                "$done of $total classes done",
                style = VTheme.type.caption.colored(c.navyDeep).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)).background(c.cream)) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(c.accentSoft, c.accent, c.accentDeep))),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Face 1 — TODAY TIMELINE (expanded)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayTimelineFace(
    day: ResolvedDayUi,
    onCollapse: () -> Unit,
    onExpand: (() -> Unit)?,
) {
    val c = VTheme.colors
    val total = day.periods.size
    val done = day.periods.indices.count { idx ->
        val ni = if (day.nowIndex >= 0) day.nowIndex else total
        idx < ni && !day.periods[idx].isCancelled
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackChip(onCollapse)
                Text("Today's schedule", style = VTheme.type.bodyStrong.colored(c.navyDeep))
            }
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(c.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    "$done / $total done",
                    style = VTheme.type.label.colored(c.accentDeep).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        day.periods.forEachIndexed { index, p ->
            TimelineRow(
                period = p,
                relation = when {
                    p.isCancelled -> 2          // 2 = cancelled (special)
                    day.nowIndex >= 0 && index == day.nowIndex -> 0  // now
                    day.nowIndex >= 0 && index < day.nowIndex -> -1  // done
                    day.nowIndex < 0 && day.nextIndex >= 0 && index < day.nextIndex -> -1 // pre-first: past slots done
                    else -> 1                   // upcoming
                },
                isFirst = index == 0,
                isLast = index == day.periods.lastIndex,
            )
        }

        if (day.calendar.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            CalendarChips(day.calendar)
        }

        if (onExpand != null) {
            Spacer(Modifier.height(8.dp))
            SwipeHint("Swipe for the weekly timetable")
        }
    }
}

/**
 * A single connected timeline row. [relation]: -1 done, 0 now, 1 upcoming, 2 cancelled.
 * Cancelled rows render with a struck-through subject and a muted node (no tick/pulse).
 */
@Composable
private fun TimelineRow(period: ResolvedPeriodUi, relation: Int, isFirst: Boolean, isLast: Boolean) {
    val c = VTheme.colors
    val isNow = relation == 0
    val isDone = relation == -1
    val isCancelled = relation == 2

    val nodeColor = when {
        isCancelled -> c.placeholder
        isDone -> c.successInk
        isNow -> c.accent
        else -> c.placeholder
    }
    val railColor = c.hairline
    val subjectColor = when {
        isCancelled -> c.ink3
        isDone -> c.ink3
        else -> c.navyDeep
    }
    val timeColor = when {
        isNow -> c.accentDeep
        isDone || isCancelled -> c.placeholder
        else -> c.ink2
    }

    val pulse = rememberInfiniteTransition(label = "teacherNowPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "teacherPulseScale",
    )

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // ── Rail + node column ────────────────────────────────────────────────
        Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(2.dp).height(8.dp).background(if (isFirst) Color.Transparent else railColor))
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (isNow) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = 0.25f }
                            .clip(CircleShape)
                            .background(c.accent),
                    )
                }
                Box(
                    Modifier.size(if (isNow) 18.dp else 16.dp).clip(CircleShape).background(nodeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isDone -> Icon(VIcons.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(11.dp))
                        isNow -> Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                        isCancelled -> Icon(VIcons.Close, contentDescription = "Cancelled", tint = Color.White, modifier = Modifier.size(10.dp))
                        else -> {}
                    }
                }
            }
            Box(Modifier.width(2.dp).height(if (isLast) 0.dp else 30.dp).background(if (isLast) Color.Transparent else railColor))
        }

        Spacer(Modifier.width(10.dp))

        // ── Period detail ─────────────────────────────────────────────────────
        Column(
            Modifier
                .weight(1f)
                .padding(top = 6.dp, bottom = 8.dp)
                .then(
                    if (isNow) Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.accent.copy(alpha = 0.07f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                    else Modifier,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${clock(period.startTime)} – ${clock(period.endTime)}",
                    style = VTheme.type.caption.colored(timeColor).copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                )
                PeriodStatusTag(relation = relation, isSub = period.isSubstituteForMe)
                if (period.hasOverlap) OverlapChip()
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${period.subject} · ${period.classLabel}",
                style = VTheme.type.bodyStrong.colored(subjectColor).copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null,
                ),
            )
            val meta = buildList {
                if (period.room.isNotBlank()) add("Room ${period.room}")
                if (period.isSubstituteForMe) add("Substitution")
                else if (!period.substituteTeacherName.isNullOrBlank()) add("Covered by ${period.substituteTeacherName}")
                if (period.note.isNotBlank()) add(period.note)
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
            }
            if (!isCancelled && period.assignmentId != null) {
                Spacer(Modifier.height(4.dp))
                AttendanceBadge(marked = period.attendanceMarked)
            }
        }
    }
}

/** Per-row status tag — Done / Now / Cover / Cancelled / Upcoming. COLOR IS SEMANTIC. */
@Composable
private fun PeriodStatusTag(relation: Int, isSub: Boolean) {
    val c = VTheme.colors
    val (label, bg, fg) = when {
        relation == 2 -> Triple("Cancelled", c.danger.copy(alpha = 0.18f), c.dangerInk)
        relation == -1 -> Triple("Done", c.success.copy(alpha = 0.35f), c.successInk)
        relation == 0 && isSub -> Triple("Cover", c.accent.copy(alpha = 0.18f), c.accentDeep)
        relation == 0 -> Triple("Now", c.accent.copy(alpha = 0.18f), c.accentDeep)
        else -> Triple("Upcoming", c.cream, c.ink3)
    }
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 7.dp, vertical = 1.dp)) {
        Text(label, style = VTheme.type.label.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
    }
}

/** A warning chip flagging a timetable overlap (two periods clashing). */
@Composable
private fun OverlapChip() {
    val c = VTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(c.warning.copy(alpha = 0.20f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.warningInk, modifier = Modifier.size(9.dp))
        Text("Overlap", style = VTheme.type.label.colored(c.warningInk).copy(fontWeight = FontWeight.Bold, fontSize = 8.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Face 2 — WEEKLY (Mon–Sat)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyFace(week: List<ResolvedDayUi>, onCollapse: () -> Unit) {
    val c = VTheme.colors
    // Mon–Sat only (weekday 1..6); sorted; today is detected by matching the day's
    // server weekday against the resolved "today" carried in the list (nowIndex/next
    // are per-day, so we mark today purely by structural position — the screen passes
    // a week whose entries each know their own weekday).
    val days = week.filter { it.weekday in 1..6 }.sortedBy { it.weekday }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackChip(onCollapse)
            Text("This week", style = VTheme.type.bodyStrong.colored(c.navyDeep))
            Spacer(Modifier.width(30.dp))
        }

        Spacer(Modifier.height(12.dp))

        if (days.isEmpty()) {
            Text("No schedule for this week yet", style = VTheme.type.caption.colored(c.placeholder).copy(fontSize = 11.sp))
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            days.forEach { d ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            weekdayName(d.weekday),
                            style = VTheme.type.labelStrong.colored(c.ink2).copy(fontSize = 11.sp),
                        )
                        if (d.isHoliday) {
                            Box(
                                Modifier.clip(RoundedCornerShape(999.dp)).background(c.danger.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    d.holidayName ?: "Holiday",
                                    style = VTheme.type.label.colored(c.dangerInk).copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    when {
                        d.isHoliday -> Text("Holiday — no classes", style = VTheme.type.caption.colored(c.placeholder).copy(fontSize = 11.sp))
                        d.periods.isEmpty() -> Text("No classes", style = VTheme.type.caption.colored(c.placeholder).copy(fontSize = 11.sp))
                        else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            d.periods.sortedBy { parseHourMinute(it.startTime) ?: 0 }.forEach { p ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        clock(p.startTime),
                                        style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp),
                                        modifier = Modifier.width(56.dp),
                                    )
                                    Text(
                                        "${p.subject} · ${p.classLabel}",
                                        style = VTheme.type.body.colored(if (p.isCancelled) c.ink3 else c.navyDeep).copy(
                                            fontSize = 12.sp,
                                            textDecoration = if (p.isCancelled) TextDecoration.LineThrough else null,
                                        ),
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

// ─────────────────────────────────────────────────────────────────────────────
//  Shared plates / chips / helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Calendar overlay chips — exams / events / holidays for the day (Doc 05 §5.3). */
@Composable
private fun CalendarChips(items: List<CalendarOverlayUi>) {
    val c = VTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "ON THE CALENDAR",
            style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items.forEach { e ->
                val (icon, fg) = when (e.type.uppercase()) {
                    "EXAM" -> VIcons.ClipboardList to c.dangerInk
                    "HOLIDAY" -> VIcons.Calendar to c.dangerInk
                    "EVENT" -> VIcons.Megaphone to c.accentDeep
                    else -> VIcons.Calendar to c.ink2
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.cream).padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
                    Text(e.title, style = VTheme.type.caption.colored(c.navyDeep).copy(fontSize = 11.sp), maxLines = 1)
                }
            }
        }
    }
}

/** The holiday content plate (distinct from "no timetable" — Doc 10 §8). */
@Composable
private fun HolidayPlate(name: String?) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.danger.copy(alpha = 0.10f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.danger.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VIcons.Calendar, contentDescription = null, tint = c.dangerInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                name?.takeIf { it.isNotBlank() } ?: "Holiday",
                style = VTheme.type.h3.colored(c.navyDeep).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            )
            Text(
                "School is closed today — no classes scheduled",
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }
    }
}

/** The "timetable not set up" plate (genuinely no periods, not a holiday). */
@Composable
private fun NotSetUpPlate() {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accent.copy(alpha = 0.07f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VIcons.Calendar, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "No classes today",
                style = VTheme.type.h3.colored(c.navyDeep).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            )
            Text(
                "Your timetable for today isn't set up yet",
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }
    }
}

/** A round back-chevron chip, shared by the expanded faces. */
@Composable
private fun BackChip(onClick: () -> Unit) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.10f))
            .clickable(interactionSource = ix, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(VIcons.ChevronLeft, contentDescription = "Back", tint = c.accentDeep, modifier = Modifier.size(15.dp))
    }
}

/** The consistent swipe-affordance hint line. */
@Composable
private fun SwipeHint(text: String) {
    val c = VTheme.colors
    Text(
        text,
        style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp, letterSpacing = 0.4.sp),
    )
}

/** Format an "HH:mm" wire time to a friendly 12h clock, falling back to the raw string. */
private fun clock(hm: String): String = parseHourMinute(hm)?.let { formatClock12h(it) } ?: hm

private fun weekdayName(weekday: Int): String = when (weekday) {
    1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
    5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"; else -> "—"
}
