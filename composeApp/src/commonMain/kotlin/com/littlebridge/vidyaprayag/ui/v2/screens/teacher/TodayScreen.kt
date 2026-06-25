package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherObligationsState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherObligationsViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * TodayScreen — the Enroll+ Teacher Portal **Home tab**, now built from the loop
 * redesign's signature components (Phases P2-T1…T7), wired to the REAL view-models.
 *
 * This is the surface where the loop redesign actually lands and is visible:
 *   • [TeacherHomeHeader]  (P2-T1) — the signature gradient hero (greeting + first
 *     name + date + avatar→Profile + bell→Notifications), fed by [TeacherTodayState].
 *   • [QuickActionRow]     (P2-T3) — the three core jobs one tap away, routed to the
 *     self-sufficient tabs / pre-scoped write planes.
 *   • [SmartNudgeSection]  (P2-T5) — the "NEEDS ATTENTION" stack, derived HONESTLY
 *     from the live [TeacherObligationsState] (no fabricated nudges; empty → hidden).
 *   • [TodayClassStrip]    (P2-T2) — the horizontally-scrollable day timeline,
 *     consuming the server-resolved [ResolvedDayUi] directly (real period order,
 *     times, room, attendance status, authoritative `nowIndex`).
 *
 * Below the loop hero, the working production surfaces are preserved so nothing
 * regresses functionally: the self check-in band ([TeacherCheckInCard]) and the
 * live 3-face [TeacherScheduleCard] (with its own transport states via [VStateHost]).
 *
 * DESIGN LAW (carried from the parents portal): NEVER COLLAPSE TO WHITE SPACE — the
 * hero always renders rich content for the signed-in teacher, and every section
 * either shows real data or hides itself cleanly. Nothing here is hardcoded; the
 * whole screen is fed by [TeacherTodayViewModel] + [TeacherObligationsViewModel].
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
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onMessageParent: () -> Unit = {},
    unreadCount: Int = 0,
    viewModel: TeacherTodayViewModel = koinViewModel(),
    obligationsViewModel: TeacherObligationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val obligations by obligationsViewModel.state.collectAsStateV2()
    TodayContent(
        state = state,
        obligations = obligations,
        unreadCount = unreadCount,
        onMarkAttendance = onMarkAttendance,
        onOpenSyllabus = onOpenSyllabus,
        onOpenHomework = onOpenHomework,
        onOpenObligation = onOpenObligation,
        onGoToClasses = onGoToClasses,
        onGoToPlanner = onGoToPlanner,
        onGoToGradebook = onGoToGradebook,
        onOpenNotifications = onOpenNotifications,
        onOpenProfile = onOpenProfile,
        onMessageParent = onMessageParent,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun TodayContent(
    state: TeacherTodayState,
    obligations: TeacherObligationsState,
    unreadCount: Int,
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
    onOpenObligation: (ObligationItemDto) -> Unit,
    onGoToClasses: () -> Unit,
    onGoToPlanner: () -> Unit,
    onGoToGradebook: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onMessageParent: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    Box(
        modifier
            .fillMaxSize()
            // The calm lavender canvas with the faintest accent aurora wash — identical
            // to ParentHomeScreenV2 (≤4% accent), the cross-portal parity contract.
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
                .padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(Enroll.space.lg),
        ) {
            // ── P2-T1: the signature gradient Home hero (full-bleed; sits on status bar) ──
            TeacherHomeHeader(
                teacherName = state.teacherName.ifBlank { "Teacher" },
                photoUrl = null,
                unreadCount = unreadCount,
                onOpenProfile = onOpenProfile,
                onOpenNotifications = onOpenNotifications,
            )

            // The rest of the Home content is on the screen-padding rhythm.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Enroll.space.lg),
                verticalArrangement = Arrangement.spacedBy(Enroll.space.lg),
            ) {
                // ── P2-T3: the three core jobs, one tap away ─────────────────────
                // Schedule-independent — routed to the self-sufficient tabs / pre-scoped
                // planes, so the work is reachable even on a holiday / off-timetable day.
                QuickActionRow(
                    onTakeAttendance = onGoToClasses,
                    onAddMarks = onGoToGradebook,
                    onMessageParent = onMessageParent,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── P2-T5: "NEEDS ATTENTION" — derived from REAL obligations ──────
                // Honest: only shows when the server says there is outstanding work.
                // Each nudge's action routes back through the host's obligation deep-link
                // contract (the same pre-scoped surfaces the strip used).
                val nudges = obligations.toNudges()
                SmartNudgeSection(
                    nudges = nudges,
                    onAction = { nudge -> obligations.obligationFor(nudge)?.let(onOpenObligation) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Self check-in band (biometric ladder + manual fallback) ───────
                // Owns its own VM; never a hard gate. Preserved from the production flow.
                TeacherCheckInCard(modifier = Modifier.fillMaxWidth())

                // ── P2-T2: the signature Today strip + the live 3-face schedule card ──
                VStateHost(
                    loading = state.isLoading,
                    error = state.error,
                    isEmpty = state.day == null && !state.isLoading && state.error == null,
                    emptyIcon = VIcons.Calendar,
                    emptyTitle = "No schedule yet",
                    emptyBody = "Your timetable will appear here once it's published",
                    onRetry = onRetry,
                ) {
                    val day = state.day
                    if (day != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(Enroll.space.lg)) {
                            // The loop's signature horizontally-scrollable day timeline.
                            TodayClassStrip(
                                day = day,
                                onTakeAttendance = onMarkAttendance,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            // The production 3-face schedule card (full per-period actions),
                            // kept so syllabus/homework pre-scoped CTAs remain available.
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VM → loop UI-model mapping
//
// The loop's [SmartNudgeSection] consumes a typed [TeacherNudge] list. We build it
// HONESTLY from the live [TeacherObligationsState] — one nudge per outstanding
// obligation item, capped so the Home tab stays calm. Each nudge is paired back to
// its source [ObligationItemDto] so the action chip routes through the existing
// pre-scoped deep-link contract ([onOpenObligation]) the obligations strip uses.
// ─────────────────────────────────────────────────────────────────────────────

/** How many nudges Home shows at once (the rest live in the obligations strip). */
private const val MAX_HOME_NUDGES = 3

/** Build the typed nudge list from real obligation items (never fabricated). */
private fun TeacherObligationsState.toNudges(): List<TeacherNudge> =
    items.take(MAX_HOME_NUDGES).mapNotNull { it.toNudge() }

/** Map one obligation item onto the nudge type that best matches its `type`. */
private fun ObligationItemDto.toNudge(): TeacherNudge? = when (type) {
    "attendance" -> TeacherNudge.AttendanceNotTaken(
        className = title,
        periodTime = subtitle,
        periodId = assignmentId,
    )
    "marks" -> TeacherNudge.MarksNotEntered(
        className = title,
        subjectName = subtitle,
        examName = subtitle.ifBlank { "Exam" },
        classId = assignmentId,
        examId = refId,
    )
    "homework" -> TeacherNudge.HomeworkUngraded(
        className = title,
        count = count.coerceAtLeast(1),
        classId = assignmentId,
    )
    else -> null
}

/**
 * Resolve a nudge back to its originating obligation item so the action chip can
 * fire the host's existing pre-scoped routing. Matches on the carried id/title so
 * the right scoped surface opens (never a blind default).
 */
private fun TeacherObligationsState.obligationFor(nudge: TeacherNudge): ObligationItemDto? {
    val targetId: String? = when (nudge) {
        is TeacherNudge.AttendanceNotTaken -> nudge.periodId
        is TeacherNudge.MarksNotEntered -> nudge.classId
        is TeacherNudge.HomeworkUngraded -> nudge.classId
        is TeacherNudge.ParentUnread -> nudge.threadId
    }
    val targetTitle: String = when (nudge) {
        is TeacherNudge.AttendanceNotTaken -> nudge.className
        is TeacherNudge.MarksNotEntered -> nudge.className
        is TeacherNudge.HomeworkUngraded -> nudge.className
        is TeacherNudge.ParentUnread -> nudge.parentName
    }
    return items.firstOrNull { it.assignmentId == targetId && targetId != null }
        ?: items.firstOrNull { it.title == targetTitle }
}
