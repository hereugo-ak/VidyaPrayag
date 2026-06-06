package com.littlebridge.vidyaprayag.ui.v2.screens.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import com.littlebridge.vidyaprayag.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * DiscoveryScreenV2 — the school marketplace, translated from Discovery.tsx.
 *
 * Search + board filter chips over the schools list (from [ParentDashboardViewModel], which wraps
 * `GetSchoolsUseCase`), with shortlist toggling (max 3, enforced in the VM). SRI / reviews / compare
 * are gap G11 — rendered as a `VComingSoon` teaser, never fabricated.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryScreenV2(
    modifier: Modifier = Modifier,
    onOpenSchool: (String) -> Unit = {},
    viewModel: ParentDashboardViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val schoolsState by viewModel.schools.collectAsStateV2()
    val shortlist by viewModel.shortlist.collectAsStateV2()

    var query by remember { mutableStateOf("") }
    var board by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        Text("Discover schools", style = VTheme.type.h2.colored(c.ink))

        VInput(value = query, onValueChange = { query = it }, placeholder = "Search by name or area", leadingIcon = VIcons.Search)

        val boards = listOf("CBSE", "ICSE", "State", "IB")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalArrangement = Arrangement.spacedBy(d.sm)) {
            boards.forEach { b ->
                com.littlebridge.vidyaprayag.ui.v2.components.VTag(
                    text = b,
                    active = board == b,
                    onClick = { board = if (board == b) null else b },
                )
            }
        }

        when (val s = schoolsState) {
            is UiState.Loading -> VEmptyState(title = "Loading schools", icon = VIcons.School)
            is UiState.Error -> Text(s.message, style = VTheme.type.caption.colored(c.dangerInk))
            is UiState.Success -> {
                val filtered = s.data.filter { school ->
                    (query.isBlank() || school.name.contains(query, ignoreCase = true) || school.location.contains(query, ignoreCase = true)) &&
                        (board == null || school.board.equals(board, ignoreCase = true))
                }
                if (filtered.isEmpty()) {
                    VEmptyState(title = "No schools match", icon = VIcons.Search, body = "Try a different search or filter.")
                } else {
                    VSectionHeader("${filtered.size} SCHOOLS")
                    filtered.forEach { school ->
                        SchoolCard(
                            school = school,
                            shortlisted = shortlist.contains(school.id),
                            onToggle = { viewModel.toggleShortlist(school.id) },
                            onOpen = { onOpenSchool(school.id) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun SchoolCard(
    school: School,
    shortlisted: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val c = VTheme.colors
    VCard(onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
            Box(Modifier.weight(1f)) {
                Text(school.name, style = VTheme.type.h4.colored(c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (school.isVerified) VBadge(text = "VERIFIED", tone = VBadgeTone.Success)
        }
        Text("${school.location} · ${school.board}", style = VTheme.type.caption.colored(c.ink3))
        if (school.feesRange.isNotBlank()) {
            Text(school.feesRange, style = VTheme.type.dataSm.colored(c.ink2))
        }
        Spacer(Modifier.height(VTheme.dimens.xs))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            VButton(
                text = if (shortlisted) "Shortlisted" else "Shortlist",
                onClick = onToggle,
                variant = VButtonVariant.Secondary,
                tone = if (shortlisted) VButtonTone.Teal else VButtonTone.Navy,
                size = VButtonSize.Sm,
            )
            if (school.sriScore > 0) {
                VBadge(text = "SRI ${school.sriScore}", tone = VBadgeTone.Arctic)
            }
        }
    }
}
