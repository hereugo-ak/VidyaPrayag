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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.teacher.domain.model.HomeworkSubmissionStatus
import com.littlebridge.enrollplus.feature.teacher.presentation.HomeworkBoardRow
import com.littlebridge.enrollplus.feature.teacher.presentation.HomeworkMode
import com.littlebridge.enrollplus.feature.teacher.presentation.HomeworkSummary
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherHomeworkViewModel
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherHomeworkScreenV2 — the scoped homework lifecycle (Doc 08 §6–§8). Reached PRE-SCOPED with a
 * pre-authorized [assignmentId]. Two faces:
 *   • LIST  — this class's active homework (each with a live turned-in ratio) + an assign composer
 *             (title / description / due date(+time) / allow-late).
 *   • BOARD — one homework's roster-joined submissions board (every enrolled student, incl. NOT
 *             SUBMITTED), grant extension (whole-class or one student), per-row review, and close.
 */
@Composable
fun TeacherHomeworkScreenV2(
    assignmentId: String,
    scopeLabel: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherHomeworkViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) viewModel.load(assignmentId)
    }

    Box(modifier.fillMaxSize().background(c.background)) {
        when (state.mode) {
            HomeworkMode.List -> HomeworkListMode(viewModel, scopeLabel)
            HomeworkMode.Board -> HomeworkBoardMode(viewModel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeworkListMode(viewModel: TeacherHomeworkViewModel, scopeLabel: String) {
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
                    TEyebrow("HOMEWORK", dot = c.tealDeep)
                    Spacer(Modifier.height(6.dp))
                    Text(scopeLabel.ifBlank { state.scopeLabel }, style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
                    Spacer(Modifier.height(12.dp))
                    VButton(
                        text = if (state.isComposerOpen) "Close" else "Assign homework",
                        onClick = { if (state.isComposerOpen) viewModel.closeComposer() else viewModel.openComposer() },
                        full = true,
                        variant = if (state.isComposerOpen) VButtonVariant.Ghost else VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                        leading = { Icon(if (state.isComposerOpen) VIcons.Close else VIcons.Plus, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    )
                }
            }
        }

        if (state.isComposerOpen) item { HomeworkComposer(viewModel) }

        when {
            state.isLoading && state.items.isEmpty() -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
            state.error != null && state.items.isEmpty() -> item {
                TCard { Column {
                    Text("Couldn't load homework", style = VTheme.type.bodyStrong.colored(c.ink))
                    Spacer(Modifier.height(8.dp))
                    VButton("Retry", onClick = { viewModel.retry() }, tone = VButtonTone.Teal, size = VButtonSize.Sm)
                } }
            }
            state.items.isEmpty() -> item {
                TCard { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TIconDisc(VIcons.FileText, tint = c.tealDeep, bg = c.teal.copy(alpha = 0.14f), size = 48.dp, glyph = 24.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("No active homework", style = VTheme.type.h3.colored(c.ink))
                    Text("Assign your first homework for this class.", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                } }
            }
            else -> items(state.items, key = { it.id }) { hw -> HomeworkRow(hw) { viewModel.openBoard(hw.id) } }
        }
    }
}

@Composable
private fun HomeworkComposer(viewModel: TeacherHomeworkViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    TCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("New homework", style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
            VInput(value = state.composerTitle, onValueChange = viewModel::setComposerTitle, label = "Title", placeholder = "e.g. Chapter 4 exercises")
            VInput(value = state.composerDescription, onValueChange = viewModel::setComposerDescription, label = "Details (optional)", placeholder = "Instructions for students", singleLine = false)
            VDatePicker(value = state.composerDueDate, onValueChange = viewModel::setComposerDueDate, label = "Due date")
            // Allow-late toggle
            val late = state.composerAllowLate
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cream).border(1.dp, c.hairline, RoundedCornerShape(12.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.setComposerAllowLate(!late) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (late) c.tealDeep else c.card).border(1.dp, if (late) c.tealDeep else c.hairline, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (late) Icon(VIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                Spacer(Modifier.width(10.dp))
                Text("Allow late submissions", style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.5.sp))
            }
            if (state.composerError != null) Text(state.composerError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp))
            VButton("Assign homework", onClick = { viewModel.assign() }, full = true, tone = VButtonTone.Teal, loading = state.isAssigning, enabled = state.canAssign)
        }
    }
}

@Composable
private fun HomeworkRow(hw: HomeworkSummary, onOpen: () -> Unit) {
    val c = VTheme.colors
    val pct = (hw.turnedInRatio * 100).toInt()
    TCard(padding = 14.dp, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TRing(percent = pct, modifier = Modifier.size(52.dp), accent = c.tealDeep, label = "$pct%", labelSize = 12.sp, stroke = 5.dp)
            Column(Modifier.weight(1f)) {
                Text(hw.title, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Due ${prettyDateShort(hw.dueDate)}${if (hw.isPastDue) " · past due" else ""} · ${hw.turnedInCount}/${hw.totalCount} turned in",
                    style = VTheme.type.caption.colored(if (hw.isPastDue) c.warningInk else c.ink3).copy(fontSize = 11.5.sp),
                )
            }
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOARD mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeworkBoardMode(viewModel: TeacherHomeworkViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    val board = state.board
    var closeConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(999.dp)).background(c.cream)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.closeBoard() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = c.ink, modifier = Modifier.size(15.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(board?.title ?: "Submissions", style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
                            Text("Due ${prettyDateShort(board?.dueDate)}", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
                        }
                    }
                    if (board != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TMetricTile(board.submittedCount.toString(), "Submitted", c.successInk, Modifier.weight(1f))
                            TMetricTile(board.lateCount.toString(), "Late", c.warningInk, Modifier.weight(1f))
                            TMetricTile(board.gradedCount.toString(), "Graded", c.accent, Modifier.weight(1f))
                            TMetricTile(board.notSubmittedCount.toString(), "Pending", c.dangerInk, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VButton("Extend for class", onClick = { viewModel.openExtension(null) }, modifier = Modifier.weight(1f), variant = VButtonVariant.Secondary, tone = VButtonTone.Sky, size = VButtonSize.Sm, leading = { Icon(VIcons.Clock, contentDescription = null, modifier = Modifier.size(14.dp)) })
                            VButton("Close", onClick = { closeConfirm = true }, modifier = Modifier.weight(1f), variant = VButtonVariant.Ghost, size = VButtonSize.Sm)
                        }
                    }
                }
            }
        }

        when {
            state.isBoardLoading && board == null -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
            board == null -> item { TCard { Text("Couldn't load the board.", style = VTheme.type.body.colored(c.ink2)) } }
            else -> items(board.rows, key = { it.studentId }) { row ->
                BoardStudentRow(row, updating = state.updatingStudentId == row.studentId, onReview = { st -> viewModel.reviewSubmission(row.studentId, st) }, onExtend = { viewModel.openExtension(row.studentId, row.name) })
            }
        }
    }

    if (state.isExtensionOpen) ExtensionSheet(viewModel)
    if (closeConfirm) {
        TeacherConfirmDialog(
            title = "Close this homework?",
            body = "Closing archives the homework. Students can no longer submit.",
            confirmLabel = "Close it",
            destructive = true,
            onConfirm = { closeConfirm = false; viewModel.closeHomework() },
            onDismiss = { closeConfirm = false },
        )
    }
}

