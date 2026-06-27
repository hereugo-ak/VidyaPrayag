package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.teacher.domain.model.TeacherClassSummaryDto
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherClassesViewModel
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/** The five scoped tools the Update tab fronts. */
enum class UpdateTool(val label: String, val icon: ImageVector) {
    Attendance("Attendance", VIcons.ListChecks),
    Marks("Marks", VIcons.GraduationCap),
    Syllabus("Syllabus", VIcons.BookOpen),
    Homework("Homework", VIcons.FileText),
    LessonPlan("Lesson", VIcons.ClipboardList),
}

/**
 * TeacherUpdateScreenV2 — the UPDATE tab. A segmented switch chooses the tool (Attendance · Marks ·
 * Syllabus · Homework); each tool first asks the teacher to pick ONE of their allocations (the scope
 * gate → assignmentId), then hands off to the tool's scoped sub-screen. A back affordance returns to
 * the scope gate so the teacher can switch class without leaving the tab.
 *
 * [initialAssignmentId]/[initialScopeLabel]/[initialTool] let a Home deep-link (e.g. "mark this
 * class now") jump straight into a pre-scoped tool.
 */
@Composable
fun TeacherUpdateScreenV2(
    modifier: Modifier = Modifier,
    initialAssignmentId: String? = null,
    initialScopeLabel: String = "",
    initialTool: UpdateTool = UpdateTool.Attendance,
    classesViewModel: TeacherClassesViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val classesState by classesViewModel.state.collectAsStateV2()

    var tool by rememberSaveable { mutableStateOf(initialTool) }
    // The picked scope per tool session. null = show the scope gate.
    var pickedAssignment by rememberSaveable { mutableStateOf(initialAssignmentId) }
    var pickedLabel by rememberSaveable { mutableStateOf(initialScopeLabel) }

    Column(modifier.fillMaxSize().background(c.background)) {
        // ── Tool segmented switch ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UpdateTool.entries.forEach { t ->
                ToolChip(
                    tool = t,
                    active = t == tool,
                    modifier = Modifier.weight(1f),
                ) {
                    tool = t
                    // Switching tool returns to its scope gate (each tool re-scopes deliberately).
                    pickedAssignment = null
                    pickedLabel = ""
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val asg = pickedAssignment
            if (asg == null) {
                // Scope gate.
                when {
                    classesState.isLoading && classesState.classes.isEmpty() -> TeacherCenterState { TeacherSpinner() }
                    classesState.error != null && classesState.classes.isEmpty() -> TeacherCenterState {
                        Text(classesState.error ?: "Couldn't load your classes", style = VTheme.type.body.colored(c.ink2))
                    }
                    classesState.classes.isEmpty() -> TeacherCenterState {
                        Text("You have no class allocations yet.", style = VTheme.type.body.colored(c.ink2))
                    }
                    else -> ScopeGate(tool, classesState.classes) { cls ->
                        pickedAssignment = cls.assignmentId
                        pickedLabel = scopeLabelFor(cls)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ScopeBar(label = pickedLabel, onChange = { pickedAssignment = null; pickedLabel = "" })
                    AnimatedContent(
                        targetState = tool,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "updateTool",
                    ) { active ->
                        when (active) {
                            UpdateTool.Attendance -> TeacherAttendanceScreenV2(asg, pickedLabel)
                            UpdateTool.Marks -> TeacherMarksScreenV2(asg, pickedLabel)
                            UpdateTool.Syllabus -> TeacherSyllabusScreenV2(asg, pickedLabel)
                            UpdateTool.Homework -> TeacherHomeworkScreenV2(asg, pickedLabel)
                            UpdateTool.LessonPlan -> TeacherLessonPlanScreenV2(asg, pickedLabel)
                        }
                    }
                }
            }
        }
    }
}

private fun scopeLabelFor(cls: TeacherClassSummaryDto): String {
    val classLabel = if (cls.section.isBlank()) cls.className else "${cls.className}-${cls.section}"
    return "$classLabel · ${cls.subject}"
}

@Composable
private fun ToolChip(tool: UpdateTool, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) c.accent.copy(alpha = 0.14f) else c.card)
            .border(1.dp, if (active) c.accent.copy(alpha = 0.5f) else c.hairline, RoundedCornerShape(16.dp))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(tool.icon, contentDescription = tool.label, tint = if (active) c.accentDeep else c.ink3, modifier = Modifier.size(18.dp))
        Text(
            tool.label,
            style = VTheme.type.label.colored(if (active) c.accentDeep else c.ink3).copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun ScopeGate(tool: UpdateTool, classes: List<TeacherClassSummaryDto>, onPick: (TeacherClassSummaryDto) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 12.dp)) {
        TeacherScopeSelector(
            classes = classes,
            onPick = onPick,
            title = "Which class?",
            caption = "Pick a class to ${tool.label.lowercase()} for.",
        )
    }
}

@Composable
private fun ScopeBar(label: String, onChange: () -> Unit) {
    val c = VTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .background(c.accent.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(VIcons.School, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(15.dp))
            Text(label, style = VTheme.type.bodyStrong.colored(c.accentDeep).copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        }
        val ix = remember { MutableInteractionSource() }
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(c.cream)
                .clickable(interactionSource = ix, indication = null) { onChange() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(VIcons.ArrowLeft, contentDescription = null, tint = c.ink2, modifier = Modifier.size(13.dp))
            Text("Change", style = VTheme.type.label.colored(c.ink2).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
        }
    }
}
