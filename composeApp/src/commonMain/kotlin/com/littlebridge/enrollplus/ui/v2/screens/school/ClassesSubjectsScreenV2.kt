package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkPeriodItem
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateExceptionRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodExceptionDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDaySlotDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TeacherCardDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetablePeriodDto
import com.littlebridge.enrollplus.feature.admin.presentation.ClassesSubjectsState
import com.littlebridge.enrollplus.feature.admin.presentation.ClassesSubjectsViewModel
import com.littlebridge.enrollplus.feature.admin.presentation.SchoolDayConfigViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VConfirmDialog
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VTopTabs
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.VColors
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

private val TABS = listOf("Classes", "Subjects", "Schedule", "Exceptions & Requests")

@Composable
fun ClassesSubjectsScreenV2(
    onBack: () -> Unit = {},
    onOpenClassDetail: (SchoolClassDto) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ClassesSubjectsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var activeTab by remember { mutableStateOf("Classes") }

    Column(modifier.fillMaxSize().statusBarsPadding()
        .imePadding()
        .navigationBarsPadding()) {
        VBackHeader(title = "Classes & Subjects", onBack = onBack)
        VTopTabs(tabs = TABS, selected = activeTab, onSelect = { activeTab = it })
        when (activeTab) {
            "Classes" -> ClassesTab(
                state = state,
                onOpenClass = onOpenClassDetail,
                onCreate = { code, name, sections, onDone -> viewModel.createClass(code, name, sections, onDone) },
                onUpdate = { id, code, name, sections, onDone -> viewModel.updateClass(id, code, name, sections, onDone) },
                onDelete = { id -> viewModel.deleteClass(id) },
            )
            "Subjects" -> SubjectsTab(
                state = state,
                onSelectClass = { viewModel.selectClass(it) },
                onCreateSubject = { classId, name, code, onDone -> viewModel.createSubject(classId, name, code, onDone) },
                onUpdateSubject = { subjectId, classId, name, code, onDone -> viewModel.updateSubject(subjectId, classId, name, code, onDone) },
                onDeleteSubject = { subjectId, classId -> viewModel.deleteSubject(subjectId, classId) },
            )
            "Schedule" -> ScheduleTab(
                state = state,
                onLoadTimetable = { filter -> viewModel.loadTimetable(filter) },
                onCreatePeriod = { teacherId, className, section, subject, weekday, startTime, endTime, room, onDone ->
                    viewModel.createPeriod(teacherId, className, section, subject, weekday, startTime, endTime, room, onDone)
                },
                onBulkCreatePeriods = { weekday, periods, onDone ->
                    viewModel.bulkCreatePeriods(weekday, periods, onDone)
                },
                onDeletePeriod = { id -> viewModel.deletePeriod(id) },
                onCreateTeacherInline = { name, identifier, onDone ->
                    viewModel.createTeacherInline(name, identifier, onDone)
                },
                onCreateSubjectInline = { classId, name, code, onDone ->
                    viewModel.createSubject(classId, name, code, onDone)
                },
            )
            "Exceptions & Requests" -> ExceptionsRequestsTab(
                state = state,
                onLoadExceptions = { date -> viewModel.loadExceptions(date) },
                onCreateException = { req, onDone -> viewModel.createException(req, onDone) },
                onDeleteException = { id -> viewModel.deleteException(id) },
                onLoadRequests = { status -> viewModel.loadChangeRequests(status) },
                onApprove = { id, note -> viewModel.approveChangeRequest(id, note) },
                onReject = { id, note -> viewModel.rejectChangeRequest(id, note) },
            )
        }
    }
}

// ── Classes Tab ───────────────────────────────────────────────────────────────

