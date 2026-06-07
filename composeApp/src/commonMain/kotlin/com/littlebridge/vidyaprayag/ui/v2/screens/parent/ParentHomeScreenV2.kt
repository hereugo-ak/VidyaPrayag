package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressState
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentHomeScreenV2 — a faithful copy of `Parent.tsx → ParentHome`.
 *
 * Renders the gradient child-hero card (avatar + name + journey level + overall-progress bar) plus
 * the child's academic competencies. **Wired to the real [TrackProgressViewModel]** (`shared/`) →
 * `ParentRepository.getTrackProgress` → `GET /api/v1/parent/track-progress`.
 *
 * NOTE (conflict resolution): the gap audit §4.1 mapped this tab to `ParentDashboardViewModel`, but
 * that VM models the *school-discovery list*, not the child's home. `TrackProgressViewModel` is the
 * real per-child backend data that matches this child-centric layout, so we bind it here instead —
 * the least-invasive change that removes MockV2 while showing genuine data.
 */
@Composable
fun ParentHomeScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TrackProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    ParentHomeContent(state = state, modifier = modifier)
}

/** Stateless body — also used by @Preview with seeded state (no MockV2 in the live path). */
@Composable
private fun ParentHomeContent(
    state: TrackProgressState,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.childName.isBlank() && state.academicCompetencies.isEmpty(),
            emptyTitle = "No child linked yet",
            emptyBody = "Link your child to see their daily journey and progress.",
        ) {
            // ── Child hero ──────────────────────────────────────────────────────
            VCard(padding = 0.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (c.isNight) listOf(c.cream, c.card)
                                else listOf(Color(0xFFF6F1FF), Color(0xFFE8F7F3)),
                            ),
                        )
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        VAvatar(name = state.childName.ifBlank { "?" }, size = 68.dp, ring = true)
                        Column(Modifier.weight(1f)) {
                            Text(state.childName.ifBlank { "—" }, style = VTheme.type.h2.colored(c.ink))
                            Text(
                                state.journeyDescription.ifBlank { "Level ${state.currentLevel}" },
                                style = VTheme.type.caption.colored(c.ink2),
                            )
                            Spacer(Modifier.height(8.dp))
                            VBadge(text = "Level ${state.currentLevel}", tone = VBadgeTone.Arctic)
                        }
                    }
                }
                // overall-progress strip
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        VLabel("Overall progress")
                        Text(
                            "${(state.overallProgress * 100f).toInt()}%",
                            style = VTheme.type.dataLg.colored(c.navy).copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                        )
                    }
                    VProgressBar(value = state.overallProgress * 100f)
                }
            }

            // ── Academic competencies ──────────────────────────────────────────
            if (state.academicCompetencies.isNotEmpty()) {
                VCard {
                    VLabel("Academic competencies")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.academicCompetencies.forEach { comp ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(comp.title, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp))
                                    Text("${(comp.progress * 100f).toInt()}%", style = VTheme.type.dataSm.colored(c.ink2))
                                }
                                VProgressBar(value = comp.progress * 100f)
                            }
                        }
                    }
                }
            }

            // ── Achievement badges ──────────────────────────────────────────────
            if (state.badges.isNotEmpty()) {
                VCard {
                    VLabel("Achievements")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.badges.forEach { b ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    Modifier.height(10.dp).clip(RoundedCornerShape(999.dp))
                                        .background(if (b.isLocked) c.cream else c.teal)
                                        .padding(horizontal = 5.dp),
                                )
                                Text(
                                    b.title,
                                    style = VTheme.type.body.colored(if (b.isLocked) c.ink3 else c.ink).copy(fontSize = 13.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
