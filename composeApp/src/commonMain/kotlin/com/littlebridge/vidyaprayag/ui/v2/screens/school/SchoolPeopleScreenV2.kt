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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.littlebridge.vidyaprayag.feature.admin.presentation.RiskStudent
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SubjectEngagement
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * SchoolPeopleScreenV2 — admin "People" tab, translated from Admin.tsx → People.
 *
 * Cohort risk overview (critical/medium/low counts), at-risk student list, and subject-engagement
 * meters. Bound 1:1 to the existing [StudentAnalyticsViewModel] (auto-loads in `init`). The teacher-
 * roster half of People depends on the teacher backend (gap G1) and stays a `VComingSoon` until then.
 */
@Composable
fun SchoolPeopleScreenV2(
    modifier: Modifier = Modifier,
    viewModel: StudentAnalyticsViewModel = koinViewModel(),
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
        VSectionHeader("RETENTION RISK")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            RiskCard("Critical", state.criticalRiskCount, VBadgeTone.Danger, Modifier.weight(1f))
            RiskCard("Medium", state.mediumRiskCount, VBadgeTone.Warning, Modifier.weight(1f))
            RiskCard("Low", state.lowRiskCount, VBadgeTone.Success, Modifier.weight(1f))
        }

        VSectionHeader("AT-RISK STUDENTS")
        if (state.atRiskStudents.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No students at risk", icon = VIcons.Users, body = "Everyone is on track.")
        } else {
            state.atRiskStudents.forEach { RiskStudentRow(it) }
        }

        if (state.subjectEngagements.isNotEmpty()) {
            VSectionHeader("SUBJECT ENGAGEMENT")
            VCard {
                state.subjectEngagements.forEach { EngagementRow(it) }
            }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun RiskCard(label: String, count: Int, tone: VBadgeTone, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Text(count.toString(), style = VTheme.type.dataLg.colored(c.ink))
        VBadge(text = label.uppercase(), tone = tone)
    }
}

@Composable
private fun RiskStudentRow(s: RiskStudent) {
    val c = VTheme.colors
    val tone = when (s.riskLevel) {
        "Critical" -> VBadgeTone.Danger
        "Medium" -> VBadgeTone.Warning
        else -> VBadgeTone.Success
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            VAvatar(name = s.name, src = s.imageUrl.ifBlank { null })
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.h4.colored(c.ink))
                Text("Retention risk ${s.retentionRisk}% · ${s.masteryTrend}", style = VTheme.type.caption.colored(c.ink3))
            }
            VBadge(text = s.riskLevel.uppercase(), tone = tone)
        }
    }
}

@Composable
private fun EngagementRow(e: SubjectEngagement) {
    VLabel(e.name)
    VProgressBar(value = e.percentage.coerceIn(0f, 100f), tone = VBadgeTone.Arctic)
    Spacer(Modifier.height(VTheme.dimens.sm))
}
