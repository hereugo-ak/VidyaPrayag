package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.SyllabusUnit
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherSyllabusState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherSyllabusViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * TeacherSyllabusScreenV2 — T-403 (Doc 08 §2, Doc 10). The clean rebuild of the syllabus plane
 * on the typed, assignment-scoped contract (T-402), replacing the legacy class+subject screen.
 *
 * The whole point is **the single tap** (F-SYL-1): one tap on a unit row toggles coverage —
 * no modal, no form, no save button — optimistic, with a server-stamped covered_on. A progress
 * bar tops the list ("12 / 30 units covered · 40%"). Chapters carry topics (indented). The rare,
 * deliberate affordances (add unit, rename) live behind an **edit-mode** toggle so they never
 * clutter the one-tap path.
 *
 * Reached PRE-SCOPED via [assignmentId] (the TSA id = TeacherClassSummaryDto.assignmentId) — no shared picker here
 * (the Planner host owns class selection). When empty (no curriculum seeded) the honest
 * "No syllabus yet — add your first unit" state offers a single Add affordance (fixes B-SYL-1
 * dead-empty F-SYL-2). Closes F-SYL-1..4. Three states via [VStateHost].
 */
@Composable
fun TeacherSyllabusScreenV2(
    assignmentId: String = "",
    scopeHint: String = "",
    modifier: Modifier = Modifier,
    viewModel: TeacherSyllabusViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank()) viewModel.load(assignmentId)
    }

    TeacherSyllabusContent(
        state = state,
        hasScope = assignmentId.isNotBlank(),
        scopeHint = scopeHint,
        onToggle = viewModel::toggleUnit,
        onRetry = viewModel::retry,
        onToggleEditing = viewModel::toggleEditing,
        onOpenAdd = viewModel::openAdd,
        onCloseAdd = viewModel::closeAdd,
        onSetAddTitle = viewModel::setAddTitle,
        onSubmitAdd = viewModel::submitAdd,
        modifier = modifier,
    )
}

@Composable
private fun TeacherSyllabusContent(
    state: TeacherSyllabusState,
    hasScope: Boolean,
    scopeHint: String,
    onToggle: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleEditing: () -> Unit,
    onOpenAdd: (String?) -> Unit,
    onCloseAdd: () -> Unit,
    onSetAddTitle: (String) -> Unit,
    onSubmitAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    // The scope label: server value once loaded, else the pre-known hint.
    val scopeLabel = remember(state.className, state.subject, scopeHint) {
        listOf(state.className, state.subject).filter { it.isNotBlank() }.joinToString(" · ")
            .ifBlank { scopeHint }
    }

    Column(modifier.fillMaxSize()) {
        // Scope guard header — shown instantly so the teacher always knows which class
        // they're logging against (never a silent wrong-class write).
        if (scopeLabel.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 4.dp)) {
                VLabel("Syllabus")
                Text(scopeLabel, style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(top = 2.dp))
            }
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            // No scope yet → VStateHost's own empty ("Choose a class"). When scoped, even a
            // zero-unit syllabus is CONTENT (we render an honest empty WITH an Add affordance
            // inside, fixing the B-SYL-1 dead-empty F-SYL-2 — VStateHost's empty has no action).
            isEmpty = !hasScope,
            emptyTitle = "Choose a class",
            emptyBody = "Pick a class to log syllabus progress.",
            emptyIcon = VIcons.FileText,
            onRetry = null,
        ) {
            if (!state.hasUnits) {
                // Honest, ACTIONABLE empty (Doc 08 §4 / F-SYL-2): not blank/dead.
                VEmptyState(
                    title = "No syllabus yet",
                    body = "Add your first unit to start tracking coverage.",
                    icon = VIcons.FileText,
                    action = {
                        VButton(
                            text = "Add your first unit",
                            onClick = { onOpenAdd(null) },
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                    },
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProgressCard(state)
                    EditModeBar(state, onToggleEditing = onToggleEditing, onAddChapter = { onOpenAdd(null) })
                    UnitList(
                        state = state,
                        onToggle = onToggle,
                        onAddTopic = { chapterId -> onOpenAdd(chapterId) },
                    )
                }
            }
        }
    }

    // The add-unit composer (chapter when parentId is "", topic otherwise) — a light sheet,
    // result-driven (Add spins while in flight, surfaces addError).
    val addingParent = state.addingUnderParentId
    if (addingParent != null) {
        val parentTitle = state.units.firstOrNull { it.id == addingParent }?.title
        AddUnitDialog(
            isTopic = addingParent.isNotBlank(),
            parentTitle = parentTitle,
            title = state.addTitle,
            isAdding = state.isAdding,
            error = state.addError,
            onTitleChange = onSetAddTitle,
            onConfirm = onSubmitAdd,
            onDismiss = onCloseAdd,
        )
    }
}

