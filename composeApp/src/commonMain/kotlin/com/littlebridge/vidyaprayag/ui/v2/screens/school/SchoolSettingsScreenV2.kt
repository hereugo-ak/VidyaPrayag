package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * SchoolSettingsScreenV2 — admin "Settings" tab, translated from Admin.tsx → Settings.
 *
 * Institutional-profile summary (identity, public-listing toggle, profile-completion + storage
 * meters) and a sign-out action surfaced to the host. Bound 1:1 to the existing
 * [InstitutionalProfileViewModel] (auto-loads in `init`). The full onboarding wizard is reached via
 * dedicated routes (Phase 3E navigation) — Settings keeps the at-a-glance summary per the design.
 */
@Composable
fun SchoolSettingsScreenV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: InstitutionalProfileViewModel = koinViewModel(),
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
        Text("Settings", style = VTheme.type.h2.colored(c.ink))

        // Identity
        VCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                VAvatar(name = state.schoolName.ifBlank { "School" }, src = state.profileImageUrl.ifBlank { null }, size = 56.dp, ring = true)
                Column(Modifier.weight(1f)) {
                    Text(state.schoolName.ifBlank { "—" }, style = VTheme.type.h3.colored(c.ink))
                    if (state.location.isNotBlank()) {
                        Text(state.location, style = VTheme.type.caption.colored(c.ink3))
                    }
                    if (state.licenseType.isNotBlank()) {
                        Text(state.licenseType, style = VTheme.type.dataSm.colored(c.tealDeep))
                    }
                }
            }
        }

        // Public listing toggle
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                Column(Modifier.weight(1f)) {
                    Text("Public listing", style = VTheme.type.h4.colored(c.ink))
                    Text("Show this school in Discovery", style = VTheme.type.caption.colored(c.ink3))
                }
                Switch(checked = state.isPublic, onCheckedChange = { viewModel.togglePublic(it) })
            }
        }

        // Profile completion + storage
        VSectionHeader("PROFILE HEALTH")
        VCard {
            VLabel("PROFILE COMPLETION ${state.profileCompletion}%")
            VProgressBar(value = state.profileCompletion.toFloat().coerceIn(0f, 100f), tone = VBadgeTone.Success)
            Spacer(Modifier.height(d.sm))
            VDivider()
            Spacer(Modifier.height(d.sm))
            VLabel("STORAGE ${state.storageUsedHuman} / ${state.totalStorageHuman}")
            VProgressBar(value = state.storageUsage.coerceIn(0f, 1f) * 100f, tone = VBadgeTone.Arctic)
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage!!, style = VTheme.type.caption.colored(c.dangerInk))
        }

        Spacer(Modifier.height(d.sm))
        VButton(
            text = "Sign out",
            onClick = onLogout,
            variant = VButtonVariant.Destructive,
            tone = VButtonTone.Rose,
            full = true,
        )
        Spacer(Modifier.height(d.xl))
    }
}
