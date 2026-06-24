package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherSelfLeaveDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ActionResult
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedDayUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherLeaveUiState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileActionsViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTodayViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VConfirmDialog
import com.littlebridge.vidyaprayag.ui.v2.components.VDatePicker
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherProfileScreenV2 — REBUILT (T-602b, Doc 04 §5.14). DELETE-don't-patch: the old
 * screen's inert "Notification preferences" / "Change password" rows (F-PROF-1/F-PROF-2)
 * are gone. The Profile is now the teacher's own data + controls in four honest sections:
 *
 *   1. Identity — avatar, name, username, school, subject badges (TeacherProfileViewModel).
 *   2. My Schedule — the FULL weekly timetable (reuses TeacherTodayViewModel.week, the
 *      T-104 server-resolved week; honest holiday/empty per day).
 *   3. My Leave — apply (inline sheet: date-from/to + reason + optional image URL) and the
 *      live status list (TeacherProfileActionsViewModel over the T-602a /teacher/leave API).
 *   4. Settings — REAL change-password (inline form → AuthRepository, the RA-54 endpoint),
 *      theme switch (Warm / Light / Night via the global pref the portal reads), the leave
 *      inbox shortcut (onOpenLeave), and a confirmed logout.
 *
 * Every data surface uses VStateHost; the leave list shows an honest "No leave requests."
 */
@Composable
fun TeacherProfileScreenV2(
    onLogout: () -> Unit = {},
    onOpenLeave: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherProfileViewModel = koinViewModel(),
    actionsViewModel: TeacherProfileActionsViewModel = koinViewModel(),
    scheduleViewModel: TeacherTodayViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val leave by actionsViewModel.leave.collectAsStateV2()
    val applyResult by actionsViewModel.apply.collectAsStateV2()
    val passwordResult by actionsViewModel.password.collectAsStateV2()
    val themeName by actionsViewModel.themeName.collectAsStateV2()
    val schedule by scheduleViewModel.state.collectAsStateV2()

    TeacherProfileContent(
        state = state,
        leave = leave,
        applyResult = applyResult,
        passwordResult = passwordResult,
        themeName = themeName,
        schedule = schedule,
        onLogout = onLogout,
        onOpenLeaveInbox = onOpenLeave,
        onRetry = viewModel::load,
        onRetrySchedule = scheduleViewModel::load,
        onRetryLeave = actionsViewModel::loadLeave,
        onApplyLeave = actionsViewModel::applyLeave,
        onClearApply = actionsViewModel::clearApplyResult,
        onChangePassword = actionsViewModel::changePassword,
        onClearPassword = actionsViewModel::clearPasswordResult,
        onSetTheme = actionsViewModel::setTheme,
        modifier = modifier,
    )
}

