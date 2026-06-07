package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStep
import com.littlebridge.vidyaprayag.feature.admin.presentation.DashboardOnboardingStatus
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * SchoolHomeScreenV2 — `Admin.tsx → AdminHome`, wired to the real
 * [SchoolDashboardViewModel] (`AuthRepository` → `GET /api/v1/user/details`).
 *
 * ⚠️ This VM exposes **six separate flows** (adminName / progress / steps /
 * onboardingStatus / isLoading / errorMessage), not a single state object. The only
 * data the dashboard endpoint genuinely returns today is the **onboarding hero**
 * (greeting + onboarding progress + the four setup steps). The rich operational
 * metrics in the original mock (attendance-by-class, syllabus coverage, subject
 * performance, teacher activity, fee glance) have no backend feed at the home level,
 * so they're shown as `VComingSoon` rather than fabricating numbers (LAW 6). No
 * MockV2 in production; the three UI states come from [VStateHost] (LAW 2/3).
 */
@Composable
fun SchoolHomeScreenV2(
    modifier: Modifier = Modifier,
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onExit: () -> Unit = {},
    viewModel: SchoolDashboardViewModel = koinViewModel(),
) {
    val adminName by viewModel.adminName.collectAsStateV2()
    val progress by viewModel.progress.collectAsStateV2()
    val steps by viewModel.steps.collectAsStateV2()
    val onboardingStatus by viewModel.onboardingStatus.collectAsStateV2()
    val isLoading by viewModel.isLoading.collectAsStateV2()
    val errorMessage by viewModel.errorMessage.collectAsStateV2()

    SchoolHomeContent(
        adminName = adminName,
        progress = progress,
        steps = steps,
        onboardingStatus = onboardingStatus,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onRetry = viewModel::refresh,
        onOpenNotifications = onOpenNotifications,
        onOpenCalendar = onOpenCalendar,
        onExit = onExit,
        modifier = modifier,
    )
}

@Composable
private fun SchoolHomeContent(
    adminName: String,
    progress: Float,
    steps: List<OnboardingStep>,
    onboardingStatus: DashboardOnboardingStatus,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCalendar: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val completed = onboardingStatus == DashboardOnboardingStatus.COMPLETED

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.teal), contentAlignment = Alignment.Center) {
                    Icon(VIcons.GraduationCap, contentDescription = null, tint = Color(0xFF080808), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("School console", style = VTheme.type.h4.colored(c.ink))
                    Text(
                        if (completed) "Campus live" else "Setup in progress",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.ink.copy(alpha = 0.06f)).clickable { onOpenNotifications() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Bell, contentDescription = "Notifications", tint = c.ink, modifier = Modifier.size(18.dp))
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(6.dp).clip(CircleShape).background(c.danger))
                }
                Box(Modifier.clickable { onExit() }) { VAvatar(name = adminName, size = 36.dp) }
            }
        }

        VStateHost(
            loading = isLoading,
            error = errorMessage,
            isEmpty = false,
            onRetry = onRetry,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // ── Greeting ───────────────────────────────────────────────────
                Column {
                    Text("Welcome, $adminName", style = VTheme.type.h1.colored(c.ink))
                    Text(
                        if (completed) "Your campus is live. Manage everything from here."
                        else "Let's finish setting up your school.",
                        style = VTheme.type.body.colored(c.ink2),
                    )
                }

                // ── Onboarding hero (REAL data) ────────────────────────────────
                VCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        VLabel(if (completed) "Onboarding complete" else "Onboarding progress")
                        VBadge(
                            text = "${(progress * 100).roundToInt()}%",
                            tone = if (completed) VBadgeTone.Success else VBadgeTone.Arctic,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    VProgressBar(
                        value = progress * 100f,
                        tone = if (completed) VBadgeTone.Success else VBadgeTone.Arctic,
                    )
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        steps.forEach { step -> OnboardingStepRow(step) }
                    }
                }

                // ── Operational metrics (no backend feed yet) ──────────────────
                Column {
                    Text("Today at a glance", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    VComingSoon(
                        title = "Live campus metrics",
                        description = "Attendance-by-class, syllabus coverage, subject performance and the teacher activity feed will appear here once the daily-metrics rollup endpoint is connected.",
                    )
                }

                Column {
                    Text("Early-warning radar", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    VComingSoon(
                        title = "PEWS — Predictive Early Warning",
                        description = "Combines attendance, marks, fee status and behavioural signals to surface at-risk students before exam season.",
                        preview = { PewsPreview() },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepRow(step: OnboardingStep) {
    val c = VTheme.colors
    val (icon, tint) = when {
        step.isCompleted -> VIcons.Check to c.successInk
        step.status.equals(OnboardingStep.STATUS_LOCKED, ignoreCase = true) -> VIcons.Lock to c.ink3
        else -> VIcons.ClipboardList to c.ink2
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(c.ink.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(step.title, style = VTheme.type.bodyStrong.colored(c.ink))
            if (step.description.isNotBlank()) {
                Text(step.description, style = VTheme.type.caption.colored(c.ink2))
            }
        }
        if (step.isCompleted) {
            VBadge(text = "Done", tone = VBadgeTone.Success)
        }
    }
}
