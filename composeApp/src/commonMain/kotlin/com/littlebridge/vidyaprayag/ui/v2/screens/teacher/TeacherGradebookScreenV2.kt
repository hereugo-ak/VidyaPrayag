package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentStatus
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.AssessmentType
import com.littlebridge.vidyaprayag.feature.teacher.presentation.GradebookMode
import com.littlebridge.vidyaprayag.feature.teacher.presentation.GradebookStudentMark
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherGradebookState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherGradebookViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VConfirmDialog
import com.littlebridge.vidyaprayag.ui.v2.components.VDatePicker
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * TeacherGradebookScreenV2 — T-305 (Doc 07 §3/§5, Doc 10 §6.4). The clean rebuild of the
 * marks plane that replaces the legacy TeacherMarksScreenV2 + TeacherExamPicker split (both
 * deleted in T-305).
 * Reached PRE-SCOPED from a Today/Classes CTA via [assignmentId] (Doc 05 binding) — there is
 * no shared class picker (kills F-SHELL-3).
 *
 * Two faces, driven by [GradebookMode]:
 *  - **List** — a scope guard header + the scoped assessment list ("entered k/n" per row,
 *    status pill, published badge) + an inline create flow (scope pre-filled, typed kind,
 *    max/pass validated, optional exam date). Long lists virtualized (Doc 10 §7).
 *  - **Marks** — a dense, DM-Mono validated marks grid with a per-student AB toggle, a sticky
 *    running counter ("entered k/n · avg"), a **result-driven SAVE that never publishes**
 *    (the B-MK-1 fix), and a **distinct PUBLISH** gated behind a confirm dialog that names the
 *    assessment (the ONLY parent-notify path). Once published, an Unpublish (retract) affordance
 *    appears; re-marking re-arms Save.
 *
 * Closes F-MK-1..7 and F-SHELL-3. Three states via [VStateHost].
 *
 * [scopeHint] is the pre-known label from the launching period (so the guard header shows
 * instantly while loading); the server load is the source of truth.
 */
