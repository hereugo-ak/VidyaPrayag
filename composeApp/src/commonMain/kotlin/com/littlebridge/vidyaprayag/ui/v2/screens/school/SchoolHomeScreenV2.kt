package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardSummary
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DashboardAlert
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DashboardAttendanceTrend
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DashboardCampusHealth
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DashboardCountTrend
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DashboardStatistics
import com.littlebridge.vidyaprayag.feature.admin.domain.model.DashboardTeacherInsight
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStep
import com.littlebridge.vidyaprayag.feature.admin.presentation.DashboardOnboardingStatus
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonDashboard
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VElevationLevel
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.vElevation
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SchoolHomeScreenV2(
    modifier: Modifier = Modifier,
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenPews: () -> Unit = {},
    onExit: () -> Unit = {},
    viewModel: SchoolDashboardViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
) {
    val adminName by viewModel.adminName.collectAsStateV2()
    val progress by viewModel.progress.collectAsStateV2()
    val steps by viewModel.steps.collectAsStateV2()
    val onboardingStatus by viewModel.onboardingStatus.collectAsStateV2()
    val loading by viewModel.isLoading.collectAsStateV2() 
    val error by viewModel.errorMessage.collectAsStateV2()
    val notifications by notificationsViewModel.state.collectAsStateV2() 
    val summary by viewModel.summary.collectAsStateV2() 
    val analytics by viewModel.analytics.collectAsStateV2() 
    val activity by viewModel.activity.collectAsStateV2()

    SchoolDashboardContent(
        modifier = modifier,
        adminName = adminName,
        progress = progress,
        steps = steps,
        onboardingStatus = onboardingStatus,
        unreadCount = notifications.unreadCount,
        loading = loading,
        error = error,
        summary = summary,
        analytics = analytics,
        activity = activity,
        onRetry = {
            viewModel.refresh()
        },
        onOpenNotifications = onOpenNotifications,
        onOpenAnalytics = onOpenAnalytics,
        onOpenPews = onOpenPews,
        onExit = onExit
    )
}

@Composable
private fun SchoolDashboardContent(
    modifier: Modifier = Modifier,
    adminName: String,
    progress: Float,
    steps: List<OnboardingStep>,
    onboardingStatus: DashboardOnboardingStatus,
    unreadCount: Int,
    loading: Boolean,
    error: String?,
    summary: AdminDashboardSummary?,
    analytics: AdminDashboardAnalytics?,
    activity: AdminDashboardActivity?,
    onRetry: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenPews: () -> Unit,
    onExit: () -> Unit,
) {
    val completed = onboardingStatus == DashboardOnboardingStatus.COMPLETED
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(
            rememberScrollState()
        ).padding(
            horizontal = 20.dp,
            vertical = 20.dp,
        ).padding(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        VStateHost(
            loading = loading,
            error = error,
            isEmpty = false,
            onRetry = onRetry,
            skeleton = {
                SkeletonDashboard()
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                AdminHeader(
                    name = adminName,
                    completed = completed,
                    unreadCount = unreadCount,
                    onNotifications = onOpenNotifications,
                    onExit = onExit
                )
                val alerts = activity?.alerts.orEmpty()
                if (alerts.isNotEmpty()) {
                    AlertsSection(alerts = alerts)
                }
                CampusHealthCard(
                    campusHealth = summary?.campusHealth,
                    students = summary?.statistics?.students?.total ?: 0,
                    teachers = summary?.statistics?.teachers?.total ?: 0,
                    attendanceTrend = analytics?.attendanceTrend?.values.orEmpty(),
                )
                DashboardMetricGrid(
                    statistics = summary?.statistics,
                    onStudentClick = onOpenPews,
                    onTeacherClick = onOpenPews,
                    onClassesClick ={},
                    onSubjectClick = {}
                )
                AttendanceChartCard(
                    attendanceTrend = analytics?.attendanceTrend,
                )
                
                QuickActionGrid(
                    onAddTeacher = onOpenPews,
                    onAddStudent = onOpenPews,
                    onCreateClass = {},
                    onReports = {}
                )
                TeacherInsightCard(
                    insight = summary?.teacherInsight,
                    modifier = Modifier.clickable(
                        enabled = true,
                        onClick = onOpenPews
                    )
                )
                ActivityTimeline(
                    activities = activity?.activities.orEmpty().map {
                        ActivityItem(
                            title = it.title,
                            subtitle = it.description,
                            time = it.time,
                        )
                    },
                    modifier = Modifier.clickable(
                        enabled = true,
                        onClick = onOpenPews
                    )
                )
                AnalyticsEntryCard(
                    onClick = onOpenAnalytics
                )
                PewsEntryCard(
                    onClick = onOpenPews
                )
            }
        }
    }
}

