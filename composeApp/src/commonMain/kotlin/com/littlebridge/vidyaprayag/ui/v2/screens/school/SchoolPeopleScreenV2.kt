package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StaffDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StudentDto
import com.littlebridge.vidyaprayag.feature.admin.presentation.RiskStudent
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolTeachersState
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolTeachersViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.StaffRosterState
import com.littlebridge.vidyaprayag.feature.admin.presentation.StaffViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsState
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentRosterState
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentRosterViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherRosterItem
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.staggeredItemEntrance
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * SchoolPeopleScreenV2 — RA-S17 rebuild.
 *
 * The People tab is now a [VTopTabs]-driven 3-sub-tab surface — **Teachers /
 * Students / Non-teaching staff** — each with a search field and tappable,
 * DB-backed rows that open the person's profile. Deletion has been removed from
 * the rows entirely: it now lives inside each profile behind a confirm dialog
 * (RA-S17 directive). The parent→child link-request queue stays as a top entry.
 *
 * Data: teachers via [SchoolTeachersViewModel] (`/school/teachers`), students via
 * [StudentRosterViewModel] (`/school/students?q=&class=`), staff via [StaffViewModel]
 * (`/school/staff?q=&department=`), cohort analytics via [StudentAnalyticsViewModel]
 * (`/student-cohort`). All three states come from [VStateHost] (LAW 2/3/6); no MockV2.
 */
@Composable
fun SchoolPeopleScreenV2(
    modifier: Modifier = Modifier,
    onOpenLinkRequests: () -> Unit = {},
    onOpenStudent: (String) -> Unit = {},
    onOpenTeacher: (String) -> Unit = {},
    onOpenStaff: (String) -> Unit = {},
    viewModel: StudentAnalyticsViewModel = koinViewModel(),
    teachersViewModel: SchoolTeachersViewModel = koinViewModel(),
    studentsViewModel: StudentRosterViewModel = koinViewModel(),
    staffViewModel: StaffViewModel = koinViewModel(),
) {
    val analyticsState by viewModel.state.collectAsStateV2()
    val teachersState by teachersViewModel.state.collectAsStateV2()
    val studentsState by studentsViewModel.state.collectAsStateV2()
    val staffState by staffViewModel.state.collectAsStateV2()

    SchoolPeopleContent(
        analyticsState = analyticsState,
        onAnalyticsRetry = viewModel::load,
        teachersState = teachersState,
        onTeachersRetry = teachersViewModel::load,
        onAddTeacher = teachersViewModel::addTeacher,
        studentsState = studentsState,
        onStudentsRetry = studentsViewModel::load,
        onStudentSearch = { studentsViewModel.load() }, // students VM reloads full list; client-side filter below
        staffState = staffState,
        onStaffRetry = staffViewModel::load,
        onStaffSearch = staffViewModel::onQueryChange,
        onAddStaff = staffViewModel::addStaff,
        onOpenLinkRequests = onOpenLinkRequests,
        onOpenStudent = onOpenStudent,
        onOpenTeacher = onOpenTeacher,
        onOpenStaff = onOpenStaff,
        modifier = modifier,
    )
}

