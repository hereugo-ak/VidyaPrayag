/*
 * File: PewsStudentDetailScreenV2.kt
 * Module: ui.v2.screens.school
 *
 * The school-admin PEWS student detail — one at-risk student's current snapshot
 * (GET /api/v1/school/pews/student/{code}): the deterministic metrics + signal
 * bundle, the (nullable) AI explanation, the open interventions, and a short
 * history. Lets the admin mark an intervention done / dismissed and record an
 * outcome (the LEARN loop).
 *
 * Honesty (RA-S10 / LAW 6): metrics and signals are deterministic and always
 * shown; the AI cause/recommendation render only when the server provides them.
 */
package com.littlebridge.enrollplus.ui.v2.screens.school

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.pews.domain.model.ParentDraftDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsInterventionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.pews.presentation.PewsStudentDetailState
import com.littlebridge.enrollplus.feature.pews.presentation.PewsStudentDetailViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PewsStudentDetailScreenV2(
    studentCode: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PewsStudentDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(studentCode) { viewModel.load(studentCode) }

    Column(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        VBackHeader(title = "Student Signal", onBack = onBack)
        PewsStudentDetailContent(
            state = state,
            onRetry = { viewModel.load(studentCode) },
            onStart = { id -> viewModel.updateIntervention(id, status = "in_progress") },
            onMarkDone = { id, outcome -> viewModel.updateIntervention(id, status = "done", outcome = outcome) },
            onDismiss = { id -> viewModel.updateIntervention(id, status = "dismissed") },
            onGenerateDraft = { id, lang -> viewModel.generateParentDraft(id, lang) },
            onSendParentMessage = viewModel::sendParentMessage,
            onClearDraft = viewModel::clearDraft,
            onClearMessage = viewModel::clearMessages,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PewsStudentDetailContent(
    state: PewsStudentDetailState,
    onRetry: () -> Unit,
    onStart: (String) -> Unit,
    onMarkDone: (String, String) -> Unit,
    onDismiss: (String) -> Unit,
    onGenerateDraft: (String, String) -> Unit,
    onSendParentMessage: (String) -> Unit,
    onClearDraft: (String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.isEmpty,
        emptyIcon = VIcons.ShieldCheck,
        emptyTitle = "No signal on record",
        emptyBody = "This student has no early-warning snapshot yet.",
        onRetry = onRetry,
        modifier = modifier,
    ) {
        val detail = state.detail ?: return@VStateHost
        val cur = detail.current ?: return@VStateHost
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard(cur)
            MetricsCard(cur)
            if (cur.signals.isNotEmpty()) SignalsCard(cur)
            AiExplanationCard(cur)
            if (state.interventions.isNotEmpty()) {
                Text(
                    "INTERVENTIONS",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                )
                state.interventions.forEach { iv ->
                    InterventionCard(
                        iv = iv,
                        isUpdating = iv.id in state.updatingIds,
                        parentDrafts = state.parentDrafts,
                        draftLoadingIds = state.draftLoadingIds,
                        onStart = onStart,
                        onMarkDone = onMarkDone,
                        onDismiss = onDismiss,
                        onGenerateDraft = onGenerateDraft,
                        onSendParentMessage = onSendParentMessage,
                        onClearDraft = onClearDraft,
                    )
                }
            }
            if (detail.history.size > 1) HistoryCard(detail.history)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderCard(s: PewsStudentDto) {
    val c = VTheme.colors
    val (tone, levelLabel) = when (s.riskLevel) {
        "high" -> VBadgeTone.Danger to "High risk"
        "medium" -> VBadgeTone.Warning to "Medium risk"
        else -> VBadgeTone.Success to "Watch"
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.h3.colored(c.ink))
                Spacer(Modifier.height(2.dp))
                Text(
                    "Class ${s.className}${if (s.section.isNotBlank()) "-${s.section}" else ""}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            VBadge(text = levelLabel, tone = tone)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Risk score ${s.riskScore} · as of ${s.runDate}",
            style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
        )
    }
}

@Composable
private fun MetricsCard(s: PewsStudentDto) {
    val c = VTheme.colors
    VCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Attendance", s.attendancePct?.let { "$it%" } ?: "—", s.attendanceSlope, Modifier.weight(1f))
            Metric("Marks", s.marksPct?.let { "$it%" } ?: "—", s.marksSlope, Modifier.weight(1f))
            Metric("Leaves", "${s.leaveCount}", null, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, slope: Double?, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = VTheme.type.data.colored(c.ink).copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(2.dp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
        if (slope != null && slope != 0.0) {
            Spacer(Modifier.height(4.dp))
            val falling = slope < 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                    if (falling) VIcons.TrendingDown else VIcons.TrendingUp,
                    contentDescription = null,
                    tint = if (falling) c.dangerInk else c.successInk,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    if (falling) "falling" else "rising",
                    style = VTheme.type.caption.colored(if (falling) c.dangerInk else c.successInk).copy(fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun SignalsCard(s: PewsStudentDto) {
    val c = VTheme.colors
    VCard {
        Text("WHY THIS STUDENT", style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
        Spacer(Modifier.height(10.dp))
        s.signals.forEach { sig ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val dot = when {
                    sig.severity >= 3 -> c.dangerInk
                    sig.severity == 2 -> c.warningInk
                    else -> c.successInk
                }
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dot))
                Text(sig.label, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AiExplanationCard(s: PewsStudentDto) {
    val c = VTheme.colors
    val cause = s.aiCause
    val rec = s.aiRecommendation
    val narrative = s.aiNarrative
    if (cause.isNullOrBlank() && rec.isNullOrBlank() && narrative.isNullOrBlank()) return
    VCard(background = c.teal.copy(alpha = 0.08f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(VIcons.Sparkles, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(14.dp))
            Text(
                "AI EXPLANATION",
                style = VTheme.type.label.colored(c.tealDeep).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            )
        }
        if (!narrative.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(narrative, style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp, lineHeight = 19.sp))
        }
        if (!cause.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Likely cause", style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
            Text(cause, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp, lineHeight = 19.sp))
        }
        if (!rec.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Suggested action", style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
            Text(rec, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp, lineHeight = 19.sp))
        }
        s.aiProviderUsed?.let {
            Spacer(Modifier.height(8.dp))
            Text("Generated by $it · review before acting", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
        }
    }
}

@Composable
private fun InterventionCard(
    iv: PewsInterventionDto,
    isUpdating: Boolean,
    parentDrafts: Map<String, ParentDraftDto>,
    draftLoadingIds: Set<String>,
    onStart: (String) -> Unit,
    onMarkDone: (String, String) -> Unit,
    onDismiss: (String) -> Unit,
    onGenerateDraft: (String, String) -> Unit,
    onSendParentMessage: (String) -> Unit,
    onClearDraft: (String) -> Unit,
) {
    val c = VTheme.colors
    val statusTone = when (iv.status) {
        "done" -> VBadgeTone.Success
        "dismissed" -> VBadgeTone.Neutral
        "in_progress" -> VBadgeTone.Accent
        else -> VBadgeTone.Warning
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(iv.actionType.replace('_', ' '), style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.weight(1f))
            VBadge(text = iv.status.replace('_', ' '), tone = statusTone)
        }

        // Urgency + escalation indicators
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            iv.urgency?.let { urg ->
                val urgTone = when (urg) {
                    "high" -> VBadgeTone.Danger
                    "medium" -> VBadgeTone.Warning
                    else -> VBadgeTone.Neutral
                }
                VBadge(text = urg.replaceFirstChar { it.uppercase() }, tone = urgTone)
            }
            if (iv.escalationLevel > 0) {
                val escLabel = when (iv.escalationLevel) {
                    2 -> "ESCALATED"
                    else -> "REMINDED"
                }
                val escTone = if (iv.escalationLevel >= 2) VBadgeTone.Danger else VBadgeTone.Warning
                VBadge(text = escLabel, tone = escTone)
            }
            iv.causeFamily?.let { cf ->
                VBadge(text = cf, tone = VBadgeTone.Neutral)
            }
        }

        // SLA countdown
        iv.slaDays?.let { sla ->
            Spacer(Modifier.height(6.dp))
            val slaText = if (iv.followUpDate != null) {
                "SLA: $sla days · follow-up ${iv.followUpDate}"
            } else {
                "SLA: $sla days"
            }
            Text(
                slaText,
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }

        val notes = iv.notes
        if (!notes.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(notes, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp))
        }

        // Plan steps from plan_json
        iv.planJson?.let { planJson ->
            val steps = parsePlanSteps(planJson)
            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "PLAN",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                )
                Spacer(Modifier.height(4.dp))
                steps.forEachIndexed { i, step ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "${i + 1}.",
                            style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
                        )
                        Text(
                            step,
                            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Parent draft — prefer API-generated from parentDrafts, fall back to pre-generated from CaseFile
        val draftBody = parentDrafts[iv.id]?.body ?: iv.parentDraftBody
        val draftLang = parentDrafts[iv.id]?.language ?: iv.parentDraftLang
        val isParentAction = iv.actionType.contains("parent") || iv.actionType.contains("message") || iv.actionType.contains("call") || iv.actionType.contains("visit")
        val hasDraft = !draftBody.isNullOrBlank()

        if (hasDraft && draftBody != null) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.teal.copy(alpha = 0.1f)).padding(8.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(VIcons.Sparkles, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "PARENT MESSAGE (${draftLang?.uppercase() ?: "EN"})",
                            style = VTheme.type.label.colored(c.tealDeep).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.weight(1f),
                        )
                        if (parentDrafts[iv.id] != null) {
                            VButton("✕", { onClearDraft(iv.id) }, variant = VButtonVariant.Ghost, size = VButtonSize.Sm)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(draftBody, style = VTheme.type.body.colored(c.ink).copy(fontSize = 12.sp, lineHeight = 17.sp))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Opened ${iv.openedAt}", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))

        val open = iv.status == "open" || iv.status == "in_progress"
        val outcome = iv.outcome
        val isDraftLoading = iv.id in draftLoadingIds
        if (open) {
            Spacer(Modifier.height(10.dp))
            if (iv.status == "open") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(
                        text = "Start",
                        onClick = { onStart(iv.id) },
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Sm,
                        enabled = !isUpdating,
                    )
                    VButton(
                        text = "Dismiss",
                        onClick = { onDismiss(iv.id) },
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Sm,
                        enabled = !isUpdating,
                    )
                }
            } else {
                // In-progress: action-type-specific
                if (isParentAction) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasDraft) {
                            VButton(
                                text = "Send to parent",
                                onClick = { onSendParentMessage(iv.id) },
                                variant = VButtonVariant.Primary,
                                size = VButtonSize.Sm,
                                enabled = !isUpdating,
                            )
                        } else {
                            var draftLang by remember { mutableStateOf("en") }
                            var langDropdownOpen by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                VButton(
                                    text = "Draft parent message",
                                    onClick = { onGenerateDraft(iv.id, draftLang) },
                                    variant = VButtonVariant.Secondary,
                                    size = VButtonSize.Sm,
                                    enabled = !isDraftLoading,
                                )
                                Box {
                                    VButton(
                                        text = draftLang.uppercase(),
                                        onClick = { langDropdownOpen = true },
                                        variant = VButtonVariant.Ghost,
                                        size = VButtonSize.Sm,
                                    )
                                    DropdownMenu(
                                        expanded = langDropdownOpen,
                                        onDismissRequest = { langDropdownOpen = false },
                                        containerColor = c.card,
                                    ) {
                                        listOf("en" to "English", "hi" to "हिन्दी", "mr" to "मराठी", "ta" to "தமிழ்", "te" to "తెలుగు", "bn" to "বাংলা").forEach { (code, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label, style = VTheme.type.body.colored(c.ink)) },
                                                onClick = {
                                                    draftLang = code
                                                    langDropdownOpen = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        VButton(
                            text = "Dismiss",
                            onClick = { onDismiss(iv.id) },
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Sm,
                            enabled = !isUpdating,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = "Mark improved",
                            onClick = { onMarkDone(iv.id, "improved") },
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Sm,
                            enabled = !isUpdating,
                        )
                        VButton(
                            text = "No change",
                            onClick = { onMarkDone(iv.id, "unchanged") },
                            variant = VButtonVariant.Secondary,
                            size = VButtonSize.Sm,
                            enabled = !isUpdating,
                        )
                        VButton(
                            text = "Dismiss",
                            onClick = { onDismiss(iv.id) },
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Sm,
                            enabled = !isUpdating,
                        )
                    }
                }
            }
        } else if (!outcome.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Outcome: $outcome", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
        }
    }
}

/** Parse plan_json to extract step descriptions. */
private fun parsePlanSteps(planJson: String): List<String> {
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

@Composable
private fun HistoryCard(history: List<PewsStudentDto>) {
    val c = VTheme.colors
    VCard {
        Text("HISTORY", style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        history.take(8).forEach { h ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(h.runDate, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp), modifier = Modifier.weight(1f))
                val tone = when (h.riskLevel) {
                    "high" -> c.dangerInk
                    "medium" -> c.warningInk
                    else -> c.successInk
                }
                Text(
                    "${h.riskLevel} · ${h.riskScore}",
                    style = VTheme.type.caption.colored(tone).copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}
