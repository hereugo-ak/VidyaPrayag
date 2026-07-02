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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkPeriodItem
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateExceptionRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodExceptionDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TeacherCardDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetablePeriodDto
import com.littlebridge.enrollplus.feature.admin.presentation.ClassesSubjectsState
import com.littlebridge.enrollplus.feature.admin.presentation.ClassesSubjectsViewModel
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
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

private val TABS = listOf("Classes", "Subjects", "Schedule", "Exceptions & Requests")
private val WEEKDAY_NAMES = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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

// ── Schedule Tab (merged Bell Schedule + Timetable) ──────────────────────────

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
    var scheduleMode by remember { mutableStateOf("structure") }

    Column(Modifier.fillMaxSize()) {
        // Mode toggle: Day Structure | Assignments
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("structure" to "Day Structure", "assignments" to "Assignments").forEach { (mode, label) ->
                VBadge(
                    text = label,
                    tone = if (scheduleMode == mode) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable { scheduleMode = mode },
                )
            }
        }

        when (scheduleMode) {
            "structure" -> SchoolDayConfigEmbeddedV2(
                modifier = Modifier.fillMaxSize(),
            )
            "assignments" -> TimetableAssignmentsContent(
                state = state,
                onLoadTimetable = onLoadTimetable,
                onCreatePeriod = onCreatePeriod,
                onBulkCreatePeriods = onBulkCreatePeriods,
                onDeletePeriod = onDeletePeriod,
                onCreateTeacherInline = onCreateTeacherInline,
                onCreateSubjectInline = onCreateSubjectInline,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableAssignmentsContent(
    state: ClassesSubjectsState,
    onLoadTimetable: (String?) -> Unit,
    onCreatePeriod: (teacherId: String, className: String, section: String, subject: String, weekday: Int, startTime: String, endTime: String, room: String, onDone: () -> Unit) -> Unit,
    onBulkCreatePeriods: (weekday: Int, periods: List<BulkPeriodItem>, onDone: () -> Unit) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
) {
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableStateOf(1) }
    var showCellEditor by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var showBulkEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onLoadTimetable(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSectionHeader("Timetable Grid")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VButton(
                text = "Refresh",
                onClick = { onLoadTimetable(selectedClassFilter) },
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Teal,
            )
            VButton(
                text = "Bulk Fill Day",
                onClick = { showBulkEditor = true },
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Teal,
            )
        }

        val tt = state.timetable
        if (tt != null && tt.classes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VBadge(
                    text = "All",
                    tone = if (selectedClassFilter == null) VBadgeTone.Arctic else VBadgeTone.Neutral,
                    modifier = Modifier.clickable {
                        selectedClassFilter = null
                        onLoadTimetable(null)
                    },
                )
                tt.classes.forEach { cls ->
                    VBadge(
                        text = cls,
                        tone = if (selectedClassFilter == cls) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable {
                            selectedClassFilter = cls
                            onLoadTimetable(cls)
                        },
                    )
                }
            }
        }

        if (tt == null || tt.weekdays.isEmpty()) {
            VEmptyState(
                title = "No timetable yet",
                icon = VIcons.Calendar,
                body = "Select a day and tap a time slot to start building your weekly schedule.",
            )
        } else {
            Text("Select Day", style = VTheme.type.body, fontWeight = FontWeight.Medium, color = VTheme.colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..7).forEach { day ->
                    VBadge(
                        text = WEEKDAY_NAMES[day],
                        tone = if (selectedDay == day) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable { selectedDay = day },
                    )
                }
            }

            val dayData = tt.weekdays.find { it.weekday == selectedDay }
            if (dayData == null || dayData.periods.isEmpty()) {
                VEmptyState(
                    title = "No periods for ${WEEKDAY_NAMES[selectedDay]}",
                    icon = VIcons.Calendar,
                    body = "Tap 'Add Period' to start filling this day.",
                )
            } else {
                dayData.periods.forEach { period ->
                    TimetablePeriodRow(
                        period = period,
                        onDelete = { deleteTargetId = period.id },
                    )
                }
            }

            VButton(
                text = "+ Add Period",
                onClick = { showCellEditor = true },
                full = true,
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
        }

        val info = state.infoMessage
        if (info != null) {
            Text(info, style = VTheme.type.caption.colored(VTheme.colors.successInk))
        }
        val err = state.errorMessage
        if (err != null) {
            Text(err, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showCellEditor) {
        CellEditorDialog(
            state = state,
            weekday = selectedDay,
            isSaving = state.isSaving,
            onSave = { teacherId, className, section, subject, startTime, endTime, room ->
                onCreatePeriod(teacherId, className, section, subject, selectedDay, startTime, endTime, room) {
                    showCellEditor = false
                }
            },
            onDismiss = { showCellEditor = false },
            onCreateTeacherInline = onCreateTeacherInline,
            onCreateSubjectInline = onCreateSubjectInline,
        )
    }

    if (showBulkEditor) {
        BulkDayEditorDialog(
            state = state,
            weekday = selectedDay,
            isSaving = state.isSaving,
            onSave = { periods ->
                onBulkCreatePeriods(selectedDay, periods) { showBulkEditor = false }
            },
            onDismiss = { showBulkEditor = false },
            onCreateTeacherInline = onCreateTeacherInline,
        )
    }

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
private fun TimetablePeriodRow(
    period: TimetablePeriodDto,
    onDelete: () -> Unit = {},
) {
    VCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${period.startTime} – ${period.endTime}",
                        style = VTheme.type.body,
                        fontWeight = FontWeight.Medium,
                        color = VTheme.colors.ink,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    VBadge(text = period.className, tone = VBadgeTone.Arctic)
                    if (period.section.isNotBlank()) VBadge(text = period.section, tone = VBadgeTone.Accent)
                    Text(period.subject, style = VTheme.type.caption, color = VTheme.colors.ink2)
                    if (period.teacherName.isNotBlank()) {
                        Text("• ${period.teacherName}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (period.room.isNotBlank()) {
                    VBadge(text = "Room ${period.room}", tone = VBadgeTone.Success)
                }
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
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

// ── Cell Editor Dialog (single period with dropdowns) ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CellEditorDialog(
    state: ClassesSubjectsState,
    weekday: Int,
    isSaving: Boolean,
    onSave: (teacherId: String, className: String, section: String, subject: String, startTime: String, endTime: String, room: String) -> Unit,
    onDismiss: () -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
    onCreateSubjectInline: (classId: String, name: String, code: String, onDone: () -> Unit) -> Unit,
) {
    var selectedTeacherId by remember { mutableStateOf<String?>(null) }
    var selectedClassName by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf("A") }
    var subjectName by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("") }
    var showNewTeacher by remember { mutableStateOf(false) }
    var showNewSubject by remember { mutableStateOf(false) }

    val teachers = state.teachers
    val classes = state.classes
    val selectedClass = classes.find { it.name == selectedClassName }
    val subjectsForClass = selectedClass?.let { state.subjectsByClass[it.id] } ?: emptyList()

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Period — ${WEEKDAY_NAMES[weekday]}", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)

                Text("Teacher", style = VTheme.type.caption, color = VTheme.colors.ink2)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    teachers.forEach { teacher ->
                        VBadge(
                            text = teacher.profile.name.ifBlank { teacher.id.take(8) },
                            tone = if (selectedTeacherId == teacher.id) VBadgeTone.Arctic else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { selectedTeacherId = teacher.id },
                        )
                    }
                    VBadge(
                        text = "+ New",
                        tone = VBadgeTone.Success,
                        modifier = Modifier.clickable { showNewTeacher = true },
                    )
                }

                Text("Class", style = VTheme.type.caption, color = VTheme.colors.ink2)
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

                if (selectedClass != null && selectedClass.sections.size > 1) {
                    Text("Section", style = VTheme.type.caption, color = VTheme.colors.ink2)
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

                Text("Subject", style = VTheme.type.caption, color = VTheme.colors.ink2)
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

                VInput(value = startTime, onValueChange = { startTime = it }, label = "Start Time", hint = "HH:mm", placeholder = "09:00")
                VInput(value = endTime, onValueChange = { endTime = it }, label = "End Time", hint = "HH:mm", placeholder = "10:00")
                VInput(value = room, onValueChange = { room = it }, label = "Room", hint = "e.g. 101", placeholder = "101")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = "Save Period",
                        onClick = {
                            val tid = selectedTeacherId ?: return@VButton
                            val cn = selectedClassName ?: return@VButton
                            val sn = subjectName.trim().ifBlank { return@VButton }
                            onSave(tid, cn, selectedSection, sn, startTime.trim(), endTime.trim(), room.trim())
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
                    selectedTeacherId = state.teachers.lastOrNull()?.id
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

// ── Bulk Day Editor Dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkDayEditorDialog(
    state: ClassesSubjectsState,
    weekday: Int,
    isSaving: Boolean,
    onSave: (List<BulkPeriodItem>) -> Unit,
    onDismiss: () -> Unit,
    onCreateTeacherInline: (name: String, identifier: String, onDone: () -> Unit) -> Unit,
) {
    val teachers = state.teachers
    val classes = state.classes
    var periods by remember { mutableStateOf(listOf(BulkPeriodRow())) }
    var showNewTeacher by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bulk Fill — ${WEEKDAY_NAMES[weekday]}", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)

                periods.forEachIndexed { index, row ->
                    BulkPeriodRowEditor(
                        row = row,
                        teachers = teachers,
                        classes = classes,
                        subjectsByClass = state.subjectsByClass,
                        onUpdate = { updated ->
                            periods = periods.toMutableList().also { it[index] = updated }
                        },
                        onRemove = {
                            periods = periods.toMutableList().also { it.removeAt(index) }
                        },
                        canRemove = periods.size > 1,
                    )
                }

                VButton(
                    text = "+ Add Row",
                    onClick = { periods = periods + BulkPeriodRow() },
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Teal,
                )

                VBadge(
                    text = "+ New Teacher",
                    tone = VBadgeTone.Success,
                    modifier = Modifier.clickable { showNewTeacher = true },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = "Save All (${periods.size})",
                        onClick = {
                            val items = periods.mapNotNull { row ->
                                val tid = row.teacherId ?: return@mapNotNull null
                                val cn = row.className ?: return@mapNotNull null
                                val sn = row.subject.trim().ifBlank { return@mapNotNull null }
                                BulkPeriodItem(
                                    teacherId = tid,
                                    className = cn,
                                    section = row.section,
                                    subject = sn,
                                    startTime = row.startTime.trim(),
                                    endTime = row.endTime.trim(),
                                    room = row.room.trim(),
                                )
                            }
                            if (items.isNotEmpty()) onSave(items)
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
                }
            },
            onDismiss = { showNewTeacher = false },
        )
    }
}

