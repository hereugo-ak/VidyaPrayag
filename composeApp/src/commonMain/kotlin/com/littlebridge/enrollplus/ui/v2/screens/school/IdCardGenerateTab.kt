package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardState
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VProgressBar
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

@Composable
internal fun GenerateTab(
    state: IdCardState,
    viewModel: IdCardViewModel,
) {
    val c = VTheme.colors
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var selectedScope by remember { mutableStateOf("all_students") }
    var classIdInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        state.error?.let { err ->
            VCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(err, color = c.dangerInk, style = VTheme.type.body)
            }
        }
        state.infoMessage?.let { msg ->
            VCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(msg, color = c.successInk, style = VTheme.type.body)
            }
        }

        if (state.templates.isEmpty()) {
            VEmptyState(
                title = "No templates available",
                body = "Create a template first in the Templates tab.",
                icon = Icons.Filled.School,
                modifier = Modifier.padding(top = 48.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            return
        }

        Text("SELECT TEMPLATE", style = VTheme.type.label.colored(c.ink3))
        Spacer(modifier = Modifier.height(8.dp))

        state.templates.forEach { template ->
            val isSelected = selectedTemplateId == template.id
            VCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .then(
                        if (isSelected) Modifier.border(2.dp, c.accent, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { selectedTemplateId = template.id },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text(
                            "${template.roleType.replaceFirstChar { it.uppercase() }} • ${if (template.isActive) "Active" else "Inactive"}",
                            style = VTheme.type.caption.colored(c.ink2),
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("SELECT SCOPE", style = VTheme.type.label.colored(c.ink3))
        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "all_students" to "All Students",
            "all_staff" to "All Staff",
            "class" to "By Class",
        ).forEach { (scope, label) ->
            val isSelected = selectedScope == scope
            VCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .then(
                        if (isSelected) Modifier.border(2.dp, c.accent, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { selectedScope = scope },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = VTheme.type.body.colored(c.ink))
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (selectedScope == "class") {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = classIdInput,
                onValueChange = { classIdInput = it },
                label = { Text("Class ID (UUID)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        VButton(
            text = if (state.isGenerating) "Generating..." else "Generate Cards",
            onClick = {
                selectedTemplateId?.let { tid ->
                    viewModel.clearMessages()
                    val classId = if (selectedScope == "class" && classIdInput.isNotBlank()) classIdInput else null
                    viewModel.generateCards(tid, selectedScope, classId)
                }
            },
            variant = VButtonVariant.Primary,
            enabled = !state.isGenerating && selectedTemplateId != null &&
                (selectedScope != "class" || classIdInput.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.isGenerating) {
            Spacer(modifier = Modifier.height(12.dp))
            VProgressBar(value = 50f, tone = VBadgeTone.Accent, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rendering and uploading cards in parallel...",
                style = VTheme.type.caption.colored(c.ink3),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
