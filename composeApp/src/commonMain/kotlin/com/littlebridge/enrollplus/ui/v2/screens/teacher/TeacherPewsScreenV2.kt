/*
 * File: TeacherPewsScreenV2.kt
 * Module: ui.v2.screens.teacher
 *
 * The teacher's "Students needing attention" screen — own-class scoped
 * (GET /api/v1/teacher/pews/students). Shows each at-risk student in the
 * teacher's assigned classes with the deterministic signals + optional AI line,
 * and lets the teacher act on interventions assigned to them
 * (PATCH /api/v1/teacher/pews/interventions/{id}).
 *
 * Honesty (RA-S10 / LAW 6): every student is a real snapshot in a class the
 * teacher actually teaches. AI text shows only when the server provides it.
 */
package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsInterventionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.pews.presentation.TeacherPewsState
import com.littlebridge.enrollplus.feature.pews.presentation.TeacherPewsViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeacherPewsScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherPewsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    Column(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        VBackHeader(title = "Needs Attention", onBack = onBack)
        TeacherPewsContent(
            state = state,
            onRetry = viewModel::load,
            onMarkDone = { id, outcome -> viewModel.updateIntervention(id, status = "done", outcome = outcome) },
            onDismiss = { id -> viewModel.updateIntervention(id, status = "dismissed") },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TeacherPewsContent(
    state: TeacherPewsState,
    onRetry: () -> Unit,
    onMarkDone: (String, String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.isEmpty,
        emptyIcon = VIcons.ShieldCheck,
        emptyTitle = "Your classes are on track",
        emptyBody = "No student in your assigned classes needs attention right now.",
        onRetry = onRetry,
        modifier = modifier,
    ) {
        // index interventions by student for quick lookup
        val byStudent = state.interventions.groupBy { it.studentCode }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.students, key = { it.studentCode }) { s ->
                TeacherStudentCard(
                    s = s,
                    interventions = byStudent[s.studentCode].orEmpty(),
                    updatingIds = state.updatingIds,
                    onMarkDone = onMarkDone,
                    onDismiss = onDismiss,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TeacherStudentCard(
    s: PewsStudentDto,
    interventions: List<PewsInterventionDto>,
    updatingIds: Set<String>,
    onMarkDone: (String, String) -> Unit,
    onDismiss: (String) -> Unit,
) {
    val c = VTheme.colors
    val (tone, levelLabel) = when (s.riskLevel) {
        "high" -> VBadgeTone.Danger to "High"
        "medium" -> VBadgeTone.Warning to "Medium"
        else -> VBadgeTone.Success to "Watch"
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.cream), contentAlignment = Alignment.Center) {
                Text(s.name.firstOrNull()?.uppercase() ?: "?", style = VTheme.type.bodyStrong.colored(c.ink2))
            }
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink), maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Class ${s.className}${if (s.section.isNotBlank()) "-${s.section}" else ""}",
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
                )
            }
            VBadge(text = levelLabel, tone = tone)
        }

        // deterministic metrics
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.attendancePct?.let { MiniStat("Attendance", "$it%") }
            s.marksPct?.let { MiniStat("Marks", "$it%") }
            if (s.leaveCount > 0) MiniStat("Leaves", "${s.leaveCount}")
        }

        // signals
        if (s.signals.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.signals.take(3).forEach { sig ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(c.cream).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(sig.label, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                    }
                }
            }
        }

        // AI recommendation (only if present)
        val aiLine = s.aiRecommendation ?: s.aiNarrative
        if (!aiLine.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(13.dp))
                Text(aiLine, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp), maxLines = 3)
            }
        }

        // my open interventions for this student
        interventions.filter { it.status == "open" || it.status == "in_progress" }.forEach { iv ->
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.cream).padding(10.dp)) {
                val notes = iv.notes
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(iv.actionType.replace('_', ' '), style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp), modifier = Modifier.weight(1f))
                        // Escalation badge
                        if (iv.escalationLevel > 0) {
                            val escLabel = if (iv.escalationLevel >= 2) "ESCALATED" else "REMINDED"
                            val escTone = if (iv.escalationLevel >= 2) VBadgeTone.Danger else VBadgeTone.Warning
                            VBadge(text = escLabel, tone = escTone)
                        }
                    }
                    // Urgency + SLA
                    iv.urgency?.let { urg ->
                        Spacer(Modifier.height(4.dp))
                        val urgColor = when (urg) { "high" -> c.dangerInk; "medium" -> c.warningInk; else -> c.ink3 }
                        Text("Urgency: ${urg}", style = VTheme.type.caption.colored(urgColor).copy(fontSize = 11.sp))
                    }
                    iv.slaDays?.let { sla ->
                        Text("SLA: $sla days${iv.followUpDate?.let { " · follow-up $it" } ?: ""}", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
                    }
                    if (!notes.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(notes, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp))
                    }
                    // Plan steps
                    iv.planJson?.let { planJson ->
                        val steps = parseTeacherPlanSteps(planJson)
                        if (steps.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            steps.forEachIndexed { i, step ->
                                Text("${i + 1}. $step", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp, lineHeight = 15.sp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton("Improved", { onMarkDone(iv.id, "improved") }, variant = VButtonVariant.Primary, size = VButtonSize.Sm, enabled = iv.id !in updatingIds)
                        VButton("No change", { onMarkDone(iv.id, "unchanged") }, variant = VButtonVariant.Secondary, size = VButtonSize.Sm, enabled = iv.id !in updatingIds)
                        VButton("Dismiss", { onDismiss(iv.id) }, variant = VButtonVariant.Ghost, size = VButtonSize.Sm, enabled = iv.id !in updatingIds)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    val c = VTheme.colors
    Column {
        Text(value, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

/** Parse plan_json to extract step descriptions. */
private fun parseTeacherPlanSteps(planJson: String): List<String> {
    return runCatching {
        val json = Json { ignoreUnknownKeys = true }
        val obj = json.parseToJsonElement(planJson)
        val steps = obj.jsonObject["steps"]?.jsonArray
            ?: obj.jsonObject["plan"]?.jsonArray
            ?: return emptyList()
        steps.mapNotNull { step ->
            when (step) {
                is JsonObject -> step["description"]?.jsonPrimitive?.contentOrNull
                    ?: step["action"]?.jsonPrimitive?.contentOrNull
                    ?: step["text"]?.jsonPrimitive?.contentOrNull
                else -> step.jsonPrimitive.contentOrNull
            }
        }
    }.getOrDefault(emptyList())
}
