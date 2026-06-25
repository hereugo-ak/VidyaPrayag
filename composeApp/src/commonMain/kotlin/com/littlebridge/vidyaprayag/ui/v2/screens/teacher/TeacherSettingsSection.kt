package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherSettingsSection — the Profile tab's "PREFERENCES" block (Loop task P6-T4).
 *
 * A `SectionHeader("PREFERENCES")` over one [EnrollCard] of [SettingsRow]s. The spec's
 * six rows, in order:
 *   • Notification Preferences → toggle ([VToggle])
 *   • Smart Nudges            → toggle (this is the permission gate for the P2-T5 /
 *                               P4-T4 planner nudges; off = no nudges surfaced)
 *   • Dark Mode               → toggle
 *   • Language                → trailing current value + chevron (opens host picker)
 *   • Help & Support          → chevron (stub navigation)
 *   • Log Out                 → `StatusAbsent`-tinted destructive action that raises a
 *                               confirmation dialog before calling `onLogOut`
 *
 * State is HOISTED — every toggle value and callback comes from the host
 * (`TeacherProfileViewModel` / app settings DataStore), so this composable is pure and
 * the single source of truth stays outside. The only local state is the logout
 * confirmation dialog's visibility (pure UI, no business meaning).
 *
 * All colour/type via `Enroll.*` → VTheme (no new hex). The custom [VToggle] is used
 * instead of Material3 `Switch` to keep the portal's bespoke look (quality bar:
 * custom components over Material defaults) and to bind cleanly to the brand accent.
 */
@Composable
fun TeacherSettingsSection(
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    smartNudgesEnabled: Boolean,
    onSmartNudgesChanged: (Boolean) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    languageLabel: String,
    onOpenLanguage: () -> Unit,
    onOpenHelp: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "PREFERENCES")
        Spacer(Modifier.height(Enroll.space.sm))
        EnrollCard(modifier = Modifier.fillMaxWidth(), padding = Enroll.space.xs) {
            SettingsRow(
                icon = VIcons.Bell,
                label = "Notification Preferences",
                trailing = { VToggle(checked = notificationsEnabled, onCheckedChange = onNotificationsChanged) },
            )
            RowDivider()
            SettingsRow(
                icon = VIcons.Sparkles,
                label = "Smart Nudges",
                trailing = { VToggle(checked = smartNudgesEnabled, onCheckedChange = onSmartNudgesChanged) },
            )
            RowDivider()
            SettingsRow(
                icon = VIcons.Eye,
                label = "Dark Mode",
                trailing = { VToggle(checked = darkModeEnabled, onCheckedChange = onDarkModeChanged) },
            )
            RowDivider()
            SettingsRow(
                icon = VIcons.GraduationCap,
                label = "Language",
                onClick = onOpenLanguage,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = languageLabel,
                            style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(Enroll.space.xs))
                        Icon(VIcons.ChevronRight, contentDescription = null, tint = Enroll.colors.textTertiary)
                    }
                },
            )
            RowDivider()
            SettingsRow(
                icon = VIcons.ShieldCheck,
                label = "Help & Support",
                onClick = onOpenHelp,
                trailing = { Icon(VIcons.ChevronRight, contentDescription = null, tint = Enroll.colors.textTertiary) },
            )
            RowDivider()
            SettingsRow(
                icon = VIcons.Close,
                label = "Log Out",
                tint = Enroll.colors.statusAbsent,
                onClick = { showLogoutConfirm = true },
            )
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                onLogOut()
            },
        )
    }
}

/**
 * A single settings line: leading icon | label | trailing widget. The whole row is
 * tappable only when an `onClick` is supplied (the toggle rows handle their own taps
 * through [VToggle], so they pass no row `onClick`).
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val labelColor = tint ?: Enroll.colors.textPrimary
    val iconColor = tint ?: Enroll.colors.textSecondary
    val base = modifier
        .fillMaxWidth()
        .clip(Enroll.shape.chip)
    val rowMod = if (onClick != null) {
        base.clickable(interactionSource = interaction, indication = null) { onClick() }
            .semantics { contentDescription = label }
    } else {
        base
    }

    Row(
        modifier = rowMod.padding(horizontal = Enroll.space.md, vertical = Enroll.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Enroll.space.md))
        Text(
            text = label,
            style = Enroll.type.labelBold.colored(labelColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(Enroll.space.sm))
            trailing()
        }
    }
}

/** Hairline between settings rows. */
@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = Enroll.space.md)
            .background(Enroll.colors.hairline),
    )
}

/**
 * VToggle — the portal's bespoke switch. A 44×26 track that animates its fill colour
 * (surfaceSubtle → brand `primary`) and slides a 20dp white thumb on toggle. Built from
 * primitives (no Material3 `Switch`) so it inherits the brand accent and the portal's
 * motion feel. State is hoisted; tapping anywhere on the track flips it.
 */
@Composable
fun VToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val trackColor by animateColorAsState(
        targetValue = if (checked) Enroll.colors.primary else Enroll.colors.surfaceSubtle,
        label = "toggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        label = "toggleThumb",
    )

    Box(
        modifier = modifier
            .width(44.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(interactionSource = interaction, indication = null) { onCheckedChange(!checked) }
            .semantics { contentDescription = if (checked) "On" else "Off" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Confirmation dialog raised before logging the teacher out. */
@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Enroll.space.xl)
                .clip(Enroll.shape.card)
                .background(Enroll.colors.surfaceCard)
                .padding(Enroll.space.xl),
        ) {
            Text(
                text = "Log out?",
                style = Enroll.type.headingMedium.colored(Enroll.colors.textPrimary),
            )
            Spacer(Modifier.height(Enroll.space.sm))
            Text(
                text = "You'll need to sign in again to access your classes, gradebook and parent chats.",
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
            )
            Spacer(Modifier.height(Enroll.space.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Enroll.space.md),
            ) {
                VButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = VButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                VButton(
                    text = "Log Out",
                    onClick = onConfirm,
                    variant = VButtonVariant.Destructive,
                    tone = VButtonTone.Rose,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