@Composable
fun AdminHeader(
    name: String, completed: Boolean, unreadCount: Int,

    onNotifications: () -> Unit,

    onExit: () -> Unit,

    modifier: Modifier = Modifier
) {


    val c = VTheme.colors



    Row(

        modifier = modifier

            .vElevation(
                VElevationLevel.Card, radius = 28.dp
            )

            .clip(
                RoundedCornerShape(28.dp)
            )

            .background(
                c.card
            )

            .padding(
                horizontal = 14.dp, vertical = 12.dp
            ),


        verticalAlignment = Alignment.CenterVertically,


        horizontalArrangement = Arrangement.spacedBy(14.dp)

    ) {


        /*
        SCHOOL ICON
        */


        Box(

            modifier = Modifier.size(48.dp)

                .clip(
                    RoundedCornerShape(18.dp)
                )

                .background(
                    c.teal.copy(
                        alpha = .15f
                    )
                ),


            contentAlignment = Alignment.Center

        ) {


            Icon(

                imageVector = VIcons.GraduationCap,


                contentDescription = null,


                tint = c.tealDeep,


                modifier = Modifier.size(24.dp)

            )

        }


        /*
        TITLE
        */


        Column(

            modifier = Modifier.weight(1f)

        ) {


            Text(

                text = "School Console",


                style = VTheme.type.h4.colored(c.ink)

            )



            Row(

                verticalAlignment = Alignment.CenterVertically,


                horizontalArrangement = Arrangement.spacedBy(7.dp)

            ) {


                Box(

                    modifier = Modifier.size(8.dp)

                        .clip(
                            CircleShape
                        )

                        .background(

                            if (completed) c.successInk
                            else c.warningInk

                        )

                )



                Text(

                    text = if (completed) "Campus live"
                    else "Setup in progress",


                    style = VTheme.type.caption.colored(c.ink2)

                )


            }


        }


        /*
        NOTIFICATION BUTTON
        */


        NotificationButton(

            count = unreadCount,

            onClick = onNotifications

        )


        /*
        PROFILE
        */


        Box(

            modifier = Modifier.clip(
                CircleShape
            ).clickable {
                onExit()
            }

        ) {

            VAvatar(

                name = name,

                size = 42.dp

            )

        }


    }


}

@Composable
private fun NotificationButton(
    count: Int,

    onClick: () -> Unit

) {

    val c = VTheme.colors



    Box(

        modifier = Modifier

            .size(42.dp)

            .clip(
                CircleShape
            )

            .background(
                c.ink.copy(
                    alpha = .06f
                )
            )

            .clickable {
                onClick()
            },


        contentAlignment = Alignment.Center

    ) {


        Icon(

            imageVector = VIcons.Bell,


            contentDescription = "Notifications",


            tint = c.ink,


            modifier = Modifier.size(20.dp)

        )





        if (count > 0) {


            Box(

                modifier = Modifier

                    .size(9.dp)

                    .clip(
                        CircleShape
                    )

                    .background(
                        c.danger
                    )

                    .align(
                        Alignment.TopEnd
                    )

            )

        }


    }

}

@Composable
fun AlertsSection(
    alerts: List<DashboardAlert>,
    modifier: Modifier = Modifier
) {

    if (alerts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // Surface the most urgent items first; keep the list short and scannable.
        val ordered = alerts.sortedBy { priorityRank(it.priority) }

        ordered.take(3).forEach { alert ->
            AlertCard(alert = alert)
        }

    }

}

/** Lower rank = higher urgency, used to sort alerts. */
private fun priorityRank(priority: String): Int = when (priority.uppercase()) {
    "HIGH" -> 0
    "MEDIUM" -> 1
    "LOW" -> 2
    else -> 3
}

@Composable
private fun AlertCard(
    alert: DashboardAlert
) {

    val c = VTheme.colors

    /*
     Colour + icon are driven by the alert type so WARNING / CRITICAL / INFO
     each read at a glance. Unknown types degrade gracefully to a neutral
     informational style instead of crashing or showing nothing.
     */
    val type = alert.type.uppercase()
    val accent = when (type) {
        "CRITICAL" -> c.dangerInk
        "WARNING" -> c.warningInk
        else -> c.tealDeep
    }
    val icon = when (type) {
        "CRITICAL" -> VIcons.AlertCircle
        "WARNING" -> VIcons.AlertTriangle
        else -> VIcons.Bell
    }

    VCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = alert.title.ifBlank { "Action needed" },
                    style = VTheme.type.bodyStrong.colored(c.ink)
                )

                if (alert.description.isNotBlank()) {
                    Text(
                        text = alert.description,
                        style = VTheme.type.caption.colored(c.ink2)
                    )
                }

            }

        }

    }

}

