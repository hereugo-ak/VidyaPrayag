package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.core.locale.StringKeys
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherSelfLeaveDto
import com.littlebridge.enrollplus.feature.teacher.presentation.ActionResult
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfile
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfileActionsViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.enrollplus.ui.tokens.VColors
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VPullRefresh
import com.littlebridge.enrollplus.ui.v2.components.VThemePicker
import com.littlebridge.enrollplus.ui.v2.components.VLanguagePicker
import com.littlebridge.enrollplus.core.locale.LocaleManager
import com.littlebridge.enrollplus.ui.v2.locale.appString
import com.littlebridge.enrollplus.util.AnalyticsTracker
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.littlebridge.enrollplus.ui.v2.screens.teacher.TeacherSpinner

/**
 * PROFILE tab — the teacher's account home, RESTRUCTURED into a premium layout.
 *
 * Instead of a flat stack of white cards, the tab is now organised into a hero +
 * three clearly-labelled setting GROUPS, so it reads like a modern system settings
 * screen:
 *
 *   ▸ IDENTITY HERO — a violet-gradient card: big avatar monogram, name, @username,
 *     school, plus a live stat strip (Subjects · Classes) pulled from the profile.
 *   ▸ "TIME OFF"     — the leave group (apply + live status list).
 *   ▸ "SECURITY"     — change-password group.
 *   ▸ "PREFERENCES"  — appearance (theme) + language, as two rows in one grouped card.
 *   ▸ FOOTER         — a full-width destructive Log out button + "signed in as".
 *
 * Each group is introduced by a [SectionLabel] eyebrow and wrapped in a rounded
 * grouped surface, matching the system look & feel. The whole tab is one
 * [LazyColumn] reserving [TeacherDockClearance] at the bottom so nothing hides
 * behind the floating dock.
 *
 * Identity comes from the read-only [TeacherProfileViewModel]; the actionable
 * parts (leave / password / theme) from [TeacherProfileActionsViewModel]. Both
 * view-models and the public signature are PRESERVED.
 */
