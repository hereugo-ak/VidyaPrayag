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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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

private val SLOT_TYPES = listOf("TEACHING", "BREAK", "ASSEMBLY", "LAB", "OTHER")

private val DEFAULT_TEMPLATE_SLOTS = listOf(
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
)

private val ALL_DAYS = listOf(1, 2, 3, 4, 5, 6, 7)

private fun parseTimeToMinutes(time: String): Int {
    val parts = time.split(":")
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 60 + m
}

private fun minutesToTime(minutes: Int): String {
    val h = (minutes / 60).coerceIn(0, 23)
    val m = (minutes % 60).coerceIn(0, 59)
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

private fun shiftSlotsAfterRemove(
    slots: List<SchoolDaySlotDto>,
    removedIdx: Int,
): List<SchoolDaySlotDto> {
    if (removedIdx < 0 || removedIdx >= slots.size) return slots
    val removed = slots[removedIdx]
    val removedDuration = parseTimeToMinutes(removed.endTime) - parseTimeToMinutes(removed.startTime)
    if (removedDuration <= 0) {
        return slots.toMutableList().also { it.removeAt(removedIdx) }
            .mapIndexed { i, s -> s.copy(slotIndex = i) }
    }
    return slots.toMutableList().also { it.removeAt(removedIdx) }
        .mapIndexed { i, s ->
            if (i >= removedIdx) {
                val newStart = (parseTimeToMinutes(s.startTime) - removedDuration).coerceAtLeast(0)
                val newEnd = (parseTimeToMinutes(s.endTime) - removedDuration).coerceAtLeast(0)
                s.copy(slotIndex = i, startTime = minutesToTime(newStart), endTime = minutesToTime(newEnd))
            } else {
                s.copy(slotIndex = i)
            }
        }
}

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
    var presetSlots by remember { mutableStateOf<List<SchoolDaySlotDto>?>(null) }

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
                onSaved = { savedSlots -> presetSlots = savedSlots },
            )
            1 -> ScheduleStepAssign(
                state = state,
                presetSlots = presetSlots,
                onLoadTimetable = onLoadTimetable,
                onCreatePeriod = onCreatePeriod,
                onBulkCreatePeriods = onBulkCreatePeriods,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleStepStructure(
    onNext: () -> Unit,
    onSaved: (List<SchoolDaySlotDto>) -> Unit = {},
) {
    val dayConfigViewModel: SchoolDayConfigViewModel = koinViewModel()
    val state by dayConfigViewModel.state.collectAsStateV2()
    var templateName by remember { mutableStateOf("Standard Day") }
    var slots by remember { mutableStateOf(DEFAULT_TEMPLATE_SLOTS.toMutableList()) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSectionHeader("Day Structure Template")
        Text("Customize every element below — add, remove, reorder, edit times and labels.", style = VTheme.type.caption.colored(VTheme.colors.ink2))

        // Import button
        VButton(
            text = "📥 Import from Photo / PDF / Text",
            onClick = { showImportDialog = true },
            full = true,
            variant = VButtonVariant.Secondary,
            tone = VButtonTone.Navy,
        )

        // Template name
        VInput(
            value = templateName,
            onValueChange = { templateName = it },
            label = "Template Name",
            hint = "e.g. Standard Day, Half Day, Exam Day",
            placeholder = "Standard Day",
        )

        // Applicable days
        VSectionHeader("Applicable Days")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ALL_DAYS.forEach { day ->
                val isSelected = day in selectedDays
                VBadge(
                    text = WEEKDAY_LABELS[day],
                    tone = if (isSelected) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable {
                        selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                    },
                )
            }
        }

        // Live timeline preview
        VSectionHeader("Live Preview")
        DayTimelinePreview(slots = slots)

        // Editable slots
        VSectionHeader("Slots (${slots.size})")
        slots.forEachIndexed { idx, slot ->
            EditableSlotCard(
                slot = slot,
                canMoveUp = idx > 0,
                canMoveDown = idx < slots.size - 1,
                onMoveUp = {
                    slots = slots.toMutableList().also {
                        val tmp = it[idx - 1]; it[idx - 1] = it[idx]; it[idx] = tmp
                    }
                },
                onMoveDown = {
                    slots = slots.toMutableList().also {
                        val tmp = it[idx + 1]; it[idx + 1] = it[idx]; it[idx] = tmp
                    }
                },
                onRemove = {
                    slots = shiftSlotsAfterRemove(slots, idx).toMutableList()
                },
                onUpdate = { updated ->
                    slots = slots.toMutableList().also { it[idx] = updated }
                },
            )
        }

        // Add slot button
        VButton(
            text = "+ Add Slot",
            onClick = {
                val nextIdx = slots.size
                val lastEnd = slots.lastOrNull()?.endTime ?: "08:00"
                slots = (slots + SchoolDaySlotDto(nextIdx, "TEACHING", "New Period", lastEnd, "${lastEnd.take(2).toIntOrNull()?.plus(1) ?: 9}:00")).toMutableList()
            },
            full = true,
            variant = VButtonVariant.Secondary,
            tone = VButtonTone.Teal,
        )

        // Save & continue
        VButton(
            text = "Save Template & Continue →",
            onClick = {
                val days = selectedDays.sorted().joinToString(",")
                val reindexed = slots.mapIndexed { i, s -> s.copy(slotIndex = i) }
                onSaved(reindexed)
                dayConfigViewModel.createConfig(
                    name = templateName.trim().ifBlank { "Standard Day" },
                    applicableDays = days,
                    classLevel = "ALL",
                    slots = reindexed,
                ) { onNext() }
            },
            full = true,
            variant = VButtonVariant.Primary,
            tone = VButtonTone.Teal,
            loading = state.isSaving,
        )

        // Messages
        state.infoMessage?.let {
            Text(it, style = VTheme.type.caption.colored(VTheme.colors.successInk))
        }
        state.errorMessage?.let {
            Text(it, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
        }

        // Existing configs
        if (state.configs.isNotEmpty()) {
            VSectionHeader("Existing Configurations")
            state.configs.forEach { config ->
                ExistingConfigCard(config = config)
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onParsed = { parsedSlots, parsedName ->
                if (parsedSlots.isNotEmpty()) {
                    slots = parsedSlots.toMutableList()
                    if (parsedName.isNotBlank()) templateName = parsedName
                }
                showImportDialog = false
            },
            dayConfigViewModel = dayConfigViewModel,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onParsed: (List<SchoolDaySlotDto>, String) -> Unit,
    dayConfigViewModel: SchoolDayConfigViewModel,
) {
    var importMode by remember { mutableStateOf<String?>(null) }
    var pastedText by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Import Schedule", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)

                if (importMode == null) {
                    Text("Choose an import source:", style = VTheme.type.caption.colored(VTheme.colors.ink2))

                    // Photo (OCR)
                    VCard(Modifier.fillMaxWidth().clickable { importMode = "photo" }) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(VTheme.colors.teal.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("📷", style = VTheme.type.body)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Photo (OCR)", style = VTheme.type.bodyStrong.colored(VTheme.colors.ink))
                                Text("Take a photo or pick from gallery — text will be extracted automatically.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                            }
                        }
                    }

                    // PDF
                    VCard(Modifier.fillMaxWidth().clickable { importMode = "pdf" }) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(VTheme.colors.accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("📄", style = VTheme.type.body)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("PDF Document", style = VTheme.type.bodyStrong.colored(VTheme.colors.ink))
                                Text("Pick a PDF file — timetable text will be extracted.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                            }
                        }
                    }

                    // Paste Text
                    VCard(Modifier.fillMaxWidth().clickable { importMode = "text" }) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(VTheme.colors.warning.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("📋", style = VTheme.type.body)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Paste Text", style = VTheme.type.bodyStrong.colored(VTheme.colors.ink))
                                Text("Paste timetable text from any source — we'll parse it into slots.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    }
                }

                // Photo / PDF — AI OCR via server
                if (importMode == "photo" || importMode == "pdf") {
                    val label = if (importMode == "photo") "Photo OCR" else "PDF Import"
                    if (isImporting) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("⏳", style = VTheme.type.h1)
                            Text("AI is reading your timetable...", style = VTheme.type.body.colored(VTheme.colors.ink))
                            Text("This uses AI vision to extract text from your image.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                        }
                    } else {
                        VEmptyState(
                            title = "$label — AI Vision OCR",
                            icon = VIcons.FileText,
                            body = "Take a photo or pick an image of a printed timetable. Our AI will extract the schedule automatically.",
                        )
                        parseError?.let {
                            Text(it, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VButton(text = "← Back", onClick = { importMode = null; parseError = null }, variant = VButtonVariant.Ghost)
                            VButton(
                                text = if (importMode == "photo") "Pick Photo" else "Pick PDF",
                                onClick = {
                                    parseError = null
                                    isImporting = true
                                    parseError = "File picker integration requires platform-specific code. Please use 'Paste Text' mode for now — copy text from your photo/PDF and paste it."
                                    isImporting = false
                                },
                                variant = VButtonVariant.Primary,
                                tone = VButtonTone.Teal,
                            )
                            VButton(
                                text = "Use Paste Text Instead",
                                onClick = { importMode = "text"; parseError = null },
                                variant = VButtonVariant.Secondary,
                            )
                        }
                    }
                }

                // Paste Text mode
                if (importMode == "text") {
                    Text("Paste your timetable text below.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                    Text("Supported formats: '08:00-08:40 Period 1' or '08:00 08:40 English' (one slot per line)", style = VTheme.type.caption.colored(VTheme.colors.ink3))
                    VInput(
                        value = pastedText,
                        onValueChange = { pastedText = it; parseError = null },
                        label = "Timetable Text",
                        hint = "One slot per line, e.g.\n08:00-08:40 Period 1\n08:40-09:20 Period 2\n09:20-09:35 Short Break",
                        placeholder = "08:00-08:40 Period 1\n08:40-09:20 Period 2...",
                    )

                    parseError?.let {
                        Text(it, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(text = "← Back", onClick = { importMode = null }, variant = VButtonVariant.Ghost)
                        VButton(
                            text = "Parse & Fill",
                            onClick = {
                                val result = parseTimetableText(pastedText)
                                if (result.first.isEmpty()) {
                                    parseError = "Could not parse any slots. Make sure each line has a time range (e.g. 08:00-08:40) and a label."
                                } else {
                                    onParsed(result.first, result.second)
                                }
                            },
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Teal,
                        )
                        VButton(
                            text = "AI Parse",
                            onClick = {
                                parseError = null
                                isImporting = true
                                dayConfigViewModel.importText(
                                    text = pastedText,
                                    onResult = { slots, name ->
                                        isImporting = false
                                        onParsed(slots, name)
                                    },
                                    onError = { msg ->
                                        isImporting = false
                                        parseError = msg
                                    },
                                )
                            },
                            variant = VButtonVariant.Primary,
                            tone = VButtonTone.Teal,
                            loading = isImporting,
                        )
                    }
                }
            }
        }
    }
}

private val TIME_REGEX = Regex("(\\d{1,2}[:.]\\d{2})\\s*[-–to ]+\\s*(\\d{1,2}[:.]\\d{2})")
private val BREAK_KEYWORDS = setOf("break", "recess", "lunch", "interval", "assembly", "prayer")

private fun parseTimetableText(text: String): Pair<List<SchoolDaySlotDto>, String> {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val slots = mutableListOf<SchoolDaySlotDto>()
    var name = ""

    for ((idx, line) in lines.withIndex()) {
        val match = TIME_REGEX.find(line)
        if (match != null) {
            val startRaw = match.groupValues[1].replace('.', ':')
            val endRaw = match.groupValues[2].replace('.', ':')
            val startTime = normalizeTime(startRaw)
            val endTime = normalizeTime(endRaw)

            // Extract label: everything after the time match
            val afterMatch = line.substring(match.range.last + 1).trim()
                .removePrefix("-").removePrefix("–").trim()
            // Also try before the match (some formats: "Period 1 08:00-08:40")
            val beforeMatch = line.substring(0, match.range.first).trim()
                .removeSuffix("-").removeSuffix("–").trim()

            val label = when {
                afterMatch.isNotBlank() -> afterMatch
                beforeMatch.isNotBlank() -> beforeMatch
                else -> "Slot ${idx + 1}"
            }

            val lowerLabel = label.lowercase()
            val slotType = when {
                BREAK_KEYWORDS.any { lowerLabel.contains(it) } -> {
                    if (lowerLabel.contains("lunch")) "BREAK"
                    else if (lowerLabel.contains("assembly") || lowerLabel.contains("prayer")) "ASSEMBLY"
                    else "BREAK"
                }
                lowerLabel.contains("lab") -> "LAB"
                else -> "TEACHING"
            }

            slots.add(SchoolDaySlotDto(idx, slotType, label, startTime, endTime))
        }
    }

    // Try to detect a name from the first non-time line
    for (line in lines) {
        if (TIME_REGEX.find(line) == null && line.length in 3..40) {
            name = line
            break
        }
    }

    return slots to name
}

private fun normalizeTime(time: String): String {
    val parts = time.split(":")
    if (parts.size == 2) {
        val h = parts[0].padStart(2, '0')
        val m = parts[1].padStart(2, '0')
        return "$h:$m"
    }
    return time
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableSlotCard(
    slot: SchoolDaySlotDto,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: (SchoolDaySlotDto) -> Unit,
) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row: color bar + label input + controls
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(width = 4.dp, height = 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(slotTypeColor(slot.slotType, c)),
                )
                Box(Modifier.weight(1f)) {
                    VInput(
                        value = slot.label,
                        onValueChange = { onUpdate(slot.copy(label = it)) },
                        label = null,
                        hint = null,
                        placeholder = "Slot label",
                    )
                }
                // Move up
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (canMoveUp) c.teal.copy(alpha = 0.1f) else c.cream)
                        .clickable(enabled = canMoveUp) { onMoveUp() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↑", color = if (canMoveUp) c.tealDeep else c.ink3, fontWeight = FontWeight.Bold)
                }
                // Move down
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (canMoveDown) c.teal.copy(alpha = 0.1f) else c.cream)
                        .clickable(enabled = canMoveDown) { onMoveDown() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↓", color = if (canMoveDown) c.tealDeep else c.ink3, fontWeight = FontWeight.Bold)
                }
                // Remove
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(c.dangerInk.copy(alpha = 0.1f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = c.dangerInk, fontWeight = FontWeight.Bold)
                }
            }

            // Type selector chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SLOT_TYPES.forEach { type ->
                    VBadge(
                        text = type,
                        tone = if (slot.slotType == type) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable { onUpdate(slot.copy(slotType = type)) },
                    )
                }
            }

            // Time inputs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    VInput(
                        value = slot.startTime,
                        onValueChange = { onUpdate(slot.copy(startTime = it)) },
                        label = "Start",
                        hint = "HH:mm",
                        placeholder = "08:00",
                    )
                }
                Box(Modifier.weight(1f)) {
                    VInput(
                        value = slot.endTime,
                        onValueChange = { onUpdate(slot.copy(endTime = it)) },
                        label = "End",
                        hint = "HH:mm",
                        placeholder = "08:40",
                    )
                }
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

