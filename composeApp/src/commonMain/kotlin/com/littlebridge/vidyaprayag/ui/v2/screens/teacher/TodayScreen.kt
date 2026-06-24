package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedDayUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import androidx.compose.ui.graphics.vector.ImageVector
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay
import org.koin.compose.viewmodel.koinViewModel

/**
 * TodayScreen — the new first tab (Doc 04 §4: Today · Classes · Gradebook · Planner ·
 * Profile). It is the teacher's at-a-glance "what now?": a flagship hero (greeting +
 * identity + a live day-journey ring), the self check-in band, the real obligations
 * strip, and the live [TeacherScheduleCard] (3-face, server-resolved). Attendance is
 * reached as an ACTION from the card's pre-scoped CTAs — never as its own tab.
 *
 * T-601: the greeting + notification bell + account avatar live in the ONE canonical
 * [TeacherHeader] mounted by [TeacherPortalV2] (so there is no double chrome). The hero
 * here is the CONTENT-level greeting card (the teacher equivalent of the parents portal's
 * `TodayCard`) — a richer, on-brand identity + journey moment that the slim header chip
 * cannot carry. It never opens the screen on emptiness.
 *
 * DESIGN LAW (carried verbatim from the parents portal): NEVER COLLAPSE TO WHITE SPACE.
 * The canvas paints the brand aurora wash, the hero always renders rich content for the
 * signed-in teacher, and every feature card below shows a designed state (holiday/unseeded
 * are content states inside the schedule card; the obligations strip earns "all caught up").
 * Nothing is hardcoded — the hero is fed entirely by [TeacherTodayState].
 *
 * State is fully VM-driven via [TeacherTodayViewModel] (T-105b) over the T-104 resolved
 * day/week. VStateHost handles the three transport states for the schedule card only.
 */
