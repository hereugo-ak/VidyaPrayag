package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAcademicsState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAcademicsViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressState
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
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

/**
 * ParentAcademicsScreenV2 — a faithful copy of `Parent.tsx → Academics`.
 *
 * Top tabs: Overview · Attendance · Marks · Syllabus · Report. **Overview is wired to the real
 * [TrackProgressViewModel]** (`shared/`) → `GET /api/v1/parent/track-progress` and renders the
 * child's academic competencies + emotional-intelligence radar from genuine backend data.
 *
 * RA-43 + RA-56: Attendance / Marks / Syllabus are now wired to REAL child-scoped endpoints
 * (`GET /api/v1/parent/child/{id}/{attendance,marks,syllabus}`) via [ParentAcademicsViewModel], and a
 * child chip selector at the top switches which child every tab reads (RA-56). Each tab carries the
 * three states (loading / error / empty) through [VStateHost]. Report stays an honest preview.
 */
@Composable
fun ParentAcademicsScreenV2(
    modifier: Modifier = Modifier,
    onOpenLeave: () -> Unit = {},
    viewModel: TrackProgressViewModel = koinViewModel(),
    academicsViewModel: ParentAcademicsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val academics by academicsViewModel.state.collectAsStateV2()
    ParentAcademicsContent(
        state = state,
        academics = academics,
        onSelectChild = academicsViewModel::selectChild,
        onLoadAttendance = { academicsViewModel.loadAttendance() },
        onLoadMarks = { academicsViewModel.loadMarks() },
        onLoadSyllabus = { academicsViewModel.loadSyllabus() },
        onOpenLeave = onOpenLeave,
        modifier = modifier,
    )
}

/** Stateless body — also used by @Preview with seeded state (no MockV2 in the live path). */
@Composable
private fun ParentAcademicsContent(
    state: TrackProgressState,
    academics: ParentAcademicsState,
    onSelectChild: (String) -> Unit,
    onLoadAttendance: () -> Unit,
    onLoadMarks: () -> Unit,
    onLoadSyllabus: () -> Unit,
    onOpenLeave: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    var tab by remember { mutableStateOf("Overview") }

    // RA-56: each tab pulls fresh data for the selected child the first time it's
    // opened (and whenever the child changes — the VM clears stale data on switch).
    LaunchedEffect(tab, academics.selectedChildId) {
        when (tab) {
            "Attendance" -> if (academics.attendance == null && !academics.attendanceLoading) onLoadAttendance()
            "Marks" -> if (academics.marks == null && !academics.marksLoading) onLoadMarks()
            "Syllabus" -> if (academics.syllabus == null && !academics.syllabusLoading) onLoadSyllabus()
        }
    }

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

        // ── RA-44: apply-for-leave entry ─────────────────────────────────────
        VCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(onClick = onOpenLeave),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Apply for leave", style = VTheme.type.bodyStrong.colored(c.ink))
                    Text(
                        "Request leave for your child — routed to their class teacher",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
                Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        // RA-56: child switcher — only shown for a multi-child parent. Single-child
        // parents see no chrome; zero children is handled by the empty tabs below.
        if (academics.children.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                academics.children.forEach { child ->
                    val selected = child.id == (academics.selectedChild?.id)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) c.accentDeep else c.cream)
                            .border(1.dp, if (selected) c.accentDeep else c.hairline, RoundedCornerShape(20.dp))
                            .clickable { onSelectChild(child.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            child.name,
                            style = VTheme.type.body.colored(if (selected) c.card else c.ink),
                        )
                    }
                }
            }
        }

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
                "Attendance" -> AttendanceTab(academics, onLoadAttendance)
                "Marks" -> MarksTab(academics, onLoadMarks)
                "Syllabus" -> SyllabusTab(academics, onLoadSyllabus)
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
private fun AttendanceTab(academics: ParentAcademicsState, onRetry: () -> Unit) {
    val c = VTheme.colors
    val data = academics.attendance
    VStateHost(
        loading = academics.attendanceLoading,
        error = academics.attendanceError,
        isEmpty = data != null && data.totalDays == 0,
        emptyTitle = "No attendance yet",
        emptyBody = "Daily attendance will appear here once the school marks it.",
        onRetry = onRetry,
    ) {
        if (data != null) {
            VCard {
                VLabel("This term")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Attendance rate", style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("${data.attendanceRate}%", style = VTheme.type.data.colored(c.ink))
                }
                Spacer(Modifier.height(8.dp))
                VProgressBar(value = data.attendanceRate.toFloat(), tone = VBadgeTone.Success)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Present ${data.presentDays}", style = VTheme.type.caption.colored(c.ink2))
                    Text("Late ${data.lateDays}", style = VTheme.type.caption.colored(c.ink2))
                    Text("Absent ${data.absentDays}", style = VTheme.type.caption.colored(c.ink2))
                }
            }
            // RA-S19: month-grid calendar (was a flat list of date rows). Each day cell
            // is colour-coded by status; the month can be paged. See ParentAttendanceCalendar.
            ParentAttendanceCalendar(records = data.records)
        }
    }
}

@Composable
private fun MarksTab(academics: ParentAcademicsState, onRetry: () -> Unit) {
    val c = VTheme.colors
    val data = academics.marks
    VStateHost(
        loading = academics.marksLoading,
        error = academics.marksError,
        isEmpty = data != null && data.results.isEmpty(),
        emptyTitle = "No published marks yet",
        emptyBody = "Marks appear here once teachers publish results to parents.",
        onRetry = onRetry,
    ) {
        data?.results?.forEach { m ->
            VCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(m.examName, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text(m.subject, style = VTheme.type.caption.colored(c.ink2))
                    }
                    val marksValue = m.marks
                    Text(
                        if (marksValue != null) "${marksValue.toInt()} / ${m.maxMarks}" else "—",
                        style = VTheme.type.data.colored(c.ink),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllabusTab(academics: ParentAcademicsState, onRetry: () -> Unit) {
    val c = VTheme.colors
    val data = academics.syllabus
    VStateHost(
        loading = academics.syllabusLoading,
        error = academics.syllabusError,
        isEmpty = data != null && data.subjects.isEmpty(),
        emptyTitle = "No syllabus shared yet",
        emptyBody = "A subject-wise coverage log will appear here once the school shares it.",
        onRetry = onRetry,
    ) {
        data?.subjects?.forEach { subj ->
            VCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(subj.subject, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("${subj.progress}%", style = VTheme.type.data.colored(c.ink))
                }
                Spacer(Modifier.height(8.dp))
                VProgressBar(value = subj.progress.toFloat(), tone = VBadgeTone.Arctic)
                Spacer(Modifier.height(8.dp))
                subj.units.forEach { u ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(u.title, style = VTheme.type.caption.colored(if (u.isCovered) c.ink else c.ink2))
                        Text(if (u.isCovered) "✓ ${u.coveredOn ?: "Covered"}" else "Pending", style = VTheme.type.caption.colored(c.ink2))
                    }
                }
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
        isEmpty = state.academicCompetencies.isEmpty() &&
            state.emotionalIntelligence.isEmpty() && state.emotionalDescription.isBlank(),
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

        if (state.emotionalIntelligence.isNotEmpty() || state.emotionalDescription.isNotBlank()) {
            VCard {
                VLabel("Emotional intelligence")
                if (state.emotionalDescription.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.emotionalDescription,
                        style = VTheme.type.body.colored(c.ink2),
                    )
                }
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
