package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.littlebridge.vidyaprayag.feature.admin.presentation.Announcement
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolAnnouncementsViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * SchoolCommsScreenV2 — admin "Comms" tab, translated from Admin.tsx → Comms.
 *
 * Announcement list with category filter chips (driven by [SchoolAnnouncementsViewModel]). Messaging
 * and PTM (gaps G7/G8) are not wired — they surface as `VComingSoon` in the portal shell. Read +
 * category-filter is live against the announcements route.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolCommsScreenV2(
    modifier: Modifier = Modifier,
    viewModel: SchoolAnnouncementsViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    val categories = listOf("All", "Holidays", "PTM", "Events", "Update", "Reminder")

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        VSectionHeader("ANNOUNCEMENTS")

        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalArrangement = Arrangement.spacedBy(d.sm)) {
            categories.forEach { cat ->
                val active = (cat == "All" && state.selectedCategory == null) || state.selectedCategory == cat
                VTag(
                    text = cat,
                    active = active,
                    onClick = { viewModel.setCategoryFilter(if (cat == "All") null else cat) },
                )
            }
        }

        if (state.announcements.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No announcements", icon = VIcons.Megaphone, body = "Published updates will appear here.")
        } else {
            state.announcements.forEach { AnnouncementCard(it) }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun AnnouncementCard(a: Announcement) {
    val c = VTheme.colors
    val tone = when (a.category) {
        "PTM" -> VBadgeTone.Arctic
        "Events" -> VBadgeTone.Success
        "Reminder" -> VBadgeTone.Warning
        "Holidays" -> VBadgeTone.Arctic
        else -> VBadgeTone.Neutral
    }
    VCard(border = !a.isFeatured, background = if (a.isFeatured) c.cream else c.card) {
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
