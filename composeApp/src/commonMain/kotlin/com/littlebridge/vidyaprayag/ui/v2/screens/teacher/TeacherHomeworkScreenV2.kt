package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HOMEWORK_STATUS_GRADED
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HOMEWORK_STATUS_LATE
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HOMEWORK_STATUS_NOT_SUBMITTED
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HOMEWORK_STATUS_SUBMITTED
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HomeworkBoard
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HomeworkBoardRow
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HomeworkMode
import com.littlebridge.vidyaprayag.feature.teacher.presentation.HomeworkSummary
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeworkState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeworkViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VConfirmDialog
import com.littlebridge.vidyaprayag.ui.v2.components.VDatePicker
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
 * TeacherHomeworkScreenV2 — T-406 (Doc 08 §6–§9). The clean rebuild of the Planner › Homework
 * plane on the typed lifecycle contract (T-405), replacing the legacy screen whose **Assign
 * button was dead** (F-HW-1) and whose holder had no scope, no board, no extensions.
 *
 * Two surfaces (driven by [HomeworkMode]):
 *   • LIST  — this assignment's active homework, each a card with a live turned-in ratio +
 *             status counts and a "past due" / "just assigned" badge; a primary
 *             "Assign new homework" CTA opens the assign composer (title / description /
 *             due-date(+optional time) / allow-late). This is the **real** fix for F-HW-1.
 *   • BOARD — one homework's roster-joined submissions board (every enrolled student appears,
 *             even NOT-SUBMITTED — B-HW-3/H7) with status pills + counts, per-row review/grade,
 *             grant-extension (whole-class or single-student), and close.
 *
 * Reached PRE-SCOPED by [assignmentId] (X-1) — the Planner host owns class selection; there's no
 * picker here. Empty/loading/error are all designed via [VStateHost] + [VEmptyState] (never
 * defaulted). All numbers come from the backend. Closes F-HW-1..4.
 */
@Composable
fun TeacherHomeworkScreenV2(
    assignmentId: String = "",
    scopeHint: String = "",
    modifier: Modifier = Modifier,
    viewModel: TeacherHomeworkViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffectLoad(assignmentId, viewModel)

    TeacherHomeworkContent(
        state = state,
        hasScope = assignmentId.isNotBlank(),
        scopeHint = scopeHint,
        onRetry = viewModel::retry,
        onOpenComposer = viewModel::openComposer,
        onCloseComposer = viewModel::closeComposer,
        onComposerTitle = viewModel::setComposerTitle,
        onComposerDescription = viewModel::setComposerDescription,
        onComposerDueDate = viewModel::setComposerDueDate,
        onComposerDueTime = viewModel::setComposerDueTime,
        onComposerAllowLate = viewModel::setComposerAllowLate,
        onAssign = viewModel::assign,
        onOpenBoard = viewModel::openBoard,
        onCloseBoard = viewModel::closeBoard,
        onReview = viewModel::reviewSubmission,
        onOpenExtension = viewModel::openExtension,
        onCloseExtension = viewModel::closeExtension,
        onExtensionDate = viewModel::setExtensionDate,
        onExtensionTime = viewModel::setExtensionTime,
        onExtensionReason = viewModel::setExtensionReason,
        onGrantExtension = viewModel::grantExtension,
        onCloseHomework = viewModel::closeHomework,
        modifier = modifier,
    )
}

@Composable
private fun LaunchedEffectLoad(assignmentId: String, viewModel: TeacherHomeworkViewModel) {
    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank()) viewModel.load(assignmentId)
    }
}

