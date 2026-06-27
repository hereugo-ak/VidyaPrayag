package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherSelfLeaveDto
import com.littlebridge.enrollplus.feature.teacher.presentation.ActionResult
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfile
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfileActionsViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VThemePicker
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * PROFILE tab — the teacher's account home, rebuilt from scratch in the
 * Parents-Portal vocabulary. Five movements:
 *   1. Identity card (avatar, name, username, school, subjects, classes).
 *   2. My leave — apply (date range + reason) and a live status list.
 *   3. Change password.
 *   4. Theme switch (Warm / Light / Night) — REAL, writes the global pref the
 *      portal reads to drive its tone (the shell re-themes live).
 *   5. Logout.
 *
 * Identity comes from the read-only [TeacherProfileViewModel]; the actionable
 * parts (leave / password / theme) from [TeacherProfileActionsViewModel].
 */
@Composable
fun TeacherProfileScreenV2(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: TeacherProfileViewModel = koinViewModel(),
    actionsViewModel: TeacherProfileActionsViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val profileState by profileViewModel.state.collectAsStateV2()
    val leave by actionsViewModel.leave.collectAsStateV2()
    val applyResult by actionsViewModel.apply.collectAsStateV2()
    val passwordResult by actionsViewModel.password.collectAsStateV2()
    val themeName by actionsViewModel.themeName.collectAsStateV2()
    val themeMode by actionsViewModel.themeMode.collectAsStateV2()
    val customThemeId by actionsViewModel.customThemeId.collectAsStateV2()

    var showLeaveComposer by remember { mutableStateOf(false) }
    var showPasswordForm by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Profile", style = VTheme.type.h1.colored(c.navyDeep))
            Spacer(Modifier.height(4.dp))
        }

        // 1 — Identity
        item {
            when {
                profileState.isLoading && profileState.profile == null -> TCard { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
                profileState.profile != null -> IdentityCard(profileState.profile!!)
                else -> TCard {
                    Column {
                        Text("Couldn't load profile", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                        profileState.error?.let { Spacer(Modifier.height(4.dp)); Text(it, style = VTheme.type.caption.colored(c.ink3)) }
                        Spacer(Modifier.height(12.dp))
                        VButton("Try again", onClick = { profileViewModel.load() }, size = VButtonSize.Sm, tone = VButtonTone.Lavender)
                    }
                }
            }
        }

        // 2 — Leave
        item {
            LeaveCard(
                leave = leave,
                applyResult = applyResult,
                showComposer = showLeaveComposer,
                onToggleComposer = {
                    showLeaveComposer = !showLeaveComposer
                    if (!showLeaveComposer) actionsViewModel.clearApplyResult()
                },
                onApply = { from, to, reason -> actionsViewModel.applyLeave(from, to, reason) },
                onApplied = {
                    showLeaveComposer = false
                    actionsViewModel.clearApplyResult()
                },
                onRetry = { actionsViewModel.loadLeave() },
            )
        }

        // 3 — Password
        item {
            PasswordCard(
                result = passwordResult,
                expanded = showPasswordForm,
                onToggle = {
                    showPasswordForm = !showPasswordForm
                    if (!showPasswordForm) actionsViewModel.clearPasswordResult()
                },
                onSubmit = { old, new, confirm -> actionsViewModel.changePassword(old, new, confirm) },
                onDone = {
                    showPasswordForm = false
                    actionsViewModel.clearPasswordResult()
                },
            )
        }

        // 4 — Theme
        item {
            TCard {
                VThemePicker(
                    currentMode = themeMode,
                    currentCustomId = customThemeId,
                    onSelect = { mode, customId ->
                        actionsViewModel.setThemeMode(mode)
                        actionsViewModel.setCustomThemeId(customId)
                    },
                )
            }
        }

        // 5 — Logout
        item {
            VButton(
                text = "Log out",
                onClick = { confirmLogout = true },
                variant = VButtonVariant.Destructive,
                full = true,
                leading = { Icon(VIcons.ArrowLeft, contentDescription = null, modifier = Modifier.size(15.dp)) },
            )
            Spacer(Modifier.height(8.dp))
            profileState.profile?.let {
                Text(
                    "Signed in as ${it.username}",
                    style = VTheme.type.caption.colored(c.ink3),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmLogout) {
        TeacherConfirmDialog(
            title = "Log out?",
            body = "You'll need to sign in again to access your classes.",
            confirmLabel = "Log out",
            destructive = true,
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1 — IDENTITY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdentityCard(p: TeacherProfile) {
    val c = VTheme.colors
    TCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(c.accentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(p.name.take(1).uppercase(), style = VTheme.type.h2.colored(c.accentDeep))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = VTheme.type.h3.colored(c.navyDeep))
                    Spacer(Modifier.height(2.dp))
                    Text("@${p.username}", style = VTheme.type.body.colored(c.ink2))
                    if (p.schoolName.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(p.schoolName, style = VTheme.type.caption.colored(c.ink3))
                    }
                }
            }
            if (p.email.isNotBlank() || p.phone.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.email.isNotBlank()) ContactLine(VIcons.Mail, p.email)
                    if (p.phone.isNotBlank()) ContactLine(VIcons.Phone, p.phone)
                }
            }
            if (p.subjects.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TEyebrow("SUBJECTS")
                Spacer(Modifier.height(6.dp))
                ChipFlow(p.subjects) { s -> teacherSubjectColor(c, s) }
            }
            if (p.classes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TEyebrow("CLASSES")
                Spacer(Modifier.height(6.dp))
                ChipFlow(p.classes) { c.navy }
            }
        }
    }
}

