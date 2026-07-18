package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.core.locale.StringKeys
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardOverview
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardSummary
import com.littlebridge.enrollplus.feature.admin.domain.model.DashboardActivity
import com.littlebridge.enrollplus.feature.admin.domain.model.DashboardAlert
import com.littlebridge.enrollplus.feature.admin.domain.model.DashboardQuickAction
import com.littlebridge.enrollplus.feature.admin.domain.model.SetupProgress
import com.littlebridge.enrollplus.feature.admin.domain.model.SetupProgressStep
import com.littlebridge.enrollplus.feature.admin.presentation.AcademicCalendarPlatformViewModel
import com.littlebridge.enrollplus.feature.admin.presentation.PinnedScreensViewModel
import com.littlebridge.enrollplus.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.enrollplus.feature.admin.presentation.SchoolSetupViewModel
import com.littlebridge.enrollplus.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.enrollplus.platform.rememberNotificationPermissionLauncher
import com.littlebridge.enrollplus.presentation.PermissionViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackOnlineBanner
import com.littlebridge.enrollplus.ui.v2.components.VConfirmDialog
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VOfflineBanner
import com.littlebridge.enrollplus.ui.v2.components.VPullRefresh
import com.littlebridge.enrollplus.ui.v2.locale.appString
import com.littlebridge.enrollplus.ui.v2.screens.SkeletonDashboard
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SchoolHomeScreenV2(
    modifier: Modifier = Modifier,
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenPews: () -> Unit = {},
    onOpenTransport: () -> Unit = {},
    onOpenReportPublish: () -> Unit = {},
    onOpenReportEffectiveness: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onCreateAnnouncement: () -> Unit = {},
    onOpenApprovals: () -> Unit = {},
    onOpenPinnedScreen: (String) -> Unit = {},
    onExit: () -> Unit = {},
    viewModel: SchoolDashboardViewModel = koinViewModel(),
    setupViewModel: SchoolSetupViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    calendarViewModel: AcademicCalendarPlatformViewModel = koinViewModel(),
    permissionVm: PermissionViewModel = koinViewModel(),
    pinnedVm: PinnedScreensViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val notifications by notificationsViewModel.state.collectAsStateV2()
    val setupState by setupViewModel.state.collectAsStateV2()
    var analyticsType by remember { mutableStateOf<String?>(null) }
    var commandPaletteVisible by remember { mutableStateOf(false) }

    val showRationale by permissionVm.showNotificationRationale.collectAsStateV2()
    val launchPermission by permissionVm.launchPermissionRequest.collectAsStateV2()
    val permissionLauncher = rememberNotificationPermissionLauncher(permissionVm::onPermissionResult)
    androidx.compose.runtime.LaunchedEffect(launchPermission) {
        if (launchPermission) {
            permissionVm.consumeLaunchPermissionRequest()
            permissionLauncher.launch()
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        permissionVm.checkNotificationPermission()
        setupViewModel.load(forceRefresh = true)
    }
    androidx.compose.runtime.LaunchedEffect(state.pinnedScreens) { pinnedVm.setInitial(state.pinnedScreens) }

    VPullRefresh(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh(); setupViewModel.load(forceRefresh = true); calendarViewModel.refresh() },
        modifier = modifier.fillMaxSize().background(AdminHomeTokens.Cream),
    ) {
        Box(Modifier.fillMaxSize()) {
            VStateHost(
                loading = state.isLoading && state.overview == null,
                error = state.errorMessage.takeIf { state.overview == null },
                isEmpty = state.overview == null && !state.isLoading && state.errorMessage == null,
                emptyTitle = "Dashboard unavailable",
                emptyBody = "Pull to refresh when your school data is ready.",
                onRetry = viewModel::refresh,
                skeleton = { SkeletonDashboard() },
                modifier = Modifier.fillMaxSize(),
            ) {
                val overview = state.overview ?: return@VStateHost
                PremiumAdminHome(
                    overview = overview,
                    summary = state.summary,
                    analytics = state.analytics,
                    activity = state.activity,
                    adminName = state.adminName,
                    unreadCount = notifications.unreadCount,
                    setupProgress = setupState.progress?.takeUnless { it.setupComplete },
                    onSetupStep = onOpenPinnedScreen,
                    onNotifications = onOpenNotifications,
                    onSearch = { commandPaletteVisible = true },
                    onKpi = { type -> analyticsType = type; viewModel.loadHomeAnalytics(type) },
                    onAlert = { alert -> routeAlert(alert, onOpenPinnedScreen, onOpenApprovals, onOpenEvents) },
                    onQuickAction = { action -> routeQuickAction(action.id, onOpenPinnedScreen, onCreateAnnouncement, onOpenTransport, onOpenReportPublish, onOpenAnalytics) },
                    onActivity = { row -> routeActivity(row, onOpenPinnedScreen, onOpenNotifications) },
                    onAllAlerts = onOpenNotifications,
                    onAllTools = { onOpenPinnedScreen("tab_settings") },
                    onAllActivity = onOpenNotifications,
                    onClassPerformance = { onOpenPinnedScreen("overlay_class_performance") },
                )
            }
            if (state.isOffline) VOfflineBanner(true)
            if (!state.isOffline && state.isStale) VBackOnlineBanner()
        }
    }

    AdminAnalyticsOverlay(
        visible = analyticsType != null,
        data = state.homeAnalytics?.takeIf { it.type == analyticsType },
        loading = state.isHomeAnalyticsLoading,
        error = state.homeAnalyticsError,
        onSelectFilter = { filter -> analyticsType?.let { viewModel.loadHomeAnalytics(it, filter) } },
        onDismiss = { analyticsType = null; viewModel.clearHomeAnalytics() },
    )

    VConfirmDialog(
        visible = showRationale,
        title = appString(StringKeys.HOME_NOTIF_RATIONALE_TITLE),
        message = appString(StringKeys.HOME_NOTIF_RATIONALE_MSG),
        confirmLabel = appString(StringKeys.HOME_NOTIF_ENABLE),
        onConfirm = permissionVm::requestNotificationPermission,
        onDismiss = permissionVm::declineNotifications,
        cancelLabel = appString(StringKeys.HOME_NOTIF_NOT_NOW),
        icon = VIcons.Bell,
    )
    HomeCommandPalette(visible = commandPaletteVisible, onDismiss = { commandPaletteVisible = false }, onSelect = onOpenPinnedScreen)
}