@Composable
fun TodayScreen(
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
    modifier: Modifier = Modifier,
    onOpenObligation: (ObligationItemDto) -> Unit = {},
    onGoToClasses: () -> Unit = {},
    onGoToPlanner: () -> Unit = {},
    onGoToGradebook: () -> Unit = {},
    viewModel: TeacherTodayViewModel = koinViewModel(),
    classesViewModel: TeacherClassesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val classesState by classesViewModel.state.collectAsStateV2()
    TodayContent(
        state = state,
        classesState = classesState,
        onMarkAttendance = onMarkAttendance,
        onOpenSyllabus = onOpenSyllabus,
        onOpenHomework = onOpenHomework,
        onOpenObligation = onOpenObligation,
        onGoToClasses = onGoToClasses,
        onGoToPlanner = onGoToPlanner,
        onGoToGradebook = onGoToGradebook,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun TodayContent(
    state: TeacherTodayState,
    classesState: TeacherClassesState,
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
    onOpenObligation: (ObligationItemDto) -> Unit,
    onGoToClasses: () -> Unit,
    onGoToPlanner: () -> Unit,
    onGoToGradebook: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    Box(
        modifier
            .fillMaxSize()
            // The canvas IS the brand background token (#FCF8FF) with the faintest lavender
            // aurora wash top-left — identical to ParentHomeScreenV2 (≤4% accent, a whisper,
            // never a wall-to-wall fill). This is what makes the teacher Home read as the same
            // product as the parents portal (Doc 10 §12).
            .background(c.background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accent.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.02f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Flagship hero: greeting + identity + live day-journey ring ───────
            // The teacher equivalent of the parents portal TodayCard. Always rich for the
            // signed-in teacher; the journey ring tracks REAL classes-done/total for today
            // (server-resolved), and the context line adapts honestly to holiday/unseeded
            // days. Nothing here is hardcoded.
            TeacherTodayHero(state = state, modifier = Modifier.fillMaxWidth())

            // ── At-a-glance stats (ALWAYS useful, never blank) ───────────────────
            // Even on a holiday / off-timetable day, these four facts give the teacher a
            // real reason to open Home: how many classes & students they're responsible for,
            // today's attendance-marking progress, and the live at-risk headcount. Sourced
            // from the same classes feed the Classes tab uses (server-computed).
            TeacherStatsStrip(classesState = classesState, modifier = Modifier.fillMaxWidth())

            // ── Quick actions (schedule-independent entry to the work) ───────────
            // The brief: attendance must be reachable even with no schedule. These tiles
            // route to the self-sufficient tabs (each fronts its own class picker), so the
            // four core teaching jobs are always one tap away.
            TeacherQuickActions(
                onMarkAttendance = onGoToClasses,
                onAssignHomework = onGoToPlanner,
                onUpdateSyllabus = onGoToPlanner,
                onOpenGradebook = onGoToGradebook,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Self check-in band (biometric ladder + manual fallback) ──────────
            // T-106c / Doc 04 §5.1: record the teacher's own presence first thing.
            // Owns its own VM; the device biometric prompt is platform-abstracted and
            // never a hard gate (manual confirm always available).
            TeacherCheckInCard(modifier = Modifier.fillMaxWidth())

            // ── Real obligations strip ("what needs me") ────────────────────────
            // T-107 / Doc 04 §5.5: live, allocation-scoped counts. The strip hides itself
            // on read-failure and shows "all caught up" only when genuinely zero. Owns its VM.
            TeacherObligationsStrip(
                onOpenObligation = onOpenObligation,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── The live schedule card (3 faces, swipe-within-card) ──────────────
            VStateHost(
                loading = state.isLoading,
                error = state.error,
                // Holiday / unseeded are CONTENT states inside the card; "empty" only when
                // the day genuinely failed to resolve (no day object at all).
                isEmpty = state.day == null && !state.isLoading && state.error == null,
                emptyIcon = VIcons.Calendar,
                emptyTitle = "No schedule yet",
                emptyBody = "Your timetable will appear here once it's published",
                onRetry = onRetry,
            ) {
                val day = state.day
                if (day != null) {
                    TeacherScheduleCard(
                        day = day,
                        week = state.week,
                        onMarkAttendance = onMarkAttendance,
                        onOpenSyllabus = onOpenSyllabus,
                        onOpenHomework = onOpenHomework,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * TeacherTodayHero — the dashboard's flagship greeting card, the teacher counterpart of the
 * parents portal `TodayCard`. It carries the teacher's avatar, a time-aware greeting + first
 * name, a truthful day-context chip, a live **day-journey ring** built from real
 * classes-done/total, and a contextual line. Always renders rich content, so Today never opens
 * on emptiness (DESIGN LAW). Everything is sourced from [TeacherTodayState] — nothing hardcoded.
 */
@Composable
private fun TeacherTodayHero(state: TeacherTodayState, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val name = state.teacherName.ifBlank { "Teacher" }
    val firstName = name.substringBefore(' ').ifBlank { name }
    val greeting = teacherGreeting(nowMinutesOfDay() / 60)

    val day = state.day
    val total = day?.periods?.count { !it.isCancelled } ?: 0
    val done = dayClassesDone(day)
    val progressPct = if (total > 0) ((done.toFloat() / total) * 100f).toInt().coerceIn(0, 100) else 0

    val context = heroContext(state, done, total)
    val dateLine = todayLongLabel()

    // A premium gradient hero (the brand-accent moment) — a deep violet→navy wash with the
    // day-journey ring on the right, mirroring the parents portal TodayCard's flagship weight.
    // The header already owns the small "greeting · name" chrome chip; this is the flagship
    // welcome (one personal greeting + live date + today's teaching context). White ink on the
    // gradient; everything is real day data, nothing fabricated.
    VCard(modifier = modifier, padding = 0.dp, border = false) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(c.accentDeep, c.navyDeep)))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$greeting, $firstName",
                        style = VTheme.type.h2.colored(Color.White).copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        dateLine,
                        style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.7f)).copy(fontSize = 11.5.sp),
                    )
                    Spacer(Modifier.height(14.dp))
                    // Day-context headline (e.g. "3 classes ahead" / "All classes done").
                    Text(
                        context.headline,
                        style = VTheme.type.bodyStrong.colored(Color.White).copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                    if (context.sub.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            context.sub,
                            style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.75f)).copy(fontSize = 11.sp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                DayJourneyRing(
                    percent = progressPct,
                    label = if (total > 0) "$done/$total" else "—",
                    onDark = true,
                    modifier = Modifier.size(76.dp),
                )
            }
        }
    }
}

/**
 * TeacherStatsStrip — four at-a-glance facts that are useful EVERY day (including holidays),
 * so Home is never blank: classes I teach, total students, today's attendance progress
 * (marked/total), and the live at-risk headcount. All server-computed via the classes feed.
 */
@Composable
private fun TeacherStatsStrip(classesState: TeacherClassesState, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val classes = classesState.classes
    val classCount = classes.size
    val studentTotal = classes.sumOf { it.studentCount }
    val markedToday = classes.count { it.todayAttendanceMarked }
    val atRiskTotal = classes.sumOf { it.atRiskCount }

    // While the classes feed is still loading, render a calm skeleton row (never collapse).
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            icon = VIcons.Users,
            value = if (classCount == 0 && classesState.isLoading) "—" else classCount.toString(),
            label = if (classCount == 1) "Class" else "Classes",
            tint = c.accentDeep,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = VIcons.GraduationCap,
            value = if (classCount == 0 && classesState.isLoading) "—" else studentTotal.toString(),
            label = "Students",
            tint = c.tealDeep,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = VIcons.Check,
            value = if (classCount == 0) "—" else "$markedToday/$classCount",
            label = "Marked",
            tint = if (classCount > 0 && markedToday >= classCount) c.successInk else c.warningInk,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            icon = VIcons.AlertTriangle,
            value = if (classCount == 0 && classesState.isLoading) "—" else atRiskTotal.toString(),
            label = "At risk",
            tint = if (atRiskTotal > 0) c.dangerInk else c.ink3,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier, padding = 12.dp) {
        Column(horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = VTheme.type.dataLg.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp), maxLines = 1)
            Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp), maxLines = 1)
        }
    }
}

