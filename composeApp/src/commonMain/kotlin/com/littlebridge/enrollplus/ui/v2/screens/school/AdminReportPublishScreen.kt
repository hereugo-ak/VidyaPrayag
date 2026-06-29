package com.littlebridge.enrollplus.ui.v2.screens.school

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels
import com.littlebridge.enrollplus.feature.reportcard.presentation.AdminReportPublishViewModel
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * AdminReportPublishScreen — admin oversight for report card generation
 * across all classes. Shows draft status counts and allows publishing
 * approved drafts per class.
 */
@Composable
fun AdminReportPublishScreen(
    onBack: () -> Unit,
    viewModel: AdminReportPublishViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors
    var termInput by remember { mutableStateOf("Term 1") }

    LaunchedEffect(Unit) { viewModel.loadTermConfig() }

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
            Text("Report Card Publishing", style = VTheme.type.h3.colored(c.ink))
        }

        // Term input + load
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = termInput,
                onValueChange = { termInput = it },
                label = { Text("Term") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            VButton(
                text = "Load",
                onClick = { viewModel.loadOversight(termInput) },
                size = VButtonSize.Sm,
            )
        }

        // Term config info
        state.termConfig?.let { config ->
            VCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigChip("Enabled", if (config.enabled) "Yes" else "No")
                    ConfigChip("Current Term", config.currentTerm ?: "Not set")
                    ConfigChip("Concurrency", config.batchConcurrency.toString())
                    ConfigChip("Fallback", if (config.fallbackOnAiFail) "On" else "Off")
                }
            }
        }

        state.publishedCount?.let { count ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                VCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(VIcons.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
                        Text("$count reports published successfully", style = VTheme.type.body.colored(c.ink))
                    }
                }
            }
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
            state.oversight != null -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.oversight!!.classes) { row ->
                        OversightClassCard(
                            row = row,
                            publishing = state.publishing,
                            onPublish = { viewModel.publishClass(row.className, row.section, row.term) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigChip(label: String, value: String) {
    val c = VTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = VTheme.type.body.colored(c.ink).copy(fontWeight = FontWeight.Medium, fontSize = 13.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

@Composable
private fun OversightClassCard(
    row: ReportCardModels.ClassOversightRow,
    publishing: Boolean,
    onPublish: () -> Unit,
) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${row.className} ${row.section}", style = VTheme.type.h3.colored(c.ink).copy(fontSize = 15.sp))
                Text("${row.totalDrafts} drafts", style = VTheme.type.caption.colored(c.ink2))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip("Draft", row.draftCount, c.warning)
                StatusChip("Flagged", row.flaggedCount, c.danger)
                StatusChip("Approved", row.approvedCount, c.success)
                StatusChip("Published", row.publishedCount, c.accent)
            }

            if (row.approvedCount > 0 && row.publishedCount == 0) {
                VButton(
                    text = if (publishing) "Publishing…" else "Publish ${row.approvedCount} Approved",
                    onClick = onPublish,
                    size = VButtonSize.Sm,
                    enabled = !publishing,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    val c = VTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("$count", style = VTheme.type.body.colored(color).copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}