@Composable
private fun PremiumAdminHome(
    overview: AdminDashboardOverview,
    summary: AdminDashboardSummary?,
    analytics: AdminDashboardAnalytics?,
    activity: AdminDashboardActivity?,
    adminName: String,
    unreadCount: Int,
    setupProgress: SetupProgress?,
    onSetupStep: (String) -> Unit,
    onNotifications: () -> Unit,
    onSearch: () -> Unit,
    onKpi: (String) -> Unit,
    onAlert: (DashboardAlert) -> Unit,
    onQuickAction: (DashboardQuickAction) -> Unit,
    onActivity: (DashboardActivity) -> Unit,
    onAllAlerts: () -> Unit,
    onAllTools: () -> Unit,
    onAllActivity: () -> Unit,
    onClassPerformance: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 24.dp).padding(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PremiumHeader(overview, adminName, unreadCount, onNotifications, onSearch)
        HeroCard(overview, summary)
        setupProgress?.let { progress ->
            SchoolSetupChecklistCard(progress = progress, onStepClick = onSetupStep)
        }
        KeyMetrics(overview, summary, analytics, onKpi)
        OperationsDashboard(overview, summary, analytics, activity, onAlert, onAllAlerts)
        summary?.quickActions?.filter { it.enabled }?.takeIf { it.isNotEmpty() }?.let {
            QuickActions(it, onQuickAction, onAllTools)
        }
        activity?.activities?.takeIf { it.isNotEmpty() }?.let {
            RecentActivity(it, onActivity, onAllActivity)
        }
        analytics?.classPerformance?.topClasses?.takeIf { it.isNotEmpty() }?.let {
            ClassPerformance(analytics, onClassPerformance)
        }
    }
}

