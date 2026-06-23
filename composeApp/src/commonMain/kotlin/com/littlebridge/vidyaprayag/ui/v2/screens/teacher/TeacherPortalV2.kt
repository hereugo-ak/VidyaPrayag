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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/** Full-screen overlays the teacher portal can push above its tab content. */
private enum class TeacherOverlay { None, Notifications, Calendar, Leave, Update }

/**
 * TeacherPortalV2 — the rebuilt 5-tab teacher shell (Doc 04 §4 IA).
 *
 * Bottom nav: **Today · Classes · Gradebook · Planner · Profile**. Per the brief,
 * "attendance is an ACTION, not a tab" — so the legacy 4-tab IA (Home · Update ·
 * My Classes · Profile) is replaced. The write planes (Attendance / Marks / Syllabus
 * / Homework) are no longer a bottom-nav tab; they are reached from the Today card's
 * pre-scoped CTAs and pushed as the [TeacherOverlay.Update] full-screen overlay, so
 * existing backend write flows remain wired while the IA is correct.
 *
 *   Today     — [TodayScreen]: greeting + live 3-face schedule card (T-105, server-resolved).
 *   Classes   — [TeacherClassesScreenV2] (existing).
 *   Gradebook — [TeacherGradebookScreenV2] (T-305): scoped assessment list + create + a
 *               validated marks grid with distinct Save (private) vs Publish (notifies parents).
 *   Planner   — STAGED (P4 / T-401..406). Honest placeholder for now.
 *   Profile   — [TeacherProfileScreenV2] (existing; logout lives here).
 *
 * Notifications / AcademicCalendar / Leave / the Update write-plane are full-screen
 * overlays. The portal is `tone = Warm`.
 *
 * NOTE (Doc 11 deviation): Planner is a placeholder by SEQUENCE design — its real screen
 * lands in P4. Wiring it as an honest staged tab now (rather than hiding the tab) keeps the
 * 5-tab IA visible and avoids a disruptive nav reshuffle later.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TeacherPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // UI_FIDELITY_AUDIT §0.5 — the teacher portal is WARM-LIGHT (lavender bg, dark ink,
    // white cards), never Night.
    VTheme(tone = VPortalTone.Warm) {
        var tab by remember { mutableStateOf("today") }
        var updateSub by remember { mutableStateOf("Attendance") }
        var overlay by remember { mutableStateOf(TeacherOverlay.None) }

        // T-505: the scoped student-profile overlay. Reached from a Classes-tab roster
        // row tap (each row carries the already-scoped studentId). A non-null id pushes
        // [TeacherStudentProfileScreenV2] full-screen above the tab content; back clears it.
        // Scope is enforced server-side — a forbidden id renders the screen's own
        // ForbiddenState rather than leaking another teacher's student.
        var selectedStudentId by remember { mutableStateOf<String?>(null) }

        // The Update write-plane needs real ids. These hold the teacher's current
        // class/subject/exam selection. When the Update overlay is opened from a Today
        // CTA, they are pre-seeded from the tapped (already pre-authorized) period.
        var selectedClassId by remember { mutableStateOf("") }
        var selectedSubject by remember { mutableStateOf("") }
        // T-205: the pre-known scope label for the attendance wrong-class guard header
        // (E15), shown instantly while the server load confirms scope.
        var selectedScope by remember { mutableStateOf("") }

        // T-305: the Gradebook tab is reached PRE-SCOPED from a Today/Classes CTA or a
        // "marks" obligation (Doc 04 §5.5). These hold the pre-authorized assignment +
        // its scope label so [TeacherGradebookScreenV2] opens directly on the right class
        // (a blank id falls back to the screen's "Choose a class" empty state).
        var gradebookAssignmentId by remember { mutableStateOf("") }
        var gradebookScope by remember { mutableStateOf("") }

        // Open the Update write-plane on a specific sub-tab, pre-seeded from a Today
        // period's pre-authorized assignment (so no class re-pick is needed — Doc 04 §4).
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

        // T-107: route an obligations-strip tap to the right SCOPED surface. The
        // item already carries the pre-authorized assignment/ref ids, so taps land
        // directly on the tool — never a picker (Doc 04 §5.5 deep-link contract):
        //   • attendance → Update plane on Attendance, pre-seeded by assignment id
        //   • leave      → the teacher Leave inbox overlay
        //   • marks      → Gradebook tab, pre-scoped to the assignment (T-305 — real flow)
        //   • homework   → Planner tab (real review flow lands in P4 — staged)
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
                    // Pre-seed the Gradebook tab with the pre-authorized assignment so it
                    // opens directly on the right class (Doc 04 §5.5 deep-link contract).
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

        // T-505: the student-profile overlay sits ABOVE the tab content (and above the
        // other overlays it is reached from the Classes tab, which has overlay == None).
        // A non-null selectedStudentId short-circuits the whole portal to the profile.
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
                    onSelectClass = { id, subject -> selectedClassId = id; selectedSubject = subject; selectedScope = "" },
                    onBack = { overlay = TeacherOverlay.None },
                    modifier = modifier,
                )
                return@VTheme
            }
            TeacherOverlay.None -> Unit
        }

        // Doc 04 §4 IA — Today · Classes · Gradebook · Planner · Profile.
        val items = listOf(
            VNavItem("today", "Today", VIcons.Home),
            VNavItem("classes", "Classes", VIcons.Users),
            VNavItem("gradebook", "Gradebook", VIcons.GraduationCap),
            VNavItem("planner", "Planner", VIcons.Edit3),
            VNavItem("profile", "Profile", VIcons.User),
        )

        VScreenScaffold(
            modifier = modifier,
            bottomBar = {
                VBottomNav(items = items, selected = tab, onSelect = { tab = it })
            },
        ) { _ ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "today" -> TodayScreen(
                        onMarkAttendance = { openUpdate(it, "Attendance") },
                        onOpenSyllabus = { openUpdate(it, "Syllabus") },
                        onOpenHomework = { openUpdate(it, "Homework") },
                        onOpenObligation = { openObligation(it) },
                        onOpenNotifications = { overlay = TeacherOverlay.Notifications },
                        onOpenProfile = { tab = "profile" },
                    )
                    "classes" -> TeacherClassesScreenV2(
                        onOpenStudent = { studentId -> selectedStudentId = studentId },
                    )
                    // T-305: the real Gradebook — scoped assessment list + create + a
                    // validated marks grid with distinct Save (private) vs Publish (notifies
                    // parents). Reached pre-scoped from a Today/obligation CTA; a blank id
                    // shows the screen's own "Choose a class" empty state.
                    "gradebook" -> TeacherGradebookScreenV2(
                        assignmentId = gradebookAssignmentId,
                        scopeHint = gradebookScope,
                    )
                    // T-403: the real Planner — class-pick → typed, scoped Syllabus (one-tap
                    // coverage). Homework sub-tab is still staged until T-406.
                    "planner" -> PlannerScreen()
                    "profile" -> TeacherProfileScreenV2(onLogout = onLogout)
                }
            }
        }
    }
}

/**
 * The Update write-plane, lifted out of the bottom nav into a full-screen overlay
 * (attendance is an action, not a tab). It keeps the existing Attendance / Marks /
 * Syllabus / Homework leaves and their real-id pickers, so backend write flows stay wired.
 */
