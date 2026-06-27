package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherObligationsViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VNavItem
import com.littlebridge.enrollplus.ui.v2.components.VScreenScaffold
import com.littlebridge.enrollplus.ui.v2.navigation.DeepLinkTarget
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.enrollplus.ui.v2.theme.VPortalTone
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Full-screen overlays the teacher portal can push above its tab content. */
private enum class TeacherOverlay { None, Notifications }

/**
 * TeacherPortalV2 — the teacher shell, rebuilt FROM SCRATCH on the Parents-Portal
 * design language (lavender canvas, white rounded cards, Canvas rings, floating
 * dock, brand violet reserved for active/brand moments).
 *
 * New 4-tab IA (replacing the old Today/Classes/Gradebook/Planner/Profile):
 *
 *   HOME · UPDATE · CLASSES · PROFILE
 *
 *   • HOME    — time-sensitive greeting, first-login fingerprint check-in popup,
 *               attendance clubbed into DB-backed summary cards, today's schedule,
 *               assignments/tests/reminders. Swipe cards expand in place.
 *   • UPDATE  — the write plane (Attendance · Marks · Syllabus · Homework) with a
 *               class/section/subject scope gate. Reached pre-scoped from HOME CTAs
 *               or picked fresh from the gate.
 *   • CLASSES — rich roster plane: class list → composite class detail → scoped
 *               student-profile drill-down (all self-contained in the tab).
 *   • PROFILE — identity, leave apply/status, password, theme switch, logout.
 *
 * The signature `TeacherPortalV2(onLogout, modifier)` is PRESERVED — it is the only
 * external reference (NavGraphV2 line 309).
 *
 * Live theme: the Profile → Appearance switch writes the global theme pref; this
 * shell reads `getThemeName()` and wraps content in a nested [VTheme] so the tone
 * (Warm / Light / Night) flips immediately without a relaunch. Default is Warm —
 * the teacher portal's canonical lavender look.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TeacherPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    deepLinkTarget: DeepLinkTarget? = null,
    profileViewModel: TeacherProfileViewModel = koinViewModel(),
    obligationsViewModel: TeacherObligationsViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    preferenceRepository: PreferenceRepository = koinInject(),
) {
    // Live tone from the saved preference (defaults Warm — the canonical teacher look).
    val themeName by preferenceRepository.getThemeName().collectAsState(initial = "WARM")
    val tone = when (themeName.uppercase()) {
        "LIGHT" -> VPortalTone.Light
        "NIGHT" -> VPortalTone.Night
        else -> VPortalTone.Warm
    }

    VTheme(tone = tone) {
        var tab by remember { mutableStateOf("home") }
        var overlay by remember { mutableStateOf(TeacherOverlay.None) }

        // Apply deep-link routing: set tab from the typed target.
        LaunchedEffect(deepLinkTarget) {
            when (deepLinkTarget) {
                is DeepLinkTarget.TeacherScreen -> tab = deepLinkTarget.screen
                else -> Unit
            }
        }

        // The UPDATE tab can be entered pre-scoped from a HOME CTA. These hold the
        // pre-authorized scope; a bump on [updateScopeNonce] forces the Update screen
        // to re-read its initial* values (so a fresh HOME tap re-seeds the gate).
        var updateAssignmentId by remember { mutableStateOf<String?>(null) }
        var updateScopeLabel by remember { mutableStateOf("") }
        var updateInitialTool by remember { mutableStateOf(UpdateTool.Attendance) }
        var updateScopeNonce by remember { mutableStateOf(0) }

        val profile by profileViewModel.state.collectAsStateV2()
        val obligations by obligationsViewModel.state.collectAsStateV2()
        val notifications by notificationsViewModel.state.collectAsStateV2()

        BackHandler(enabled = overlay != TeacherOverlay.None) {
            overlay = TeacherOverlay.None
        }
        // From a non-home tab, Back returns to HOME (familiar app behaviour).
        BackHandler(enabled = overlay == TeacherOverlay.None && tab != "home") {
            tab = "home"
        }

        // ── Overlays sit above all tab content ──────────────────────────────────
        when (overlay) {
            TeacherOverlay.Notifications -> {
                NotificationsScreenV2(onBack = { overlay = TeacherOverlay.None }, modifier = modifier)
                return@VTheme
            }
            TeacherOverlay.None -> Unit
        }

        // ── Dock items. The Update badge rides the LIVE obligation count (hidden at 0). ──
        val items = listOf(
            VNavItem("home", "Home", VIcons.Home),
            VNavItem("update", "Update", VIcons.Edit3, badge = obligations.totalOutstanding),
            VNavItem("classes", "Classes", VIcons.Users),
            VNavItem("profile", "Profile", VIcons.User),
        )

        // Canonical header identity (hidden on HOME — HOME renders its own greeting hero).
        val teacherName = profile.profile?.name.orEmpty()
        val schoolName = profile.profile?.schoolName.orEmpty()
        val photoUrl = profile.profile?.photoUrl
        val subline = when (tab) {
            "update" -> "Mark & publish"
            "classes" -> "Your classes & students"
            "profile" -> schoolName.ifBlank { "Your account" }
            else -> schoolName
        }

        VScreenScaffold(
            modifier = modifier,
            topBar = {
                // HOME owns its own greeting hero, so the slim canonical header only
                // mounts on the other three tabs (no double chrome).
                if (tab != "home") {
                    TeacherHeader(
                        teacherName = teacherName.ifBlank { "Teacher" },
                        subline = subline,
                        photoUrl = photoUrl,
                        unreadCount = notifications.unreadCount,
                        onOpenProfile = { tab = "profile" },
                        onOpenNotifications = { overlay = TeacherOverlay.Notifications },
                    )
                }
            },
            bottomBar = {
                TeacherDock(items = items, selected = tab, onSelect = { tab = it })
            },
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "home" -> TeacherHomeScreenV2(
                        onOpenAttendanceForAssignment = { assignmentId, scope ->
                            updateAssignmentId = assignmentId
                            updateScopeLabel = scope
                            updateInitialTool = UpdateTool.Attendance
                            updateScopeNonce++
                            tab = "update"
                        },
                        onOpenUpdateTab = {
                            // Fresh, unscoped entry → the Update gate picks a class.
                            updateAssignmentId = null
                            updateScopeLabel = ""
                            updateInitialTool = UpdateTool.Attendance
                            updateScopeNonce++
                            tab = "update"
                        },
                        onOpenClasses = { tab = "classes" },
                    )

                    "update" -> key(updateScopeNonce) {
                        TeacherUpdateScreenV2(
                            initialAssignmentId = updateAssignmentId,
                            initialScopeLabel = updateScopeLabel,
                            initialTool = updateInitialTool,
                        )
                    }

                    "classes" -> TeacherClassesScreenV2()

                    "profile" -> TeacherProfileScreenV2(onLogout = onLogout)
                }
            }
        }
    }
}
