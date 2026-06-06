package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.StudentMark
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherMarksViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherMarksScreenV2 — the Marks sub-tab of Teacher.tsx → Update.
 *
 * Loads the mark sheet for [classId] + [examId], lets the teacher type a per-student score
 * (clamped 0..maxMarks by the VM), shows entered/total progress, and submits. Bound to
 * [TeacherMarksViewModel].
 */
@Composable
fun TeacherMarksScreenV2(
    classId: String,
    examId: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherMarksViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(classId, examId) { viewModel.load(classId, examId) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.sm),
    ) {
        Text(state.examName.ifBlank { "Marks" }, style = VTheme.type.h2.colored(c.ink))
        Text(
            "${state.className} · ${state.subject} · /${state.maxMarks}",
            style = VTheme.type.caption.colored(c.ink3),
        )
        Text(
            "${state.enteredCount}/${state.students.size} entered",
            style = VTheme.type.dataSm.colored(if (state.allEntered) c.successInk else c.ink2),
        )

        if (state.students.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No mark sheet", icon = VIcons.Target, body = "No students/exam found for this selection.")
        } else {
            state.students.forEach { MarkRow(it, state.maxMarks, viewModel::setMark) }
        }

        if (state.error != null) Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))

        Spacer(Modifier.height(d.sm))
        VButton(
            text = if (state.submitSuccess) "Submitted" else "Submit marks",
            onClick = viewModel::submit,
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            soft = false,
            loading = state.isSubmitting,
            enabled = state.enteredCount > 0,
        )
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun MarkRow(s: StudentMark, maxMarks: Int, onSet: (String, Float?) -> Unit) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.h4.colored(c.ink))
                Text("Roll ${s.rollNo}", style = VTheme.type.dataSm.colored(c.ink3))
            }
            VInput(
                value = s.marks?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "",
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    onSet(s.studentId, if (clean.isBlank()) null else clean.toFloatOrNull())
                },
                placeholder = "/$maxMarks",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}
