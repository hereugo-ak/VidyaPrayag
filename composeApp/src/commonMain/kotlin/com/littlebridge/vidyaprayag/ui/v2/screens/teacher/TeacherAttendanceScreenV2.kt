package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.AttendanceStatus
import com.littlebridge.vidyaprayag.feature.teacher.presentation.StudentAttendance
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherAttendanceViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VDatePicker
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherAttendanceScreenV2 — the scoped attendance plane (Doc 06 §3). Reached PRE-SCOPED with a
 * pre-authorized [assignmentId] from the Update scope gate or a Home/Classes CTA. It loads the typed
 * roster, defaults the date to today (correctable), pre-sets approved-leave students to "leave"
 * (locked), supports the 4-state space (present · absent · late · leave), a bulk "mark all present",
 * a live running counter, and a result-driven Save that NEVER auto-publishes.
 */
@Composable
fun TeacherAttendanceScreenV2(
    assignmentId: String,
    scopeLabel: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherAttendanceViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) viewModel.load(assignmentId)
    }

    Box(modifier.fillMaxSize().background(c.background)) {
        when {
            state.isLoading && state.students.isEmpty() -> TeacherCenterState { TeacherSpinner() }
            state.error != null && state.students.isEmpty() -> TeacherCenterState {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load attendance", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(6.dp))
                    Text(state.error ?: "", style = VTheme.type.body.colored(c.ink2), maxLines = 3)
                    Spacer(Modifier.height(14.dp))
                    VButton("Retry", onClick = { viewModel.retry() }, tone = VButtonTone.Lavender)
                }
            }
            else -> AttendanceBody(state.students, viewModel, scopeLabel)
        }
    }
}

@Composable
private fun AttendanceBody(
    students: List<StudentAttendance>,
    viewModel: TeacherAttendanceViewModel,
    scopeLabel: String,
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Scope + date + running counter header ──
        item {
            TCard(padding = 16.dp) {
                Column {
                    TEyebrow("MARKING ATTENDANCE", dot = c.accent)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        scopeLabel.ifBlank { "${state.className}-${state.section} · ${state.subject}" },
                        style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Spacer(Modifier.height(12.dp))
                    VDatePicker(
                        value = state.date,
                        onValueChange = { viewModel.changeDate(it) },
                        label = "Date",
                    )
                    if (state.isHoliday || state.isCancelled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.isHoliday) "This is a holiday${state.holidayName?.let { " — $it" } ?: ""}." else "This class is cancelled on this date.",
                            style = VTheme.type.caption.colored(c.warningInk).copy(fontSize = 12.sp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TMetricTile(state.presentCount.toString(), "Present", c.successInk, Modifier.weight(1f))
                        TMetricTile(state.absentCount.toString(), "Absent", c.dangerInk, Modifier.weight(1f))
                        TMetricTile(state.lateCount.toString(), "Late", c.warningInk, Modifier.weight(1f))
                        TMetricTile(state.leaveCount.toString(), "Leave", c.navy, Modifier.weight(1f))
                    }
                    if (state.alreadyMarked && state.lastMarkedBy != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Last marked by ${state.lastMarkedBy}${state.lastMarkedAt?.let { " · ${prettyDate(it.take(10))}" } ?: ""}",
                            style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    VButton(
                        text = "Mark all present",
                        onClick = { viewModel.markAllPresent() },
                        full = true,
                        variant = VButtonVariant.Secondary,
                        tone = VButtonTone.Mint,
                        size = VButtonSize.Md,
                        leading = { Icon(VIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    )
                }
            }
        }

        items(students, key = { it.studentId }) { s ->
            AttendanceStudentRow(s, onSetStatus = { status -> viewModel.setStatus(s.studentId, status) })
        }

        // ── Save footer ──
        item {
            Spacer(Modifier.height(4.dp))
            if (state.saveError != null) {
                Text(state.saveError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp))
                Spacer(Modifier.height(8.dp))
            }
            VButton(
                text = if (state.alreadyMarked) "Update attendance" else "Save attendance",
                onClick = { viewModel.save() },
                full = true,
                tone = VButtonTone.Lavender,
                size = VButtonSize.Lg,
                loading = state.isSaving,
                success = state.saveSuccess,
                successLabel = "Saved",
                stateful = true,
                enabled = students.isNotEmpty() && !state.isHoliday,
            )
        }
    }
}

@Composable
private fun AttendanceStudentRow(s: StudentAttendance, onSetStatus: (String) -> Unit) {
    val c = VTheme.colors
    val locked = s.isOnApprovedLeave
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.hairline, RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VAvatar(name = s.name, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(
                    if (locked) "Roll ${s.rollNo} · On approved leave" else "Roll ${s.rollNo}",
                    style = VTheme.type.caption.colored(if (locked) c.navy else c.ink3).copy(fontSize = 11.sp),
                )
            }
        }
        // The 4-state segmented control sits on its own line under the identity for tap comfort.
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip("P", AttendanceStatus.PRESENT, s.status, c.successInk, locked, onSetStatus, Modifier.weight(1f))
            StatusChip("A", AttendanceStatus.ABSENT, s.status, c.dangerInk, locked, onSetStatus, Modifier.weight(1f))
            StatusChip("Late", AttendanceStatus.LATE, s.status, c.warningInk, locked, onSetStatus, Modifier.weight(1f))
            StatusChip("Leave", AttendanceStatus.LEAVE, s.status, c.navy, locked, onSetStatus, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: String,
    current: String,
    tint: androidx.compose.ui.graphics.Color,
    locked: Boolean,
    onSet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val active = current == status
    val ix = remember { MutableInteractionSource() }
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) tint.copy(alpha = 0.16f) else c.cream)
            .border(1.dp, if (active) tint.copy(alpha = 0.5f) else c.hairline, RoundedCornerShape(12.dp))
            .clickable(interactionSource = ix, indication = null, enabled = !locked) { onSet(status) }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = VTheme.type.bodyStrong.colored(if (active) tint else c.ink2)
                .copy(fontSize = 12.5.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium),
        )
    }
}
