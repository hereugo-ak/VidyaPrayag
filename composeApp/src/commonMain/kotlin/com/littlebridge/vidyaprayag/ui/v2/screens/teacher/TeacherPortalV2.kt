package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
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
@OptIn(ExperimentalComposeUiApi::class)
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

        // §11 cross-platform — Android predictive back / iOS edge-swipe pops
        // the full-screen Notifications/Calendar overlay back to the teacher
        // tabs instead of leaving the portal. Mirrors the React `onBack` wiring.
        BackHandler(enabled = overlay != TeacherOverlay.None) {
            overlay = TeacherOverlay.None
        }

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

        // §0.6 React nav glyphs (Teacher.tsx:8-13): Home, ListChecks (Update), Users (My Classes), User.
        val items = listOf(
            VNavItem("home", "Home", VIcons.Home),
            VNavItem("update", "Update", VIcons.ListChecks, badge = 1),
            VNavItem("classes", "My Classes", VIcons.Users),
            VNavItem("profile", "Profile", VIcons.User),
        )

        VScreenScaffold(
            modifier = modifier,
            topBar = {
                if (tab == "update") {
                    // §6.2 React Update() renders an <h1 className="mb-4">Update</h1> above the tabs
                    // (Teacher.tsx:113-115). pt-6 (24) / px-5 (20) / mb-4 (16).
                    Column(Modifier.fillMaxWidth().background(VTheme.colors.card)) {
                        Text(
                            "Update",
                            style = VTheme.type.h1.copy(color = VTheme.colors.ink),
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
                        )
                        VTopTabs(
                            tabs = listOf("Attendance", "Marks", "Syllabus", "Homework"),
                            selected = updateSub,
                            onSelect = { updateSub = it },
                        )
                    }
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
                        // §7 finding K — tapping the avatar opens the Profile tab (where logout
                        // lives), instead of logging the teacher out outright.
                        onExit = { tab = "profile" },
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
