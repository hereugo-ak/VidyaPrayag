package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme

/** Full-screen overlays the teacher portal can push above its tab content. */
private enum class TeacherOverlay { None, Notifications, Calendar }

/**
 * TeacherPortalV2 — the 4-tab teacher shell, translated from Teacher.tsx.
 *
 * Bottom nav: Home · Update · Classes · Profile. The "Update" tab carries an inner [VTopTabs]
 * row for the four data-entry screens (Attendance / Marks / Syllabus / Homework). Each leaf is the
 * corresponding `*ScreenV2`, which `koinViewModel()`s its own VM. The portal is `tone = Warm`
 * (set by the host `VTheme`).
 *
 * Notifications and AcademicCalendar (from the `App.tsx` graph) are pushed as full-screen overlays.
 * Class/exam/subject ids are stubbed locally for now; Phase 3E wires real selection + navigation.
 */
@Composable
fun TeacherPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // UI_FIDELITY_AUDIT §0.5: Teacher.tsx renders under `PhoneFrame dark`, but legacy `dark` == the
    // `.warm` scope, which is a WARM-LIGHT theme (lavender bg, dark ink, white cards) — NOT black.
    // So the teacher portal must be Warm, never Night.
    VTheme(tone = VPortalTone.Warm) {
        var tab by remember { mutableStateOf("home") }
        var updateSub by remember { mutableStateOf("Attendance") }
        var overlay by remember { mutableStateOf(TeacherOverlay.None) }

        when (overlay) {
            TeacherOverlay.Notifications -> {
                NotificationsScreenV2(onBack = { overlay = TeacherOverlay.None }, modifier = modifier)
                return@VTheme
            }
            TeacherOverlay.Calendar -> {
                AcademicCalendarScreenV2(onBack = { overlay = TeacherOverlay.None }, modifier = modifier)
                return@VTheme
            }
            TeacherOverlay.None -> Unit
        }

        val items = listOf(
            VNavItem("home", "Home", VIcons.Home),
            VNavItem("update", "Update", VIcons.Check, badge = 1),
            VNavItem("classes", "My Classes", VIcons.School),
            VNavItem("profile", "Profile", VIcons.User),
        )

        VScreenScaffold(
            modifier = modifier,
            topBar = {
                if (tab == "update") {
                    VTopTabs(
                        tabs = listOf("Attendance", "Marks", "Syllabus", "Homework"),
                        selected = updateSub,
                        onSelect = { updateSub = it },
                    )
                }
            },
            bottomBar = {
                VBottomNav(items = items, selected = tab, onSelect = { tab = it })
            },
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "home" -> TeacherHomeScreenV2(
                        onOpenNotifications = { overlay = TeacherOverlay.Notifications },
                        onOpenCalendar = { overlay = TeacherOverlay.Calendar },
                        onExit = onLogout,
                    )
                    "classes" -> TeacherClassesScreenV2()
                    "profile" -> TeacherProfileScreenV2(onLogout = onLogout)
                    "update" -> when (updateSub) {
                        "Attendance" -> TeacherAttendanceScreenV2(classId = "", date = "")
                        "Marks" -> TeacherMarksScreenV2(classId = "", examId = "")
                        "Syllabus" -> TeacherSyllabusScreenV2(classId = "", subject = "")
                        "Homework" -> TeacherHomeworkScreenV2()
                    }
                }
            }
        }
    }
}