@Composable
private fun TeacherProfileContent(
    state: TeacherProfileState,
    leave: TeacherLeaveUiState,
    applyResult: ActionResult,
    passwordResult: ActionResult,
    themeName: String,
    schedule: TeacherTodayState,
    onLogout: () -> Unit,
    onOpenLeaveInbox: () -> Unit,
    onRetry: () -> Unit,
    onRetrySchedule: () -> Unit,
    onRetryLeave: () -> Unit,
    onApplyLeave: (String, String, String, String?) -> Unit,
    onClearApply: () -> Unit,
    onChangePassword: (String?, String, String) -> Unit,
    onClearPassword: () -> Unit,
    onSetTheme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var showLogoutConfirm by remember { mutableStateOf(false) }

    VConfirmDialog(
        visible = showLogoutConfirm,
        title = "Log out?",
        message = "You'll need to sign in again to manage your classes.",
        confirmLabel = "Log out",
        onConfirm = { showLogoutConfirm = false; onLogout() },
        onDismiss = { showLogoutConfirm = false },
        icon = VIcons.AlertTriangle,
    )

    Box(
        modifier
            .fillMaxSize()
            // Same aurora wash as the parents portal home — a barely-there lavender whisper
            // top-left over the website background token, so Profile reads as part of the
            // same surface (not a floating dialog).
            .background(c.background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accent.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.02f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        // CRITICAL LAYOUT FIX (carried from ParentHomeScreenV2): the body used to live inside
        // VStateHost, whose loading leg drives an AnimatedContent. AnimatedContent lays out
        // Box-like (it STACKS children to crossfade), and nested inside a verticalScroll (an
        // UNBOUNDED-height parent) the settled Content child COLLAPSED — every section painted
        // on top of each other at y=0, so only the last (Settings) card was visible floating in
        // dead space (exactly the screenshot bug). The state legs are now resolved OUTSIDE the
        // scroll in bounded centered Boxes, and the real content is a plain verticalScroll Column
        // with NO AnimatedContent in its parentage.
        val me = state.profile
        when {
            state.isLoading && me == null ->
                ProfileCenterState { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonProfile() }

            state.error != null && me == null ->
                ProfileCenterState {
                    com.littlebridge.vidyaprayag.ui.v2.screens.VErrorState(message = state.error ?: "", onRetry = onRetry)
                }

            me == null ->
                ProfileCenterState {
                    com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState(
                        icon = VIcons.User,
                        title = "Profile unavailable",
                        body = "We couldn't load your profile. Please try again.",
                    )
                }

            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .statusBarsPadding()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(top = 16.dp, bottom = 140.dp),
                ) {
                    // ── 1. Identity hero ─────────────────────────────────────────
                    ProfileIdentityHero(
                        name = me.name,
                        username = me.username,
                        schoolName = me.schoolName,
                        photoUrl = me.photoUrl,
                        subjects = me.subjects,
                        classes = me.classes,
                    )

                    Spacer(Modifier.height(20.dp))
                    ProfileScheduleSection(schedule = schedule, onRetry = onRetrySchedule)
                    Spacer(Modifier.height(20.dp))
                    ProfileLeaveSection(
                        leave = leave,
                        applyResult = applyResult,
                        onRetry = onRetryLeave,
                        onApply = onApplyLeave,
                        onClearApply = onClearApply,
                    )
                    Spacer(Modifier.height(20.dp))
                    ProfileSettingsSection(
                        phone = me.phone,
                        email = me.email,
                        themeName = themeName,
                        passwordResult = passwordResult,
                        onChangePassword = onChangePassword,
                        onClearPassword = onClearPassword,
                        onSetTheme = onSetTheme,
                        onOpenLeaveInbox = onOpenLeaveInbox,
                        onLogout = { showLogoutConfirm = true },
                    )
                }
            }
        }
    }
}

/** Bounded, centered state slot — fills the canvas once, OUTSIDE any scroll (the layout fix). */
@Composable
private fun ProfileCenterState(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Identity hero — a flagship card (parent-grade) so the teacher's identity reads
//    as a premium surface, never a bare floating block.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileIdentityHero(
    name: String,
    username: String,
    schoolName: String,
    photoUrl: String?,
    subjects: List<String>,
    classes: List<String>,
) {
    val c = VTheme.colors
    VCard(padding = 20.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VAvatar(name = name, src = photoUrl, size = 72.dp, ring = true)
            Column(Modifier.weight(1f)) {
                Text(name.ifBlank { "Teacher" }, style = VTheme.type.h2.colored(c.ink))
                if (username.isNotBlank()) {
                    Text("@$username", style = VTheme.type.dataSm.colored(c.ink2).copy(fontSize = 12.sp))
                }
                if (schoolName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(VIcons.School, contentDescription = null, tint = c.ink3, modifier = Modifier.size(13.dp))
                        Text(schoolName, style = VTheme.type.caption.colored(c.ink3))
                    }
                }
            }
        }

        if (subjects.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("SUBJECTS", style = VTheme.type.label.colored(c.ink3))
            Spacer(Modifier.height(8.dp))
            ChipFlow(items = subjects, tone = VBadgeTone.Accent)
        }

        if (classes.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("CLASSES", style = VTheme.type.label.colored(c.ink3))
            Spacer(Modifier.height(8.dp))
            ChipFlow(items = classes, tone = VBadgeTone.Neutral)
        }
    }
}

