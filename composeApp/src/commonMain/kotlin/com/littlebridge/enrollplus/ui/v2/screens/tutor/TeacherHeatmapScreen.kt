package com.littlebridge.enrollplus.ui.v2.screens.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.tutor.domain.model.HeatmapCellDto
import com.littlebridge.enrollplus.feature.tutor.domain.model.HeatmapDto
import com.littlebridge.enrollplus.feature.tutor.domain.model.TeacherScopeItemDto
import com.littlebridge.enrollplus.feature.tutor.presentation.TeacherHeatmapViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VErrorState
import com.littlebridge.enrollplus.ui.v2.screens.VLoadingState
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherHeatmapScreen — class misconception heatmap.
 *
 * Shows aggregated misconception data for the teacher's assigned
 * classes and subjects. Teachers can see which topics are challenging
 * students and how many children are affected.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Teacher heatmap)
 */
@Composable
fun TeacherHeatmapScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherHeatmapViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors

    Box(
        modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            VBackHeader(title = "Class Heatmap", onBack = onBack)

            when {
                state.isLoading && state.heatmap == null -> VLoadingState()
                state.error != null -> VErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.loadScope() },
                )
                state.scope.isEmpty() -> VEmptyState(
                    title = "No assignments",
                    body = "You have no class-subject assignments yet.",
                    icon = VIcons.BookOpen,
                )
                else -> {
                    if (state.scope.size > 1) {
                        ScopeSelector(
                            scope = state.scope,
                            selectedClassId = state.selectedClassId,
                            selectedSubjectId = state.selectedSubjectId,
                            onSelect = viewModel::selectScope,
                        )
                    }
                    if (state.heatmap != null) {
                        HeatmapContent(state.heatmap!!)
                    } else if (!state.isLoading) {
                        VEmptyState(
                            title = "No data",
                            body = "No misconceptions recorded for this class yet.",
                            icon = VIcons.BookOpen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeSelector(
    scope: List<TeacherScopeItemDto>,
    selectedClassId: String?,
    selectedSubjectId: String?,
    onSelect: (String, String) -> Unit,
) {
    val c = VTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(scope) { item ->
            val isSelected = item.classId == selectedClassId && item.subjectId == selectedSubjectId
            VCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item.classId, item.subjectId) },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            item.className,
                            style = VTheme.type.body.colored(c.ink),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            item.subjectName,
                            style = VTheme.type.caption.colored(c.ink3),
                        )
                    }
                    if (isSelected) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(c.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "SELECTED",
                                style = VTheme.type.caption.colored(c.accent),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapContent(heatmap: HeatmapDto) {
    val c = VTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    StatBlock("Children", heatmap.totalChildren.toString())
                    StatBlock("Misconceptions", heatmap.totalMisconceptions.toString())
                    StatBlock("Topics", heatmap.cells.size.toString())
                }
            }
        }

        items(heatmap.cells) { cell ->
            HeatmapCellCard(cell)
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    val c = VTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = VTheme.type.h2.colored(c.ink),
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = VTheme.type.caption.colored(c.ink3),
        )
    }
}

@Composable
private fun HeatmapCellCard(cell: HeatmapCellDto) {
    val c = VTheme.colors
    val severityColor = when (cell.severity) {
        "high" -> c.warmOrange
        "medium" -> c.accent
        else -> c.teal
    }
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cell.misconceptionType,
                    style = VTheme.type.body.colored(c.ink),
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(severityColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        cell.severity.uppercase(),
                        style = VTheme.type.caption.colored(severityColor),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                "${cell.affectedChildren} children affected",
                style = VTheme.type.caption.colored(c.ink3),
            )
            if (cell.evidence.isNotEmpty()) {
                Text(
                    "Evidence:",
                    style = VTheme.type.caption.colored(c.ink3),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                cell.evidence.take(2).forEach { ev ->
                    Text(
                        "• $ev",
                        style = VTheme.type.caption.colored(c.ink2),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
