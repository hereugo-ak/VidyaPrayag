package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherSyllabusScreenV2 — a pixel-faithful copy of `Teacher.tsx → SyllabusFlow`.
 *
 * Class/subject selectors, a "log today's progress" form (chapter/topics/homework/note), a live
 * parent-notification preview, and a "Log & notify parents" stateful button.
 */
@Composable
fun TeacherSyllabusScreenV2(
    classId: String = "",
    subject: String = "",
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var chapter by remember { mutableStateOf("Ch 8 — Trigonometric Identities") }
    var topics by remember { mutableStateOf("") }
    var homework by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
            VLabel("Log today's progress")
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VInput(value = chapter, onValueChange = { chapter = it }, label = "Chapter")
                VInput(value = topics, onValueChange = { topics = it }, label = "Topics covered", placeholder = "Pythagorean identities, sum-of-angles…")
                VInput(value = homework, onValueChange = { homework = it }, label = "Homework given", placeholder = "Exercise 8.3, Q 4–14")
                VInput(value = note, onValueChange = { note = it }, label = "Teaching note (admin only)", placeholder = "Class struggled with topic X")
            }
        }
        VCard {
            VLabel("Parent notification preview")
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp)) {
                Text(
                    "Class 10-A covered Trigonometric Identities in Mathematics today. Homework: Exercise 8.3, Q 4–14.",
                    style = VTheme.type.caption.colored(c.ink2),
                )
            }
        }
        VButton(text = "Log & notify parents", onClick = {}, full = true, size = VButtonSize.Lg, tone = VButtonTone.Lavender, stateful = true, successLabel = "Logged")
    }
}
