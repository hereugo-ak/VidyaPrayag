package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.SyllabusUnit
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherSyllabusViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressRing
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherSyllabusScreenV2 — the Syllabus sub-tab of Teacher.tsx → Update.
 *
 * Shows overall coverage as a [VProgressRing] and a list of units the teacher can toggle covered/
 * uncovered (optimistic, with revert-on-failure handled by the VM). Bound to
 * [TeacherSyllabusViewModel].
 */
@Composable
fun TeacherSyllabusScreenV2(
    classId: String,
    subject: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherSyllabusViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(classId, subject) { viewModel.load(classId, subject) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        Text(state.subject.ifBlank { "Syllabus" }, style = VTheme.type.h2.colored(c.ink))
        Text(state.className, style = VTheme.type.caption.colored(c.ink3))

        VCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.md)) {
                VProgressRing(
                    value = state.overallProgress,
                    tone = VBadgeTone.Success,
                    label = "${(state.overallProgress * 100).toInt()}%",
                )
                Column(Modifier.weight(1f)) {
                    Text("Coverage", style = VTheme.type.h4.colored(c.ink))
                    Text("${state.coveredCount}/${state.units.size} units covered", style = VTheme.type.caption.colored(c.ink3))
                }
            }
        }

        if (state.units.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No syllabus", icon = VIcons.Bookmark, body = "No units defined for this subject yet.")
        } else {
            state.units.forEach { UnitRow(it, state.updatingUnitId == it.id, viewModel::toggleUnit) }
        }

        if (state.error != null) Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun UnitRow(u: SyllabusUnit, updating: Boolean, onToggle: (String) -> Unit) {
    val c = VTheme.colors
    VCard(onClick = if (updating) null else ({ onToggle(u.id) })) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            Icon(
                imageVector = if (u.isCovered) VIcons.Check else VIcons.Plus,
                contentDescription = null,
                tint = if (u.isCovered) c.successInk else c.ink3,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(u.title, style = VTheme.type.h4.colored(c.ink))
                if (u.coveredOn != null) Text("Covered ${u.coveredOn}", style = VTheme.type.dataSm.colored(c.ink3))
            }
        }
    }
}