@Composable
fun CampusHealthCard(
    campusHealth: DashboardCampusHealth?,
    students: Int,
    teachers: Int,
    attendanceTrend: List<Int>,
    modifier: Modifier = Modifier
) {


    val c = VTheme.colors


    /*
     The hero "Present" figure is the attendance metric reported by the
     /summary endpoint (campusHealth.metrics keyed "attendance"). We fall
     back to any metric whose unit is "%" so a contract tweak still renders,
     and finally to a neutral "—" when the server has nothing to report.
     */
    val attendanceMetric = campusHealth?.metrics?.firstOrNull { it.key == "attendance" }
        ?: campusHealth?.metrics?.firstOrNull { it.unit.equals("percentage", true) || it.unit == "%" }

    // Normalise the server's unit token ("percentage") into a display suffix.
    val presentLabel = attendanceMetric?.let { "${it.value}${unitSuffix(it.unit)}" } ?: "—"

    val headerMessage = campusHealth?.message
        ?.takeIf { it.isNotBlank() }
        ?: when (campusHealth?.status) {
            "CRITICAL" -> "Needs attention today"
            "WATCH" -> "A few things to review"
            "HEALTHY" -> "Everything looks stable today"
            else -> "Campus overview"
        }

    /*
     Normalise the attendance-trend percentages (0..100) into 0..1 ratios for
     the mini graph. When the server returns nothing we keep a flat baseline
     so the card still reads as a calm, intentional empty state.
     */
    val graphPoints = if (attendanceTrend.isEmpty()) emptyList()
    else attendanceTrend.map { (it.coerceIn(0, 100)) / 100f }



    VCard(

        modifier = modifier.fillMaxWidth()

            .clip(
                RoundedCornerShape(28.dp)
            )

    ) {


        Box(

            modifier = Modifier

                .fillMaxWidth()

                .background(

                    Brush.linearGradient(

                        listOf(

                            c.tealDeep,

                            c.teal

                        )

                    )

                )

                .padding(22.dp)

        ) {


            Column(

                verticalArrangement = Arrangement.spacedBy(18.dp)

            ) {


                /*
                HEADER
                 */


                Row(

                    modifier = Modifier.fillMaxWidth(),


                    horizontalArrangement = Arrangement.SpaceBetween,


                    verticalAlignment = Alignment.CenterVertically

                ) {


                    Column {


                        Text(

                            text = "Campus Health",


                            style = VTheme.type.h3.colored(Color.White)

                        )


                        Text(

                            text = headerMessage,


                            style = VTheme.type.caption.colored(
                                Color.White.copy(
                                    alpha = .8f
                                )
                            )

                        )


                    }





                    Box(

                        modifier = Modifier

                            .size(52.dp)

                            .clip(
                                CircleShape
                            )

                            .background(
                                Color.White.copy(
                                    alpha = .15f
                                )
                            ),


                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = VIcons.TrendingUp,


                            contentDescription = null,


                            tint = Color.White,


                            modifier = Modifier.size(26.dp)

                        )


                    }


                }


                /*
                ATTENDANCE SCORE
                 */


                Row(

                    verticalAlignment = Alignment.CenterVertically,


                    horizontalArrangement = Arrangement.spacedBy(18.dp)

                ) {


                    Box(

                        modifier = Modifier.size(86.dp)

                            .clip(
                                CircleShape
                            )

                            .background(
                                Color.White.copy(
                                    alpha = .18f
                                )
                            ),


                        contentAlignment = Alignment.Center

                    ) {


                        Column(

                            horizontalAlignment = Alignment.CenterHorizontally

                        ) {


                            Text(

                                presentLabel,


                                style = VTheme.type.h2.colored(Color.White)

                            )


                            Text(

                                "Present",


                                style = VTheme.type.caption.colored(
                                    Color.White.copy(
                                        alpha = .8f
                                    )
                                )

                            )

                        }


                    }





                    Column {


                        HealthMetric(
                            icon = VIcons.Users, value = students.toString(), label = "Students"
                        )


                        Spacer(
                            Modifier.height(10.dp)
                        )


                        HealthMetric(
                            icon = VIcons.Users, value = teachers.toString(), label = "Teachers"
                        )


                    }


                }


                /*
                MINI GRAPH (only when the server reports a trend)
                 */


                if (graphPoints.size >= 2) {
                    AttendanceMiniGraph(points = graphPoints)
                }


            }


        }


    }


}

