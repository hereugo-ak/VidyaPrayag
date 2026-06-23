package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import org.koin.compose.viewmodel.koinViewModel

/**
 * TodayScreen — the new first tab (Doc 04 §4: Today · Classes · Gradebook · Planner ·
 * Profile). It is the teacher's at-a-glance "what now?": the self check-in band, the real
 * obligations strip, and the live [TeacherScheduleCard] (3-face, server-resolved).
 * Attendance is reached as an ACTION from the card's pre-scoped CTAs — never as its own tab.
 *
 * T-601: the greeting + notification bell + account avatar moved to the ONE canonical
 * [TeacherHeader] mounted by [TeacherPortalV2] (so there is no double chrome). This screen now
 * starts at the check-in band.
 *
 * State is fully VM-driven via [TeacherTodayViewModel] (T-105b) over the T-104 resolved
 * day/week. VStateHost handles the three transport states; holiday vs. unseeded are
 * CONTENT states distinguished inside the card itself (Doc 10 §8), so the empty leg here
 * only fires when the day genuinely failed to resolve.
 */
@Composable
fun TodayScreen(
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
    modifier: Modifier = Modifier,
    onOpenObligation: (ObligationItemDto) -> Unit = {},
    viewModel: TeacherTodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    TodayContent(
        state = state,
        onMarkAttendance = onMarkAttendance,
        onOpenSyllabus = onOpenSyllabus,
        onOpenHomework = onOpenHomework,
        onOpenObligation = onOpenObligation,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun TodayContent(
    state: TeacherTodayState,
    onMarkAttendance: (ResolvedPeriodUi) -> Unit,
    onOpenSyllabus: (ResolvedPeriodUi) -> Unit,
    onOpenHomework: (ResolvedPeriodUi) -> Unit,
    onOpenObligation: (ObligationItemDto) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Self check-in band (biometric ladder + manual fallback) ──────────
        // T-106c / Doc 04 §5.1: record the teacher's own presence first thing.
        // Owns its own VM; the device biometric prompt is platform-abstracted and
        // never a hard gate (manual confirm always available).
        TeacherCheckInCard(modifier = Modifier.fillMaxWidth())

        // ── Real obligations strip ("what needs me") ────────────────────────
        // T-107 / Doc 04 §5.5: live, allocation-scoped counts (unmarked classes,
        // unpublished results, homework to review, leave decisions). Replaces the
        // fabricated Today tasks (B-HOME-4). Each row deep-links pre-scoped; the
        // strip hides itself on read-failure and shows "all caught up" only when
        // genuinely zero. Owns its own VM.
        TeacherObligationsStrip(
            onOpenObligation = onOpenObligation,
            modifier = Modifier.fillMaxWidth(),
        )

        // ── The live schedule card (3 faces) ────────────────────────────────
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            // Holiday / unseeded are CONTENT states inside the card; "empty" only
            // when the day genuinely failed to resolve (no day object at all).
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
