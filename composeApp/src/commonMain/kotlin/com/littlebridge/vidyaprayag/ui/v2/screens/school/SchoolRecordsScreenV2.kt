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
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicMilestone
import com.littlebridge.vidyaprayag.feature.admin.presentation.DepartmentProgress
import com.littlebridge.vidyaprayag.feature.admin.presentation.LaggingAlert
import com.littlebridge.vidyaprayag.feature.admin.presentation.SyllabusCoverageViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * SchoolRecordsScreenV2 — admin "Records" tab, translated from Admin.tsx → Records.
 *
 * Syllabus-coverage overview: overall %, per-department progress bars, lagging alerts, and academic
 * milestones. Bound 1:1 to the existing [SyllabusCoverageViewModel] (auto-loads in `init`).
 * Attendance/marks records (gaps G3/G4) live in the teacher write-path and aren't fabricated here.
 */
@Composable
fun SchoolRecordsScreenV2(
    modifier: Modifier = Modifier,
    viewModel: SyllabusCoverageViewModel = koinViewModel(),
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
        // Overall coverage
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SYLLABUS COVERAGE", style = VTheme.type.label.colored(c.ink3))
                Text("${state.overallPercentage}%", style = VTheme.type.dataLg.colored(c.tealDeep))
            }
            if (state.overallTrend.isNotBlank()) {
                Text(state.overallTrend, style = VTheme.type.caption.colored(c.ink3))
            }
            Spacer(Modifier.height(d.xs))
            VProgressBar(value = state.overallPercentage.toFloat().coerceIn(0f, 100f), tone = VBadgeTone.Success)
        }

        VSectionHeader("DEPARTMENTS")
        if (state.departmentProgress.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No department data", icon = VIcons.School)
        } else {
            state.departmentProgress.forEach { DepartmentRow(it) }
        }

        if (state.alerts.isNotEmpty()) {
            VSectionHeader("LAGGING ALERTS")
            state.alerts.forEach { AlertRow(it) }
        }

        if (state.milestones.isNotEmpty()) {
            VSectionHeader("MILESTONES")
            state.milestones.forEach { MilestoneRow(it) }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun DepartmentRow(dp: DepartmentProgress) {
    val c = VTheme.colors
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            VLabel(dp.name)
            Text(dp.trend, style = VTheme.type.caption.colored(if (dp.isDelayed) c.dangerInk else c.successInk))
        }
        VProgressBar(
            value = dp.progress.coerceIn(0f, 100f),
            tone = if (dp.isDelayed) VBadgeTone.Warning else VBadgeTone.Success,
        )
    }
}

@Composable
private fun AlertRow(a: LaggingAlert) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            VStatusDot(color = if (a.isCritical) c.dangerInk else c.warningInk)
            Column(Modifier.weight(1f)) {
                Text("${a.subject} · ${a.className}", style = VTheme.type.h4.colored(c.ink))
                Text(a.instructor, style = VTheme.type.caption.colored(c.ink3))
            }
            VBadge(text = "-${a.delayPercentage}%", tone = if (a.isCritical) VBadgeTone.Danger else VBadgeTone.Warning)
        }
    }
}

@Composable
private fun MilestoneRow(m: AcademicMilestone) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(m.day, style = VTheme.type.dataLg.colored(c.ink))
                Text(m.month.uppercase(), style = VTheme.type.label.colored(c.ink3))
            }
            Column(Modifier.weight(1f)) {
                Text(m.title, style = VTheme.type.h4.colored(c.ink))
                Text(m.description, style = VTheme.type.caption.colored(c.ink3))
            }
            if (m.isVerified) VBadge(text = "VERIFIED", tone = VBadgeTone.Success)
        }
    }
}
