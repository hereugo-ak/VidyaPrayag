package com.littlebridge.vidyaprayag.ui.v2.screens.parent

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeState
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentFeesScreenV2 — a pixel-faithful copy of `Parent.tsx → Fees`.
 *
 * Navy-gradient balance hero (outstanding fees + collection progress bar) and the school's
 * fee-related announcements feed. **Wired to the real [FeeViewModel]** (`shared/`) →
 * `ParentRepository.getFees` → `GET /api/v1/parent/fees`. MockV2 is no longer referenced; the
 * three UI states (loading / error / empty) are handled by [VStateHost].
 */
@Composable
fun ParentFeesScreenV2(
    modifier: Modifier = Modifier,
    viewModel: FeeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    ParentFeesContent(state = state, modifier = modifier)
}

/** Stateless body — also used by the @Preview with seeded state (no MockV2 in the live path). */
@Composable
private fun ParentFeesContent(
    state: FeeState,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(d.sm + 4.dp),
    ) {
        Text("Fees", style = VTheme.type.h1.colored(c.ink))

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.announcements.isEmpty() &&
                state.outstandingFees.isBlank() &&
                state.totalCollected.isBlank(),
            emptyTitle = "No fee records yet",
            emptyBody = "Once your school publishes fees, they'll appear here.",
            skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonFee() },
        ) {
            // ── Hero: outstanding balance + collection progress ─────────────────
            VCard(padding = 0.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(c.navy, Color(0xFF3B3870))))
                        .padding(20.dp),
                ) {
                    Column {
                        VLabel("Balance due", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            state.outstandingFees,
                            style = VTheme.type.dataLg.colored(Color.White)
                                .copy(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (state.overdueCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Box(Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFFFD4A3)))
                                Text(
                                    "${state.overdueCount} overdue",
                                    style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.78f)),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                // RA-PP-THEME: website-accent violet CTA (was green teal).
                                .background(c.accent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pay now", style = VTheme.type.bodyStrong.colored(Color.White))
                                Text(" · Coming Soon", style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.8f)))
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        VLabel("Collected this term")
                        Text(state.totalCollected, style = VTheme.type.data.colored(c.ink).copy(fontWeight = FontWeight.SemiBold))
                    }
                    VProgressBar(value = (state.collectionProgress * 100f))
                }
            }

            // ── Fee announcements ──────────────────────────────────────────────
            if (state.announcements.isNotEmpty()) {
                VLabel("Fee notices")
                state.announcements.forEach { a ->
                    VCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(a.title, style = VTheme.type.bodyStrong.colored(c.ink))
                                Text("${a.description} • ${a.time}", style = VTheme.type.caption.colored(c.ink2))
                            }
                            VBadge(
                                text = a.type,
                                tone = when (a.type) {
                                    "Emergency" -> VBadgeTone.Danger
                                    "Payment" -> VBadgeTone.Warning
                                    else -> VBadgeTone.Neutral
                                },
                            )
                        }
                    }
                }
            }

            Text(
                "Have a question about your fees? Message the school →",
                style = VTheme.type.caption.colored(c.accentDeep).copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
