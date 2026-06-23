package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClass
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * RA-40: the teacher write plane (Attendance / Syllabus) was unreachable because the
 * portal hardcoded blank class/subject ids. This selector sources the real owned classes
 * from `GET /teacher/classes`, feeding non-blank ids into the data-entry screens.
 *
 * NOTE (T-305): the legacy `TeacherExamPicker` (the inline exam list + create for the old
 * Marks write-plane leaf) lived here and was DELETED — marks graduated to the dedicated
 * Gradebook tab ([TeacherGradebookScreenV2]) with the full assessment → entry → publish
 * lifecycle, so a separate exam_id picker is no longer a reachability link. Only the shared
 * class picker (used by the Syllabus leaf) remains.
 */
@Composable
fun TeacherClassPicker(
    classesViewModel: TeacherClassesViewModel = koinViewModel(),
    selectedClassId: String,
    onSelectClass: (TeacherClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by classesViewModel.state.collectAsStateV2()
    val c = VTheme.colors

    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            "SELECT CLASS",
            style = VTheme.type.label.copy(color = c.ink3, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        when {
            state.isLoading -> Text("Loading classes…", style = VTheme.type.body.copy(color = c.ink3))
            state.error != null -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(state.error ?: "Couldn't load classes", style = VTheme.type.body.copy(color = c.danger))
                VButton(
                    text = "Retry",
                    onClick = { classesViewModel.load() },
                    variant = VButtonVariant.Ghost,
                    size = VButtonSize.Sm,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            state.classes.isEmpty() -> Text(
                "No classes assigned to you yet. Ask your administrator to assign a class.",
                style = VTheme.type.body.copy(color = c.ink3),
            )
            else -> Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.classes.forEach { cls ->
                    val selected = cls.id == selectedClassId
                    val label = "${cls.className} · ${cls.subject}"
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) c.tealDeep else c.cream)
                            .border(
                                width = 1.dp,
                                color = if (selected) c.tealDeep else c.hairline,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelectClass(cls) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            label,
                            style = VTheme.type.body.copy(
                                color = if (selected) c.card else c.ink,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}
