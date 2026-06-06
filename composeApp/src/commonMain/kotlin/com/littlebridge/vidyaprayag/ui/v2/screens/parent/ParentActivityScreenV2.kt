package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * ParentActivityScreenV2 — a pixel-faithful copy of `Parent.tsx → Activity`.
 *
 * Filter chips (All · Attendance · Academic · Fees · Announcement · Messages), the notification feed
 * with a category icon-chip + unread accent, and a "From School" conversation card with an inline
 * reply box. All from [MockV2.notifications].
 */
@Composable
fun ParentActivityScreenV2(
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var filter by remember { mutableStateOf("All") }
    var replyOpen by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }

    val filters = listOf("All", "Attendance", "Academic", "Fees", "Announcement", "Messages")
    val filtered = if (filter == "All") MockV2.notifications else MockV2.notifications.filter {
        it.category.name.equals(filter, ignoreCase = true)
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        Text("Activity", style = VTheme.type.h1.colored(c.ink), modifier = Modifier.padding(bottom = 12.dp))

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { f ->
                VTag(text = f, active = filter == f, onClick = { filter = f })
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filtered.forEach { n ->
                NotificationCard(n)
            }

            // From School conversation card
            VCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(c.cream),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(VIcons.Chat, contentDescription = null, tint = c.ink2, modifier = Modifier.size(16.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("From School", style = VTheme.type.bodyStrong.colored(c.ink))
                            VBadge(text = "Conversation", tone = VBadgeTone.Neutral)
                        }
                        Text("Please carry the report stub to tomorrow's PTM.", style = VTheme.type.caption.colored(c.ink2))
                        val rInteraction = remember { MutableInteractionSource() }
                        Text(
                            "Reply to school",
                            style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable(interactionSource = rInteraction, indication = null) { replyOpen = !replyOpen },
                        )
                        if (replyOpen) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VInput(
                                    value = reply,
                                    onValueChange = { reply = it },
                                    placeholder = "Write a reply…",
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(c.teal.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(VIcons.Send, contentDescription = "Send", tint = c.tealDeep, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: MockV2.Notification) {
    val c = VTheme.colors
    val (chipBg, icon) = when (n.category) {
        MockV2.NotifCategory.Attendance -> c.teal.copy(alpha = 0.16f) to VIcons.Check
        MockV2.NotifCategory.Academic -> c.warning.copy(alpha = 0.45f) to VIcons.School
        MockV2.NotifCategory.Fees -> c.danger.copy(alpha = 0.45f) to VIcons.Wallet
        MockV2.NotifCategory.Announcement -> c.cream to VIcons.Bell
    }
    Row(Modifier.fillMaxWidth()) {
        if (n.unread) {
            Box(Modifier.width(3.dp).height(56.dp).background(c.tealDeep))
            Spacer(Modifier.width(8.dp))
        }
        VCard(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(chipBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = c.ink2, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(n.title, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text(n.body, style = VTheme.type.caption.colored(c.ink2))
                    Text(n.time, style = VTheme.type.label.colored(c.ink3), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
