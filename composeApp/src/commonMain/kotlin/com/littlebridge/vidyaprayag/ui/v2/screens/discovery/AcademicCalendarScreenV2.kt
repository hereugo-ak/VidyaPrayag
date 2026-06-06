package com.littlebridge.vidyaprayag.ui.v2.screens.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * AcademicCalendarScreenV2 — a pixel-faithful copy of `Discovery.tsx → AcademicCalendar`.
 *
 * Month switcher, a 7-column day grid where event days are tinted by type, and an "Upcoming events"
 * list. Driven by [MockV2.calendarEvents].
 */
@Composable
fun AcademicCalendarScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        VBackHeader(title = "Academic calendar", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                MonthPill("‹ May")
                Text("June 2026", style = VTheme.type.bodyStrong.colored(c.ink))
                MonthPill("Jul ›")
            }

            // grid
            VCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(d, style = VTheme.type.label.colored(c.ink3))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                val weeks = (1..30).toList().chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weeks.forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { day ->
                                val ev = MockV2.calendarEvents.find { it.day == day }
                                val tone = when (ev?.type) {
                                    MockV2.EventType.Academic -> c.teal.copy(alpha = 0.16f)
                                    MockV2.EventType.Holiday -> c.warning.copy(alpha = 0.55f)
                                    MockV2.EventType.Deadline -> c.danger.copy(alpha = 0.55f)
                                    MockV2.EventType.Event -> c.cream
                                    null -> Color.Transparent
                                }
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(tone),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(day.toString(), style = VTheme.type.caption.colored(c.ink))
                                }
                            }
                            // pad the partial last week
                            repeat(7 - week.size) { Box(Modifier.weight(1f)) {} }
                        }
                    }
                }
            }

            VLabel("Upcoming events")
            MockV2.calendarEvents.forEach { e ->
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.size(width = 48.dp, height = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(e.day.toString(), style = VTheme.type.dataLg.colored(c.ink).copy(fontSize = 20.sp))
                            Text("JUN", style = VTheme.type.label.colored(c.ink3))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(e.label, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(e.type.name, style = VTheme.type.label.colored(c.ink3))
                        }
                        Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthPill(label: String) {
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
