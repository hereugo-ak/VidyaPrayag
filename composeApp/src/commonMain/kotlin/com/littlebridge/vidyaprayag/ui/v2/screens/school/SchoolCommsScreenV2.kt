package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressRing
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * SchoolCommsScreenV2 — a pixel-faithful copy of `Admin.tsx → Comms`.
 *
 * Four sub-tabs — Announcements (compose + list with open-rate badge), Messages (parent inbox),
 * PTM (next-meeting card + schedule + online-PTM preview), Notifications (delivery log). From [MockV2].
 */
@Composable
fun SchoolCommsScreenV2(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    var tab by remember { mutableStateOf("Announcements") }
    var openAnnouncement by remember { mutableStateOf<String?>(null) }

    // React `Comms` opens `AnnouncementDetail` on card tap; we mirror that with a leaf overlay.
    openAnnouncement?.let { id ->
        AnnouncementDetailV2(id = id, onBack = { openAnnouncement = null }, modifier = modifier)
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Communications", style = VTheme.type.h1.colored(c.ink))
        VTopTabs(tabs = listOf("Announcements", "Messages", "PTM", "Notifications"), selected = tab, onSelect = { tab = it })
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
                "Announcements" -> {
                    VButton(text = "Compose announcement", onClick = {}, full = true, size = VButtonSize.Lg, tone = VButtonTone.Teal, leading = { Icon(VIcons.Plus, null, modifier = Modifier.size(16.dp)) })
                    MockV2.announcements.forEach { a ->
                        VCard(onClick = { openAnnouncement = a.id }) {
                            Text(a.title, style = VTheme.type.bodyStrong.colored(c.ink))
                            // React: subtitle has marginTop 2 below the title.
                            Text("${a.recipients} • ${a.date}", style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 2.dp))
                            Spacer(Modifier.height(8.dp))
                            VBadge(text = "Opens ${a.opens}", tone = VBadgeTone.Arctic)
                        }
                    }
                }
                "Messages" -> {
                    MockV2.messagesInbox.forEach { m ->
                        VCard {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                VAvatar(name = m.parent, size = 36.dp)
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(m.parent, style = VTheme.type.bodyStrong.colored(c.ink))
                                        if (m.overdue) VBadge(text = "Overdue", tone = VBadgeTone.Danger)
                                    }
                                    Text(m.child, style = VTheme.type.label.colored(c.ink3))
                                    Text(m.preview, style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 6.dp))
                                    Text(m.time, style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
                "PTM" -> {
                    VCard {
                        VLabel("Next PTM")
                        Text("Half-yearly PTM — Class 10", style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.padding(top = 4.dp))
                        Text("Tomorrow, 10:00 AM — 1:00 PM • Physical", style = VTheme.type.caption.colored(c.ink2))
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VProgressRing(value = 74f, size = 56.dp, strokeWidth = 6.dp)
                            Column {
                                Text("47 of 65 slots booked", style = VTheme.type.bodyStrong.colored(c.ink))
                                Text("Bookings close in 18 hours", style = VTheme.type.caption.colored(c.ink2))
                            }
                        }
                    }
                    VButton(text = "Schedule new PTM", onClick = {}, full = true, size = VButtonSize.Lg, tone = VButtonTone.Peach, leading = { Icon(VIcons.Plus, null, modifier = Modifier.size(16.dp)) })
                    VComingSoon(title = "Online PTM (video)", description = "Hold the meeting inside the app over secure video. Releasing next quarter.")
                }
                "Notifications" -> {
                    MockV2.notifications.take(6).forEach { n ->
                        VCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(n.title, style = VTheme.type.body.colored(c.ink), modifier = Modifier.weight(1f))
                                VBadge(text = "Delivered", tone = VBadgeTone.Success)
                            }
                            Text("${n.body} • ${n.time}", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * AnnouncementDetailV2 — `Admin.tsx → AnnouncementDetail` (exported). Title, meta, recipient +
 * channel badges, body copy, and (Admin context = React `dark`) the 3-up Delivery stat card.
 */
@Composable
private fun AnnouncementDetailV2(id: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val a = MockV2.announcements.find { it.id == id } ?: MockV2.announcements[0]
    // React splits opens "opened / sent" — index 0 = opened, index 1 = sent.
    val parts = a.opens.split(" / ")
    val opened = parts.getOrElse(0) { a.opens }
    val sent = parts.getOrElse(1) { a.opens }

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Announcement", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 20.dp),
        ) {
            Text(a.title, style = VTheme.type.h2.colored(c.ink))
            Text("${a.date} • Posted by School Administration", style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VBadge(text = a.recipients, tone = VBadgeTone.Arctic)
                VBadge(text = "App + WhatsApp", tone = VBadgeTone.Success)
            }
            // React body: 14px, line-height 1.6 → 22.4sp.
            Text(a.body, style = VTheme.type.body.colored(c.ink2).copy(lineHeight = 22.4.sp), modifier = Modifier.padding(top = 16.dp))
            Spacer(Modifier.height(16.dp))
            VCard {
                VLabel("Delivery")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeliveryStat("Sent", sent, Modifier.weight(1f))
                    DeliveryStat("Opened", opened, Modifier.weight(1f))
                    DeliveryStat("Replied", "9", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeliveryStat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    // Mirrors Admin.tsx `Stat`: tile bg ink@6%, 10px label, 16px mono value.
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp),
    ) {
        Text(label.uppercase(), style = VTheme.type.label.colored(c.ink3).copy(fontSize = 10.sp))
        Text(value, style = VTheme.type.data.colored(c.ink).copy(fontSize = 16.sp), modifier = Modifier.padding(top = 4.dp))
    }
}
