package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentHomeState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentHomeViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
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
 * ParentHomeScreenV2 — a faithful copy of `Parent.tsx → ParentHome`.
 *
 * Renders the greeting, the gradient child-hero card (avatar + name + level + overall-progress bar),
 * actionable alerts (e.g. overdue fees), and featured schools.
 *
 * **Wired to the real [ParentHomeViewModel]** (`shared/`) → `ParentRepository.getDashboard` →
 * `GET /api/v1/parent/dashboard` — the primary parent "handshake" endpoint.
 *
 * Audit findings **J** (§8.1) + **SHAREDVM** (§5.4): this tab previously borrowed the academics
 * `TrackProgressViewModel`, leaving the richer `/dashboard` endpoint orphaned and coupling Home's
 * state to the Academics tab. It now has its own dedicated VM and real source of truth.
 */
@Composable
fun ParentHomeScreenV2(
    modifier: Modifier = Modifier,
    onDiscoverSchools: () -> Unit = {},
    viewModel: ParentHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    ParentHomeContent(
        state = state,
        onRetry = viewModel::load,
        onSelectChild = viewModel::selectChild,
        onDiscoverSchools = onDiscoverSchools,
        modifier = modifier,
    )
}

/** Stateless body — also used by @Preview with seeded state (no MockV2 in the live path). */
@Composable
private fun ParentHomeContent(
    state: ParentHomeState,
    onRetry: () -> Unit,
    onSelectChild: (String) -> Unit,
    onDiscoverSchools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.childSummary == null &&
                state.alerts.isEmpty() &&
                state.featuredSchools.isEmpty(),
            emptyTitle = "No child linked yet",
            emptyBody = "Link your child to see their daily journey and progress.",
            onRetry = onRetry,
            skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonDashboard() },
        ) {
            // ── Greeting ────────────────────────────────────────────────────────
            if (state.greeting.isNotBlank()) {
                Text(state.greeting, style = VTheme.type.h2.colored(c.ink))
            }

            // ── Child switcher (RA-31: only when 2+ children are linked) ─────────
            if (state.children.size > 1) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.children.forEach { ch ->
                        ChildChip(
                            name = ch.name.ifBlank { "—" },
                            selected = ch.id == state.selectedChildId,
                            onClick = { onSelectChild(ch.id) },
                        )
                    }
                }
            }

            // ── Child hero ──────────────────────────────────────────────────────
            val child = state.childSummary
            if (child != null) {
                VCard(padding = 0.dp) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = if (c.isNight) listOf(c.cream, c.card)
                                    else listOf(Color(0xFFF6F1FF), Color(0xFFE8F7F3)),
                                ),
                            )
                            .padding(20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            VAvatar(name = child.name.ifBlank { "?" }, src = child.profilePic, size = 68.dp, ring = true)
                            Column(Modifier.weight(1f)) {
                                Text(child.name.ifBlank { "—" }, style = VTheme.type.h2.colored(c.ink))
                                Text(
                                    "Attendance: ${child.attendanceStatus}",
                                    style = VTheme.type.caption.colored(c.ink2),
                                )
                                Spacer(Modifier.height(8.dp))
                                VBadge(text = "Level ${child.currentLevel}", tone = VBadgeTone.Arctic)
                            }
                        }
                    }
                    // overall-progress strip
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                            VLabel("Overall progress")
                            Text(
                                "${(child.overallProgress * 100.0).toInt()}%",
                                style = VTheme.type.dataLg.colored(c.navy).copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                            )
                        }
                        VProgressBar(value = (child.overallProgress * 100.0).toFloat())
                    }
                }
            }

            // ── Alerts (overdue fees, info) ─────────────────────────────────────
            if (state.alerts.isNotEmpty()) {
                VCard {
                    VLabel("Alerts")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.alerts.forEach { a ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(a.title, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp))
                                VBadge(
                                    text = a.value,
                                    tone = when (a.type.uppercase()) {
                                        "CRITICAL" -> VBadgeTone.Danger
                                        "WARNING" -> VBadgeTone.Warning
                                        else -> VBadgeTone.Neutral
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── Featured schools ────────────────────────────────────────────────
            if (state.featuredSchools.isNotEmpty()) {
                VCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        VLabel("Featured schools")
                        // Opens the full Discovery marketplace as a portal overlay.
                        Text(
                            "View all ›",
                            style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.clickable { onDiscoverSchools() },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.featuredSchools.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                VAvatar(name = s.name, src = s.image, size = 40.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp))
                                    Text(s.location, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                                }
                                VBadge(text = "★ ${s.rating}", tone = VBadgeTone.Success)
                            }
                        }
                    }
                    if (state.curationLogic.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.curationLogic, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

/**
 * RA-31: a child selector chip used when a parent has 2+ linked children.
 * Frozen V* primitives + theme tokens only (teal/tealDeep, cream, ink) — no
 * new tokens, no Material defaults.
 */
@Composable
private fun ChildChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val (bg, fg) = if (selected) c.teal.copy(alpha = 0.16f) to c.tealDeep else c.cream to c.ink2
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VAvatar(name = name, size = 24.dp)
        Text(name, style = VTheme.type.label.colored(fg))
    }
}
