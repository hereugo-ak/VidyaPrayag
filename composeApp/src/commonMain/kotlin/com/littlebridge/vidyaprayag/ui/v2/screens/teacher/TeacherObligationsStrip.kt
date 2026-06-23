package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ObligationItemDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherObligationsState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherObligationsViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherObligationsStrip — the Today "what needs me" strip (T-107, Doc 04 §5.5).
 *
 * REAL obligations only (kills B-HOME-4): the rows are the server's live,
 * allocation-scoped aggregate — unmarked classes, unpublished results, homework
 * to review, leave decisions. Each row deep-links (pre-scoped) to its tool via
 * [onOpenObligation], so a tap lands on the scoped surface, never a picker.
 *
 * HONESTY rules (Doc 04 §5.5):
 *  - **"All caught up" only when truly zero** — the green calm state shows ONLY
 *    when the load succeeded and every count is genuinely zero
 *    ([TeacherObligationsState.isAllCaughtUp]); it is never a default.
 *  - **Never fabricate** — when the strip couldn't load (`unavailable`) it hides
 *    entirely rather than pretending everything is done; the initial load shows
 *    nothing (no skeleton lie), then animates in once real data arrives.
 */
@Composable
fun TeacherObligationsStrip(
    onOpenObligation: (ObligationItemDto) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherObligationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    TeacherObligationsStripContent(
        state = state,
        onOpenObligation = onOpenObligation,
        modifier = modifier,
    )
}

@Composable
private fun TeacherObligationsStripContent(
    state: TeacherObligationsState,
    onOpenObligation: (ObligationItemDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    // Honesty: hide on read-failure and on the very first (pre-loaded) frame so we
    // never render a fabricated or stale strip. We only show once we KNOW the truth.
    val visible = state.loaded && !state.unavailable
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (state.isAllCaughtUp) {
            // ── All caught up (earned, calm green) ──────────────────────────────
            VCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().semantics { contentDescription = "All caught up — nothing outstanding" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LeadingDot(fill = c.success, icon = VIcons.Sparkles, iconTint = c.successInk)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "All caught up",
                            style = VTheme.type.bodyStrong.colored(c.ink)
                                .copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                        )
                        Text(
                            "Nothing needs you right now",
                            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                        )
                    }
                }
            }
        } else {
            // ── Outstanding work (real, deep-linkable rows) ─────────────────────
            VCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        VLabel("Needs you")
                        // Honest rolled-up count chip (feeds the same number the dock badge uses).
                        val total = state.totalOutstanding
                        if (total > 0) CountChip(total, c.warning, c.warningInk)
                    }

                    state.items.forEach { item ->
                        ObligationRow(item = item, onClick = { onOpenObligation(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ObligationRow(
    item: ObligationItemDto,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val (icon, tint, ink) = item.type.visuals(c)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .background(c.accentTint)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .semantics {
                contentDescription = buildString {
                    append(item.title)
                    if (item.subtitle.isNotBlank()) append(", ").append(item.subtitle)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LeadingDot(fill = tint, icon = icon, iconTint = ink)
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = VTheme.type.bodyStrong.colored(c.ink)
                    .copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 2,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    item.subtitle,
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        // A per-row count chip only when the row rolls up more than one unit
        // (e.g. "3 awaiting grading"); single-item rows stay clean.
        if (item.count > 1) CountChip(item.count, tint, ink)
        Icon(
            VIcons.ChevronRight,
            contentDescription = null,
            tint = c.ink2,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CountChip(count: Int, fill: Color, ink: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fill)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.toString(),
            style = VTheme.type.caption.colored(ink).copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp),
        )
    }
}

@Composable
private fun LeadingDot(fill: Color, icon: ImageVector, iconTint: Color) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
    }
}

/**
 * Map an obligation type to its (icon, plate, ink) visuals. Color is paired with an
 * icon + text so meaning never relies on hue alone (accessibility — Doc 10 §11).
 */
private fun String.visuals(c: com.littlebridge.vidyaprayag.ui.v2.theme.VColors): Triple<ImageVector, Color, Color> =
    when (this) {
        "attendance" -> Triple(VIcons.ClipboardList, c.warning, c.warningInk)
        "marks" -> Triple(VIcons.GraduationCap, c.success, c.successInk)
        "homework" -> Triple(VIcons.BookOpen, c.danger, c.dangerInk)
        "leave" -> Triple(VIcons.Calendar, c.warning, c.warningInk)
        else -> Triple(VIcons.FileText, c.warning, c.warningInk)
    }