@Composable
private fun ProgressCard(state: TeacherSyllabusState) {
    val c = VTheme.colors
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            VLabel("Syllabus progress")
            Text(
                "${state.coveredCount} / ${state.totalCount} covered · ${(state.progress * 100).roundToInt()}%",
                style = VTheme.type.dataSm.colored(c.ink2).copy(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.height(8.dp))
        VProgressBar(value = state.progress * 100f)
    }
}

@Composable
private fun EditModeBar(state: TeacherSyllabusState, onToggleEditing: () -> Unit, onAddChapter: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        VButton(
            text = if (state.isEditing) "Done" else "Edit units",
            onClick = onToggleEditing,
            variant = VButtonVariant.Ghost,
            size = VButtonSize.Sm,
            leading = { Icon(if (state.isEditing) VIcons.Check else VIcons.Edit3, contentDescription = null, modifier = Modifier.size(14.dp)) },
        )
        AnimatedVisibility(visible = state.isEditing) {
            VButton(
                text = "Add chapter",
                onClick = onAddChapter,
                variant = VButtonVariant.Secondary,
                size = VButtonSize.Sm,
                leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(14.dp)) },
            )
        }
    }
}

@Composable
private fun UnitList(
    state: TeacherSyllabusState,
    onToggle: (String) -> Unit,
    onAddTopic: (String) -> Unit,
) {
    VCard {
        // Virtualized — a fat syllabus (chapters × topics) can run long (Doc 10 §7).
        LazyColumn(Modifier.fillMaxWidth().height(((state.units.size.coerceAtMost(14)) * 52 + 8).dp)) {
            items(state.units, key = { it.id }) { unit ->
                UnitRow(
                    unit = unit,
                    isEditing = state.isEditing,
                    updating = state.updatingUnitId == unit.id,
                    enabled = state.updatingUnitId == null,
                    onToggle = { onToggle(unit.id) },
                    onAddTopic = { onAddTopic(unit.id) },
                )
            }
        }
    }
}

@Composable
private fun UnitRow(
    unit: SyllabusUnit,
    isEditing: Boolean,
    updating: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onAddTopic: () -> Unit,
) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val coveredVerb = if (unit.isCovered) "covered" else "not covered"
    Row(
        Modifier
            .fillMaxWidth()
            // The WHOLE row is the tap target (≥48dp) — the one-tap gesture, not just the dot.
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !isEditing, onClick = onToggle)
            .padding(start = (unit.depth * 16).dp)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .semantics { contentDescription = "${unit.title}, $coveredVerb" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverToggle(covered = unit.isCovered, updating = updating, isChapter = unit.isChapter)
        Column(Modifier.weight(1f)) {
            Text(
                unit.title,
                style = (if (unit.isChapter) VTheme.type.bodyStrong else VTheme.type.body)
                    .colored(if (unit.isCovered) c.ink else c.ink2)
                    .copy(fontSize = if (unit.isChapter) 14.sp else 13.sp),
            )
            val coveredOn = unit.coveredOn
            if (unit.isCovered && !coveredOn.isNullOrBlank()) {
                Text("Covered $coveredOn", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 10.sp))
            }
        }
        // In edit mode, a chapter offers "+ topic"; the one-tap toggle is suppressed.
        if (isEditing && unit.isChapter) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onAddTopic),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Plus, contentDescription = "Add topic", tint = c.accentDeep, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CoverToggle(covered: Boolean, updating: Boolean, isChapter: Boolean) {
    val c = VTheme.colors
    Box(
        Modifier
            .size(if (isChapter) 28.dp else 24.dp)
            .clip(CircleShape)
            .background(if (covered) c.success else c.ink.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        if (covered) {
            Icon(VIcons.Check, contentDescription = null, tint = c.successInk, modifier = Modifier.size(if (isChapter) 16.dp else 14.dp))
        }
    }
}

/** A light add-unit composer dialog: title field + result-driven confirm. */
@Composable
private fun AddUnitDialog(
    isTopic: Boolean,
    parentTitle: String?,
    title: String,
    isAdding: Boolean,
    error: String?,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = VTheme.colors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        VCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (isTopic) "Add topic" else "Add chapter",
                    style = VTheme.type.h3.colored(c.ink),
                )
                if (isTopic && !parentTitle.isNullOrBlank()) {
                    Text("Under \"$parentTitle\"", style = VTheme.type.caption.colored(c.ink3))
                }
                VInput(
                    value = title,
                    onValueChange = onTitleChange,
                    placeholder = if (isTopic) "e.g. Long division" else "e.g. Fractions",
                    enabled = !isAdding,
                )
                if (error != null) {
                    Text(error, style = VTheme.type.caption.colored(c.dangerInk))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Md,
                        enabled = !isAdding,
                        modifier = Modifier.weight(1f),
                    )
                    VButton(
                        text = "Add",
                        onClick = onConfirm,
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Md,
                        loading = isAdding,
                        enabled = !isAdding && title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