@Composable
fun TeacherProfileScreenV2(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    teacherName: String = "",
    unreadCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenSalary: () -> Unit = {},
    profileViewModel: TeacherProfileViewModel = koinViewModel(),
    actionsViewModel: TeacherProfileActionsViewModel = koinViewModel(),
) {
    val c = VtC
    val profileState by profileViewModel.state.collectAsStateV2()
    val leave by actionsViewModel.leave.collectAsStateV2()
    val applyResult by actionsViewModel.apply.collectAsStateV2()
    val passwordResult by actionsViewModel.password.collectAsStateV2()
    val themeMode by actionsViewModel.themeMode.collectAsStateV2()
    val customThemeId by actionsViewModel.customThemeId.collectAsStateV2()
    val localeManager = koinInject<LocaleManager>()
    val currentLocale by localeManager.currentLocale.collectAsStateV2()

    // Fall back to the loaded profile name if the shell didn't pass one.
    val headerName = teacherName.ifBlank { profileState.profile?.name ?: "" }

    var showLeaveComposer by remember { mutableStateOf(false) }
    var showPasswordForm by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(profileState.isLoading) {
        if (!profileState.isLoading) isRefreshing = false
    }

    VPullRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; profileViewModel.load() },
        modifier = modifier.fillMaxSize().background(VColors.cream),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = TeacherDockClearance),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
        item {
            TeacherPremiumHeader(
                teacherName = headerName,
                lead = appString(StringKeys.TC_YOUR),
                accent = appString(StringKeys.TC_ACCOUNT_ACCENT),
                unreadCount = unreadCount,
                onOpenNotifications = onOpenNotifications,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // ── IDENTITY HERO
        item {
            when {
                profileState.isLoading && profileState.profile == null ->
                    TCard { Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
                profileState.profile != null -> {
                    val profile = profileState.profile ?: return@item
                    IdentityHero(profile)
                }
                else -> TCard {
                    Column {
                        Text(appString(StringKeys.TC_COULDNT_LOAD_PROFILE), style = VtT.bodyStrong.coloredV(c.navyDeep))
                        profileState.error?.let { Spacer(Modifier.height(4.dp)); Text(it, style = VtT.caption.coloredV(c.ink3)) }
                        Spacer(Modifier.height(12.dp))
                        VButton(appString(StringKeys.COMMON_BUTTON_TRY_AGAIN), onClick = { profileViewModel.load() }, size = VButtonSize.Sm, tone = VButtonTone.Lavender)
                    }
                }
            }
        }

        // ── DETAILS (subjects / classes / contact) — a secondary identity surface.
        profileState.profile?.let { p ->
            if (p.email.isNotBlank() || p.phone.isNotBlank() || p.subjects.isNotEmpty() || p.classes.isNotEmpty()) {
                item { IdentityDetailsCard(p) }
            }
        }

        // ── TIME OFF (leave)
        item {
            SettingsGroup(label = appString(StringKeys.TC_SEC_TIME_OFF), icon = VIcons.Calendar, tint = VColors.gold) {
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
        }

        // ── SECURITY (password)
        item {
            SettingsGroup(label = appString(StringKeys.TC_SEC_SECURITY), icon = VIcons.Lock, tint = VColors.violet) {
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
        }

        // ── SALARY & PAYMENTS
        item {
            SettingsGroup(label = "Salary & Payments", icon = VIcons.Wallet, tint = VColors.violet) {
                TCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSalary() },
                    ) {
                        Text("View Salary History", style = VtT.h3.coloredV(c.navyDeep))
                        Spacer(Modifier.height(4.dp))
                        Text("See your monthly salary breakdown and payment status", style = VtT.caption.coloredV(c.ink3))
                    }
                }
            }
        }

        // ── PREFERENCES (appearance + language) in one grouped card.
        item {
            SettingsGroup(label = appString(StringKeys.TC_SEC_PREFERENCES), icon = VIcons.Settings, tint = VColors.sky) {
                TCard {
                    Column {
                        Text(appString(StringKeys.TC_APPEARANCE), style = VtT.h3.coloredV(c.navyDeep))
                        Spacer(Modifier.height(10.dp))
                        VThemePicker(
                            currentMode = themeMode,
                            currentCustomId = customThemeId,
                            onSelect = { mode, customId ->
                                AnalyticsTracker.event("vp_teacher_theme_change", mapOf("theme" to mode))
                                actionsViewModel.setThemeMode(mode)
                                actionsViewModel.setCustomThemeId(customId)
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                        Spacer(Modifier.height(16.dp))
                        Text(appString(StringKeys.TC_LANGUAGE), style = VtT.h3.coloredV(c.navyDeep))
                        Spacer(Modifier.height(10.dp))
                        VLanguagePicker(
                            currentLang = currentLocale,
                            onSelect = { lang -> localeManager.setLocale(lang) },
                        )
                    }
                }
            }
        }

        // ── FOOTER — logout
        item {
            Column {
                VButton(
                    text = appString(StringKeys.TC_LOG_OUT),
                    onClick = { confirmLogout = true },
                    variant = VButtonVariant.Destructive,
                    full = true,
                    leading = { Icon(VIcons.ArrowLeft, contentDescription = null, modifier = Modifier.size(15.dp)) },
                )
                Spacer(Modifier.height(10.dp))
                profileState.profile?.let {
                    Text(
                        appString(StringKeys.TC_SIGNED_IN_AS, "username" to it.username),
                        style = VtT.caption.coloredV(c.ink3),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
    }

    if (confirmLogout) {
        TeacherConfirmSheet(
            title = appString(StringKeys.TC_LOG_OUT_Q),
            body = appString(StringKeys.TC_LOG_OUT_DESC),
            confirmLabel = appString(StringKeys.TC_LOG_OUT),
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
// GROUPING PRIMITIVES — a labelled section, matching the system settings look.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * SettingsGroup — a labelled section: a small tinted icon + all-caps eyebrow, then
 * the group's content. Used to organise the profile into Time off / Security /
 * Preferences, so the tab reads like a modern settings screen.
 */
@Composable
private fun SettingsGroup(
    label: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable () -> Unit,
) {
    val c = VtC
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            }
            Text(
                label.uppercase(),
                style = VtT.label.coloredV(c.ink3).copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1 — IDENTITY HERO — a premium violet-gradient card with a live stat strip.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdentityHero(p: TeacherProfile) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(VColors.violet, VColors.violetHover),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Big avatar monogram on a frosted disc.
                Box(
                    Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(VColors.white.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        p.name.take(1).uppercase().ifBlank { "?" },
                        style = VtT.h2.coloredV(VColors.white).copy(fontWeight = FontWeight.ExtraBold),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        p.name.ifBlank { "—" },
                        style = VtT.h3.coloredV(VColors.white).copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(if (p.username.contains("@")) p.username else "@${p.username}", style = VtT.body.coloredV(VColors.white.copy(alpha = 0.82f)))
                    if (p.schoolName.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(VIcons.School, contentDescription = null, tint = VColors.white.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                            Text(p.schoolName, style = VtT.caption.coloredV(VColors.white.copy(alpha = 0.82f)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Live stat strip — subjects / classes taught.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VColors.white.copy(alpha = 0.12f))
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroStat(p.subjects.size.toString(), appString(StringKeys.TC_STAT_SUBJECTS), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(28.dp).background(VColors.white.copy(alpha = 0.22f)))
                HeroStat(p.classes.size.toString(), appString(StringKeys.TC_STAT_CLASSES), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = VtT.h2.coloredV(VColors.white).copy(fontWeight = FontWeight.ExtraBold))
        Spacer(Modifier.height(1.dp))
        Text(
            label.uppercase(),
            style = VtT.label.coloredV(VColors.white.copy(alpha = 0.75f)).copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
        )
    }
}

/** IdentityDetailsCard — contact lines + subject/class chips, split out of the hero. */
@Composable
private fun IdentityDetailsCard(p: TeacherProfile) {
    val c = VtC
    TCard {
        Column {
            if (p.email.isNotBlank() || p.phone.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (p.email.isNotBlank()) ContactLine(VIcons.Mail, p.email)
                    if (p.phone.isNotBlank()) ContactLine(VIcons.Phone, p.phone)
                }
            }
            if (p.subjects.isNotEmpty()) {
                if (p.email.isNotBlank() || p.phone.isNotBlank()) Spacer(Modifier.height(14.dp))
                TEyebrow(appString(StringKeys.TC_SUBJECTS))
                Spacer(Modifier.height(8.dp))
                ChipFlow(p.subjects) { s -> vtSubjectColor(s) }
            }
            if (p.classes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                TEyebrow(appString(StringKeys.TC_CLASSES))
                Spacer(Modifier.height(8.dp))
                ChipFlow(p.classes) { c.accent }
            }
        }
    }
}

@Composable
private fun ContactLine(icon: ImageVector, text: String) {
    val c = VtC
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
        Text(text, style = VtT.body.coloredV(c.ink2))
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
    val c = VtC
    // Auto-collapse the composer on success.
    LaunchedEffect(applyResult) {
        if (applyResult is ActionResult.Success) onApplied()
    }
    TCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(appString(StringKeys.TC_MY_LEAVE), style = VtT.h3.coloredV(c.navyDeep))
                    if (leave.pendingCount > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(appString(StringKeys.TC_PENDING_COUNT, "count" to leave.pendingCount.toString()), style = VtT.caption.coloredV(c.warningInk))
                    }
                }
                VButton(
                    text = if (showComposer) appString(StringKeys.COMMON_BUTTON_CLOSE) else appString(StringKeys.TC_APPLY),
                    onClick = onToggleComposer,
                    size = VButtonSize.Sm,
                    variant = if (showComposer) VButtonVariant.Ghost else VButtonVariant.Secondary,
                    tone = VButtonTone.Lavender,
                    leading = if (showComposer) null else {
                        { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    },
                )
            }

            if (showComposer) {
                LeaveComposer(applyResult = applyResult, onApply = onApply)
            }

            Spacer(Modifier.height(12.dp))
            when {
                leave.isLoading && leave.requests.isEmpty() -> Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) { TeacherSpinner(24.dp) }
                leave.error != null && leave.requests.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(leave.error ?: "", style = VtT.caption.coloredV(c.ink3), modifier = Modifier.weight(1f))
                    VButton(appString(StringKeys.COMMON_BUTTON_RETRY), onClick = onRetry, size = VButtonSize.Sm, variant = VButtonVariant.Ghost)
                }
                leave.requests.isEmpty() -> Text(appString(StringKeys.TC_NO_LEAVE_REQUESTS), style = VtT.body.coloredV(c.ink3))
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
    val c = VtC
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val inFlight = applyResult is ActionResult.InFlight

    Column(Modifier.fillMaxWidth().wrapContentHeight().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VDatePicker(value = from, onValueChange = { from = it }, label = appString(StringKeys.TC_FROM), enabled = !inFlight)
        VDatePicker(value = to, onValueChange = { to = it }, label = appString(StringKeys.TC_TO), enabled = !inFlight)
        VInput(
            value = reason,
            onValueChange = { reason = it },
            label = appString(StringKeys.TC_REASON),
            placeholder = appString(StringKeys.TC_WHY_APPLYING),
            singleLine = false,
            enabled = !inFlight,
        )
        if (applyResult is ActionResult.Failure) {
            Text(applyResult.message, style = VtT.caption.coloredV(c.dangerInk))
        }
        VButton(
            text = appString(StringKeys.TC_SUBMIT_REQUEST),
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
    val c = VtC
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.cream).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (leave.dateFrom == leave.dateTo) prettyDateShort(leave.dateFrom)
                else "${prettyDateShort(leave.dateFrom)} – ${prettyDateShort(leave.dateTo)}",
                style = VtT.bodyStrong.coloredV(c.navyDeep),
            )
            if (leave.reason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(leave.reason, style = VtT.caption.coloredV(c.ink3))
            }
        }
        LeaveStatusPill(leave.status)
    }
}

@Composable
private fun LeaveStatusPill(status: String) {
    val c = VtC
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
    val c = VtC
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
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    Text(appString(StringKeys.TC_PASSWORD), style = VtT.bodyStrong.coloredV(c.navyDeep), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(appString(StringKeys.TC_CHANGE_PASSWORD), style = VtT.caption.coloredV(c.ink3), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Icon(
                    if (expanded) VIcons.ChevronUp else VIcons.ChevronRight,
                    contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp),
                )
            }
            if (expanded) {
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
    val c = VtC
    var old by remember { mutableStateOf("") }
    var new0 by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val inFlight = result is ActionResult.InFlight

    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VInput(
            value = old, onValueChange = { old = it }, label = appString(StringKeys.TC_CURRENT_PASSWORD),
            isPassword = true, passwordVisible = reveal, enabled = !inFlight,
            keyboardType = KeyboardType.Password,
        )
        VInput(
            value = new0, onValueChange = { new0 = it }, label = appString(StringKeys.TC_NEW_PASSWORD),
            hint = appString(StringKeys.TC_AT_LEAST_),
            isPassword = true, passwordVisible = reveal, enabled = !inFlight,
            keyboardType = KeyboardType.Password,
        )
        VInput(
            value = confirm, onValueChange = { confirm = it }, label = appString(StringKeys.TC_CONFIRM_NEW_PASSWORD),
            isPassword = true, passwordVisible = reveal, enabled = !inFlight,
            keyboardType = KeyboardType.Password,
            trailing = {
                val ix = remember { MutableInteractionSource() }
                Icon(
                    VIcons.Eye,
                    contentDescription = appString(StringKeys.TC_TOGGLE_VISIBILITY),
                    tint = c.ink3,
                    modifier = Modifier.size(18.dp).clickable(interactionSource = ix, indication = null) { reveal = !reveal },
                )
            },
        )
        if (result is ActionResult.Failure) {
            Text(result.message, style = VtT.caption.coloredV(c.dangerInk))
        }
        VButton(
            text = appString(StringKeys.TC_UPDATE_PASSWORD),
            onClick = { onSubmit(old, new0, confirm) },
            full = true,
            tone = VButtonTone.Navy,
            stateful = true,
            loading = inFlight,
            enabled = new0.isNotBlank() && confirm.isNotBlank() && !inFlight,
        )
    }
}
