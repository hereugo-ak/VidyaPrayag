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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StudentProfileDto
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentProfileUiState
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * RA-45: StudentProfileScreenV2 — a single student's record (attendance / marks /
 * leave / fees) for the admin. The [studentId] is passed by the caller (portal
 * overlay) and loaded via [StudentProfileViewModel.load] in a LaunchedEffect.
 * Three states via [VStateHost] (LAW 3). Portal overlay — back returns to roster.
 */
@Composable
fun StudentProfileScreenV2(
    studentId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StudentProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    LaunchedEffect(studentId) { viewModel.load(studentId) }

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Student", onBack = onBack)
        StudentProfileContent(
            state = state,
            onRetry = viewModel::retry,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StudentProfileContent(
    state: StudentProfileUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.profile == null && !state.isLoading && state.error == null,
            emptyTitle = "No profile",
            emptyBody = "This student's record could not be found.",
            emptyIcon = VIcons.User,
            onRetry = onRetry,
        ) {
            val p = state.profile ?: return@VStateHost
            StudentProfileBody(p)
        }
    }
}

@Composable
private fun StudentProfileBody(p: StudentProfileDto) {
    val c = VTheme.colors
    val s = p.student

    // ── Header card ─────────────────────────────────────────────────────────
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VAvatar(name = s.fullName, src = s.profilePhotoUrl, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text(s.fullName, style = VTheme.type.h3.colored(c.ink))
                Text(
                    "${s.className} · Sec ${s.section} · Roll ${s.rollNumber}",
                    style = VTheme.type.caption.colored(c.ink2),
                )
                Text("Code ${s.studentCode}", style = VTheme.type.label.colored(c.ink3))
            }
        }
    }

    // ── Attendance ────────────────────────────────────────────────────────────
    VSectionHeader(title = "ATTENDANCE")
    VCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Attendance rate", style = VTheme.type.body.colored(c.ink))
            Text("${p.attendanceRate}%", style = VTheme.type.dataSm.colored(c.ink2))
        }
        Spacer(Modifier.height(4.dp))
        VProgressBar(
            value = p.attendanceRate.toFloat(),
            tone = if (p.attendanceRate < 75) VBadgeTone.Warning else VBadgeTone.Success,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Present", p.presentDays, Modifier.weight(1f))
            StatPill("Absent", p.absentDays, Modifier.weight(1f))
            StatPill("Late", p.lateDays, Modifier.weight(1f))
        }
    }

    // ── Marks ──────────────────────────────────────────────────────────────────
    VSectionHeader(title = "MARKS")
    if (p.marks.isEmpty()) {
        VCard { Text("No marks recorded yet.", style = VTheme.type.body.colored(c.ink2)) }
    } else {
        p.marks.forEach { m ->
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("${m.subject} · ${m.assessmentName}", style = VTheme.type.bodyStrong.colored(c.ink))
                        m.examDate?.let { Text(it, style = VTheme.type.label.colored(c.ink3)) }
                    }
                    Text(
                        "${m.marks?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "—"} / ${m.maxMarks}",
                        style = VTheme.type.dataSm.colored(c.ink),
                    )
                }
            }
        }
    }

    // ── Leave ────────────────────────────────────────────────────────────────
    VSectionHeader(title = "LEAVE")
    if (p.leave.isEmpty()) {
        VCard { Text("No leave applications.", style = VTheme.type.body.colored(c.ink2)) }
    } else {
        p.leave.forEach { l ->
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${l.dateFrom} → ${l.dateTo}", style = VTheme.type.bodyStrong.colored(c.ink))
                        Text(l.reason, style = VTheme.type.caption.colored(c.ink2))
                    }
                    VBadge(
                        text = l.status,
                        tone = when (l.status.lowercase()) {
                            "approved" -> VBadgeTone.Success
                            "rejected" -> VBadgeTone.Danger
                            else -> VBadgeTone.Warning
                        },
                    )
                }
            }
        }
    }

    // ── Fees ───────────────────────────────────────────────────────────────────
    VSectionHeader(title = "FEES")
    if (p.fees.isEmpty()) {
        VCard { Text("No fee records.", style = VTheme.type.body.colored(c.ink2)) }
    } else {
        p.fees.forEach { f ->
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(f.title, style = VTheme.type.bodyStrong.colored(c.ink))
                        f.dueDate?.let { Text("Due $it", style = VTheme.type.label.colored(c.ink3)) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${f.currency} ${if (f.amount % 1.0 == 0.0) f.amount.toInt().toString() else f.amount.toString()}",
                            style = VTheme.type.dataSm.colored(c.ink),
                        )
                        VBadge(
                            text = f.status,
                            tone = when (f.status.uppercase()) {
                                "PAID" -> VBadgeTone.Success
                                "OVERDUE" -> VBadgeTone.Danger
                                else -> VBadgeTone.Warning
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: Int, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Column {
            Text(value.toString(), style = VTheme.type.dataSm.colored(c.ink))
            Text(label, style = VTheme.type.label.colored(c.ink3))
        }
    }
}