/** A wrapping row of chips (subjects / classes) — wraps to multiple lines instead of clipping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(items: List<String>, tone: VBadgeTone) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { VBadge(text = it, tone = tone) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header — a small uppercase eyebrow + optional trailing action.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, trailing: (@Composable () -> Unit)? = null) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title.uppercase(), style = VTheme.type.label.colored(c.accentDeep))
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. My Schedule — the FULL weekly timetable (reuses TeacherTodayViewModel.week,
//    the T-104 server-resolved week). Mon–Sat (weekday 1..6), honest holiday/empty
//    per day. Mirrors TeacherScheduleCard's WeeklyFace language but rebuilt for the
//    Profile surface (no expand/collapse — always-open reference view).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileScheduleSection(schedule: TeacherTodayState, onRetry: () -> Unit) {
    val c = VTheme.colors
    // Mon–Sat, sorted by ISO weekday (1=Mon … 6=Sat). Sunday is intentionally omitted.
    val days = schedule.week.filter { it.weekday in 1..6 }.sortedBy { it.weekday }

    SectionHeader("My schedule")
    VCard(padding = 16.dp) {
        VStateHost(
            loading = schedule.isLoading && schedule.week.isEmpty(),
            error = schedule.error.takeIf { schedule.week.isEmpty() },
            isEmpty = days.isEmpty(),
            emptyTitle = "No weekly timetable yet",
            emptyBody = "Your classes for the week haven't been scheduled.",
            emptyIcon = VIcons.Calendar,
            onRetry = onRetry,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                days.forEachIndexed { index, d ->
                    if (index > 0) VDivider()
                    ScheduleDayRow(d)
                }
            }
        }
    }
}

@Composable
private fun ScheduleDayRow(d: ResolvedDayUi) {
    val c = VTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                weekdayName(d.weekday),
                style = VTheme.type.labelStrong.colored(c.ink2),
            )
            if (d.isHoliday) {
                VBadge(text = d.holidayName ?: "Holiday", tone = VBadgeTone.Danger)
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            d.isHoliday -> Text(
                "Holiday — no classes",
                style = VTheme.type.caption.colored(c.ink3),
            )
            d.periods.isEmpty() -> Text(
                "No classes",
                style = VTheme.type.caption.colored(c.ink3),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                d.periods.sortedBy { it.startTime }.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            p.startTime,
                            style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 12.sp),
                            modifier = Modifier.width(54.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${p.subject} · ${p.classLabel}",
                                style = VTheme.type.body.colored(if (p.isCancelled) c.ink3 else c.ink),
                            )
                            if (p.room.isNotBlank()) {
                                Text(
                                    "Room ${p.room}",
                                    style = VTheme.type.caption.colored(c.ink3),
                                )
                            }
                        }
                        if (p.isCancelled) {
                            VBadge(text = "Cancelled", tone = VBadgeTone.Danger)
                        } else if (p.isSubstituteForMe) {
                            VBadge(text = "Substitute", tone = VBadgeTone.Warning)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. My Leave — apply (inline form: date-from/to + reason + optional image URL)
//    and the live status list (TeacherProfileActionsViewModel over the T-602a
//    /teacher/leave API). Honest "No leave requests." empty.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileLeaveSection(
    leave: TeacherLeaveUiState,
    applyResult: ActionResult,
    onRetry: () -> Unit,
    onApply: (String, String, String, String?) -> Unit,
    onClearApply: () -> Unit,
) {
    val c = VTheme.colors
    var formOpen by remember { mutableStateOf(false) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    // On a successful apply, collapse + reset the form, then clear the transient result.
    LaunchedEffect(applyResult) {
        if (applyResult is ActionResult.Success) {
            formOpen = false
            dateFrom = ""; dateTo = ""; reason = ""; imageUrl = ""
        }
    }

    SectionHeader(
        title = "My leave",
        trailing = {
            // ≥48dp tap target (Doc 10 §3 hard floor) — the affordance is a small label but
            // its hit-box is a full 48dp-tall row so it's reliably tappable.
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        formOpen = !formOpen
                        if (!formOpen) onClearApply()
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (formOpen) "Close" else "Apply",
                    style = VTheme.type.label.colored(c.accent),
                )
            }
        },
    )

    // ── Inline apply form (no bottom-sheet component exists — use an expandable card) ──
    AnimatedVisibility(visible = formOpen) {
        VCard(padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VDatePicker(
                    value = dateFrom,
                    onValueChange = { dateFrom = it; onClearApply() },
                    label = "From",
                    placeholder = "Start date",
                )
                VDatePicker(
                    value = dateTo,
                    onValueChange = { dateTo = it; onClearApply() },
                    label = "To",
                    placeholder = "End date",
                    isError = dateFrom.isNotBlank() && dateTo.isNotBlank() && dateTo < dateFrom,
                )
                VInput(
                    value = reason,
                    onValueChange = { reason = it; onClearApply() },
                    label = "Reason",
                    placeholder = "Why are you taking leave?",
                    singleLine = false,
                    leadingIcon = VIcons.FileText,
                )
                VInput(
                    value = imageUrl,
                    onValueChange = { imageUrl = it; onClearApply() },
                    label = "Attachment URL (optional)",
                    placeholder = "https://…",
                    leadingIcon = VIcons.Upload,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                )

                if (applyResult is ActionResult.Failure) {
                    Text(applyResult.message, style = VTheme.type.caption.colored(c.dangerInk))
                }

                VButton(
                    text = "Submit request",
                    onClick = { onApply(dateFrom, dateTo, reason, imageUrl.ifBlank { null }) },
                    variant = VButtonVariant.Primary,
                    full = true,
                    loading = applyResult is ActionResult.InFlight,
                    enabled = applyResult !is ActionResult.InFlight,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // ── Status list ───────────────────────────────────────────────────────────
    VCard(padding = 16.dp) {
        VStateHost(
            loading = leave.isLoading && leave.requests.isEmpty(),
            error = leave.error.takeIf { leave.requests.isEmpty() },
            isEmpty = leave.requests.isEmpty(),
            emptyTitle = "No leave requests",
            emptyBody = "When you apply for leave it'll show up here with its status.",
            emptyIcon = VIcons.Calendar,
            onRetry = onRetry,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                leave.requests.forEachIndexed { index, req ->
                    if (index > 0) VDivider()
                    LeaveRequestRow(req)
                }
            }
        }
    }
}

@Composable
private fun LeaveRequestRow(req: TeacherSelfLeaveDto) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (req.dateFrom == req.dateTo) req.dateFrom else "${req.dateFrom} → ${req.dateTo}",
                style = VTheme.type.bodyStrong.colored(c.ink),
            )
            if (req.reason.isNotBlank()) {
                Text(req.reason, style = VTheme.type.caption.colored(c.ink3))
            }
        }
        LeaveStatusPill(req.status)
    }
}

/** Status pill — colour AND text (never colour-alone, for accessibility). */
@Composable
private fun LeaveStatusPill(status: String) {
    val tone = when (status.uppercase()) {
        "APPROVED", "ACCEPTED" -> VBadgeTone.Success
        "REJECTED", "DENIED" -> VBadgeTone.Danger
        "PENDING" -> VBadgeTone.Warning
        else -> VBadgeTone.Neutral
    }
    val label = when (status.uppercase()) {
        "APPROVED", "ACCEPTED" -> "Approved"
        "REJECTED", "DENIED" -> "Rejected"
        "PENDING" -> "Pending"
        else -> status.lowercase().replaceFirstChar { it.uppercase() }
    }
    VBadge(text = label, tone = tone)
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Settings — REAL change-password (inline form → AuthRepository / RA-54), theme
//    switch (Warm / Light / Night → global pref the portal reads), the leave inbox
//    shortcut, and a confirmed logout.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileSettingsSection(
    phone: String,
    email: String,
    themeName: String,
    passwordResult: ActionResult,
    onChangePassword: (String?, String, String) -> Unit,
    onClearPassword: () -> Unit,
    onSetTheme: (String) -> Unit,
    onOpenLeaveInbox: () -> Unit,
    onLogout: () -> Unit,
) {
    val c = VTheme.colors

    SectionHeader("Settings")
    VCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Contact (read-only) — honest display of what the school has on file.
            if (email.isNotBlank()) {
                ContactRow(icon = VIcons.Mail, label = "Email", value = email)
            }
            if (phone.isNotBlank()) {
                ContactRow(icon = VIcons.Phone, label = "Phone", value = phone)
            }
            if (email.isNotBlank() || phone.isNotBlank()) VDivider()

            // Theme switch — REAL global preference (the portal reads this).
            Text("Appearance", style = VTheme.type.labelStrong.colored(c.ink2))
            ThemeSegmentedControl(themeName = themeName, onSetTheme = onSetTheme)

            VDivider()

            // Change password — REAL (RA-54).
            ChangePasswordForm(
                passwordResult = passwordResult,
                onChangePassword = onChangePassword,
                onClearPassword = onClearPassword,
            )

            VDivider()

            // Leave inbox shortcut (approve students' leave — the teacher's inbox tab).
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp) // Doc 10 §3 — hard floor for Mr. Rao.
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenLeaveInbox() }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(VIcons.ClipboardList, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(20.dp))
                Text("Leave inbox", style = VTheme.type.body.colored(c.ink), modifier = Modifier.weight(1f))
                Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
            }

            VDivider()

            VButton(
                text = "Log out",
                onClick = onLogout,
                variant = VButtonVariant.Destructive,
                full = true,
                soft = true,
            )
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val c = VTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = VTheme.type.caption.colored(c.ink3))
            Text(value, style = VTheme.type.body.colored(c.ink))
        }
    }
}

