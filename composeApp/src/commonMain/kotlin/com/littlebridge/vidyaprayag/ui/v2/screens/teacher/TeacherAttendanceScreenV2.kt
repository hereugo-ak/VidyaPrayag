package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

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
import com.littlebridge.vidyaprayag.feature.teacher.presentation.AttendanceStatus
import com.littlebridge.vidyaprayag.feature.teacher.presentation.StudentAttendance
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherAttendanceViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherAttendanceScreenV2 — the Attendance sub-tab of Teacher.tsx → Update.
 *
 * Loads a roster for [classId] + [date], lets the teacher tap a per-student status chip
 * (present/absent/late), offers "Mark all present", shows live present/absent/late counts, and
 * submits via the immutable attendance write. Bound to [TeacherAttendanceViewModel].
 */
@Composable
fun TeacherAttendanceScreenV2(
    classId: String,
    date: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherAttendanceViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    androidx.compose.runtime.LaunchedEffect(classId, date) { viewModel.load(classId, date) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.sm),
    ) {
        Text(state.className.ifBlank { "Attendance" }, style = VTheme.type.h2.colored(c.ink))
        Text(state.date, style = VTheme.type.dataSm.colored(c.ink3))

        // Live tallies
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            Tally("Present", state.presentCount, c.successInk, Modifier.weight(1f))
            Tally("Absent", state.absentCount, c.dangerInk, Modifier.weight(1f))
            Tally("Late", state.lateCount, c.warningInk, Modifier.weight(1f))
        }

        VButton(
            text = "Mark all present",
            onClick = { viewModel.markAll(AttendanceStatus.PRESENT) },
            variant = VButtonVariant.Ghost,
            tone = VButtonTone.Teal,
            size = VButtonSize.Sm,
        )

        if (state.students.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No roster", icon = VIcons.Users, body = "No students found for this class/date.")
        } else {
            state.students.forEach { StudentRow(it, viewModel::setStatus) }
        }

        if (state.error != null) Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))

        Spacer(Modifier.height(d.sm))
        VButton(
            text = if (state.submitSuccess) "Submitted" else "Submit attendance",
            onClick = viewModel::submit,
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            soft = false,
            loading = state.isSubmitting,
            enabled = state.students.isNotEmpty(),
        )
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun Tally(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    VCard(modifier = modifier) {
        Text(value.toString(), style = VTheme.type.dataLg.colored(color))
        Text(label, style = VTheme.type.label.colored(VTheme.colors.ink3))
    }
}

@Composable
private fun StudentRow(s: StudentAttendance, onSet: (String, String) -> Unit) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.h4.colored(c.ink))
                Text("Roll ${s.rollNo}", style = VTheme.type.dataSm.colored(c.ink3))
            }
            VTag(text = "P", active = s.status == AttendanceStatus.PRESENT, onClick = { onSet(s.studentId, AttendanceStatus.PRESENT) })
            VTag(text = "A", active = s.status == AttendanceStatus.ABSENT, onClick = { onSet(s.studentId, AttendanceStatus.ABSENT) })
            VTag(text = "L", active = s.status == AttendanceStatus.LATE, onClick = { onSet(s.studentId, AttendanceStatus.LATE) })
        }
    }
}
