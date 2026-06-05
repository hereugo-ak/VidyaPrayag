package com.littlebridge.vidyaprayag.ui.v2.screens.parent

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
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.parent.presentation.AcademicCompetency
import com.littlebridge.vidyaprayag.feature.parent.presentation.AchievementBadge
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressRing
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentAcademicsScreenV2 — parent "Academics" tab, translated from Parent.tsx → Academics.
 *
 * The whole-child journey: overall-progress ring + level, achievement badges (earned/locked),
 * academic competencies (per-skill progress bars), and emotional-intelligence meters. Bound 1:1 to
 * the existing [TrackProgressViewModel].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentAcademicsScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TrackProgressViewModel = koinViewModel(),
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
        // Journey hero
        VCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                VProgressRing(
                    value = (state.overallProgress.coerceIn(0f, 1f)) * 100f,
                    tone = VBadgeTone.Success,
                    label = "${(state.overallProgress.coerceIn(0f, 1f) * 100).toInt()}%",
                )
                Column(Modifier.weight(1f)) {
                    Text(state.childName.ifBlank { "Your child" }, style = VTheme.type.h3.colored(c.ink))
                    Text("Level ${state.currentLevel}", style = VTheme.type.label.colored(c.tealDeep))
                    if (state.journeyDescription.isNotBlank()) {
                        Text(state.journeyDescription, style = VTheme.type.caption.colored(c.ink3))
                    }
                }
            }
        }

        // Achievement badges
        VSectionHeader("ACHIEVEMENTS")
        if (state.badges.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No badges yet", icon = VIcons.Star, body = "Achievements appear as your child grows.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalArrangement = Arrangement.spacedBy(d.sm)) {
                state.badges.forEach { BadgeChip(it) }
            }
        }

        // Academic competencies
        VSectionHeader("COMPETENCIES")
        if (state.academicCompetencies.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No competency data", icon = VIcons.Target)
        } else {
            state.academicCompetencies.forEach { CompetencyRow(it) }
        }

        // Emotional intelligence meters
        if (state.emotionalIntelligence.isNotEmpty()) {
            VSectionHeader("EMOTIONAL INTELLIGENCE")
            VCard {
                state.emotionalIntelligence.forEach { (skill, value) ->
                    VLabel(skill)
                    VProgressBar(value = value.coerceIn(0f, 1f) * 100f, tone = VBadgeTone.Arctic)
                    Spacer(Modifier.height(VTheme.dimens.sm))
                }
            }
        }

        if (state.error != null) {
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun BadgeChip(badge: AchievementBadge) {
    VBadge(
        text = badge.title,
        tone = if (badge.isLocked) VBadgeTone.Neutral else VBadgeTone.Success,
    )
}

@Composable
private fun CompetencyRow(comp: AcademicCompetency) {
    VCard {
        VLabel(comp.title)
        VProgressBar(value = comp.progress.coerceIn(0f, 1f) * 100f, tone = VBadgeTone.Success)
    }
}