@Composable
private fun TeacherUpdatePlane(
    sub: String,
    onSelectSub: (String) -> Unit,
    selectedClassId: String,
    selectedSubject: String,
    selectedScope: String,
    onSelectClass: (String, String) -> Unit,
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
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(VTheme.colors.ink.copy(alpha = 0.06f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = VTheme.colors.ink, modifier = Modifier.size(18.dp))
                }
                Text(
                    "Update",
                    style = VTheme.type.h1.colored(VTheme.colors.ink),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            // T-305: "Marks" is no longer a write-plane leaf — it graduated to its own
            // Gradebook tab (assessment list → marks grid → publish). The Update plane
            // keeps only Attendance / Syllabus / Homework.
            VTopTabs(
                tabs = listOf("Attendance", "Syllabus", "Homework"),
                selected = sub,
                onSelect = onSelectSub,
            )
        }

        Column(Modifier.fillMaxSize()) {
            // T-205 (F-ATT-1): Attendance is reached PRE-SCOPED from a Today/Classes/obligations
            // CTA via the pre-authorized assignmentId — it must NOT front the shared class
            // picker (that buried it 2-3 taps and reset scope every visit). Homework authors
            // its own class field; the shared picker still fronts Syllabus until its own
            // rebuild (P4). (Marks moved out to the Gradebook tab — T-305.)
            if (sub != "Homework" && sub != "Attendance") {
                TeacherClassPicker(
                    selectedClassId = selectedClassId,
                    onSelectClass = { cls -> onSelectClass(cls.assignmentId, cls.subject) },
                )
            }
            when (sub) {
                "Attendance" -> TeacherAttendanceScreenV2(
                    assignmentId = selectedClassId,
                    date = "",
                    scopeHint = selectedScope,
                )
                // T-403: the typed, assignment-scoped syllabus (selectedClassId IS the TSA id).
                "Syllabus" -> TeacherSyllabusScreenV2(assignmentId = selectedClassId, scopeHint = selectedScope)
                "Homework" -> TeacherHomeworkScreenV2()
            }
        }
    }
}

