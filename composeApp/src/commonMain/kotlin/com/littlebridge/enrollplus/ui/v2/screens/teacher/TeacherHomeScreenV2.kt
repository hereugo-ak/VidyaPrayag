package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.enrollplus.feature.teacher.presentation.ResolvedDayUi
import com.littlebridge.enrollplus.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherCheckInState
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherCheckInViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherObligationsState
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherObligationsViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherTodayState
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.enrollplus.platform.BiometricMethod
import com.littlebridge.enrollplus.ui.v2.components.VActionCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.util.todayIso
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherHomeScreenV2 — the rebuilt-from-scratch Home tab, built on the Parents Portal's design
 * language (lavender canvas, white rounded cards, Canvas rings, the signature swipe-to-expand
 * cards) and the teacher's REAL data spine (Today / CheckIn / Obligations view-models).
 *
 * It deliberately replaces the cluttered "20-class list": today's classes are CLUBBED into a single
 * swipe-expand attendance card (face 0 = the day's progress ring + clubbed metrics; face 1 = the
 * per-class list). The greeting hero carries the time-sensitive greeting + a one-tap check-in ring.
 * A first-login-of-day check-in popup appears over this screen (closeable). Below, today's schedule
 * and the "what needs me" reminders are each their own card. Every number is server-authoritative.
 */
@Composable
fun TeacherHomeScreenV2(
    onOpenAttendanceForAssignment: (assignmentId: String, scope: String) -> Unit,
    onOpenUpdateTab: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenLessonPlanForAssignment: (assignmentId: String, scope: String) -> Unit = { _, _ -> },
    onOpenHealthAlerts: () -> Unit = {},
    onOpenTransportAttendance: () -> Unit = {},
    onOpenPews: () -> Unit = {},
    onOpenReportReview: () -> Unit = {},
    modifier: Modifier = Modifier,
    todayViewModel: TeacherTodayViewModel = koinViewModel(),
    checkInViewModel: TeacherCheckInViewModel = koinViewModel(),
    obligationsViewModel: TeacherObligationsViewModel = koinViewModel(),
) {
    val today by todayViewModel.state.collectAsStateV2()
    val checkIn by checkInViewModel.state.collectAsStateV2()
    val obligations by obligationsViewModel.state.collectAsStateV2()

    // First-login-of-day popup gate: show once per day, tracked in saveable state. It pops only when
    // the status has resolved as "not checked in" and the teacher hasn't dismissed it this session.
    var popupDismissedForDate by rememberSaveable { mutableStateOf<String?>(null) }
    val popupVisible = !checkIn.isLoading &&
        !checkIn.statusUnavailable &&
        !checkIn.checkedIn &&
        checkIn.date.isNotBlank() &&
        popupDismissedForDate != checkIn.date

    Box(modifier.fillMaxSize().background(VTheme.colors.background)) {
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GreetingHeroCard(
                teacherName = today.teacherName,
                checkIn = checkIn,
                obligations = obligations,
                onCheckIn = { method -> checkInViewModel.checkIn(method) },
            )

            AttendanceSummaryCard(
                today = today,
                obligations = obligations,
                onOpenAttendance = onOpenAttendanceForAssignment,
                onOpenUpdate = onOpenUpdateTab,
            )

            ScheduleCard(today = today, onOpenLessonPlan = onOpenLessonPlanForAssignment)

            RemindersCard(
                obligations = obligations,
                onOpenUpdate = onOpenUpdateTab,
                onOpenClasses = onOpenClasses,
            )

            VActionCard(
                title = "Needs Attention",
                subtitle = "Students in your classes the early-warning system has flagged",
                icon = VIcons.AlertTriangle,
                onClick = onOpenPews,
            )

            VActionCard(
                title = "Report Card Review",
                subtitle = "Review and approve AI-generated report card drafts for your classes",
                icon = VIcons.FileText,
                onClick = onOpenReportReview,
            )

            VActionCard(
                title = "Health Alerts",
                subtitle = "Allergies & conditions for students in your classes",
                icon = VIcons.Heart,
                onClick = onOpenHealthAlerts,
            )

            VActionCard(
                title = "Transport Attendance",
                subtitle = "Mark pickup & drop for students on your bus route",
                icon = VIcons.MapPin,
                onClick = onOpenTransportAttendance,
            )
        }

        // The first-login fingerprint check-in popup rides above everything.
        TeacherCheckInPopup(
            state = checkIn,
            visible = popupVisible,
            onDismiss = { popupDismissedForDate = checkIn.date.ifBlank { todayIso() } },
            onCheckIn = { method -> checkInViewModel.checkIn(method) },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Greeting hero — time-sensitive greeting + a one-tap check-in ring.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GreetingHeroCard(
    teacherName: String,
    checkIn: TeacherCheckInState,
    obligations: TeacherObligationsState,
    onCheckIn: (method: String) -> Unit,
) {
    val c = VTheme.colors
    val name = teacherName.trim().substringBefore(" ").ifBlank { "Teacher" }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(c.accent.copy(alpha = 0.10f), c.accentSoft.copy(alpha = 0.05f), c.card),
                ),
            )
            .border(1.dp, c.hairline, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TEyebrow(teacherGreeting().uppercase(), dot = c.accent)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Hi, $name",
                    style = VTheme.type.h1.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
                )
                Spacer(Modifier.height(4.dp))
                val line = when {
                    obligations.isAllCaughtUp -> "You're all caught up — have a great day."
                    obligations.totalOutstanding > 0 -> "${obligations.totalOutstanding} thing${if (obligations.totalOutstanding == 1) "" else "s"} need your attention."
                    else -> "Here's your day at a glance."
                }
                Text(line, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.5.sp))
            }
            Spacer(Modifier.width(12.dp))
            CheckInRing(checkIn = checkIn, onCheckIn = onCheckIn)
        }
    }
}

