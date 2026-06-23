package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TodayScreen — the new first tab (Doc 04 §4: Today · Classes · Gradebook · Planner ·
 * Profile). It is the teacher's at-a-glance "what now?": a greeting bar + the live
 * [TeacherScheduleCard] (3-face, server-resolved). Attendance is reached as an ACTION
 * from the card's pre-scoped CTAs — never as its own tab.
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
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: TeacherTodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    TodayContent(
        state = state,
        onMarkAttendance = onMarkAttendance,
        onOpenSyllabus = onOpenSyllabus,
        onOpenHomework = onOpenHomework,
        onOpenNotifications = onOpenNotifications,
        onOpenProfile = onOpenProfile,
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
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val firstName = state.teacherName.substringBefore(' ').ifBlank { state.teacherName }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Greeting bar ────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                VLabel("Today")
                Text(
                    firstName.ifBlank { "Teacher" },
                    style = VTheme.type.h1.colored(c.ink),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.ink.copy(alpha = 0.06f)).clickable { onOpenNotifications() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Bell, contentDescription = "Notifications", tint = c.ink, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.clickable { onOpenProfile() }) {
                    VAvatar(name = state.teacherName.ifBlank { "Teacher" }, size = 40.dp)
                }
            }
        }

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
