package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * SchoolOnboardingScreenV2 — pixel-faithful Compose copy of `Auth.tsx → SchoolOnboarding`.
 *
 * The full dark (Night) 6-step setup wizard the Figma prototype renders, end-to-end interactive
 * from local state exactly like the React mock:
 *   1. **School identity**  — legal/short name, affiliation, board & type tags, principal.
 *   2. **Academic year**    — year tags, start/end dates, working days, period count.
 *   3. **Classes & sections** — per-class section chips + add-class controls.
 *   4. **Subjects**         — subject cards with per-class chips + "Apply to all".
 *   5. **Teachers**         — live coverage tracker + per-teacher subject×class matrix.
 *   6. **Students**         — CSV import card + validated preview.
 * A final "You're all set" completion screen with the stat bar and quick-start tiles closes it,
 * then [onComplete] opens the dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolOnboardingScreenV2(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    VTheme(tone = VPortalTone.Warm) {
        val c = VTheme.colors
        val d = VTheme.dimens
        val titles = listOf("School identity", "Academic year", "Classes & sections", "Subjects", "Teachers", "Students")

        var step by remember { mutableIntStateOf(1) }

        // ── Classes built ──────────────────────────────────────────────────────────
        val classesBuilt = remember {
            mutableStateListOf(
                OBClass("Class 9", mutableStateListOf("A", "B")),
                OBClass("Class 10", mutableStateListOf("A", "B")),
            )
        }
        val classCodes: List<String> = classesBuilt.flatMap { cl -> cl.sections.map { "${cl.name.removePrefix("Class ")}-$it" } }

        // ── Subjects ───────────────────────────────────────────────────────────────
        val subjects = remember {
            mutableStateListOf(
                OBSubject("s1", "Mathematics", "MAT001", "Core", mutableStateListOf()),
                OBSubject("s2", "Science", "SCI001", "Core", mutableStateListOf()),
                OBSubject("s3", "English", "ENG001", "Core", mutableStateListOf()),
                OBSubject("s4", "Hindi", "HIN001", "Language", mutableStateListOf()),
                OBSubject("s5", "Social Studies", "SOC001", "Core", mutableStateListOf()),
                OBSubject("s6", "Computer Apps", "COMP01", "Core", mutableStateListOf()),
            )
        }

        // ── Teachers ───────────────────────────────────────────────────────────────
        val teachers = remember {
            mutableStateListOf(
                OBTeacher("t1", "Dr. Ramesh Sharma", "+91 98100 12121", "SVM.T01", mutableStateListOf()),
                OBTeacher("t2", "Mrs. Priya Iyer", "+91 98100 13131", "SVM.T02", mutableStateListOf()),
                OBTeacher("t3", "Mr. Arjun Mehta", "+91 98100 14141", "SVM.T03", mutableStateListOf()),
            )
        }

        // ── Completion screen ────────────────────────────────────────────────────
        if (step > 6) {
            CompletionScreen(onComplete = onComplete)
            return@VTheme
        }

        Column(modifier.fillMaxSize().background(c.background)) {
            // ── Header + step indicator ─────────────────────────────────────────────
            Column(Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.md)) {
                Spacer(Modifier.height(d.sm))
                Text("ONBOARDING", style = VTheme.type.labelStrong.colored(c.ink3))
                Spacer(Modifier.height(d.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.xs)) {
                    repeat(6) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (i < step) c.teal else c.ink.copy(alpha = 0.08f)),
                        )
                    }
                }
                Spacer(Modifier.height(d.md))
                Text(titles[step - 1], style = VTheme.type.h2.colored(c.ink))
                Text("Step $step of 6", style = VTheme.type.caption.colored(c.ink3))
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = d.lg),
                verticalArrangement = Arrangement.spacedBy(d.md),
            ) {
                when (step) {
                    1 -> IdentityStep()
                    2 -> AcademicYearStep()
                    3 -> ClassesStep(classesBuilt)
                    4 -> SubjectsStep(subjects, classCodes)
                    5 -> TeachersStep(teachers, subjects, classCodes)
                    else -> StudentsStep()
                }
                Spacer(Modifier.height(d.sm))
            }

            // ── Footer nav (top hairline border, like React borderTop --border-dark-1) ──
            VDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.md),
                horizontalArrangement = Arrangement.spacedBy(d.sm),
            ) {
                if (step > 1) {
                    VButton(
                        text = "Back",
                        onClick = { step-- },
                        variant = VButtonVariant.Ghost,
                        tone = VButtonTone.Navy,
                    )
                }
                VButton(
                    text = if (step < 6) "Continue" else "Finish setup",
                    onClick = { step++ },
                    full = true,
                    size = VButtonSize.Lg,
                    tone = if (step == 6) VButtonTone.Teal else VButtonTone.Navy,
                    stateful = step == 6,
                    successLabel = "Setting up",
                    trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}

// ═══ Step 1 — School identity ═══════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityStep() {
    val c = VTheme.colors
    val d = VTheme.dimens
    var legalName by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var affiliation by remember { mutableStateOf("") }
    var board by remember { mutableStateOf("CBSE") }
    var type by remember { mutableStateOf("Private Unaided") }
    var principal by remember { mutableStateOf("") }
    var principalMobile by remember { mutableStateOf("") }

    VInput(legalName, { legalName = it }, label = "Full legal name", placeholder = "Saraswati Vidya Mandir", modifier = Modifier.fillMaxWidth())
    VInput(shortName, { shortName = it }, label = "Short name", placeholder = "SVM", modifier = Modifier.fillMaxWidth())
    VInput(affiliation, { affiliation = it }, label = "Affiliation number", placeholder = "UP/CBSE/2021/4421", modifier = Modifier.fillMaxWidth())

    Text("BOARD", style = VTheme.type.labelStrong.colored(c.ink3))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalArrangement = Arrangement.spacedBy(d.sm)) {
        listOf("CBSE", "ICSE", "UP State", "Other").forEach { b ->
            VTag(text = b, active = board == b, onClick = { board = b })
        }
    }
    Text("SCHOOL TYPE", style = VTheme.type.labelStrong.colored(c.ink3))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalArrangement = Arrangement.spacedBy(d.sm)) {
        listOf("Government", "Private Aided", "Private Unaided", "Central").forEach { t ->
            VTag(text = t, active = type == t, onClick = { type = t })
        }
    }
    VInput(principal, { principal = it }, label = "Principal's name", placeholder = "Dr. Anita Verma", modifier = Modifier.fillMaxWidth())
    VInput(principalMobile, { principalMobile = it }, label = "Principal's mobile", placeholder = "+91 98XXX XXXXX", modifier = Modifier.fillMaxWidth())
}

