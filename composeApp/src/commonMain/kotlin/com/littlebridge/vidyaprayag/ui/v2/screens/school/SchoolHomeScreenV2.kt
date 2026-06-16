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



    SchoolDashboardContent(

        modifier = modifier,

        adminName = adminName,

        progress = progress,

        steps = steps,

        onboardingStatus = onboardingStatus,

        unreadCount = notifications.unreadCount,

        loading = loading,

        error = error,


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


                /*
                 HEADER
                 */
                AdminHeader(
                    name = adminName,

                    completed = completed,

                    unreadCount = unreadCount,


                    onNotifications = onOpenNotifications,


                    onExit = onExit
                )


                /*
                 MAIN HERO
                 */
                CampusHealthCard()


                /*
                 FOUR BIG NUMBERS
                 */
                DashboardMetricGrid(

                    students = 840,

                    teachers = 42,

                    classes = 24,

                    subjects = 36
                )


                /*
                 ANALYTICS
                 */
                AttendanceChartCard()


                /*
                 ACTION CENTER
                 */
                QuickActionGrid(

                    onAddTeacher = {},

                    onAddStudent = {},

                    onCreateClass = {},

                    onReports = {})


                /*
                 TEACHERS
                 */
                TeacherInsightCard(

                    assigned = 36,

                    pending = 6
                )


                /*
                 ACTIVITY
                 */
                ActivityTimeline(

                    activities = listOf(

                        ActivityItem(
                            "New teacher added", "Mathematics department", "10 min ago"
                        ),


                        ActivityItem(
                            "Attendance synced", "Class 8-A", "1 hour ago"
                        )

                    )
                )


                /*
                 EXISTING ANALYTICS
                 */
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
fun CampusHealthCard(
    modifier: Modifier = Modifier
) {


    val c = VTheme.colors



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

                            text = "Everything looks stable today",


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

                                "96%",


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
                            icon = VIcons.Users, value = "840", label = "Students"
                        )


                        Spacer(
                            Modifier.height(10.dp)
                        )


                        HealthMetric(
                            icon = VIcons.Users, value = "42", label = "Teachers"
                        )


                    }


                }


                /*
                MINI GRAPH
                 */


                AttendanceMiniGraph()


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
private fun AttendanceMiniGraph() {


    Canvas(

        modifier = Modifier

            .fillMaxWidth()

            .height(70.dp)

    ) {


        val points = listOf(
            0.65f, 0.72f, 0.68f, 0.84f, 0.78f, 0.92f, 0.96f
        )


        val path = Path()



        points.forEachIndexed { index, value ->


            val x = size.width * index / (points.size - 1)


            val y = size.height - (size.height * value)



            if (index == 0)

                path.moveTo(x, y)
            else

                path.lineTo(x, y)


        }



        drawPath(

            path = path,

            color = Color.White,

            )


    }

}

@Composable
fun DashboardMetricGrid(
    students: Int, teachers: Int, classes: Int, subjects: Int,

    modifier: Modifier = Modifier
) {


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

                value = students.toString(),

                change = "+12%",

                icon = VIcons.Users,

                progress = .82f

            )



            DashboardMetricCard(

                modifier = Modifier.weight(1f),

                title = "Teachers",

                value = teachers.toString(),

                change = "+4%",

                icon = VIcons.Users,

                progress = .64f

            )


        }




        Row(

            horizontalArrangement = Arrangement.spacedBy(14.dp)

        ) {


            DashboardMetricCard(

                modifier = Modifier.weight(1f),

                title = "Classes",

                value = classes.toString(),

                change = "+2",

                icon = VIcons.School,

                progress = .72f

            )




            DashboardMetricCard(

                modifier = Modifier.weight(1f),

                title = "Subjects",

                value = subjects.toString(),

                change = "Active",

                icon = VIcons.BookOpen,

                progress = .90f

            )


        }

    }

}

@Composable
private fun DashboardMetricCard(
    title: String,

    value: String,

    change: String,

    icon: ImageVector,

    progress: Float,


    modifier: Modifier = Modifier

) {


    val c = VTheme.colors



    VCard(

        modifier = modifier

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


                    style = VTheme.type.caption.colored(c.successInk)

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

                        "Last 7 months performance",

                        style = VTheme.type.caption.colored(c.ink2)

                    )


                }





                BoxPercentage()

            }






            AttendanceLineGraph()






            Row(

                modifier = Modifier.fillMaxWidth(),


                horizontalArrangement = Arrangement.SpaceBetween

            ) {


                listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul"
                ).forEach {


                    Text(

                        text = it,

                        style = VTheme.type.dataSm.colored(c.ink3)

                    )

                }


            }


        }


    }


}

@Composable
private fun BoxPercentage() {


    val c = VTheme.colors


    Box(

        modifier = Modifier

            .background(

                c.successInk.copy(
                    alpha = .12f
                ),

                RoundedCornerShape(50)

            )

            .padding(
                horizontal = 12.dp, vertical = 6.dp
            )

    ) {


        Text(

            "+4.2%",

            style = VTheme.type.caption.colored(c.successInk)

        )


    }


}

@Composable
private fun AttendanceLineGraph() {


    val c = VTheme.colors


    val points = listOf(
        .78f, .82f, .80f, .87f, .84f, .91f, .96f
    )




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


            val y = graphHeight - (graphHeight * value)



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


            val y = graphHeight - (graphHeight * value)



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

    assigned: Int,

    pending: Int,

    modifier: Modifier = Modifier

) {


    val c = VTheme.colors


    val total = assigned + pending


    val coverage = if (total == 0) 0f
    else assigned.toFloat() / total





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
            DEPARTMENT BREAKDOWN
             */


            DepartmentRow(
                "Mathematics", 8
            )


            DepartmentRow(
                "Science", 6
            )


            DepartmentRow(
                "English", 5
            )


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

        modifier = modifier.fillMaxWidth().clip(
            RoundedCornerShape(26.dp)
        )

            .background(
                c.card
            )

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