@Composable
private fun TeacherHomeworkContent(
    state: TeacherHomeworkState,
    hasScope: Boolean,
    scopeHint: String,
    onRetry: () -> Unit,
    onOpenComposer: () -> Unit,
    onCloseComposer: () -> Unit,
    onComposerTitle: (String) -> Unit,
    onComposerDescription: (String) -> Unit,
    onComposerDueDate: (String) -> Unit,
    onComposerDueTime: (String) -> Unit,
    onComposerAllowLate: (Boolean) -> Unit,
    onAssign: () -> Unit,
    onOpenBoard: (String) -> Unit,
    onCloseBoard: () -> Unit,
    onReview: (String, String, String?) -> Unit,
    onOpenExtension: (String?, String) -> Unit,
    onCloseExtension: () -> Unit,
    onExtensionDate: (String) -> Unit,
    onExtensionTime: (String) -> Unit,
    onExtensionReason: (String) -> Unit,
    onGrantExtension: () -> Unit,
    onCloseHomework: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        HomeworkMode.List -> HomeworkListSurface(
            state = state,
            hasScope = hasScope,
            scopeHint = scopeHint,
            onRetry = onRetry,
            onOpenComposer = onOpenComposer,
            onOpenBoard = onOpenBoard,
            modifier = modifier,
        )
        HomeworkMode.Board -> HomeworkBoardSurface(
            state = state,
            onBack = onCloseBoard,
            onReview = onReview,
            onOpenExtension = onOpenExtension,
            onCloseHomework = onCloseHomework,
            modifier = modifier,
        )
    }

    // Assign composer (list mode) — a light sheet, result-driven (Assign spins, surfaces error).
    if (state.isComposerOpen) {
        AssignHomeworkDialog(
            state = state,
            onTitle = onComposerTitle,
            onDescription = onComposerDescription,
            onDueDate = onComposerDueDate,
            onDueTime = onComposerDueTime,
            onAllowLate = onComposerAllowLate,
            onConfirm = onAssign,
            onDismiss = onCloseComposer,
        )
    }

    // Extension composer (board mode).
    if (state.isExtensionOpen) {
        GrantExtensionDialog(
            state = state,
            onDate = onExtensionDate,
            onTime = onExtensionTime,
            onReason = onExtensionReason,
            onConfirm = onGrantExtension,
            onDismiss = onCloseExtension,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LIST SURFACE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeworkListSurface(
    state: TeacherHomeworkState,
    hasScope: Boolean,
    scopeHint: String,
    onRetry: () -> Unit,
    onOpenComposer: () -> Unit,
    onOpenBoard: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val scopeLabel = remember(scopeHint, state.items) {
        state.items.firstOrNull()?.let { listOf(it.className, it.subject).filter { s -> s.isNotBlank() }.joinToString(" · ") }
            ?.ifBlank { scopeHint } ?: scopeHint
    }

    Column(modifier.fillMaxSize()) {
        if (scopeLabel.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 4.dp)) {
                VLabel("Homework")
                Text(scopeLabel, style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(top = 2.dp))
            }
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            // No scope yet → VStateHost's own "choose a class". When scoped, even zero homework
            // is CONTENT (we render an actionable empty WITH an Assign affordance inside).
            isEmpty = !hasScope,
            emptyTitle = "Choose a class",
            emptyBody = "Pick a class to assign and track homework.",
            emptyIcon = VIcons.ClipboardList,
            onRetry = null,
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.hasItems) {
                    VEmptyState(
                        title = "No homework yet",
                        body = "Assign your first homework — it'll appear here with a live submissions board.",
                        icon = VIcons.ClipboardList,
                        action = {
                            VButton(
                                text = "Assign homework",
                                onClick = onOpenComposer,
                                variant = VButtonVariant.Primary,
                                tone = VButtonTone.Lavender,
                                size = VButtonSize.Md,
                                leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                        },
                    )
                } else {
                    state.items.forEach { hw -> HomeworkCard(hw, onClick = { onOpenBoard(hw.id) }) }
                    VButton(
                        text = "Assign new homework",
                        onClick = onOpenComposer,
                        full = true,
                        size = VButtonSize.Lg,
                        tone = VButtonTone.Lavender,
                        leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeworkCard(hw: HomeworkSummary, onClick: () -> Unit) {
    val c = VTheme.colors
    val allTurnedIn = hw.totalCount > 0 && hw.turnedInCount >= hw.totalCount
    VCard(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(hw.title, style = VTheme.type.bodyStrong.colored(c.ink))
                Text(
                    listOfNotNull(
                        hw.subject.ifBlank { null },
                        hw.className.ifBlank { null },
                        dueLabel(hw.dueDate, hw.dueTime),
                    ).joinToString(" • "),
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp),
                )
            }
            when {
                hw.isPastDue && !allTurnedIn -> VBadge(text = "Past due", tone = VBadgeTone.Danger)
                hw.totalCount > 0 -> VBadge(
                    text = "${hw.turnedInCount} / ${hw.totalCount} in",
                    tone = if (allTurnedIn) VBadgeTone.Success else VBadgeTone.Warning,
                )
                else -> VBadge(text = "Just assigned", tone = VBadgeTone.Arctic)
            }
        }
        if (hw.totalCount > 0) {
            Spacer(Modifier.height(10.dp))
            VProgressBar(value = (hw.turnedInRatio * 100f).roundToInt().toFloat())
            Spacer(Modifier.height(8.dp))
            StatusCountStrip(
                submitted = hw.submittedCount,
                late = hw.lateCount,
                graded = hw.gradedCount,
                notSubmitted = hw.notSubmittedCount,
            )
        }
        if (hw.attachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(VIcons.FileText, contentDescription = null, tint = c.ink3, modifier = Modifier.size(13.dp))
                Text(
                    "${hw.attachments.size} attachment${if (hw.attachments.size == 1) "" else "s"}",
                    style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 10.sp),
                )
            }
        }
    }
}

