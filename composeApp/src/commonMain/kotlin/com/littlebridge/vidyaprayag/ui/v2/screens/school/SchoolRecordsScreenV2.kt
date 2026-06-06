package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * SchoolRecordsScreenV2 — a pixel-faithful copy of `Admin.tsx → Records`.
 *
 * Five sub-tabs — Attendance (mark P/A/L per student + submit), Marks (entry grid), Syllabus
 * (chapter status), Fee (outstanding overview + history), Documents (file rows + upload). All from
 * [MockV2].
 */
@Composable
fun SchoolRecordsScreenV2(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    var tab by remember { mutableStateOf("Attendance") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Records", style = VTheme.type.h1.colored(c.ink))
        VTopTabs(tabs = listOf("Attendance", "Marks", "Syllabus", "Fee", "Documents"), selected = tab, onSelect = { tab = it })
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) {
                "Attendance" -> RecordsAttendance()
                "Marks" -> RecordsMarks()
                "Syllabus" -> RecordsSyllabus()
                "Fee" -> RecordsFee()
                "Documents" -> RecordsDocs()
            }
        }
    }
}

@Composable
private fun RecordsAttendance() {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VInput(value = "5 Jun 2026", onValueChange = {}, leadingIcon = VIcons.Calendar, modifier = Modifier.weight(1f), enabled = false)
        VInput(value = "Class 10-A", onValueChange = {}, leadingIcon = VIcons.GraduationCap, modifier = Modifier.weight(1f), enabled = false)
    }
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                VLabel("Class 10-A")
                Text("28 / 32 present", style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.padding(top = 4.dp))
            }
            VButton(text = "Mark all present", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
        }
        MockV2.students.take(6).forEach { s ->
            Spacer(Modifier.height(10.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1)); Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VAvatar(name = s.name, size = 32.dp)
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("Roll ${s.roll}", style = VTheme.type.dataSm.colored(c.ink3))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PALButton("P", s.today == MockV2.Presence.Present, c.successInk)
                    PALButton("A", s.today == MockV2.Presence.Absent, c.dangerInk)
                    PALButton("L", s.today == MockV2.Presence.Late, c.warningInk)
                }
            }
        }
    }
    VButton(text = "Submit attendance", onClick = {}, full = true, size = VButtonSize.Lg, stateful = true, successLabel = "Submitted", leading = { Icon(VIcons.Check, null, modifier = Modifier.size(16.dp)) })
}

@Composable
private fun PALButton(letter: String, active: Boolean, tone: Color) {
    val c = VTheme.colors
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(if (active) tone else c.ink.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = VTheme.type.label.colored(if (active) Color.White else c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun RecordsMarks() {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VInput(value = "Class 10-A", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
        VInput(value = "Mathematics", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
    }
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                VLabel("Unit Test 2 — Trigonometry")
                Text("Max 100 • 02 Jun", style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 4.dp))
            }
            VBadge(text = "Live class avg 68", tone = VBadgeTone.Arctic)
        }
        MockV2.students.take(5).forEachIndexed { i, s ->
            Spacer(Modifier.height(10.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1)); Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VAvatar(name = s.name, size = 30.dp)
                Text(s.name, style = VTheme.type.body.colored(c.ink), modifier = Modifier.weight(1f))
                MarkBox(listOf(78, 71, 88, 65, 92)[i].toString())
                Box(Modifier.size(36.dp).clip(CircleShape).background(c.ink.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Text("AB", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
    VButton(text = "Save & publish", onClick = {}, full = true, size = VButtonSize.Lg, stateful = true, successLabel = "Published")
}

@Composable
private fun MarkBox(value: String) {
    val c = VTheme.colors
    Box(
        Modifier.size(width = 64.dp, height = 32.dp).clip(RoundedCornerShape(8.dp)).background(c.ink.copy(alpha = 0.06f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(value, style = VTheme.type.data.colored(c.ink), modifier = Modifier.padding(end = 12.dp))
    }
}

@Composable
private fun RecordsSyllabus() {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VInput(value = "Class 10-A", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
        VInput(value = "Chemistry", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
    }
    val chapters = listOf(
        "Ch 1 — Chemical Reactions", "Ch 2 — Acids, Bases & Salts", "Ch 3 — Metals & Non-Metals",
        "Ch 4 — Carbon Compounds", "Ch 5 — Periodic Classification", "Ch 6 — Life Processes",
    )
    chapters.forEachIndexed { i, ch ->
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(ch, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text(
                        when {
                            i < 4 -> "Covered ${10 + i} Apr — ${20 + i} May"
                            i == 4 -> "In progress"
                            else -> "Not started"
                        },
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
                VBadge(
                    text = if (i < 4) "Done" else if (i == 4) "Active" else "Upcoming",
                    tone = if (i < 4) VBadgeTone.Success else if (i == 4) VBadgeTone.Warning else VBadgeTone.Neutral,
                )
            }
        }
    }
}

@Composable
private fun RecordsFee() {
    val c = VTheme.colors
    VTopTabs(tabs = listOf("Structure", "Collections"), selected = "Collections", onSelect = {})
    VCard {
        VLabel("Outstanding overview")
        Text("₹ 2,18,400", style = VTheme.type.dataLg.colored(c.ink).copy(fontSize = 26.sp), modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeeStat("84", "Pending", Modifier.weight(1f))
            FeeStat("12", "Overdue", Modifier.weight(1f), c.dangerInk)
            FeeStat("220", "Paid", Modifier.weight(1f))
        }
    }
    VButton(text = "Send reminders to 84 parents", onClick = {}, full = true, variant = VButtonVariant.Secondary, leading = { Icon(VIcons.Send, null, modifier = Modifier.size(14.dp)) })
    MockV2.feeHistory.forEach { FeeRowCard(it) }
}

@Composable
private fun FeeStat(n: String, label: String, modifier: Modifier = Modifier, tone: Color? = null) {
    val c = VTheme.colors
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n, style = VTheme.type.data.colored(tone ?: c.ink).copy(fontSize = 16.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

@Composable
private fun RecordsDocs() {
    val c = VTheme.colors
    val docs = listOf(
        Triple("Circular — PTM Notice", "PDF • Uploaded 3 Jun", "All parents"),
        Triple("Half-Yearly Timetable", "PDF • Uploaded 1 Jun", "Class 9, 10, 12"),
        Triple("Holiday List 2025-26", "PDF • Uploaded 12 Apr", "All school"),
    )
    docs.forEach { (title, date, recipients) ->
        VCard {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFC8DEFF).copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
                    Icon(VIcons.Bookmark, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text(date, style = VTheme.type.caption.colored(c.ink2))
                    Spacer(Modifier.height(4.dp))
                    VBadge(text = recipients, tone = VBadgeTone.Neutral)
                }
                Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp).clickable {})
            }
        }
    }
    VButton(text = "Upload document", onClick = {}, full = true, variant = VButtonVariant.Secondary, leading = { Icon(VIcons.Plus, null, modifier = Modifier.size(14.dp)) })
}
