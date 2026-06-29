package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels
import com.littlebridge.enrollplus.feature.reportcard.presentation.ParentReportViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * ParentReportScreen — replaces the AiReportCardPreview placeholder.
 * Shows published AI report cards for the parent's child, with a
 * conference pack summary for PTM meetings.
 */
@Composable
fun ParentReportScreen(
    childId: String,
    onBack: () -> Unit,
    viewModel: ParentReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors

    LaunchedEffect(childId) {
        viewModel.loadReports(childId)
        viewModel.loadConferencePack(childId)
    }

    Column(
        Modifier.fillMaxSize().background(c.background),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VButton(text = "Back", onClick = onBack, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
            Text("AI Report Card", style = VTheme.type.h3.colored(c.ink))
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(VIcons.AlertCircle, contentDescription = null, tint = c.danger, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, style = VTheme.type.body.colored(c.ink2))
                    }
                }
            }
            state.isEmpty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(VIcons.Sparkles, contentDescription = null, tint = c.accent, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No published reports yet", style = VTheme.type.body.colored(c.ink2))
                        Text("Reports will appear here once published by the school.",
                            style = VTheme.type.caption.colored(c.ink3))
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Conference Pack card
                    state.conferencePack?.let { pack ->
                        item { ConferencePackCard(pack) }
                    }

                    // Published reports list
                    items(state.reports) { report ->
                        ReportCardItem(report) { viewModel.selectReport(report) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConferencePackCard(pack: ReportCardModels.ConferencePack) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                Text("Conference Pack", style = VTheme.type.h3.colored(c.ink).copy(fontSize = 16.sp))
            }
            Text("${pack.studentName} — ${pack.className} ${pack.section} • ${pack.term}",
                style = VTheme.type.body.colored(c.ink2))

            // Quick stats row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pack.overallPct?.let {
                    StatChip("Overall", "${it.toInt()}%")
                }
                pack.overallGrade?.let {
                    StatChip("Grade", it)
                }
                pack.attendancePct?.let {
                    StatChip("Attendance", "$it%")
                }
            }

            if (pack.parentSummary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Summary", style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.Bold))
                Text(pack.parentSummary, style = VTheme.type.body.colored(c.ink2))
            }

            if (pack.focusAreas.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Focus Areas", style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.Bold))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pack.focusAreas.forEach { fa ->
                        VBadge(text = fa, tone = VBadgeTone.Warning)
                    }
                }
            }

            if (pack.strengths.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Strengths", style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.Bold))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pack.strengths.forEach { s ->
                        VBadge(text = s, tone = VBadgeTone.Success)
                    }
                }
            }

            if (pack.conferenceTips.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Conference Tips", style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.Bold))
                pack.conferenceTips.forEach { tip ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", style = VTheme.type.body.colored(c.accent))
                        Text(tip, style = VTheme.type.body.colored(c.ink2), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    val c = VTheme.colors
    Column(
        Modifier.clip(RoundedCornerShape(8.dp)).background(c.accent.copy(alpha = 0.08f)).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = VTheme.type.h3.colored(c.accent).copy(fontSize = 16.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

@Composable
private fun ReportCardItem(report: ReportCardModels.ParentReport, onClick: () -> Unit) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(report.term, style = VTheme.type.h3.colored(c.ink).copy(fontSize = 15.sp))
                VBadge(text = "Published", tone = VBadgeTone.Success)
            }
            Text("${report.className} ${report.section}", style = VTheme.type.caption.colored(c.ink2))
            report.publishedAt?.let {
                Text("Published: $it", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
            }
        }
    }
}
