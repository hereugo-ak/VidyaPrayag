package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.littlebridge.enrollplus.feature.admin.presentation.MessagesViewModel
import com.littlebridge.enrollplus.feature.alumni.domain.model.GraduateStudentsRequest
import com.littlebridge.enrollplus.feature.alumni.domain.repository.AlumniRepository
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.ui.v2.components.VBottomNav
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VNavItem
import com.littlebridge.enrollplus.ui.v2.components.VScreenScaffold
import com.littlebridge.enrollplus.ui.v2.navigation.DeepLinkTarget
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.enrollplus.ui.v2.screens.notifications.NotificationsScreenV2
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Full-screen overlays the admin portal can push above its tab content. */
private enum class SchoolOverlay {
    None,
    Notifications,
    Calendar,
    // VP-CAL: the premium Academic Calendar platform, the 7-step create-event
    // wizard, and Academic Year management.
    AcademicCalendarPlatform,
    CreateEvent,
    AcademicYear,
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
    // PEWS (Predictive Early Warning System) — live at-risk cohort + per-student signal.
    PewsCohort,
    PewsStudentDetail,
    TeacherProfile,
    TeacherAssignments,
    Staff,
    HealthRecords,
    Alumni,
    AlumniDetail,
    AlumniCampaign,
    TransportManagement,
    ReportPublish,
    ReportEffectiveness,
    ScholarshipManagement,
    BrandingKit,
    IdCards,
    Library,
    ScheduledMessages,
    EventRegistration,
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
    deepLinkTarget: DeepLinkTarget? = null,
    // RA-S12 — drives the Comms nav badge from the real unread-thread count.
    messagesViewModel: MessagesViewModel = koinViewModel(),
) {
    // UI_FIDELITY_AUDIT §0.5: Admin.tsx renders under `PhoneFrame dark`, but legacy `dark` == the
    // `.warm` scope, which is a WARM-LIGHT theme (lavender bg, dark ink, white cards) — NOT black.
    // Theme is now applied globally at the NavGraphV2 level from user preference.
    var tab by remember { mutableStateOf("home") }
    var overlay by remember { mutableStateOf(SchoolOverlay.None) }
    // Track which screen launched the create-event wizard so onCreated returns there.
    var createEventOrigin by remember { mutableStateOf(SchoolOverlay.AcademicCalendarPlatform) }

    val scope = rememberCoroutineScope()
    val alumniRepo = koinInject<AlumniRepository>()
    val prefs = koinInject<PreferenceRepository>()

    fun graduateStudents(studentIds: List<String>, year: Int) {
        scope.launch {
            val token = prefs.getUserToken().first() ?: return@launch
            alumniRepo.graduateStudents(token, GraduateStudentsRequest(studentIds, year))
        }
    }

    // Apply deep-link routing: set tab from the typed target.
    LaunchedEffect(deepLinkTarget) {
        when (deepLinkTarget) {
            is DeepLinkTarget.SchoolScreen -> {
                if (deepLinkTarget.screen == "transport") {
                    overlay = SchoolOverlay.TransportManagement
                } else if (deepLinkTarget.screen == "report-card" || deepLinkTarget.screen == "report-review") {
                    overlay = SchoolOverlay.ReportPublish
                } else if (deepLinkTarget.screen == "library") {
                    overlay = SchoolOverlay.Library
                } else if (deepLinkTarget.screen == "events") {
                    overlay = SchoolOverlay.EventRegistration
                } else {
                    tab = deepLinkTarget.screen
                }
            }
            else -> Unit
        }
    }
    // RA-45 — id carried into the student/teacher profile overlays.
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    var selectedTeacherId by remember { mutableStateOf<String?>(null) }
    // PEWS — student code carried into the early-warning detail overlay.
    var selectedPewsStudentCode by remember { mutableStateOf<String?>(null) }
    // RA-S17 — id carried into the non-teaching-staff profile overlay.
    var selectedStaffId by remember { mutableStateOf<String?>(null) }
    // Health Records — student id + name carried into the health records overlay.
    var healthStudentId by remember { mutableStateOf<String?>(null) }
    var healthStudentName by remember { mutableStateOf<String?>(null) }
    // Alumni Management — selected alumni/campaign IDs for detail overlays.
    var selectedAlumniId by remember { mutableStateOf<String?>(null) }
    var selectedCampaignId by remember { mutableStateOf<String?>(null) }
        // RA-S12 — the Comms badge counts message threads with unread messages
        // (GET /school/messages/threads), not a hardcoded literal.
        val messagesState by messagesViewModel.state.collectAsStateV2()
        val commsBadge = messagesState.threads.count { it.unreadCount > 0 }
        var peopleRefreshKey by remember { mutableIntStateOf(0) }
        var studentRefreshKey by remember { mutableIntStateOf(0) }
        // §11 cross-platform — Android predictive back / iOS edge-swipe pops
        // the full-screen Notifications/Calendar overlay back to the admin tabs
        // instead of leaving the portal. Mirrors the React `onBack` wiring.
        BackHandler(enabled = overlay != SchoolOverlay.None) {
            overlay = SchoolOverlay.None
        }

        when (overlay) {
            SchoolOverlay.Notifications -> {
                NotificationsScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.Calendar -> {
                // Legacy read-only month grid (kept for back-compat / deep-links).
                AcademicCalendarScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.AcademicCalendarPlatform -> {
                // VP-CAL — the premium centralized planning & scheduling platform.
                AcademicCalendarPlatformScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    onCreateEvent = {
                        createEventOrigin = SchoolOverlay.AcademicCalendarPlatform
                        overlay = SchoolOverlay.CreateEvent
                    },
                    onOpenEvent = { /* event detail handled in-screen via overflow actions */ },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.CreateEvent -> {
                // Unified 3-step create-event/announcement screen.
                UnifiedCreateEventScreenV2(
                    onBack = { overlay = createEventOrigin },
                    onCreated = { overlay = createEventOrigin },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.AcademicYear -> {
                AcademicYearManagementScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.Messages -> {
                MessagesScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.LeaveRequests -> {
                LeaveRequestsScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.LinkRequests -> {
                // RA-48: the parent→child link approval queue.
                LinkRequestsScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.AdmissionsCRM -> {
                AdmissionsCrmScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.Results -> {
                ResultsPublishScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.SchedulePTM -> {
                SchedulePtmScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.DailyAttendance -> {
                DailyAttendanceScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.ClassPerformance -> {
                ClassPerformanceScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.TeacherPerformance -> {
                TeacherPerformanceScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.AnalyticsDashboard -> {
                AnalyticsDashboardScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.EditProfile -> {
                // RA-47 — edit the live schools row (institutional profile).
                EditSchoolProfileScreenV2(onBack = { overlay = SchoolOverlay.None }, modifier = modifier)
                return
            }
            SchoolOverlay.StudentRoster -> {
                // RA-45 — the live student roster; rows open a student profile.
                StudentRosterScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    onOpenStudent = { id -> selectedStudentId = id; overlay = SchoolOverlay.StudentProfile },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.StudentProfile -> {
                // RA-45 — single student record (attendance/marks/leave/fees).
                // RA-S17 — reached from the People→Students sub-tab; back pops to
                // the People tab. `onRemoved` also pops back so the roster refreshes.
                val id = selectedStudentId
                if (id == null) { overlay = SchoolOverlay.None; return }
                StudentProfileScreenV2(
                    studentId = id,
                    onBack = { overlay = SchoolOverlay.None },
                    onRemoved = { overlay = SchoolOverlay.None
                        studentRefreshKey++
                                },
                    onOpenHealth = { sid, sname ->
                        healthStudentId = sid
                        healthStudentName = sname
                        overlay = SchoolOverlay.HealthRecords
                    },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.PewsCohort -> {
                // PEWS — the live at-risk cohort; rows open the per-student signal.
                PewsCohortScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    onOpenStudent = { code ->
                        selectedPewsStudentCode = code
                        overlay = SchoolOverlay.PewsStudentDetail
                    },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.PewsStudentDetail -> {
                // PEWS — one student's deterministic signal bundle + AI explanation.
                val code = selectedPewsStudentCode
                if (code == null) { overlay = SchoolOverlay.PewsCohort; return }
                PewsStudentDetailScreenV2(
                    studentCode = code,
                    onBack = { overlay = SchoolOverlay.PewsCohort },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.TeacherProfile -> {
                // RA-45 — single teacher detail (assignments/coverage).
                val id = selectedTeacherId
                if (id == null) { overlay = SchoolOverlay.None; return }
                TeacherProfileScreenV2(
                    teacherId = id,
                    onBack = { overlay = SchoolOverlay.None },
                    onRemoved = { overlay = SchoolOverlay.None
                        peopleRefreshKey++ },
                    // RA-TAM — Quick Action → reusable assignment module.
                    onOpenAssignments = { overlay = SchoolOverlay.TeacherAssignments },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.TeacherAssignments -> {
                // RA-TAM — the single reusable Teacher Assignment Management
                // screen, reached from Teacher Listing and Teacher Profile.
                val id = selectedTeacherId
                if (id == null) { overlay = SchoolOverlay.None; return }
                TeacherAssignmentManagementScreen(
                    teacherId = id,
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.Staff -> {
                // RA-S17 — single non-teaching-staff record; delete-in-profile.
                val id = selectedStaffId
                if (id == null) { overlay = SchoolOverlay.None; return }
                StaffProfileScreenV2(
                    staffId = id,
                    onBack = { overlay = SchoolOverlay.None },
                    onRemoved = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.HealthRecords -> {
                val id = healthStudentId
                val name = healthStudentName ?: "Student"
                if (id == null) { overlay = SchoolOverlay.None; return }
                HealthRecordsScreenV2(
                    studentId = id,
                    studentName = name,
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.Alumni -> {
                AlumniScreen(
                    onBack = { overlay = SchoolOverlay.None },
                    onOpenAlumni = { id -> selectedAlumniId = id; overlay = SchoolOverlay.AlumniDetail },
                    onOpenCampaign = { id -> selectedCampaignId = id; overlay = SchoolOverlay.AlumniCampaign },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.AlumniDetail -> {
                val id = selectedAlumniId
                if (id == null) { overlay = SchoolOverlay.None; return }
                AlumniDetailScreen(
                    alumniId = id,
                    onBack = { overlay = SchoolOverlay.Alumni },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.AlumniCampaign -> {
                val id = selectedCampaignId
                if (id == null) { overlay = SchoolOverlay.None; return }
                AlumniCampaignScreen(
                    campaignId = id,
                    onBack = { overlay = SchoolOverlay.Alumni },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.TransportManagement -> {
                TransportManagementScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.ReportPublish -> {
                AdminReportPublishScreen(
                    onBack = { overlay = SchoolOverlay.None },
                )
                return
            }
            SchoolOverlay.ReportEffectiveness -> {
                AdminReportingEffectivenessScreen(
                    onBack = { overlay = SchoolOverlay.None },
                )
                return
            }
            SchoolOverlay.ScholarshipManagement -> {
                ScholarshipManagementScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.BrandingKit -> {
                BrandingSettingsScreen(
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.IdCards -> {
                IdCardScreen(
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.Library -> {
                SchoolLibraryScreen(
            SchoolOverlay.ScheduledMessages -> {
                ScheduledMessagesScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.EventRegistration -> {
                AdminEventRegistrationScreenV2(
                    onBack = { overlay = SchoolOverlay.None },
                    modifier = modifier,
                )
                return
            }
            SchoolOverlay.None -> Unit
        }

        val items = listOf(
            VNavItem("home", "Home", VIcons.Home),
            VNavItem("people", "People", VIcons.Users),
            VNavItem("records", "Records", VIcons.Bookmark),
            VNavItem("comms", "Comms", VIcons.Megaphone, badge = commsBadge),
            VNavItem("settings", "Settings", VIcons.Settings),
        )

        VScreenScaffold(
            modifier = modifier,
            bottomBar = {
                VBottomNav(items = items, selected = tab, onSelect = { tab = it })
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    "home" -> SchoolHomeScreenV2(
                        onOpenNotifications = { overlay = SchoolOverlay.Notifications },
                        // VP-CAL — Home now opens the premium Academic Calendar platform.
                        onOpenCalendar = { overlay = SchoolOverlay.AcademicCalendarPlatform },
                        // RA-24 — the Home "live metrics" / PEWS cards now open the
                        // real analytics dashboard and the at-risk cohort (People
                        // tab) instead of dead Coming-Soon placeholders.
                        onOpenAnalytics = { overlay = SchoolOverlay.AnalyticsDashboard },
                        onOpenPews = { overlay = SchoolOverlay.PewsCohort },
                        onOpenTransport = { overlay = SchoolOverlay.TransportManagement },
                        onOpenReportPublish = { overlay = SchoolOverlay.ReportPublish },
                        onOpenReportEffectiveness = { overlay = SchoolOverlay.ReportEffectiveness },
                        onOpenEvents = { overlay = SchoolOverlay.EventRegistration },
                        // Unified create-event entry from Home quick action.
                        onCreateEvent = {
                            createEventOrigin = SchoolOverlay.None
                            overlay = SchoolOverlay.CreateEvent
                        },
                        // §7 finding K — tapping the avatar opens the Settings tab (where logout
                        // lives), instead of logging the admin out outright.
                        onExit = { tab = "settings" },
                    )
                    "people" -> SchoolPeopleScreenV2(
                        teacherRefreshKey = peopleRefreshKey,
                        studentRefreshKey = studentRefreshKey,
                        // RA-48 — open the parent→child link approval queue.
                        onOpenLinkRequests = { overlay = SchoolOverlay.LinkRequests },
                        // RA-S17 — People is now a 3-sub-tab roster; rows open the
                        // matching profile overlay (delete-in-profile lives there).
                        onOpenStudent = { id -> selectedStudentId = id; overlay = SchoolOverlay.StudentProfile },
                        onOpenTeacher = { id -> selectedTeacherId = id; overlay = SchoolOverlay.TeacherProfile },
                        // RA-TAM — Teacher Listing entry point into the reusable module.
                        onAssignClasses = { id -> selectedTeacherId = id; overlay = SchoolOverlay.TeacherAssignments },
                        onOpenStaff = { id -> selectedStaffId = id; overlay = SchoolOverlay.Staff },
                        // Alumni Management — opens the alumni directory overlay.
                        onOpenAlumni = { overlay = SchoolOverlay.Alumni },
                        // Mark students as alumni (graduation bulk action)
                        onGraduateStudents = { studentIds, year ->
                            graduateStudents(studentIds, year)
                        },
                    )
                    "records" -> SchoolRecordsScreenV2()
                    "comms" -> SchoolCommsScreenV2(
                        // RA-24 — Messages and PTM open their real backend-backed
                        // screens as overlays instead of Coming-Soon cards.
                        onOpenMessages = { overlay = SchoolOverlay.Messages },
                        onOpenPtm = { overlay = SchoolOverlay.SchedulePTM },
                        onOpenScheduledMessages = { overlay = SchoolOverlay.ScheduledMessages },
                        // Unified create-event entry from Announcements tab.
                        onCreateEvent = {
                            createEventOrigin = SchoolOverlay.None
                            overlay = SchoolOverlay.CreateEvent
                        },
                    )
                    "settings" -> SchoolSettingsScreenV2(
                        onLogout = onLogout,
                        // RA-24 — "Teacher management" opens the live People tab
                        // roster (RA-22) rather than a Coming-Soon label.
                        onOpenTeachers = { tab = "people" },
                        // RA-47 — open the editable institutional-profile screen.
                        onOpenProfile = { overlay = SchoolOverlay.EditProfile },
                        // VP-CAL — "Academic year" is now a real management screen.
                        onOpenAcademicYear = { overlay = SchoolOverlay.AcademicYear },
                        // Transport Management — routes, vehicles & assignments.
                        onOpenTransport = { overlay = SchoolOverlay.TransportManagement },
                        // Scholarship Management — schemes, applications & renewals.
                        onOpenScholarships = { overlay = SchoolOverlay.ScholarshipManagement },
                        // School Branding Kit — colors, logo, subdomain.
                        onOpenBranding = { overlay = SchoolOverlay.BrandingKit },
                        // ID Card Generation — templates, card generation, PDF export.
                        onOpenIdCards = { overlay = SchoolOverlay.IdCards },
                        // Library Management — catalog, issues, returns, fines.
                        onOpenLibrary = { overlay = SchoolOverlay.Library },
                    )
                }
            }
        }
}