@Composable
private fun HealthMetric(
    icon: ImageVector,

    value: String,

    label: String

) {

    Row(

        verticalAlignment = Alignment.CenterVertically,


        horizontalArrangement = Arrangement.spacedBy(8.dp)

    ) {


        Icon(

            imageVector = icon,

            contentDescription = null,


            tint = Color.White.copy(
                alpha = .9f
            ),

            modifier = Modifier.size(18.dp)

        )



        Text(

            "$value $label",


            style = VTheme.type.body.colored(Color.White)

        )


    }

}

@Composable
private fun AttendanceMiniGraph(
    points: List<Float>
) {

    // Guard: a line needs at least two points; render nothing otherwise.
    if (points.size < 2) return

    Canvas(

        modifier = Modifier

            .fillMaxWidth()

            .height(70.dp)

    ) {


        val path = Path()



        points.forEachIndexed { index, value ->


            val x = size.width * index / (points.size - 1)


            // Clamp the ratio so an out-of-range value can never draw off-canvas.
            val y = size.height - (size.height * value.coerceIn(0f, 1f))



            if (index == 0)

                path.moveTo(x, y)
            else

                path.lineTo(x, y)


        }



        // BUGFIX: drawPath defaults to Fill, which rendered the trend as a
        // solid white wedge instead of a line. Draw it as a rounded stroke.
        drawPath(

            path = path,

            color = Color.White,

            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
            ),

            )


    }

}

@Composable
fun DashboardMetricGrid(
    statistics: DashboardStatistics?,
    modifier: Modifier = Modifier,
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit,
    onClassesClick: () -> Unit,
    onSubjectClick: () -> Unit
) {

    val students = statistics?.students
    val teachers = statistics?.teachers
    val classes = statistics?.classes
    val subjects = statistics?.subjects

    val studentTotal = students?.total ?: 0
    val teacherTotal = teachers?.total ?: 0
    val classTotal = classes?.total ?: 0
    val subjectTotal = subjects?.total ?: 0


    Column(

        modifier = modifier.fillMaxWidth(),

        verticalArrangement = Arrangement.spacedBy(14.dp)

    ) {


        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "Students",
                value = studentTotal.toString(),
                change = trendLabel(students?.trend),
                changePositive = isPositiveTrend(students?.trend),
                icon = VIcons.Users,
                // Progress reflects the active share of this metric.
                progress = activeRatio(students?.active, studentTotal),
                onClick = onStudentClick
            )

            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "Teachers",
                value = teacherTotal.toString(),
                change = trendLabel(teachers?.trend),
                changePositive = isPositiveTrend(teachers?.trend),
                icon = VIcons.Users,
                progress = activeRatio(teachers?.active, teacherTotal),
                onClick = onTeacherClick
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "Classes",
                value = classTotal.toString(),
                change = if ((classes?.active ?: 0) > 0) "${classes?.active} active" else "—",
                changePositive = true,
                icon = VIcons.School,
                progress = activeRatio(classes?.active, classTotal),
                onClick = onClassesClick
            )

            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = "Subjects",
                value = subjectTotal.toString(),
                change = if ((subjects?.active ?: 0) > 0) "${subjects?.active} active" else "—",
                changePositive = true,
                icon = VIcons.BookOpen,
                progress = activeRatio(subjects?.active, subjectTotal),
                onClick = onSubjectClick
            )
        }

    }

}

/** Formats a count trend (e.g. +12% / -3% / flat) into a compact label. */
private fun trendLabel(trend: DashboardCountTrend?): String {
    if (trend == null || trend.percentage == 0) return "—"
    val sign = when (trend.direction) {
        "up" -> "+"
        "down" -> "-"
        else -> ""
    }
    return "$sign${trend.percentage}%"
}

/** A trend is "good" (green) when flat or rising, "bad" (warning) when falling. */
private fun isPositiveTrend(trend: DashboardCountTrend?): Boolean =
    trend?.direction != "down"

/** Active-to-total ratio clamped to a sensible visible range for the bar. */
private fun activeRatio(active: Int?, total: Int): Float {
    if (total <= 0) return 0f
    val ratio = (active ?: total).toFloat() / total.toFloat()
    return ratio.coerceIn(0f, 1f)
}