/**
 * TeacherQuickActions — a 2×2 grid of the four core teaching jobs. Schedule-independent:
 * each tile routes to a self-sufficient tab (which fronts its own class picker), so the work
 * is always reachable even when today has no timetable. Solves the "attendance unreachable on
 * a holiday" gap at the Home level, mirroring the parents portal's action-tile language.
 */
@Composable
private fun TeacherQuickActions(
    onMarkAttendance: () -> Unit,
    onAssignHomework: () -> Unit,
    onUpdateSyllabus: () -> Unit,
    onOpenGradebook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "QUICK ACTIONS",
            style = VTheme.type.label.colored(c.accentDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.9.sp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(VIcons.Check, "Mark attendance", c.accent, onMarkAttendance, Modifier.weight(1f))
            ActionTile(VIcons.Edit3, "Assign homework", c.teal, onAssignHomework, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(VIcons.BookOpen, "Update syllabus", c.accentSoft, onUpdateSyllabus, Modifier.weight(1f))
            ActionTile(VIcons.GraduationCap, "Gradebook", c.tealDeep, onOpenGradebook, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier, padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Text(label, style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontSize = 13.sp), maxLines = 2)
        }
    }
}

/**
 * Day-progress ring (the brand-accent moment), mirroring the parent JourneyRing.
 * [onDark] paints a white ring + white label for use on the gradient hero; otherwise the
 * violet brand sweep on a light surface.
 */
@Composable
private fun DayJourneyRing(percent: Int, label: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val c = VTheme.colors
    val sweep by animateFloatAsState(targetValue = percent / 100f, label = "teacherJourneySweep")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = if (onDark) Color.White.copy(alpha = 0.22f) else c.accent.copy(alpha = 0.16f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = if (onDark) {
                    Brush.sweepGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White, Color.White))
                } else {
                    Brush.sweepGradient(listOf(c.accentSoft, c.accent, c.accentDeep))
                },
                startAngle = -90f, sweepAngle = 360f * sweep, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            label,
            style = VTheme.type.dataLg.colored(if (onDark) Color.White else c.accentDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
        )
    }
}

/** Friendly "Tuesday, 24 June" label from today's ISO date — for the hero. */
private fun todayLongLabel(): String {
    val iso = com.littlebridge.vidyaprayag.util.todayIso()
    val parsed = com.littlebridge.vidyaprayag.util.parseIsoDate(iso) ?: return ""
    val (y, m, d) = parsed
    val weekday = when (com.littlebridge.vidyaprayag.util.dayOfWeek(y, m, d)) {
        0 -> "Sunday"; 1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"
        4 -> "Thursday"; 5 -> "Friday"; else -> "Saturday"
    }
    val month = com.littlebridge.vidyaprayag.util.MONTH_LONG.getOrElse(m - 1) { "" }
    return "$weekday, $d $month"
}

private data class HeroContext(val headline: String, val sub: String)

/** Build the truthful hero context from the resolved day — never fabricated. */
private fun heroContext(state: TeacherTodayState, done: Int, total: Int): HeroContext {
    val day = state.day
    return when {
        day == null -> HeroContext("Loading your day…", "")
        day.isHoliday -> HeroContext("School is closed today", day.holidayName?.takeIf { it.isNotBlank() } ?: "Enjoy the holiday")
        total == 0 -> HeroContext("No classes scheduled today", "Nothing on your timetable for today")
        done >= total -> HeroContext("All classes done", "You've taught every class today")
        done == 0 -> HeroContext("$total ${if (total == 1) "class" else "classes"} ahead", "Your teaching day is about to begin")
        else -> HeroContext("$done of $total done", "${total - done} still to go today")
    }
}

/** Server-authoritative count of classes already completed today (mirrors the schedule card). */
private fun dayClassesDone(day: ResolvedDayUi?): Int {
    if (day == null) return 0
    val total = day.periods.size
    return day.periods.indices.count { idx ->
        val ni = if (day.nowIndex >= 0) day.nowIndex else total
        idx < ni && !day.periods[idx].isCancelled
    }
}

