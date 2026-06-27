package com.littlebridge.enrollplus.ui.v2.screens.parent

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.parent.presentation.ParentPulseViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentPulseScreen — full-screen weekly pulse view.
 *
 * Shows the latest pulse card with a history toggle. When history is toggled,
 * displays a scrollable list of past pulse cards (up to 12 weeks).
 */
@Composable
fun ParentPulseScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentPulseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors

    Box(
        modifier
            .fillMaxSize()
            .background(c.background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accent.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.02f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Header with back button + history toggle ───────────────────
            VBackHeader(
                title = "Parent Pulse",
                onBack = onBack,
                action = {
                    if (state.latestPulse != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clickable { viewModel.toggleHistory() }
                                .padding(4.dp),
                        ) {
                            Icon(
                                if (state.showHistory) VIcons.Close else VIcons.History,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                if (state.showHistory) "Close" else "History",
                                style = VTheme.type.label.colored(c.accent).copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                },
            )

            // ── Content ─────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                when {
                    state.isLoading && state.latestPulse == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = c.accent, modifier = Modifier.size(36.dp))
                        }
                    }

                    state.error != null && state.latestPulse == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            VEmptyState(
                                icon = VIcons.Activity,
                                title = "No pulse yet",
                                body = state.error ?: "Check back after Sunday for the weekly summary.",
                            )
                        }
                    }

                    state.showHistory -> {
                        if (state.pulseHistory.isEmpty() && state.selectedChildId != null) {
                            LaunchedEffect(state.selectedChildId) {
                                viewModel.loadHistory(state.selectedChildId!!)
                            }
                        }
                        if (state.pulseHistory.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                VEmptyState(
                                    icon = VIcons.History,
                                    title = "No history yet",
                                    body = "Pulse history will appear here after a few weeks.",
                                )
                            }
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(state.pulseHistory) { pulse ->
                                    PulseCard(pulse = pulse)
                                }
                            }
                        }
                    }

                    state.latestPulse != null -> {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            PulseCard(pulse = state.latestPulse!!)

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleHistory() }
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    "View 12-week history",
                                    style = VTheme.type.label.colored(c.accent).copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    }

                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            VEmptyState(
                                icon = VIcons.Activity,
                                title = "No pulse available",
                                body = "Your child's weekly pulse will appear here every Sunday evening.",
                            )
                        }
                    }
                }
            }
        }
    }
}
