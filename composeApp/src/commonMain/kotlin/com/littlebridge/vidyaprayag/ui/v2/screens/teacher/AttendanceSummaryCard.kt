package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P2-T4 — Attendance Summary Card
//
// A glanceable "how did today's roll-call go" card for the teacher Home tab.
// Left 60%: per-class present/total rows with a percentage pill.
// Right 40%: a Canvas donut of the day's overall attendance %, StatusPresent fill
// over a SurfaceSubtle track, DataLarge percentage in the centre.
//
// Data is server-authoritative — the card never recomputes class lists or totals;
// it renders whatever AttendanceDaySummary the ViewModel hands it. All colour,
// type, shape and spacing decisions go through Enroll.* tokens (no hardcoded hex).
// ─────────────────────────────────────────────────────────────────────────────

/** One class's roll-call tally for today (server-computed, no client drift). */
data class ClassAttendanceStat(
    val className: String,
    val present: Int,
    val total: Int,
) {
    /** Whole-number percentage, clamped 0..100. Empty rosters read as 0%. */
    val percent: Int
        get() = if (total <= 0) 0 else ((present.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
}

/** The teacher's whole-day attendance picture, aggregated across classes taught today. */
data class AttendanceDaySummary(
    val classes: List<ClassAttendanceStat> = emptyList(),
) {
    val totalPresent: Int get() = classes.sumOf { it.present }
    val totalStudents: Int get() = classes.sumOf { it.total }

    /** Overall whole-number percentage across every class taught today. */
    val overallPercent: Int
        get() = if (totalStudents <= 0) 0
        else ((totalPresent.toFloat() / totalStudents.toFloat()) * 100f).toInt().coerceIn(0, 100)

    val isEmpty: Boolean get() = classes.isEmpty()
}

/**
 * AttendanceSummaryCard — Home-tab summary of today's attendance, tap to open the
 * full attendance flow.
 *
 * @param summary           server-resolved per-class tallies for today
 * @param onOpenAttendance  navigate to the full AttendanceScreen (P7 deep-link target)
 */
@Composable
fun AttendanceSummaryCard(
    summary: AttendanceDaySummary,
    onOpenAttendance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (summary.isEmpty) return

    EnrollCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenAttendance,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Left 60%: section header + per-class stat rows ──────────────────
            Column(modifier = Modifier.weight(0.6f)) {
                SectionHeader(title = "ATTENDANCE TODAY")
                Spacer(Modifier.height(Enroll.space.md))
                summary.classes.forEachIndexed { index, stat ->
                    if (index > 0) Spacer(Modifier.height(Enroll.space.sm))
                    ClassStatRow(stat)
                }
            }

            Spacer(Modifier.width(Enroll.space.lg))

            // ── Right 40%: overall donut ────────────────────────────────────────
            Box(
                modifier = Modifier.weight(0.4f),
                contentAlignment = Alignment.Center,
            ) {
                AttendanceDonut(percent = summary.overallPercent)
            }
        }
    }
}

/** A single "Class 7A · 28/30 · 93%" row. */
@Composable
private fun ClassStatRow(stat: ClassAttendanceStat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stat.className,
            style = Enroll.type.bodyMedium.colored(Enroll.colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Enroll.space.sm))
        Text(
            text = "${stat.present}/${stat.total}",
            style = Enroll.type.dataSmall.colored(Enroll.colors.textSecondary),
        )
        Spacer(Modifier.width(Enroll.space.sm))
        PercentPill(percent = stat.percent)
    }
}

/** A compact percentage chip; tinted green when attendance is healthy. */
@Composable
private fun PercentPill(percent: Int) {
    val healthy = percent >= 90
    val bg = if (healthy) Enroll.colors.statusPresentSoft else Enroll.colors.surfaceSubtle
    val fg = if (healthy) Enroll.colors.statusPresent else Enroll.colors.textSecondary
    Box(
        modifier = Modifier
            .clip(Enroll.shape.pill)
            .background(bg)
            .padding(horizontal = Enroll.space.sm, vertical = Enroll.space.xs),
    ) {
        Text(
            text = "$percent%",
            style = Enroll.type.labelBold.colored(fg),
        )
    }
}

/**
 * A custom Canvas donut: a SurfaceSubtle full-circle track with a StatusPresent
 * arc swept to [percent], animating its length on first composition. The centre
 * carries the DataLarge percentage in TextPrimary.
 */
@Composable
private fun AttendanceDonut(percent: Int) {
    val track = Enroll.colors.surfaceSubtle
    val fill = Enroll.colors.statusPresent
    val progress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(800),
        label = "attendanceDonut",
    )

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(96.dp)) {
            val sw = 12.dp.toPx()
            val inset = sw / 2f
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(inset, inset)
            // track
            drawArc(
                color = track,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw),
            )
            // present fill, sweeping clockwise from 12 o'clock
            drawArc(
                color = fill,
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$percent%",
            style = Enroll.type.dataLarge.colored(Enroll.colors.textPrimary),
        )
    }
}