@Composable
private fun SchoolPeopleContent(
    analyticsState: StudentAnalyticsState,
    onAnalyticsRetry: () -> Unit,
    teachersState: SchoolTeachersState,
    onTeachersRetry: () -> Unit,
    onAddTeacher: (name: String, identifier: String, initialPassword: String?, onAdded: (() -> Unit)?) -> Unit,
    studentsState: StudentRosterState,
    onStudentsRetry: () -> Unit,
    onStudentSearch: (String) -> Unit,
    staffState: StaffRosterState,
    onStaffRetry: () -> Unit,
    onStaffSearch: (String) -> Unit,
    onAddStaff: (name: String, role: String, department: String, phone: String, email: String) -> Unit,
    onOpenLinkRequests: () -> Unit,
    onOpenStudent: (String) -> Unit,
    onOpenTeacher: (String) -> Unit,
    onOpenStaff: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    var subTab by remember { mutableStateOf("Teachers") }
    var showAddTeacher by remember { mutableStateOf(false) }
    var showAddStaff by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("People", style = VTheme.type.h1.colored(c.ink), modifier = Modifier.padding(horizontal = 20.dp))

        // ── RA-48: parent→child link approval queue entry ──────────────────
        VCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onOpenLinkRequests)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Child link requests", style = VTheme.type.bodyStrong.colored(c.ink))
                    Text(
                        "Review parents requesting access to a student's records",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
                Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
            }
        }

        // ── RA-S17: sub-tabs ─────────────────────────────────────────────────
        VTopTabs(
            tabs = listOf("Teachers", "Students", "Non-teaching staff"),
            selected = subTab,
            onSelect = { subTab = it },
        )

        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (subTab) {
                "Teachers" -> TeachersSubTab(
                    state = teachersState,
                    onRetry = onTeachersRetry,
                    onAddClick = { showAddTeacher = true },
                    onOpenTeacher = onOpenTeacher,
                )
                "Students" -> StudentsSubTab(
                    state = studentsState,
                    onRetry = onStudentsRetry,
                    onOpenStudent = onOpenStudent,
                    analyticsState = analyticsState,
                    onAnalyticsRetry = onAnalyticsRetry,
                )
                "Non-teaching staff" -> StaffSubTab(
                    state = staffState,
                    onRetry = onStaffRetry,
                    onSearch = onStaffSearch,
                    onAddClick = { showAddStaff = true },
                    onOpenStaff = onOpenStaff,
                )
            }
        }
    }

    // ── Add-teacher dialog (RA-22) ─────────────────────────────────────────
    if (showAddTeacher) {
        AddTeacherDialog(
            isSubmitting = teachersState.isMutating,
            onDismiss = { showAddTeacher = false },
            onSubmit = { name, identifier, password ->
                onAddTeacher(name, identifier, password) { showAddTeacher = false }
            },
        )
    }

    // ── Add-staff dialog (RA-S17) ──────────────────────────────────────────
    if (showAddStaff) {
        AddStaffDialog(
            isSubmitting = staffState.isSaving,
            onDismiss = { showAddStaff = false },
            onSubmit = { name, role, dept, phone, email ->
                onAddStaff(name, role, dept, phone, email)
                showAddStaff = false
            },
        )
    }
}

// ───────────────────────── Teachers sub-tab ─────────────────────────

@Composable
private fun TeachersSubTab(
    state: SchoolTeachersState,
    onRetry: () -> Unit,
    onAddClick: () -> Unit,
    onOpenTeacher: (String) -> Unit,
) {
    val c = VTheme.colors
    var query by remember { mutableStateOf("") }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Teachers", style = VTheme.type.h3.colored(c.ink))
        VButton(
            text = "Add teacher",
            onClick = onAddClick,
            variant = VButtonVariant.Secondary,
            size = VButtonSize.Sm,
            leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(14.dp)) },
            enabled = !state.isMutating,
        )
    }
    VInput(
        value = query,
        onValueChange = { query = it },
        label = "",
        placeholder = "Search by name or contact",
        leadingIcon = VIcons.Search,
        modifier = Modifier.fillMaxWidth(),
    )

    val filtered = state.teachers.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.contact.contains(query, ignoreCase = true)
    }

    VStateHost(
        loading = state.isLoading,
        error = state.errorMessage,
        isEmpty = filtered.isEmpty(),
        emptyTitle = if (state.teachers.isEmpty()) "No teachers yet" else "No matches",
        emptyBody = if (state.teachers.isEmpty())
            "Add your first teacher so they can sign in and manage their classes."
        else "No teacher matches \"$query\".",
        emptyIcon = VIcons.Users,
        onRetry = onRetry,
        skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 5) },
    ) {
        val ready = filtered.isNotEmpty() && !state.isLoading
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filtered.forEachIndexed { index, t ->
                Box(modifier = Modifier.staggeredItemEntrance(index = index, trigger = ready)) {
                    PersonRow(
                        name = t.name,
                        subtitle = t.contact.ifBlank { "Teacher" },
                        onClick = { onOpenTeacher(t.id) },
                    )
                }
            }
        }
    }
}