// ── Step 2: Assign Teachers to Slots (Slot-Driven Grid) ──────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleStepAssign(
    state: ClassesSubjectsState,
    presetSlots: List<SchoolDaySlotDto>? = null,
    onLoadTimetable: (String?) -> Unit,
    onCreatePeriod: (teacherId: String, className: String, section: String, subject: String, weekday: Int, startTime: String, endTime: String, room: String, onDone: () -> Unit) -> Unit,
    onBulkCreatePeriods: (weekday: Int, periods: List<BulkPeriodItem>, onDone: () -> Unit) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val dayConfigViewModel: SchoolDayConfigViewModel = koinViewModel()
    val dayConfigState by dayConfigViewModel.state.collectAsStateV2()
    val activeConfig = dayConfigState.configs.firstOrNull { it.isActive } ?: dayConfigState.configs.firstOrNull()
    val templateSlots = presetSlots?.filter { it.slotType == "TEACHING" }
        ?: activeConfig?.slots?.filter { it.slotType == "TEACHING" }
        ?: emptyList()
    val applicableDayNums = activeConfig?.applicableDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: (1..5).toList()

    var selectedClassName by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf("A") }
    var selectedDay by remember { mutableStateOf(applicableDayNums.firstOrNull() ?: 1) }
    var editingSlot by remember { mutableStateOf<SchoolDaySlotDto?>(null) }
    var editingPeriod by remember { mutableStateOf<TimetablePeriodDto?>(null) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var showCopyDayDialog by remember { mutableStateOf(false) }
    var showCopyClassDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onLoadTimetable(null) }
    LaunchedEffect(state.classes) {
        if (selectedClassName == null && state.classes.isNotEmpty()) {
            selectedClassName = state.classes.first().name
            selectedSection = state.classes.first().sections.firstOrNull() ?: "A"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val c = VTheme.colors
        val tt = state.timetable

        // ── No active day config prompt ──────────────────────────────────────
        if (activeConfig == null) {
            VEmptyState(
                title = "No day structure found",
                icon = VIcons.Calendar,
                body = "You can still add periods manually below, or go back to Step 1 to create a day structure template.",
            )
        }

        // ── Class selector ───────────────────────────────────────────────────
        if (state.classes.isEmpty()) {
            Text("No classes found. Add classes first in the Classes tab.", style = VTheme.type.caption.colored(c.ink2))
        } else {
            val selectedClassDto = state.classes.find { it.name == selectedClassName }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.classes.forEach { cls ->
                    VBadge(
                        text = cls.name,
                        tone = if (selectedClassName == cls.name) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable {
                            selectedClassName = cls.name
                            selectedSection = cls.sections.firstOrNull() ?: "A"
                        },
                    )
                }
            }
            // Section selector (if multiple)
            if (selectedClassDto != null && selectedClassDto.sections.size > 1) {
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedClassDto.sections.forEach { sec ->
                        VBadge(
                            text = sec,
                            tone = if (selectedSection == sec) VBadgeTone.Accent else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { selectedSection = sec },
                        )
                    }
                }
            }
        }

        // ── Day selector ─────────────────────────────────────────────────────
        VSectionHeader("Select Day")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            applicableDayNums.forEach { day ->
                VBadge(
                    text = WEEKDAY_LABELS[day],
                    tone = if (selectedDay == day) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { selectedDay = day },
                )
            }
        }

        // ── Slot grid ────────────────────────────────────────────────────────
        VSectionHeader("${WEEKDAY_LABELS[selectedDay]} — ${selectedClassName ?: "—"} · $selectedSection")

        // Find existing periods for this class+section+day
        val dayData = tt?.weekdays?.find { it.weekday == selectedDay }
        val existingPeriods = dayData?.periods?.filter {
            it.className.equals(selectedClassName, ignoreCase = true) && it.section.equals(selectedSection, ignoreCase = true)
        } ?: emptyList()

        if (templateSlots.isEmpty()) {
            // Manual mode — no DayConfig slots, show existing periods + manual add
            if (existingPeriods.isEmpty()) {
                VEmptyState(
                    title = "No periods yet",
                    icon = VIcons.Calendar,
                    body = "Tap \"Add Period\" below to assign a teacher and subject to this day.",
                )
            } else {
                Text(
                    "${existingPeriods.size} period${if (existingPeriods.size > 1) "s" else ""} on ${WEEKDAY_LABELS[selectedDay]}",
                    style = VTheme.type.caption.colored(c.ink2),
                )
                existingPeriods.sortedBy { it.startTime }.forEach { period ->
                    ManualPeriodRow(
                        period = period,
                        onEdit = {
                            editingPeriod = period
                            showManualAddDialog = true
                        },
                        onDelete = { deleteTargetId = period.id },
                    )
                }
            }
            VButton(
                text = "+ Add Period",
                onClick = {
                    editingPeriod = null
                    showManualAddDialog = true
                },
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
        } else {
            // Slot-driven mode — DayConfig has TEACHING slots
            // Stats
            val assignedCount = templateSlots.count { slot ->
                existingPeriods.any { it.startTime == slot.startTime && it.endTime == slot.endTime }
            }
            Text(
                "$assignedCount of ${templateSlots.size} slots assigned",
                style = VTheme.type.caption.colored(c.ink2),
            )

            // Slot rows
            templateSlots.forEach { slot ->
                val matchedPeriod = existingPeriods.find { it.startTime == slot.startTime && it.endTime == slot.endTime }
                SlotAssignmentRow(
                    slot = slot,
                    period = matchedPeriod,
                    onTap = {
                        editingSlot = slot
                        editingPeriod = matchedPeriod
                    },
                    onDelete = { matchedPeriod?.let { deleteTargetId = it.id } },
                )
            }
            // Also show any periods that don't match any slot (orphaned)
            val orphanedPeriods = existingPeriods.filter { p ->
                templateSlots.none { it.startTime == p.startTime && it.endTime == p.endTime }
            }
            if (orphanedPeriods.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Other periods (not in day structure)", style = VTheme.type.caption.colored(c.ink3))
                orphanedPeriods.forEach { period ->
                    ManualPeriodRow(
                        period = period,
                        onEdit = {
                            editingPeriod = period
                            showManualAddDialog = true
                        },
                        onDelete = { deleteTargetId = period.id },
                    )
                }
            }
        }

        // ── Bulk operations ──────────────────────────────────────────────────
        VSectionHeader("Quick Actions")
        VButton(
            text = "Copy ${WEEKDAY_LABELS[selectedDay]} to All Days",
            onClick = { showCopyDayDialog = true },
            full = true,
            variant = VButtonVariant.Secondary,
            tone = VButtonTone.Navy,
        )
        VButton(
            text = "Copy from Another Class",
            onClick = { showCopyClassDialog = true },
            full = true,
            variant = VButtonVariant.Secondary,
            tone = VButtonTone.Navy,
        )

        // ── Messages ─────────────────────────────────────────────────────────
        state.infoMessage?.let {
            Text(it, style = VTheme.type.caption.colored(c.successInk))
        }
        state.errorMessage?.let {
            Text(it, style = VTheme.type.caption.colored(c.dangerInk))
        }

        // ── Navigation ───────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VButton(text = "← Back", onClick = onBack, full = true, variant = VButtonVariant.Secondary, tone = VButtonTone.Navy)
            }
            Box(Modifier.weight(1f)) {
                VButton(text = "Review →", onClick = onNext, full = true, variant = VButtonVariant.Primary, tone = VButtonTone.Teal)
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // ── Slot assignment editor dialog ─────────────────────────────────────
    editingSlot?.let { slot ->
        SlotAssignmentEditorDialog(
            state = state,
            slot = slot,
            editingPeriod = editingPeriod,
            className = selectedClassName ?: "",
            section = selectedSection,
            weekday = selectedDay,
            isSaving = state.isSaving,
            onSave = { teacherId, subject, room ->
                if (editingPeriod != null) {
                    onDeletePeriod(editingPeriod!!.id)
                }
                onCreatePeriod(teacherId, selectedClassName ?: "", selectedSection, subject, selectedDay, slot.startTime, slot.endTime, room) {
                    editingSlot = null
                    editingPeriod = null
                }
            },
            onRemove = {
                editingPeriod?.let { onDeletePeriod(it.id) }
                editingSlot = null
                editingPeriod = null
            },
            onDismiss = { editingSlot = null; editingPeriod = null },
            onCreateTeacherInline = onCreateTeacherInline,
            onCreateSubjectInline = onCreateSubjectInline,
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────
    deleteTargetId?.let { id ->
        VConfirmDialog(
            visible = true,
            title = "Remove assignment?",
            message = "This will remove the teacher from this slot.",
            confirmLabel = "Remove",
            onConfirm = { onDeletePeriod(id); deleteTargetId = null },
            onDismiss = { deleteTargetId = null },
        )
    }

    // ── Copy day dialog ───────────────────────────────────────────────────
    if (showCopyDayDialog) {
        CopyDayConfirmDialog(
            sourceDay = selectedDay,
            targetDays = applicableDayNums.filter { it != selectedDay },
            onConfirm = { targetDays ->
                showCopyDayDialog = false
                val tt2 = state.timetable
                val dayData = tt2?.weekdays?.find { it.weekday == selectedDay }
                val periodsToCopy = dayData?.periods?.filter {
                    it.className.equals(selectedClassName, ignoreCase = true) && it.section.equals(selectedSection, ignoreCase = true)
                } ?: emptyList()
                if (periodsToCopy.isNotEmpty()) {
                    targetDays.forEach { targetDay ->
                        val bulkItems = periodsToCopy.map { p ->
                            BulkPeriodItem(
                                teacherId = p.teacherId,
                                className = p.className,
                                section = p.section,
                                subject = p.subject,
                                startTime = p.startTime,
                                endTime = p.endTime,
                                room = p.room,
                            )
                        }
                        onBulkCreatePeriods(targetDay, bulkItems) {}
                    }
                }
            },
            onDismiss = { showCopyDayDialog = false },
        )
    }

    // ── Copy class dialog ─────────────────────────────────────────────────
    if (showCopyClassDialog) {
        CopyClassConfirmDialog(
            targetClassName = selectedClassName ?: "",
            sourceClasses = state.classes.map { it.name }.filter { it != selectedClassName },
            onConfirm = { sourceClass ->
                showCopyClassDialog = false
                val tt2 = state.timetable
                applicableDayNums.forEach { day ->
                    val dayData = tt2?.weekdays?.find { it.weekday == day }
                    val sourcePeriods = dayData?.periods?.filter {
                        it.className.equals(sourceClass, ignoreCase = true)
                    } ?: emptyList()
                    if (sourcePeriods.isNotEmpty()) {
                        val bulkItems = sourcePeriods.map { p ->
                            BulkPeriodItem(
                                teacherId = p.teacherId,
                                className = selectedClassName ?: "",
                                section = selectedSection,
                                subject = p.subject,
                                startTime = p.startTime,
                                endTime = p.endTime,
                                room = p.room,
                            )
                        }
                        onBulkCreatePeriods(day, bulkItems) {}
                    }
                }
            },
            onDismiss = { showCopyClassDialog = false },
        )
    }

    // ── Manual period editor dialog (no-slot fallback) ────────────────────
    if (showManualAddDialog) {
        ManualPeriodEditorDialog(
            state = state,
            editingPeriod = editingPeriod,
            className = selectedClassName ?: "",
            section = selectedSection,
            weekday = selectedDay,
            isSaving = state.isSaving,
            onSave = { teacherId, subject, startTime, endTime, room ->
                if (editingPeriod != null) {
                    onDeletePeriod(editingPeriod!!.id)
                }
                onCreatePeriod(teacherId, selectedClassName ?: "", selectedSection, subject, selectedDay, startTime, endTime, room) {
                    showManualAddDialog = false
                    editingPeriod = null
                }
            },
            onRemove = {
                editingPeriod?.let { onDeletePeriod(it.id) }
                showManualAddDialog = false
                editingPeriod = null
            },
            onDismiss = { showManualAddDialog = false; editingPeriod = null },
            onCreateTeacherInline = onCreateTeacherInline,
            onCreateSubjectInline = onCreateSubjectInline,
        )
    }
}

@Composable
private fun SlotAssignmentRow(
    slot: SchoolDaySlotDto,
    period: TimetablePeriodDto?,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        Modifier.fillMaxWidth().clickable { onTap() },
    ) {
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
                Text(slot.startTime, style = VTheme.type.bodyStrong.colored(c.ink))
                Text("↓", style = VTheme.type.caption.colored(c.ink3))
                Text(slot.endTime, style = VTheme.type.body.colored(c.ink2))
            }

            // Divider
            Box(Modifier.width(1.dp).height(48.dp).background(c.hairline))

            // Content
            Column(Modifier.weight(1f)) {
                Text(
                    slot.label.ifBlank { "Slot ${slot.slotIndex + 1}" },
                    style = VTheme.type.caption.colored(c.ink3),
                )
                if (period != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(period.subject, style = VTheme.type.bodyStrong.colored(c.ink))
                    if (period.teacherName.isNotBlank()) {
                        Text(period.teacherName, style = VTheme.type.caption.colored(c.ink2))
                    }
                    if (period.room.isNotBlank()) {
                        Text("Room ${period.room}", style = VTheme.type.caption.colored(c.ink3))
                    }
                } else {
                    Text("Tap to assign teacher & subject", style = VTheme.type.caption.colored(c.placeholder))
                }
            }

            // Status badge + delete
            if (period != null) {
                VBadge(text = "Assigned", tone = VBadgeTone.Success)
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(c.dangerInk.copy(alpha = 0.1f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = c.dangerInk, fontWeight = FontWeight.Bold)
                }
            } else {
                VBadge(text = "Empty", tone = VBadgeTone.Neutral)
            }
        }
    }
}

