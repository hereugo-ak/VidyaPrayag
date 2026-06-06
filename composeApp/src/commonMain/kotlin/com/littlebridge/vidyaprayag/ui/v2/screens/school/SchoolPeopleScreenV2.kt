package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/** Which People sub-view is currently shown. */
private sealed class PeopleView {
    data object List : PeopleView()
    data class Student(val id: String) : PeopleView()
    data class Teacher(val id: String) : PeopleView()
}

/**
 * SchoolPeopleScreenV2 — a pixel-faithful copy of `Admin.tsx → People` plus StudentDetail / TeacherDetail.
 *
 * Students/Teachers tab toggle, search + filter chips, and tappable rows opening a back-headered
 * detail screen (student: Overview/Attendance/Marks/Fees/Notes · teacher: Profile/Activity/Classes).
 */
@Composable
fun SchoolPeopleScreenV2(modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<PeopleView>(PeopleView.List) }

    when (val v = view) {
        is PeopleView.List -> PeopleList(
            modifier = modifier,
            onOpenStudent = { view = PeopleView.Student(it) },
            onOpenTeacher = { view = PeopleView.Teacher(it) },
        )
        is PeopleView.Student -> StudentDetail(id = v.id, onBack = { view = PeopleView.List }, modifier = modifier)
        is PeopleView.Teacher -> TeacherDetail(id = v.id, onBack = { view = PeopleView.List }, modifier = modifier)
    }
}

@Composable
private fun PeopleList(
    modifier: Modifier,
    onOpenStudent: (String) -> Unit,
    onOpenTeacher: (String) -> Unit,
) {
    val c = VTheme.colors
    var tab by remember { mutableStateOf("Students") }
    var query by remember { mutableStateOf("") }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("People", style = VTheme.type.h1.colored(c.ink))
            VTopTabs(tabs = listOf("Students", "Teachers"), selected = tab, onSelect = { tab = it })

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VInput(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search ${tab.lowercase()}",
                    leadingIcon = VIcons.Search,
                    modifier = Modifier.weight(1f),
                )
                // React: 48×48 chip, bg rgba(245,245,243,0.06) (→ ink@6% in warm), border-dark-2, Filter glyph.
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Filter, contentDescription = "Filter", tint = c.ink, modifier = Modifier.size(18.dp))
                }
            }

            val filters = if (tab == "Students") listOf("All classes", "Class 10-A", "Class 9-A", "Status") else listOf("All subjects", "Active 7d", "Inactive")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // React renders an inline <ChevronDown size={12}/> after each chip label.
                filters.forEachIndexed { i, f -> VTag(text = f, active = i == 0, trailingIcon = VIcons.ChevronDown) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tab == "Students") {
                    MockV2.students.forEach { s ->
                        VCard(onClick = { onOpenStudent(s.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                VAvatar(name = s.name, size = 42.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                                    Text("Roll ${s.roll} • ${s.klass}", style = VTheme.type.dataSm.colored(c.ink2))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        VStatusDot(color = pewsColor(s.pews))
                                        Text("${s.attendance}%", style = VTheme.type.dataSm.colored(c.ink))
                                    }
                                    Text("Attendance", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
                                }
                            }
                        }
                    }
                } else {
                    MockV2.teachers.forEach { t ->
                        VCard(onClick = { onOpenTeacher(t.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                VAvatar(name = t.name, size = 42.dp)
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(t.name, style = VTheme.type.bodyStrong.colored(c.ink))
                                        // React VStatusDot tone uses the pastel --success/--warning, not the *Ink variant.
                                        VStatusDot(color = if (t.active) c.success else c.warning)
                                    }
                                    Text("${t.subjects.joinToString(" • ")} · ${t.classes.size} classes", style = VTheme.type.caption.colored(c.ink2))
                                }
                                Text(t.lastActive, style = VTheme.type.caption.colored(c.ink3))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(64.dp)) // breathing room so the FAB never overlaps the last row
        }

        // React: fixed FAB — bottom-right, 56dp, bg --arctic (teal-deep in warm), Plus glyph in --void.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(c.tealDeep)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VIcons.Plus, contentDescription = "Add", tint = c.background, modifier = Modifier.size(22.dp))
        }
    }
}

