package com.littlebridge.enrollplus.ui.v2.screens.teacher

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.ui.v2.components.VAvatar
import com.littlebridge.enrollplus.ui.v2.components.VDivider
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VStatusDot
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * TeacherHeader — THE single canonical header for the whole rebuilt Teacher Portal, rendered
 * identically on every tab (Home included), mirroring the Parents Portal's one-header law.
 *
 * Left  — a tappable identity chip: the teacher's avatar + name + a contextual subline (the
 *         time-sensitive greeting on Home, the school name elsewhere). Tapping it opens the
 *         Profile tab/overlay (account & settings live there, never "tap photo = logout").
 * Right — an icon cluster: a notifications bell (with a real unread dot) and the account avatar.
 *
 * Surface: a clean white bar on the lavender canvas with a hairline divider — lavender/violet is
 * the brand accent (active dot), never a wall-to-wall fill.
 */
@Composable
fun TeacherHeader(
    teacherName: String,
    subline: String,
    photoUrl: String?,
    unreadCount: Int,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenNotifications: (() -> Unit)? = null,
) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.card)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Identity chip → opens Profile ─────────────────────────────────────
            val chipIx = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    .clickable(interactionSource = chipIx, indication = null) { onOpenProfile() }
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VAvatar(name = teacherName.ifBlank { "Teacher" }, src = photoUrl, size = 38.dp, ring = true)
                Column {
                    Text(
                        subline,
                        style = VTheme.type.label.colored(c.ink3)
                            .copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                    )
                    Text(
                        teacherName.ifBlank { "Teacher" },
                        style = VTheme.type.bodyStrong.colored(c.ink)
                            .copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                    )
                }
            }

            // ── Icon cluster: notifications · account ─────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onOpenNotifications != null) {
                    Box {
                        HeaderIconButton(VIcons.Bell, "Notifications", onOpenNotifications)
                        if (unreadCount > 0) {
                            VStatusDot(
                                color = c.dangerInk,
                                size = 7.dp,
                                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                            )
                        }
                    }
                }
                val accountIx = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable(interactionSource = accountIx, indication = null) { onOpenProfile() },
                ) {
                    VAvatar(name = teacherName.ifBlank { "Teacher" }, src = photoUrl, size = 36.dp, ring = true)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        VDivider()
    }
}

/** A simpler header for in-portal sub-screens (overlays): a back chevron + title. */
@Composable
fun TeacherSubHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.card)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeaderIconButton(VIcons.ArrowLeft, "Back", onBack)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp), maxLines = 1)
                }
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        VDivider()
    }
}

/** A circular header action button — cream surface, navy glyph, no ripple. */
@Composable
private fun HeaderIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(c.cream)
            .clickable(interactionSource = ix, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = c.ink, modifier = Modifier.size(16.dp))
    }
}
