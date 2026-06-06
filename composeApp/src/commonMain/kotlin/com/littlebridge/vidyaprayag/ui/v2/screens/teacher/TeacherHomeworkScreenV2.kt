package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.littlebridge.vidyaprayag.feature.teacher.presentation.Homework
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeworkViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherHomeworkScreenV2 — the Homework sub-tab of Teacher.tsx → Update.
 *
 * A compact "assign new" composer (class/title/description/due) plus the list of existing
 * assignments with a submission-ratio bar. Bound to [TeacherHomeworkViewModel]; the one-shot
 * `createSuccess` flag clears the composer once consumed.
 */
@Composable
fun TeacherHomeworkScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TeacherHomeworkViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    var classId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    LaunchedEffect(state.createSuccess) {
        if (state.createSuccess) {
            classId = ""; title = ""; description = ""; dueDate = ""
            viewModel.consumeCreateSuccess()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        Text("Homework", style = VTheme.type.h2.colored(c.ink))

        VCard {
            VSectionHeader("ASSIGN NEW")
            Spacer(Modifier.height(d.sm))
            VInput(value = classId, onValueChange = { classId = it }, label = "Class", placeholder = "e.g. 8-A", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(d.sm))
            VInput(value = title, onValueChange = { title = it }, label = "Title", placeholder = "Chapter 4 worksheet", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(d.sm))
            VInput(value = description, onValueChange = { description = it }, label = "Description", placeholder = "Instructions", singleLine = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(d.sm))
            VInput(value = dueDate, onValueChange = { dueDate = it }, label = "Due date", placeholder = "YYYY-MM-DD", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(d.md))
            VButton(
                text = "Assign homework",
                onClick = { viewModel.create(classId, title, description, dueDate) },
                full = true,
                size = VButtonSize.Md,
                tone = VButtonTone.Teal,
                soft = false,
                loading = state.isCreating,
                enabled = classId.isNotBlank() && title.isNotBlank() && dueDate.isNotBlank(),
            )
        }

        VSectionHeader("ASSIGNED")
        if (state.items.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No homework yet", icon = VIcons.Bookmark, body = "Assign your first task above.")
        } else {
            state.items.forEach { HomeworkRow(it) }
        }

        if (state.error != null) Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun HomeworkRow(h: Homework) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
            Text(h.title, style = VTheme.type.h4.colored(c.ink), modifier = Modifier.weight(1f))
            VBadge(text = "DUE ${h.dueDate}", tone = VBadgeTone.Warning)
        }
        Text("${h.className} · ${h.subject}", style = VTheme.type.caption.colored(c.ink3))
        if (h.description.isNotBlank()) {
            Text(h.description, style = VTheme.type.body.colored(c.ink2))
        }
        Spacer(Modifier.height(VTheme.dimens.sm))
        Text("${h.submittedCount}/${h.totalCount} submitted", style = VTheme.type.dataSm.colored(c.ink2))
        VProgressBar(value = h.submissionRatio, tone = VBadgeTone.Success)
    }
}
