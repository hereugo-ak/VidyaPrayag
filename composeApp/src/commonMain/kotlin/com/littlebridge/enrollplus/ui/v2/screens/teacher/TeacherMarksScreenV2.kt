package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.core.locale.StringKeys
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentStatus
import com.littlebridge.enrollplus.feature.teacher.domain.model.AssessmentType
import com.littlebridge.enrollplus.feature.teacher.presentation.GradebookMode
import com.littlebridge.enrollplus.feature.teacher.presentation.GradebookStudentMark
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherGradebookViewModel
import com.littlebridge.enrollplus.ui.tokens.VColors
import com.littlebridge.enrollplus.ui.tokens.VShapes
import com.littlebridge.enrollplus.ui.tokens.VTypography
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.locale.appString
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.util.todayIso
import org.koin.compose.viewmodel.koinViewModel
import com.littlebridge.enrollplus.ui.v2.screens.teacher.TeacherSpinner

/**
 * TeacherMarksScreenV2 — the scoped gradebook (Doc 07), rebuilt on the cream token base
 * (TEACHER_PORTAL_REDESIGN.md §4-§7). Reached PRE-SCOPED with a pre-authorized [assignmentId].
 * Two faces:
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
    tool: UpdateTool = UpdateTool.Marks,
    onToolChange: (UpdateTool) -> Unit = {},
    onChangeClass: () -> Unit = {},
    onImportMarks: () -> Unit = {},
    viewModel: TeacherGradebookViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) viewModel.load(assignmentId, scopeLabel)
    }

    Box(modifier.fillMaxSize().background(VColors.cream)) {
        when (state.mode) {
            GradebookMode.List -> MarksListMode(viewModel, scopeLabel, tool, onToolChange, onChangeClass)
            GradebookMode.Marks -> MarksGridMode(viewModel, scopeLabel, tool, onToolChange, onChangeClass, onImportMarks)
            GradebookMode.History -> MarksListMode(viewModel, scopeLabel, tool, onToolChange, onChangeClass)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST mode — scoped assessments + inline create
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarksListMode(
    viewModel: TeacherGradebookViewModel,
    scopeLabel: String,
    tool: UpdateTool,
    onToolChange: (UpdateTool) -> Unit,
    onChangeClass: () -> Unit,
) {
    val state by viewModel.state.collectAsStateV2()
    var composerOpen by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = TeacherDockClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScopedToolHeader(
                tool = tool,
                scopeLabel = scopeLabel,
                onToolChange = onToolChange,
                onChangeClass = onChangeClass,
            )
        }

        item {
            VtCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VButton(
                        text = if (composerOpen) appString(StringKeys.COMMON_BUTTON_CLOSE) else appString(StringKeys.TC_CREATE_A_TEST),
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
                VtCard { Column {
                    Text(appString(StringKeys.TC_COULDNT_LOAD_TESTS), style = VTypography.caption, color = VColors.ink)
                    Spacer(Modifier.height(8.dp))
                    VButton(appString(StringKeys.COMMON_BUTTON_RETRY), onClick = { viewModel.retryList() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                } }
            }
            state.assessments.isEmpty() -> item {
                VtEmptyCard(
                    title = appString(StringKeys.TC_NO_TESTS_YET),
                    subtext = appString(StringKeys.TC_CREATE_FIRST_TEST),
                    icon = VIcons.GraduationCap,
                    tint = VColors.violet,
                )
            }
            else -> items(state.assessments, key = { it.id }) { a -> AssessmentRow(a, viewModel) }
        }
    }
}

@Composable
private fun CreateAssessmentComposer(viewModel: TeacherGradebookViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateV2()
    VtCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(appString(StringKeys.TC_NEW_TEST), style = VTypography.caption, color = VColors.ink)
            VInput(value = state.createName, onValueChange = viewModel::setCreateName, label = appString(StringKeys.TC_TEST_NAME), placeholder = appString(StringKeys.TC_TEST_NAME_PH))
            // Type chips
            Text(appString(StringKeys.TC_TYPE), style = VTypography.label, color = VColors.ink2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssessmentType.ALL.forEach { t ->
                    val active = state.createType == t
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(VShapes.full)
                            .background(if (active) VColors.violetSoft else VColors.creamDeep)
                            .border(1.dp, if (active) VColors.violet.copy(alpha = 0.5f) else VColors.line, VShapes.full)
                            .clickable { viewModel.setCreateType(t) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t.replaceFirstChar { it.uppercase() },
                            style = VTypography.caption,
                            color = if (active) VColors.violet else VColors.ink2,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VInput(value = state.createMaxMarks, onValueChange = viewModel::setCreateMaxMarks, label = appString(StringKeys.TC_MAX_MARKS), keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                VInput(value = state.createPassMarks, onValueChange = viewModel::setCreatePassMarks, label = appString(StringKeys.TC_PASS_OPTIONAL), keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            }
            VDatePicker(value = state.createExamDate, onValueChange = viewModel::setCreateExamDate, label = appString(StringKeys.TC_EXAM_DATE), placeholder = appString(StringKeys.TC_PICK_TEST_DATE))
            if (state.createError != null) {
                Text(state.createError ?: "", style = VTypography.caption, color = VColors.coral)
            }
            VButton(
                text = appString(StringKeys.TC_CREATE_TEST),
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
    val today = todayIso()
    val examDate = a.examDate
    val examPassed = examDate == null || examDate <= today
    val canEnter = examPassed && !a.isPublished
    val statusTint: Color
    val statusLabel: String
    when {
        a.isPublished -> { statusTint = VColors.success; statusLabel = appString(StringKeys.TC_PUBLISHED) }
        a.status == AssessmentStatus.MARKS_PENDING -> { statusTint = VColors.gold; statusLabel = appString(StringKeys.TC_MARKS_PENDING) }
        !examPassed -> { statusTint = VColors.sky; statusLabel = appString(StringKeys.TC_SCHEDULED) }
        else -> { statusTint = VColors.violet; statusLabel = appString(StringKeys.TC_READY_TO_MARK) }
    }
    VtCard(
        onClick = if (canEnter || a.isPublished) ({ viewModel.openMarks(a) }) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VtIconDisc(VIcons.GraduationCap, tint = statusTint, bg = statusTint.copy(alpha = 0.12f), size = 44.dp, glyph = 22.dp)
            Column(Modifier.weight(1f)) {
                Text(a.name, style = VTypography.caption, color = VColors.ink, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(appString(StringKeys.TC_MAX_N, "n" to a.maxMarks.toString()))
                        if (examDate != null) append(" · ${prettyDateShort(examDate)}")
                        if (a.rosterCount > 0) append(" · ${appString(StringKeys.TC_ENTERED_N_OF_N, "entered" to a.enteredCount.toString(), "total" to a.rosterCount.toString())}")
                    },
                    style = VTypography.caption,
                    color = VColors.ink3,
                )
                Spacer(Modifier.height(6.dp))
                VtPill(statusLabel, bg = statusTint.copy(alpha = 0.14f), fg = statusTint)
            }
            if (canEnter || a.isPublished) {
                Icon(VIcons.ChevronRight, contentDescription = null, tint = VColors.ink3, modifier = Modifier.size(20.dp))
            } else {
                Icon(VIcons.Lock, contentDescription = appString(StringKeys.TC_LOCKED_UNTIL_EXAM), tint = VColors.ink3, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARKS mode — the grid + save + publish
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarksGridMode(
    viewModel: TeacherGradebookViewModel,
    scopeLabel: String,
    tool: UpdateTool,
    onToolChange: (UpdateTool) -> Unit,
    onChangeClass: () -> Unit,
    onImportMarks: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateV2()
    val a = state.activeAssessment
    var publishConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = TeacherDockClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScopedToolHeader(
                tool = tool,
                scopeLabel = scopeLabel,
                onToolChange = onToolChange,
                onChangeClass = onChangeClass,
            )
        }

        item {
            VtCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(32.dp).clip(VShapes.full).background(VColors.creamDeep)
                                .clickable { viewModel.backToList() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(VIcons.ArrowLeft, contentDescription = appString(StringKeys.COMMON_BUTTON_BACK), tint = VColors.ink, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a?.name ?: appString(StringKeys.TC_MARKS), style = VTypography.caption, color = VColors.ink, maxLines = 1)
                            Text(appString(StringKeys.TC_MAX_N_ENTERED_N_OF_N, "max" to state.maxMarks.toString(), "entered" to state.enteredCount.toString(), "total" to state.rosterCount.toString()), style = VTypography.caption, color = VColors.ink3)
                        }
                        state.liveAverage?.let { avg ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(fmt1(avg), style = VTypography.caption, color = VColors.violet)
                                Text(appString(StringKeys.TC_AVG), style = VTypography.caption, color = VColors.ink3)
                            }
                        }
                    }
                    if (a?.isPublished == true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(VIcons.ShieldCheck, contentDescription = null, tint = VColors.success, modifier = Modifier.size(15.dp))
                            Text(appString(StringKeys.TC_PUBLISHED_PARENTS_NOTIFIED), style = VTypography.caption, color = VColors.success)
                        }
                    }
                }
            }
        }

        when {
            state.isMarksLoading && state.students.isEmpty() -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
            state.marksError != null && state.students.isEmpty() -> item {
                VtCard { Column {
                    Text(appString(StringKeys.TC_COULDNT_LOAD_ROSTER), style = VTypography.caption, color = VColors.ink)
                    Spacer(Modifier.height(8.dp))
                    VButton(appString(StringKeys.COMMON_BUTTON_RETRY), onClick = { viewModel.retryMarks() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                } }
            }
            else -> {
                val disambiguatedNames = remember(state.students) {
                    val nameCounts = state.students.groupingBy { it.name }.eachCount()
                    state.students.associate { s ->
                        val disambiguated = if (nameCounts[s.name] > 1) {
                            val suffix = s.rollNo.takeIf { it.isNotBlank() } ?: s.studentId.takeLast(4)
                            "${s.name} · #$suffix"
                        } else s.name
                        s.studentId to disambiguated
                    }
                }
                items(state.students, key = { it.studentId }) { s ->
                    MarkRow(s, displayName = disambiguatedNames[s.studentId] ?: s.name, maxMarks = state.maxMarks, readOnly = a?.isPublished == true, onMark = { v -> viewModel.setMark(s.studentId, v) }, onToggleAbsent = { viewModel.toggleAbsent(s.studentId) })
                }
            }
        }

        if (a?.isPublished != true) {
            item {
                Spacer(Modifier.height(4.dp))
                VButton(
                    text = appString(StringKeys.TC_IMPORT_MARKS),
                    onClick = onImportMarks,
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                    size = VButtonSize.Md,
                    leading = { Icon(VIcons.Upload, contentDescription = null, modifier = Modifier.size(15.dp)) },
                )
                Spacer(Modifier.height(8.dp))
                if (state.saveError != null) { Text(state.saveError ?: "", style = VTypography.caption, color = VColors.coral); Spacer(Modifier.height(8.dp)) }
                VButton(
                    text = appString(StringKeys.TC_SAVE_MARKS),
                    onClick = { viewModel.save() },
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                    size = VButtonSize.Lg,
                    loading = state.isSaving,
                    success = state.saveSuccess,
                    successLabel = appString(StringKeys.TC_SAVED_NOT_PUBLISHED),
                    stateful = true,
                )
                Spacer(Modifier.height(8.dp))
                if (state.publishError != null) { Text(state.publishError ?: "", style = VTypography.caption, color = VColors.coral); Spacer(Modifier.height(8.dp)) }
                VButton(
                    text = appString(StringKeys.TC_PUBLISH_NOTIFY_PARENTS),
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
        TeacherConfirmSheet(
            title = appString(StringKeys.TC_PUBLISH_NAME_Q, "name" to a.name),
            body = appString(StringKeys.TC_PUBLISH_DESC),
            confirmLabel = appString(StringKeys.TC_PUBLISH),
            onConfirm = { publishConfirm = false; viewModel.publish() },
            onDismiss = { publishConfirm = false },
        )
    }
}

@Composable
private fun MarkRow(s: GradebookStudentMark, displayName: String, maxMarks: Int, readOnly: Boolean, onMark: (Float?) -> Unit, onToggleAbsent: () -> Unit) {
    VtCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(displayName, style = VTypography.caption, color = VColors.ink, maxLines = 1)
                Text(
                    if (s.rollNo.isNotBlank()) appString(StringKeys.TC_ROLL_N, "n" to s.rollNo) else "—",
                    style = VTypography.caption, color = VColors.ink3,
                )
            }
            // AB toggle
            val abActive = s.isAbsent
            Box(
                Modifier
                    .clip(VShapes.sm)
                    .background(if (abActive) VColors.coral.copy(alpha = 0.16f) else VColors.creamDeep)
                    .border(1.dp, if (abActive) VColors.coral.copy(alpha = 0.5f) else VColors.line, VShapes.sm)
                    .clickable(enabled = !readOnly) { onToggleAbsent() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text("AB", style = VTypography.caption, color = if (abActive) VColors.coral else VColors.ink3)
            }
            // Mark input
            MarkInput(value = s.marks, maxMarks = maxMarks, enabled = !readOnly && !s.isAbsent, onChange = onMark)
        }
    }
}

@Composable
private fun MarkInput(value: Float?, maxMarks: Int, enabled: Boolean, onChange: (Float?) -> Unit) {
    val display = value?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: ""
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(64.dp)
                .clip(VShapes.sm)
                .background(if (enabled) VColors.creamDeep else VColors.creamDeep.copy(alpha = 0.5f))
                .border(1.dp, VColors.line, VShapes.sm)
                .padding(horizontal = 8.dp, vertical = 10.dp),
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
                textStyle = VTypography.body.copy(color = VColors.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                cursorBrush = SolidColor(VColors.violet),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (display.isBlank()) Text("—", style = VTypography.body, color = VColors.ink3)
                        inner()
                    }
                },
            )
        }
        Text(" /$maxMarks", style = VTypography.caption, color = VColors.ink3)
    }
}