/** Maps a server unit token to a compact display suffix (e.g. "percentage" → "%"). */
private fun unitSuffix(unit: String): String = when (unit.lowercase()) {
    "percentage", "percent", "%" -> "%"
    "", "count", "number" -> ""
    else -> " $unit"
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    change: String,
    changePositive: Boolean,
    icon: ImageVector,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val c = VTheme.colors
    VCard(
        modifier = modifier.clickable { onClick() }
    ) {


        Column(

            modifier = Modifier.padding(16.dp),


            verticalArrangement = Arrangement.spacedBy(10.dp)

        ) {


            Row(

                modifier = Modifier.fillMaxWidth(),


                horizontalArrangement = Arrangement.SpaceBetween,


                verticalAlignment = Alignment.CenterVertically

            ) {


                MetricIcon(

                    icon = icon

                )



                Text(

                    text = change,


                    style = VTheme.type.caption.colored(
                        if (changePositive) c.successInk else c.warningInk
                    )

                )


            }





            Spacer(
                Modifier.height(2.dp)
            )





            Text(

                value,


                style = VTheme.type.h2.colored(c.ink)

            )





            Text(

                title,


                style = VTheme.type.caption.colored(c.ink2)

            )






            ProgressLine(

                value = progress

            )


        }


    }


}

@Composable
private fun MetricIcon(
    icon: ImageVector
) {

    val c = VTheme.colors


    Column {


        Box(

            modifier = Modifier

                .size(42.dp)

                .clip(
                    RoundedCornerShape(14.dp)
                )

                .background(
                    c.teal.copy(
                        alpha = .14f
                    )
                ),


            contentAlignment = Alignment.Center

        ) {


            Icon(

                imageVector = icon,

                contentDescription = null,


                tint = c.tealDeep,


                modifier = Modifier.size(22.dp)

            )

        }

    }


}

@Composable
private fun ProgressLine(
    value: Float
) {

    val c = VTheme.colors



    Box(

        modifier = Modifier

            .fillMaxWidth()

            .height(6.dp)

            .clip(
                RoundedCornerShape(50)
            )

            .background(
                c.ink.copy(
                    alpha = .08f
                )
            )

    ) {


        Box(

            modifier = Modifier

                .fillMaxWidth(value)

                .height(6.dp)

                .clip(
                    RoundedCornerShape(50)
                )

                .background(
                    c.tealDeep
                )

        )

    }


}