// ═══ Step 2 — Academic year ═════════════════════════════════════════════════════
@Composable
private fun AcademicYearStep() {
    val c = VTheme.colors
    val d = VTheme.dimens
    var year by remember { mutableStateOf("2025-26") }
    var workingDays by remember { mutableStateOf("Mon–Sat") }
    var starts by remember { mutableStateOf("") }
    var ends by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var periods by remember { mutableStateOf("") }

    Text("CURRENT ACADEMIC YEAR", style = VTheme.type.labelStrong.colored(c.ink3))
    Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
        listOf("2025-26", "2026-27").forEach { y -> VTag(text = y, active = year == y, onClick = { year = y }) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
        VInput(starts, { starts = it }, label = "Year starts", placeholder = "01 Apr 2025", modifier = Modifier.weight(1f))
        VInput(ends, { ends = it }, label = "Year ends", placeholder = "31 Mar 2026", modifier = Modifier.weight(1f))
    }
    Text("WORKING DAYS", style = VTheme.type.labelStrong.colored(c.ink3))
    Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
        VTag(text = "Mon–Fri", active = workingDays == "Mon–Fri", onClick = { workingDays = "Mon–Fri" })
        VTag(text = "Mon–Sat", active = workingDays == "Mon–Sat", onClick = { workingDays = "Mon–Sat" })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
        VInput(startTime, { startTime = it }, label = "Start time", placeholder = "08:00 AM", modifier = Modifier.weight(1f))
        VInput(endTime, { endTime = it }, label = "End time", placeholder = "02:00 PM", modifier = Modifier.weight(1f))
    }
    VInput(periods, { periods = it }, label = "Periods per day", placeholder = "8", modifier = Modifier.fillMaxWidth())
}

