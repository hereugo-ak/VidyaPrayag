package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme

/** Full-screen overlays the admin portal can push above its tab content. */
private enum class SchoolOverlay {
    None,
    Notifications,
    Calendar,
    Messages,
    LeaveRequests,
    LinkRequests,
    AdmissionsCRM,
    Results,
    SchedulePTM,
    DailyAttendance,
    ClassPerformance,
    TeacherPerformance,
    AnalyticsDashboard,
    EditProfile,
    StudentRoster,
    StudentProfile,
    TeacherProfile,
}

/**
 * SchoolPortalV2 — the 5-tab admin shell, translated from Admin.tsx.
 *
 * Bottom nav: Home · People · Records · Comms · Settings. Each leaf is the corresponding
 * `School*ScreenV2`, which `koinViewModel()`s its own VM. The portal is `tone = Warm` (set by the
 * host `VTheme`). Many downstream admin routes are local-only stubs per the master doc §5.3; those
 * surfaces render `VComingSoon` inside their screens rather than fabricating data.
 *
 * Notifications and AcademicCalendar (from the `App.tsx` graph) are pushed as full-screen overlays.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SchoolPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // UI_FIDELITY_AUDIT §0.5: Admin.tsx renders under `PhoneFrame dark`, but legacy `dark` == the
    // `.warm` scope, which is a WARM-LIGHT theme (lavender bg, dark ink, white cards) — NOT black.
    // So the admin portal must be Warm, never Night.
    VTheme(tone = VPortalTone.Warm) {
        var tab by remember { mutableStateOf("home") }
        var overlay by remember { mutableStateOf(SchoolOverlay.None) }
        // RA-45 — id carried into the student/teacher profile overlays.
        var selectedStudentId by remember { mutableStateOf<String?>(null) }
        var selectedTeacherId by remember { mutableStateOf<String?>(null) }

        // §11 cross-platform — Android predictive back / iOS edge-swipe pops
        // the full-screen Notifications/Calendar overlay back to the admin tabs
        // instead of leaving the portal. Mirrors the React `onBack` wiring.
        BackHandler(enabled = overlay != SchoolOverlay.None) {
            overlay = SchoolOverlay.None
        }

        when (overlay) {
            SchoolOverlay.Notifications -> {
                NotificationsScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.Calendar -> {
                AcademicCalendarScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.Messages -> {
                MessagesScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.LeaveRequests -> {
                LeaveRequestsScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.LinkRequests -> {
                // RA-48: the parent→child link approval queue.
                LinkRequestsScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.AdmissionsCRM -> {
                AdmissionsCrmScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.Results -> {
                ResultsPublishScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.SchedulePTM -> {
                SchedulePtmScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.DailyAttendance -> {
                DailyAttendanceScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.ClassPerformance -> {
                ClassPerformanceScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.TeacherPerformance -> {
                TeacherPerformanceScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.AnalyticsDashboard -> {
                AnalyticsDashboardScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.EditProfile -> {
                // RA-47 — edit the live schools row (institutional profile).
                EditSchoolProfileScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return@VTheme
            }
            SchoolOverlay.StudentRoster -> {
                // RA-45 — the live student roster; rows open a student profile.
                StudentRosterScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    onOpenStudent = { id -> selectedStudentId = id; overlay = SchoolOverlay.StudentProfile },
                    modifier = modifier,
                )
                return@VTheme
            }
            SchoolOverlay.StudentProfile -> {
                // RA-45 — single student record (attendance/marks/leave/fees).
                val id = selectedStudentId
                if (id == null) { overlay = SchoolOverlay.StudentRoster; return@VTheme }
                StudentProfileScreenV2(
                    studentId = id,
                    onBack = { overlay = SchoolOverlay.StudentRoster },
                    modifier = modifier,
                )
                return@VTheme
            }
            SchoolOverlay.TeacherProfile -> {
                // RA-45 — single teacher detail (assignments/coverage).
                val id = selectedTeacherId
                if (id == null) { overlay = SchoolOverlay.None; return@VTheme }
                TeacherProfileScreenV2(
                    teacherId = id,
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return@VTheme
            }
            SchoolOverlay.None -> Unit
        }

        val items = listOf(
            VNavItem("home", "Home", VIcons.Home),
            VNavItem("people", "People", VIcons.Users),
            VNavItem("records", "Records", VIcons.Bookmark),
            VNavItem("comms", "Comms", VIcons.Megaphone, badge = 2),
            VNavItem("settings", "Settings", VIcons.Settings),
        )

        VScreenScaffold(
            modifier = modifier,
            bottomBar = {
                VBottomNav(items = items, selected = tab, onSelect = { tab = it })
            },
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "home" -> SchoolHomeScreenV2(
                        onOpenNotifications = { overlay = SchoolOverlay.Notifications },
                        onOpenCalendar = { overlay = SchoolOverlay.Calendar },
                        // RA-24 — the Home "live metrics" / PEWS cards now open the
                        // real analytics dashboard and the at-risk cohort (People
                        // tab) instead of dead Coming-Soon placeholders.
                        onOpenAnalytics = { overlay = SchoolOverlay.AnalyticsDashboard },
                        onOpenPews = { tab = "people" },
                        // §7 finding K — tapping the avatar opens the Settings tab (where logout
                        // lives), instead of logging the admin out outright.
                        onExit = { tab = "settings" },
                    )
                    "people" -> SchoolPeopleScreenV2(
                        // RA-48 — open the parent→child link approval queue.
                        onOpenLinkRequests = { overlay = SchoolOverlay.LinkRequests },
                        // RA-45 — open the live student roster + teacher profile.
                        onOpenStudentRoster = { overlay = SchoolOverlay.StudentRoster },
                        onOpenTeacher = { id -> selectedTeacherId = id; overlay = SchoolOverlay.TeacherProfile },
                    )
                    "records" -> SchoolRecordsScreenV2()
                    "comms" -> SchoolCommsScreenV2(
                        // RA-24 — Messages and PTM open their real backend-backed
                        // screens as overlays instead of Coming-Soon cards.
                        onOpenMessages = { overlay = SchoolOverlay.Messages },
                        onOpenPtm = { overlay = SchoolOverlay.SchedulePTM },
                    )
                    "settings" -> SchoolSettingsScreenV2(
                        onLogout = onLogout,
                        // RA-24 — "Teacher management" opens the live People tab
                        // roster (RA-22) rather than a Coming-Soon label.
                        onOpenTeachers = { tab = "people" },
                        // RA-47 — open the editable institutional-profile screen.
                        onOpenProfile = { overlay = SchoolOverlay.EditProfile },
                    )
                }
            }
        }
    }
}