@Composable
private fun ContactLine(icon: ImageVector, text: String) {
    val c = VTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
        Text(text, style = VTheme.type.body.colored(c.ink2))
    }
}

/** A simple wrapping chip row (no FlowRow dependency): renders chips in rows of up to 3. */
@Composable
private fun ChipFlow(items: List<String>, tint: (String) -> Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { label ->
                    val t = tint(label)
                    TPill(label, t.copy(alpha = 0.12f), t)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2 — LEAVE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LeaveCard(
    leave: com.littlebridge.enrollplus.feature.teacher.presentation.TeacherLeaveUiState,
    applyResult: ActionResult,
    showComposer: Boolean,
    onToggleComposer: () -> Unit,
    onApply: (from: String, to: String, reason: String) -> Unit,
    onApplied: () -> Unit,
    onRetry: () -> Unit,
) {
    val c = VTheme.colors
    // Auto-collapse the composer on success.
    LaunchedEffect(applyResult) {
        if (applyResult is ActionResult.Success) onApplied()
    }
    TCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("My leave", style = VTheme.type.h3.colored(c.navyDeep))
                    if (leave.pendingCount > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text("${leave.pendingCount} pending", style = VTheme.type.caption.colored(c.warningInk))
                    }
                }
                VButton(
                    text = if (showComposer) "Close" else "Apply",
                    onClick = onToggleComposer,
                    size = VButtonSize.Sm,
                    variant = if (showComposer) VButtonVariant.Ghost else VButtonVariant.Secondary,
                    tone = VButtonTone.Lavender,
                    leading = if (showComposer) null else {
                        { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    },
                )
            }

            AnimatedVisibility(visible = showComposer) {
                LeaveComposer(applyResult = applyResult, onApply = onApply)
            }

            Spacer(Modifier.height(12.dp))
            when {
                leave.isLoading && leave.requests.isEmpty() -> Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) { TeacherSpinner(24.dp) }
                leave.error != null && leave.requests.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(leave.error ?: "", style = VTheme.type.caption.colored(c.ink3), modifier = Modifier.weight(1f))
                    VButton("Retry", onClick = onRetry, size = VButtonSize.Sm, variant = VButtonVariant.Ghost)
                }
                leave.requests.isEmpty() -> Text("No leave requests yet.", style = VTheme.type.body.colored(c.ink3))
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leave.requests.forEach { LeaveRow(it) }
                }
            }
        }
    }
}

@Composable
private fun LeaveComposer(
    applyResult: ActionResult,
    onApply: (from: String, to: String, reason: String) -> Unit,
) {
    val c = VTheme.colors
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val inFlight = applyResult is ActionResult.InFlight

    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VDatePicker(value = from, onValueChange = { from = it }, label = "From", enabled = !inFlight)
        VDatePicker(value = to, onValueChange = { to = it }, label = "To", enabled = !inFlight)
        VInput(
            value = reason,
            onValueChange = { reason = it },
            label = "Reason",
            placeholder = "Why are you applying?",
            singleLine = false,
            enabled = !inFlight,
        )
        if (applyResult is ActionResult.Failure) {
            Text(applyResult.message, style = VTheme.type.caption.colored(c.dangerInk))
        }
        VButton(
            text = "Submit request",
            onClick = { onApply(from, to, reason) },
            full = true,
            tone = VButtonTone.Lavender,
            stateful = true,
            loading = inFlight,
            enabled = from.isNotBlank() && to.isNotBlank() && reason.isNotBlank() && !inFlight,
        )
    }
}

@Composable
private fun LeaveRow(leave: TeacherSelfLeaveDto) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.cream).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (leave.dateFrom == leave.dateTo) prettyDateShort(leave.dateFrom)
                else "${prettyDateShort(leave.dateFrom)} – ${prettyDateShort(leave.dateTo)}",
                style = VTheme.type.bodyStrong.colored(c.navyDeep),
            )
            if (leave.reason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(leave.reason, style = VTheme.type.caption.colored(c.ink3))
            }
        }
        LeaveStatusPill(leave.status)
    }
}