// ═══ Step 3 — Classes & sections ════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassesStep(classesBuilt: MutableList<OBClass>) {
    val c = VTheme.colors
    val d = VTheme.dimens
    var newClassName by remember { mutableStateOf("") }

    VCard(modifier = Modifier.fillMaxWidth()) {
        Text("TIP", style = VTheme.type.labelStrong.colored(c.ink3))
        Text(
            "Pick the sections your school actually runs. Subjects and teachers in the next steps will only show these classes.",
            style = VTheme.type.caption.colored(c.ink2),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    classesBuilt.forEachIndexed { idx, cl ->
        VCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(cl.name, style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.weight(1f))
                Text("${cl.sections.size} sections", style = VTheme.type.dataSm.colored(c.ink3))
            }
            Spacer(Modifier.height(d.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.xs), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                listOf("A", "B", "C", "D", "E", "F").forEach { s ->
                    val on = cl.sections.contains(s)
                    VTag(text = s, active = on, onClick = {
                        if (on) cl.sections.remove(s) else { cl.sections.add(s); cl.sections.sort() }
                    })
                }
            }
        }
    }
    VCard(modifier = Modifier.fillMaxWidth()) {
        Text("ADD CLASS MANUALLY", style = VTheme.type.labelStrong.colored(c.ink3))
        Spacer(Modifier.height(d.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalAlignment = Alignment.CenterVertically) {
            VInput(newClassName, { newClassName = it }, placeholder = "e.g. Class 11, Nursery, KG", modifier = Modifier.weight(1f))
            VButton(
                text = "Add",
                onClick = {
                    if (newClassName.isNotBlank()) {
                        classesBuilt.add(OBClass(newClassName.trim(), mutableStateListOf("A")))
                        newClassName = ""
                    }
                },
                tone = VButtonTone.Teal,
                size = VButtonSize.Sm,
                enabled = newClassName.isNotBlank(),
            )
        }
        Spacer(Modifier.height(d.sm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.xs), verticalArrangement = Arrangement.spacedBy(d.xs)) {
            listOf("Class 11", "Class 12", "Nursery", "LKG", "UKG").forEach { q ->
                VTag(text = "+ $q", onClick = { classesBuilt.add(OBClass(q, mutableStateListOf("A"))) })
            }
        }
    }
}

// ═══ Step 4 — Subjects ══════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectsStep(subjects: MutableList<OBSubject>, classCodes: List<String>) {
    val c = VTheme.colors
    val d = VTheme.dimens

    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("SUBJECTS OFFERED", style = VTheme.type.labelStrong.colored(c.ink3))
                Text("Tap a subject's class chips to set where it's taught.", style = VTheme.type.caption.colored(c.ink3), modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                "Apply to all",
                style = VTheme.type.caption.colored(c.tealDeep),
                modifier = Modifier.padding(start = d.sm),
            )
        }
    }
    subjects.forEach { s ->
        VCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("${s.code} · ${s.type}", style = VTheme.type.dataSm.colored(c.ink3))
                }
                VBadge(
                    text = if (s.classes.isEmpty()) "No classes" else "${s.classes.size} / ${classCodes.size}",
                    tone = if (s.classes.isEmpty()) VBadgeTone.Warning else VBadgeTone.Success,
                )
            }
            Spacer(Modifier.height(d.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.xs), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                classCodes.forEach { cc ->
                    val on = s.classes.contains(cc)
                    VTag(text = cc, active = on, onClick = { if (on) s.classes.remove(cc) else s.classes.add(cc) })
                }
            }
        }
    }
}