// ───────────────────────── Students sub-tab ─────────────────────────

@Composable
private fun StudentsSubTab(
    state: StudentRosterState,
    onRetry: () -> Unit,
    onOpenStudent: (String) -> Unit,
    analyticsState: StudentAnalyticsState,
    onAnalyticsRetry: () -> Unit,
) {
    val c = VTheme.colors
    var query by remember { mutableStateOf("") }

    Text("Students", style = VTheme.type.h3.colored(c.ink))
    VInput(
        value = query,
        onValueChange = { query = it },
        label = "",
        placeholder = "Search by name, roll no. or code",
        leadingIcon = VIcons.Search,
        modifier = Modifier.fillMaxWidth(),
    )

    val filtered = state.students.filter {
        query.isBlank() ||
            it.fullName.contains(query, ignoreCase = true) ||
            it.rollNumber.contains(query, ignoreCase = true) ||
            it.studentCode.contains(query, ignoreCase = true) ||
            it.className.contains(query, ignoreCase = true)
    }

    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = filtered.isEmpty(),
        emptyTitle = if (state.students.isEmpty()) "No students yet" else "No matches",
        emptyBody = if (state.students.isEmpty())
            "Students appear here once they are enrolled in your school."
        else "No student matches \"$query\".",
        emptyIcon = VIcons.Users,
        onRetry = onRetry,
        skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 6) },
    ) {
        val ready = filtered.isNotEmpty() && !state.isLoading
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filtered.forEachIndexed { index, s ->
                Box(modifier = Modifier.staggeredItemEntrance(index = index, trigger = ready)) {
                    PersonRow(
                        name = s.fullName,
                        subtitle = "${s.className} · Sec ${s.section} · Roll ${s.rollNumber}",
                        src = s.profilePhotoUrl,
                        onClick = { onOpenStudent(s.id) },
                    )
                }
            }
        }
    }

    // ── Cohort analytics (kept under Students) ──────────────────────────────
    Spacer(Modifier.height(8.dp))
    Text("Cohort analytics", style = VTheme.type.h3.colored(c.ink))
    VStateHost(
        loading = analyticsState.isLoading,
        error = analyticsState.errorMessage,
        isEmpty = analyticsState.atRiskStudents.isEmpty() &&
            analyticsState.subjectEngagements.isEmpty() &&
            analyticsState.criticalRiskCount == 0 &&
            analyticsState.mediumRiskCount == 0 &&
            analyticsState.lowRiskCount == 0,
        emptyTitle = "No cohort data yet",
        emptyBody = "Student risk and engagement analytics appear here once attendance and marks start flowing in.",
        emptyIcon = VIcons.Users,
        onRetry = onAnalyticsRetry,
        skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 4) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            VCard {
                VLabel("Student risk distribution")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RiskTile("Critical", analyticsState.criticalRiskCount, c.dangerInk, Modifier.weight(1f))
                    RiskTile("Medium", analyticsState.mediumRiskCount, c.warningInk, Modifier.weight(1f))
                    RiskTile("Low", analyticsState.lowRiskCount, c.successInk, Modifier.weight(1f))
                }
            }
            if (analyticsState.atRiskStudents.isNotEmpty()) {
                Column {
                    Text("At-risk students", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        analyticsState.atRiskStudents.forEach { RiskStudentRow(it) }
                    }
                }
            }
            if (analyticsState.subjectEngagements.isNotEmpty()) {
                VCard {
                    Text("Subject engagement", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        analyticsState.subjectEngagements.forEach { e ->
                            Column {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(e.name, style = VTheme.type.body.colored(c.ink))
                                    Text("${e.percentage.roundToInt()}%", style = VTheme.type.dataSm.colored(c.ink2))
                                }
                                Spacer(Modifier.height(4.dp))
                                VProgressBar(
                                    value = e.percentage,
                                    tone = if (e.percentage < 60f) VBadgeTone.Warning else VBadgeTone.Arctic,
                                )
                                val status = e.status
                                if (!status.isNullOrBlank()) {
                                    Text(status, style = VTheme.type.label.colored(c.ink3))
                                }
                            }
                        }
                    }
                }
            }
            if (analyticsState.cohortComparison.isNotEmpty()) {
                VCard {
                    Text("Cohort comparison", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        analyticsState.cohortComparison.forEachIndexed { i, v ->
                            val label = analyticsState.cohortLabels.getOrNull(i) ?: "Grade ${i + 1}"
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, style = VTheme.type.body.colored(c.ink))
                                    Text("${v.roundToInt()}%", style = VTheme.type.dataSm.colored(c.ink2))
                                }
                                Spacer(Modifier.height(4.dp))
                                VProgressBar(value = v, tone = VBadgeTone.Arctic)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────── Non-teaching-staff sub-tab ───────────────────────

@Composable
private fun StaffSubTab(
    state: StaffRosterState,
    onRetry: () -> Unit,
    onSearch: (String) -> Unit,
    onAddClick: () -> Unit,
    onOpenStaff: (String) -> Unit,
) {
    val c = VTheme.colors

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Non-teaching staff", style = VTheme.type.h3.colored(c.ink))
        VButton(
            text = "Add staff",
            onClick = onAddClick,
            variant = VButtonVariant.Secondary,
            size = VButtonSize.Sm,
            leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(14.dp)) },
            enabled = !state.isSaving,
        )
    }
    VInput(
        value = state.query,
        onValueChange = onSearch,
        label = "",
        placeholder = "Search by name, role or department",
        leadingIcon = VIcons.Search,
        modifier = Modifier.fillMaxWidth(),
    )

    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.staff.isEmpty(),
        emptyTitle = if (state.query.isBlank()) "No staff yet" else "No matches",
        emptyBody = if (state.query.isBlank())
            "Add office, accounts, library, transport or support staff so they appear here."
        else "No staff matches \"${state.query}\".",
        emptyIcon = VIcons.Users,
        onRetry = onRetry,
        skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 5) },
    ) {
        val ready = state.staff.isNotEmpty() && !state.isLoading
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.staff.forEachIndexed { index, s ->
                Box(modifier = Modifier.staggeredItemEntrance(index = index, trigger = ready)) {
                    PersonRow(
                        name = s.fullName,
                        subtitle = listOfNotNull(s.role, s.department?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        src = s.photoUrl,
                        onClick = { onOpenStaff(s.id) },
                    )
                }
            }
        }
    }
}

