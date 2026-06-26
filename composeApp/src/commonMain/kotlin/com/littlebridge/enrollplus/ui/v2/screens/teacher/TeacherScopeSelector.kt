package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClassSummaryDto
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherScopeSelector — the scope gate that fronts every UPDATE sub-screen (Attendance, Marks,
 * Syllabus, Homework). Per the scope law, the teacher first picks ONE of their own allocations
 * (class · section · subject), which resolves to the pre-authorized `assignmentId` every typed
 * endpoint requires. Only the teacher's real allocations appear — there is no free-text class.
 *
 * A class-teacher row is badged; a row whose attendance is already done today shows a green tick.
 * Selecting a row hands its [TeacherClassSummaryDto] back so the host can carry both the
 * assignmentId AND the human scope label into the sub-screen's wrong-class guard header.
 */
@Composable
fun TeacherScopeSelector(
    classes: List<TeacherClassSummaryDto>,
    onPick: (TeacherClassSummaryDto) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Pick a class",
    caption: String = "Choose one of your allocations to continue",
) {
    val c = VTheme.colors
    var query by remember { mutableStateOf("") }
    val filtered = remember(classes, query) {
        if (query.isBlank()) classes
        else classes.filter { "${it.className} ${it.section} ${it.subject}".contains(query.trim(), ignoreCase = true) }
    }

    Column(modifier.fillMaxWidth()) {
        TEyebrow("SELECT SCOPE", dot = c.accent)
        Spacer(Modifier.height(6.dp))
        Text(title, style = VTheme.type.h2.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
        Spacer(Modifier.height(2.dp))
        Text(caption, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
        Spacer(Modifier.height(14.dp))

        if (classes.size > 6) {
            ScopeSearchField(query) { query = it }
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.assignmentId }) { cls ->
                ScopeRow(cls, onPick)
            }
        }
    }
}

@Composable
private fun ScopeSearchField(value: String, onChange: (String) -> Unit) {
    val c = VTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.cream)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(VIcons.Search, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = VTheme.type.body.colored(c.ink).copy(fontSize = 14.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text("Search your classes", style = VTheme.type.body.colored(c.ink3).copy(fontSize = 14.sp))
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScopeRow(cls: TeacherClassSummaryDto, onPick: (TeacherClassSummaryDto) -> Unit) {
    val c = VTheme.colors
    val accent = teacherSubjectColor(c, cls.subject.ifBlank { cls.className })
    val ix = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.hairline, RoundedCornerShape(18.dp))
            .clickable(interactionSource = ix, indication = null) { onPick(cls) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TIconDisc(VIcons.School, tint = accent, bg = accent.copy(alpha = 0.12f), size = 44.dp, glyph = 22.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (cls.section.isBlank()) cls.className else "${cls.className}-${cls.section}",
                    style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                )
                if (cls.isClassTeacher) {
                    TPill("CLASS TEACHER", bg = c.accent.copy(alpha = 0.12f), fg = c.accentDeep)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${cls.subject.ifBlank { "—" }} · ${cls.studentCount} students",
                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
            )
        }
        if (cls.todayAttendanceMarked) {
            TIconDisc(VIcons.Check, tint = c.successInk, bg = c.success.copy(alpha = 0.18f), size = 28.dp, glyph = 15.dp)
        } else {
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
        }
    }
}
