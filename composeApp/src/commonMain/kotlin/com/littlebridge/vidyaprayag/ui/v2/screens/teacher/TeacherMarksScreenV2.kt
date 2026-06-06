package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherMarksScreenV2 — a pixel-faithful copy of `Teacher.tsx → MarksFlow`.
 *
 * Class/subject selectors, assessment header (with Edit), per-student score boxes, live class-avg
 * footer, and a "Save marks" stateful button.
 */
@Composable
fun TeacherMarksScreenV2(
    classId: String = "",
    examId: String = "",
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VInput(value = "Class 10-A", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
            VInput(value = "Mathematics", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
        }
        VCard {
            VLabel("Assessment")
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    // §6.2 React: title 600, subline mono 11/text-dark-2 (Teacher.tsx:182-183)
                    Text("Unit Test 2 — Trigonometry", style = VTheme.type.bodyStrong.colored(c.ink))
                    Text("02 Jun • Max 100", style = VTheme.type.dataSm.colored(c.ink2).copy(fontSize = 11.sp))
                }
                VButton(text = "Edit", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm)
            }
        }
        VCard {
            val marks = listOf(78, 71, 88, 65, 92, 74)
            MockV2.students.filter { it.klass == "10A" }.take(6).forEachIndexed { i, s ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VAvatar(name = s.name, size = 30.dp)
                    // §6.2 React: name 13px (Teacher.tsx:192)
                    Text(s.name, style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
                    // §6.2 React: <input w-20 px-3 py-1.5 rounded-md font-mono text-right> with
                    // bg rgba(245,245,243,0.06)≈ink@6%, border 1px border-dark-2 (Teacher.tsx:193).
                    Box(
                        Modifier
                            .size(width = 80.dp, height = 32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c.ink.copy(alpha = 0.06f))
                            .border(1.dp, c.border2, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(marks[i].toString(), style = VTheme.type.data.colored(c.ink), modifier = Modifier.padding(end = 12.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Class avg (live)", style = VTheme.type.caption.colored(c.ink2))
                Text("68", style = VTheme.type.data.colored(c.ink))
            }
        }
        VButton(text = "Save marks", onClick = {}, full = true, size = VButtonSize.Lg, tone = VButtonTone.Lavender, stateful = true, successLabel = "Saved")
    }
}
