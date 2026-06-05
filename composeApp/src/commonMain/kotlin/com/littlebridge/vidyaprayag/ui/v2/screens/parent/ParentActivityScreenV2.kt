package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAnnouncement
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAnnouncementViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentActivityScreenV2 — parent "Activity" tab, translated from Parent.tsx → Activity.
 *
 * The school-announcements feed plus a WhatsApp-sync toggle. Featured announcements lead the list.
 * Bound 1:1 to the existing [ParentAnnouncementViewModel]. Messaging (parent↔school) is gap G7 —
 * not wired here; this tab stays announcements-only until that route ships.
 */
@Composable
fun ParentActivityScreenV2(
    modifier: Modifier = Modifier,
    viewModel: ParentAnnouncementViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        // WhatsApp sync preference
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                Column(Modifier.weight(1f)) {
                    Text("WhatsApp updates", style = VTheme.type.h4.colored(c.ink))
                    Text("Mirror announcements to WhatsApp", style = VTheme.type.caption.colored(c.ink3))
                }
                Switch(
                    checked = state.isWhatsAppSyncEnabled,
                    onCheckedChange = { viewModel.toggleWhatsAppSync(it) },
                )
            }
        }

        VSectionHeader("ANNOUNCEMENTS")
        if (state.announcements.isEmpty() && !state.isLoading) {
            VEmptyState(title = "Nothing new", icon = VIcons.Megaphone, body = "School updates and reminders will appear here.")
        } else {
            val featured = state.announcements.filter { it.isFeatured }
            val rest = state.announcements.filterNot { it.isFeatured }
            featured.forEach { AnnouncementCard(it, featured = true) }
            rest.forEach { AnnouncementCard(it, featured = false) }
        }

        if (state.error != null) {
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun AnnouncementCard(a: ParentAnnouncement, featured: Boolean) {
    val c = VTheme.colors
    val tone = when (a.category) {
        "PTM" -> VBadgeTone.Arctic
        "Events" -> VBadgeTone.Success
        "Reminder" -> VBadgeTone.Warning
        else -> VBadgeTone.Neutral
    }
    VCard(border = !featured, background = if (featured) c.cream else c.card) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(a.title, style = VTheme.type.h4.colored(c.ink))
            VBadge(text = a.category.uppercase(), tone = tone)
        }
        Text(a.date, style = VTheme.type.dataSm.colored(c.ink3))
        if (a.description.isNotBlank()) {
            Spacer(Modifier.height(VTheme.dimens.xs))
            Text(a.description, style = VTheme.type.body.colored(c.ink2))
        }
    }
}
