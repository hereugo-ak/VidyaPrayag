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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.littlebridge.enrollplus.feature.reportcard.presentation.TeacherReportDraftEditorViewModel
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * TeacherReportDraftEditorScreen — allows teachers to edit the AI-generated
 * narrative before approving. Shows the full draft JSON in an editable text field.
 */
@Composable
fun TeacherReportDraftEditorScreen(
    draftId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TeacherReportDraftEditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors

    LaunchedEffect(draftId) { viewModel.loadDraft(draftId) }

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
            Text("Edit Draft", style = VTheme.type.h3.colored(c.ink))
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error!!, style = VTheme.type.body.colored(c.danger))
                }
            }
            state.draft != null -> {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Draft metadata
                    VCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${state.draft!!.className} ${state.draft!!.section} • ${state.draft!!.term}",
                                style = VTheme.type.body.colored(c.ink).copy(fontWeight = FontWeight.Medium))
                            Text("Status: ${state.draft!!.status}", style = VTheme.type.caption.colored(c.ink2))
                            Text("Language: ${state.draft!!.language}", style = VTheme.type.caption.colored(c.ink3))
                        }
                    }

                    // Editable draft content
                    Text("AI Narrative (editable)", style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = state.editedContent,
                        onValueChange = { viewModel.updateContent(it) },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        textStyle = VTheme.type.body.colored(c.ink),
                    )

                    if (state.saved) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(VIcons.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
                            Text("Saved successfully", style = VTheme.type.body.colored(c.success))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = if (state.saving) "Saving…" else "Save Draft",
                            onClick = { viewModel.saveDraft() },
                            enabled = !state.saving,
                        )
                        VButton(
                            text = "Save & Back",
                            onClick = { viewModel.saveDraft(); onSaved() },
                            variant = VButtonVariant.Secondary,
                            enabled = !state.saving,
                        )
                    }
                }
            }
        }
    }
}
