package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClass
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VTopTabs
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * PlannerScreen — T-403 (Doc 04 §5.12, Doc 08). The real Planner tab, replacing the staged
 * placeholder. Planner is the browse-from-scratch entry into the teacher's lesson-planning
 * surfaces: **Syllabus** (this phase) and **Homework** (lands in T-406).
 *
 * Because Planner is reached from the dock (not a pre-scoped CTA), it fronts a lightweight
 * class picker — once a class is chosen its TSA id (`TeacherClass.id`) scopes the syllabus
 * screen exactly like a Today deep-link would (X-1). The picker reuses [TeacherClassPicker]
 * (the same owned-classes source as the Update plane), so the scope contract is identical.
 *
 * T-406 (Doc 08 §6–§9): the Homework sub-tab is now the real [TeacherHomeworkScreenV2] —
 * assign composer, roster-joined submissions board and extensions — scoped by the same TSA id
 * the picker pins. The old "coming in T-406" placeholder is gone (DELETE-don't-patch).
 */
@Composable
fun PlannerScreen(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    var sub by remember { mutableStateOf("Syllabus") }
    var selectedClassId by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 16.dp)) {
            VLabel("Planner")
            Text("Plan & track", style = VTheme.type.h1.colored(c.ink), modifier = Modifier.padding(top = 4.dp))
        }
        VTopTabs(
            tabs = listOf("Syllabus", "Homework"),
            selected = sub,
            onSelect = { sub = it },
        )

        // Shared class picker — selecting a class pins its TSA id + a scope label.
        TeacherClassPicker(
            selectedClassId = selectedClassId,
            onSelectClass = { cls: TeacherClass ->
                selectedClassId = cls.id
                selectedScope = "${cls.className} · ${cls.subject}"
            },
        )

        Box(Modifier.fillMaxSize()) {
            when (sub) {
                "Syllabus" -> TeacherSyllabusScreenV2(
                    assignmentId = selectedClassId,
                    scopeHint = selectedScope,
                )
                "Homework" -> TeacherHomeworkScreenV2(
                    assignmentId = selectedClassId,
                    scopeHint = selectedScope,
                )
            }
        }
    }
}