@Composable
private fun BoardStudentRow(row: HomeworkBoardRow, updating: Boolean, onReview: (String) -> Unit, onExtend: () -> Unit) {
    val c = VTheme.colors
    val (tint, label) = when (row.status) {
        HomeworkSubmissionStatus.SUBMITTED -> c.successInk to "Submitted"
        HomeworkSubmissionStatus.LATE -> c.warningInk to "Late"
        HomeworkSubmissionStatus.GRADED -> c.accent to "Graded"
        else -> c.dangerInk to "Not submitted"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).border(1.dp, c.hairline, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(
                    buildString {
                        append(if (row.rollNo != null) "Roll ${row.rollNo}" else row.studentCode)
                        if (row.hasExtension && !row.extendedTo.isNullOrBlank()) append(" · extended to ${prettyDateShort(row.extendedTo)}")
                    },
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                )
            }
            if (updating) TeacherSpinner(16.dp) else TPill(label.uppercase(), bg = tint.copy(alpha = 0.14f), fg = tint)
        }
        // Review actions only when there's something turned in.
        if (row.status == HomeworkSubmissionStatus.SUBMITTED || row.status == HomeworkSubmissionStatus.LATE) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VButton("Mark graded", onClick = { onReview(HomeworkSubmissionStatus.GRADED) }, modifier = Modifier.weight(1f), variant = VButtonVariant.Secondary, tone = VButtonTone.Mint, size = VButtonSize.Sm)
                VButton("Extend", onClick = onExtend, modifier = Modifier.weight(1f), variant = VButtonVariant.Ghost, size = VButtonSize.Sm)
            }
        } else if (row.status == HomeworkSubmissionStatus.NOT_SUBMITTED) {
            Spacer(Modifier.height(8.dp))
            VButton("Grant extension", onClick = onExtend, full = true, variant = VButtonVariant.Ghost, size = VButtonSize.Sm, leading = { Icon(VIcons.Clock, contentDescription = null, modifier = Modifier.size(14.dp)) })
        }
    }
}

@Composable
private fun ExtensionSheet(viewModel: TeacherHomeworkViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    Box(
        Modifier.fillMaxSize().background(c.navyDeep.copy(alpha = 0.42f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.closeExtension() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.padding(24.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .clip(RoundedCornerShape(24.dp)).background(c.card).border(1.dp, c.hairline, RoundedCornerShape(24.dp)).padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (state.extensionStudentId == null) "Extend for the whole class" else "Extend for ${state.extensionStudentName}",
                    style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold),
                )
                VDatePicker(value = state.extensionDate, onValueChange = viewModel::setExtensionDate, label = "New due date")
                VInput(value = state.extensionReason, onValueChange = viewModel::setExtensionReason, label = "Reason (optional)", placeholder = "Why the extension?")
                if (state.extensionError != null) Text(state.extensionError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VButton("Cancel", onClick = { viewModel.closeExtension() }, modifier = Modifier.weight(1f), variant = VButtonVariant.Ghost, size = VButtonSize.Md)
                    VButton("Grant", onClick = { viewModel.grantExtension() }, modifier = Modifier.weight(1f), tone = VButtonTone.Sky, size = VButtonSize.Md, loading = state.isGrantingExtension)
                }
            }
        }
    }
}
