package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlebridge.vidyaprayag.feature.teacher.presentation.GradebookStudentMark
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// P3-T4 — Student Marks List
//
// The fast-entry grid: one EnrollCard row per student with a pill mark field that
// debounce-auto-saves (800ms, no button), an auto-derived grade chip, and a trend
// arrow vs the previous same-type exam. Tap the ROW (not the field) → expand the
// StudentMarkDetailSheet (history line + remark + Message Parent).
//
// All colour/type/shape/space via Enroll.*; status palette drives grade + trend.
// ─────────────────────────────────────────────────────────────────────────────

/** The auto-save lifecycle of a single mark field. */
enum class MarkSaveState { Idle, Saving, Saved }

/** Direction of a student's mark vs their previous same-type exam. */
enum class MarkTrend { Up, Down, Flat, None }

/** Map a percentage onto a letter grade (standard 90/75/60/40 bands). */
fun gradeForPercent(percent: Float): String = when {
    percent >= 90f -> "A"
    percent >= 75f -> "B"
    percent >= 60f -> "C"
    percent >= 40f -> "D"
    else -> "F"
}

/**
 * StudentMarkRow — one editable student row.
 *
 * @param student      the live local edit (from the gradebook VM)
 * @param maxMarks     the active exam's max (for /N + grade)
 * @param trend        vs previous same-type exam (host computes)
 * @param saveState    auto-save indicator state (host flips Saving→Saved)
 * @param onMarkChanged debounced-clamped value (null = cleared); fired after 800ms idle
 * @param onOpenDetail tap the row → open the detail sheet
 */
@Composable
fun StudentMarkRow(
    student: GradebookStudentMark,
    maxMarks: Int,
    trend: MarkTrend,
    saveState: MarkSaveState,
    onMarkChanged: (Float?) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EnrollCard(modifier = modifier.fillMaxWidth(), onClick = onOpenDetail) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Roll number
            Text(
                text = student.rollNo.ifBlank { "—" },
                style = Enroll.type.labelBold.colored(Enroll.colors.textTertiary),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp),
            )
            Spacer(Modifier.width(Enroll.space.md))

            // Name + auto-save indicator
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = Enroll.type.bodyLarge.colored(Enroll.colors.textPrimary),
                )
                AutoSaveHint(saveState)
            }

            // Grade chip (from current mark)
            val markValue = student.marks
            if (markValue != null && maxMarks > 0 && !student.isAbsent) {
                GradeChip(grade = gradeForPercent(markValue / maxMarks * 100f))
                Spacer(Modifier.width(Enroll.space.sm))
            }

            // Trend arrow
            TrendArrow(trend)
            Spacer(Modifier.width(Enroll.space.sm))

            // Mark entry pill
            MarkField(
                value = markValue,
                isAbsent = student.isAbsent,
                onValueChanged = onMarkChanged,
            )
        }
    }
}

