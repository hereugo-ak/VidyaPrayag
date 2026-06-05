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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeAnnouncement
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentFeesScreenV2 — parent "Fees" tab, translated from Parent.tsx → Fees.
 *
 * Outstanding/collected summary card with a collection-progress bar, an overdue counter, and the
 * fee-announcements feed. Bound 1:1 to the existing [FeeViewModel]; all monetary strings come pre-
 * formatted from the VM (no client-side currency math).
 */
@Composable
fun ParentFeesScreenV2(
    modifier: Modifier = Modifier,
    viewModel: FeeViewModel = koinViewModel(),
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
        // Summary
        VCard {
            Text("OUTSTANDING", style = VTheme.type.label.colored(c.ink3))
            Text(state.outstandingFees, style = VTheme.type.dataLg.colored(c.ink))
            Spacer(Modifier.height(d.sm))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Collected ${state.totalCollected}", style = VTheme.type.dataSm.colored(c.ink2))
                if (state.overdueCount > 0) {
                    VBadge(text = "${state.overdueCount} OVERDUE", tone = VBadgeTone.Danger)
                } else {
                    VBadge(text = "ON TRACK", tone = VBadgeTone.Success)
                }
            }
            Spacer(Modifier.height(d.xs))
            VProgressBar(
                value = (state.collectionProgress.coerceIn(0f, 1f)) * 100f,
                tone = if (state.overdueCount > 0) VBadgeTone.Warning else VBadgeTone.Success,
            )
        }

        VSectionHeader("FEE UPDATES")
        if (state.announcements.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No fee updates", icon = VIcons.Wallet, body = "Payment reminders and campaigns appear here.")
        } else {
            state.announcements.forEach { FeeAnnouncementCard(it) }
        }

        if (state.error != null) {
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun FeeAnnouncementCard(a: FeeAnnouncement) {
    val c = VTheme.colors
    val tone = when (a.type) {
        "Emergency" -> VBadgeTone.Danger
        "Payment" -> VBadgeTone.Success
        else -> VBadgeTone.Arctic
    }
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(a.title, style = VTheme.type.h4.colored(c.ink))
            VBadge(text = a.type.uppercase(), tone = tone)
        }
        Text(a.time, style = VTheme.type.dataSm.colored(c.ink3))
        if (a.description.isNotBlank()) {
            Spacer(Modifier.height(VTheme.dimens.xs))
            Text(a.description, style = VTheme.type.body.colored(c.ink2))
        }
    }
}
