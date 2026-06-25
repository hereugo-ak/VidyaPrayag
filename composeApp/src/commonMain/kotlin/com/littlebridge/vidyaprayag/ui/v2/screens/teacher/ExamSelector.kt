package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentType
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

// ─────────────────────────────────────────────────────────────────────────────
// P3-T3 — Exam Selector
//
// A horizontal LazyRow of exam chips above the marks list: an "All" chip first,
// each exam chip (name + date), and a trailing outlined "+ Add Exam" chip that
// opens AddExamSheet. Selecting an exam swaps the marks column the host shows.
//
// Selected chip → Enroll.colors.primary; "Add" chip → primaryMid hairline outline.
// All tokens via Enroll.* (violet primary stands in for PrimaryIndigo, IMPORTANT NOTE).
// ─────────────────────────────────────────────────────────────────────────────

/** A new-exam form result, handed to the host VM's create path. */
data class NewExamDraft(
    val name: String,
    val date: String,             // "YYYY-MM-DD" (blank → server default)
    val maxMarks: Int,
    val type: String,             // an AssessmentType constant
)

/** Friendly labels for the three exam types the sheet offers. */
private val examTypeChoices = listOf(
    AssessmentType.SCHEDULED to "Unit Test",
    AssessmentType.EXAM to "Term",
    AssessmentType.ASSIGNMENT to "Assignment",
)

/**
 * ExamSelector — the exam chip row.
 *
 * @param exams           the class+subject's assessments (newest first is fine)
 * @param selectedExamId  null = "All" view (distribution + per-exam columns hidden)
 * @param onSelectAll     tap "All"
 * @param onSelectExam    tap a specific exam
 * @param onAddExam       tap "+ Add Exam" → open the sheet
 */
@Composable
fun ExamSelector(
    exams: List<AssessmentDto>,
    selectedExamId: String?,
    onSelectAll: () -> Unit,
    onSelectExam: (String) -> Unit,
    onAddExam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Enroll.space.sm),
        contentPadding = PaddingValues(horizontal = Enroll.space.lg),
    ) {
        item(key = "all") {
            ExamChip(
                title = "All",
                subtitle = null,
                selected = selectedExamId == null,
                onClick = onSelectAll,
            )
        }
        items(exams, key = { it.id }) { exam ->
            ExamChip(
                title = exam.name,
                subtitle = exam.examDate,
                selected = exam.id == selectedExamId,
                onClick = { onSelectExam(exam.id) },
            )
        }
        item(key = "add") {
            AddExamChip(onClick = onAddExam)
        }
    }
}

@Composable
private fun ExamChip(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ix = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) Enroll.colors.primary else Enroll.colors.surfaceSubtle,
        label = "examBg",
    )
    val titleColor = if (selected) Enroll.colors.onPrimary else Enroll.colors.textPrimary
    val subColor = if (selected) Enroll.colors.onPrimary else Enroll.colors.textTertiary

    Column(
        modifier = Modifier
            .pressScale(ix)
            .clip(Enroll.shape.pill)
            .background(bg)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
    ) {
        Text(text = title, style = Enroll.type.labelBold.colored(titleColor))
        if (!subtitle.isNullOrBlank()) {
            Text(text = subtitle, style = Enroll.type.bodySmall.colored(subColor))
        }
    }
}

@Composable
private fun AddExamChip(onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Text(
        text = "+ Add Exam",
        style = Enroll.type.labelBold.colored(Enroll.colors.primaryMid),
        modifier = Modifier
            .pressScale(ix)
            .clip(Enroll.shape.pill)
            .border(1.dp, Enroll.colors.primaryMid, Enroll.shape.pill)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
    )
}

/**
 * AddExamSheet — the create-exam form, opened from "+ Add Exam".
 *
 * @param visible   show/hide
 * @param onDismiss close without saving
 * @param onSave    emit the draft (host VM validates + persists)
 */
@Composable
fun AddExamSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (NewExamDraft) -> Unit,
) {
    if (!visible) return

    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var maxMarks by remember { mutableStateOf("100") }
    var type by remember { mutableStateOf(AssessmentType.SCHEDULED) }

    val canSave = name.isNotBlank() && (maxMarks.toIntOrNull() ?: 0) > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Enroll.shape.sheet)
                .background(Enroll.colors.surfaceBase)
                .padding(horizontal = Enroll.space.lg)
                .padding(bottom = Enroll.space.xxl),
        ) {
            // Handle bar
            Spacer(Modifier.height(Enroll.space.md))
            SectionHeader(title = "ADD EXAM")
            Spacer(Modifier.height(Enroll.space.lg))

            VInput(
                value = name,
                onValueChange = { name = it },
                label = "Exam name",
                placeholder = "e.g. Mid-Term Examination",
            )
            Spacer(Modifier.height(Enroll.space.md))
            VInput(
                value = date,
                onValueChange = { date = it },
                label = "Date",
                placeholder = "YYYY-MM-DD",
            )
            Spacer(Modifier.height(Enroll.space.md))
            VInput(
                value = maxMarks,
                onValueChange = { maxMarks = it.filter { ch -> ch.isDigit() } },
                label = "Maximum marks",
                placeholder = "100",
                keyboardType = KeyboardType.Number,
            )

            Spacer(Modifier.height(Enroll.space.lg))
            Text(
                text = "TYPE",
                style = Enroll.type.labelCaps.colored(Enroll.colors.textTertiary),
            )
            Spacer(Modifier.height(Enroll.space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Enroll.space.sm)) {
                examTypeChoices.forEach { (value, label) ->
                    ExamTypeChip(
                        label = label,
                        selected = type == value,
                        onClick = { type = value },
                    )
                }
            }

            Spacer(Modifier.height(Enroll.space.xxl))
            VButton(
                text = "Save exam",
                onClick = {
                    onSave(
                        NewExamDraft(
                            name = name.trim(),
                            date = date.trim(),
                            maxMarks = maxMarks.toIntOrNull() ?: 100,
                            type = type,
                        ),
                    )
                },
                full = true,
                enabled = canSave,
            )
        }
    }
}

@Composable
private fun ExamTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) Enroll.colors.primary else Enroll.colors.surfaceSubtle,
        label = "typeBg",
    )
    val fg = if (selected) Enroll.colors.onPrimary else Enroll.colors.textSecondary
    Text(
        text = label,
        style = Enroll.type.labelBold.colored(fg),
        modifier = Modifier
            .pressScale(ix)
            .clip(Enroll.shape.pill)
            .background(bg)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
    )
}