// ═══ Step 5 — Teachers (coverage + matrix) ══════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeachersStep(teachers: MutableList<OBTeacher>, subjects: List<OBSubject>, classCodes: List<String>) {
    val c = VTheme.colors
    val d = VTheme.dimens

    val allSlots = subjects.flatMap { s -> s.classes.map { s.name to it } }
    val assignedSlots = teachers.flatMap { it.assignments }
    val coveredCount = allSlots.count { slot -> assignedSlots.any { it.first == slot.first && it.second == slot.second } }
    val coverage = if (allSlots.isNotEmpty()) (coveredCount * 100 / allSlots.size) else 0

    // Coverage tracker
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("TEACHER COVERAGE", style = VTheme.type.labelStrong.colored(c.ink3))
                Text("$coveredCount of ${allSlots.size} subject × class slots assigned", style = VTheme.type.caption.colored(c.ink3), modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                "$coverage%",
                style = VTheme.type.dataLg.colored(if (coverage == 100) c.successInk else if (coverage > 50) c.tealDeep else c.warningInk),
            )
        }
        Spacer(Modifier.height(d.sm))
        VProgressBar(value = coverage.toFloat(), modifier = Modifier.fillMaxWidth())
        if (coverage < 100 && allSlots.isNotEmpty()) {
            Spacer(Modifier.height(d.xs))
            Text("${allSlots.size - coveredCount} unassigned — keep adding assignments below.", style = VTheme.type.caption.colored(c.warningInk))
        }
    }

    teachers.forEach { t ->
        VCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                VAvatar(name = t.name, size = 40.dp)
                Spacer(Modifier.width(d.md))
                Column(Modifier.weight(1f)) {
                    Text(t.name, style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("${t.username} · ${t.mobile}", style = VTheme.type.dataSm.colored(c.ink3))
                }
                VBadge(text = "${t.assignments.size} slots", tone = if (t.assignments.isNotEmpty()) VBadgeTone.Arctic else VBadgeTone.Neutral)
            }
            Spacer(Modifier.height(d.sm))
            // ── Assignment matrix — bordered header grid (110px + repeat(N,1fr)) ──
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, c.shadowTint.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            ) {
                // header row — cream bg, uppercase labels
                Row(Modifier.fillMaxWidth().background(c.cream), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SUBJECT",
                        style = VTheme.type.labelStrong.colored(c.ink3).copy(fontSize = 10.sp, letterSpacing = 0.05.em),
                        modifier = Modifier.width(110.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    classCodes.forEach { cc ->
                        Text(
                            cc,
                            style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
                // subject rows
                subjects.filter { it.classes.isNotEmpty() }.forEachIndexed { rowI, s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(if (rowI > 0) Modifier.border(0.dp, Color.Transparent) else Modifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.name, style = VTheme.type.caption.colored(c.ink).copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.width(110.dp).padding(horizontal = 10.dp, vertical = 8.dp))
                        classCodes.forEach { cc ->
                            val inSubject = s.classes.contains(cc)
                            val mine = t.assignments.any { it.first == s.name && it.second == cc }
                            val takenByOther = !mine && teachers.any { other -> other.id != t.id && other.assignments.any { it.first == s.name && it.second == cc } }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                MatrixCell(
                                    label = if (!inSubject) "" else if (mine) "✓" else if (takenByOther) "—" else "+",
                                    inSubject = inSubject,
                                    mine = mine,
                                    takenByOther = takenByOther,
                                    enabled = inSubject && !takenByOther,
                                    onClick = {
                                        if (mine) t.assignments.removeAll { it.first == s.name && it.second == cc }
                                        else if (inSubject && !takenByOther) t.assignments.add(s.name to cc)
                                    },
                                )
                            }
                        }
                    }
                    if (rowI < subjects.count { it.classes.isNotEmpty() } - 1) {
                        VDivider(color = c.shadowTint.copy(alpha = 0.05f))
                    }
                }
            }
            // assignment chip summary row (font-mono, teal pill SUBJ·class)
            if (t.assignments.isNotEmpty()) {
                Spacer(Modifier.height(d.sm))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.xs), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                    t.assignments.forEach { a ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(c.teal.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "${a.first.take(4)}·${a.second}",
                                style = VTheme.type.dataSm.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            )
                        }
                    }
                }
            }
        }
    }

    VButton(
        text = "Import roster from CSV",
        onClick = {},
        full = true,
        tone = VButtonTone.Sand,
        leading = { Icon(VIcons.Upload, contentDescription = null, modifier = Modifier.size(14.dp)) },
    )
}

@Composable
private fun MatrixCell(label: String, inSubject: Boolean, mine: Boolean, takenByOther: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val available = inSubject && !mine && !takenByOther
    val bg = when {
        !inSubject -> Color.Transparent
        mine -> c.tealDeep
        takenByOther -> c.cream
        else -> c.teal.copy(alpha = 0.10f)
    }
    val fg = when {
        mine -> Color.White
        takenByOther -> c.ink3
        else -> c.tealDeep
    }
    var cell = Modifier
        .padding(4.dp)
        .height(32.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(bg)
    if (available) {
        // 1px dashed available border rgba(0,106,96,0.35)
        cell = cell.dashedBorder(c.tealDeep.copy(alpha = 0.35f), strokeWidth = 1.dp, cornerRadius = 6.dp)
    }
    if (enabled) cell = cell.clickableNoRipple(onClick)
    Box(
        cell.then(if (!inSubject) Modifier.alpha(0.25f) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) {
            Text(label, style = VTheme.type.caption.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
        }
    }
}

// ═══ Step 6 — Students ══════════════════════════════════════════════════════════
@Composable
private fun StudentsStep() {
    val c = VTheme.colors
    val d = VTheme.dimens

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = d.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(VIcons.Upload, contentDescription = null, tint = c.ink3, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(d.sm))
            Text("Drop your students CSV here", style = VTheme.type.bodyStrong.colored(c.ink))
            Text("or tap to browse", style = VTheme.type.caption.colored(c.ink3))
            Spacer(Modifier.height(d.md))
            VButton(text = "Download template", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
        }
    }
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Preview: 347 rows detected", style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.weight(1f))
            VBadge(text = "Validated", tone = VBadgeTone.Success)
        }
        Spacer(Modifier.height(d.sm))
        StatLine("Valid records", "344", c.ink2)
        StatLine("Duplicate roll numbers", "3", c.dangerInk)
        StatLine("Parent accounts to create", "312", c.ink2)
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color) {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.weight(1f))
        Text(value, style = VTheme.type.dataSm.colored(valueColor))
    }
}

