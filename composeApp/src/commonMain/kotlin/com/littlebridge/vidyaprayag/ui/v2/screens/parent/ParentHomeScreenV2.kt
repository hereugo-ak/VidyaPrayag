package com.littlebridge.vidyaprayag.ui.v2.screens.parent

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.auth.domain.model.UserDetailsData
import com.littlebridge.vidyaprayag.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VPortalHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentHomeScreenV2 — parent "Home" tab, translated from Parent.tsx → Home.
 *
 * Child hero (avatar + name + today's status), a quick-glance status strip, and a shortlisted-
 * schools list (Discovery cross-link). Bound to the existing [ParentDashboardViewModel] — which
 * exposes `userDetails` (child/parent identity) and `schools` (Discovery list + shortlist).
 *
 * The richer daily-status feed (topics / homework / tests) is owned by `DailyStatusViewModel` and
 * surfaced in the Academics tab; Home keeps the lightweight glance per the design.
 */
@Composable
fun ParentHomeScreenV2(
    modifier: Modifier = Modifier,
    viewModel: ParentDashboardViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val userState by viewModel.userDetails.collectAsStateV2()
    val hasChild by viewModel.hasChildProfile.collectAsStateV2()

    val name = (userState as? UiState.Success<UserDetailsData>)?.data?.personalDetails?.name ?: ""
    val photo = (userState as? UiState.Success<UserDetailsData>)?.data?.personalDetails?.profilePic

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        VPortalHeader(name = name, subtitle = "Welcome back", photoUrl = photo)

        // Today's glance strip
        VCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                VStatusDot(color = if (hasChild) c.successInk else c.warningInk, ring = true)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (hasChild) "Your child is marked present today" else "Link your child to see daily status",
                        style = VTheme.type.h4.colored(c.ink),
                    )
                    Text("Live attendance & updates", style = VTheme.type.caption.colored(c.ink3))
                }
                VBadge(text = if (hasChild) "ACTIVE" else "SETUP", tone = if (hasChild) VBadgeTone.Success else VBadgeTone.Warning)
            }
        }

        VSectionHeader("QUICK ACTIONS")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            QuickAction("Academics", VIcons.School, Modifier.weight(1f))
            QuickAction("Fees", VIcons.Wallet, Modifier.weight(1f))
            QuickAction("Activity", VIcons.Bell, Modifier.weight(1f))
        }

        // Shortlisted schools (Discovery cross-link)
        ShortlistSection(viewModel)

        if (userState is UiState.Error) {
            Text((userState as UiState.Error).message, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VTheme.dimens.xs)) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(22.dp))
            Text(label, style = VTheme.type.caption.colored(c.ink2))
        }
    }
}

@Composable
private fun ShortlistSection(viewModel: ParentDashboardViewModel) {
    val c = VTheme.colors
    val schoolsState by viewModel.schools.collectAsStateV2()
    val shortlist by viewModel.shortlist.collectAsStateV2()

    VSectionHeader("SHORTLISTED SCHOOLS")
    when (val s = schoolsState) {
        is UiState.Loading -> VEmptyState(title = "Loading schools", icon = VIcons.School)
        is UiState.Error -> Text(s.message, style = VTheme.type.caption.colored(c.dangerInk))
        is UiState.Success -> {
            val shortlisted = s.data.filter { shortlist.contains(it.id) }
            if (shortlisted.isEmpty()) {
                VEmptyState(
                    title = "No schools shortlisted",
                    icon = VIcons.Bookmark,
                    body = "Explore Discovery and bookmark up to 3 schools to compare.",
                )
            } else {
                shortlisted.forEach { school ->
                    VCard(onClick = { viewModel.toggleShortlist(school.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
                            Box(Modifier.weight(1f)) {
                                Text(school.name, style = VTheme.type.h4.colored(c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            VBadge(text = "SHORTLISTED", tone = VBadgeTone.Arctic)
                        }
                    }
                }
            }
        }
    }
}
