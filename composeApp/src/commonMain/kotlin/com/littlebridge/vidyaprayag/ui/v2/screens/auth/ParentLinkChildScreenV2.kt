package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.parent.presentation.ChildBasicInfoViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.YourPreferencesViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentLinkChildScreenV2 — the 3-step "link your child" wizard, translated from
 * `Auth.tsx → ParentLinkChild`.
 *
 *  1. About you / child  → [ChildBasicInfoViewModel] (name, grade, interests)
 *  2. Preferences        → [YourPreferencesViewModel] (what matters to this family)
 *  3. Review & finish    → local confirmation
 *
 * **Backend gap (documented in PHASE_PLAN):** there is *no* parent→child link API or Supabase
 * schema yet — these ViewModels are local-state only and have no `submit()`. So this wizard collects
 * the family's input but does **not** persist it; the final step simply calls [onDone] to route into
 * the Parent portal. Per the hard UI rule we render only real (locally-held) state and never fake a
 * server "match". When the link endpoint lands, wire a `submit { onDone() }` into step 3.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentLinkChildScreenV2(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }
    val total = 3

    val childVm: ChildBasicInfoViewModel = koinViewModel()
    val prefsVm: YourPreferencesViewModel = koinViewModel()
    val child by childVm.state.collectAsStateV2()
    val prefs by prefsVm.state.collectAsStateV2()

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = VTheme.dimens.lg)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(VTheme.dimens.xl))
        Text("Step ${step + 1} of $total", style = VTheme.type.label.colored(VTheme.colors.ink3))
        Spacer(Modifier.height(VTheme.dimens.sm))
        StepBars(current = step, total = total)
        Spacer(Modifier.height(VTheme.dimens.lg))

        when (step) {
            0 -> {
                Text("Tell us about your child", style = VTheme.type.h2.colored(VTheme.colors.ink))
                Text(
                    "So your child's school knows who to send updates to.",
                    style = VTheme.type.body.colored(VTheme.colors.ink2),
                    modifier = Modifier.padding(top = VTheme.dimens.xs),
                )
                Spacer(Modifier.height(VTheme.dimens.lg))
                VInput(child.name, childVm::updateName, label = "Child's full name", placeholder = "e.g. Riya Sharma", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(VTheme.dimens.md))
                VInput(child.grade, childVm::updateGrade, label = "Grade / class", placeholder = "e.g. Class 10", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(VTheme.dimens.md))
                VInput(child.dob, childVm::updateDob, label = "Date of birth", placeholder = "DD/MM/YYYY", modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(VTheme.dimens.md))
                Text("Interests", style = VTheme.type.label.colored(VTheme.colors.ink3))
                Spacer(Modifier.height(VTheme.dimens.sm))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
                    child.availableInterests.forEach { interest ->
                        VTag(
                            text = interest,
                            active = child.selectedInterests.contains(interest),
                            onClick = { childVm.toggleInterest(interest) },
                        )
                    }
                }
            }

            1 -> {
                Text("What matters to you?", style = VTheme.type.h2.colored(VTheme.colors.ink))
                Text(
                    "We'll highlight schools that fit your family.",
                    style = VTheme.type.body.colored(VTheme.colors.ink2),
                    modifier = Modifier.padding(top = VTheme.dimens.xs),
                )
                Spacer(Modifier.height(VTheme.dimens.lg))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
                    prefs.availablePreferences.forEach { pref ->
                        VTag(
                            text = pref.title,
                            active = prefs.selectedPreferences.contains(pref.id),
                            onClick = { prefsVm.togglePreference(pref.id) },
                        )
                    }
                }
            }

            else -> {
                Text("Review & finish", style = VTheme.type.h2.colored(VTheme.colors.ink))
                Text(
                    "Confirm the details before opening your dashboard.",
                    style = VTheme.type.body.colored(VTheme.colors.ink2),
                    modifier = Modifier.padding(top = VTheme.dimens.xs),
                )
                Spacer(Modifier.height(VTheme.dimens.lg))
                ReviewRow("Child", child.name.ifBlank { "—" })
                ReviewRow("Grade", child.grade.ifBlank { "—" })
                ReviewRow("Interests", child.selectedInterests.joinToString(", ").ifBlank { "—" })
                ReviewRow(
                    "Priorities",
                    prefs.availablePreferences
                        .filter { prefs.selectedPreferences.contains(it.id) }
                        .joinToString(", ") { it.title }
                        .ifBlank { "—" },
                )
            }
        }

        Spacer(Modifier.height(VTheme.dimens.xl))
        VButton(
            text = if (step < total - 1) "Continue" else "Finish & open dashboard",
            onClick = { if (step < total - 1) step++ else onDone() },
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            soft = false,
        )
        Spacer(Modifier.height(VTheme.dimens.md))
        VButton(
            text = if (step == 0) "Cancel" else "Back",
            onClick = { if (step == 0) onBack() else step-- },
            full = true,
            variant = VButtonVariant.Ghost,
            tone = VButtonTone.Navy,
        )
        Spacer(Modifier.height(VTheme.dimens.xl))
    }
}

@Composable
private fun StepBars(current: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.xs)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) VTheme.colors.tealDeep else VTheme.colors.border2),
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = VTheme.dimens.xs)) {
        Text(label, style = VTheme.type.caption.colored(VTheme.colors.ink3), modifier = Modifier.weight(1f))
        Text(value, style = VTheme.type.bodyStrong.colored(VTheme.colors.ink), modifier = Modifier.weight(2f))
    }
}
