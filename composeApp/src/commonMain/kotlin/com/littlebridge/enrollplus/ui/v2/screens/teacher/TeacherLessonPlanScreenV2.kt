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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkItemDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.SyllabusNodeDto
import com.littlebridge.enrollplus.feature.teacher.presentation.LessonPlanMode
import com.littlebridge.enrollplus.feature.teacher.presentation.LessonPlanSummary
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherLessonPlanViewModel
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherLessonPlanScreenV2 — the scoped lesson plan surface (LESSON_PLANNING_SPEC P1-20).
 * Reached PRE-SCOPED with a pre-authorized [assignmentId]. Four modes:
 *   • LIST     — this class's lesson plans + create button + calendar/templates shortcuts
 *   • EDITOR   — create/edit: title, objectives, activities, resources, assessment, duration, date
 *   • CALENDAR — month grid with per-day lesson plans
 *   • TEMPLATES — list own+shared templates; instantiate or save new
 */
@Composable
fun TeacherLessonPlanScreenV2(
    assignmentId: String,
    scopeLabel: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherLessonPlanViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) {
            viewModel.load(assignmentId, scopeLabel)
        }
    }

    Box(modifier.fillMaxSize().background(c.background)) {
        when (state.mode) {
            LessonPlanMode.List -> LessonPlanListMode(viewModel, scopeLabel)
            LessonPlanMode.Editor -> LessonPlanEditorMode(viewModel)
            LessonPlanMode.Calendar -> LessonPlanCalendarMode(viewModel)
            LessonPlanMode.Templates -> LessonPlanTemplatesMode(viewModel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LessonPlanListMode(viewModel: TeacherLessonPlanViewModel, scopeLabel: String) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Column {
                    TEyebrow("LESSON PLANS", dot = c.accent)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        scopeLabel.ifBlank { state.scopeLabel },
                        style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = "New plan",
                            onClick = { viewModel.openNewEditor() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Primary,
                            tone = VButtonTone.Lavender,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                        VButton(
                            text = "Calendar",
                            onClick = { viewModel.openCalendar() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Lavender,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.Calendar, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                        VButton(
                            text = "Templates",
                            onClick = { viewModel.openTemplates() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Lavender,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.ClipboardList, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                    }
                }
            }
        }

        // Status filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LessonStatusChip("All", state.statusFilter == null) { viewModel.setStatusFilter(null) }
                LessonStatusChip("Planned", state.statusFilter == "planned") { viewModel.setStatusFilter("planned") }
                LessonStatusChip("Completed", state.statusFilter == "completed") { viewModel.setStatusFilter("completed") }
                LessonStatusChip("Skipped", state.statusFilter == "skipped") { viewModel.setStatusFilter("skipped") }
            }
        }

        when {
            state.isLoading && state.items.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() }
            }
            state.error != null && state.items.isEmpty() -> item {
                TCard {
                    Column {
                        Text("Couldn't load lesson plans", style = VTheme.type.bodyStrong.colored(c.ink))
                        Spacer(Modifier.height(8.dp))
                        VButton("Retry", onClick = { viewModel.retry() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                    }
                }
            }
            state.items.isEmpty() -> item {
                TCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TIconDisc(VIcons.ClipboardList, tint = c.accent, bg = c.accent.copy(alpha = 0.14f), size = 48.dp, glyph = 24.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("No lesson plans yet", style = VTheme.type.h3.colored(c.ink))
                        Text("Create your first lesson plan for this class.", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                    }
                }
            }
            else -> items(state.items, key = { it.id }) { plan ->
                LessonPlanRow(plan) { viewModel.openExistingEditor(plan.id) }
            }
        }
    }
}

@Composable
private fun LessonStatusChip(label: String, active: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) c.accent.copy(alpha = 0.14f) else c.cream)
            .border(1.dp, if (active) c.accent.copy(alpha = 0.5f) else c.hairline, RoundedCornerShape(999.dp))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = VTheme.type.label.colored(if (active) c.accentDeep else c.ink3).copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun LessonPlanRow(plan: LessonPlanSummary, onClick: () -> Unit) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    val statusColor = when (plan.status) {
        "completed" -> c.tealDeep
        "skipped" -> c.ink3
        else -> c.accent
    }
    val statusIcon = when (plan.status) {
        "completed" -> VIcons.Check
        "skipped" -> VIcons.Close
        else -> VIcons.ClipboardList
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.hairline, RoundedCornerShape(18.dp))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TIconDisc(statusIcon, tint = statusColor, bg = statusColor.copy(alpha = 0.12f), size = 44.dp, glyph = 22.dp)
        Column(Modifier.weight(1f)) {
            Text(
                plan.title,
                style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val dateText = plan.plannedDate?.let { prettyDateShort(it) } ?: "No date"
            val unitText = plan.unitTitle?.let { " · $it" } ?: ""
            Text(
                "$dateText · ${plan.durationMinutes} min$unitText",
                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
            )
        }
        TPill(plan.status.uppercase(), bg = statusColor.copy(alpha = 0.12f), fg = statusColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDITOR mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LessonPlanEditorMode(viewModel: TeacherLessonPlanViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    val e = state.editor

    var objectiveText by remember { mutableStateOf("") }
    var resourceText by remember { mutableStateOf("") }
    var activityText by remember { mutableStateOf("") }
    var activityDuration by remember { mutableStateOf("15") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item {
            TCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ix = remember { MutableInteractionSource() }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c.cream)
                            .clickable(interactionSource = ix, indication = null) { viewModel.closeEditor() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = c.ink2, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    TEyebrow(if (e.isNew) "NEW LESSON PLAN" else "EDIT LESSON PLAN", dot = c.accent)
                }
            }
        }

        // Title
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    VInput(value = e.title, onValueChange = viewModel::setEditorTitle, placeholder = "Lesson title")
                }
            }
        }

        // Planned date + duration
        item {
            TCard(padding = 16.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Planned date", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))
                        VInput(value = e.plannedDate, onValueChange = viewModel::setEditorDate, placeholder = "YYYY-MM-DD")
                    }
                    Column(Modifier.width(100.dp)) {
                        Text("Minutes", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))
                        VInput(value = e.durationMinutes.toString(), onValueChange = { v -> v.toIntOrNull()?.let { viewModel.setEditorDuration(it) } }, placeholder = "45")
                    }
                }
            }
        }

        // Curriculum unit picker
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Curriculum unit (optional)", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    if (state.syllabusUnits.isEmpty()) {
                        Text("No syllabus units available", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                    } else {
                        UnitPicker(
                            units = state.syllabusUnits,
                            selectedId = e.curriculumUnitId,
                            onPick = viewModel::setEditorUnit,
                        )
                    }
                }
            }
        }

        // Homework link picker
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Link homework (optional)", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    if (state.homeworkOptions.isEmpty()) {
                        Text("No active homework for this class", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                    } else {
                        HomeworkPicker(
                            homework = state.homeworkOptions,
                            selectedId = e.homeworkId,
                            onPick = viewModel::setEditorHomework,
                        )
                    }
                }
            }
        }

        // Objectives
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Objectives", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    e.objectives.forEachIndexed { i, obj ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
                            Text(obj, style = VTheme.type.body.colored(c.ink).copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                            val ix = remember { MutableInteractionSource() }
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(c.cream)
                                    .clickable(interactionSource = ix, indication = null) { viewModel.removeObjective(i) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(VIcons.Close, contentDescription = "Remove", tint = c.ink3, modifier = Modifier.size(12.dp)) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput(
                            value = objectiveText,
                            onValueChange = { objectiveText = it },
                            placeholder = "Add objective…",
                            modifier = Modifier.weight(1f),
                        )
                        VButton(
                            text = "Add",
                            onClick = { viewModel.addObjective(objectiveText); objectiveText = "" },
                            size = VButtonSize.Sm,
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Lavender,
                        )
                    }
                }
            }
        }

        // Activities
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Activities", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    e.activities.forEachIndexed { i, act ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(c.tealDeep))
                            Text("${act.activity} (${act.durationMin} min)", style = VTheme.type.body.colored(c.ink).copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                            val ix = remember { MutableInteractionSource() }
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(c.cream)
                                    .clickable(interactionSource = ix, indication = null) { viewModel.removeActivity(i) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(VIcons.Close, contentDescription = "Remove", tint = c.ink3, modifier = Modifier.size(12.dp)) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput(
                            value = activityText,
                            onValueChange = { activityText = it },
                            placeholder = "Add activity…",
                            modifier = Modifier.weight(1f),
                        )
                        VInput(
                            value = activityDuration,
                            onValueChange = { activityDuration = it },
                            placeholder = "min",
                            modifier = Modifier.width(60.dp),
                        )
                        VButton(
                            text = "Add",
                            onClick = {
                                val dur = activityDuration.toIntOrNull() ?: 15
                                viewModel.addActivity(activityText, dur)
                                activityText = ""
                                activityDuration = "15"
                            },
                            size = VButtonSize.Sm,
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Teal,
                        )
                    }
                }
            }
        }

        // Resources
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resources", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    e.resources.forEachIndexed { i, res ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(c.tealDeep))
                            Text(res, style = VTheme.type.body.colored(c.ink).copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                            val ix = remember { MutableInteractionSource() }
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(c.cream)
                                    .clickable(interactionSource = ix, indication = null) { viewModel.removeResource(i) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(VIcons.Close, contentDescription = "Remove", tint = c.ink3, modifier = Modifier.size(12.dp)) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput(
                            value = resourceText,
                            onValueChange = { resourceText = it },
                            placeholder = "Add resource…",
                            modifier = Modifier.weight(1f),
                        )
                        VButton(
                            text = "Add",
                            onClick = { viewModel.addResource(resourceText); resourceText = "" },
                            size = VButtonSize.Sm,
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Teal,
                        )
                    }
                }
            }
        }

        // Assessment method
        item {
            TCard(padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Assessment method", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    VInput(value = e.assessmentMethod, onValueChange = viewModel::setEditorAssessment, placeholder = "How will you assess? (optional)")
                }
            }
        }

        // Error
        if (e.error != null) {
            item {
                Text(e.error ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp))
            }
        }

        // Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(
                    text = if (e.isNew) "Create plan" else "Save changes",
                    onClick = { viewModel.savePlan() },
                    full = true,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Md,
                    loading = e.isSaving,
                )
                if (!e.isNew) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = "Complete",
                            onClick = { viewModel.completePlan() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Teal,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            enabled = !e.isCompleted && !e.isSkipped,
                        )
                        VButton(
                            text = "Skip",
                            onClick = { viewModel.skipPlan() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Lavender,
                            size = VButtonSize.Md,
                            enabled = !e.isCompleted && !e.isSkipped,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = "Save as template",
                            onClick = { viewModel.openSaveTemplateDialog() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Sm,
                        )
                        VButton(
                            text = "Delete",
                            onClick = { viewModel.deletePlan() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Sm,
                        )
                    }
                }
            }
        }
    }

    // Save template dialog
    if (state.showSaveTemplateDialog) {
        SaveTemplateDialog(viewModel)
    }
}