// ───────────────────────────── shared row ─────────────────────────────

/**
 * RA-S17: a tap-to-open person row. There is intentionally **no** delete button
 * here — deletion lives inside the profile behind a confirm dialog.
 */
@Composable
private fun PersonRow(
    name: String,
    subtitle: String,
    src: String? = null,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VAvatar(name = name, src = src, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Text(name, style = VTheme.type.bodyStrong.colored(c.ink))
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = VTheme.type.caption.colored(c.ink2))
                }
            }
            Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        }
    }
}

// ───────────────────────────── dialogs ─────────────────────────────

/**
 * RA-22: add-teacher form. A teacher is provisioned by email (with an initial
 * password) or by phone (OTP login). Frozen primitives only.
 */
@Composable
private fun AddTeacherDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, identifier: String, initialPassword: String?) -> Unit,
) {
    val c = VTheme.colors
    var name by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isEmail = identifier.contains("@")
    val canSubmit = name.isNotBlank() &&
        identifier.isNotBlank() &&
        (!isEmail || password.isNotBlank()) &&
        !isSubmitting

    Dialog(onDismissRequest = onDismiss) {
        VCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add teacher", style = VTheme.type.h3.colored(c.ink))
                VInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full name",
                    placeholder = "e.g. Asha Verma",
                    leadingIcon = VIcons.User,
                )
                VInput(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = "Email or phone",
                    placeholder = "teacher@school.edu or 98765 43210",
                    leadingIcon = if (isEmail) VIcons.Mail else VIcons.Phone,
                    keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Text,
                )
                if (isEmail) {
                    VInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Initial password",
                        placeholder = "Shared with the teacher to sign in",
                        leadingIcon = VIcons.Lock,
                        isPassword = true,
                    )
                } else {
                    Text(
                        "This teacher will sign in with a one-time code sent to their phone.",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
                Spacer(Modifier.height(4.dp))
                VButton(
                    text = "Add teacher",
                    onClick = {
                        onSubmit(name, identifier, password.takeIf { isEmail && it.isNotBlank() })
                    },
                    variant = VButtonVariant.Primary,
                    full = true,
                    enabled = canSubmit,
                    loading = isSubmitting,
                )
                VButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = VButtonVariant.Ghost,
                    full = true,
                    enabled = !isSubmitting,
                )
            }
        }
    }
}

