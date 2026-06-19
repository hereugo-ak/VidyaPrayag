package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.domain.model.ParentAttendanceData
import com.littlebridge.vidyaprayag.feature.parent.presentation.AttendanceDayState
import com.littlebridge.vidyaprayag.feature.parent.presentation.TodayAttendance
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * The dashboard's primary feature card — today's attendance verdict + this month's
 * attendance ring. A faithful port of the website reference's hero attendance card:
 * a "Live · today" eyebrow, a state badge, the headline verdict, and a violet→teal
 * gradient ring summarising the month.
 *
 * Commit 3 lands the default + every attendance state correctly. The swipe-within-card
 * transform into the month calendar lands in commit 4 (this composable becomes the
 * "front face" of that interaction).
 *
 * LAW: every state handled (present/absent/late/sunday/holiday/vacation/no-data); every
 * number from the real backend (counts + rate from /attendance), nothing hardcoded.
 */
@Composable
fun ParentAttendanceCard(
    today: TodayAttendance,
    attendance: ParentAttendanceData?,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val tone = toneFor(today.state)

    VCard(modifier = modifier, padding = 14.dp) {
        // ── eyebrow + state badge ──────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VStatusDot(color = tone.dot, size = 6.dp)
                Text(
                    "LIVE · TODAY",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                )
            }
            StatePill(label = tone.badge, bg = tone.badgeBg, fg = tone.badgeFg)
        }

        Spacer(Modifier.height(8.dp))

        // ── headline verdict + sub-line ────────────────────────────────────────
        Text(
            tone.headline,
            style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
        )
        if (tone.subline.isNotBlank()) {
            Text(tone.subline, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
        }

        // ── month ring (only when the backend gave us a month to summarise) ────
        if (attendance != null && attendance.totalDays > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AttendanceRing(
                    percent = attendance.attendanceRate.coerceIn(0, 100),
                    modifier = Modifier.size(48.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Attendance this month",
                        style = VTheme.type.bodyStrong.colored(c.navyDeep).copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        monthBreakdown(attendance),
                        style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp),
                    )
                }
            }
        }
    }
}

/** A small rounded pill rendering the state badge. */
@Composable
private fun StatePill(label: String, bg: Color, fg: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, style = VTheme.type.label.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
    }
}

/**
 * The month attendance ring — a violet→teal sweep over a soft track, with the percent
 * centred. The sweep animates in (springy) so the card feels alive on load.
 */
@Composable
private fun AttendanceRing(percent: Int, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val sweep by animateFloatAsState(targetValue = percent / 100f, label = "attendanceSweep")
    val gradient = Brush.linearGradient(listOf(c.accent, c.teal))
    val trackColor = c.accent.copy(alpha = 0.12f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            // track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // progress
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$percent%",
            style = VTheme.type.dataSm.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
        )
    }
}

/** A real, honest month breakdown line built from the backend counts. */
private fun monthBreakdown(a: ParentAttendanceData): String {
    val schoolDays = a.totalDays
    val attended = a.presentDays + a.lateDays
    val parts = buildList {
        add("$attended of $schoolDays school days")
        if (a.lateDays > 0) add("${a.lateDays} late")
        if (a.absentDays > 0) add("${a.absentDays} absent")
    }
    return parts.joinToString(" · ")
}

/** Visual + copy tokens for one resolved today-state — drives the whole card face. */
private data class AttendanceTone(
    val badge: String,
    val headline: String,
    val subline: String,
    val dot: Color,
    val badgeBg: Color,
    val badgeFg: Color,
)

@Composable
private fun toneFor(state: AttendanceDayState): AttendanceTone {
    val c = VTheme.colors
    return when (state) {
        AttendanceDayState.Present -> AttendanceTone(
            badge = "Present",
            headline = "Marked present today",
            subline = "Your child is in school",
            dot = c.successInk,
            badgeBg = c.success.copy(alpha = 0.42f),
            badgeFg = c.successInk,
        )
        AttendanceDayState.Late -> AttendanceTone(
            badge = "Late",
            headline = "Arrived late today",
            subline = "Marked present, after the bell",
            dot = c.warningInk,
            badgeBg = c.warning.copy(alpha = 0.55f),
            badgeFg = c.warningInk,
        )
        AttendanceDayState.Absent -> AttendanceTone(
            badge = "Absent",
            headline = "Marked absent today",
            subline = "No attendance recorded for today",
            dot = c.dangerInk,
            badgeBg = c.danger.copy(alpha = 0.55f),
            badgeFg = c.dangerInk,
        )
        AttendanceDayState.Holiday -> AttendanceTone(
            badge = "Holiday",
            headline = "School holiday today",
            subline = "",
            dot = c.accentDeep,
            badgeBg = c.accent.copy(alpha = 0.14f),
            badgeFg = c.accentDeep,
        )
        AttendanceDayState.Vacation -> AttendanceTone(
            badge = "Break",
            headline = "On vacation",
            subline = "Enjoy the break",
            dot = c.accentDeep,
            badgeBg = c.accent.copy(alpha = 0.14f),
            badgeFg = c.accentDeep,
        )
        AttendanceDayState.Sunday -> AttendanceTone(
            badge = "Sunday",
            headline = "No school today",
            subline = "It's a Sunday",
            dot = c.ink3,
            badgeBg = c.cream,
            badgeFg = c.ink2,
        )
        AttendanceDayState.NoData -> AttendanceTone(
            badge = "Awaiting",
            headline = "Attendance not marked yet",
            subline = "You'll see today's status once the class is marked",
            dot = c.ink3,
            badgeBg = c.cream,
            badgeFg = c.ink2,
        )
    }
}