/** Inline status legend — four soft pills with live counts (Doc 08 §8). */
@Composable
private fun StatusCountStrip(submitted: Int, late: Int, graded: Int, notSubmitted: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (graded > 0) StatusDotCount("Graded", graded, VBadgeTone.Accent)
        if (submitted > 0) StatusDotCount("Submitted", submitted, VBadgeTone.Success)
        if (late > 0) StatusDotCount("Late", late, VBadgeTone.Warning)
        if (notSubmitted > 0) StatusDotCount("Pending", notSubmitted, VBadgeTone.Neutral)
    }
}

@Composable
private fun StatusDotCount(label: String, count: Int, tone: VBadgeTone) {
    VBadge(text = "$count $label", tone = tone)
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOARD SURFACE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeworkBoardSurface(
    state: TeacherHomeworkState,
    onBack: () -> Unit,
    onReview: (String, String, String?) -> Unit,
    onOpenExtension: (String?, String) -> Unit,
    onCloseHomework: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var confirmClose by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        // Back row + close.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            VButton(
                text = "Back",
                onClick = onBack,
                variant = VButtonVariant.Ghost,
                size = VButtonSize.Sm,
                leading = { Icon(VIcons.ArrowLeft, contentDescription = null, modifier = Modifier.size(14.dp)) },
            )
            VButton(
                text = "Close homework",
                onClick = { confirmClose = true },
                variant = VButtonVariant.Ghost,
                tone = VButtonTone.Rose,
                size = VButtonSize.Sm,
                loading = state.isClosing,
            )
        }

        VStateHost(
            loading = state.isBoardLoading,
            error = state.boardError,
            isEmpty = false,   // a loaded board is always content (roster-joined, never blank)
            onRetry = null,
        ) {
            val board = state.board
            if (board != null) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp).padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BoardHeader(board)
                    BoardSummaryCard(board, onExtendAll = { onOpenExtension(null, "") })
                    if (board.rows.isEmpty()) {
                        VEmptyState(
                            title = "No students enrolled",
                            body = "Once students are enrolled in this class they'll appear here.",
                            icon = VIcons.Users,
                        )
                    } else {
                        BoardRosterCard(
                            board = board,
                            updatingStudentId = state.updatingStudentId,
                            onReview = onReview,
                            onExtendStudent = { row -> onOpenExtension(row.studentId, row.name) },
                        )
                    }
                }
            }
        }
    }

    VConfirmDialog(
        visible = confirmClose,
        title = "Close this homework?",
        message = "Students will no longer be able to submit. You can still see the board from your records.",
        confirmLabel = "Close homework",
        onConfirm = { confirmClose = false; onCloseHomework() },
        onDismiss = { confirmClose = false },
        icon = VIcons.AlertTriangle,
    )
}

@Composable
private fun BoardHeader(board: HomeworkBoard) {
    val c = VTheme.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accentTint).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ClipboardList, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(board.title, style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.ExtraBold))
                Text(
                    listOfNotNull(
                        listOf(board.className, board.subject).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                        dueLabel(board.dueDate, board.dueTime),
                    ).joinToString(" • "),
                    style = VTheme.type.caption.colored(c.accentDeep),
                )
            }
            if (board.isPastDue) VBadge(text = "Past due", tone = VBadgeTone.Danger)
        }
    }
}

@Composable
private fun BoardSummaryCard(board: HomeworkBoard, onExtendAll: () -> Unit) {
    val c = VTheme.colors
    val turnedIn = board.submittedCount + board.lateCount + board.gradedCount
    val ratio = if (board.totalCount == 0) 0f else turnedIn.toFloat() / board.totalCount
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            VLabel("Submissions")
            Text(
                "$turnedIn / ${board.totalCount} turned in · ${(ratio * 100).roundToInt()}%",
                style = VTheme.type.dataSm.colored(c.ink2).copy(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.height(8.dp))
        VProgressBar(value = ratio * 100f)
        Spacer(Modifier.height(10.dp))
        StatusCountStrip(
            submitted = board.submittedCount,
            late = board.lateCount,
            graded = board.gradedCount,
            notSubmitted = board.notSubmittedCount,
        )
        Spacer(Modifier.height(10.dp))
        VButton(
            text = "Extend for whole class",
            onClick = onExtendAll,
            variant = VButtonVariant.Secondary,
            tone = VButtonTone.Sky,
            size = VButtonSize.Sm,
            leading = { Icon(VIcons.Clock, contentDescription = null, modifier = Modifier.size(14.dp)) },
        )
    }
}