// ═══ Completion screen ══════════════════════════════════════════════════════════
@Composable
private fun CompletionScreen(onComplete: () -> Unit) {
    val c = VTheme.colors
    val d = VTheme.dimens

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Teal hero — vertical gradient #3cb9a9 → #2a8f80 + radial white glow
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(c.teal, Color(0xFF2A8F80))),
                )
                .drawBehind {
                    // radial glow circle at 50% 30%, white@30%, fade to transparent ~55%
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.30f),
                            radius = size.maxDimension * 0.55f,
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.30f),
                    )
                },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp, bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "You're all set",
                    style = VTheme.type.h1.colored(Color.White).copy(fontSize = 30.sp, letterSpacing = (-0.02).em),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text("Saraswati Vidya Mandir is live on VidyaSetu.", style = VTheme.type.body.colored(Color.White.copy(alpha = 0.88f)), textAlign = TextAlign.Center)
            }
        }

        // Body
        Column(Modifier.fillMaxWidth().padding(d.lg), verticalArrangement = Arrangement.spacedBy(d.md)) {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    val cells = listOf("6" to "Classes", "8" to "Teachers", "180" to "Students", "156" to "Parents")
                    cells.forEachIndexed { i, (n, l) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(n, style = VTheme.type.dataLg.colored(c.navy).copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                l.uppercase(),
                                style = VTheme.type.labelStrong.colored(c.ink3).copy(fontSize = 10.sp, letterSpacing = 0.06.em),
                            )
                        }
                        if (i < cells.size - 1) {
                            Box(Modifier.width(1.dp).fillMaxHeight().background(c.shadowTint.copy(alpha = 0.06f)))
                        }
                    }
                }
            }

            Text("GET STARTED", style = VTheme.type.labelStrong.colored(c.ink3))
            listOf(
                Triple(VIcons.Mail, "Email teachers their credentials", "8 invites · sent in one tap"),
                Triple(VIcons.Upload, "Download parent invite pack", "Personalised PDF · 156 parents"),
                Triple(VIcons.ShieldCheck, "Review attendance permissions", "Class teachers only by default"),
            ).forEach { (icon, title, sub) ->
                VCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDCF2EF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, contentDescription = null, tint = Color(0xFF006A60), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(d.md))
                        Column(Modifier.weight(1f)) {
                            Text(title, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(sub, style = VTheme.type.caption.colored(c.ink3))
                        }
                        Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(d.xs))
            VButton(
                text = "Open dashboard",
                onClick = onComplete,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Teal,
                trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            Text(
                "You can edit any of this later in Settings.",
                style = VTheme.type.caption.colored(c.ink3),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Local wizard models (mirror Auth.tsx OB* types) ─────────────────────────────
private class OBClass(val name: String, val sections: androidx.compose.runtime.snapshots.SnapshotStateList<String>)
private class OBSubject(
    val id: String,
    val name: String,
    val code: String,
    val type: String,
    val classes: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
)
private class OBTeacher(
    val id: String,
    val name: String,
    val mobile: String,
    val username: String,
    val assignments: androidx.compose.runtime.snapshots.SnapshotStateList<Pair<String, String>>,
)

/** Tiny clickable helper. */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

/** 1px dashed rounded border (the React `border: 1px dashed` available-cell style). */
private fun Modifier.dashedBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp, cornerRadius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        val sw = strokeWidth.toPx()
        val r = cornerRadius.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(sw * 3f, sw * 3f), 0f)
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(sw / 2f, sw / 2f),
            size = Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = sw, pathEffect = dash),
        )
    }
