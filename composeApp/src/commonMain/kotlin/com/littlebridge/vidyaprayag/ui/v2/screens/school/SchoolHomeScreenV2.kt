package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.littlebridge.vidyaprayag.ui.v2.theme.VElevationLevel
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStep
import com.littlebridge.vidyaprayag.feature.admin.presentation.DashboardOnboardingStatus
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.vElevation
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
 * metrics (attendance-by-class, syllabus coverage, subject performance) and the
 * at-risk cohort (PEWS) ARE served by `SchoolAnalyticsRouting` (/overview,
 * /class-performance, /syllabus-coverage, /student-cohort), so the dashboard
 * surfaces them as real entry points into `AnalyticsDashboardScreenV2` and the
 * People/cohort tab (RA-24) — no Coming-Soon where a feed exists (LAW 6). No
 * MockV2 in production; the three UI states come from [VStateHost] (LAW 2/3).
 */
@Composable
fun SchoolHomeScreenV2(
    modifier: Modifier = Modifier,
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenPews: () -> Unit = {},
    onExit: () -> Unit = {},
    viewModel: SchoolDashboardViewModel = koinViewModel(),
    // RA-S06: account-level notifications feed (GET /api/v1/notifications is
    // JWT-scoped, role-agnostic) drives the header bell's unread dot.
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
) {
    val adminName by viewModel.adminName.collectAsStateV2()
    val notifications by notificationsViewModel.state.collectAsStateV2()
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
        unreadCount = notifications.unreadCount,
        onRetry = viewModel::refresh,
        onOpenNotifications = onOpenNotifications,
        onOpenCalendar = onOpenCalendar,
        onOpenAnalytics = onOpenAnalytics,
        onOpenPews = onOpenPews,
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
    unreadCount: Int,
    onRetry: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenPews: () -> Unit,
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
            .padding(top = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        FloatingSchoolHomeHeader(
            adminName = adminName,
            completed = completed,
            unreadCount = unreadCount,
            onOpenNotifications = onOpenNotifications,
            onExit = onExit,
            modifier = Modifier
                .fillMaxWidth()
        )

        VStateHost(
            loading = isLoading,
            error = errorMessage,
            isEmpty = false,
            onRetry = onRetry,
            skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonDashboard() },
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

                // ── Operational metrics (REAL — opens the Analytics dashboard) ─
                // RA-24: the /overview, /class-performance, /syllabus-coverage and
                // /student-cohort aggregates are live and rendered by
                // AnalyticsDashboardScreenV2, so this is a real entry point, not a
                // Coming-Soon placeholder.
                Column {
                    Text("Today at a glance", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    MetricEntryCard(
                        icon = VIcons.TrendingUp,
                        title = "Live campus metrics",
                        description = "Attendance-by-class, syllabus coverage and subject performance from the live analytics rollup.",
                        onClick = onOpenAnalytics,
                    )
                }

                // ── Early-warning radar (REAL — opens the at-risk cohort) ──────
                // RA-24: /student-cohort returns real at_risk_students + risk
                // counts, rendered on the People tab. Tapping opens it.
                Column {
                    Text("Early-warning radar", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    MetricEntryCard(
                        icon = VIcons.AlertTriangle,
                        title = "PEWS — Predictive Early Warning",
                        description = "Attendance, marks and risk signals surface at-risk students before exam season.",
                        onClick = onOpenPews,
                        preview = { PewsPreview() },
                    )
                }
            }
        }
    }
}


@Composable
private fun FloatingSchoolHomeHeader(
    adminName: String,
    completed: Boolean,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Row(
        modifier
            .vElevation(VElevationLevel.Card, radius = 28.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(c.card.copy(alpha = if (c.isNight) 0.94f else 0.98f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(c.teal.copy(alpha = if (c.isNight) 0.26f else 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VIcons.GraduationCap,
                contentDescription = null,
                tint = c.tealDeep,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(Modifier.weight(1f)) {
            Text("School console", style = VTheme.type.h4.colored(c.ink))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (completed) c.successInk else c.warningInk),
                )
                Text(
                    if (completed) "Campus live" else "Setup in progress",
                    style = VTheme.type.caption.colored(c.ink2),
                )
            }
        }

        HeaderIconButton(onClick = onOpenNotifications) {
            Icon(VIcons.Bell, contentDescription = "Notifications", tint = c.ink, modifier = Modifier.size(18.dp))
            if (unreadCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(c.danger),
                )
            }
        }

        Box(Modifier.clip(CircleShape).clickable { onExit() }) {
            VAvatar(name = adminName, size = 38.dp)
        }
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = VTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.ink.copy(alpha = if (c.isNight) 0.12f else 0.06f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * RA-24: a tappable entry card that opens an existing, backend-backed screen
 * (Analytics dashboard or the at-risk cohort). Replaces the old `VComingSoon`
 * placeholders where the data feed already exists. Frozen V* primitives only.
 */
@Composable
private fun MetricEntryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    preview: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    VCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.teal.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = VTheme.type.bodyStrong.colored(c.ink))
                Text(description, style = VTheme.type.caption.colored(c.ink2))
            }
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        }
        if (preview != null) {
            Spacer(Modifier.height(12.dp))
            preview()
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