@Composable
private fun BoardRosterCard(
    board: HomeworkBoard,
    updatingStudentId: String?,
    onReview: (String, String, String?) -> Unit,
    onExtendStudent: (HomeworkBoardRow) -> Unit,
) {
    VCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            board.rows.forEach { row ->
                BoardRow(
                    row = row,
                    updating = updatingStudentId == row.studentId,
                    enabled = updatingStudentId == null,
                    onMarkGraded = { onReview(row.studentId, HOMEWORK_STATUS_GRADED, null) },
                    onMarkSubmitted = { onReview(row.studentId, HOMEWORK_STATUS_SUBMITTED, null) },
                    onExtend = { onExtendStudent(row) },
                )
            }
        }
    }
}

@Composable
private fun BoardRow(
    row: HomeworkBoardRow,
    updating: Boolean,
    enabled: Boolean,
    onMarkGraded: () -> Unit,
    onMarkSubmitted: () -> Unit,
    onExtend: () -> Unit,
) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Roll badge.
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(c.cream),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.rollNo?.toString() ?: "–",
                style = VTheme.type.dataSm.colored(c.ink2).copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(row.name, style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp))
            val sub = listOfNotNull(
                statusVerb(row.status),
                row.grade?.takeIf { it.isNotBlank() }?.let { "Grade $it" },
                row.extendedTo?.takeIf { row.hasExtension && it.isNotBlank() }?.let { "Extended to $it" },
            ).joinToString(" • ")
            if (sub.isNotBlank()) {
                Text(sub, style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 10.sp))
            }
        }
        StatusPill(row.status)
        // Per-row affordances (deliberate, compact). Marking graded/submitted + extend.
        BoardRowActions(
            row = row,
            updating = updating,
            enabled = enabled,
            onMarkGraded = onMarkGraded,
            onMarkSubmitted = onMarkSubmitted,
            onExtend = onExtend,
        )
    }
}

@Composable
private fun BoardRowActions(
    row: HomeworkBoardRow,
    updating: Boolean,
    enabled: Boolean,
    onMarkGraded: () -> Unit,
    onMarkSubmitted: () -> Unit,
    onExtend: () -> Unit,
) {
    val c = VTheme.colors
    // A submitted/late student can be moved to graded; a not-submitted student can be granted
    // an extension (the "she was sick" case) or marked submitted (offline turn-in).
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        when (row.status) {
            HOMEWORK_STATUS_SUBMITTED, HOMEWORK_STATUS_LATE -> RowIconButton(
                icon = VIcons.Check, tint = c.successInk, bg = c.success.copy(alpha = 0.4f),
                enabled = enabled, busy = updating, onClick = onMarkGraded, label = "Mark graded",
            )
            HOMEWORK_STATUS_NOT_SUBMITTED -> {
                RowIconButton(
                    icon = VIcons.Clock, tint = c.accentDeep, bg = c.accent.copy(alpha = 0.12f),
                    enabled = enabled, busy = false, onClick = onExtend, label = "Grant extension",
                )
                RowIconButton(
                    icon = VIcons.Check, tint = c.tealDeep, bg = c.teal.copy(alpha = 0.16f),
                    enabled = enabled, busy = updating, onClick = onMarkSubmitted, label = "Mark submitted",
                )
            }
            HOMEWORK_STATUS_GRADED -> Unit   // already terminal
        }
    }
}