// ── Student detail ───────────────────────────────────────────────────────────
@Composable
private fun StudentDetail(id: String, onBack: () -> Unit, modifier: Modifier) {
    val c = VTheme.colors
    val s = MockV2.students.find { it.id == id } ?: MockV2.students[0]
    var tab by remember { mutableStateOf("Overview") }

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Student", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    VAvatar(name = s.name, size = 64.dp)
                    Column {
                        Text(s.name, style = VTheme.type.h2.colored(c.ink))
                        Text("Class ${MockV2.classDisplay(s.klass)} • Roll ${s.roll}", style = VTheme.type.caption.colored(c.ink2))
                        Text("Adm SVM-2024-${100 + (s.roll.toIntOrNull() ?: 0)}", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Attendance", "${s.attendance}%", Modifier.weight(1f))
                    StatTile("Last marks", "${s.lastMarks}%", Modifier.weight(1f))
                    StatTile("Dues", if (s.fees > 0) "₹${s.fees}" else "₹0", Modifier.weight(1f), if (s.fees > 0) c.dangerInk else c.successInk)
                }
            }
            VTopTabs(tabs = listOf("Overview", "Attendance", "Marks", "Fees", "Notes"), selected = tab, onSelect = { tab = it })
            Column(Modifier.padding(horizontal = 20.dp).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    "Overview" -> {
                        VCard {
                            VLabel("Parent")
                            Spacer(Modifier.height(8.dp))
                            Text(s.parentName, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(s.parentMobile, style = VTheme.type.dataSm.colored(c.ink2))
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VBadge(text = "Verified", tone = VBadgeTone.Success)
                                VBadge(text = "WhatsApp opt-in", tone = VBadgeTone.Arctic)
                            }
                        }
                        VCard {
                            VLabel("Personal")
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FieldCell("DOB", s.dob, Modifier.weight(1f))
                                FieldCell("Gender", if (s.gender == "M") "Male" else "Female", Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FieldCell("Blood group", "—", Modifier.weight(1f))
                                FieldCell("Admission yr", "2024", Modifier.weight(1f))
                            }
                        }
                    }
                    "Attendance" -> AttendanceHeatV2()
                    "Marks" -> VCard {
                        listOf("Mathematics" to 74, "Science" to 88, "English" to 81, "Hindi" to 76).forEachIndexed { i, (sub, m) ->
                            if (i > 0) { Spacer(Modifier.height(12.dp)); androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1)); Spacer(Modifier.height(12.dp)) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sub, style = VTheme.type.bodyStrong.colored(c.ink))
                                Text("$m%", style = VTheme.type.dataSm.colored(c.ink))
                            }
                            Spacer(Modifier.height(4.dp))
                            VProgressBar(value = m.toFloat())
                        }
                    }
                    "Fees" -> {
                        VCard {
                            VLabel("Outstanding")
                            Text("₹ ${formatCommas(s.fees)}", style = VTheme.type.dataLg.colored(if (s.fees > 0) c.dangerInk else c.successInk).copy(fontSize = 28.sp), modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(12.dp))
                            VButton(text = "Send reminder", onClick = {}, variant = VButtonVariant.Secondary, full = true)
                        }
                        MockV2.feeHistory.forEach { FeeRowCard(it) }
                    }
                    "Notes" -> VCard {
                        VLabel("Internal note — Admin only")
                        Text(
                            if (s.fees > 0) "Parent requested fee deferment until 25 Jun. Approved verbally."
                            else "Excellent academic performance. Consider for science olympiad.",
                            style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.padding(top = 8.dp),
                        )
                        Text("Logged by Principal A. Verma • 4 Jun 2026", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified), modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
}

