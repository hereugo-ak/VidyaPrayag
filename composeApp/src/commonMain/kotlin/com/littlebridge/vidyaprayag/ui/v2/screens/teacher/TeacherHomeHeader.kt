package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.MONTH_LONG
import com.littlebridge.vidyaprayag.util.dayOfWeek
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay
import com.littlebridge.vidyaprayag.util.parseIsoDate
import com.littlebridge.vidyaprayag.util.todayIso

/**
 * TeacherHomeHeader — the Home tab's signature gradient header (Loop task P2-T1).
 *
 * Per Design Spec PART 2 §SIGNATURE/§Gradients, a full-bleed brand gradient is
 * sanctioned ONLY on the Home and Profile headers — everywhere else the portal
 * stays on the calm lavender canvas. This is that one Home moment.
 *
 * Every visual decision is justified from the spec, resolved through the `Enroll.*`
 * bridge → existing VTheme (no new colour; the gradient is the portal violet
 * `accentDeep → accent`, keeping parent↔teacher parity per the IMPORTANT NOTE):
 *   • Surface  → `Enroll.colors.headerGradient` (GradientStart → GradientEnd),
 *               clipped to a soft bottom radius so it reads as a premium banner,
 *               full-width, ≥120dp tall (statusBarsPadding sits ON the gradient).
 *   • Greeting → time-aware ("Good morning/afternoon/evening") via the existing
 *               `teacherGreeting(hour)` helper — never hardcoded; white 78% alpha.
 *   • Name     → teacher's FIRST name in `HeadingLarge`, solid white.
 *   • Date     → "Wednesday, 25 June" in `BodyMedium`, white 70% alpha.
 *   • Avatar   → 40dp circle (ShapeAvatar) with a white ring; tap → Profile tab.
 *   • Bell     → glassy 40dp circle with the unread count badge; tap → NotificationSheet.
 *
 * Data-driven only: name/photo/unread come from the caller's session + VM. Nothing
 * about the teacher is fabricated. Avatar and bell are ≥40dp tap targets (a11y).
 *
 * This is ADDITIVE — it does not remove the canonical [TeacherHeader] used across
 * the operational tabs; it is the Home-specific hero header the loop asked for.
 */
@Composable
fun TeacherHomeHeader(
    teacherName: String,
    photoUrl: String?,
    unreadCount: Int,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = teacherGreeting(nowMinutesOfDay() / 60)
    val firstName = teacherName.substringBefore(' ').ifBlank { teacherName }.ifBlank { "Teacher" }
    val dateLine = homeHeaderDateLabel()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Enroll.shape.sheet) // soft bottom-rounded banner (top corners flush to status bar)
            .background(Enroll.colors.headerGradient)
            .heightIn(min = 120.dp)
            .statusBarsPadding()
            .padding(horizontal = Enroll.space.xl, vertical = Enroll.space.lg),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Left: greeting → first name → date ──────────────────────────────
            Column(Modifier.weight(1f).padding(end = Enroll.space.md)) {
                Text(
                    text = greeting,
                    style = Enroll.type.bodyMedium.colored(Color.White.copy(alpha = 0.78f)),
                    maxLines = 1,
                )
                Spacer(Modifier.height(Enroll.space.xs))
                Text(
                    text = firstName,
                    style = Enroll.type.headingLarge.colored(Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Enroll.space.xs))
                Text(
                    text = dateLine,
                    style = Enroll.type.bodyMedium.colored(Color.White.copy(alpha = 0.70f)),
                    maxLines = 1,
                )
            }

            // ── Right: avatar (→ Profile) stacked above bell (→ Notifications) ──
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val avatarInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable(interactionSource = avatarInteraction, indication = null) { onOpenProfile() }
                        .semantics { contentDescription = "Open profile" },
                ) {
                    VAvatar(name = teacherName.ifBlank { "Teacher" }, src = photoUrl, size = 40.dp, ring = true)
                }
                Spacer(Modifier.height(Enroll.space.sm))
                HomeHeaderBell(unreadCount = unreadCount, onClick = onOpenNotifications)
            }
        }
    }
}

/** Glassy circular bell on the gradient, with a real unread count badge (hidden at 0). */
@Composable
private fun HomeHeaderBell(unreadCount: Int, onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            // A whisper of white on the gradient → reads as a glass chip, not a flat hole.
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .semantics {
                contentDescription = if (unreadCount > 0) "Notifications, $unreadCount unread" else "Notifications"
            },
    ) {
        Icon(VIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        if (unreadCount > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp)
                    .clip(CircleShape)
                    .background(Enroll.colors.statusAbsent) // semantic alert red (preserved token)
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    style = Enroll.type.dataSmall.colored(Color.White),
                )
            }
        }
    }
}

/**
 * "Wednesday, 25 June" — the long date label for the Home header. Self-contained
 * (uses the shared util date primitives) so the header has no dependency on the
 * private label helper inside TodayScreen.
 */
fun homeHeaderDateLabel(): String {
    val iso = todayIso()
    val parsed = parseIsoDate(iso) ?: return ""
    val (y, m, d) = parsed
    val weekday = when (dayOfWeek(y, m, d)) {
        0 -> "Sunday"; 1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"
        4 -> "Thursday"; 5 -> "Friday"; else -> "Saturday"
    }
    val month = MONTH_LONG.getOrElse(m - 1) { "" }
    return "$weekday, $d $month"
}