private data class BulkPeriodRow(
    val teacherId: String? = null,
    val className: String? = null,
    val section: String = "A",
    val subject: String = "",
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val room: String = "",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkPeriodRowEditor(
    row: BulkPeriodRow,
    teachers: List<TeacherCardDto>,
    classes: List<SchoolClassDto>,
    subjectsByClass: Map<String, List<SchoolSubjectDto>>,
    onUpdate: (BulkPeriodRow) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    val selectedClass = classes.find { it.name == row.className }
    val subjectsForClass = selectedClass?.let { subjectsByClass[it.id] } ?: emptyList()

    VCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                teachers.forEach { teacher ->
                    VBadge(
                        text = teacher.profile.name.ifBlank { teacher.id.take(8) },
                        tone = if (row.teacherId == teacher.id) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable { onUpdate(row.copy(teacherId = teacher.id)) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                classes.forEach { cls ->
                    VBadge(
                        text = cls.name,
                        tone = if (row.className == cls.name) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable {
                            onUpdate(row.copy(className = cls.name, section = cls.sections.firstOrNull() ?: "A", subject = ""))
                        },
                    )
                }
            }
            if (selectedClass != null && selectedClass.sections.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    selectedClass.sections.forEach { sec ->
                        VBadge(
                            text = sec,
                            tone = if (row.section == sec) VBadgeTone.Accent else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { onUpdate(row.copy(section = sec)) },
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                subjectsForClass.forEach { sub ->
                    VBadge(
                        text = sub.name,
                        tone = if (row.subject == sub.name) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable { onUpdate(row.copy(subject = sub.name)) },
                    )
                }
            }
            if (row.subject.isBlank() && subjectsForClass.isEmpty() && selectedClass != null) {
                VInput(
                    value = row.subject,
                    onValueChange = { onUpdate(row.copy(subject = it)) },
                    label = "Subject",
                    hint = "Type subject name",
                    placeholder = "e.g. Physics",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VInput(value = row.startTime, onValueChange = { onUpdate(row.copy(startTime = it)) }, label = "Start", hint = "HH:mm", placeholder = "09:00")
                VInput(value = row.endTime, onValueChange = { onUpdate(row.copy(endTime = it)) }, label = "End", hint = "HH:mm", placeholder = "10:00")
            }
            VInput(value = row.room, onValueChange = { onUpdate(row.copy(room = it)) }, label = "Room", hint = "e.g. 101", placeholder = "101")
            if (canRemove) {
                VButton(text = "Remove", onClick = onRemove, variant = VButtonVariant.Destructive, full = true)
            }
        }
    }
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
        // View toggle: Exceptions | Pending | Approved | Rejected
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(
                    text = "Load",
                    onClick = { onLoadExceptions(null) },
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                )
                VButton(
                    text = "Add Exception",
                    onClick = { showAddException = true },
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Teal,
                )
            }
            if (state.exceptions.isEmpty() && !state.isLoading) {
                VEmptyState(
                    title = "No exceptions",
                    icon = VIcons.Calendar,
                    body = "Tap 'Load' to view period overrides, or create one.",
                )
            }
            state.exceptions.forEach { exc ->
                ExceptionCard(
                    exception = exc,
                    onDelete = { deleteExceptionTargetId = exc.id },
                )
            }
        } else {
            VSectionHeader("Timetable Change Requests")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(
                    text = "Load",
                    onClick = { onLoadRequests(statusFilter) },
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                )
            }
            if (state.changeRequests.isEmpty() && !state.isLoading) {
                VEmptyState(
                    title = "No change requests",
                    icon = VIcons.Calendar,
                    body = "Tap 'Load' to view teacher timetable change requests.",
                )
            }
            state.changeRequests.forEach { req ->
                ChangeRequestCard(
                    request = req,
                    onApprove = { note -> onApprove(req.id, note) },
                    onReject = { note -> onReject(req.id, note) },
                )
            }
        }

        val info = state.infoMessage
        if (info != null) {
            Text(info, style = VTheme.type.caption.colored(VTheme.colors.successInk))
        }
        val err = state.errorMessage
        if (err != null) {
            Text(err, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showAddException) {
        ExceptionEditDialog(
            isSaving = state.isSaving,
            onSave = { req ->
                onCreateException(req) { showAddException = false }
            },
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

@Composable
private fun ExceptionCard(
    exception: PeriodExceptionDto,
    onDelete: () -> Unit,
) {
    VCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VBadge(text = exception.kind, tone = when (exception.kind) {
                        "CANCELLED" -> VBadgeTone.Danger
                        "RESCHEDULED" -> VBadgeTone.Warning
                        "ROOM_CHANGE" -> VBadgeTone.Accent
                        "SUBSTITUTION" -> VBadgeTone.Arctic
                        "EXTRA" -> VBadgeTone.Success
                        else -> VBadgeTone.Neutral
                    })
                    Text(exception.date, style = VTheme.type.body, fontWeight = FontWeight.Medium, color = VTheme.colors.ink)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (exception.className.isNotBlank()) VBadge(text = exception.className, tone = VBadgeTone.Arctic)
                    if (exception.section.isNotBlank()) VBadge(text = exception.section, tone = VBadgeTone.Accent)
                    if (exception.subject.isNotBlank()) Text(exception.subject, style = VTheme.type.caption, color = VTheme.colors.ink2)
                }
                if (exception.newStart != null) {
                    Text("New time: ${exception.newStart}${exception.newEnd?.let { " – $it" } ?: ""}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                }
                if (exception.newRoom != null) {
                    Text("New room: ${exception.newRoom}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                }
                if (exception.substituteTeacherName != null) {
                    Text("Substitute: ${exception.substituteTeacherName}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                }
                if (exception.note.isNotBlank()) {
                    Text(exception.note, style = VTheme.type.caption, color = VTheme.colors.ink3)
                }
            }
            Box(
                Modifier.size(28.dp).clip(CircleShape)
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
private fun ExceptionEditDialog(
    isSaving: Boolean,
    onSave: (CreateExceptionRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var periodId by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("CANCELLED") }
    var newStart by remember { mutableStateOf("") }
    var newEnd by remember { mutableStateOf("") }
    var newRoom by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val kinds = listOf("CANCELLED", "RESCHEDULED", "ROOM_CHANGE", "SUBSTITUTION", "EXTRA")

    Dialog(onDismissRequest = onDismiss) {
    VCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Exception", style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink)
            VInput(value = periodId, onValueChange = { periodId = it }, label = "Period ID", hint = "Recurring period UUID (blank for EXTRA)", placeholder = "uuid...")
            VInput(value = date, onValueChange = { date = it }, label = "Date", hint = "YYYY-MM-DD", placeholder = "2026-07-15")
            Text("Kind: $kind", style = VTheme.type.body, color = VTheme.colors.ink2)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                kinds.forEach { k ->
                    VBadge(
                        text = k,
                        tone = if (kind == k) VBadgeTone.Arctic else VBadgeTone.Neutral,
                        modifier = Modifier.clickable { kind = k },
                    )
                }
            }
            if (kind != "CANCELLED") {
                VInput(value = newStart, onValueChange = { newStart = it }, label = "New Start", hint = "HH:mm", placeholder = "09:00")
                VInput(value = newEnd, onValueChange = { newEnd = it }, label = "New End", hint = "HH:mm", placeholder = "10:00")
            }
            if (kind == "ROOM_CHANGE") {
                VInput(value = newRoom, onValueChange = { newRoom = it }, label = "New Room", hint = "e.g. 102", placeholder = "102")
            }
            VInput(value = note, onValueChange = { note = it }, label = "Note", hint = "Optional", placeholder = "")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                VButton(
                    text = "Save",
                    onClick = {
                        onSave(
                            CreateExceptionRequest(
                                periodId = periodId.trim().ifBlank { null },
                                date = date.trim(),
                                kind = kind,
                                newStart = newStart.trim().ifBlank { null },
                                newEnd = newEnd.trim().ifBlank { null },
                                newRoom = newRoom.trim().ifBlank { null },
                                note = note.trim(),
                            )
                        )
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

// ── Change Request Card (shared by ExceptionsRequestsTab) ─────────────────────

@Composable
private fun ChangeRequestCard(
    request: TimetableChangeRequestDto,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    var adminNote by remember { mutableStateOf("") }
    var showActions by remember { mutableStateOf(false) }

    VCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VBadge(text = request.kind, tone = VBadgeTone.Accent)
                VBadge(
                    text = request.status,
                    tone = when (request.status) {
                        "PENDING" -> VBadgeTone.Warning
                        "APPROVED" -> VBadgeTone.Success
                        "REJECTED" -> VBadgeTone.Danger
                        else -> VBadgeTone.Neutral
                    },
                )
                Text(WEEKDAY_NAMES.getOrElse(request.weekday) { "Day ${request.weekday}" }, style = VTheme.type.caption, color = VTheme.colors.ink3)
            }
            if (request.teacherName.isNotBlank()) {
                Text("Teacher: ${request.teacherName}", style = VTheme.type.body, color = VTheme.colors.ink2)
            }
            if (request.className.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    VBadge(text = request.className, tone = VBadgeTone.Arctic)
                    if (request.section.isNotBlank()) VBadge(text = request.section, tone = VBadgeTone.Accent)
                    if (request.subject.isNotBlank()) Text(request.subject, style = VTheme.type.caption, color = VTheme.colors.ink2)
                }
            }
            if (request.startTime != null && request.endTime != null) {
                Text("${request.startTime} – ${request.endTime}", style = VTheme.type.caption, color = VTheme.colors.ink3)
            }
            if (request.room.isNotBlank()) {
                Text("Room: ${request.room}", style = VTheme.type.caption, color = VTheme.colors.ink3)
            }
            if (request.reason.isNotBlank()) {
                Text("Reason: ${request.reason}", style = VTheme.type.caption, color = VTheme.colors.ink3)
            }
            if (request.adminNote.isNotBlank()) {
                Text("Admin note: ${request.adminNote}", style = VTheme.type.caption, color = VTheme.colors.ink3)
            }
            if (request.status == "PENDING") {
                if (!showActions) {
                    VButton(
                        text = "Review",
                        onClick = { showActions = true },
                        variant = VButtonVariant.Secondary,
                        tone = VButtonTone.Teal,
                    )
                } else {
                    VInput(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = "Admin Note",
                        hint = "Optional",
                        placeholder = "",
                    )
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
                }
            }
        }
    }
}