@Composable
private fun LeaveStatusPill(status: String) {
    val c = VTheme.colors
    val (bg, fg) = when (status.lowercase()) {
        "approved" -> c.success.copy(alpha = 0.16f) to c.successInk
        "rejected" -> c.danger.copy(alpha = 0.12f) to c.dangerInk
        else -> c.warning.copy(alpha = 0.16f) to c.warningInk
    }
    TPill(status.uppercase(), bg, fg)
}

// ─────────────────────────────────────────────────────────────────────────────
// 3 — PASSWORD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PasswordCard(
    result: ActionResult,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSubmit: (old: String, new: String, confirm: String) -> Unit,
    onDone: () -> Unit,
) {
    val c = VTheme.colors
    LaunchedEffect(result) {
        if (result is ActionResult.Success) onDone()
    }
    TCard {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TIconDisc(VIcons.Lock, c.navy, c.navy.copy(alpha = 0.10f))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Password", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                    Text("Change your sign-in password", style = VTheme.type.caption.colored(c.ink3))
                }
                Icon(
                    if (expanded) VIcons.ChevronUp else VIcons.ChevronRight,
                    contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp),
                )
            }
            AnimatedVisibility(visible = expanded) {
                PasswordForm(result = result, onSubmit = onSubmit)
            }
        }
    }
}

@Composable
private fun PasswordForm(
    result: ActionResult,
    onSubmit: (old: String, new: String, confirm: String) -> Unit,
) {
    val c = VTheme.colors
    var old by remember { mutableStateOf("") }
    var new0 by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val inFlight = result is ActionResult.InFlight

    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VInput(
            value = old, onValueChange = { old = it }, label = "Current password",
            isPassword = true, passwordVisible = reveal, enabled = !inFlight,
            keyboardType = KeyboardType.Password,
        )
        VInput(
            value = new0, onValueChange = { new0 = it }, label = "New password",
            hint = "At least 8 characters",
            isPassword = true, passwordVisible = reveal, enabled = !inFlight,
            keyboardType = KeyboardType.Password,
        )
        VInput(
            value = confirm, onValueChange = { confirm = it }, label = "Confirm new password",
            isPassword = true, passwordVisible = reveal, enabled = !inFlight,
            keyboardType = KeyboardType.Password,
            trailing = {
                val ix = remember { MutableInteractionSource() }
                Icon(
                    VIcons.Eye,
                    contentDescription = "Toggle visibility",
                    tint = c.ink3,
                    modifier = Modifier.size(18.dp).clickable(interactionSource = ix, indication = null) { reveal = !reveal },
                )
            },
        )
        if (result is ActionResult.Failure) {
            Text(result.message, style = VTheme.type.caption.colored(c.dangerInk))
        }
        VButton(
            text = "Update password",
            onClick = { onSubmit(old, new0, confirm) },
            full = true,
            tone = VButtonTone.Navy,
            stateful = true,
            loading = inFlight,
            enabled = new0.isNotBlank() && confirm.isNotBlank() && !inFlight,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4 — THEME
// ─────────────────────────────────────────────────────────────────────────────

private data class ThemeOption(val key: String, val label: String, val caption: String, val icon: ImageVector)

@Composable
private fun ThemeCard(current: String, onSelect: (String) -> Unit) {
    val c = VTheme.colors
    val options = listOf(
        ThemeOption("WARM", "Warm", "Cream & lavender", VIcons.Sparkles),
        ThemeOption("LIGHT", "Light", "Crisp & bright", VIcons.Star),
        ThemeOption("NIGHT", "Night", "Easy on the eyes", VIcons.Bookmark),
    )
    TCard {
        Column {
            Text("Appearance", style = VTheme.type.h3.colored(c.navyDeep))
            Spacer(Modifier.height(10.dp))
            options.forEachIndexed { i, opt ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                val active = current.equals(opt.key, ignoreCase = true)
                val ix = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) c.accentTint else c.cream)
                        .border(1.dp, if (active) c.accent.copy(alpha = 0.35f) else c.hairline, RoundedCornerShape(14.dp))
                        .clickable(interactionSource = ix, indication = null) { onSelect(opt.key) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TIconDisc(opt.icon, if (active) c.accentDeep else c.ink2, if (active) c.accent.copy(alpha = 0.15f) else c.card)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(opt.label, style = VTheme.type.bodyStrong.colored(if (active) c.accentDeep else c.navyDeep))
                        Text(opt.caption, style = VTheme.type.caption.colored(c.ink3))
                    }
                    if (active) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(c.accentDeep), contentAlignment = Alignment.Center) {
                            Icon(VIcons.Check, contentDescription = "Selected", tint = c.card, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
