package com.littlebridge.enrollplus.ui.v2.screens.tutor

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.tutor.domain.model.PlanItemDto
import com.littlebridge.enrollplus.feature.tutor.presentation.TutorPlanViewModel
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
 * TutorPlanScreen — today's adaptive plan + due reviews.
 *
 * Shows FSRS-scheduled review items sorted by priority, weak topics,
 * and the recommended action for the child.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4 (Tier 3 Act — Plan)
 */
@Composable
fun TutorPlanScreen(
    onBack: () -> Unit = {},
    childId: String? = null,
    subjectId: String = "",
    modifier: Modifier = Modifier,
    viewModel: TutorPlanViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors

    androidx.compose.runtime.LaunchedEffect(subjectId) {
        if (subjectId.isNotBlank()) viewModel.updateSubject(subjectId)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            VBackHeader(title = "Study Plan", onBack = onBack)

            when {
                state.isLoading -> VLoadingState()
                state.error != null -> VErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.loadPlan() },
                )
                state.planItems.isEmpty() -> VEmptyState(
                    title = "No plan yet",
                    body = "Your adaptive study plan will appear here.",
                    icon = VIcons.BookOpen,
                )
                else -> PlanContent(state.planItems)
            }
        }
    }
}

@Composable
private fun PlanContent(planItems: List<PlanItemDto>) {
    val c = VTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Due Reviews (${planItems.size})",
                style = VTheme.type.h3.colored(c.ink),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(planItems) { item ->
            ReviewCard(item)
        }
    }
}

@Composable
private fun ReviewCard(item: PlanItemDto) {
    val c = VTheme.colors
    val priorityColor = when {
        item.difficulty >= 0.7 -> c.warmOrange
        item.difficulty >= 0.4 -> c.accent
        else -> c.teal
    }
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Topic: ${item.topicId.take(8)}...",
                    style = VTheme.type.body.colored(c.ink),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Due: ${item.dueAt}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
                Text(
                    "Reps: ${item.reps}  Lapses: ${item.lapses}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(priorityColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "DUE",
                    style = VTheme.type.caption.colored(priorityColor),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

