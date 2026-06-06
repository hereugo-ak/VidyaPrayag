package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VBars
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VChartDatum
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VLegendDot
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressRing
import com.littlebridge.vidyaprayag.ui.v2.components.VSparkline
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * SchoolHomeScreenV2 — a pixel-faithful copy of `Admin.tsx → AdminHome` (dark / night).
 *
 * School chip header (avatar + day-of-year + bell + admin avatar) · "Good afternoon" greeting ·
 * horizontally-scrolling glance cards · attendance-by-class grid · syllabus coverage · subject
 * performance (bars + sparkline) · teacher activity feed · PEWS early-warning preview · pending
 * actions. All values from [MockV2].
 */
@Composable
fun SchoolHomeScreenV2(
    modifier: Modifier = Modifier,
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onExit: () -> Unit = {},
) {
    val c = VTheme.colors

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Header row ───────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.teal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.GraduationCap, contentDescription = null, tint = Color(0xFF080808), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(MockV2.school.shortName, style = VTheme.type.h4.colored(c.ink))
                    Text("Day ${MockV2.school.dayOfYear} of ${MockV2.school.totalDays}", style = VTheme.type.caption.colored(c.ink2))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.ink.copy(alpha = 0.06f)).clickable { onOpenNotifications() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Bell, contentDescription = "Notifications", tint = c.ink, modifier = Modifier.size(18.dp))
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(6.dp).clip(CircleShape).background(c.dangerInk))
                }
                Box(Modifier.clickable { onExit() }) { VAvatar(name = "Admin Office", size = 36.dp) }
            }
        }

        // ── Greeting ──────────────────────────────────────────────────────────
        Column {
            Text("Good afternoon", style = VTheme.type.h1.colored(c.ink))
            Text("Here's how ${MockV2.school.name} is doing today.", style = VTheme.type.body.colored(c.ink2))
        }

        // ── Glance cards (horizontal scroll) ───────────────────────────────────
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlanceCard(title = "Attendance today", big = "87%", sub = "274 present / 316 total", ring = 87f, cta = "View details")
            GlanceCard(title = "Pending from teachers", big = "3", sub = "haven't marked yet", chips = listOf("Mr. Pillai", "Ms. Bose", "+1"), cta = "Send reminder")
            GlanceCard(title = "Fee collection", big = "₹ 24,500", sub = "₹ 2,18,400 outstanding", bar = 42f, cta = "Fee dashboard")
            GlanceCard(title = "Upcoming", big = "PTM", sub = "Class 10 — Tomorrow 10 AM", cta = "View calendar", onCta = onOpenCalendar)
        }

        // ── Attendance by class ─────────────────────────────────────────────────
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Attendance by class", style = VTheme.type.h3.colored(c.ink))
                Text("Today", style = VTheme.type.caption.colored(c.ink2))
            }
            Spacer(Modifier.height(12.dp))
            MockV2.classAttendanceGrid.chunked(3).forEach { rowCells ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowCells.forEach { (cls, pct) ->
                        val (bg, fg) = when {
                            pct >= 80 -> Color(0xFFC8DEFF).copy(alpha = 0.30f) to Color(0xFF7FB0FF)
                            pct >= 60 -> c.warning.copy(alpha = 0.45f) to c.warningInk
                            else -> c.danger.copy(alpha = 0.45f) to c.dangerInk
                        }
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg).padding(12.dp),
                        ) {
                            Text("${cls.name.removePrefix("Class ")} ${cls.section}", style = VTheme.type.label.colored(fg).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified, fontWeight = FontWeight.SemiBold))
                            Text("$pct%", style = VTheme.type.dataLg.colored(fg).copy(fontSize = 18.sp), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    repeat(3 - rowCells.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ── Syllabus coverage ───────────────────────────────────────────────────
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Syllabus coverage", style = VTheme.type.h3.colored(c.ink))
                Text("Full report", style = VTheme.type.caption.colored(c.teal))
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MockV2.syllabusCoverage.forEach { s ->
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.klass, style = VTheme.type.body.colored(c.ink))
                            Text("${s.pct}%", style = VTheme.type.dataSm.colored(c.ink2))
                        }
                        Spacer(Modifier.height(4.dp))
                        VProgressBar(value = s.pct.toFloat(), tone = if (s.pct < 70) VBadgeTone.Warning else VBadgeTone.Arctic)
                    }
                }
            }
        }

        // ── Subject performance ──────────────────────────────────────────────────
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Subject performance", style = VTheme.type.h3.colored(c.ink))
                    Text("Class 10 averages · last 7 days", style = VTheme.type.caption.colored(c.ink2))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("78%", style = VTheme.type.dataLg.colored(c.ink).copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
                    VSparkline(values = listOf(72f, 70f, 74f, 71f, 75f, 76f, 78f), width = 84.dp, height = 28.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            VBars(
                data = listOf(
                    VChartDatum("Math", 74f), VChartDatum("Sci", 81f), VChartDatum("Eng", 86f),
                    VChartDatum("Hin", 72f), VChartDatum("Soc", 69f), VChartDatum("Today", 78f),
                ),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VLegendDot(color = c.teal.copy(alpha = 0.45f), label = "Week")
                VLegendDot(color = c.tealDeep, label = "Today", value = "78%")
            }
        }

        // ── Teacher activity ─────────────────────────────────────────────────────
        VCard {
            Text("Teacher activity", style = VTheme.type.h3.colored(c.ink))
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MockV2.recentTeacherActivity.forEach { a ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VAvatar(name = a.who, size = 32.dp)
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(a.who, style = VTheme.type.bodyStrong.colored(c.ink))
                                Text(" ${a.what}", style = VTheme.type.body.colored(c.ink2))
                            }
                            Text(a.whenAt, style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified))
                        }
                    }
                }
            }
        }

        // ── PEWS — early-warning radar ───────────────────────────────────────────
        Column {
            Text("Early-warning radar", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
            VComingSoon(
                title = "PEWS — Predictive Early Warning",
                description = "Combines attendance, marks, fee status and behavioural signals to surface at-risk students before exam season.",
            )
        }

        // ── Pending actions ──────────────────────────────────────────────────────
        Column {
            Text("Pending actions", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MockV2.pendingActions.forEach { p ->
                    VCard {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(VIcons.Bell, contentDescription = null, tint = c.warningInk, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, style = VTheme.type.bodyStrong.colored(c.ink))
                                Text(p.sub, style = VTheme.type.caption.colored(c.ink2))
                            }
                            VButton(text = p.cta, onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlanceCard(
    title: String,
    big: String,
    sub: String,
    cta: String,
    ring: Float? = null,
    bar: Float? = null,
    chips: List<String>? = null,
    onCta: () -> Unit = {},
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.widthIn(min = 230.dp)) {
        VLabel(title)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(big, style = VTheme.type.dataLg.colored(c.ink).copy(fontSize = 28.sp))
                Text(sub, style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 4.dp))
            }
            if (ring != null) VProgressRing(value = ring, size = 56.dp, strokeWidth = 6.dp)
        }
        if (bar != null) {
            Spacer(Modifier.height(12.dp))
            VProgressBar(value = bar)
        }
        if (chips != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEach { ch ->
                    Text(
                        ch,
                        style = VTheme.type.caption.colored(c.ink2),
                        modifier = Modifier.clip(CircleShape).background(c.ink.copy(alpha = 0.06f)).padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onCta() },
        ) {
            Text(cta, style = VTheme.type.caption.colored(c.teal).copy(fontWeight = FontWeight.SemiBold))
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.teal, modifier = Modifier.size(14.dp))
        }
    }
}
