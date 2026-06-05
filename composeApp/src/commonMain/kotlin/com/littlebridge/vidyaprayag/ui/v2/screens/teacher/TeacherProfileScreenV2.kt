package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherProfileScreenV2 — teacher "Profile" tab, translated from Teacher.tsx → Profile.
 *
 * Identity card (avatar + name + username + school), subject & class chips, contact rows, and a
 * sign-out action surfaced to the host. Bound to the existing [TeacherProfileViewModel].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherProfileScreenV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherProfileViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()
    val p = state.profile

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        Text("Profile", style = VTheme.type.h2.colored(c.ink))

        VCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                VAvatar(name = p?.name ?: "?", src = p?.photoUrl, size = 56.dp, ring = true)
                Column(Modifier.weight(1f)) {
                    Text(p?.name ?: "—", style = VTheme.type.h3.colored(c.ink))
                    Text(p?.username ?: "", style = VTheme.type.dataSm.colored(c.ink3))
                    Text(p?.schoolName ?: "", style = VTheme.type.caption.colored(c.ink2))
                }
            }
        }

        if (p != null) {
            VCard {
                VLabel("SUBJECTS")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.xs), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                    p.subjects.forEach { VBadge(text = it, tone = VBadgeTone.Arctic) }
                    if (p.subjects.isEmpty()) Text("—", style = VTheme.type.caption.colored(c.ink3))
                }
                Spacer(Modifier.height(d.sm))
                VLabel("CLASSES")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.xs), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                    p.classes.forEach { VBadge(text = it, tone = VBadgeTone.Neutral) }
                    if (p.classes.isEmpty()) Text("—", style = VTheme.type.caption.colored(c.ink3))
                }
            }

            VCard {
                ContactRow(VIcons.Mail, p.email.ifBlank { "No email on file" })
                VDivider(Modifier.padding(vertical = d.sm))
                ContactRow(VIcons.Phone, p.phone.ifBlank { "No phone on file" })
            }
        }

        if (state.error != null) {
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }

        VButton(
            text = "Sign out",
            onClick = onLogout,
            full = true,
            variant = VButtonVariant.Destructive,
            tone = VButtonTone.Rose,
        )
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm),
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        Text(text, style = VTheme.type.body.colored(c.ink2))
    }
}
