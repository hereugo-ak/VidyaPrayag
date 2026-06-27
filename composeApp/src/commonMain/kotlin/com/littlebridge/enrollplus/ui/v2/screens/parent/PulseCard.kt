package com.littlebridge.enrollplus.ui.v2.screens.parent

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.parent.domain.model.PulseDto
import com.littlebridge.enrollplus.feature.parent.domain.model.PulseMarkEntry
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * PulseCard — the weekly Parent Pulse summary card.
 *
 * A premium, glanceable card showing:
 *   - Student name + week range
 *   - Attendance ring with trend arrow
 *   - Marks highlights (top score)
 *   - Homework pending/completed stat
 *   - Unread messages + announcements badges
 *   - AI narrative summary
 *   - Actionable items list
 */
@Composable
fun PulseCard(
    pulse: PulseDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val c = VTheme.colors

    VCard(modifier = modifier, padding = 16.dp, onClick = onClick) {
        // ── Header: student name + week range ──────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Activity, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                Text(
                    "WEEKLY PULSE",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                )
            }
            Text(
                pulse.weekRange,
                style = VTheme.type.label.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Student name ────────────────────────────────────────────────────
        Text(
            pulse.studentName,
            style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
        )

        Spacer(Modifier.height(14.dp))

        // ── Attendance ring + trend ─────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AttendanceRing(
                percentage = pulse.attendancePercentage,
                modifier = Modifier.size(72.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Attendance",
                    style = VTheme.type.label.colored(c.ink3).copy(fontSize = 12.sp),
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${pulse.attendancePercentage?.toInt() ?: "--"}%",
                        style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                    )
                    TrendArrow(trend = pulse.attendanceTrend)
                }
                // ── Quick stats row ──────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PulseStatChip(
                        label = "HW",
                        value = "${pulse.homeworkCompleted}/${pulse.homeworkCompleted + pulse.homeworkPending}",
                        tint = if (pulse.homeworkPending > 0) c.warning else c.success,
                    )
                    PulseStatChip(
                        label = "Msgs",
                        value = "${pulse.unreadMessages}",
                        tint = if (pulse.unreadMessages > 0) c.accent else c.ink3,
                    )
                    PulseStatChip(
                        label = "Alerts",
                        value = "${pulse.announcementsCount}",
                        tint = c.ink3,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── AI Narrative ────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.accent.copy(alpha = 0.06f))
                .padding(12.dp),
        ) {
            Text(
                pulse.aiNarrative,
                style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp, lineHeight = 18.sp),
            )
        }

        // ── Marks highlights ────────────────────────────────────────────────
        if (pulse.marksSummary.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Marks This Week",
                style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
            Spacer(Modifier.height(6.dp))
            pulse.marksSummary.take(3).forEach { mark ->
                MarkRow(mark)
                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Actionable items ────────────────────────────────────────────────
        if (pulse.actionableItems.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Action Items",
                style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
            Spacer(Modifier.height(6.dp))
            pulse.actionableItems.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c.accent),
                    )
                    Text(
                        item,
                        style = VTheme.type.body.colored(c.ink2).copy(fontSize = 12.sp),
                    )
                }
            }
        }

        // ── Upcoming events ─────────────────────────────────────────────────
        if (pulse.upcomingEvents.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Upcoming",
                style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
            Spacer(Modifier.height(6.dp))
            pulse.upcomingEvents.take(3).forEach { event ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(VIcons.Calendar, contentDescription = null, tint = c.ink3, modifier = Modifier.size(12.dp))
                    Text(
                        "${event.title} — ${event.date}",
                        style = VTheme.type.body.colored(c.ink2).copy(fontSize = 12.sp),
                    )
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun AttendanceRing(
    percentage: Double?,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val pct = (percentage ?: 0.0).toFloat().coerceIn(0f, 100f) / 100f
    val animated by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(800),
        label = "attendance_ring",
    )

    val ringColor = when {
        percentage == null -> c.ink3
        percentage >= 75 -> c.success
        percentage >= 50 -> c.warning
        else -> c.danger
    }

    Canvas(modifier) {
        val stroke = 8.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        // Background track
        drawArc(
            color = c.border1,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        // Progress arc
        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun TrendArrow(trend: String?) {
    val c = VTheme.colors
    if (trend == null) return
    val (icon, color) = when (trend) {
        "up" -> VIcons.TrendingUp to c.success
        "down" -> VIcons.TrendingDown to c.danger
        else -> VIcons.Minus to c.ink3
    }
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
}

@Composable
private fun PulseStatChip(
    label: String,
    value: String,
    tint: Color,
) {
    val c = VTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                label,
                style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp),
            )
            Text(
                value,
                style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
        }
    }
}

@Composable
private fun MarkRow(mark: PulseMarkEntry) {
    val c = VTheme.colors
    val pct = (mark.marks / mark.max * 100).toInt()
    val markColor = when {
        pct >= 75 -> c.success
        pct >= 50 -> c.warning
        else -> c.danger
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${mark.subject} — ${mark.test}",
            style = VTheme.type.body.colored(c.ink2).copy(fontSize = 12.sp),
            modifier = Modifier.weight(1f),
        )
        Text(
            "${mark.marks.toInt()}/${mark.max}",
            style = VTheme.type.label.colored(markColor).copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
        )
    }
}
