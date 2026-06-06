package com.littlebridge.vidyaprayag.ui.v2.screens.notifications

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * NotificationsScreenV2 — faithful Compose translation of `Notifications.tsx → NotificationsScreen`.
 *
 * Reproduces the React layout exactly: navy→indigo gradient "Inbox" hero with the unread count,
 * `all / unread` filter pills, the per-item card (category badge + time, title, body, unread dot,
 * chevron, category-tinted icon tile), the "You're all caught up" empty state, and the
 * "Notification preferences" footer button.
 *
 * **Backend gap (documented in PHASE_PLAN):** there is *no* notifications API or Supabase table in the
 * shared layer — the React screen was driven entirely by `lib/mock`. Per the hard UI rule we never
 * fabricate inbox data, so [items] defaults to **empty**: the screen renders the genuine
 * "all caught up" state until a `GET /api/v1/notifications` feed exists. When it lands, pass a real
 * list (or bind a NotificationsViewModel) into [items] and the entire layout lights up unchanged.
 */
@Composable
fun NotificationsScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    items: List<VNotification> = emptyList(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    var filterUnread by remember { mutableStateOf(false) }
    // Local read-state overlay so tapping an item / "Mark all" feels live even before a backend.
    var readIds by remember { mutableStateOf(setOf<String>()) }

    val effective = items.map { it.copy(unread = it.unread && !readIds.contains(it.id)) }
    val unread = effective.count { it.unread }
    val visible = if (filterUnread) effective.filter { it.unread } else effective

    Column(modifier.fillMaxSize().background(c.background)) {
        VBackHeader(
            title = "Notifications",
            onBack = onBack,
            action = {
                Box(
                    Modifier.clickable { readIds = items.map { it.id }.toSet() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Check, contentDescription = "Mark all read", tint = c.tealDeep, modifier = Modifier.size(18.dp))
                }
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.lg, vertical = d.md),
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            // ── Inbox hero ──────────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(c.navy, Color(0xFF3B3870)),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        ),
                    )
                    .padding(d.lg),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(VIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(d.md))
                    Column(Modifier.weight(1f)) {
                        Text("INBOX", style = VTheme.type.label.colored(Color.White.copy(alpha = 0.7f)))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(unread.toString(), style = VTheme.type.h1.colored(Color.White))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "unread",
                                style = VTheme.type.body.colored(Color.White.copy(alpha = 0.7f)),
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                    }
                }
            }

            // ── Filter pills ──────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                VTag(text = "All", active = !filterUnread, onClick = { filterUnread = false })
                VTag(
                    text = if (unread > 0) "Unread · $unread" else "Unread",
                    active = filterUnread,
                    onClick = { filterUnread = true },
                )
            }

            // ── List or empty state ────────────────────────────────────────────────
            if (visible.isEmpty()) {
                CaughtUpState()
            } else {
                visible.forEach { NotificationRow(it, onClick = { readIds = readIds + it.id }) }
            }

            // ── Preferences footer ──────────────────────────────────────────────────
            Spacer(Modifier.height(d.sm))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.cream)
                    .clickable {}
                    .padding(vertical = d.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(VIcons.Close, contentDescription = null, tint = c.ink2, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Notification preferences", style = VTheme.type.caption.colored(c.ink2))
            }
            Spacer(Modifier.height(d.lg))
        }
    }
}

@Composable
private fun NotificationRow(n: VNotification, onClick: () -> Unit) {
    val c = VTheme.colors
    val (tileBg, tileFg) = categoryTile(n.category)
    VCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIcon(n.category), contentDescription = null, tint = tileFg, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(VTheme.dimens.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VBadge(text = n.category, tone = categoryBadgeTone(n.category))
                    Spacer(Modifier.width(VTheme.dimens.sm))
                    Text(n.time, style = VTheme.type.caption.colored(c.ink3))
                }
                Spacer(Modifier.height(6.dp))
                Text(n.title, style = VTheme.type.bodyStrong.colored(c.ink))
                Text(n.body, style = VTheme.type.caption.colored(c.ink2))
            }
            if (n.unread) {
                Box(
                    Modifier
                        .padding(start = VTheme.dimens.sm, top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c.tealDeep),
                )
            } else {
                Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CaughtUpState() {
    val c = VTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(c.cream),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VIcons.Check, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(22.dp))
        }
        Text("You're all caught up", style = VTheme.type.bodyStrong.colored(c.ink), textAlign = TextAlign.Center)
        Text("No notifications.", style = VTheme.type.caption.colored(c.ink3), textAlign = TextAlign.Center)
    }
}

/** UI-only notification model. Mirrors the React `notifications` mock shape so a real feed can map to it. */
data class VNotification(
    val id: String,
    val category: String, // "attendance" | "academic" | "fees" | "announcement"
    val title: String,
    val body: String,
    val time: String,
    val unread: Boolean,
)

private fun categoryIcon(cat: String): ImageVector = when (cat.lowercase()) {
    "attendance" -> VIcons.Calendar
    "academic" -> VIcons.School
    "fees" -> VIcons.Wallet
    else -> VIcons.Megaphone
}

private fun categoryBadgeTone(cat: String): VBadgeTone = when (cat.lowercase()) {
    "fees" -> VBadgeTone.Danger
    "attendance" -> VBadgeTone.Warning
    "academic" -> VBadgeTone.Arctic
    else -> VBadgeTone.Success
}

/** Category-tinted icon-tile colors, matching the React `toneFor()` map. */
@Composable
private fun categoryTile(cat: String): Pair<Color, Color> {
    val c = VTheme.colors
    return when (cat.lowercase()) {
        "attendance" -> c.warning.copy(alpha = 0.55f) to c.warningInk
        "fees" -> c.danger.copy(alpha = 0.55f) to c.dangerInk
        "academic" -> c.teal.copy(alpha = 0.18f) to c.tealDeep
        else -> c.success.copy(alpha = 0.42f) to c.successInk
    }
}