@Composable
fun AttendanceChartCard(
    attendanceTrend: DashboardAttendanceTrend?,
    modifier: Modifier = Modifier
) {


    val c = VTheme.colors

    val values = attendanceTrend?.values.orEmpty()
    val labels = attendanceTrend?.labels.orEmpty()

    // Normalise reported percentages (0..100) into 0..1 ratios for the graph.
    val points = values.map { (it.coerceIn(0, 100)) / 100f }

    /*
     The badge shows the net change between the first and last reported
     points (e.g. "+4%"). Hidden when there isn't enough data to compare.
     */
    val delta = if (values.size >= 2) values.last() - values.first() else null

    val hasData = points.size >= 2


    VCard(

        modifier = modifier.fillMaxWidth()

    ) {


        Column(

            modifier = Modifier.padding(18.dp),


            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {


            Row(

                modifier = Modifier.fillMaxWidth(),


                horizontalArrangement = Arrangement.SpaceBetween,


                verticalAlignment = Alignment.CenterVertically

            ) {


                Column {


                    Text(

                        "Attendance Trend",

                        style = VTheme.type.h3.colored(c.ink)

                    )



                    Text(

                        if (hasData) "Last ${values.size} ${attendanceTrend?.period.orEmpty().ifEmpty { "period" }} performance"
                        else "No attendance data yet",

                        style = VTheme.type.caption.colored(c.ink2)

                    )


                }





                if (delta != null) {
                    BoxPercentage(delta = delta)
                }

            }




            if (hasData) {

                AttendanceLineGraph(points = points)


                Row(

                    modifier = Modifier.fillMaxWidth(),


                    horizontalArrangement = Arrangement.SpaceBetween

                ) {


                    labels.forEach {


                        Text(

                            text = it,

                            style = VTheme.type.dataSm.colored(c.ink3)

                        )

                    }


                }

            } else {

                Text(
                    "Attendance trends will appear once daily records are captured.",
                    style = VTheme.type.caption.colored(c.ink2)
                )

            }


        }


    }


}

@Composable
private fun BoxPercentage(delta: Int) {


    val c = VTheme.colors

    val positive = delta >= 0
    val tint = if (positive) c.successInk else c.warningInk
    val sign = if (positive) "+" else ""


    Box(

        modifier = Modifier

            .background(

                tint.copy(
                    alpha = .12f
                ),

                RoundedCornerShape(50)

            )

            .padding(
                horizontal = 12.dp, vertical = 6.dp
            )

    ) {


        Text(

            "$sign$delta%",

            style = VTheme.type.caption.colored(tint)

        )


    }


}

@Composable
private fun AttendanceLineGraph(
    points: List<Float>
) {


    val c = VTheme.colors


    // A line/area needs at least two points.
    if (points.size < 2) return




    Canvas(

        modifier = Modifier

            .fillMaxWidth()

            .height(180.dp)

    ) {


        val widthStep = size.width / (points.size - 1)


        val graphHeight = size.height - 20.dp.toPx()


        val path = Path()


        val fillPath = Path()



        points.forEachIndexed { index, value ->


            val x = index * widthStep


            val y = graphHeight - (graphHeight * value.coerceIn(0f, 1f))



            if (index == 0) {

                path.moveTo(
                    x, y
                )


                fillPath.moveTo(
                    x, size.height
                )


                fillPath.lineTo(
                    x, y
                )

            } else {


                path.lineTo(
                    x, y
                )


                fillPath.lineTo(
                    x, y
                )

            }


        }



        fillPath.lineTo(
            size.width, size.height
        )


        fillPath.close()



        drawPath(

            path = fillPath,

            color = c.teal.copy(
                alpha = .12f
            )

        )




        drawPath(

            path = path,

            color = c.tealDeep,


            style = Stroke(
                width = 4.dp.toPx(),

                cap = StrokeCap.Round

            )

        )





        points.forEachIndexed { index, value ->


            val x = index * widthStep


            val y = graphHeight - (graphHeight * value.coerceIn(0f, 1f))



            drawCircle(

                color = c.tealDeep,


                radius = 6.dp.toPx(),


                center = Offset(
                    x, y
                )

            )

        }


    }


}


@Composable
fun QuickActionGrid(

    onAddTeacher: () -> Unit,

    onAddStudent: () -> Unit,

    onCreateClass: () -> Unit,

    onReports: () -> Unit,


    modifier: Modifier = Modifier

) {


    Column(

        modifier = modifier.fillMaxWidth(),


        verticalArrangement = Arrangement.spacedBy(14.dp)

    ) {


        Text(

            "Quick Actions",

            style = VTheme.type.h3.colored(
                VTheme.colors.ink
            )

        )





        Row(

            horizontalArrangement = Arrangement.spacedBy(14.dp)

        ) {


            ActionCard(

                modifier = Modifier.weight(1f),


                title = "Add Teacher",


                description = "Create staff profile",


                icon = VIcons.Users,


                onClick = onAddTeacher

            )



            ActionCard(

                modifier = Modifier.weight(1f),


                title = "Add Student",


                description = "New admission",


                icon = VIcons.Users,


                onClick = onAddStudent

            )

        }





        Row(

            horizontalArrangement = Arrangement.spacedBy(14.dp)

        ) {


            ActionCard(

                modifier = Modifier.weight(1f),


                title = "Create Class",


                description = "Setup classroom",


                icon = VIcons.School,


                onClick = onCreateClass

            )





            ActionCard(

                modifier = Modifier.weight(1f),


                title = "Reports",


                description = "View analytics",


                icon = VIcons.AlertTriangle,


                onClick = onReports

            )


        }


    }


}

@Composable
private fun ActionCard(

    title: String,

    description: String,

    icon: ImageVector,


    onClick: () -> Unit,


    modifier: Modifier = Modifier

) {


    val c = VTheme.colors



    VCard(

        modifier = modifier

            .clip(
                RoundedCornerShape(22.dp)
            )

            .clickable {
                onClick()
            }

    ) {


        Column(

            modifier = Modifier.padding(16.dp),


            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {


            IconContainer(

                icon = icon

            )





            Text(

                title,


                style = VTheme.type.bodyStrong.colored(c.ink)

            )





            Text(

                description,


                style = VTheme.type.caption.colored(c.ink2)

            )


        }


    }


}

@Composable
private fun IconContainer(

    icon: ImageVector

) {


    val c = VTheme.colors



    Box(

        modifier = Modifier

            .size(44.dp)

            .clip(
                RoundedCornerShape(14.dp)
            )

            .background(
                c.teal.copy(
                    alpha = .15f
                )
            ),


        contentAlignment = Alignment.Center

    ) {


        Icon(

            imageVector = icon,


            contentDescription = null,


            tint = c.tealDeep,


            modifier = Modifier.size(22.dp)

        )


    }


}


@Composable
fun TeacherInsightCard(

    insight: DashboardTeacherInsight?,

    modifier: Modifier = Modifier

) {


    val c = VTheme.colors


    val assigned = insight?.assignedTeachers ?: 0

    val pending = insight?.pendingAssignment ?: 0

    val total = insight?.totalTeachers ?: (assigned + pending)


    /*
     Prefer the server-computed coverage (already a 0..100 percentage). Fall
     back to deriving it locally so an older payload still renders sanely.
     */
    val coverage = when {
        insight != null && insight.assignmentCoverage > 0 ->
            (insight.assignmentCoverage.coerceIn(0, 100)) / 100f
        total > 0 -> (assigned.toFloat() / total).coerceIn(0f, 1f)
        else -> 0f
    }

    val departments = insight?.departments.orEmpty()





    VCard(

        modifier = modifier.fillMaxWidth()

    ) {


        Column(

            modifier = Modifier.padding(18.dp),


            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {


            /*
             HEADER
             */


            Row(

                modifier = Modifier.fillMaxWidth(),


                verticalAlignment = Alignment.CenterVertically

            ) {


                BoxIcon()



                Spacer(
                    Modifier.size(12.dp)
                )



                Column(

                    modifier = Modifier.weight(1f)

                ) {


                    Text(

                        "Teacher Insights",

                        style = VTheme.type.h3.colored(c.ink)

                    )


                    Text(

                        "Staff assignment overview",

                        style = VTheme.type.caption.colored(c.ink2)

                    )

                }


            }


            /*
             SUMMARY NUMBERS
             */


            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp)

            ) {


                TeacherMetric(

                    modifier = Modifier.weight(1f),


                    title = "Assigned",


                    value = assigned.toString(),


                    positive = true

                )



                TeacherMetric(

                    modifier = Modifier.weight(1f),


                    title = "Pending",


                    value = pending.toString(),


                    positive = false

                )


            }


            /*
             COVERAGE
             */



            Column {


                Row(

                    modifier = Modifier.fillMaxWidth(),


                    horizontalArrangement = Arrangement.SpaceBetween

                ) {


                    Text(

                        "Assignment coverage",

                        style = VTheme.type.caption.colored(c.ink2)

                    )


                    Text(

                        "${(coverage * 100).toInt()}%",

                        style = VTheme.type.caption.colored(c.tealDeep)

                    )

                }



                Spacer(
                    Modifier.height(8.dp)
                )



                CoverageBar(
                    progress = coverage
                )

            }


            /*
            DEPARTMENT BREAKDOWN (server-driven; honest empty state)
             */


            if (departments.isEmpty()) {

                Text(
                    "No departments recorded yet",
                    style = VTheme.type.caption.colored(c.ink2)
                )

            } else {

                departments.forEach { dept ->
                    DepartmentRow(
                        name = dept.name,
                        count = dept.teacherCount
                    )
                }

            }


        }

    }


}


@Composable
private fun BoxIcon() {


    val c = VTheme.colors



    Box(

        modifier = Modifier

            .size(44.dp)

            .clip(
                RoundedCornerShape(14.dp)
            )

            .background(
                c.teal.copy(
                    alpha = .15f
                )
            ),


        contentAlignment = Alignment.Center

    ) {

        Icon(

            imageVector = VIcons.Users,


            contentDescription = null,


            tint = c.tealDeep,


            modifier = Modifier.size(22.dp)

        )

    }


}


@Composable
private fun TeacherMetric(

    title: String,

    value: String,

    positive: Boolean,


    modifier: Modifier = Modifier

) {


    val c = VTheme.colors



    Column(

        modifier = modifier

            .clip(
                RoundedCornerShape(16.dp)
            )

            .background(

                if (positive)

                    c.successInk.copy(
                        alpha = .10f
                    )
                else

                    c.warningInk.copy(
                        alpha = .10f
                    )

            )

            .padding(14.dp)


    ) {


        Text(

            value,


            style = VTheme.type.h2.colored(

                if (positive) c.successInk
                else c.warningInk

            )

        )



        Text(

            title,


            style = VTheme.type.caption.colored(c.ink2)

        )


    }

}


@Composable
private fun CoverageBar(

    progress: Float

) {

    val c = VTheme.colors



    Box(

        modifier = Modifier

            .fillMaxWidth()

            .height(8.dp)

            .clip(
                RoundedCornerShape(50)
            )

            .background(
                c.ink.copy(
                    alpha = .08f
                )
            )

    ) {


        Box(

            modifier = Modifier

                .fillMaxWidth(progress)

                .height(8.dp)

                .clip(
                    RoundedCornerShape(50)
                )

                .background(
                    c.tealDeep
                )

        )

    }

}


@Composable
private fun DepartmentRow(

    name: String,

    count: Int

) {


    val c = VTheme.colors



    Row(

        modifier = Modifier.fillMaxWidth(),


        horizontalArrangement = Arrangement.SpaceBetween

    ) {


        Text(

            name,


            style = VTheme.type.body.colored(c.ink)

        )



        Text(

            "$count teachers",


            style = VTheme.type.caption.colored(c.ink2)

        )


    }

}


data class ActivityItem(
    val title: String, val subtitle: String, val time: String
)


@Composable
fun ActivityTimeline(

    activities: List<ActivityItem>,

    modifier: Modifier = Modifier

) {

    val c = VTheme.colors



    VCard(

        modifier = modifier.fillMaxWidth()

    ) {

        Column(

            modifier = Modifier.padding(18.dp),


            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {


            Text(

                "Recent Activity",

                style = VTheme.type.h3.colored(c.ink)

            )



            Text(

                "Latest school operations",

                style = VTheme.type.caption.colored(c.ink2)

            )



            Spacer(
                Modifier.height(4.dp)
            )




            if (activities.isEmpty()) {


                Text(

                    "No activity recorded",

                    style = VTheme.type.caption.colored(c.ink2)

                )


            } else {


                activities.take(6).forEachIndexed { index, item ->


                    TimelineItem(

                        activity = item,

                        isLast = index == activities.lastIndex

                    )


                }


            }


        }


    }


}


@Composable
private fun TimelineItem(

    activity: ActivityItem,

    isLast: Boolean

) {

    val c = VTheme.colors



    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.Top

    ) {


        Column(

            horizontalAlignment = Alignment.CenterHorizontally

        ) {


            Box(

                modifier = Modifier

                    .size(12.dp)

                    .background(
                        c.tealDeep, CircleShape
                    )

            )



            if (!isLast) {


                Spacer(

                    Modifier.height(48.dp)

                )


            }


        }





        Spacer(
            Modifier.size(14.dp)
        )





        Column(

            modifier = Modifier.weight(1f),

            verticalArrangement = Arrangement.spacedBy(4.dp)

        ) {


            Text(

                activity.title,

                style = VTheme.type.bodyStrong.colored(c.ink)

            )



            Text(

                activity.subtitle,

                style = VTheme.type.caption.colored(c.ink2)

            )



            Text(

                activity.time,

                style = VTheme.type.dataSm.colored(c.ink3)

            )



            if (!isLast) {

                HorizontalDivider(

                    modifier = Modifier.padding(
                        top = 12.dp
                    )

                )

            }


        }


    }


}


@Composable
fun AnalyticsEntryCard(

    onClick: () -> Unit,

    modifier: Modifier = Modifier

) {

    DashboardFeatureCard(

        title = "School Analytics",

        description = "View attendance, academic performance and growth insights",


        icon = VIcons.AlertTriangle,


        button = "Explore Analytics",


        onClick = onClick,


        modifier = modifier

    )


}


@Composable
fun PewsEntryCard(

    onClick: () -> Unit,

    modifier: Modifier = Modifier

) {

    DashboardFeatureCard(

        title = "Student Risk Monitor",


        description = "Identify students requiring attention early",


        icon = VIcons.AlertCircle,


        button = "Open Monitor",


        onClick = onClick,


        modifier = modifier

    )

}


@Composable
private fun DashboardFeatureCard(

    title: String,

    description: String,

    icon: ImageVector,


    button: String,


    onClick: () -> Unit,


    modifier: Modifier

) {


    val c = VTheme.colors




    VCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .background(c.card)
    ) {


        Row(

            modifier = Modifier

                .padding(18.dp),


            verticalAlignment = Alignment.CenterVertically,


            horizontalArrangement = Arrangement.spacedBy(16.dp)

        ) {


            IconBox(

                icon = icon

            )






            Column(

                modifier = Modifier.weight(1f),


                verticalArrangement = Arrangement.spacedBy(6.dp)

            ) {


                Text(

                    title,


                    style = VTheme.type.h4.colored(c.ink)

                )





                Text(

                    description,


                    style = VTheme.type.caption.colored(c.ink2)

                )





                Text(

                    button + "  →",


                    style = VTheme.type.bodyStrong.colored(c.tealDeep)

                )


            }


        }


    }


}


@Composable
private fun IconBox(

    icon: ImageVector

) {


    val c = VTheme.colors



    Box(

        modifier = Modifier

            .size(52.dp)

            .clip(
                RoundedCornerShape(18.dp)
            )

            .background(
                c.teal.copy(
                    alpha = .15f
                )
            ),


        contentAlignment = Alignment.Center

    ) {


        Icon(

            imageVector = icon,


            contentDescription = null,


            tint = c.tealDeep,


            modifier = Modifier.size(26.dp)

        )


    }

}