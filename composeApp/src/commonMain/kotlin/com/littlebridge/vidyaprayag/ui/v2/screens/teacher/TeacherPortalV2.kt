package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherObligationsViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Full-screen overlays the teacher portal can push above its tab content. */
private enum class TeacherOverlay { None, Notifications, Calendar, Leave, Update }

/**
 * TeacherPortalV2 — the rebuilt 5-tab teacher shell (Doc 04 §4 IA), now with the full P6 nav
 * chrome (T-601): the premium floating [TeacherDock] (ParentDock physics) and ONE canonical
 * [TeacherHeader] mounted in the scaffold's top bar on every tab.
 *
 * Bottom nav: **Today · Classes · Gradebook · Planner · Profile**. Per the brief,
 * "attendance is an ACTION, not a tab" — the write planes (Attendance / Syllabus / Homework)
 * are reached PRE-SCOPED from the Today card's CTAs and pushed as the [TeacherOverlay.Update]
 * full-screen overlay. The Today badge is the LIVE obligation count from
 * [TeacherObligationsViewModel.totalOutstanding] — never a hardcoded `1` (F-SHELL-4).
 *
 * T-601 nav-chrome changes:
 *   • [VBottomNav] → [TeacherDock] (Doc 10 §5.1 — same dock physics as the parents portal).
 *   • A single canonical [TeacherHeader] (greeting + notifications + account + class-context chip)
 *     renders on every tab (Doc 10 §5.2). The per-tab greeting bars were removed from the leaf
 *     screens so there is exactly ONE header (no double chrome).
 *   • The shared Update-overlay class picker is ELIMINATED (Doc 04 §7 anti-picker rule): the
 *     Update plane is only ever reached pre-scoped from a Today CTA / obligation, so it never
 *     needs to re-declare scope. Deliberate re-scoping happens via the operational tabs and the
 *     header class-context chip.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TeacherPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    // The header identity comes from the real teacher profile; the bell dot + dock are fed by the
    // shared obligations/notifications feeds. All three are owned at the portal level so the
    // canonical header and dock stay in sync across every tab.
    profileViewModel: TeacherProfileViewModel = koinViewModel(),
    obligationsViewModel: TeacherObligationsViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    todayViewModel: TeacherTodayViewModel = koinViewModel(),
    preferenceRepository: PreferenceRepository = koinInject(),
) {
    // T-602b: the portal tone is now driven by the teacher's saved theme preference
    // (the Profile → Settings → Appearance switch writes it). It DEFAULTS to Warm —
    // the teacher portal's canonical look (UI_FIDELITY_AUDIT §0.5: lavender bg, dark
    // ink, white cards) — so behaviour is unchanged until the teacher opts into
    // Light or Night. The pref is global; reading it here keeps the switch honest.
    val themeName by preferenceRepository.getThemeName().collectAsState(initial = "WARM")
    val tone = when (themeName.uppercase()) {
        "LIGHT" -> VPortalTone.Light
        "NIGHT" -> VPortalTone.Night
        else -> VPortalTone.Warm
    }
    VTheme(tone = tone) {
        var tab by remember { mutableStateOf("today") }
        var updateSub by remember { mutableStateOf("Attendance") }
        var overlay by remember { mutableStateOf(TeacherOverlay.None) }

        val profile by profileViewModel.state.collectAsStateV2()
        val obligations by obligationsViewModel.state.collectAsStateV2()
        val notifications by notificationsViewModel.state.collectAsStateV2()
        val today by todayViewModel.state.collectAsStateV2()

        // T-505: the scoped student-profile overlay. Reached from a Classes-tab roster
        // row tap (each row carries the already-scoped studentId). A non-null id pushes
        // [TeacherStudentProfileScreenV2] full-screen above the tab content; back clears it.
        var selectedStudentId by remember { mutableStateOf<String?>(null) }

        // The Update write-plane needs real ids. These hold the teacher's current
        // class/subject selection, ALWAYS pre-seeded from the tapped (already pre-authorized)
        // period or obligation — never re-picked inside the overlay (Doc 04 §7).
        var selectedClassId by remember { mutableStateOf("") }
        var selectedSubject by remember { mutableStateOf("") }
        // T-205: the pre-known scope label for the attendance wrong-class guard header (E15).
        var selectedScope by remember { mutableStateOf("") }

        // T-305: the Gradebook tab is reached PRE-SCOPED from a Today/Classes CTA or a
        // "marks" obligation (Doc 04 §5.5). These hold the pre-authorized assignment + scope.
        var gradebookAssignmentId by remember { mutableStateOf("") }
        var gradebookScope by remember { mutableStateOf("") }

        // Open the Update write-plane on a specific sub-tab, pre-seeded from a Today period's
        // pre-authorized assignment (so no class re-pick is needed — Doc 04 §4/§7).
        fun openUpdate(period: ResolvedPeriodUi, sub: String) {
            selectedClassId = period.assignmentId.orEmpty()
            selectedSubject = period.subject
            selectedScope = listOfNotNull(
                period.classLabel.takeIf { period.className.isNotBlank() },
                period.subject.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            updateSub = sub
            overlay = TeacherOverlay.Update
        }

        // T-107: route an obligations-strip tap to the right SCOPED surface (deep-link contract).
        fun openObligation(item: ObligationItemDto) {
            when (item.type) {
                "attendance" -> {
                    selectedClassId = item.assignmentId.orEmpty()
                    selectedSubject = ""
                    selectedScope = item.title
                    updateSub = "Attendance"
                    overlay = TeacherOverlay.Update
                }
                "leave" -> overlay = TeacherOverlay.Leave
                "marks" -> {
                    gradebookAssignmentId = item.assignmentId.orEmpty()
                    gradebookScope = item.title
                    tab = "gradebook"
                }
                "homework" -> tab = "planner"
                else -> Unit
            }
        }

        BackHandler(enabled = overlay != TeacherOverlay.None) {
            overlay = TeacherOverlay.None
        }

        // T-505: the student-profile overlay sits ABOVE the tab content.
        selectedStudentId?.let { studentId ->
            BackHandler(enabled = true) { selectedStudentId = null }
            TeacherStudentProfileScreenV2(
                studentId = studentId,
                onBack = { selectedStudentId = null },
                modifier = modifier,
            )
            return@VTheme
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
            TeacherOverlay.Leave -> {
                TeacherLeaveScreenV2(onBack = { overlay = TeacherOverlay.None }, modifier = modifier)
                return@VTheme
            }
            TeacherOverlay.Update -> {
                TeacherUpdatePlane(
                    sub = updateSub,
                    onSelectSub = { updateSub = it },
                    selectedClassId = selectedClassId,
                    selectedSubject = selectedSubject,
                    selectedScope = selectedScope,
                    onBack = { overlay = TeacherOverlay.None },
                    modifier = modifier,
                )
                return@VTheme
            }
            TeacherOverlay.None -> Unit
        }

        // Doc 04 §4 IA — Today · Classes · Gradebook · Planner · Profile.
        // F-SHELL-4: the Today badge rides the LIVE obligation count, hidden at 0.
        val items = listOf(
            VNavItem("today", "Today", VIcons.Home, badge = obligations.totalOutstanding),
            VNavItem("classes", "Classes", VIcons.Users),
            VNavItem("gradebook", "Gradebook", VIcons.GraduationCap),
            VNavItem("planner", "Planner", VIcons.Edit3),
            VNavItem("profile", "Profile", VIcons.User),
        )

        // The canonical greeting/identity for the shared header.
        val teacherName = today.teacherName.ifBlank { profile.profile?.name.orEmpty() }
        val greeting = teacherGreeting(nowMinutesOfDay() / 60)
        val schoolName = profile.profile?.schoolName.orEmpty()
        val photoUrl = profile.profile?.photoUrl

        // The class-context chip shows the current scope inside the operational tabs and changes
        // it in one tap (Doc 04 §7 — replaces the eliminated shared picker). On Gradebook it
        // surfaces the pre-scoped class and lets the teacher jump to Classes to re-scope; on
        // Classes/Planner those tabs own their own in-content pickers, so the chip is informational.
        val classContext: String? = when (tab) {
            "gradebook" -> gradebookScope.takeIf { it.isNotBlank() }
            else -> null
        }

        VScreenScaffold(
            modifier = modifier,
            topBar = {
                TeacherHeader(
                    teacherName = teacherName,
                    greeting = greeting,
                    schoolName = schoolName,
                    photoUrl = photoUrl,
                    unreadCount = notifications.unreadCount,
                    onOpenNotifications = { overlay = TeacherOverlay.Notifications },
                    onOpenProfile = { tab = "profile" },
                    classContext = classContext,
                    onChangeScope = if (tab == "gradebook") {
                        { tab = "classes" }
                    } else null,
                )
            },
            bottomBar = {
                // T-601 / Doc 10 §5.1 — the premium floating dock (ParentDock physics).
                TeacherDock(items = items, selected = tab, onSelect = { tab = it })
            },
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "today" -> TodayScreen(
                        onMarkAttendance = { openUpdate(it, "Attendance") },
                        onOpenSyllabus = { openUpdate(it, "Syllabus") },
                        onOpenHomework = { openUpdate(it, "Homework") },
                        onOpenObligation = { openObligation(it) },
                        // Schedule-independent quick actions — route to the self-sufficient tabs
                        // (each fronts its own class picker), so the work is always one tap away
                        // even on a holiday / off-timetable day.
                        onGoToClasses = { tab = "classes" },
                        onGoToPlanner = { tab = "planner" },
                        onGoToGradebook = { tab = "gradebook" },
                    )
                    "classes" -> TeacherClassesScreenV2(
                        onOpenStudent = { studentId -> selectedStudentId = studentId },
                        // Attendance is an ACTION, not a tab — but it must also be reachable when
                        // Today has no schedule (holiday / off-timetable). The Classes tab is the
                        // canonical roster surface, so each class can open the pre-scoped
                        // Attendance write-plane directly.
                        onMarkAttendance = { assignmentId, scope ->
                            selectedClassId = assignmentId
                            selectedSubject = ""
                            selectedScope = scope
                            updateSub = "Attendance"
                            overlay = TeacherOverlay.Update
                        },
                    )
                    "gradebook" -> TeacherGradebookScreenV2(
                        assignmentId = gradebookAssignmentId,
                        scopeHint = gradebookScope,
                    )
                    "planner" -> PlannerScreen()
                    "profile" -> TeacherProfileScreenV2(
                        onLogout = onLogout,
                        onOpenLeave = { overlay = TeacherOverlay.Leave },
                    )
                }
            }
        }
    }
}

/**
 * The Update write-plane, lifted out of the bottom nav into a full-screen overlay (attendance
 * is an action, not a tab). It is ALWAYS reached pre-scoped (Doc 04 §7) — there is no shared
 * class picker here anymore; the class/subject arrive from the Today CTA or obligation that
 * opened it. Keeps the Attendance / Syllabus / Homework leaves wired to their real backends.
 */