@Composable
private fun MarkField(
    value: Float?,
    isAbsent: Boolean,
    onValueChanged: (Float?) -> Unit,
) {
    val initial = remember(value) {
        value?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: ""
    }
    var text by remember(value) { mutableStateOf(initial) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    // Debounce: 800ms after the last keystroke, push the parsed value up.
    LaunchedEffect(text) {
        if (text == initial) return@LaunchedEffect
        delay(800)
        onValueChanged(text.trim().toFloatOrNull())
    }

    val borderColor = if (focused) Enroll.colors.primary else Enroll.colors.surfaceSubtle
    val borderWidth = if (focused) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(40.dp)
            .clip(Enroll.shape.chip)
            .background(Enroll.colors.surfaceSubtle)
            .border(borderWidth, borderColor, Enroll.shape.chip),
        contentAlignment = Alignment.Center,
    ) {
        if (isAbsent) {
            Text("AB", style = Enroll.type.dataMedium.colored(Enroll.colors.textTertiary))
        } else {
            BasicTextField(
                value = text,
                onValueChange = { raw -> text = raw.filter { it.isDigit() || it == '.' } },
                singleLine = true,
                textStyle = Enroll.type.dataMedium.colored(Enroll.colors.textPrimary)
                    .copy(textAlign = TextAlign.Center),
                cursorBrush = SolidColor(Enroll.colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AutoSaveHint(state: MarkSaveState) {
    val label = when (state) {
        MarkSaveState.Saving -> "saving…"
        MarkSaveState.Saved -> "✓ saved"
        MarkSaveState.Idle -> null
    }
    AnimatedVisibility(visible = label != null) {
        Text(
            text = label ?: "",
            style = Enroll.type.bodySmall.colored(
                if (state == MarkSaveState.Saved) Enroll.colors.statusPresent else Enroll.colors.textTertiary,
            ),
        )
    }
}

@Composable
private fun GradeChip(grade: String) {
    val color = when (grade) {
        "A", "B" -> Enroll.colors.statusPresent
        "C", "D" -> Enroll.colors.statusLate
        else -> Enroll.colors.statusAbsent
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(Enroll.shape.pill)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = grade, style = Enroll.type.labelBold.colored(color))
    }
}

@Composable
private fun TrendArrow(trend: MarkTrend) {
    when (trend) {
        MarkTrend.Up -> Icon(
            VIcons.TrendingUp, contentDescription = "Improved",
            tint = Enroll.colors.statusPresent, modifier = Modifier.size(16.dp),
        )
        MarkTrend.Down -> Icon(
            VIcons.TrendingUp, contentDescription = "Declined",
            tint = Enroll.colors.statusAbsent, modifier = Modifier.size(16.dp).rotate(180f),
        )
        MarkTrend.Flat -> Box(
            modifier = Modifier
                .width(12.dp)
                .height(2.dp)
                .background(Enroll.colors.textTertiary),
        )
        MarkTrend.None -> Spacer(Modifier.size(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StudentMarkDetailSheet — expanded per-student view
// ─────────────────────────────────────────────────────────────────────────────

/** One point in a student's exam-history line (percentage 0..100). */
data class ExamScorePoint(
    val examLabel: String,
    val percent: Float,
)

/**
 * StudentMarkDetailSheet — the row's expanded detail.
 *
 * @param visible      show/hide
 * @param studentName  for the header + avatar
 * @param history      exam-score history (oldest→newest) for the line chart
 * @param remark       current teacher note
 * @param onRemarkChange edit the note
 * @param onMessageParent jump to this student's parent chat thread (P7-T3). The host
 *                        builds the destination from this student's id via
 *                        [TeacherNavRouter.messageParentDestination]; nothing here
 *                        needs to know about navigation, keeping the sheet pure.
 * @param onDismiss    close the sheet
 */
@Composable
fun StudentMarkDetailSheet(
    visible: Boolean,
    studentName: String,
    history: List<ExamScorePoint>,
    remark: String,
    onRemarkChange: (String) -> Unit,
    onMessageParent: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

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
            Spacer(Modifier.height(Enroll.space.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VAvatar(name = studentName, size = 44.dp)
                Spacer(Modifier.width(Enroll.space.md))
                Text(
                    text = studentName,
                    style = Enroll.type.headingSmall.colored(Enroll.colors.textPrimary),
                )
            }

            Spacer(Modifier.height(Enroll.space.lg))
            SectionHeader(title = "SCORE HISTORY")
            Spacer(Modifier.height(Enroll.space.md))
            MarksHistoryChart(history)

            Spacer(Modifier.height(Enroll.space.lg))
            VInput(
                value = remark,
                onValueChange = onRemarkChange,
                label = "Remark (optional)",
                placeholder = "A private note for this student",
                singleLine = false,
            )

            Spacer(Modifier.height(Enroll.space.xxl))
            VButton(
                text = "Message Parent",
                onClick = onMessageParent,
                full = true,
                leading = {
                    Icon(VIcons.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

/** A minimal Canvas line chart over a student's exam percentages. */
@Composable
private fun MarksHistoryChart(points: List<ExamScorePoint>) {
    val line = Enroll.colors.primary
    val grid = Enroll.colors.surfaceSubtle
    if (points.size < 2) {
        Text(
            text = "Not enough exams yet to chart a trend.",
            style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
        )
        return
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(Enroll.shape.card)
            .background(grid)
            .padding(Enroll.space.md),
    ) {
        val w = size.width
        val h = size.height
        val maxPercent = 100f
        val stepX = if (points.size > 1) w / (points.size - 1) else w
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = stepX * i
            val y = h - (p.percent.coerceIn(0f, maxPercent) / maxPercent) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = line,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // end dot
        val last = points.last()
        val lx = stepX * (points.size - 1)
        val ly = h - (last.percent.coerceIn(0f, maxPercent) / maxPercent) * h
        drawCircle(color = line, radius = 4.dp.toPx(), center = Offset(lx, ly))
    }
}
