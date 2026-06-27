package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentStatus
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentType
import com.littlebridge.enrollplus.feature.teacher.presentation.GradebookMode
import com.littlebridge.enrollplus.feature.teacher.presentation.GradebookStudentMark
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherGradebookViewModel
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.util.todayIso
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherMarksScreenV2 — the scoped gradebook (Doc 07). Reached PRE-SCOPED with a pre-authorized
 * [assignmentId]. Two faces:
 *   • LIST  — the scoped assessment list (scheduled tests show their exam date; marks open only once
 *             the exam date has passed, per the directive) + an inline "Create test" composer that
 *             needs the scope (already pre-filled) before it can create.
 *   • MARKS — the dense marks grid for one assessment, with a result-driven SAVE (never publishes)
 *             and an explicit PUBLISH (the ONLY parent-notify path, behind a confirm).
 */
@Composable
fun TeacherMarksScreenV2(
    assignmentId: String,
    scopeLabel: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherGradebookViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) viewModel.load(assignmentId, scopeLabel)
    }

    Box(modifier.fillMaxSize().background(c.background)) {
        when (state.mode) {
            GradebookMode.List -> MarksListMode(viewModel, scopeLabel)
            GradebookMode.Marks -> MarksGridMode(viewModel)
            GradebookMode.History -> MarksListMode(viewModel, scopeLabel) // history not surfaced in update flow
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST mode — scoped assessments + inline create
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarksListMode(viewModel: TeacherGradebookViewModel, scopeLabel: String) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    var composerOpen by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Column {
                    TEyebrow("TESTS & MARKS", dot = c.accent)
                    Spacer(Modifier.height(6.dp))
                    Text(scopeLabel.ifBlank { state.scopeHint }, style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
                    Spacer(Modifier.height(12.dp))
                    VButton(
                        text = if (composerOpen) "Close" else "Create a test",
                        onClick = { composerOpen = !composerOpen },
                        full = true,
                        variant = if (composerOpen) VButtonVariant.Ghost else VButtonVariant.Primary,
                        tone = VButtonTone.Lavender,
                        leading = { Icon(if (composerOpen) VIcons.Close else VIcons.Plus, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    )
                }
            }
        }

        if (composerOpen) {
            item { CreateAssessmentComposer(viewModel) { composerOpen = false } }
        }

        when {
            state.isListLoading && state.assessments.isEmpty() -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
            state.listError != null && state.assessments.isEmpty() -> item {
                TCard { Column {
                    Text("Couldn't load tests", style = VTheme.type.bodyStrong.colored(c.ink))
                    Spacer(Modifier.height(8.dp))
                    VButton("Retry", onClick = { viewModel.retryList() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                } }
            }
            state.assessments.isEmpty() -> item {
                TCard { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TIconDisc(VIcons.GraduationCap, tint = c.accent, bg = c.accent.copy(alpha = 0.12f), size = 48.dp, glyph = 24.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("No tests yet", style = VTheme.type.h3.colored(c.ink))
                    Text("Create your first test for this class.", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                } }
            }
            else -> items(state.assessments, key = { it.id }) { a -> AssessmentRow(a, viewModel) }
        }
    }
}

@Composable
private fun CreateAssessmentComposer(viewModel: TeacherGradebookViewModel, onDone: () -> Unit) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    TCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("New test", style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
            VInput(value = state.createName, onValueChange = viewModel::setCreateName, label = "Test name", placeholder = "e.g. Unit Test 1")
            // Type chips
            Text("Type", style = VTheme.type.inputLabel.colored(c.ink2))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssessmentType.ALL.forEach { t ->
                    val active = state.createType == t
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) c.accent.copy(alpha = 0.14f) else c.cream)
                            .border(1.dp, if (active) c.accent.copy(alpha = 0.5f) else c.hairline, RoundedCornerShape(999.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.setCreateType(t) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(t.replaceFirstChar { it.uppercase() }, style = VTheme.type.caption.colored(if (active) c.accentDeep else c.ink2).copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VInput(value = state.createMaxMarks, onValueChange = viewModel::setCreateMaxMarks, label = "Max marks", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                VInput(value = state.createPassMarks, onValueChange = viewModel::setCreatePassMarks, label = "Pass (optional)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            }
            VDatePicker(value = state.createExamDate, onValueChange = viewModel::setCreateExamDate, label = "Exam date", placeholder = "Pick the test date")
            if (state.createError != null) {
                Text(state.createError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp))
            }
            VButton(
                text = "Create test",
                onClick = { viewModel.createAssessment() },
                full = true,
                tone = VButtonTone.Lavender,
                loading = state.isCreating,
            )
        }
    }
}

@Composable
private fun AssessmentRow(a: AssessmentDto, viewModel: TeacherGradebookViewModel) {
    val c = VTheme.colors
    // Per directive: marks open only once the exam date has passed (a scheduled future test is locked).
    val today = todayIso()
    val examDate = a.examDate
    val examPassed = examDate == null || examDate <= today
    val canEnter = examPassed && !a.isPublished
    val statusTint: Color
    val statusLabel: String
    when {
        a.isPublished -> { statusTint = c.successInk; statusLabel = "PUBLISHED" }
        a.status == AssessmentStatus.MARKS_PENDING -> { statusTint = c.warningInk; statusLabel = "MARKS PENDING" }
        !examPassed -> { statusTint = c.navy; statusLabel = "SCHEDULED" }
        else -> { statusTint = c.accent; statusLabel = "READY TO MARK" }
    }
    val ix = remember { MutableInteractionSource() }
    TCard(
        padding = 14.dp,
        onClick = if (canEnter || a.isPublished) ({ viewModel.openMarks(a) }) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TIconDisc(VIcons.GraduationCap, tint = statusTint, bg = statusTint.copy(alpha = 0.12f), size = 42.dp, glyph = 21.dp)
            Column(Modifier.weight(1f)) {
                Text(a.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append("Max ${a.maxMarks}")
                        if (examDate != null) append(" · ${prettyDateShort(examDate)}")
                        if (a.rosterCount > 0) append(" · entered ${a.enteredCount}/${a.rosterCount}")
                    },
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.5.sp),
                )
                Spacer(Modifier.height(6.dp))
                TPill(statusLabel, bg = statusTint.copy(alpha = 0.14f), fg = statusTint)
            }
            if (canEnter || a.isPublished) {
                Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
            } else {
                Icon(VIcons.Lock, contentDescription = "Locked until exam date", tint = c.ink3, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARKS mode — the grid + save + publish
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarksGridMode(viewModel: TeacherGradebookViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    val a = state.activeAssessment
    var publishConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(999.dp)).background(c.cream)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.backToList() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = c.ink, modifier = Modifier.size(15.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a?.name ?: "Marks", style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
                            Text("Max ${state.maxMarks} · entered ${state.enteredCount}/${state.rosterCount}", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
                        }
                        state.liveAverage?.let { avg ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(fmt1(avg), style = VTheme.type.dataLg.colored(c.accentDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp))
                                Text("avg", style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp))
                            }
                        }
                    }
                    if (a?.isPublished == true) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(VIcons.ShieldCheck, contentDescription = null, tint = c.successInk, modifier = Modifier.size(15.dp))
                            Text("Published — parents notified. Marks are read-only.", style = VTheme.type.caption.colored(c.successInk).copy(fontSize = 11.5.sp))
                        }
                    }
                }
            }
        }

        when {
            state.isMarksLoading && state.students.isEmpty() -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
            state.marksError != null && state.students.isEmpty() -> item {
                TCard { Column {
                    Text("Couldn't load the roster", style = VTheme.type.bodyStrong.colored(c.ink))
                    Spacer(Modifier.height(8.dp))
                    VButton("Retry", onClick = { viewModel.retryMarks() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                } }
            }
            else -> items(state.students, key = { it.studentId }) { s ->
                MarkRow(s, maxMarks = state.maxMarks, readOnly = a?.isPublished == true, onMark = { v -> viewModel.setMark(s.studentId, v) }, onToggleAbsent = { viewModel.toggleAbsent(s.studentId) })
            }
        }

        if (a?.isPublished != true) {
            item {
                Spacer(Modifier.height(4.dp))
                if (state.saveError != null) { Text(state.saveError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp)); Spacer(Modifier.height(8.dp)) }
                VButton(
                    text = "Save marks",
                    onClick = { viewModel.save() },
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                    size = VButtonSize.Lg,
                    loading = state.isSaving,
                    success = state.saveSuccess,
                    successLabel = "Saved (not published)",
                    stateful = true,
                )
                Spacer(Modifier.height(8.dp))
                if (state.publishError != null) { Text(state.publishError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp)); Spacer(Modifier.height(8.dp)) }
                VButton(
                    text = "Publish & notify parents",
                    onClick = { publishConfirm = true },
                    full = true,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Lg,
                    enabled = state.canPublish,
                    loading = state.isPublishing,
                    leading = { Icon(VIcons.Send, contentDescription = null, modifier = Modifier.size(15.dp)) },
                )
            }
        }
    }

    if (publishConfirm && a != null) {
        TeacherConfirmDialog(
            title = "Publish ${a.name}?",
            body = "This will publish the results and notify parents. You can't unpublish from here.",
            confirmLabel = "Publish",
            onConfirm = { publishConfirm = false; viewModel.publish() },
            onDismiss = { publishConfirm = false },
        )
    }
}

