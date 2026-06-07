package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.RiskStudent
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsState
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * SchoolPeopleScreenV2 — wired to the real [StudentAnalyticsViewModel]
 * (`AnalyticsApi` → `GET /api/v1/student-cohort`).
 *
 * The backend's people endpoint is a **cohort-analytics** feed (risk distribution,
 * at-risk students, subject engagement, cohort comparison) — not a flat student/teacher
 * roster. So this screen renders the analytics it actually returns rather than the old
 * mock roster + per-student detail (which had no backing endpoint). A directory/roster
 * screen is tracked as a later addition. No MockV2 in production; the three UI states
 * come from [VStateHost] (LAW 2/3/6).
 */
@Composable
fun SchoolPeopleScreenV2(
    modifier: Modifier = Modifier,
    viewModel: StudentAnalyticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    SchoolPeopleContent(state = state, onRetry = viewModel::load, modifier = modifier)
}

@Composable
private fun SchoolPeopleContent(
    state: StudentAnalyticsState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("People", style = VTheme.type.h1.colored(c.ink))

        VStateHost(
            loading = state.isLoading,
            error = state.errorMessage,
            isEmpty = state.atRiskStudents.isEmpty() &&
                state.subjectEngagements.isEmpty() &&
                state.criticalRiskCount == 0 &&
                state.mediumRiskCount == 0 &&
                state.lowRiskCount == 0,
            emptyTitle = "No cohort data yet",
            emptyBody = "Student risk and engagement analytics appear here once attendance and marks start flowing in.",
            emptyIcon = VIcons.Users,
            onRetry = onRetry,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ── Risk distribution ─────────────────────────────────────────
                VCard {
                    VLabel("Student risk distribution")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RiskTile("Critical", state.criticalRiskCount, c.dangerInk, Modifier.weight(1f))
                        RiskTile("Medium", state.mediumRiskCount, c.warningInk, Modifier.weight(1f))
                        RiskTile("Low", state.lowRiskCount, c.successInk, Modifier.weight(1f))
                    }
                }

                // ── At-risk students ──────────────────────────────────────────
                if (state.atRiskStudents.isNotEmpty()) {
                    Column {
                        Text("At-risk students", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.atRiskStudents.forEach { RiskStudentRow(it) }
                        }
                    }
                }

                // ── Subject engagement ────────────────────────────────────────
                if (state.subjectEngagements.isNotEmpty()) {
                    VCard {
                        Text("Subject engagement", style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.subjectEngagements.forEach { e ->
                                Column {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(e.name, style = VTheme.type.body.colored(c.ink))
                                        Text("${e.percentage.roundToInt()}%", style = VTheme.type.dataSm.colored(c.ink2))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    VProgressBar(
                                        value = e.percentage,
                                        tone = if (e.percentage < 60f) VBadgeTone.Warning else VBadgeTone.Arctic,
                                    )
                                    if (!e.status.isNullOrBlank()) {
                                        Text(e.status, style = VTheme.type.label.colored(c.ink3))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cohort comparison ─────────────────────────────────────────
                if (state.cohortComparison.isNotEmpty()) {
                    VCard {
                        Text("Cohort comparison", style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.cohortComparison.forEachIndexed { i, v ->
                                val label = state.cohortLabels.getOrNull(i) ?: "Grade ${i + 1}"
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label, style = VTheme.type.body.colored(c.ink))
                                        Text("${v.roundToInt()}%", style = VTheme.type.dataSm.colored(c.ink2))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    VProgressBar(value = v, tone = VBadgeTone.Arctic)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskTile(label: String, count: Int, tone: Color, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.toString(), style = VTheme.type.dataLg.colored(tone).copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
    }
}

@Composable
private fun RiskStudentRow(s: RiskStudent) {
    val c = VTheme.colors
    val tone = when (s.riskLevel.lowercase()) {
        "critical" -> c.danger
        "medium" -> c.warning
        else -> c.success
    }
    val badgeTone = when (s.riskLevel.lowercase()) {
        "critical" -> VBadgeTone.Danger
        "medium" -> VBadgeTone.Warning
        else -> VBadgeTone.Success
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VAvatar(name = s.name, src = s.imageUrl.ifBlank { null }, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VStatusDot(color = tone)
                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                }
                if (s.masteryTrend.isNotBlank()) {
                    Text("Mastery: ${s.masteryTrend}", style = VTheme.type.caption.colored(c.ink2))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                VBadge(text = s.riskLevel, tone = badgeTone)
                Text("${s.retentionRisk}% risk", style = VTheme.type.label.colored(c.ink3).copy(fontSize = 10.sp))
            }
        }
    }
}
