package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
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
import com.littlebridge.vidyaprayag.feature.admin.presentation.RiskStudent
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolTeachersState
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolTeachersViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsState
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherRosterItem
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VConfirmDialog
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.staggeredItemEntrance
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * SchoolPeopleScreenV2 — wired to the real [StudentAnalyticsViewModel]
 * (`AnalyticsApi` → `GET /api/v1/student-cohort`).
 *
 * The backend's people endpoint is a **cohort-analytics** feed (risk distribution,
 * at-risk students, subject engagement, cohort comparison) — not a flat student/teacher
 * roster. So this screen renders the analytics it actually returns rather than the old
 * mock roster + per-student detail (which had no backing endpoint). A directory/roster
 * screen is tracked as a later addition. No MockV2 in production; the three UI states
 * come from [VStateHost] (LAW 2/3/6).
 */
@Composable
fun SchoolPeopleScreenV2(
    modifier: Modifier = Modifier,
    viewModel: StudentAnalyticsViewModel = koinViewModel(),
    teachersViewModel: SchoolTeachersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val teachersState by teachersViewModel.state.collectAsStateV2()
    SchoolPeopleContent(
        state = state,
        onRetry = viewModel::load,
        teachersState = teachersState,
        onTeachersRetry = teachersViewModel::load,
        onAddTeacher = teachersViewModel::addTeacher,
        onRemoveTeacher = teachersViewModel::removeTeacher,
        modifier = modifier,
    )
}

@Composable
private fun SchoolPeopleContent(
    state: StudentAnalyticsState,
    onRetry: () -> Unit,
    teachersState: SchoolTeachersState,
    onTeachersRetry: () -> Unit,
    onAddTeacher: (name: String, identifier: String, initialPassword: String?, onAdded: (() -> Unit)?) -> Unit,
    onRemoveTeacher: (teacherId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    var showAddTeacher by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<TeacherRosterItem?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("People", style = VTheme.type.h1.colored(c.ink))

        // ── Teacher roster (RA-22) ─────────────────────────────────────────
        TeacherRosterSection(
            state = teachersState,
            onRetry = onTeachersRetry,
            onAddClick = { showAddTeacher = true },
            onRemoveClick = { pendingRemoval = it },
        )

        Text("Cohort analytics", style = VTheme.type.h3.colored(c.ink))

        VStateHost(
            loading = state.isLoading,
            error = state.errorMessage,
            isEmpty = state.atRiskStudents.isEmpty() &&
                state.subjectEngagements.isEmpty() &&
                state.criticalRiskCount == 0 &&
                state.mediumRiskCount == 0 &&
                state.lowRiskCount == 0,
            emptyTitle = "No cohort data yet",
            emptyBody = "Student risk and engagement analytics appear here once attendance and marks start flowing in.",
            emptyIcon = VIcons.Users,
            onRetry = onRetry,
            skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 6) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ── Risk distribution ─────────────────────────────────────────
                VCard {
                    VLabel("Student risk distribution")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RiskTile("Critical", state.criticalRiskCount, c.dangerInk, Modifier.weight(1f))
                        RiskTile("Medium", state.mediumRiskCount, c.warningInk, Modifier.weight(1f))
                        RiskTile("Low", state.lowRiskCount, c.successInk, Modifier.weight(1f))
                    }
                }

                // ── At-risk students ──────────────────────────────────────────
                if (state.atRiskStudents.isNotEmpty()) {
                    Column {
                        Text("At-risk students", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.atRiskStudents.forEach { RiskStudentRow(it) }
                        }
                    }
                }

                // ── Subject engagement ────────────────────────────────────────
                if (state.subjectEngagements.isNotEmpty()) {
                    VCard {
                        Text("Subject engagement", style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.subjectEngagements.forEach { e ->
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
                                    // Capture into a local val so Kotlin can smart-cast — the
                                    // public `status: String?` on `SubjectEngagement` lives in
                                    // the `shared/` module and cannot be smart-cast directly
                                    // through `!e.status.isNullOrBlank()`.
                                    val status = e.status
                                    if (!status.isNullOrBlank()) {
                                        Text(status, style = VTheme.type.label.colored(c.ink3))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cohort comparison ─────────────────────────────────────────
                if (state.cohortComparison.isNotEmpty()) {
                    VCard {
                        Text("Cohort comparison", style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.cohortComparison.forEachIndexed { i, v ->
                                val label = state.cohortLabels.getOrNull(i) ?: "Grade ${i + 1}"
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

    // ── Remove-teacher confirmation (RA-22) ────────────────────────────────
    val removal = pendingRemoval
    VConfirmDialog(
        visible = removal != null,
        title = "Remove teacher",
        message = "Remove ${removal?.name ?: "this teacher"} from your school? " +
            "They will lose access immediately. This can be reversed by re-adding them.",
        confirmLabel = "Remove",
        icon = VIcons.AlertTriangle,
        onConfirm = {
            removal?.let { onRemoveTeacher(it.id) }
            pendingRemoval = null
        },
        onDismiss = { pendingRemoval = null },
    )
}

/**
 * RA-22: the teacher roster. Honours LAW (RULE-7) — loading, error, and empty
 * states all come from [VStateHost]; uses only frozen V* primitives and theme
 * tokens (no Material defaults, no new tokens).
 */
@Composable
private fun TeacherRosterSection(
    state: SchoolTeachersState,
    onRetry: () -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: (TeacherRosterItem) -> Unit,
) {
    val c = VTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        VStateHost(
            loading = state.isLoading,
            error = state.errorMessage,
            isEmpty = state.teachers.isEmpty(),
            emptyTitle = "No teachers yet",
            emptyBody = "Add your first teacher so they can sign in and manage their classes.",
            emptyIcon = VIcons.Users,
            onRetry = onRetry,
            skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 5) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Feature 5 — staggered list entrance after the skeleton → content
                // transition. `trigger` is the "list is loaded and non-empty"
                // condition; it does NOT flip back on refresh so the entrance
                // runs ONCE per data-load. RULE-2: the modifier only animates
                // alpha + translationY via graphicsLayer — no layout shift.
                val ready = state.teachers.isNotEmpty() && !state.isLoading
                state.teachers.forEachIndexed { index, t ->
                    Box(modifier = Modifier.staggeredItemEntrance(index = index, trigger = ready)) {
                        TeacherRosterRow(
                            item = t,
                            mutating = state.isMutating,
                            onRemove = { onRemoveClick(t) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherRosterRow(
    item: TeacherRosterItem,
    mutating: Boolean,
    onRemove: () -> Unit,
) {
    val c = VTheme.colors
    VCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VAvatar(name = item.name, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = VTheme.type.bodyStrong.colored(c.ink))
                if (item.contact.isNotBlank()) {
                    Text(item.contact, style = VTheme.type.caption.colored(c.ink2))
                }
            }
            VButton(
                text = "Remove",
                onClick = onRemove,
                variant = VButtonVariant.Ghost,
                size = VButtonSize.Sm,
                leading = { Icon(VIcons.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                enabled = !mutating,
            )
        }
    }
}

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
