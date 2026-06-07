package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherHomeScreenV2 — a pixel-faithful copy of `Teacher.tsx → TeacherHome` (dark / night).
 *
 * Greeting header (bell + avatar) · "Today's tasks" colour-coded cards · "Today's periods"
 * horizontal scroller (Period 3 highlighted) · recent-activity list. From [MockV2].
 */
@Composable
fun TeacherHomeScreenV2(
    modifier: Modifier = Modifier,
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onExit: () -> Unit = {},
) {
    val c = VTheme.colors
    val me = MockV2.teachers[1] // Mrs. Priya Iyer

    Column(
        modifier
            .fillMaxSize()
            // §11.1 — push header under the status bar / iOS notch. The bottom
            // is owned by the host's VBottomNav (already applies
            // navigationBarsPadding); we only need top here.
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                VLabel("Good morning")
                Text("Priya", style = VTheme.type.h1.colored(c.ink), modifier = Modifier.padding(top = 4.dp))
                Text("Friday, 5 June 2026", style = VTheme.type.caption.colored(c.ink2))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.ink.copy(alpha = 0.06f)).clickable { onOpenNotifications() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Bell, contentDescription = "Notifications", tint = c.ink, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.clickable { onExit() }) { VAvatar(name = me.name, size = 40.dp) }
            }
        }

        // ── Today's tasks ────────────────────────────────────────────────────
        Column {
            Text("Today's tasks", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // §6: React TaskCard icon-circle uses SOFT pastel fills (not ink):
                //   success=var(--success) #A8E6CF · warning=var(--warning) #FFD4A3 ·
                //   arctic=var(--arctic)=teal · neutral=rgba(245,245,243,0.15). (Teacher.tsx L95)
                TaskCard(c.success, "Class 10-A attendance", "Marked at 9:12 AM • 28 / 32 present", "View details", VIcons.Check)
                TaskCard(c.warning, "Syllabus update pending", "You haven't logged Period 2 — Mathematics", "Update now", VIcons.AlertCircle)
                TaskCard(c.teal, "Class 10-A Unit Test 2", "Marks not entered yet • 23 students", "Enter marks", VIcons.ListChecks)
                TaskCard(Color(0x26F5F5F3), "4 students haven't submitted yesterday's HW", "Mathematics – Algebra worksheet", "View", VIcons.Clock, onCalendar = onOpenCalendar)
            }
        }

        // ── Today's periods ──────────────────────────────────────────────────
        Column {
            Text("Today's periods", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MockV2.timetableToday.forEachIndexed { i, p ->
                    val active = i == 2
                    Column(
                        Modifier
                            .widthIn(min = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) c.teal else c.ink.copy(alpha = 0.06f))
                            .padding(12.dp),
                    ) {
                        // §6: React period header uses the `Label` component = labelStrong.
                        Text("PERIOD ${p.period}", style = VTheme.type.labelStrong.colored(if (active) Color(0xFF080808) else c.ink3))
                        Text(p.subject, style = VTheme.type.bodyStrong.colored(if (active) Color(0xFF080808) else c.ink), modifier = Modifier.padding(top = 4.dp))
                        Text("${p.time} • ${p.klass}", style = VTheme.type.dataSm.colored(if (active) Color(0xFF080808).copy(alpha = 0.8f) else c.ink2).copy(fontSize = 11.sp))
                    }
                }
            }
        }

        // ── Recent activity ──────────────────────────────────────────────────
        Column {
            Text("Recent activity", style = VTheme.type.h3.colored(c.ink), modifier = Modifier.padding(bottom = 8.dp))
            VCard {
                val rows = listOf(
                    "Marked Class 9-A attendance" to "9:12 AM",
                    "Entered Class 10-A Math UT2 marks" to "Yesterday 3:40 PM",
                    "Updated Class 9-A syllabus — Ch 4" to "Yesterday 11:20 AM",
                    "Assigned homework — Class 10-A" to "Wed 4:10 PM",
                )
                rows.forEachIndexed { i, (what, whenAt) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(what, style = VTheme.type.body.colored(c.ink), modifier = Modifier.weight(1f))
                        Text(whenAt, style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(tone: Color, title: String, sub: String, cta: String, icon: ImageVector, onCalendar: () -> Unit = {}) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(tone), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF080808), modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = VTheme.type.bodyStrong.colored(c.ink))
                Text(sub, style = VTheme.type.caption.colored(c.ink2))
            }
            Text(cta, style = VTheme.type.caption.colored(c.teal).copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.clickable { onCalendar() })
        }
    }
}