@Composable
private fun SaveTemplateDialog(viewModel: TeacherLessonPlanViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    TCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Save as template", style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
            VInput(value = state.templateTitle, onValueChange = viewModel::setTemplateTitle, placeholder = "Template title")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val ix = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(24.dp).clip(CircleShape)
                        .background(if (state.templateIsShared) c.accent.copy(alpha = 0.14f) else c.cream)
                        .border(1.dp, if (state.templateIsShared) c.accent else c.hairline, CircleShape)
                        .clickable(interactionSource = ix, indication = null) { viewModel.setTemplateShared(!state.templateIsShared) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.templateIsShared) Icon(VIcons.Check, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(14.dp))
                }
                Text("Share with other teachers in school", style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VButton("Cancel", onClick = { viewModel.closeSaveTemplateDialog() }, modifier = Modifier.weight(1f), variant = VButtonVariant.Ghost, size = VButtonSize.Md)
                VButton("Save", onClick = { viewModel.saveTemplate() }, modifier = Modifier.weight(1f), tone = VButtonTone.Lavender, size = VButtonSize.Md, loading = state.isSavingTemplate)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CALENDAR mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LessonPlanCalendarMode(viewModel: TeacherLessonPlanViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    val cal = state.calendar

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ix = remember { MutableInteractionSource() }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c.cream)
                            .clickable(interactionSource = ix, indication = null) { viewModel.closeCalendar() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = c.ink2, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    TEyebrow("CALENDAR", dot = c.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(cal.month.ifBlank { "—" }, style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        if (state.isCalendarLoading && cal.days.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
        } else if (cal.days.isEmpty()) {
            item {
                TCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        TIconDisc(VIcons.Calendar, tint = c.accent, bg = c.accent.copy(alpha = 0.14f), size = 48.dp, glyph = 24.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("No plans this month", style = VTheme.type.h3.colored(c.ink))
                    }
                }
            }
        } else {
            items(cal.days, key = { it.date }) { day ->
                TCard(padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            prettyDateShort(day.date),
                            style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold),
                        )
                        day.plans.forEach { plan ->
                            val statusColor = when (plan.status) {
                                "completed" -> c.tealDeep
                                "skipped" -> c.ink3
                                else -> c.accent
                            }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(statusColor.copy(alpha = 0.08f))
                                    .clickable { viewModel.openExistingEditor(plan.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                                Text(plan.title, style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(plan.status, style = VTheme.type.caption.colored(statusColor).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TEMPLATES mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LessonPlanTemplatesMode(viewModel: TeacherLessonPlanViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ix = remember { MutableInteractionSource() }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c.cream)
                            .clickable(interactionSource = ix, indication = null) { viewModel.closeTemplates() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = c.ink2, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    TEyebrow("TEMPLATES", dot = c.accent)
                }
            }
        }

        if (state.isTemplatesLoading && state.templates.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
        } else if (state.templatesError != null && state.templates.isEmpty()) {
            item {
                TCard {
                    Column {
                        Text("Couldn't load templates", style = VTheme.type.bodyStrong.colored(c.ink))
                        Spacer(Modifier.height(8.dp))
                        VButton("Retry", onClick = { viewModel.loadTemplates() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                    }
                }
            }
        } else if (state.templates.isEmpty()) {
            item {
                TCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        TIconDisc(VIcons.ClipboardList, tint = c.accent, bg = c.accent.copy(alpha = 0.14f), size = 48.dp, glyph = 24.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("No templates yet", style = VTheme.type.h3.colored(c.ink))
                        Text("Save a lesson plan as a template for quick reuse.", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                    }
                }
            }
        } else {
            items(state.templates, key = { it.id }) { tpl ->
                TemplateRow(
                    template = tpl,
                    onInstantiate = { viewModel.openInstantiateDialog(tpl.id) },
                    onDelete = { viewModel.deleteTemplate(tpl.id) },
                )
            }
        }
    }

    // Instantiate dialog
    if (state.showInstantiateDialog) {
        InstantiateDialog(viewModel)
    }
}

@Composable
private fun TemplateRow(
    template: com.littlebridge.enrollplus.feature.teacher.domain.model.LessonTemplateDto,
    onInstantiate: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    TCard(padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TIconDisc(VIcons.ClipboardList, tint = c.accent, bg = c.accent.copy(alpha = 0.12f), size = 40.dp, glyph = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(template.title, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${template.objectives.size} objectives · ${template.durationMinutes} min", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
                }
                if (template.isShared) {
                    TPill("SHARED", bg = c.teal.copy(alpha = 0.12f), fg = c.tealDeep)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(
                    text = "Use template",
                    onClick = onInstantiate,
                    modifier = Modifier.weight(1f),
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Lavender,
                    size = VButtonSize.Sm,
                )
                VButton(
                    text = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    variant = VButtonVariant.Ghost,
                    size = VButtonSize.Sm,
                )
            }
        }
    }
}

@Composable
private fun InstantiateDialog(viewModel: TeacherLessonPlanViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    TCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Instantiate from template", style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
            Text("Pick a date for this lesson plan", style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
            VInput(value = state.instantiateDate, onValueChange = viewModel::setInstantiateDate, placeholder = "YYYY-MM-DD")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VButton("Cancel", onClick = { viewModel.closeInstantiateDialog() }, modifier = Modifier.weight(1f), variant = VButtonVariant.Ghost, size = VButtonSize.Md)
                VButton("Create", onClick = { viewModel.instantiateFromTemplate() }, modifier = Modifier.weight(1f), tone = VButtonTone.Lavender, size = VButtonSize.Md, loading = state.isInstantiating)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pickers for curriculum unit + homework link
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UnitPicker(
    units: List<SyllabusNodeDto>,
    selectedId: String?,
    onPick: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PickerRow(label = "No unit linked", selected = selectedId == null, onClick = { onPick(null) })
        units.forEach { unit ->
            val indent = (unit.depth.coerceIn(0, 3) * 12).dp
            PickerRow(
                label = unit.title,
                selected = selectedId == unit.id,
                onClick = { onPick(unit.id) },
                modifier = Modifier.padding(start = indent),
            )
        }
    }
}

@Composable
private fun HomeworkPicker(
    homework: List<HomeworkItemDto>,
    selectedId: String?,
    onPick: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PickerRow(label = "No homework linked", selected = selectedId == null, onClick = { onPick(null) })
        homework.forEach { hw ->
            PickerRow(
                label = "${hw.title} (due ${hw.dueDate})",
                selected = selectedId == hw.id,
                onClick = { onPick(hw.id) },
            )
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.accent.copy(alpha = 0.10f) else c.cream)
            .border(1.dp, if (selected) c.accent.copy(alpha = 0.4f) else c.hairline, RoundedCornerShape(12.dp))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .background(if (selected) c.accent else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.dp, if (selected) c.accent else c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(VIcons.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(12.dp))
        }
        Text(
            label,
            style = VTheme.type.body.colored(if (selected) c.accentDeep else c.ink).copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