@Composable
private fun PremiumHeader(
    overview: AdminDashboardOverview,
    fallbackName: String,
    unreadCount: Int,
    onNotifications: () -> Unit,
    onSearch: () -> Unit,
) {
    val name = overview.header.adminName.ifBlank { fallbackName }
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enroll+", color = AdminHomeTokens.Violet, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color.White)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onNotifications),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Bell, "Notifications", tint = AdminHomeTokens.Ink, modifier = Modifier.size(18.dp))
                if (unreadCount > 0) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(7.dp).clip(CircleShape).background(AdminHomeTokens.Coral).border(1.5.dp, Color.White, CircleShape))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("${overview.header.greeting.ifBlank { "Hi" }} $name", color = AdminHomeTokens.Violet, fontSize = 15.4.sp, fontWeight = FontWeight.SemiBold)
        Text("here's your overview", color = AdminHomeTokens.Ink, fontSize = 26.4.sp, fontWeight = FontWeight.ExtraBold)
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).clip(AdminHomeTokens.Lg).background(Color.White)
                .border(1.dp, AdminHomeTokens.Line, AdminHomeTokens.Lg)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onSearch)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(VIcons.Search, null, tint = AdminHomeTokens.Ink3, modifier = Modifier.size(18.dp))
            Text("Search students, staff, classes…", fontSize = 14.sp, color = AdminHomeTokens.Ink3)
        }
    }
}

