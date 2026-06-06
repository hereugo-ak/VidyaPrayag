package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClass
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherClassesScreenV2 — teacher "My Classes" tab, translated from Teacher.tsx → MyClasses.
 *
 * Each assigned class card shows subject, headcount, syllabus progress and average attendance,
 * with a class-teacher badge. Bound to the existing [TeacherClassesViewModel]. Tapping a card
 * surfaces the class id to the host (navigation handled in Phase 3E).
 */
@Composable
fun TeacherClassesScreenV2(
    onOpenClass: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherClassesViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        Text("My Classes", style = VTheme.type.h2.colored(c.ink))

        if (state.classes.isEmpty() && !state.isLoading) {
            VEmptyState(
                title = "No assigned classes",
                icon = VIcons.School,
                body = "Your school admin hasn't assigned classes to you yet.",
            )
        } else {
            state.classes.forEach { ClassCard(it, onOpenClass) }
        }

        if (state.error != null) {
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun ClassCard(cls: TeacherClass, onOpen: (String) -> Unit) {
    val c = VTheme.colors
    VCard(onClick = { onOpen(cls.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm)) {
            Text(cls.className, style = VTheme.type.h3.colored(c.ink), modifier = Modifier.weight(1f))
            if (cls.isClassTeacher) VBadge(text = "CLASS TEACHER", tone = VBadgeTone.Arctic)
        }
        Text("${cls.subject} · ${cls.studentCount} students", style = VTheme.type.caption.colored(c.ink3))
        Spacer(Modifier.height(VTheme.dimens.sm))
        VLabel("SYLLABUS ${(cls.syllabusProgress * 100).toInt()}%")
        VProgressBar(value = cls.syllabusProgress, tone = VBadgeTone.Success)
        Spacer(Modifier.height(VTheme.dimens.xs))
        VLabel("AVG ATTENDANCE ${(cls.avgAttendance * 100).toInt()}%")
        VProgressBar(value = cls.avgAttendance, tone = VBadgeTone.Arctic)
    }
}