@Composable
private fun ClassesTab(
    state: ClassesSubjectsState,
    onOpenClass: (SchoolClassDto) -> Unit,
    onCreate: (code: String, name: String, sections: List<String>, onDone: () -> Unit) -> Unit,
    onUpdate: (id: String, code: String, name: String, sections: List<String>, onDone: () -> Unit) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<SchoolClassDto?>(null) }
    var deleteTarget by remember { mutableStateOf<SchoolClassDto?>(null) }

    VStateHost(
        loading = state.isLoading,
        error = state.errorMessage,
        isEmpty = state.classes.isEmpty() && !state.isLoading,
        emptyTitle = "No classes yet",
        emptyBody = "Add your first class to get started.",
        emptyIcon = VIcons.BookOpen,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VSectionHeader("Classes")
            VButton(
                text = "Add Class",
                onClick = { showAddDialog = true },
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
            state.classes.forEach { cls ->
                ClassCard(
                    cls = cls,
                    onOpen = { onOpenClass(cls) },
                    onEdit = { editingClass = cls },
                    onDelete = { deleteTarget = cls },
                )
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        ClassEditDialog(
            title = "Add Class",
            initialCode = "",
            initialName = "",
            initialSections = listOf("A"),
            isSaving = state.isSaving,
            onSave = { code, name, sections ->
                onCreate(code, name, sections) { showAddDialog = false }
            },
            onDismiss = { showAddDialog = false },
        )
    }
    editingClass?.let { cls ->
        ClassEditDialog(
            title = "Edit Class",
            initialCode = cls.code,
            initialName = cls.name,
            initialSections = cls.sections.ifEmpty { listOf("A") },
            isSaving = state.isSaving,
            onSave = { code, name, sections ->
                onUpdate(cls.id, code, name, sections) { editingClass = null }
            },
            onDismiss = { editingClass = null },
        )
    }
    deleteTarget?.let { cls ->
        VConfirmDialog(
            visible = true,
            title = "Delete ${cls.name}?",
            message = "This will also delete all subjects in this class. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(cls.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun ClassCard(
    cls: SchoolClassDto,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    VCard(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { onOpen() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(cls.name, style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
                        VBadge(text = cls.code, tone = VBadgeTone.Arctic)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        cls.sections.forEach { s ->
                            VBadge(text = s, tone = VBadgeTone.Accent)
                        }
                        if (cls.sections.isEmpty()) {
                            Text("No sections", style = VTheme.type.caption, color = VTheme.colors.ink3)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("${cls.subjectCount} subjects", style = VTheme.type.caption, color = VTheme.colors.ink3)
                    }
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(VTheme.colors.tealDeep.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("›", color = VTheme.colors.tealDeep, fontWeight = FontWeight.Bold, style = VTheme.type.h3)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(
                    text = "Edit",
                    onClick = onEdit,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Teal,
                )
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(VTheme.colors.dangerInk.copy(alpha = 0.1f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = VTheme.colors.dangerInk, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ClassEditDialog(
    title: String,
    initialCode: String,
    initialName: String,
    initialSections: List<String>,
    isSaving: Boolean,
    onSave: (code: String, name: String, sections: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf(initialCode) }
    var name by remember { mutableStateOf(initialName) }
    var sectionsText by remember { mutableStateOf(initialSections.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
    VCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
            VInput(value = code, onValueChange = { code = it }, label = "Class Code", hint = "e.g. 10A", placeholder = "10A")
            VInput(value = name, onValueChange = { name = it }, label = "Class Name", hint = "e.g. Grade 10", placeholder = "Grade 10")
            VInput(
                value = sectionsText,
                onValueChange = { sectionsText = it },
                label = "Sections (comma-separated)",
                hint = "A, B, C",
                placeholder = "A, B, C",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                VButton(
                    text = "Save",
                    onClick = {
                        val sections = sectionsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val finalSections = if (sections.isEmpty()) listOf("A") else sections
                        onSave(code, name, finalSections)
                    },
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                    loading = isSaving,
                )
            }
        }
    }
    }
}

// ── Subjects Tab ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectsTab(
    state: ClassesSubjectsState,
    onSelectClass: (String) -> Unit,
    onCreateSubject: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
    onUpdateSubject: (subjectId: String, classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
    onDeleteSubject: (subjectId: String, classId: String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SchoolSubjectDto?>(null) }
    var deleteSubjectTarget by remember { mutableStateOf<SchoolSubjectDto?>(null) }

    val selectedClassId = state.selectedClassId
    val subjects = selectedClassId?.let { state.subjectsByClass[it] } ?: emptyList()
    val selectedClass = state.classes.find { it.id == selectedClassId }

    VStateHost(
        loading = state.isLoading,
        error = state.errorMessage,
        isEmpty = state.classes.isEmpty() && !state.isLoading,
        emptyTitle = "No classes available",
        emptyBody = "Add classes first in the Classes tab.",
        emptyIcon = VIcons.BookOpen,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VSectionHeader("Subjects")
            if (state.classes.isNotEmpty()) {
                // Class selector chips
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.classes.forEach { cls ->
                        val isSelected = cls.id == selectedClassId
                        VBadge(
                            text = cls.name,
                            tone = if (isSelected) VBadgeTone.Arctic else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { onSelectClass(cls.id) },
                        )
                    }
                }

                if (selectedClass != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${selectedClass.name} — Subjects", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
                        VButton(
                            text = "Add",
                            onClick = { showAddDialog = true },
                            variant = VButtonVariant.Primary,
                            tone = VButtonTone.Teal,
                        )
                    }
                    if (subjects.isEmpty()) {
                        VEmptyState(title = "No subjects yet", icon = VIcons.BookOpen, body = "Add a subject to this class.")
                    }
                    subjects.forEach { subj ->
                        SubjectCard(
                            subject = subj,
                            onEdit = { editingSubject = subj },
                            onDelete = { deleteSubjectTarget = subj },
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showAddDialog && selectedClassId != null) {
        SubjectEditDialog(
            title = "Add Subject",
            initialName = "",
            initialCode = "",
            isSaving = state.isSaving,
            onSave = { name, code ->
                onCreateSubject(selectedClassId, name, code) { showAddDialog = false }
            },
            onDismiss = { showAddDialog = false },
        )
    }
    editingSubject?.let { subj ->
        val classId = selectedClassId ?: subj.classId
        SubjectEditDialog(
            title = "Edit Subject",
            initialName = subj.name,
            initialCode = subj.code,
            isSaving = state.isSaving,
            onSave = { name, code ->
                onUpdateSubject(subj.id, classId, name, code) { editingSubject = null }
            },
            onDismiss = { editingSubject = null },
        )
    }
    deleteSubjectTarget?.let { subj ->
        val classId = selectedClassId ?: subj.classId
        VConfirmDialog(
            visible = true,
            title = "Delete ${subj.name}?",
            message = "This subject will be removed from the class.",
            confirmLabel = "Delete",
            onConfirm = { onDeleteSubject(subj.id, classId); deleteSubjectTarget = null },
            onDismiss = { deleteSubjectTarget = null },
        )
    }
}

@Composable
private fun SubjectCard(
    subject: SchoolSubjectDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    VCard(Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(subject.name, style = VTheme.type.body, fontWeight = FontWeight.Medium, color = VTheme.colors.ink)
                    VBadge(text = subject.code, tone = VBadgeTone.Accent)
                }
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .background(VTheme.colors.dangerInk.copy(alpha = 0.1f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = VTheme.colors.dangerInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SubjectEditDialog(
    title: String,
    initialName: String,
    initialCode: String,
    isSaving: Boolean,
    onSave: (name: String, code: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var code by remember { mutableStateOf(initialCode) }

    Dialog(onDismissRequest = onDismiss) {
    VCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
            VInput(value = name, onValueChange = { name = it }, label = "Subject Name", hint = "e.g. Mathematics", placeholder = "Mathematics")
            VInput(value = code, onValueChange = { code = it }, label = "Subject Code", hint = "e.g. MATH", placeholder = "MATH")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                VButton(
                    text = "Save",
                    onClick = { onSave(name, code) },
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                    loading = isSaving,
                )
            }
        }
    }
    }
}

// ── Schedule Tab V2 (3-step wizard: Structure → Assign → Review) ─────────────

private val SCHEDULE_STEPS = listOf("1. Day Structure", "2. Assign", "3. Review")
private val WEEKDAY_LABELS = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private data class SchedulePreset(
    val name: String,
    val description: String,
    val slots: List<SchoolDaySlotDto>,
)

private fun standardPresets(): List<SchedulePreset> = listOf(
    SchedulePreset(
        name = "8-Period Standard",
        description = "8 periods × 40 min, 2 short breaks",
        slots = listOf(
            SchoolDaySlotDto(0, "TEACHING", "Period 1", "08:00", "08:40"),
            SchoolDaySlotDto(1, "TEACHING", "Period 2", "08:40", "09:20"),
            SchoolDaySlotDto(2, "BREAK", "Short Break", "09:20", "09:35"),
            SchoolDaySlotDto(3, "TEACHING", "Period 3", "09:35", "10:15"),
            SchoolDaySlotDto(4, "TEACHING", "Period 4", "10:15", "10:55"),
            SchoolDaySlotDto(5, "BREAK", "Lunch Break", "10:55", "11:25"),
            SchoolDaySlotDto(6, "TEACHING", "Period 5", "11:25", "12:05"),
            SchoolDaySlotDto(7, "TEACHING", "Period 6", "12:05", "12:45"),
            SchoolDaySlotDto(8, "BREAK", "Short Break", "12:45", "13:00"),
            SchoolDaySlotDto(9, "TEACHING", "Period 7", "13:00", "13:40"),
            SchoolDaySlotDto(10, "TEACHING", "Period 8", "13:40", "14:20"),
        ),
    ),
    SchedulePreset(
        name = "6-Period with Long Lunch",
        description = "6 periods × 45 min, 1 long lunch",
        slots = listOf(
            SchoolDaySlotDto(0, "TEACHING", "Period 1", "09:00", "09:45"),
            SchoolDaySlotDto(1, "TEACHING", "Period 2", "09:45", "10:30"),
            SchoolDaySlotDto(2, "BREAK", "Short Break", "10:30", "10:45"),
            SchoolDaySlotDto(3, "TEACHING", "Period 3", "10:45", "11:30"),
            SchoolDaySlotDto(4, "TEACHING", "Period 4", "11:30", "12:15"),
            SchoolDaySlotDto(5, "BREAK", "Lunch Break", "12:15", "13:00"),
            SchoolDaySlotDto(6, "TEACHING", "Period 5", "13:00", "13:45"),
            SchoolDaySlotDto(7, "TEACHING", "Period 6", "13:45", "14:30"),
        ),
    ),
    SchedulePreset(
        name = "4-Block (90 min)",
        description = "4 blocks × 90 min, 2 breaks",
        slots = listOf(
            SchoolDaySlotDto(0, "TEACHING", "Block 1", "08:00", "09:30"),
            SchoolDaySlotDto(1, "BREAK", "Short Break", "09:30", "09:45"),
            SchoolDaySlotDto(2, "TEACHING", "Block 2", "09:45", "11:15"),
            SchoolDaySlotDto(3, "BREAK", "Lunch Break", "11:15", "12:00"),
            SchoolDaySlotDto(4, "TEACHING", "Block 3", "12:00", "13:30"),
            SchoolDaySlotDto(5, "BREAK", "Short Break", "13:30", "13:45"),
            SchoolDaySlotDto(6, "TEACHING", "Block 4", "13:45", "15:15"),
        ),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleTab(
    state: ClassesSubjectsState,
    onLoadTimetable: (String?) -> Unit,
    onCreatePeriod: (teacherId: String, className: String, section: String, subject: String, weekday: Int, startTime: String, endTime: String, room: String, onDone: () -> Unit) -> Unit,
    onBulkCreatePeriods: (weekday: Int, periods: List<BulkPeriodItem>, onDone: () -> Unit) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
) {
    var currentStep by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        // Step indicator
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SCHEDULE_STEPS.forEachIndexed { idx, label ->
                VBadge(
                    text = label,
                    tone = if (currentStep == idx) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.weight(1f).clickable {
                        if (idx <= currentStep + 1) currentStep = idx
                    },
                )
            }
        }

        when (currentStep) {
            0 -> ScheduleStepStructure(
                onNext = { currentStep = 1 },
            )
            1 -> ScheduleStepAssign(
                state = state,
                onLoadTimetable = onLoadTimetable,
                onCreatePeriod = onCreatePeriod,
                onDeletePeriod = onDeletePeriod,
                onCreateTeacherInline = onCreateTeacherInline,
                onCreateSubjectInline = onCreateSubjectInline,
                onBack = { currentStep = 0 },
                onNext = { currentStep = 2 },
            )
            2 -> ScheduleStepReview(
                state = state,
                onLoadTimetable = onLoadTimetable,
                onBack = { currentStep = 1 },
            )
        }
    }
}

// ── Step 1: Day Structure (Presets + Config) ──────────────────────────────────

@Composable
private fun ScheduleStepStructure(
    onNext: () -> Unit,
) {
    val presets = remember { standardPresets() }
    val dayConfigViewModel: SchoolDayConfigViewModel = koinViewModel()
    val state by dayConfigViewModel.state.collectAsStateV2()
    var selectedPresetIdx by remember { mutableStateOf(-1) }
    var showCustomConfig by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSectionHeader("Choose a Preset Template")

        presets.forEachIndexed { idx, preset ->
            PresetCard(
                preset = preset,
                isSelected = selectedPresetIdx == idx,
                onTap = {
                    selectedPresetIdx = idx
                    showCustomConfig = false
                },
            )
        }

        // Custom option
        PresetCard(
            preset = SchedulePreset("Custom", "Build your own schedule from scratch", emptyList()),
            isSelected = showCustomConfig,
            onTap = {
                showCustomConfig = true
                selectedPresetIdx = -1
            },
        )

        // Show preset preview or custom config
        if (selectedPresetIdx >= 0) {
            val preset = presets[selectedPresetIdx]
            VSectionHeader("Preview — ${preset.name}")
            DayTimelinePreview(slots = preset.slots)

            VButton(
                text = "Use This Preset → Continue",
                onClick = {
                    dayConfigViewModel.createConfig(
                        name = preset.name,
                        applicableDays = "1,2,3,4,5",
                        classLevel = "ALL",
                        slots = preset.slots,
                    ) { onNext() }
                },
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
                loading = state.isSaving,
            )
        }

        if (showCustomConfig) {
            SchoolDayConfigEmbeddedV2(
                modifier = Modifier.fillMaxWidth(),
            )
            VButton(
                text = "Continue →",
                onClick = onNext,
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
        }

        // Show existing configs
        if (state.configs.isNotEmpty() && selectedPresetIdx < 0 && !showCustomConfig) {
            VSectionHeader("Existing Configurations")
            state.configs.forEach { config ->
                ExistingConfigCard(config = config)
            }
            VButton(
                text = "Continue →",
                onClick = onNext,
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun PresetCard(
    preset: SchedulePreset,
    isSelected: Boolean,
    onTap: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        Modifier.fillMaxWidth().clickable { onTap() },
        border = isSelected,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (isSelected) c.teal.copy(alpha = 0.15f) else c.cream),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (preset.slots.isEmpty()) "+" else "✓",
                    color = if (isSelected) c.tealDeep else c.ink2,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(preset.name, style = VTheme.type.bodyStrong.colored(c.ink))
                Text(preset.description, style = VTheme.type.caption.colored(c.ink2))
            }
        }
    }
}

@Composable
private fun DayTimelinePreview(slots: List<SchoolDaySlotDto>) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        slots.forEachIndexed { idx, slot ->
            if (idx > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Color bar by type
                Box(
                    Modifier.size(width = 4.dp, height = 32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(slotTypeColor(slot.slotType, c)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        slot.label.ifBlank { slot.slotType },
                        style = VTheme.type.body.colored(c.ink),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${slot.startTime} – ${slot.endTime}",
                        style = VTheme.type.caption.colored(c.ink3),
                    )
                }
                VBadge(
                    text = slot.slotType,
                    tone = when (slot.slotType) {
                        "TEACHING" -> VBadgeTone.Arctic
                        "BREAK" -> VBadgeTone.Warning
                        "ASSEMBLY" -> VBadgeTone.Accent
                        else -> VBadgeTone.Neutral
                    },
                )
            }
        }
    }
}

@Composable
private fun ExistingConfigCard(config: SchoolDayConfigDto) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (config.isActive) {
                VBadge(text = "ACTIVE", tone = VBadgeTone.Success)
            } else {
                VBadge(text = "INACTIVE", tone = VBadgeTone.Neutral)
            }
            Text(config.name, style = VTheme.type.bodyStrong.colored(c.ink))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Days: ${config.applicableDays}  ·  Level: ${config.classLevel}  ·  ${config.slots.size} slots",
            style = VTheme.type.caption.colored(c.ink3),
        )
        if (config.slots.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            config.slots.forEachIndexed { i, slot ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(slot.label.ifBlank { slot.slotType }, style = VTheme.type.caption.colored(c.ink), modifier = Modifier.weight(1f))
                    Text("${slot.startTime}–${slot.endTime}", style = VTheme.type.caption.colored(c.ink3))
                }
            }
        }
    }
}

private fun slotTypeColor(type: String, c: VColors): Color {
    return when (type) {
        "TEACHING" -> c.teal
        "BREAK" -> c.warning
        "ASSEMBLY" -> c.accent
        "LAB" -> c.lavenderLight
        else -> c.cream
    }
}

// ── Step 2: Assign Teachers & Subjects ────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleStepAssign(
    state: ClassesSubjectsState,
    onLoadTimetable: (String?) -> Unit,
    onCreatePeriod: (teacherId: String, className: String, section: String, subject: String, weekday: Int, startTime: String, endTime: String, room: String, onDone: () -> Unit) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableStateOf(1) }
    var showPeriodEditor by remember { mutableStateOf(false) }
    var editingPeriod by remember { mutableStateOf<TimetablePeriodDto?>(null) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var editingStartTime by remember { mutableStateOf("09:00") }
    var editingEndTime by remember { mutableStateOf("10:00") }

    LaunchedEffect(Unit) { onLoadTimetable(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Class filter
        VSectionHeader("Select Class")
        val tt = state.timetable
        if (tt != null && tt.classes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VBadge(
                    text = "All Classes",
                    tone = if (selectedClass == null) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable {
                        selectedClass = null
                        onLoadTimetable(null)
                    },
                )
                tt.classes.forEach { cls ->
                    VBadge(
                        text = cls,
                        tone = if (selectedClass == cls) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable {
                            selectedClass = cls
                            onLoadTimetable(cls)
                        },
                    )
                }
            }
        } else {
            Text("No classes found. Add classes first in the Classes tab.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
        }

        // Day selector
        VSectionHeader("Select Day")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..7).forEach { day ->
                VBadge(
                    text = WEEKDAY_LABELS[day],
                    tone = if (selectedDay == day) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { selectedDay = day },
                )
            }
        }

        // Periods for selected day
        VSectionHeader("${WEEKDAY_LABELS[selectedDay]} — Periods")
        if (tt == null) {
            if (state.isLoading) {
                Text("Loading timetable…", style = VTheme.type.body.colored(VTheme.colors.ink2))
            } else {
                VEmptyState(
                    title = "No timetable loaded",
                    icon = VIcons.Calendar,
                    body = "Tap the button below to load the timetable.",
                )
                VButton(
                    text = "Load Timetable",
                    onClick = { onLoadTimetable(selectedClass) },
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Teal,
                )
            }
        } else {
            val dayData = tt.weekdays.find { it.weekday == selectedDay }
            val periods = dayData?.periods ?: emptyList()
            val filteredPeriods = if (selectedClass != null) {
                periods.filter { it.className.equals(selectedClass, ignoreCase = true) }
            } else {
                periods
            }

            if (filteredPeriods.isEmpty()) {
                VEmptyState(
                    title = "No periods for ${WEEKDAY_LABELS[selectedDay]}",
                    icon = VIcons.Calendar,
                    body = "Tap 'Add Period' to start assigning teachers and subjects.",
                )
            } else {
                filteredPeriods.forEach { period ->
                    PeriodCard(
                        period = period,
                        onTap = {
                            editingPeriod = period
                            editingStartTime = period.startTime
                            editingEndTime = period.endTime
                            showPeriodEditor = true
                        },
                        onDelete = { deleteTargetId = period.id },
                    )
                }
            }

            VButton(
                text = "+ Add Period",
                onClick = {
                    editingPeriod = null
                    editingStartTime = "09:00"
                    editingEndTime = "10:00"
                    showPeriodEditor = true
                },
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
        }

        // Info/error messages
        state.infoMessage?.let {
            Text(it, style = VTheme.type.caption.colored(VTheme.colors.successInk))
        }
        state.errorMessage?.let {
            Text(it, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
        }

        // Navigation buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "← Back",
                    onClick = onBack,
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                )
            }
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "Review →",
                    onClick = onNext,
                    full = true,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // Period editor dialog
    if (showPeriodEditor) {
        PeriodEditorDialog(
            state = state,
            weekday = selectedDay,
            editingPeriod = editingPeriod,
            startTime = editingStartTime,
            endTime = editingEndTime,
            isSaving = state.isSaving,
            onSave = { teacherId, className, section, subject, startTime, endTime, room ->
                if (editingPeriod != null) {
                    // Delete old + create new (no update API for full period)
                    onDeletePeriod(editingPeriod!!.id)
                }
                onCreatePeriod(teacherId, className, section, subject, selectedDay, startTime, endTime, room) {
                    showPeriodEditor = false
                }
            },
            onDismiss = { showPeriodEditor = false },
            onCreateTeacherInline = onCreateTeacherInline,
            onCreateSubjectInline = onCreateSubjectInline,
        )
    }

    // Delete confirmation
    deleteTargetId?.let { id ->
        VConfirmDialog(
            visible = true,
            title = "Delete period?",
            message = "This will remove the period from the recurring timetable.",
            confirmLabel = "Delete",
            onConfirm = { onDeletePeriod(id); deleteTargetId = null },
            onDismiss = { deleteTargetId = null },
        )
    }
}

@Composable
private fun PeriodCard(
    period: TimetablePeriodDto,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth().clickable { onTap() }) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Time block
            Column(
                Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(period.startTime, style = VTheme.type.bodyStrong.colored(c.ink))
                Text("↓", style = VTheme.type.caption.colored(c.ink3))
                Text(period.endTime, style = VTheme.type.body.colored(c.ink2))
            }

            // Vertical divider
            Box(Modifier.width(1.dp).height(48.dp).background(c.hairline))

            // Content
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    VBadge(text = period.className, tone = VBadgeTone.Arctic)
                    if (period.section.isNotBlank()) VBadge(text = period.section, tone = VBadgeTone.Accent)
                }
                Spacer(Modifier.height(4.dp))
                Text(period.subject, style = VTheme.type.bodyStrong.colored(c.ink))
                if (period.teacherName.isNotBlank()) {
                    Text(period.teacherName, style = VTheme.type.caption.colored(c.ink2))
                }
                if (period.room.isNotBlank()) {
                    Text("Room ${period.room}", style = VTheme.type.caption.colored(c.ink3))
                }
            }

            // Delete button
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .background(c.dangerInk.copy(alpha = 0.1f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = c.dangerInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodEditorDialog(
    state: ClassesSubjectsState,
    weekday: Int,
    editingPeriod: TimetablePeriodDto?,
    startTime: String,
    endTime: String,
    isSaving: Boolean,
    onSave: (teacherId: String, className: String, section: String, subject: String, startTime: String, endTime: String, room: String) -> Unit,
    onDismiss: () -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
) {
    val teachers = state.teachers
    val classes = state.classes
    var selectedTeacherId by remember { mutableStateOf(editingPeriod?.teacherId ?: "") }
    var selectedClassName by remember { mutableStateOf(editingPeriod?.className ?: "") }
    var selectedSection by remember { mutableStateOf(editingPeriod?.section ?: "A") }
    var subjectName by remember { mutableStateOf(editingPeriod?.subject ?: "") }
    var sTime by remember { mutableStateOf(startTime) }
    var eTime by remember { mutableStateOf(endTime) }
    var room by remember { mutableStateOf(editingPeriod?.room ?: "") }
    var showNewTeacher by remember { mutableStateOf(false) }
    var showNewSubject by remember { mutableStateOf(false) }

    val selectedClass = classes.find { it.name == selectedClassName }
    val subjectsForClass = selectedClass?.let { state.subjectsByClass[it.id] } ?: emptyList()

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (editingPeriod != null) "Edit Period — ${WEEKDAY_LABELS[weekday]}"
                    else "Add Period — ${WEEKDAY_LABELS[weekday]}",
                    style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink,
                )

                // Teacher selection
                Text("Teacher", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                if (teachers.isEmpty()) {
                    Text("No teachers yet. Add one below.", style = VTheme.type.caption.colored(VTheme.colors.ink3))
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        teachers.forEach { teacher ->
                            VBadge(
                                text = teacher.profile.name.ifBlank { teacher.id.take(8) },
                                tone = if (selectedTeacherId == teacher.id) VBadgeTone.Arctic else VBadgeTone.Neutral,
                                modifier = Modifier.clickable { selectedTeacherId = teacher.id },
                            )
                        }
                    }
                }
                VBadge(
                    text = "+ New Teacher",
                    tone = VBadgeTone.Success,
                    modifier = Modifier.clickable { showNewTeacher = true },
                )

                // Class selection
                Text("Class", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    classes.forEach { cls ->
                        VBadge(
                            text = cls.name,
                            tone = if (selectedClassName == cls.name) VBadgeTone.Arctic else VBadgeTone.Neutral,
                            modifier = Modifier.clickable {
                                selectedClassName = cls.name
                                selectedSection = cls.sections.firstOrNull() ?: "A"
                                subjectName = ""
                            },
                        )
                    }
                }

                // Section selection
                if (selectedClass != null && selectedClass.sections.size > 1) {
                    Text("Section", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedClass.sections.forEach { sec ->
                            VBadge(
                                text = sec,
                                tone = if (selectedSection == sec) VBadgeTone.Accent else VBadgeTone.Neutral,
                                modifier = Modifier.clickable { selectedSection = sec },
                            )
                        }
                    }
                }

                // Subject selection
                Text("Subject", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjectsForClass.forEach { sub ->
                        VBadge(
                            text = sub.name,
                            tone = if (subjectName == sub.name) VBadgeTone.Arctic else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { subjectName = sub.name },
                        )
                    }
                    if (selectedClass != null) {
                        VBadge(
                            text = "+ New",
                            tone = VBadgeTone.Success,
                            modifier = Modifier.clickable { showNewSubject = true },
                        )
                    }
                }
                if (subjectName.isBlank() && subjectsForClass.isEmpty() && selectedClass != null) {
                    VInput(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = "Subject Name",
                        hint = "Type subject name",
                        placeholder = "e.g. Mathematics",
                    )
                }

                // Time + Room
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        VInput(value = sTime, onValueChange = { sTime = it }, label = "Start", hint = "HH:mm", placeholder = "09:00")
                    }
                    Box(Modifier.weight(1f)) {
                        VInput(value = eTime, onValueChange = { eTime = it }, label = "End", hint = "HH:mm", placeholder = "10:00")
                    }
                }
                VInput(value = room, onValueChange = { room = it }, label = "Room", hint = "e.g. 101", placeholder = "101")

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = if (editingPeriod != null) "Update" else "Save Period",
                        onClick = {
                            val tid = selectedTeacherId.ifBlank { return@VButton }
                            val cn = selectedClassName.ifBlank { return@VButton }
                            val sn = subjectName.trim().ifBlank { return@VButton }
                            onSave(tid, cn, selectedSection, sn, sTime.trim(), eTime.trim(), room.trim())
                        },
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                        loading = isSaving,
                    )
                }
            }
        }
    }

    if (showNewTeacher) {
        InlineCreateTeacherDialog(
            isSaving = isSaving,
            onCreate = { name, identifier ->
                onCreateTeacherInline(name, identifier) {
                    showNewTeacher = false
                    selectedTeacherId = state.teachers.lastOrNull()?.id ?: ""
                }
            },
            onDismiss = { showNewTeacher = false },
        )
    }

    if (showNewSubject && selectedClass != null) {
        InlineCreateSubjectDialog(
            isSaving = isSaving,
            onCreate = { name, code ->
                onCreateSubjectInline(selectedClass.id, name, code) {
                    showNewSubject = false
                    subjectName = name
                }
            },
            onDismiss = { showNewSubject = false },
        )
    }
}

