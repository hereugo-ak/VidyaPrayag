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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VSparkline
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * ParentHomeScreenV2 — a pixel-faithful copy of `Parent.tsx → ParentHome`.
 *
 * Renders the gradient child-hero card (avatar + name + today-status badge + attendance % +
 * sparkline), today's schedule, "what was covered today", an exam reminder, the fees-due card and
 * this-month's attendance bar. All values come from [MockV2] so the screen matches the design.
 */
@Composable
fun ParentHomeScreenV2(
    child: MockV2.Student = MockV2.childForParent,
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
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        // ── Child hero ────────────────────────────────────────────────────────
        VCard(padding = 0.dp) {
            // gradient top
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
                    VAvatar(name = child.name, size = 68.dp, ring = true)
                    Column(Modifier.weight(1f)) {
                        Text(child.name, style = VTheme.type.h2.colored(c.ink))
                        Text(
                            "Class ${MockV2.classDisplay(child.klass)} • ${MockV2.school.name}",
                            style = VTheme.type.caption.colored(c.ink2),
                        )
                        Spacer(Modifier.height(8.dp))
                        val (tone, label) = when (child.today) {
                            MockV2.Presence.Present -> VBadgeTone.Success to "● Present today"
                            MockV2.Presence.Absent -> VBadgeTone.Danger to "● Absent today"
                            MockV2.Presence.Late -> VBadgeTone.Warning to "● Late today"
                        }
                        VBadge(text = label, tone = tone)
                    }
                }
            }
            // attendance strip
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    VLabel("Attendance · last 30 days")
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("92%", style = VTheme.type.dataLg.colored(c.navy).copy(fontWeight = FontWeight.Bold, fontSize = 24.sp))
                        Text(
                            "+4 vs class avg",
                            style = VTheme.type.label.colored(c.successInk).copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                VSparkline(
                    values = listOf(78f, 82f, 80f, 85f, 88f, 90f, 87f, 92f, 94f, 92f),
                    width = 120.dp,
                    height = 44.dp,
                )
            }
        }

        // ── Today's schedule ────────────────────────────────────────────────
        VCard {
            VLabel("Today's schedule")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MockV2.timetableToday.take(5).forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            p.period.toString(),
                            style = VTheme.type.dataSm.colored(c.ink3),
                            modifier = Modifier.width(20.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(p.subject, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(p.teacher, style = VTheme.type.caption.colored(c.ink3))
                        }
                        Text(p.time.substringBefore(" – "), style = VTheme.type.dataSm.colored(c.ink2))
                        if (i == 2) VBadge(text = "Now", tone = VBadgeTone.Arctic)
                    }
                }
            }
        }

        // ── What was covered today ─────────────────────────────────────────
        VCard {
            VLabel("What was covered today")
            Spacer(Modifier.height(8.dp))
            Text("Science — Ch 6 Periodic Table", style = VTheme.type.bodyStrong.colored(c.ink))
            Text(
                "Trends across periods & groups. Mendeleev vs. modern arrangement. Homework: Ex 6.2 Q 1–6.",
                style = VTheme.type.caption.colored(c.ink2),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text("By Dr. Ramesh Sharma • 2h ago", style = VTheme.type.label.colored(c.ink3), modifier = Modifier.padding(top = 8.dp))
        }

        // ── Reminder ───────────────────────────────────────────────────────
        VCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.warning.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Calendar, contentDescription = null, tint = c.warningInk, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    VLabel("Remind ${child.name.substringBefore(" ")} about this")
                    Text("Mathematics Unit Test — Day after tomorrow", style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.padding(top = 2.dp))
                    Text("Trigonometric Identities • Ch 8", style = VTheme.type.caption.colored(c.ink2))
                }
            }
        }

        // ── Fees due ───────────────────────────────────────────────────────
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    VLabel("Fees")
                    Text("₹ 12,500 due", style = VTheme.type.data.colored(c.warmOrange).copy(fontSize = 20.sp), modifier = Modifier.padding(top = 4.dp))
                    Text("by 11 Jun 2026", style = VTheme.type.caption.colored(c.ink2))
                }
                VButton(
                    text = "Pay now",
                    onClick = {},
                    tone = VButtonTone.Peach,
                    size = VButtonSize.Sm,
                    stateful = true,
                    successLabel = "Soon",
                )
            }
        }

        // ── This month's attendance ────────────────────────────────────────
        VCard {
            VLabel("This month's attendance")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("91%", style = VTheme.type.dataLg.colored(c.ink).copy(fontSize = 38.sp))
                Text("21 of 23 days", style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
            ) {
                Box(Modifier.weight(21f).fillMaxSize().background(c.teal))
                Box(Modifier.weight(2f).fillMaxSize().background(c.dangerInk))
            }
        }
    }
}