/**
 * RA-S17: add-staff form for a non-teaching-staff member. Name + role required;
 * department / phone / email optional. Frozen primitives only.
 */
@Composable
private fun AddStaffDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, role: String, department: String, phone: String, email: String) -> Unit,
) {
    val c = VTheme.colors
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val canSubmit = name.isNotBlank() && role.isNotBlank() && !isSubmitting

    Dialog(onDismissRequest = onDismiss) {
        VCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add staff member", style = VTheme.type.h3.colored(c.ink))
                VInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full name",
                    placeholder = "e.g. Ramesh Kumar",
                    leadingIcon = VIcons.User,
                )
                VInput(
                    value = role,
                    onValueChange = { role = it },
                    label = "Role",
                    placeholder = "e.g. Accountant, Librarian, Security",
                    leadingIcon = VIcons.User,
                )
                VInput(
                    value = department,
                    onValueChange = { department = it },
                    label = "Department (optional)",
                    placeholder = "e.g. Office, Transport",
                    leadingIcon = VIcons.Bookmark,
                )
                VInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone (optional)",
                    placeholder = "98765 43210",
                    leadingIcon = VIcons.Phone,
                    keyboardType = KeyboardType.Phone,
                )
                VInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email (optional)",
                    placeholder = "staff@school.edu",
                    leadingIcon = VIcons.Mail,
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(4.dp))
                VButton(
                    text = "Add staff",
                    onClick = { onSubmit(name, role, department, phone, email) },
                    variant = VButtonVariant.Primary,
                    full = true,
                    enabled = canSubmit,
                    loading = isSubmitting,
                )
                VButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = VButtonVariant.Ghost,
                    full = true,
                    enabled = !isSubmitting,
                )
            }
        }
    }
}

// ───────────────────────────── analytics bits ─────────────────────────────

@Composable
private fun RiskTile(label: String, count: Int, tone: Color, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.toString(), style = VTheme.type.dataLg.colored(tone).copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
    }
}

@Composable
private fun RiskStudentRow(s: RiskStudent) {
    val c = VTheme.colors
    val tone = when (s.riskLevel.lowercase()) {
        "critical" -> c.danger
        "medium" -> c.warning
        else -> c.success
    }
    val badgeTone = when (s.riskLevel.lowercase()) {
        "critical" -> VBadgeTone.Danger
        "medium" -> VBadgeTone.Warning
        else -> VBadgeTone.Success
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VAvatar(name = s.name, src = s.imageUrl.ifBlank { null }, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VStatusDot(color = tone)
                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                }
                if (s.masteryTrend.isNotBlank()) {
                    Text("Mastery: ${s.masteryTrend}", style = VTheme.type.caption.colored(c.ink2))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                VBadge(text = s.riskLevel, tone = badgeTone)
                Text("${s.retentionRisk}% risk", style = VTheme.type.label.colored(c.ink3).copy(fontSize = 10.sp))
            }
        }
    }
}