// ── Step 3: Review (Week Overview) ────────────────────────────────────────────

@Composable
private fun ScheduleStepReview(
    state: ClassesSubjectsState,
    onLoadTimetable: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val tt = state.timetable
    LaunchedEffect(Unit) { if (tt == null) onLoadTimetable(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSectionHeader("Weekly Overview")

        if (tt == null || tt.weekdays.isEmpty()) {
            VEmptyState(
                title = "No timetable to review",
                icon = VIcons.Calendar,
                body = "Go back to Step 2 and add some periods first.",
            )
        } else {
            (1..7).forEach { day ->
                val dayData = tt.weekdays.find { it.weekday == day }
                val periods = dayData?.periods ?: emptyList()
                if (periods.isNotEmpty()) {
                    VSectionHeader(WEEKDAY_LABELS[day])
                    periods.forEach { period ->
                        ReviewPeriodRow(period = period)
                    }
                }
            }
        }

        // Conflict detection
        if (tt != null) {
            val allPeriods = tt.weekdays.flatMap { it.periods }
            val conflicts = detectConflicts(allPeriods)
            if (conflicts.isNotEmpty()) {
                VSectionHeader("⚠ Conflicts Detected")
                conflicts.forEach { conflict ->
                    VCard(Modifier.fillMaxWidth()) {
                        Text(
                            "${conflict.teacherName} — ${WEEKDAY_LABELS[conflict.weekday]} ${conflict.startTime}–${conflict.endTime}",
                            style = VTheme.type.body.colored(VTheme.colors.dangerInk),
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "← Back",
                    onClick = onBack,
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ReviewPeriodRow(period: TimetablePeriodDto) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            period.startTime,
            style = VTheme.type.caption.colored(c.ink2),
            modifier = Modifier.width(48.dp),
        )
        VBadge(text = period.className, tone = VBadgeTone.Arctic)
        Text(period.subject, style = VTheme.type.caption.colored(c.ink), modifier = Modifier.weight(1f))
        Text(period.teacherName, style = VTheme.type.caption.colored(c.ink3))
    }
}

private data class ConflictInfo(
    val teacherName: String,
    val weekday: Int,
    val startTime: String,
    val endTime: String,
)

private fun detectConflicts(periods: List<TimetablePeriodDto>): List<ConflictInfo> {
    val conflicts = mutableListOf<ConflictInfo>()
    // Group by teacher + day, check for overlapping times
    // Since TimetablePeriodDto doesn't have weekday, we can't detect per-day conflicts
    // Instead, check for same teacher with overlapping time ranges across all periods
    val byTeacher = periods.groupBy { it.teacherId }
    byTeacher.forEach { (_, teacherPeriods) ->
        if (teacherPeriods.size > 1) {
            for (i in teacherPeriods.indices) {
                for (j in i + 1 until teacherPeriods.size) {
                    val a = teacherPeriods[i]
                    val b = teacherPeriods[j]
                    if (a.startTime == b.startTime && a.className != b.className) {
                        conflicts.add(ConflictInfo(a.teacherName, 0, a.startTime, a.endTime))
                    }
                }
            }
        }
    }
    return conflicts.distinctBy { it.teacherName + it.startTime }
}

// ── Inline Create Dialogs ─────────────────────────────────────────────────────

@Composable
private fun InlineCreateTeacherDialog(
    isSaving: Boolean,
    onCreate: (name: String, identifier: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New Teacher", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
                VInput(value = name, onValueChange = { name = it }, label = "Full Name", hint = "Teacher name", placeholder = "John Doe")
                VInput(value = identifier, onValueChange = { identifier = it }, label = "Email or Phone", hint = "Login identifier", placeholder = "john@school.edu")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = "Create",
                        onClick = { onCreate(name.trim(), identifier.trim()) },
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                        loading = isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineCreateSubjectDialog(
    isSaving: Boolean,
    onCreate: (name: String, code: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New Subject", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
                VInput(value = name, onValueChange = { name = it }, label = "Subject Name", hint = "e.g. Mathematics", placeholder = "Mathematics")
                VInput(value = code, onValueChange = { code = it }, label = "Subject Code", hint = "e.g. MATH", placeholder = "MATH")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = "Create",
                        onClick = { onCreate(name.trim(), code.trim()) },
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                        loading = isSaving,
                    )
                }
            }
        }
    }
}

// ── Exceptions & Requests Tab (merged) ────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExceptionsRequestsTab(
    state: ClassesSubjectsState,
    onLoadExceptions: (String?) -> Unit,
    onCreateException: (CreateExceptionRequest, () -> Unit) -> Unit,
    onDeleteException: (String) -> Unit,
    onLoadRequests: (String?) -> Unit,
    onApprove: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
) {
    var activeView by remember { mutableStateOf("exceptions") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var showAddException by remember { mutableStateOf(false) }
    var deleteExceptionTargetId by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("exceptions", "Exceptions", null as String?),
                Triple("pending", "Pending", "PENDING"),
                Triple("approved", "Approved", "APPROVED"),
                Triple("rejected", "Rejected", "REJECTED"),
            ).forEach { (key, label, statusValue) ->
                val isSelected = if (key == "exceptions") activeView == "exceptions"
                    else activeView == "requests" && statusFilter == statusValue
                VBadge(
                    text = label,
                    tone = if (isSelected) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable {
                        if (key == "exceptions") {
                            activeView = "exceptions"
                        } else {
                            activeView = "requests"
                            statusFilter = statusValue
                            onLoadRequests(statusValue)
                        }
                    },
                )
            }
        }

        if (activeView == "exceptions") {
            VSectionHeader("Period Exceptions")
            VButton(
                text = "Load Exceptions",
                onClick = { onLoadExceptions(null) },
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Teal,
            )
            if (state.exceptions.isEmpty()) {
                VEmptyState(
                    title = "No exceptions",
                    icon = VIcons.Calendar,
                    body = "Tap 'Add Exception' to create a one-off period override.",
                )
            } else {
                state.exceptions.forEach { ex ->
                    ExceptionCard(
                        exception = ex,
                        onDelete = { deleteExceptionTargetId = ex.id },
                    )
                }
            }
            VButton(
                text = "+ Add Exception",
                onClick = { showAddException = true },
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
        } else {
            VSectionHeader("Change Requests")
            VButton(
                text = "Load",
                onClick = { onLoadRequests(statusFilter) },
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
            if (state.changeRequests.isEmpty()) {
                VEmptyState(
                    title = "No requests",
                    icon = VIcons.FileText,
                    body = "Change requests from teachers will appear here.",
                )
            } else {
                state.changeRequests.forEach { req ->
                    ChangeRequestCard(
                        request = req,
                        onApprove = { note -> onApprove(req.id, note) },
                        onReject = { note -> onReject(req.id, note) },
                    )
                }
            }
        }

        state.infoMessage?.let {
            Text(it, style = VTheme.type.caption.colored(VTheme.colors.successInk))
        }
        state.errorMessage?.let {
            Text(it, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showAddException) {
        AddExceptionDialog(
            isSaving = state.isSaving,
            onCreate = { req, onDone -> onCreateException(req, onDone) },
            onDismiss = { showAddException = false },
        )
    }

    deleteExceptionTargetId?.let { id ->
        VConfirmDialog(
            visible = true,
            title = "Delete exception?",
            message = "This will remove the period override.",
            confirmLabel = "Delete",
            onConfirm = { onDeleteException(id); deleteExceptionTargetId = null },
            onDismiss = { deleteExceptionTargetId = null },
        )
    }
}

// ── Exceptions & Requests helpers ─────────────────────────────────────────────

@Composable
private fun ExceptionCard(
    exception: PeriodExceptionDto,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exception.date, style = VTheme.type.bodyStrong.colored(c.ink))
                Spacer(Modifier.height(4.dp))
                Text("${exception.kind}", style = VTheme.type.caption.colored(c.ink2))
                if (exception.note.isNotBlank()) {
                    Text(exception.note, style = VTheme.type.caption.colored(c.ink3))
                }
            }
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(c.dangerInk.copy(alpha = 0.1f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = c.dangerInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddExceptionDialog(
    isSaving: Boolean,
    onCreate: (CreateExceptionRequest, () -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("CANCEL") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Exception", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
                VInput(value = date, onValueChange = { date = it }, label = "Date", hint = "YYYY-MM-DD", placeholder = "2025-07-15")
                VInput(value = kind, onValueChange = { kind = it }, label = "Kind", hint = "CANCEL, RESCHEDULE, SUBSTITUTE", placeholder = "CANCEL")
                VInput(value = note, onValueChange = { note = it }, label = "Note", hint = "Optional note", placeholder = "Holiday")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = "Create",
                        onClick = {
                            if (date.isNotBlank()) {
                                onCreate(CreateExceptionRequest(date = date.trim(), kind = kind.trim(), note = note.trim())) {
                                    onDismiss()
                                }
                            }
                        },
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                        loading = isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeRequestCard(
    request: TimetableChangeRequestDto,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    var adminNote by remember { mutableStateOf("") }
    var showActions by remember { mutableStateOf(false) }
    val c = VTheme.colors

    VCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(request.teacherName, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("${request.className} · ${request.subject}", style = VTheme.type.caption.colored(c.ink2))
                }
                VBadge(
                    text = request.status,
                    tone = when (request.status) {
                        "PENDING" -> VBadgeTone.Warning
                        "APPROVED" -> VBadgeTone.Success
                        "REJECTED" -> VBadgeTone.Neutral
                        else -> VBadgeTone.Neutral
                    },
                )
            }

            Text("Day: ${WEEKDAY_LABELS[request.weekday]} ${request.startTime ?: ""}–${request.endTime ?: ""}", style = VTheme.type.caption.colored(c.ink3))

            if (request.reason.isNotBlank()) {
                Text("Reason: ${request.reason}", style = VTheme.type.caption.colored(c.ink3))
            }

            if (request.status == "PENDING") {
                if (showActions) {
                    VInput(value = adminNote, onValueChange = { adminNote = it }, label = "Admin Note", hint = "Optional", placeholder = "Approved/Rejected with note")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = "Approve",
                            onClick = { onApprove(adminNote.trim()) },
                            variant = VButtonVariant.Primary,
                            tone = VButtonTone.Teal,
                        )
                        VButton(
                            text = "Reject",
                            onClick = { onReject(adminNote.trim()) },
                            variant = VButtonVariant.Destructive,
                        )
                    }
                } else {
                    VButton(
                        text = "Review",
                        onClick = { showActions = true },
                        full = true,
                        variant = VButtonVariant.Secondary,
                        tone = VButtonTone.Navy,
                    )
                }
            }
        }
    }
}