@Composable
private fun HeroCard(overview: AdminDashboardOverview, summary: AdminDashboardSummary?) {
    val students = summary?.statistics?.students?.total ?: kpi(overview, "students")
    val staff = summary?.statistics?.teachers?.total ?: kpi(overview, "teachers")
    val classes = summary?.statistics?.classes?.total ?: 0
    val pending = kpi(overview, "approvals")
    val attendance = kpi(overview, "attendance")
    val fee = overview.feeAnalytics
    val target = fee.totalCollected + fee.pending
    val infinite = rememberInfiniteTransition(label = "heroShimmer")
    val shimmer by infinite.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "heroShimmerX",
    )
    Column(
        Modifier.fillMaxWidth().clip(AdminHomeTokens.Xxl)
            .background(Brush.linearGradient(listOf(Color(0xFF7B5FE8), AdminHomeTokens.Violet, AdminHomeTokens.VioletDark, AdminHomeTokens.VioletInk)))
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(overview.header.schoolName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                Text(
                    listOf(overview.header.academicYear.takeIf { it.isNotBlank() }?.let { "AY $it" }, overview.header.currentTerm.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
                    color = Color.White.copy(alpha = .55f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                )
            }
            Row(
                Modifier.clip(AdminHomeTokens.Full).background(Color.White.copy(alpha = .10f)).border(1.dp, Color.White.copy(alpha = .12f), AdminHomeTokens.Full).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                Text("$attendance% present", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(AdminHomeTokens.Lg).background(Color.White.copy(alpha = .04f)).border(1.dp, Color.White.copy(alpha = .08f), AdminHomeTokens.Lg).padding(1.dp),
        ) {
            listOf("Students" to students, "Staff" to staff, "Classes" to classes, "Pending" to pending).forEach { (label, value) ->
                Column(Modifier.weight(1f).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatCount(value), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(label.uppercase(), color = Color.White.copy(alpha = .45f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (fee.available) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fee collection", color = Color.White.copy(alpha = .65f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${money(fee.totalCollected)} / ${money(target)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(AdminHomeTokens.Full).background(Color.White.copy(alpha = .10f))) {
                Box(
                    Modifier.fillMaxWidth(fee.collectionRate.coerceIn(0, 100) / 100f).height(6.dp).clip(AdminHomeTokens.Full)
                        .background(Brush.linearGradient(listOf(Color(0xFFC9B8FF), Color.White, Color(0xFFC9B8FF)), start = Offset(shimmer * 200f, 0f), end = Offset(shimmer * 200f + 220f, 0f))),
                )
            }
        }
    }
}

private data class SetupStepVisual(
    val key: String,
    val label: String,
    val route: String,
    val icon: ImageVector,
)

private val setupStepVisuals = listOf(
    SetupStepVisual("add_teachers", "Add Teachers", "setup_add_teachers", VIcons.UserPlus),
    SetupStepVisual("add_students", "Add Students", "setup_add_students", VIcons.UsersGroup),
    SetupStepVisual("add_subjects", "Add Subjects", "setup_add_subjects", VIcons.BookOpen),
    SetupStepVisual("create_classes", "Create Classes", "setup_create_classes", VIcons.School),
    SetupStepVisual("create_timetable", "Create Timetable", "setup_create_timetable", VIcons.Calendar),
)

@Composable
private fun SchoolSetupChecklistCard(
    progress: SetupProgress,
    onStepClick: (String) -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.completionPercent.coerceIn(0, 100).toFloat(),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "schoolSetupProgress",
    )
    val stepsByKey = progress.steps.associateBy { it.step }
    val firstPending = setupStepVisuals.firstOrNull { stepsByKey[it.key]?.status != "done" }

    Column(
        Modifier.fillMaxWidth().shadow(12.dp, AdminHomeTokens.Xl, ambientColor = Color.Black.copy(alpha = .05f), spotColor = Color.Black.copy(alpha = .07f))
            .clip(AdminHomeTokens.Xl).background(AdminHomeTokens.Card)
            .border(1.dp, AdminHomeTokens.Line, AdminHomeTokens.Xl).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SetupProgressRing(animatedProgress, Modifier.size(42.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("School Setup", color = AdminHomeTokens.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${progress.completedSteps} of ${progress.totalSteps} completed",
                    color = AdminHomeTokens.Ink3,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(AdminHomeTokens.VioletSoft)
                    .clickable(
                        enabled = firstPending != null,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { firstPending?.let { onStepClick(it.route) } },
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ChevronRight, "Continue school setup", tint = AdminHomeTokens.Violet, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        setupStepVisuals.forEach { visual ->
            val step = stepsByKey[visual.key] ?: SetupProgressStep(visual.key, "pending", 0)
            SetupChecklistRow(visual, step) { onStepClick(visual.route) }
        }
    }
}

@Composable
private fun SetupProgressRing(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            drawArc(AdminHomeTokens.LineSoft, -90f, 360f, false, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    AdminHomeTokens.Violet,
                    -90f,
                    progress * 3.6f,
                    false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Text("${progress.roundToInt()}%", color = AdminHomeTokens.Violet, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SetupChecklistRow(
    visual: SetupStepVisual,
    step: SetupProgressStep,
    onClick: () -> Unit,
) {
    val done = step.status == "done"
    val active = step.status == "in_progress"
    val iconColor = when {
        done -> AdminHomeTokens.Mint
        active -> AdminHomeTokens.Gold
        else -> AdminHomeTokens.Violet
    }
    val badgeBackground = when {
        done -> AdminHomeTokens.MintSoft
        active -> AdminHomeTokens.GoldSoft
        else -> AdminHomeTokens.SurfaceTint
    }
    val badgeTextColor = when {
        done -> AdminHomeTokens.Success
        active -> Color(0xFF8A6100)
        else -> AdminHomeTokens.Ink3
    }
    val statusText = when (visual.key) {
        "create_classes" -> "${step.count} created"
        "create_timetable" -> "${step.count} periods"
        else -> "${step.count} added"
    }

    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AdminHomeTokens.Md)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(28.dp).clip(AdminHomeTokens.Sm).background(iconColor), contentAlignment = Alignment.Center) {
            Icon(visual.icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Text(
            visual.label,
            color = if (done) AdminHomeTokens.Ink3 else AdminHomeTokens.Ink2,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            statusText,
            color = badgeTextColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(AdminHomeTokens.Full).background(badgeBackground).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun KeyMetrics(
    overview: AdminDashboardOverview,
    summary: AdminDashboardSummary?,
    analytics: AdminDashboardAnalytics?,
    onKpi: (String) -> Unit,
) {
    val fee = overview.feeAnalytics
    val attendance = kpi(overview, "attendance")
    val admissions = summary?.statistics?.students?.newAdmissions ?: 0
    val staffTotal = summary?.statistics?.teachers?.total ?: 0
    val staffActive = summary?.statistics?.teachers?.active ?: 0
    val staffRate = if (staffTotal > 0) staffActive * 100 / staffTotal else 0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminSectionHeader("Key Metrics", "This month")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard("Fee Collection", money(fee.totalCollected), "${fee.collectionRate}% of ${money(fee.totalCollected + fee.pending)} target", VIcons.Wallet, AdminHomeTokens.Violet, "fee", onKpi, Modifier.weight(1f)) {
                AdminLinearProgress(fee.collectionRate.toDouble(), AdminHomeTokens.Violet, Modifier.fillMaxWidth())
            }
            KpiCard("Attendance", "$attendance%", "Latest recorded attendance", VIcons.Check, AdminHomeTokens.Mint, "attendance", onKpi, Modifier.weight(1f)) {
                AdminRing(attendance.toDouble(), AdminHomeTokens.Mint, Modifier.size(28.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard("Admissions", formatCount(admissions), "New in the last 30 days", VIcons.UsersGroup, AdminHomeTokens.Sky, "admissions", onKpi, Modifier.weight(1f)) {
                AdminSparkline(analytics?.studentGrowth?.values?.map(Int::toDouble).orEmpty(), AdminHomeTokens.Sky, Modifier.fillMaxWidth().height(24.dp))
            }
            KpiCard("Staff & Teachers", "$staffRate%", "$staffActive of $staffTotal active", VIcons.User, AdminHomeTokens.Gold, "staff", onKpi, Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AdminLinearProgress(staffRate.toDouble(), AdminHomeTokens.Mint, Modifier.fillMaxWidth(), 3)
                    AdminLinearProgress(summary?.teacherInsight?.assignmentCoverage?.toDouble() ?: 0.0, AdminHomeTokens.Sky, Modifier.fillMaxWidth(), 3)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    supporting: String,
    icon: ImageVector,
    color: Color,
    type: String,
    onKpi: (String) -> Unit,
    modifier: Modifier,
    infographic: @Composable () -> Unit,
) {
    AdminPremiumCard(modifier, onClick = { onKpi(type) }, padding = 13) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(24.dp).clip(AdminHomeTokens.Sm).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
            Text(label, color = AdminHomeTokens.Ink2, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, color = AdminHomeTokens.Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { infographic() }
        }
        Spacer(Modifier.height(6.dp))
        Text(supporting, color = AdminHomeTokens.Ink3, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OperationsDashboard(
    overview: AdminDashboardOverview,
    summary: AdminDashboardSummary?,
    analytics: AdminDashboardAnalytics?,
    activity: AdminDashboardActivity?,
    onAlert: (DashboardAlert) -> Unit,
    onAllAlerts: () -> Unit,
) {
    val alerts = activity?.alerts.orEmpty()
    val growth = analytics?.studentGrowth
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminSectionHeader("Operations")
        Column(Modifier.fillMaxWidth().clip(AdminHomeTokens.Xl).background(Color.White).border(1.dp, AdminHomeTokens.Line, AdminHomeTokens.Xl)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Priority Alerts", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AdminHomeTokens.Ink)
                Text("View all", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminHomeTokens.Violet, modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onAllAlerts))
            }
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.take(4).forEachIndexed { index, alert -> OperationAlert(alert, index, onAlert) }
                if (alerts.isEmpty()) Text("No priority alerts", color = AdminHomeTokens.Ink3, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp).height(1.dp).background(AdminHomeTokens.Line))
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(formatCount(summary?.statistics?.students?.total ?: kpi(overview, "students")), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AdminHomeTokens.Ink)
                        Text("Total enrolled students", fontSize = 11.sp, color = AdminHomeTokens.Ink2)
                    }
                    val start = growth?.values?.firstOrNull() ?: 0
                    val end = growth?.values?.lastOrNull() ?: 0
                    val delta = if (start > 0) ((end - start) * 100.0 / start).roundToInt() else 0
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (delta > 0) "+$delta%" else "$delta%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminHomeTokens.Success)
                        Text("current period", fontSize = 10.sp, color = AdminHomeTokens.Ink3)
                    }
                }
                Spacer(Modifier.height(12.dp))
                AdminBarChart(
                    growth?.labels.orEmpty().zip(growth?.values.orEmpty()).map { com.littlebridge.enrollplus.feature.admin.domain.model.HomeAnalyticsPoint(it.first, it.second.toDouble()) },
                    AdminHomeTokens.Violet, true, Modifier.fillMaxWidth().height(82.dp),
                )
            }
        }
    }
}

@Composable
private fun OperationAlert(alert: DashboardAlert, index: Int, onClick: (DashboardAlert) -> Unit) {
    val colors = listOf(AdminHomeTokens.Violet, AdminHomeTokens.Coral, AdminHomeTokens.Gold, AdminHomeTokens.Sky)
    val color = colors[index % colors.size]
    Row(
        Modifier.fillMaxWidth().clip(AdminHomeTokens.Md).background(AdminHomeTokens.SurfaceTint)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick(alert) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(if (alert.type.equals("WARNING", true)) VIcons.AlertCircle else VIcons.Bell, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(alert.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminHomeTokens.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(alert.description, fontSize = 10.sp, color = AdminHomeTokens.Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(alert.priority.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(AdminHomeTokens.Full).background(color).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun QuickActions(actions: List<DashboardQuickAction>, onAction: (DashboardQuickAction) -> Unit, onAllTools: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminSectionHeader("Quick Actions", "All tools", onAllTools)
        actions.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { action -> QuickActionTile(action, onAction, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionTile(action: DashboardQuickAction, onAction: (DashboardQuickAction) -> Unit, modifier: Modifier) {
    val (icon, color) = when (action.id) {
        "ADD_STUDENT" -> VIcons.UsersGroup to AdminHomeTokens.Violet
        "ADD_STAFF" -> VIcons.User to AdminHomeTokens.Mint
        "COLLECT_FEES" -> VIcons.Wallet to AdminHomeTokens.Gold
        "ANNOUNCE" -> VIcons.Megaphone to AdminHomeTokens.Coral
        "TRANSPORT" -> VIcons.MapPin to AdminHomeTokens.Sky
        "REPORTS" -> VIcons.FileText to AdminHomeTokens.Violet
        "TIMETABLE" -> VIcons.Calendar to AdminHomeTokens.Mint
        else -> VIcons.Target to AdminHomeTokens.Gold
    }
    AdminPremiumCard(modifier, onClick = { onAction(action) }, padding = 11) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp)).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(action.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminHomeTokens.Ink, maxLines = 1)
                Text(action.subtitle, fontSize = 10.sp, color = AdminHomeTokens.Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentActivity(rows: List<DashboardActivity>, onActivity: (DashboardActivity) -> Unit, onAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminSectionHeader("Recent Activity", "View all", onAll)
        AdminPremiumCard(padding = 14) {
            rows.take(6).forEachIndexed { index, row ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onActivity(row) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val colors = listOf(AdminHomeTokens.Violet, AdminHomeTokens.Mint, AdminHomeTokens.Sky, AdminHomeTokens.Gold, AdminHomeTokens.Coral)
                    Box(Modifier.size(36.dp).clip(CircleShape).background(colors[index % colors.size]), contentAlignment = Alignment.Center) {
                        Text(initials(row.performedBy.ifBlank { row.title }), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(row.title, color = AdminHomeTokens.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(row.time, color = AdminHomeTokens.Ink3, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassPerformance(analytics: AdminDashboardAnalytics, onDetails: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminSectionHeader("Class Performance", "Details", onDetails)
        AdminPremiumCard(onClick = onDetails, padding = 20) {
            val colors = listOf(AdminHomeTokens.Violet, AdminHomeTokens.Mint, AdminHomeTokens.Sky, AdminHomeTokens.Gold, AdminHomeTokens.Coral)
            analytics.classPerformance.topClasses.take(6).forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.className, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdminHomeTokens.Ink)
                    Text("${item.score}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors[index % colors.size])
                }
                Spacer(Modifier.height(6.dp))
                AdminLinearProgress(item.score.toDouble(), colors[index % colors.size], Modifier.fillMaxWidth(), 6)
            }
        }
    }
}

private fun kpi(overview: AdminDashboardOverview, key: String): Int = overview.kpis.firstOrNull { it.key == key }?.value ?: 0
private fun formatCount(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()
private fun money(value: Double): String = when {
    value >= 10_000_000 -> "₹${trim(value / 10_000_000)}Cr"
    value >= 100_000 -> "₹${trim(value / 100_000)}L"
    value >= 1_000 -> "₹${trim(value / 1_000)}K"
    else -> "₹${value.roundToInt()}"
}
private fun trim(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else ((value * 10).roundToInt() / 10.0).toString()
private fun initials(value: String): String = value.split(" ").filter(String::isNotBlank).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "A" }

private fun routeQuickAction(
    id: String,
    open: (String) -> Unit,
    announce: () -> Unit,
    transport: () -> Unit,
    reports: () -> Unit,
    analytics: () -> Unit,
) = when (id) {
    "ADD_STUDENT" -> open("overlay_admissions")
    "ADD_STAFF" -> open("tab_people")
    "COLLECT_FEES" -> open("overlay_fee_salary")
    "ANNOUNCE" -> announce()
    "TRANSPORT" -> transport()
    "REPORTS" -> reports()
    "TIMETABLE" -> open("overlay_classes_subjects")
    "ANALYTICS" -> analytics()
    else -> open("tab_settings")
}

private fun routeAlert(alert: DashboardAlert, open: (String) -> Unit, approvals: () -> Unit, events: () -> Unit) = when (alert.action) {
    "VIEW_ADMISSIONS" -> open("overlay_admissions")
    "ASSIGN_TEACHER" -> open("tab_people")
    "VIEW_APPROVALS" -> approvals()
    "VIEW_EVENTS" -> events()
    "VIEW_FEES" -> open("overlay_fee_salary")
    "VIEW_EXAMS" -> open("overlay_events")
    else -> open("overlay_notifications")
}

private fun routeActivity(row: DashboardActivity, open: (String) -> Unit, notifications: () -> Unit) = when {
    row.type.contains("ADMISSION", true) -> open("overlay_admissions")
    row.type.contains("FEE", true) || row.type.contains("PAY", true) -> open("overlay_fee_salary")
    row.type.contains("LEAVE", true) -> open("overlay_leave_requests")
    row.type.contains("RESULT", true) -> open("overlay_results")
    row.type.contains("ANNOUNCEMENT", true) || row.type.contains("EVENT", true) -> open("overlay_events")
    else -> notifications()
}
