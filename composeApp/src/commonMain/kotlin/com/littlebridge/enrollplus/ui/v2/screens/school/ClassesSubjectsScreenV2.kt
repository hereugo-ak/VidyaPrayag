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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
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
import org.koin.compose.viewmodel.koinViewModel

private val TABS = listOf("Classes", "Subjects", "Bell Schedule", "Timetable")
private val WEEKDAY_NAMES = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun ClassesSubjectsScreenV2(
    onBack: () -> Unit = {},
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
            "Bell Schedule" -> BellScheduleTab()
            "Timetable" -> TimetableTab(
                state = state,
                onLoadTimetable = { filter -> viewModel.loadTimetable(filter) },
            )
        }
    }
}

// ── Classes Tab ───────────────────────────────────────────────────────────────

@Composable
private fun ClassesTab(
    state: ClassesSubjectsState,
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    VCard(Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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

// ── Bell Schedule Tab ─────────────────────────────────────────────────────────

@Composable
private fun BellScheduleTab() {
    SchoolDayConfigEmbeddedV2(
        modifier = Modifier.fillMaxSize(),
    )
}

// ── Timetable Tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableTab(
    state: ClassesSubjectsState,
    onLoadTimetable: (String?) -> Unit,
) {
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }

    VStateHost(
        loading = state.isLoading,
        error = state.errorMessage,
        isEmpty = state.timetable == null && !state.isLoading,
        emptyTitle = "No timetable loaded",
        emptyBody = "Tap 'Load Timetable' to view the weekly schedule.",
        emptyIcon = VIcons.Calendar,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VSectionHeader("Timetable")
            VButton(
                text = "Load Timetable",
                onClick = { onLoadTimetable(selectedClassFilter) },
                variant = VButtonVariant.Primary,
                tone = VButtonTone.Teal,
            )
            val tt = state.timetable
            if (tt != null) {
                if (tt.classes.isNotEmpty()) {
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
                if (tt.weekdays.isEmpty()) {
                    VEmptyState(
                        title = "No periods scheduled",
                        icon = VIcons.Calendar,
                        body = "The timetable is empty. Assign teachers to periods to populate it.",
                    )
                }
                tt.weekdays.forEach { day ->
                    Text(
                        WEEKDAY_NAMES.getOrElse(day.weekday) { "Day ${day.weekday}" },
                        style = VTheme.type.h3,
                        fontWeight = FontWeight.Bold,
                        color = VTheme.colors.ink,
                    )
                    day.periods.forEach { period ->
                        TimetablePeriodRow(period)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TimetablePeriodRow(period: TimetablePeriodDto) {
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
            if (period.room.isNotBlank()) {
                VBadge(text = "Room ${period.room}", tone = VBadgeTone.Success)
            }
        }
    }
}