/** Warm / Light / Night segmented control writing the global theme pref. */
@Composable
private fun ThemeSegmentedControl(themeName: String, onSetTheme: (String) -> Unit) {
    val c = VTheme.colors
    val options = listOf("WARM" to "Warm", "LIGHT" to "Light", "NIGHT" to "Night")
    val selected = themeName.uppercase()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.cream)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            val active = selected == key
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp) // Doc 10 §3 — hard floor for Mr. Rao.
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) c.accent else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (!active) onSetTheme(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = VTheme.type.bodyStrong.colored(if (active) Color.White else c.ink2),
                )
            }
        }
    }
}

/** Inline change-password form → AuthRepository.changePassword (RA-54). */
@Composable
private fun ChangePasswordForm(
    passwordResult: ActionResult,
    onChangePassword: (String?, String, String) -> Unit,
    onClearPassword: () -> Unit,
) {
    val c = VTheme.colors
    var open by remember { mutableStateOf(false) }
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }

    // On success, collapse + reset, then clear the transient result.
    LaunchedEffect(passwordResult) {
        if (passwordResult is ActionResult.Success) {
            open = false
            oldPw = ""; newPw = ""; confirmPw = ""
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp) // Doc 10 §3 — hard floor for Mr. Rao.
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                open = !open
                if (!open) onClearPassword()
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(VIcons.Lock, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(20.dp))
        Text("Change password", style = VTheme.type.body.colored(c.ink), modifier = Modifier.weight(1f))
        Icon(
            if (open) VIcons.ChevronUp else VIcons.ChevronDown,
            contentDescription = null,
            tint = c.ink3,
            modifier = Modifier.size(18.dp),
        )
    }

    AnimatedVisibility(visible = open) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
            VInput(
                value = oldPw,
                onValueChange = { oldPw = it; onClearPassword() },
                label = "Current password",
                placeholder = "Current password",
                isPassword = true,
                passwordVisible = reveal,
                leadingIcon = VIcons.Lock,
            )
            VInput(
                value = newPw,
                onValueChange = { newPw = it; onClearPassword() },
                label = "New password",
                placeholder = "At least 8 characters",
                isPassword = true,
                passwordVisible = reveal,
                leadingIcon = VIcons.ShieldCheck,
                trailing = {
                    // ≥48dp tap target around the small Eye glyph (Doc 10 §3).
                    Box(
                        Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { reveal = !reveal },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            VIcons.Eye,
                            contentDescription = if (reveal) "Hide passwords" else "Show passwords",
                            tint = c.ink3,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            VInput(
                value = confirmPw,
                onValueChange = { confirmPw = it; onClearPassword() },
                label = "Confirm new password",
                placeholder = "Re-enter new password",
                isPassword = true,
                passwordVisible = reveal,
                leadingIcon = VIcons.ShieldCheck,
            )
            if (confirmPw.isNotBlank() && confirmPw != newPw) {
                Text("Passwords don't match", style = VTheme.type.caption.colored(c.dangerInk))
            }

            when (passwordResult) {
                is ActionResult.Failure -> Text(passwordResult.message, style = VTheme.type.caption.colored(c.dangerInk))
                is ActionResult.Success -> Text(passwordResult.message, style = VTheme.type.caption.colored(c.successInk))
                else -> Unit
            }

            VButton(
                text = "Update password",
                onClick = { onChangePassword(oldPw.ifBlank { null }, newPw, confirmPw) },
                variant = VButtonVariant.Primary,
                full = true,
                loading = passwordResult is ActionResult.InFlight,
                enabled = passwordResult !is ActionResult.InFlight,
            )
        }
    }
}

/** ISO weekday (1=Mon … 7=Sun) → display name. */
private fun weekdayName(weekday: Int): String = when (weekday) {
    1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
    5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"; else -> "—"
}
