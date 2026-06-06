package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VLegendDot
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * ParentAcademicsScreenV2 — a pixel-faithful copy of `Parent.tsx → Academics`.
 *
 * Top tabs: Overview · Attendance · Marks · Syllabus · Report. Each tab reproduces the design's
 * cards from [MockV2] (subject progress bars + class standing, attendance heatmap + absence alert,
 * marks trend chart + history, syllabus log, and the AI Report "coming soon" card).
 */
@Composable
fun ParentAcademicsScreenV2(
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    var tab by remember { mutableStateOf("Overview") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            "Academics",
            style = VTheme.type.h1.colored(c.ink),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        VTopTabs(
            tabs = listOf("Overview", "Attendance", "Marks", "Syllabus", "Report"),
            selected = tab,
            onSelect = { tab = it },
        )
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(d.sm + 4.dp),
        ) {
            when (tab) {
                "Overview" -> OverviewTab()
                "Attendance" -> AttendanceTab()
                "Marks" -> MarksTab()
                "Syllabus" -> SyllabusTab()
                "Report" -> VComingSoon(
                    title = "AI Report Card",
                    description = "At the end of each term, VidyaSetu will generate a personalised academic summary for Riya — narrative strengths, focus areas and study tips.",
                    preview = { AiReportCardPreview() },
                )
            }
        }
    }
}

@Composable
private fun OverviewTab() {
    val c = VTheme.colors
    val subjects = listOf("Mathematics", "Science", "English", "Hindi", "Social Studies", "Computer")
    val pcts = listOf(74, 88, 81, 76, 79, 85)
    subjects.forEachIndexed { i, s ->
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s, style = VTheme.type.bodyStrong.colored(c.ink))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${pcts[i]}%", style = VTheme.type.data.colored(c.ink))
                    val up = i in listOf(1, 2, 4, 5)
                    VBadge(text = if (up) "▲" else "▼", tone = if (up) VBadgeTone.Success else VBadgeTone.Warning)
                }
            }
            Spacer(Modifier.height(8.dp))
            VProgressBar(value = pcts[i].toFloat(), tone = VBadgeTone.Arctic)
        }
    }
    VCard {
        VLabel("Class standing")
        Text("Rank 6 of 32", style = VTheme.type.dataLg.colored(c.ink), modifier = Modifier.padding(top = 8.dp))
        Text("Last assessment cycle", style = VTheme.type.caption.colored(c.ink2))
    }
}

@Composable
private fun AttendanceTab() {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        MonthChip("‹ May")
        Text("June 2026", style = VTheme.type.bodyStrong.colored(c.ink))
        MonthChip("Jul ›")
    }
    AttendanceHeatV2()
    VCard {
        VLabel("3 consecutive absences alert")
        Text(
            "We've noticed Riya was absent on 5, 6 and 12 Jun. Is everything okay?",
            style = VTheme.type.caption.colored(c.ink2),
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VButton(text = "Mark as medical leave", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm, tone = VButtonTone.Navy)
            VButton(text = "Talk to school", onClick = {}, size = VButtonSize.Sm, tone = VButtonTone.Teal, stateful = true, successLabel = "Sent")
        }
    }
}

@Composable
private fun MonthChip(label: String) {
    val c = VTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.cream)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, style = VTheme.type.caption.colored(c.ink2))
    }
}

/** A compact 30-day attendance heat grid from [MockV2.attendanceMonth]. */
@Composable
private fun AttendanceHeatV2(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        VLabel("Daily attendance")
        Spacer(Modifier.height(12.dp))
        val weeks = MockV2.attendanceMonth.chunked(6)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        val fill = when (day.status) {
                            MockV2.DayStatus.Present -> c.teal
                            MockV2.DayStatus.Absent -> c.dangerInk
                            MockV2.DayStatus.Late -> c.warningInk
                            MockV2.DayStatus.Holiday -> c.border2
                            MockV2.DayStatus.Future -> c.cream
                        }
                        val faded = day.status == MockV2.DayStatus.Future || day.status == MockV2.DayStatus.Holiday
                        Box(
                            Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(fill),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.day.toString(),
                                style = VTheme.type.label.colored(if (faded) c.ink3 else Color.White),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VLegendDot(color = c.teal, label = "Present")
            VLegendDot(color = c.dangerInk, label = "Absent")
            VLegendDot(color = c.warningInk, label = "Late")
        }
    }
}

@Composable
private fun MarksTab() {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Mathematics", "Science", "English", "Hindi", "Social Studies").forEachIndexed { i, s ->
            VTag(text = s, active = i == 0)
        }
    }
    VCard {
        VLabel("Performance trend")
        Spacer(Modifier.height(12.dp))
        TrendChart()
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VLegendDot(color = c.teal, label = "Riya")
            VLegendDot(color = c.ink3, label = "Class avg")
        }
    }
    MockV2.marksHistory.filter { it.subject == "Mathematics" }.forEach { m ->
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(m.name, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("${m.date} • ${m.subject}", style = VTheme.type.caption.colored(c.ink2))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${m.marks} / ${m.total}", style = VTheme.type.data.colored(c.ink).copy(fontSize = 17.sp))
                    Text("Class avg ${m.avg}", style = VTheme.type.label.colored(c.ink3))
                }
            }
        }
    }
}

/** A small two-line trend chart (you vs class-avg) drawn from [MockV2.subjectTrend]. */
@Composable
private fun TrendChart(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val pts = MockV2.subjectTrend
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val maxV = 100f
        val minV = 50f
        val span = maxV - minV
        val stepX = if (pts.size > 1) w / (pts.size - 1) else w
        fun y(v: Int): Float = h - ((v - minV) / span) * (h - 16f) - 8f
        val avgPath = Path()
        pts.forEachIndexed { i, p ->
            val x = i * stepX
            if (i == 0) avgPath.moveTo(x, y(p.avg)) else avgPath.lineTo(x, y(p.avg))
        }
        drawPath(avgPath, color = c.ink3, style = Stroke(width = 2f))
        val youPath = Path()
        pts.forEachIndexed { i, p ->
            val x = i * stepX
            if (i == 0) youPath.moveTo(x, y(p.you)) else youPath.lineTo(x, y(p.you))
        }
        drawPath(youPath, color = c.teal, style = Stroke(width = 3f, cap = StrokeCap.Round))
        pts.forEachIndexed { i, p ->
            drawCircle(c.teal, radius = 4f, center = Offset(i * stepX, y(p.you)))
        }
    }
}

@Composable
private fun SyllabusTab() {
    val c = VTheme.colors
    val items = listOf(
        Triple("Ch 6 — Periodic Table", "Trends across periods. Modern arrangement.", "5 Jun"),
        Triple("Ch 5 — Carbon Compounds", "Functional groups. Soaps & detergents.", "29 May"),
        Triple("Ch 4 — Metals & Non-metals", "Reactivity series. Extraction.", "20 May"),
    )
    items.forEach { (t, b, dt) ->
        VCard {
            Text(t, style = VTheme.type.bodyStrong.colored(c.ink))
            Text(b, style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 2.dp))
            Text("Covered $dt", style = VTheme.type.label.colored(c.ink3), modifier = Modifier.padding(top = 6.dp))
        }
    }
    VCard {
        VLabel("Coming up next")
        Text("Ch 7 — Life Processes", style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.padding(top = 4.dp))
    }
}