@Composable
private fun CheckInRing(checkIn: TeacherCheckInState, onCheckIn: (method: String) -> Unit) {
    val c = VTheme.colors
    val checkedIn = checkIn.checkedIn
    val accent = if (checkedIn) c.successInk else c.warningInk
    val ix = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(78.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .clickable(
                    interactionSource = ix,
                    indication = null,
                    enabled = !checkedIn && !checkIn.isCheckingIn,
                ) { onCheckIn(BiometricMethod.Manual.wire) },
            contentAlignment = Alignment.Center,
        ) {
            TRing(
                percent = if (checkedIn) 100 else 0,
                modifier = Modifier.fillMaxSize(),
                accent = accent,
                stroke = 6.dp,
                label = "",
            )
            if (checkIn.isCheckingIn) {
                TeacherSpinner(26.dp)
            } else {
                Icon(
                    if (checkedIn) VIcons.Check else VIcons.ShieldCheck,
                    contentDescription = "Check in",
                    tint = accent,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (checkedIn) "Checked in" else "Tap to check in",
            style = VTheme.type.label.colored(accent).copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Attendance summary swipe card — clubbed metrics (face 0) / per-class list (face 1).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttendanceSummaryCard(
    today: TeacherTodayState,
    obligations: TeacherObligationsState,
    onOpenAttendance: (assignmentId: String, scope: String) -> Unit,
    onOpenUpdate: () -> Unit,
) {
    val c = VTheme.colors
    var face by remember { mutableStateOf(0) }

    // Classes with a real, teachable period today (skip holiday/cancelled rows for the count).
    val periods = today.day?.periods.orEmpty().filter { !it.isCancelled }
    val totalToday = obligations.classesTodayTotal.takeIf { it > 0 } ?: periods.size
    val unmarked = obligations.unmarkedClasses
    val done = (totalToday - unmarked).coerceIn(0, totalToday)
    val percent = if (totalToday == 0) 0 else (done * 100) / totalToday

    SwipeExpandCard(face = face, faceCount = 2, onFaceChange = { face = it }, padding = 18.dp) { f ->
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TEyebrow("ATTENDANCE TODAY", dot = if (unmarked == 0) c.successInk else c.warningInk)
                Spacer(Modifier.weight(1f))
                FaceDots(face, 2)
            }
            Spacer(Modifier.height(12.dp))

            when (f) {
                0 -> {
                    // Clubbed metrics — the single answer to "how many classes is attendance done".
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TRing(
                            percent = percent,
                            modifier = Modifier.size(86.dp),
                            accent = if (unmarked == 0) c.successInk else c.accent,
                            label = "$done/$totalToday",
                            labelSize = 17.sp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (totalToday == 0) "No classes today"
                                else if (unmarked == 0) "All attendance done"
                                else "$unmarked class${if (unmarked == 1) "" else "es"} to mark",
                                style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$done of $totalToday classes marked",
                                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TMetricTile(done.toString(), "Done", c.successInk, Modifier.weight(1f))
                                TMetricTile(unmarked.toString(), "Pending", c.warningInk, Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    TSwipeHint("Swipe to see each class →")
                }
                else -> {
                    // Per-class list — the detail in-place (no navigation), tap a row to mark.
                    if (periods.isEmpty()) {
                        Text("No classes scheduled today.", style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            periods.forEach { p ->
                                AttendanceClassRow(p, onOpenAttendance)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TSwipeHint("← Swipe back to summary")
                }
            }
        }
    }
}

@Composable
private fun AttendanceClassRow(p: ResolvedPeriodUi, onOpen: (assignmentId: String, scope: String) -> Unit) {
    val c = VTheme.colors
    val accent = teacherSubjectColor(c, p.subject.ifBlank { p.className })
    val asg = p.assignmentId
    val ix = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.cream)
            .clickable(interactionSource = ix, indication = null, enabled = asg != null) {
                if (asg != null) onOpen(asg, "${p.classLabel} · ${p.subject}")
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TIconDisc(VIcons.ListChecks, tint = accent, bg = accent.copy(alpha = 0.12f), size = 36.dp, glyph = 18.dp)
        Column(Modifier.weight(1f)) {
            Text("${p.classLabel} · ${p.subject}", style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold))
            Text(
                "${p.startTime}–${p.endTime}${if (p.room.isNotBlank()) " · ${p.room}" else ""}",
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }
        if (p.attendanceMarked) {
            TPill("DONE", bg = c.success.copy(alpha = 0.16f), fg = c.successInk)
        } else {
            TPill("MARK", bg = c.accent.copy(alpha = 0.12f), fg = c.accentDeep)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Schedule card — today's timetable, with a live "now / next" cue.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleCard(today: TeacherTodayState, onOpenLessonPlan: (assignmentId: String, scope: String) -> Unit = { _, _ -> }) {
    val c = VTheme.colors
    val day = today.day
    TCard(padding = 18.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TEyebrow("TODAY'S SCHEDULE", dot = c.accent)
                Spacer(Modifier.weight(1f))
                Text(prettyDate(day?.date), style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
            }
            Spacer(Modifier.height(12.dp))
            when {
                today.isLoading && day == null -> Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { TeacherSpinner(26.dp) }
                day == null -> Text("Couldn't load your schedule.", style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
                day.isHoliday -> HolidayRow(day)
                day.periods.isEmpty() -> Text("No periods scheduled today.", style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    day.periods.forEachIndexed { i, p ->
                        SchedulePeriodRow(
                            p,
                            isNow = i == day.nowIndex,
                            isNext = i == day.nextIndex,
                            onOpenLessonPlan = onOpenLessonPlan,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HolidayRow(day: ResolvedDayUi) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.navy.copy(alpha = 0.06f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TIconDisc(VIcons.Calendar, tint = c.navy, bg = c.navy.copy(alpha = 0.12f), size = 36.dp, glyph = 18.dp)
        Column {
            Text("Holiday", style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            val holidayName = day.holidayName
            if (!holidayName.isNullOrBlank()) Text(holidayName, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
        }
    }
}

@Composable
private fun SchedulePeriodRow(
    p: ResolvedPeriodUi,
    isNow: Boolean,
    isNext: Boolean,
    onOpenLessonPlan: (assignmentId: String, scope: String) -> Unit = { _, _ -> },
) {
    val c = VTheme.colors
    val accent = teacherSubjectColor(c, p.subject.ifBlank { p.className })
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isNow) accent.copy(alpha = 0.10f) else c.cream)
            .then(if (isNow) Modifier.border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.width(54.dp)) {
            Text(p.startTime, style = VTheme.type.bodyStrong.colored(if (isNow) accent else c.ink).copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Text(p.endTime, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.5.sp))
        }
        Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(accent))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (p.isCancelled) "${p.classLabel} · ${p.subject} (cancelled)" else "${p.classLabel} · ${p.subject}",
                    style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                )
                if (isNow) TPill("NOW", bg = accent.copy(alpha = 0.18f), fg = accent)
                else if (isNext) TPill("NEXT", bg = c.accent.copy(alpha = 0.10f), fg = c.accentDeep)
                // Lesson plan chip (LESSON_PLANNING_SPEC §7.3)
                p.lessonPlanStatus?.let { lps ->
                    val lpColor = when (lps) {
                        "completed" -> c.tealDeep
                        "skipped" -> c.ink3
                        else -> c.accent
                    }
                    val lpIcon = when (lps) {
                        "completed" -> VIcons.Check
                        "skipped" -> VIcons.Close
                        else -> VIcons.ClipboardList
                    }
                    val lpIx = remember { MutableInteractionSource() }
                    val scopeLabel = if (p.section.isBlank()) "${p.className} · ${p.subject}" else "${p.className}-${p.section} · ${p.subject}"
                    TPill(
                        label = lps.uppercase(),
                        bg = lpColor.copy(alpha = 0.12f),
                        fg = lpColor,
                        leading = { Icon(lpIcon, contentDescription = null, modifier = Modifier.size(10.dp)) },
                        modifier = Modifier.clickable(interactionSource = lpIx, indication = null) {
                            p.assignmentId?.let { aid -> onOpenLessonPlan(aid, scopeLabel) }
                        },
                    )
                }
            }
            val sub = p.substituteTeacherName
            if (p.room.isNotBlank() || sub != null) {
                Text(
                    buildString {
                        if (p.room.isNotBlank()) append(p.room)
                        if (sub != null) {
                            if (isNotEmpty()) append(" · ")
                            append("Sub: $sub")
                        }
                    },
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reminders / "what needs me" card — server obligations, honest when caught up.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RemindersCard(
    obligations: TeacherObligationsState,
    onOpenUpdate: () -> Unit,
    onOpenClasses: () -> Unit,
) {
    val c = VTheme.colors
    if (obligations.unavailable) return // honest: hide rather than fake "all caught up"
    TCard(padding = 18.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TEyebrow("WHAT NEEDS YOU", dot = if (obligations.isAllCaughtUp) c.successInk else c.warningInk)
                Spacer(Modifier.weight(1f))
                if (obligations.totalOutstanding > 0) {
                    TPill(obligations.totalOutstanding.toString(), bg = c.warning.copy(alpha = 0.18f), fg = c.warningInk)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (obligations.isAllCaughtUp || obligations.items.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TIconDisc(VIcons.Sparkles, tint = c.successInk, bg = c.success.copy(alpha = 0.16f), size = 40.dp, glyph = 20.dp)
                    Column {
                        Text("All caught up", style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text("Nothing pending right now.", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    obligations.items.take(5).forEach { item ->
                        ReminderRow(item, onOpenUpdate, onOpenClasses)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(item: ObligationItemDto, onOpenUpdate: () -> Unit, onOpenClasses: () -> Unit) {
    val c = VTheme.colors
    val tint = when (item.type) {
        "attendance" -> c.warningInk
        "marks" -> c.accent
        "homework" -> c.tealDeep
        "leave" -> c.dangerInk
        else -> c.ink2
    }
    val ix = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.cream)
            .clickable(interactionSource = ix, indication = null) {
                if (item.type == "leave") onOpenClasses() else onOpenUpdate()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TIconDisc(obligationIcon(item.type), tint = tint, bg = tint.copy(alpha = 0.12f), size = 36.dp, glyph = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(item.title, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            if (item.subtitle.isNotBlank()) {
                Text(item.subtitle, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp), maxLines = 1)
            }
        }
        if (item.count > 0) TPill(item.count.toString(), bg = tint.copy(alpha = 0.14f), fg = tint)
        Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
    }
}
