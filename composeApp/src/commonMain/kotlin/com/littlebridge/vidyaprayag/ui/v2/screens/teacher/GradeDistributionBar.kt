package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P3-T2 — Grade Distribution Bar
//
// A single horizontal bar that shows, at a glance, how a class split across grade
// bands for the selected exam: one gapless 8dp segmented bar (ShapeCard corners)
// over a legend of "A · 12   B · 8   …". Only rendered when an exam is selected
// (the host hides it in the "All Exams" view).
//
// Colours map across the portal's existing status palette (NO new hex, per the
// IMPORTANT NOTE): top bands green (statusPresent), mid bands amber (statusLate),
// bottom band red (statusAbsent). All via Enroll.*.
// ─────────────────────────────────────────────────────────────────────────────

/** One grade band of the distribution. `color` defaults are filled by [defaultGradeBands]. */
data class GradeBand(
    val label: String,            // "A", "B", "C", "D", "F"
    val count: Int,
    val color: Color = Color.Unspecified,
)

/**
 * Map the canonical A/B/C/D/F counts onto the portal status palette so the bar reads
 * the same as every other "good → bad" surface in the product.
 *   A,B → statusPresent (green) · C,D → statusLate (amber) · F → statusAbsent (red)
 */
@Composable
fun defaultGradeBands(a: Int, b: Int, c: Int, d: Int, f: Int): List<GradeBand> = listOf(
    GradeBand("A", a, Enroll.colors.statusPresent),
    GradeBand("B", b, Enroll.colors.statusPresentSoft),
    GradeBand("C", c, Enroll.colors.statusLate),
    GradeBand("D", d, Enroll.colors.statusLateSoft),
    GradeBand("F", f, Enroll.colors.statusAbsent),
)

/**
 * GradeDistributionBar — the segmented bar + count legend.
 *
 * @param bands  grade bands (use [defaultGradeBands] for the standard A–F mapping)
 */
@Composable
fun GradeDistributionBar(
    bands: List<GradeBand>,
    modifier: Modifier = Modifier,
) {
    val total = bands.sumOf { it.count }
    if (total <= 0) return

    val track = Enroll.colors.surfaceSubtle
    // Capture the brand fallback colour HERE, in the @Composable scope. The Canvas
    // draw lambda runs in DrawScope (NOT a composable context), so reading the
    // @Composable `Enroll.colors.primary` inside it is illegal — hoist it out.
    val fallbackColor = Enroll.colors.primary
    val progress by animateFloatAsState(targetValue = 1f, animationSpec = tween(700), label = "dist")

    Column(modifier = modifier.fillMaxWidth()) {
        // The gapless segmented bar.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(Enroll.shape.card),
        ) {
            val w = size.width
            val h = size.height
            // base track
            drawRect(color = track, topLeft = Offset.Zero, size = Size(w, h))
            // segments, left→right, no gaps
            var x = 0f
            bands.forEach { band ->
                if (band.count <= 0) return@forEach
                val segW = (band.count.toFloat() / total.toFloat()) * w * progress
                val segColor = if (band.color == Color.Unspecified) fallbackColor else band.color
                drawRect(color = segColor, topLeft = Offset(x, 0f), size = Size(segW, h))
                x += (band.count.toFloat() / total.toFloat()) * w
            }
        }

        Spacer(Modifier.height(Enroll.space.sm))

        // The "A · 12   B · 8 …" legend.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Enroll.space.md),
        ) {
            bands.forEach { band ->
                LegendItem(band)
            }
        }
    }
}

@Composable
private fun LegendItem(band: GradeBand) {
    val dot = if (band.color == Color.Unspecified) Enroll.colors.primary else band.color
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(Enroll.shape.pill)
                .background(dot),
        )
        Spacer(Modifier.size(Enroll.space.xs))
        Text(
            text = "${band.label} · ${band.count}",
            style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
        )
    }
}