@Composable
private fun MarkRow(s: GradebookStudentMark, maxMarks: Int, readOnly: Boolean, onMark: (Float?) -> Unit, onToggleAbsent: () -> Unit) {
    val c = VTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.hairline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text("Roll ${s.rollNo}", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
        }
        // AB toggle
        val abActive = s.isAbsent
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (abActive) c.dangerInk.copy(alpha = 0.16f) else c.cream)
                .border(1.dp, if (abActive) c.dangerInk.copy(alpha = 0.5f) else c.hairline, RoundedCornerShape(10.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !readOnly) { onToggleAbsent() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text("AB", style = VTheme.type.bodyStrong.colored(if (abActive) c.dangerInk else c.ink3).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
        }
        // Mark input
        MarkInput(value = s.marks, maxMarks = maxMarks, enabled = !readOnly && !s.isAbsent, onChange = onMark)
    }
}

@Composable
private fun MarkInput(value: Float?, maxMarks: Int, enabled: Boolean, onChange: (Float?) -> Unit) {
    val c = VTheme.colors
    val display = value?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: ""
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) c.cream else c.cream.copy(alpha = 0.5f))
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = display,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    onChange(cleaned.toFloatOrNull())
                },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                cursorBrush = SolidColor(c.accent),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (display.isBlank()) Text("—", style = VTheme.type.body.colored(c.ink3).copy(fontSize = 15.sp))
                        inner()
                    }
                },
            )
        }
        Text(" /$maxMarks", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
    }
}