// ── Teacher detail ───────────────────────────────────────────────────────────
@Composable
private fun TeacherDetail(id: String, onBack: () -> Unit, modifier: Modifier) {
    val c = VTheme.colors
    val t = MockV2.teachers.find { it.id == id } ?: MockV2.teachers[0]
    var tab by remember { mutableStateOf("Profile") }

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Teacher", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VAvatar(name = t.name, size = 64.dp)
                Column {
                    Text(t.name, style = VTheme.type.h2.colored(c.ink))
                    Text(t.username, style = VTheme.type.dataSm.colored(c.ink2))
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        t.subjects.forEach { VBadge(text = it, tone = VBadgeTone.Arctic) }
                    }
                }
            }
            VTopTabs(tabs = listOf("Profile", "Activity", "Classes"), selected = tab, onSelect = { tab = it })
            when (tab) {
                "Profile" -> VCard {
                    FieldCell("Mobile", "+91 98XXX 12121")
                    Spacer(Modifier.height(12.dp)); FieldCell("Employee ID", "EMP-0${t.id.drop(1)}")
                    Spacer(Modifier.height(12.dp)); FieldCell("Joined", "14 Apr 2022")
                    Spacer(Modifier.height(12.dp)); FieldCell("Class teacher of", "10-A")
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(text = "Reset credentials", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
                        VButton(text = "Deactivate", onClick = {}, variant = VButtonVariant.Destructive, size = VButtonSize.Sm)
                    }
                }
                "Activity" -> {
                    VCard {
                        VLabel("Update frequency — last 30 days")
                        Spacer(Modifier.height(12.dp))
                        (0 until 30).chunked(15).forEach { rowIdx ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowIdx.forEach { i ->
                                    val v = (i * 7) % 4
                                    val bg = when (v) {
                                        0 -> c.ink.copy(alpha = 0.06f)
                                        1 -> Color(0xFFC8DEFF).copy(alpha = 0.20f)
                                        2 -> Color(0xFFC8DEFF).copy(alpha = 0.45f)
                                        else -> c.teal
                                    }
                                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).background(bg))
                                }
                            }
                        }
                    }
                    VCard {
                        VLabel("Recent updates")
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            (1..5).forEach { i ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(VIcons.Bookmark, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Column {
                                        Text("Updated Class 10-A Chemistry — Ch. 6 Periodic Table", style = VTheme.type.caption.colored(c.ink))
                                        Text("${i}d ago", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified))
                                    }
                                }
                            }
                        }
                    }
                }
                "Classes" -> {
                    t.classes.chunked(2).forEach { rowCls ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowCls.forEach { cls ->
                                VCard(modifier = Modifier.weight(1f)) {
                                    Text(cls, style = VTheme.type.bodyStrong.colored(c.ink))
                                    Text("${32 + (cls.first().code % 6)} students", style = VTheme.type.caption.colored(c.ink2))
                                }
                            }
                            if (rowCls.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

/**
 * AttendanceHeatV2 — a calendar-style 30-day attendance heatmap, copied from `Admin.tsx →
 * AttendanceHeat`. Used by both the admin student-detail and (privately) the parent academics tab.
 */
@Composable
fun AttendanceHeatV2(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            VLabel("June 2026")
            Text("91%", style = VTheme.type.dataSm.colored(c.ink))
        }
        Spacer(Modifier.height(12.dp))
        // weekday header
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp), modifier = Modifier.weight(1f))
            }
        }
        MockV2.attendanceMonth.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    // React `AttendanceHeat.tone`: pastel --arctic/--danger/--warning fills.
                    val fill = when (day.status) {
                        MockV2.DayStatus.Present -> c.tealDeep                 // var(--arctic)
                        MockV2.DayStatus.Absent -> c.danger                    // var(--danger) (pastel #FFADA8)
                        MockV2.DayStatus.Late -> c.warning                     // var(--warning) (pastel #FFD4A3)
                        MockV2.DayStatus.Holiday -> c.ink.copy(alpha = 0.04f)  // rgba(8,8,8,0.04)
                        MockV2.DayStatus.Future -> Color.Transparent
                    }
                    // React fg: present #0a3a76, absent #7a1c18, late #7a3f00, else text-light-3.
                    val fg = when (day.status) {
                        MockV2.DayStatus.Present -> Color(0xFF0A3A76) // §AttendanceHeat present ink
                        MockV2.DayStatus.Absent -> Color(0xFF7A1C18)  // §AttendanceHeat absent ink
                        MockV2.DayStatus.Late -> Color(0xFF7A3F00)    // §AttendanceHeat late ink
                        else -> c.ink3
                    }
                    var cell = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(fill)
                    if (day.status == MockV2.DayStatus.Future) {
                        cell = cell.border(1.dp, c.border1, CircleShape) // future days get a hairline ring
                    }
                    Box(cell, contentAlignment = Alignment.Center) {
                        Text(day.day.toString(), style = VTheme.type.dataSm.colored(fg).copy(fontSize = 11.sp))
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HeatLegend(21, "Present"); HeatLegend(2, "Absent"); HeatLegend(1, "Late"); HeatLegend(4, "Holiday")
        }
    }
}

@Composable
private fun HeatLegend(n: Int, label: String) {
    val c = VTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n.toString(), style = VTheme.type.data.colored(c.ink).copy(fontSize = 16.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

// React VStatusDot maps tone→ --success/--warning/--danger (the *pastel* semantic colors),
// not the darker *Ink ramps. See primitives.tsx `VStatusDot`.
@Composable
private fun pewsColor(p: MockV2.Pews): Color = when (p) {
    MockV2.Pews.Ok -> VTheme.colors.success
    MockV2.Pews.Warn -> VTheme.colors.warning
    MockV2.Pews.Risk -> VTheme.colors.danger
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, tone: Color? = null) {
    val c = VTheme.colors
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp)) {
        Text(label.uppercase(), style = VTheme.type.label.colored(c.ink3).copy(fontSize = 10.sp))
        Text(value, style = VTheme.type.data.colored(tone ?: c.ink).copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FieldCell(label: String, value: String, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(modifier) {
        Text(label.uppercase(), style = VTheme.type.label.colored(c.ink3).copy(fontSize = 10.sp))
        Text(value, style = VTheme.type.body.colored(c.ink), modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun FeeRowCard(f: MockV2.FeePayment) {
    val c = VTheme.colors
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(f.head, style = VTheme.type.bodyStrong.colored(c.ink))
                Text("${f.date} • ${f.receipt}", style = VTheme.type.dataSm.colored(c.ink2))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹ ${formatCommas(f.amount)}", style = VTheme.type.data.colored(c.ink))
                Text("Receipt", style = VTheme.type.caption.colored(c.teal))
            }
        }
    }
}

/** Indian-style thousands grouping for currency display. */
internal fun formatCommas(n: Int): String {
    val s = n.toString()
    if (s.length <= 3) return s
    val head = s.dropLast(3)
    val tail = s.takeLast(3)
    val grouped = StringBuilder()
    var count = 0
    for (i in head.indices.reversed()) {
        grouped.append(head[i])
        count++
        if (count == 2 && i != 0) { grouped.append(','); count = 0 }
    }
    return grouped.reverse().toString() + "," + tail
}