@Composable
private fun ManualPeriodRow(
    period: TimetablePeriodDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        Modifier.fillMaxWidth().clickable { onEdit() },
    ) {
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

            Box(Modifier.width(1.dp).height(48.dp).background(c.hairline))

            // Content
            Column(Modifier.weight(1f)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualPeriodEditorDialog(
    state: ClassesSubjectsState,
    editingPeriod: TimetablePeriodDto?,
    className: String,
    section: String,
    weekday: Int,
    isSaving: Boolean,
    onSave: (teacherId: String, subject: String, startTime: String, endTime: String, room: String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
) {
    val teachers = state.teachers
    val classes = state.classes
    var selectedTeacherId by remember { mutableStateOf(editingPeriod?.teacherId ?: "") }
    var subjectName by remember { mutableStateOf(editingPeriod?.subject ?: "") }
    var startTime by remember { mutableStateOf(editingPeriod?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(editingPeriod?.endTime ?: "10:00") }
    var room by remember { mutableStateOf(editingPeriod?.room ?: "") }
    var showNewTeacher by remember { mutableStateOf(false) }
    var showNewSubject by remember { mutableStateOf(false) }

    val selectedClass = classes.find { it.name == className }
    val subjectsForClass = selectedClass?.let { state.subjectsByClass[it.id] } ?: emptyList()

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header
                Text(
                    "${WEEKDAY_LABELS[weekday]} — $className · $section",
                    style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink,
                )

                // Time inputs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        VInput(value = startTime, onValueChange = { startTime = it }, label = "Start", hint = "HH:mm", placeholder = "09:00")
                    }
                    Box(Modifier.weight(1f)) {
                        VInput(value = endTime, onValueChange = { endTime = it }, label = "End", hint = "HH:mm", placeholder = "10:00")
                    }
                }

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

                // Room
                VInput(value = room, onValueChange = { room = it }, label = "Room", hint = "e.g. 101", placeholder = "101")

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    if (editingPeriod != null) {
                        VButton(
                            text = "Remove",
                            onClick = onRemove,
                            variant = VButtonVariant.Destructive,
                        )
                    }
                    VButton(
                        text = if (editingPeriod != null) "Update" else "Add Period",
                        onClick = {
                            val tid = selectedTeacherId.ifBlank { return@VButton }
                            val sn = subjectName.trim().ifBlank { return@VButton }
                            val st = startTime.trim().ifBlank { return@VButton }
                            val et = endTime.trim().ifBlank { return@VButton }
                            onSave(tid, sn, st, et, room.trim())
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotAssignmentEditorDialog(
    state: ClassesSubjectsState,
    slot: SchoolDaySlotDto,
    editingPeriod: TimetablePeriodDto?,
    className: String,
    section: String,
    weekday: Int,
    isSaving: Boolean,
    onSave: (teacherId: String, subject: String, room: String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
) {
    val teachers = state.teachers
    val classes = state.classes
    var selectedTeacherId by remember { mutableStateOf(editingPeriod?.teacherId ?: "") }
    var subjectName by remember { mutableStateOf(editingPeriod?.subject ?: "") }
    var room by remember { mutableStateOf(editingPeriod?.room ?: "") }
    var showNewTeacher by remember { mutableStateOf(false) }
    var showNewSubject by remember { mutableStateOf(false) }

    val selectedClass = classes.find { it.name == className }
    val subjectsForClass = selectedClass?.let { state.subjectsByClass[it.id] } ?: emptyList()

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header
                Text(
                    "${slot.label.ifBlank { "Slot ${slot.slotIndex + 1}" }} — ${WEEKDAY_LABELS[weekday]}",
                    style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink,
                )
                Text(
                    "${slot.startTime} – ${slot.endTime} · $className · $section",
                    style = VTheme.type.caption.colored(VTheme.colors.ink2),
                )

                // Teacher dropdown
                Text("Teacher", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                var teacherMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = teachers.find { it.id == selectedTeacherId }?.let { t -> t.profile.name.ifBlank { t.id.take(8) } } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Teacher") },
                        modifier = Modifier.fillMaxWidth().clickable { teacherMenuExpanded = true },
                        enabled = false,
                    )
                    DropdownMenu(
                        expanded = teacherMenuExpanded,
                        onDismissRequest = { teacherMenuExpanded = false },
                    ) {
                        teachers.forEach { teacher ->
                            DropdownMenuItem(
                                text = { Text(teacher.profile.name.ifBlank { teacher.id.take(8) }) },
                                onClick = {
                                    selectedTeacherId = teacher.id
                                    teacherMenuExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Add New Teacher", color = VTheme.colors.tealDeep, fontWeight = FontWeight.Bold) },
                            onClick = {
                                teacherMenuExpanded = false
                                showNewTeacher = true
                            },
                        )
                    }
                }

                // Subject dropdown
                Text("Subject", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                var subjectMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Subject") },
                        modifier = Modifier.fillMaxWidth().clickable { subjectMenuExpanded = true },
                        enabled = false,
                    )
                    DropdownMenu(
                        expanded = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false },
                    ) {
                        subjectsForClass.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    subjectName = sub.name
                                    subjectMenuExpanded = false
                                },
                            )
                        }
                        if (selectedClass != null) {
                            DropdownMenuItem(
                                text = { Text("+ Add New Subject", color = VTheme.colors.tealDeep, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    subjectMenuExpanded = false
                                    showNewSubject = true
                                },
                            )
                        }
                    }
                }

                // Room
                VInput(value = room, onValueChange = { room = it }, label = "Room", hint = "e.g. 101", placeholder = "101")

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    if (editingPeriod != null) {
                        VButton(
                            text = "Remove",
                            onClick = onRemove,
                            variant = VButtonVariant.Destructive,
                        )
                    }
                    VButton(
                        text = if (editingPeriod != null) "Update" else "Assign",
                        onClick = {
                            val tid = selectedTeacherId.ifBlank { return@VButton }
                            val sn = subjectName.trim().ifBlank { return@VButton }
                            onSave(tid, sn, room.trim())
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

@Composable
private fun CopyDayConfirmDialog(
    sourceDay: Int,
    targetDays: List<Int>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val targetLabels = targetDays.joinToString(", ") { WEEKDAY_LABELS[it] }
    VConfirmDialog(
        visible = true,
        title = "Copy ${WEEKDAY_LABELS[sourceDay]} to all days?",
        message = "This will copy all assignments from ${WEEKDAY_LABELS[sourceDay]} to: $targetLabels.",
        confirmLabel = "Copy",
        onConfirm = { onConfirm(targetDays) },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CopyClassConfirmDialog(
    targetClassName: String,
    sourceClasses: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSource by remember { mutableStateOf(sourceClasses.firstOrNull() ?: "") }
    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Copy from Another Class", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
                Text("Copy all periods from a source class to $targetClassName across all days.", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                if (sourceClasses.isEmpty()) {
                    Text("No other classes available to copy from.", style = VTheme.type.caption.colored(VTheme.colors.ink3))
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sourceClasses.forEach { cls ->
                            VBadge(
                                text = cls,
                                tone = if (selectedSource == cls) VBadgeTone.Arctic else VBadgeTone.Neutral,
                                modifier = Modifier.clickable { selectedSource = cls },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                        VButton(
                            text = "Copy",
                            onClick = { if (selectedSource.isNotBlank()) onConfirm(selectedSource) },
                            variant = VButtonVariant.Primary,
                            tone = VButtonTone.Teal,
                        )
                    }
                }
            }
        }
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
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
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
            // Coverage stats
            val allPeriods = tt.weekdays.flatMap { it.periods }
            val uniqueClasses = allPeriods.map { "${it.className}-${it.section}" }.distinct()
            val uniqueTeachers = allPeriods.map { it.teacherName }.filter { it.isNotBlank() }.distinct()
            val daysWithPeriods = tt.weekdays.count { it.periods.isNotEmpty() }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    VCard(Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${allPeriods.size}", style = VTheme.type.h2, color = VTheme.colors.tealDeep)
                            Text("Periods", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    VCard(Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uniqueClasses.size}", style = VTheme.type.h2, color = VTheme.colors.tealDeep)
                            Text("Classes", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    VCard(Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uniqueTeachers.size}", style = VTheme.type.h2, color = VTheme.colors.tealDeep)
                            Text("Teachers", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    VCard(Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$daysWithPeriods", style = VTheme.type.h2, color = VTheme.colors.tealDeep)
                            Text("Days", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                        }
                    }
                }
            }

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