@Composable
private fun TeacherUpdatePlane(
    sub: String,
    onSelectSub: (String) -> Unit,
    selectedClassId: String,
    selectedSubject: String,
    selectedScope: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(VTheme.colors.card)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(VTheme.colors.ink.copy(alpha = 0.06f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = VTheme.colors.ink, modifier = Modifier.size(18.dp))
                }
                Text(
                    "Update",
                    style = VTheme.type.h2.colored(VTheme.colors.ink),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            // "Marks" graduated to its own Gradebook tab (T-305). The Update plane keeps only
            // Attendance / Syllabus / Homework — all reached pre-scoped from Today (Doc 04 §7).
            VTopTabs(
                tabs = listOf("Attendance", "Syllabus", "Homework"),
                selected = sub,
                onSelect = onSelectSub,
            )
        }

        Column(Modifier.fillMaxSize()) {
            // T-601 (Doc 04 §7): the shared Update-overlay class picker is ELIMINATED. Every
            // leaf here is reached pre-scoped from a Today CTA / obligation that carries the
            // pre-authorized assignmentId, so no leaf re-declares its scope.
            when (sub) {
                "Attendance" -> TeacherAttendanceScreenV2(
                    assignmentId = selectedClassId,
                    date = "",
                    scopeHint = selectedScope,
                )
                "Syllabus" -> TeacherSyllabusScreenV2(assignmentId = selectedClassId, scopeHint = selectedScope)
                "Homework" -> TeacherHomeworkScreenV2(assignmentId = selectedClassId, scopeHint = selectedScope)
            }
        }
    }
}