@Composable
fun TeacherGradebookScreenV2(
    assignmentId: String = "",
    scopeHint: String = "",
    modifier: Modifier = Modifier,
    viewModel: TeacherGradebookViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank()) viewModel.load(assignmentId, scopeHint)
    }

    when (state.mode) {
        GradebookMode.List -> GradebookListContent(
            state = state,
            hasScope = assignmentId.isNotBlank(),
            scopeHint = scopeHint,
            onSetName = viewModel::setCreateName,
            onSetType = viewModel::setCreateType,
            onSetMax = viewModel::setCreateMaxMarks,
            onSetPass = viewModel::setCreatePassMarks,
            onSetDate = viewModel::setCreateExamDate,
            onCreate = viewModel::createAssessment,
            onOpenMarks = viewModel::openMarks,
            onRetry = viewModel::retryList,
            modifier = modifier,
        )
        GradebookMode.Marks -> GradebookMarksContent(
            state = state,
            onBack = viewModel::backToList,
            onSetMark = viewModel::setMark,
            onToggleAbsent = viewModel::toggleAbsent,
            onSave = viewModel::save,
            onPublish = viewModel::publish,
            onUnpublish = viewModel::unpublish,
            onRetry = viewModel::retryMarks,
            modifier = modifier,
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  LIST MODE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun GradebookListContent(
    state: TeacherGradebookState,
    hasScope: Boolean,
    scopeHint: String,
    onSetName: (String) -> Unit,
    onSetType: (String) -> Unit,
    onSetMax: (String) -> Unit,
    onSetPass: (String) -> Unit,
    onSetDate: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenMarks: (AssessmentDto) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var showCreate by remember { mutableStateOf(false) }
    val headerScope = state.assessments.firstOrNull()
        ?.let { listOfNotNull(it.className.takeIf { n -> n.isNotBlank() }, it.subject.takeIf { s -> s.isNotBlank() }).joinToString(" · ") }
        ?.takeIf { it.isNotBlank() }
        ?: scopeHint

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 12.dp)) {

        ScopeGuardHeader(scope = headerScope.ifBlank { "Gradebook" }, subtitle = "Assessments & marks")
        Spacer(Modifier.height(12.dp))

        VStateHost(
            loading = state.isListLoading,
            error = state.listError,
            isEmpty = !hasScope,
            emptyTitle = "Choose a class",
            emptyBody = "Open the gradebook from a class on Today to manage its assessments.",
            emptyIcon = VIcons.GraduationCap,
            onRetry = onRetry,
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // New-assessment toggle / inline form (scope pre-filled — F-SHELL-3).
                VButton(
                    text = if (showCreate) "Close" else "New assessment",
                    onClick = { showCreate = !showCreate },
                    full = true,
                    variant = if (showCreate) VButtonVariant.Secondary else VButtonVariant.Primary,
                    tone = VButtonTone.Lavender,
                    leading = {
                        Icon(
                            if (showCreate) VIcons.Close else VIcons.Plus,
                            contentDescription = null,
                            tint = if (showCreate) c.ink2 else c.card,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )

                if (showCreate) {
                    CreateAssessmentCard(
                        state = state,
                        onSetName = onSetName,
                        onSetType = onSetType,
                        onSetMax = onSetMax,
                        onSetPass = onSetPass,
                        onSetDate = onSetDate,
                        onCreate = onCreate,
                    )
                }

                if (state.assessments.isEmpty()) {
                    EmptyAssessmentsHint()
                } else {
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.assessments, key = { it.id }) { a ->
                            AssessmentRow(a = a, onClick = { onOpenMarks(a) })
                        }
                        item { Spacer(Modifier.height(120.dp)) } // clear the dock
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAssessmentCard(
    state: TeacherGradebookState,
    onSetName: (String) -> Unit,
    onSetType: (String) -> Unit,
    onSetMax: (String) -> Unit,
    onSetPass: (String) -> Unit,
    onSetDate: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VLabel("New assessment")
            VInput(
                value = state.createName,
                onValueChange = onSetName,
                label = "Name",
                placeholder = "Unit Test I",
            )
            // Typed kind — horizontal chip row (D-ASMT-4). No free-text; fixed taxonomy.
            VLabel("Kind")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssessmentType.ALL.forEach { kind ->
                    TypeChip(kind = kind, selected = kind == state.createType) { onSetType(kind) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VInput(
                    value = state.createMaxMarks,
                    onValueChange = onSetMax,
                    label = "Max marks",
                    placeholder = "100",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                VInput(
                    value = state.createPassMarks,
                    onValueChange = onSetPass,
                    label = "Pass (optional)",
                    placeholder = "35",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            VDatePicker(
                value = state.createExamDate,
                onValueChange = onSetDate,
                label = "Exam date (optional)",
            )
            state.createError?.let { err ->
                Text(err, style = VTheme.type.caption.colored(c.danger))
            }
            VButton(
                text = "Create assessment",
                onClick = onCreate,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Lavender,
                loading = state.isCreating,
                enabled = !state.isCreating && state.createName.isNotBlank(),
            )
        }
    }
}

@Composable
private fun TypeChip(kind: String, selected: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.accentDeep else c.cream)
            .border(1.dp, if (selected) c.accentDeep else c.border2, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            kind.replaceFirstChar { it.uppercase() },
            style = VTheme.type.label.colored(if (selected) c.card else c.ink2)
                .copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun AssessmentRow(a: AssessmentDto, onClick: () -> Unit) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        a.name,
                        style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                    )
                    if (a.isPublished) PublishedBadge()
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // DM Mono "entered k/n" + max — tabular figures (Doc 10 §2).
                    Text(
                        "${a.enteredCount}/${a.rosterCount} entered",
                        style = VTheme.type.dataSm.colored(c.ink2).copy(fontSize = 12.sp),
                    )
                    Text("·", style = VTheme.type.caption.colored(c.ink3))
                    Text(
                        "max ${a.maxMarks}${a.passMarks?.let { " · pass $it" } ?: ""}",
                        style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 12.sp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    LifecyclePill(status = a.status)
                    Text(
                        a.type.replaceFirstChar { it.uppercase() }
                            + (a.examDate?.let { " · ${prettyGbStamp(it)}" } ?: ""),
                        style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                    )
                }
            }
            Icon(VIcons.ChevronRight, contentDescription = "Open marks", tint = c.ink3, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PublishedBadge() {
    val c = VTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(c.success.copy(alpha = 0.22f)).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text("Published", style = VTheme.type.label.colored(c.successInk).copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
    }
}

/** Lifecycle status pill — color + WORD encoded (never hue-only, Doc 10 §11). */
@Composable
private fun LifecyclePill(status: String) {
    val c = VTheme.colors
    val (fill, ink, label) = when (status) {
        AssessmentStatus.PUBLISHED -> Triple(c.success.copy(alpha = 0.20f), c.successInk, "Published")
        AssessmentStatus.MARKS_PENDING -> Triple(c.warning.copy(alpha = 0.22f), c.warningInk, "Marks pending")
        AssessmentStatus.SCHEDULED -> Triple(c.accentSoft.copy(alpha = 0.22f), c.accentDeep, "Scheduled")
        AssessmentStatus.ARCHIVED -> Triple(c.ink3.copy(alpha = 0.14f), c.ink2, "Archived")
        else -> Triple(c.cream, c.ink3, "Draft")
    }
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(fill).padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, style = VTheme.type.label.colored(ink).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun EmptyAssessmentsHint() {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ListChecks, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("No assessments yet", style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp))
                Text(
                    "Tap \"New assessment\" to create one — then enter and publish its marks.",
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  MARKS MODE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun GradebookMarksContent(
    state: TeacherGradebookState,
    onBack: () -> Unit,
    onSetMark: (String, Float?) -> Unit,
    onToggleAbsent: (String) -> Unit,
    onSave: () -> Unit,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val a = state.activeAssessment
    var showPublishConfirm by remember { mutableStateOf(false) }
    var showUnpublishConfirm by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 12.dp)) {

        // Back + assessment guard header.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ArrowLeft, contentDescription = "Back to assessments", tint = c.ink, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    a?.name ?: "Marks",
                    style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.ExtraBold),
                )
                a?.let {
                    Text(
                        listOfNotNull(it.className.takeIf { n -> n.isNotBlank() }, it.subject.takeIf { s -> s.isNotBlank() }, "max ${it.maxMarks}")
                            .joinToString(" · "),
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
            }
            if (a?.isPublished == true) PublishedBadge()
        }
        Spacer(Modifier.height(12.dp))

        VStateHost(
            loading = state.isMarksLoading,
            error = state.marksError,
            isEmpty = state.students.isEmpty(),
            emptyTitle = "No students",
            emptyBody = "This class has no enrolled students to mark yet.",
            emptyIcon = VIcons.Users,
            onRetry = onRetry,
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // SAVE never publishes — make that promise explicit to the teacher (B-MK-1).
                if (!state.isPublished) {
                    Text(
                        "Saving keeps marks private. Parents are notified only when you publish.",
                        style = VTheme.type.caption.colored(c.ink3),
                    )
                }

                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.students, key = { it.studentId }) { s ->
                        MarkRow(
                            s = s,
                            maxMarks = state.maxMarks,
                            passMarks = a?.passMarks,
                            onSetMark = onSetMark,
                            onToggleAbsent = onToggleAbsent,
                        )
                    }
                    item { Spacer(Modifier.height(180.dp)) } // clear footer + dock
                }

                MarksFooter(
                    state = state,
                    onSave = onSave,
                    onPublishClick = { showPublishConfirm = true },
                    onUnpublishClick = { showUnpublishConfirm = true },
                )

                state.saveError?.let { Text(it, style = VTheme.type.caption.colored(c.danger), modifier = Modifier.fillMaxWidth()) }
                state.publishError?.let { Text(it, style = VTheme.type.caption.colored(c.danger), modifier = Modifier.fillMaxWidth()) }
                state.parentsNotified?.let { n ->
                    Text(
                        if (n > 0) "Published — $n parent${if (n == 1) "" else "s"} notified." else "Published.",
                        style = VTheme.type.caption.colored(c.successInk),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // PUBLISH confirm — names the assessment + warns parents are notified (the only notify path).
    VConfirmDialog(
        visible = showPublishConfirm,
        title = "Publish marks?",
        message = "\"${a?.name.orEmpty()}\" will be published to parents of " +
            "${a?.className.orEmpty().ifBlank { "this class" }}. They will be notified immediately. " +
            "You can retract it later, but the notification can't be unsent.",
        confirmLabel = "Publish & notify",
        cancelLabel = "Not yet",
        icon = VIcons.Send,
        onConfirm = { showPublishConfirm = false; onPublish() },
        onDismiss = { showPublishConfirm = false },
    )

    // UNPUBLISH (retract) confirm — audited, no re-notify.
    VConfirmDialog(
        visible = showUnpublishConfirm,
        title = "Retract published marks?",
        message = "\"${a?.name.orEmpty()}\" will be hidden from parents again. They are not re-notified. " +
            "This is recorded in the audit log.",
        confirmLabel = "Retract",
        cancelLabel = "Keep published",
        icon = VIcons.Lock,
        onConfirm = { showUnpublishConfirm = false; onUnpublish() },
        onDismiss = { showUnpublishConfirm = false },
    )
}

@Composable
private fun MarkRow(
    s: GradebookStudentMark,
    maxMarks: Int,
    passMarks: Int?,
    onSetMark: (String, Float?) -> Unit,
    onToggleAbsent: (String) -> Unit,
) {
    val c = VTheme.colors
    // Below-pass coloring only when a pass line exists and a real (non-AB) mark is entered (M11).
    val belowPass = passMarks != null && !s.isAbsent && s.marks != null && s.marks < passMarks
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VAvatar(name = s.name, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 16.sp))
                Text("Roll ${s.rollNo.ifBlank { "—" }}", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 12.sp))
            }

            // Marks field — DM Mono, validated/clamped in the VM; dimmed when AB.
            Box(
                Modifier
                    .width(72.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (belowPass) c.danger else c.border2,
                        RoundedCornerShape(8.dp),
                    )
                    .background(if (s.isAbsent) c.cream else c.card),
                contentAlignment = Alignment.Center,
            ) {
                if (s.isAbsent) {
                    Text("AB", style = VTheme.type.data.colored(c.ink3).copy(fontWeight = FontWeight.Bold))
                } else {
                    VInput(
                        value = s.marks?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "",
                        onValueChange = { raw -> onSetMark(s.studentId, raw.trim().toFloatOrNull()) },
                        placeholder = "—",
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
            Text("/$maxMarks", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 12.sp))

            // AB toggle — ≥48dp, letter + color encoded (Doc 10 §3/§11).
            AbToggle(active = s.isAbsent) { onToggleAbsent(s.studentId) }
        }
    }
}

@Composable
private fun AbToggle(active: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) c.warning else c.cream)
            .border(1.dp, if (active) c.warning else c.border2, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = if (active) "Absent, selected" else "Mark absent" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "AB",
            style = VTheme.type.label.colored(if (active) c.warningInk else c.ink3)
                .copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun MarksFooter(
    state: TeacherGradebookState,
    onSave: () -> Unit,
    onPublishClick: () -> Unit,
    onUnpublishClick: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${state.enteredCount}/${state.rosterCount} entered",
                        style = VTheme.type.data.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                    val avg = state.liveAverage
                    Text(
                        avg?.let { "Class avg ${it.roundToInt()} / ${state.maxMarks}" } ?: "No marks entered yet",
                        style = VTheme.type.caption.colored(if (avg != null) c.ink2 else c.ink3),
                    )
                }
                // SAVE — result-driven, NEVER publishes (B-MK-1). Re-arms when a mark is edited.
                VButton(
                    text = if (state.saveSuccess) "Saved" else "Save",
                    onClick = onSave,
                    size = VButtonSize.Lg,
                    tone = VButtonTone.Lavender,
                    loading = state.isSaving,
                    success = state.saveSuccess,
                    enabled = !state.isSaving && !state.saveSuccess && state.rosterCount > 0,
                    successLabel = "Saved",
                    modifier = Modifier.widthIn(min = 110.dp),
                )
            }

            // PUBLISH — visually + structurally distinct from Save (the ONLY notify path).
            if (state.isPublished) {
                VButton(
                    text = "Unpublish (retract)",
                    onClick = onUnpublishClick,
                    full = true,
                    variant = VButtonVariant.Ghost,
                    loading = state.isPublishing,
                    leading = { Icon(VIcons.Lock, contentDescription = null, tint = c.ink2, modifier = Modifier.size(16.dp)) },
                )
            } else {
                VButton(
                    text = "Publish to parents",
                    onClick = onPublishClick,
                    full = true,
                    size = VButtonSize.Lg,
                    tone = VButtonTone.Teal,
                    loading = state.isPublishing,
                    enabled = !state.isPublishing && state.canPublish,
                    leading = { Icon(VIcons.Send, contentDescription = null, tint = c.card, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  SHARED
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScopeGuardHeader(scope: String, subtitle: String) {
    val c = VTheme.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accentTint).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.GraduationCap, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(scope, style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.ExtraBold))
                Text(subtitle, style = VTheme.type.caption.colored(c.accentDeep))
            }
        }
    }
}

/** ISO "YYYY-MM-DD" → "Mon D, YYYY" without a date lib. */
private fun prettyGbStamp(raw: String): String {
    val parts = raw.take(10).split('-')
    if (parts.size != 3) return raw
    val (y, m, d) = parts
    val month = GB_MONTHS.getOrNull((m.toIntOrNull() ?: 0) - 1) ?: m
    val day = d.toIntOrNull() ?: return raw
    return "$month $day, $y"
}

private val GB_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
