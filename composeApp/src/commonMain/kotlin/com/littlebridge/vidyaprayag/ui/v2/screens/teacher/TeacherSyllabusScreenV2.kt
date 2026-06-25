package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.SyllabusUnit
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherSyllabusViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherSyllabusScreenV2 — the scoped syllabus tracker (Doc 08 §2). Reached PRE-SCOPED with a
 * pre-authorized [assignmentId]. The core gesture is a SINGLE TAP on a unit row → optimistic
 * coverage toggle (no form, no save). Hierarchy (chapter ▸ topic) comes pre-flattened from the
 * server. An "Edit" toggle reveals the deliberate add/rename affordances behind it.
 */
@Composable
fun TeacherSyllabusScreenV2(
    assignmentId: String,
    scopeLabel: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherSyllabusViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) viewModel.load(assignmentId)
    }

    Box(modifier.fillMaxSize().background(c.background)) {
        when {
            state.isLoading && state.units.isEmpty() -> TeacherCenterState { TeacherSpinner() }
            state.error != null && state.units.isEmpty() -> TeacherCenterState {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load syllabus", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(12.dp))
                    VButton("Retry", onClick = { viewModel.retry() }, tone = VButtonTone.Lavender)
                }
            }
            else -> SyllabusBody(viewModel, scopeLabel)
        }
    }
}

@Composable
private fun SyllabusBody(viewModel: TeacherSyllabusViewModel, scopeLabel: String) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    val pct = (state.progress * 100).toInt()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TRing(percent = pct, modifier = Modifier.size(72.dp), accent = c.tealDeep, label = "$pct%", labelSize = 16.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        TEyebrow("SYLLABUS", dot = c.tealDeep)
                        Spacer(Modifier.height(4.dp))
                        Text(scopeLabel.ifBlank { "${state.className}-${state.section} · ${state.subject}" }, style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
                        Text("${state.coveredCount} of ${state.totalCount} units covered", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
                    }
                    val ix = remember { MutableInteractionSource() }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (state.isEditing) c.accent.copy(alpha = 0.14f) else c.cream)
                            .clickable(interactionSource = ix, indication = null) { viewModel.toggleEditing() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(VIcons.Edit3, contentDescription = "Edit", tint = if (state.isEditing) c.accentDeep else c.ink2, modifier = Modifier.size(16.dp)) }
                }
            }
        }

        if (state.isEditing) {
            item {
                if (state.addingUnderParentId == null) {
                    VButton("Add a chapter", onClick = { viewModel.openAdd(null) }, full = true, variant = VButtonVariant.Secondary, tone = VButtonTone.Teal, size = VButtonSize.Md, leading = { Icon(VIcons.Plus, contentDescription = null, modifier = Modifier.size(15.dp)) })
                } else {
                    AddUnitComposer(viewModel)
                }
            }
        }

        if (state.units.isEmpty()) {
            item {
                TCard { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TIconDisc(VIcons.BookOpen, tint = c.tealDeep, bg = c.teal.copy(alpha = 0.14f), size = 48.dp, glyph = 24.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("No units yet", style = VTheme.type.h3.colored(c.ink))
                    Text("Tap Edit to add chapters & topics.", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                } }
            }
        } else {
            items(state.units, key = { it.id }) { u ->
                SyllabusRow(u, isUpdating = state.updatingUnitId == u.id, editing = state.isEditing, onToggle = { viewModel.toggleUnit(u.id) }, onAddTopic = { viewModel.openAdd(u.id) })
            }
        }
    }
}

@Composable
private fun AddUnitComposer(viewModel: TeacherSyllabusViewModel) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()
    val isChapter = state.addingUnderParentId.isNullOrBlank()
    TCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isChapter) "New chapter" else "New topic", style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
            VInput(value = state.addTitle, onValueChange = viewModel::setAddTitle, placeholder = if (isChapter) "Chapter title" else "Topic title")
            if (state.addError != null) Text(state.addError ?: "", style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VButton("Cancel", onClick = { viewModel.closeAdd() }, modifier = Modifier.weight(1f), variant = VButtonVariant.Ghost, size = VButtonSize.Md)
                VButton("Add", onClick = { viewModel.submitAdd() }, modifier = Modifier.weight(1f), tone = VButtonTone.Teal, size = VButtonSize.Md, loading = state.isAdding)
            }
        }
    }
}

@Composable
private fun SyllabusRow(u: SyllabusUnit, isUpdating: Boolean, editing: Boolean, onToggle: () -> Unit, onAddTopic: () -> Unit) {
    val c = VTheme.colors
    val indent = (u.depth.coerceIn(0, 3) * 16).dp
    val ix = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .clip(RoundedCornerShape(16.dp))
            .background(if (u.isCovered) c.teal.copy(alpha = 0.08f) else c.card)
            .border(1.dp, if (u.isCovered) c.teal.copy(alpha = 0.35f) else c.hairline, RoundedCornerShape(16.dp))
            .clickable(interactionSource = ix, indication = null, enabled = !isUpdating) { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Coverage check disc.
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (u.isCovered) c.tealDeep else c.cream).border(1.dp, if (u.isCovered) c.tealDeep else c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isUpdating) TeacherSpinner(14.dp)
            else if (u.isCovered) Icon(VIcons.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                u.title,
                style = (if (u.isChapter) VTheme.type.bodyStrong else VTheme.type.body).colored(c.ink).copy(fontSize = if (u.isChapter) 14.5.sp else 13.5.sp, fontWeight = if (u.isChapter) FontWeight.ExtraBold else FontWeight.Medium),
            )
            if (u.isCovered && !u.coveredOn.isNullOrBlank()) {
                Text("Covered ${prettyDateShort(u.coveredOn)}", style = VTheme.type.caption.colored(c.tealDeep).copy(fontSize = 10.5.sp))
            }
        }
        if (editing && u.isChapter) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(c.cream)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAddTopic() },
                contentAlignment = Alignment.Center,
            ) { Icon(VIcons.Plus, contentDescription = "Add topic", tint = c.ink2, modifier = Modifier.size(14.dp)) }
        }
    }
}
