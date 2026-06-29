package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels
import com.littlebridge.enrollplus.feature.reportcard.presentation.TeacherReportReviewViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * TeacherReportReviewQueueScreen — shows AI-generated report card drafts
 * for a class/term, allowing teachers to review, edit, approve, or regenerate.
 */
@Composable
fun TeacherReportReviewQueueScreen(
    className: String,
    section: String,
    term: String,
    onBack: () -> Unit,
    onEditDraft: (String) -> Unit,
    viewModel: TeacherReportReviewViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors

    LaunchedEffect(className, section, term) {
        viewModel.loadReviewQueue(className, section, term)
    }

    Column(
        Modifier.fillMaxSize().background(c.background),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VButton(text = "Back", onClick = onBack, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
            Column {
                Text("Report Card Review", style = VTheme.type.h3.colored(c.ink))
                Text("$className $section • $term", style = VTheme.type.caption.colored(c.ink2))
            }
        }

        // Summary bar
        if (state.drafts.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("Total", state.drafts.size, c.accent)
                SummaryChip("Draft", state.drafts.count { it.status == "draft" }, c.warning)
                SummaryChip("Flagged", state.drafts.count { it.status == "flagged_for_review" }, c.danger)
                SummaryChip("Approved", state.drafts.count { it.status == "approved" }, c.success)
            }
        }

        state.message?.let { msg ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                VCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(VIcons.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
                        Text(msg, style = VTheme.type.body.colored(c.ink))
                    }
                }
            }
            LaunchedEffect(msg) { kotlinx.coroutines.delay(3000); viewModel.clearMessage() }
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!, style = VTheme.type.body.colored(c.danger))
                        Spacer(Modifier.height(8.dp))
                        VButton(text = "Retry", onClick = { viewModel.loadReviewQueue(className, section, term) })
                    }
                }
            }
            state.isEmpty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No drafts found for this class/term", style = VTheme.type.body.colored(c.ink2))
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.drafts) { draft ->
                        DraftReviewCard(
                            draft = draft,
                            approving = state.approvingId == draft.id,
                            regenerating = state.regeneratingId == draft.id,
                            onApprove = { viewModel.approveDraft(draft.id) },
                            onRegenerate = { viewModel.regenerateDraft(draft.id) },
                            onEdit = { onEditDraft(draft.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    val c = VTheme.colors
    Column(
        Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$count", style = VTheme.type.h3.colored(color).copy(fontSize = 16.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

@Composable
private fun DraftReviewCard(
    draft: ReportCardModels.DraftDto,
    approving: Boolean,
    regenerating: Boolean,
    onApprove: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = VTheme.colors
    val statusTone = when (draft.status) {
        "draft" -> VBadgeTone.Neutral
        "flagged_for_review" -> VBadgeTone.Warning
        "approved" -> VBadgeTone.Success
        "published" -> VBadgeTone.Arctic
        else -> VBadgeTone.Neutral
    }
    val statusLabel = when (draft.status) {
        "draft" -> "Draft"
        "flagged_for_review" -> "Flagged"
        "approved" -> "Approved"
        "published" -> "Published"
        else -> draft.status
    }

    VCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Student: ${draft.studentId.take(8)}…", style = VTheme.type.body.colored(c.ink).copy(fontWeight = FontWeight.Medium))
                VBadge(text = statusLabel, tone = statusTone)
            }

            if (draft.groundingFlags != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(VIcons.AlertCircle, contentDescription = null, tint = c.warning, modifier = Modifier.size(12.dp))
                    Text("Grounding flags detected", style = VTheme.type.caption.colored(c.warning).copy(fontSize = 11.sp))
                }
            }

            if (draft.aiDraft != null) {
                val preview = draft.aiDraft.take(120) + if (draft.aiDraft.length > 120) "…" else ""
                Text(preview, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 12.sp), maxLines = 3)
            }

            if (draft.status == "draft" || draft.status == "flagged_for_review") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Edit", onClick = onEdit, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
                    VButton(
                        text = if (approving) "…" else "Approve",
                        onClick = onApprove,
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Sm,
                        enabled = !approving,
                    )
                    VButton(
                        text = if (regenerating) "…" else "Regenerate",
                        onClick = onRegenerate,
                        variant = VButtonVariant.Secondary,
                        size = VButtonSize.Sm,
                        enabled = !regenerating,
                    )
                }
            }
        }
    }
}
