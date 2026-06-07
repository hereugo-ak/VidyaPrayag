package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressState
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentAcademicsScreenV2 — a faithful copy of `Parent.tsx → Academics`.
 *
 * Top tabs: Overview · Attendance · Marks · Syllabus · Report. **Overview is wired to the real
 * [TrackProgressViewModel]** (`shared/`) → `GET /api/v1/parent/track-progress` and renders the
 * child's academic competencies + emotional-intelligence radar from genuine backend data.
 *
 * NOTE (honest state): the parent backend (`ParentApi`) currently exposes only track-progress,
 * fees, scholarships and announcements — there is **no** per-child attendance-heatmap / marks-history
 * / syllabus-log endpoint. Rather than fabricate those (LAW 2: no MockV2 in production paths), those
 * tabs render [VComingSoon] until their backend exists. This keeps every tab honest and MockV2-free.
 */
@Composable
fun ParentAcademicsScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TrackProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    ParentAcademicsContent(state = state, modifier = modifier)
}

/** Stateless body — also used by @Preview with seeded state (no MockV2 in the live path). */
@Composable
private fun ParentAcademicsContent(
    state: TrackProgressState,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    var tab by remember { mutableStateOf("Overview") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            "Academics",
            style = VTheme.type.h1.colored(c.ink),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        VTopTabs(
            tabs = listOf("Overview", "Attendance", "Marks", "Syllabus", "Report"),
            selected = tab,
            onSelect = { tab = it },
        )
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(d.sm + 4.dp),
        ) {
            when (tab) {
                "Overview" -> OverviewTab(state)
                "Attendance" -> VComingSoon(
                    title = "Daily attendance",
                    description = "A month-by-month attendance heatmap for your child will appear here once the school enables the attendance feed.",
                )
                "Marks" -> VComingSoon(
                    title = "Marks & trends",
                    description = "Assessment marks and subject-wise performance trends will appear here once the school publishes results to parents.",
                )
                "Syllabus" -> VComingSoon(
                    title = "Syllabus log",
                    description = "A day-by-day log of what's been covered in class will appear here once the school shares the syllabus feed.",
                )
                "Report" -> VComingSoon(
                    title = "AI Report Card",
                    description = "At the end of each term, VidyaSetu will generate a personalised academic summary — narrative strengths, focus areas and study tips.",
                    preview = { AiReportCardPreview() },
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(state: TrackProgressState) {
    val c = VTheme.colors
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.academicCompetencies.isEmpty() && state.emotionalIntelligence.isEmpty(),
        emptyTitle = "No progress data yet",
        emptyBody = "Your child's competencies will appear here as teachers update them.",
    ) {
        if (state.academicCompetencies.isNotEmpty()) {
            state.academicCompetencies.forEach { comp ->
                VCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(comp.title, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("${(comp.progress * 100f).toInt()}%", style = VTheme.type.data.colored(c.ink))
                    }
                    Spacer(Modifier.height(8.dp))
                    VProgressBar(value = comp.progress * 100f, tone = VBadgeTone.Arctic)
                }
            }
        }

        if (state.emotionalIntelligence.isNotEmpty()) {
            VCard {
                VLabel("Emotional intelligence")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.emotionalIntelligence.forEach { (trait, score) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(trait, style = VTheme.type.caption.colored(c.ink2))
                                Text("${(score * 100f).toInt()}%", style = VTheme.type.dataSm.colored(c.ink))
                            }
                            VProgressBar(value = score * 100f, tone = VBadgeTone.Success)
                        }
                    }
                }
            }
        }

        if (state.currentLevel > 0) {
            VCard {
                VLabel("Journey level")
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Level ${state.currentLevel}", style = VTheme.type.dataLg.colored(c.ink))
                    VBadge(text = "${(state.overallProgress * 100f).toInt()}% complete", tone = VBadgeTone.Arctic)
                }
            }
        }
    }
}