@Composable
private fun RowIconButton(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(bg)
            .clickable(enabled = enabled && !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (busy) VIcons.Spinner else icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val (label, tone) = when (status) {
        HOMEWORK_STATUS_GRADED -> "Graded" to VBadgeTone.Accent
        HOMEWORK_STATUS_SUBMITTED -> "Submitted" to VBadgeTone.Success
        HOMEWORK_STATUS_LATE -> "Late" to VBadgeTone.Warning
        else -> "Pending" to VBadgeTone.Neutral
    }
    VBadge(text = label, tone = tone)
}

private fun statusVerb(status: String): String? = when (status) {
    HOMEWORK_STATUS_GRADED -> "Reviewed"
    HOMEWORK_STATUS_SUBMITTED -> "Turned in"
    HOMEWORK_STATUS_LATE -> "Turned in late"
    else -> null
}

// ─────────────────────────────────────────────────────────────────────────────
//  ASSIGN COMPOSER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AssignHomeworkDialog(
    state: TeacherHomeworkState,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onDueDate: (String) -> Unit,
    onDueTime: (String) -> Unit,
    onAllowLate: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = VTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        VCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Assign homework", style = VTheme.type.h3.colored(c.ink))
                VInput(
                    value = state.composerTitle,
                    onValueChange = onTitle,
                    label = "Title",
                    placeholder = "e.g. Chapter 4 — exercises 1–10",
                    enabled = !state.isAssigning,
                )
                VInput(
                    value = state.composerDescription,
                    onValueChange = onDescription,
                    label = "Instructions (optional)",
                    placeholder = "What should students do?",
                    singleLine = false,
                    enabled = !state.isAssigning,
                )
                VDatePicker(
                    value = state.composerDueDate,
                    onValueChange = onDueDate,
                    label = "Due date",
                    enabled = !state.isAssigning,
                )
                VInput(
                    value = state.composerDueTime,
                    onValueChange = onDueTime,
                    label = "Due time (optional)",
                    placeholder = "e.g. 17:00",
                    enabled = !state.isAssigning,
                )
                AllowLateToggle(checked = state.composerAllowLate, enabled = !state.isAssigning, onToggle = onAllowLate)
                if (state.composerError != null) {
                    Text(state.composerError, style = VTheme.type.caption.colored(c.danger))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Md,
                        enabled = !state.isAssigning,
                        modifier = Modifier.weight(1f),
                    )
                    VButton(
                        text = "Assign",
                        onClick = onConfirm,
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Lavender,
                        size = VButtonSize.Md,
                        loading = state.isAssigning,
                        enabled = !state.isAssigning && state.canAssign,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllowLateToggle(checked: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cream)
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                .background(if (checked) c.success else c.ink.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(VIcons.Check, contentDescription = null, tint = c.successInk, modifier = Modifier.size(15.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Allow late submissions", style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp))
            Text("Students can still turn in after the due date (marked Late).", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 10.sp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EXTENSION COMPOSER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GrantExtensionDialog(
    state: TeacherHomeworkState,
    onDate: (String) -> Unit,
    onTime: (String) -> Unit,
    onReason: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = VTheme.colors
    val wholeClass = state.extensionStudentId == null
    Dialog(onDismissRequest = onDismiss) {
        VCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (wholeClass) "Extend for whole class" else "Grant extension",
                    style = VTheme.type.h3.colored(c.ink),
                )
                Text(
                    if (wholeClass) "Moves the due date for everyone in this class."
                    else "Gives ${state.extensionStudentName.ifBlank { "this student" }} more time — the class due date doesn't change.",
                    style = VTheme.type.caption.colored(c.ink3),
                )
                VDatePicker(
                    value = state.extensionDate,
                    onValueChange = onDate,
                    label = "New due date",
                    enabled = !state.isGrantingExtension,
                )
                VInput(
                    value = state.extensionTime,
                    onValueChange = onTime,
                    label = "New due time (optional)",
                    placeholder = "e.g. 17:00",
                    enabled = !state.isGrantingExtension,
                )
                VInput(
                    value = state.extensionReason,
                    onValueChange = onReason,
                    label = "Reason (optional)",
                    placeholder = "e.g. Was unwell",
                    enabled = !state.isGrantingExtension,
                )
                if (state.extensionError != null) {
                    Text(state.extensionError, style = VTheme.type.caption.colored(c.danger))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Md,
                        enabled = !state.isGrantingExtension,
                        modifier = Modifier.weight(1f),
                    )
                    VButton(
                        text = "Grant",
                        onClick = onConfirm,
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Sky,
                        size = VButtonSize.Md,
                        loading = state.isGrantingExtension,
                        enabled = !state.isGrantingExtension && state.extensionDate.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  helpers
// ─────────────────────────────────────────────────────────────────────────────

/** "Due Mon D" / "Due Mon D · 17:00" without a date lib. Blank-safe. */
private fun dueLabel(dueDate: String, dueTime: String?): String? {
    if (dueDate.isBlank()) return null
    val pretty = prettyDate(dueDate)
    return if (!dueTime.isNullOrBlank()) "Due $pretty · ${dueTime.take(5)}" else "Due $pretty"
}

private fun prettyDate(raw: String): String {
    val parts = raw.take(10).split('-')
    if (parts.size != 3) return raw
    val (y, m, d) = parts
    val month = HW_MONTHS.getOrNull((m.toIntOrNull() ?: 0) - 1) ?: m
    val day = d.toIntOrNull() ?: return raw
    return "$month $day, $y"
}

private val HW_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
