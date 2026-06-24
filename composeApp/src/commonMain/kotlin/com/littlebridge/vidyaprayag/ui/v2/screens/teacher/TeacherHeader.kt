package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherHeader — THE single canonical header for the whole teacher portal (Doc 04 §6,
 * Doc 10 §5.2). It is the teacher equivalent of the parents portal's `ParentHeader`,
 * **minus the child switcher** (a teacher has no child to switch) and **plus an optional
 * class-context chip** that surfaces the currently-scoped class inside the operational tabs
 * (Classes / Gradebook / Planner) and lets the teacher change scope in one tap (Doc 04 §7 —
 * the anti-picker rule; this chip REPLACES the eliminated shared cross-tool picker).
 *
 * It renders identically on every tab so a parent and a teacher in the same household perceive
 * one product (Doc 10 §12). Carries:
 *   • a time-sensitive **greeting** ("Good morning, Aanya") sourced from the active session —
 *     never hardcoded;
 *   • an optional **class-context chip** (only inside the operational tabs, only when a scope
 *     is known) — tappable to change scope;
 *   • a right cluster: notification bell with a real unread dot + the account avatar.
 *
 * Surface: a clean `card` bar on the lavender canvas with a hairline divider — lavender/violet
 * is the brand accent (chip tint, active dot), never a wall-to-wall fill.
 */
@Composable
fun TeacherHeader(
    teacherName: String,
    greeting: String,
    schoolName: String,
    photoUrl: String?,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    // The class-context chip is shown ONLY when a scope label is supplied (operational tabs).
    // Tapping it opens the tab's own scope picker (Classes list / Gradebook class / Planner class).
    classContext: String? = null,
    onChangeScope: (() -> Unit)? = null,
) {
    val c = VTheme.colors
    val firstName = teacherName.substringBefore(' ').ifBlank { teacherName }.ifBlank { "Teacher" }

    Column(
        modifier
            .fillMaxWidth()
            .background(c.card)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Premium identity chip (avatar + greeting + name) — mirrors the parents
            //    portal header so a parent and a teacher perceive one product. The whole
            //    chip is the tap target into Profile.
            val idInteraction = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.cream)
                    .clickable(interactionSource = idInteraction, indication = null) { onOpenProfile() }
                    .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
                    .semantics { contentDescription = "Open profile" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VAvatar(name = teacherName.ifBlank { "Teacher" }, src = photoUrl, size = 40.dp, ring = true)
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting,
                        style = VTheme.type.caption.colored(c.accentDeep).copy(fontSize = 10.5.sp),
                        maxLines = 1,
                    )
                    Text(
                        firstName,
                        style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── Icon cluster: notifications only (account lives in the identity chip) ─
            Box {
                HeaderIconButton(VIcons.Bell, "Notifications", onOpenNotifications)
                // Real unread dot only — never a permanent cry-wolf indicator.
                if (unreadCount > 0) {
                    VStatusDot(color = c.dangerInk, size = 7.dp, modifier = Modifier.align(Alignment.TopEnd).padding(7.dp))
                }
            }
        }

        // ── Optional class-context chip (operational tabs only) — full-width row below
        //    the identity, so it never crowds the name. Tappable to change scope.
        if (classContext != null && classContext.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            val chipInteraction = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.accentTint)
                    .let { base ->
                        if (onChangeScope != null) {
                            base.clickable(interactionSource = chipInteraction, indication = null) { onChangeScope() }
                        } else base
                    }
                    .padding(start = 12.dp, end = if (onChangeScope != null) 10.dp else 14.dp, top = 6.dp, bottom = 6.dp)
                    .semantics {
                        contentDescription = "Current class: $classContext" +
                            if (onChangeScope != null) ". Tap to change class." else ""
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(VIcons.Users, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(14.dp))
                Text(
                    classContext,
                    style = VTheme.type.label.colored(c.accentDeep).copy(fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
                if (onChangeScope != null) {
                    Icon(VIcons.ChevronDown, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(15.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        VDivider()
    }
}

/** A circular header action button — cream surface, navy glyph, no ripple. ≥40dp for the 58yo. */
@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.cream)
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
    }
}

/**
 * Computes a time-sensitive greeting from the device clock. The hour boundaries match the
 * common Indian-school convention (Doc 04 §2.1 morning context). Kept here so the portal and
 * any standalone teacher surface produce the same wording.
 */
fun teacherGreeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Hello"
}
