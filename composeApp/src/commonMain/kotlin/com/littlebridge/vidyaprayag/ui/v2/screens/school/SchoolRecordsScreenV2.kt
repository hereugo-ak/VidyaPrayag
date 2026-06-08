package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.presentation.SyllabusCoverageState
import com.littlebridge.vidyaprayag.feature.admin.presentation.SyllabusCoverageViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * SchoolRecordsScreenV2 — `Admin.tsx → Records`, wired to the real
 * [SyllabusCoverageViewModel] (`AnalyticsApi` → `GET /api/v1/syllabus-coverage`).
 *
 * The **Coverage** tab renders live department progress, lagging alerts and academic
 * milestones from the analytics endpoint. The other admin-records tabs (per-student
 * Attendance, Marks entry, Fee collection, Documents) belong to the teacher data plane
 * or to backends that don't yet exist at the admin level, so they're shown as
 * `VComingSoon` rather than fabricating data (LAW 6). No MockV2 in production; the three
 * UI states come from [VStateHost].
 */
@Composable
fun SchoolRecordsScreenV2(
    modifier: Modifier = Modifier,
    viewModel: SyllabusCoverageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    SchoolRecordsContent(
        state = state,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun SchoolRecordsContent(
    state: SyllabusCoverageState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var tab by remember { mutableStateOf("Coverage") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Records", style = VTheme.type.h1.colored(c.ink))
        VTopTabs(
            tabs = listOf("Coverage", "Attendance", "Marks", "Fee", "Documents"),
            selected = tab,
            onSelect = { tab = it },
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
                "Coverage" -> CoverageTab(state = state, onRetry = onRetry)
                "Attendance" -> VComingSoon(
                    title = "School-wide attendance register",
                    description = "Class-by-class daily attendance rolls up here once the admin attendance feed is connected.",
                )
                "Marks" -> VComingSoon(
                    title = "Exam marks & report cards",
                    description = "Consolidated marks entry and report-card generation arrive with the assessments backend.",
                )
                "Fee" -> VComingSoon(
                    title = "Fee collection records",
                    description = "Outstanding fees, collections and reminders surface here when the fees ledger endpoint is live.",
                )
                "Documents" -> VComingSoon(
                    title = "Document library",
                    description = "Circulars, timetables and holiday lists will be uploadable once media storage is configured.",
                )
            }
        }
    }
}

@Composable
private fun CoverageTab(state: SyllabusCoverageState, onRetry: () -> Unit) {
    val c = VTheme.colors
    VStateHost(
        loading = state.isLoading,
        error = state.errorMessage,
        isEmpty = state.departmentProgress.isEmpty() && state.alerts.isEmpty() && state.milestones.isEmpty(),
        emptyTitle = "No coverage data yet",
        emptyBody = "Syllabus coverage will appear here once teachers start marking units complete.",
        emptyIcon = VIcons.BookOpen,
        onRetry = onRetry,
        skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonList(rows = 5, withAvatar = false) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // ── Overall ───────────────────────────────────────────────────────
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    VLabel("Overall syllabus coverage")
                    if (state.overallTrend.isNotBlank()) {
                        VBadge(text = state.overallTrend, tone = VBadgeTone.Arctic)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${state.overallPercentage}%", style = VTheme.type.dataLg.colored(c.ink))
                Spacer(Modifier.height(8.dp))
                VProgressBar(
                    value = state.overallPercentage.toFloat(),
                    tone = if (state.overallPercentage < 70) VBadgeTone.Warning else VBadgeTone.Arctic,
                )
            }

            // ── By department ─────────────────────────────────────────────────
            if (state.departmentProgress.isNotEmpty()) {
                VCard {
                    Text("By department", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.departmentProgress.forEach { d ->
                            Column {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(d.name, style = VTheme.type.body.colored(c.ink))
                                    Text("${(d.progress * 100).roundToInt()}%", style = VTheme.type.dataSm.colored(c.ink2))
                                }
                                Spacer(Modifier.height(4.dp))
                                VProgressBar(
                                    value = d.progress * 100f,
                                    tone = if (d.isDelayed) VBadgeTone.Danger else VBadgeTone.Arctic,
                                )
                                if (d.trend.isNotBlank()) {
                                    Text(d.trend, style = VTheme.type.label.colored(if (d.isDelayed) c.dangerInk else c.ink3))
                                }
                            }
                        }
                    }
                }
            }

            // ── Lagging alerts ────────────────────────────────────────────────
            if (state.alerts.isNotEmpty()) {
                Column {
                    Text("Lagging classes", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.alerts.forEach { a ->
                            VCard {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(VIcons.AlertCircle, contentDescription = null, tint = if (a.isCritical) c.dangerInk else c.warningInk, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("${a.subject} • ${a.className}", style = VTheme.type.bodyStrong.colored(c.ink))
                                        if (a.instructor.isNotBlank()) {
                                            Text(a.instructor, style = VTheme.type.caption.colored(c.ink2))
                                        }
                                    }
                                    VBadge(
                                        text = "${a.delayPercentage}% behind",
                                        tone = if (a.isCritical) VBadgeTone.Danger else VBadgeTone.Warning,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Academic milestones ───────────────────────────────────────────
            if (state.milestones.isNotEmpty()) {
                Column {
                    Text("Academic milestones", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.milestones.forEach { m ->
                            VCard {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(m.month, style = VTheme.type.label.colored(c.ink3))
                                        Text(m.day, style = VTheme.type.dataLg.colored(c.ink))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(m.title, style = VTheme.type.bodyStrong.colored(c.ink))
                                        if (m.description.isNotBlank()) {
                                            Text(m.description, style = VTheme.type.caption.colored(c.ink2))
                                        }
                                    }
                                    if (m.isVerified) {
                                        Icon(VIcons.Check, contentDescription = "Verified", tint = c.successInk, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
