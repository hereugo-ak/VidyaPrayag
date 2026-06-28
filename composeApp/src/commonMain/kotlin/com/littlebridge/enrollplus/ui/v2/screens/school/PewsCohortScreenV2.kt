/*
 * File: PewsCohortScreenV2.kt
 * Module: ui.v2.screens.school
 *
 * The REAL school-admin PEWS cohort screen (the live at-risk roster). This is
 * the screen behind the PewsPreview teaser — it shows actual deterministic
 * snapshots served by GET /api/v1/school/pews/cohort, with band counts, a band
 * filter, a manual recompute, and per-student rows that open the detail screen.
 *
 * Honesty (RA-S10 / LAW 6): every row is a real student snapshot. Risk levels,
 * scores and signals are deterministic; the optional AI line is shown only when
 * the server provides it. The screen never invents a student or a number.
 */
package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsCohortDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsConfigDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.pews.presentation.PewsCohortState
import com.littlebridge.enrollplus.feature.pews.presentation.PewsCohortViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * PewsCohortScreenV2 — the admin's live at-risk roster.
 *
 * Wired to [PewsCohortViewModel] (`GET /api/v1/school/pews/cohort`, school-scoped).
 * Tapping a row opens that student's PEWS detail. Three states via [VStateHost].
 */
@Composable
fun PewsCohortScreenV2(
    onBack: () -> Unit = {},
    onOpenStudent: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PewsCohortViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    Column(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        VBackHeader(
            title = "Early Warning",
            onBack = onBack,
            action = {
                VButton(
                    text = if (state.isRunning) "…" else "Recompute",
                    onClick = viewModel::runNow,
                    variant = VButtonVariant.Secondary,
                    size = VButtonSize.Sm,
                    enabled = !state.isRunning && !state.isLoading,
                )
            },
        )
        PewsCohortContent(
            state = state,
            onRetry = viewModel::load,
            onSetMinLevel = viewModel::setMinLevel,
            onOpenStudent = onOpenStudent,
            onSaveConfig = viewModel::saveConfig,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PewsCohortContent(
    state: PewsCohortState,
    onRetry: () -> Unit,
    onSetMinLevel: (String) -> Unit,
    onOpenStudent: (String) -> Unit,
    onSaveConfig: (PewsConfigDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.isEmpty,
        emptyIcon = VIcons.ShieldCheck,
        emptyTitle = "No students need attention",
        emptyBody = "Every student in the selected band is on track right now. " +
            "Tap Recompute to refresh, or widen the band filter.",
        onRetry = onRetry,
        modifier = modifier,
    ) {
        val cohort = state.cohort ?: return@VStateHost
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { RiskBandSummary(cohort) }
            item {
                BandFilterRow(selected = state.minLevel, onSelect = onSetMinLevel)
            }
            if (!cohort.aiEnabled) {
                item { AiDisabledNote() }
            }
            items(cohort.students, key = { it.studentCode }) { s ->
                PewsStudentRow(s, onClick = { onOpenStudent(s.studentCode) })
            }
            // ── Effectiveness rollup (LEARN loop) — admin parity with the web portal
            state.effectiveness?.let { eff ->
                if (eff.total > 0) {
                    item { EffectivenessCard(eff) }
                }
            }
            // ── Config (thresholds, run frequency, AI + parent-share toggles) ──────
            state.config?.let { cfg ->
                item {
                    ConfigCard(
                        config = cfg,
                        isSaving = state.isSavingConfig,
                        onSave = onSaveConfig,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Effectiveness — what the intervention loop is achieving (the LEARN stage). */
@Composable
private fun EffectivenessCard(eff: PewsEffectivenessDto) {
    val c = VTheme.colors
    val resolved = eff.done + eff.dismissed
    val outcomeTotal = eff.improved + eff.unchanged + eff.worsened
    val improvedPct = if (outcomeTotal > 0) (eff.improved * 100) / outcomeTotal else 0
    VCard {
        Text(
            "EFFECTIVENESS",
            style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "What the intervention loop is achieving",
            style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EffStat("Open", "${eff.open}", c.ink, Modifier.weight(1f))
            EffStat("Resolved", "$resolved", c.ink, Modifier.weight(1f))
            EffStat("Improved", if (outcomeTotal > 0) "$improvedPct%" else "—", c.successInk, Modifier.weight(1f))
        }
        if (outcomeTotal > 0) {
            Spacer(Modifier.height(12.dp))
            OutcomeBar("Improved", eff.improved, eff.total, c.success)
            Spacer(Modifier.height(6.dp))
            OutcomeBar("No change", eff.unchanged, eff.total, c.ink3.copy(alpha = 0.5f))
            Spacer(Modifier.height(6.dp))
            OutcomeBar("Worsened", eff.worsened, eff.total, c.danger)
        }
    }
}

@Composable
private fun EffStat(
    label: String,
    value: String,
    fg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(c.cream).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = VTheme.type.dataLg.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
    }
}

@Composable
private fun OutcomeBar(
    label: String,
    value: Int,
    total: Int,
    fill: androidx.compose.ui.graphics.Color,
) {
    val c = VTheme.colors
    val frac = if (total > 0) value.toFloat() / total.toFloat() else 0f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
            Text("$value", style = VTheme.type.caption.colored(c.ink).copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)).background(c.cream),
        ) {
            Box(
                Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(999.dp)).background(fill),
            )
        }
    }
}

/** Config — thresholds, run frequency, and what gets shared (admin parity with web). */
@Composable
private fun ConfigCard(
    config: PewsConfigDto,
    isSaving: Boolean,
    onSave: (PewsConfigDto) -> Unit,
) {
    val c = VTheme.colors
    var draft by remember(config) { mutableStateOf(config) }
    val dirty = draft != config

    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "CONFIGURATION",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Thresholds, run frequency & what's shared",
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
                )
            }
            if (dirty) {
                VButton(
                    text = if (isSaving) "…" else "Save",
                    onClick = { onSave(draft) },
                    variant = VButtonVariant.Primary,
                    size = VButtonSize.Sm,
                    enabled = !isSaving,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        ConfigToggle(
            label = "Relative thresholds",
            hint = "Use z-scores across the cohort rather than fixed floors",
            checked = draft.useRelativeThresholds,
            onChange = { draft = draft.copy(useRelativeThresholds = it) },
        )
        ConfigToggle(
            label = "AI narrative",
            hint = "Let the AI write a plain-language explanation of the signals",
            checked = draft.aiNarrativeEnabled,
            onChange = { draft = draft.copy(aiNarrativeEnabled = it) },
        )
        ConfigToggle(
            label = "Share with parents",
            hint = "When on, parents see a gentle, label-free nudge for their child",
            checked = draft.parentShareEnabled,
            onChange = { draft = draft.copy(parentShareEnabled = it) },
        )
        // Run frequency pills
        Spacer(Modifier.height(10.dp))
        Text(
            "Run frequency",
            style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FreqChip("Daily", "daily", draft.runFrequency, { draft = draft.copy(runFrequency = it) }, Modifier.weight(1f))
            FreqChip("Weekly", "weekly", draft.runFrequency, { draft = draft.copy(runFrequency = it) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConfigToggle(
    label: String,
    hint: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = VTheme.type.body.colored(c.ink).copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                hint,
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp, lineHeight = 15.sp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.card,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.card,
                uncheckedTrackColor = c.ink3.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun FreqChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val isSel = selected == value
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSel) c.ink else c.cream)
            .clickable { onSelect(value) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = VTheme.type.label.colored(if (isSel) c.card else c.ink2)
                .copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        )
    }
}

/** Real band counts — High / Watch (medium) / Watch (early). */
@Composable
private fun RiskBandSummary(cohort: PewsCohortDto) {
    val c = VTheme.colors
    VCard {
        Text(
            "RISK BAND",
            style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BandCount("High", cohort.high, c.danger, c.dangerInk, Modifier.weight(1f))
            BandCount("Medium", cohort.medium, c.warning, c.warningInk, Modifier.weight(1f))
            BandCount("Watch", cohort.watch, c.success, c.successInk, Modifier.weight(1f))
        }
        cohort.runDate?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                "As of $it",
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }
    }
}

@Composable
private fun BandCount(
    label: String,
    count: Int,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$count", style = VTheme.type.dataLg.colored(fg).copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(2.dp))
        Text(label, style = VTheme.type.caption.colored(fg).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun BandFilterRow(selected: String, onSelect: (String) -> Unit) {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("All", "watch", selected, onSelect, Modifier.weight(1f))
        FilterChip("Medium+", "medium", selected, onSelect, Modifier.weight(1f))
        FilterChip("High only", "high", selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun FilterChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val isSel = selected == value
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSel) c.ink else c.cream)
            .clickable { onSelect(value) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = VTheme.type.label.colored(if (isSel) c.card else c.ink2)
                .copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        )
    }
}

@Composable
private fun AiDisabledNote() {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(c.cream).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(VIcons.AlertCircle, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
        Text(
            "AI explanations are off. Rows still show the real attendance, marks and leave signals.",
            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp),
        )
    }
}

@Composable
private fun PewsStudentRow(s: PewsStudentDto, onClick: () -> Unit) {
    val c = VTheme.colors
    val (tone, levelLabel) = when (s.riskLevel) {
        "high" -> VBadgeTone.Danger to "High"
        "medium" -> VBadgeTone.Warning to "Medium"
        else -> VBadgeTone.Success to "Watch"
    }
    VCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // initial avatar
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.cream),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.name.firstOrNull()?.uppercase() ?: "?",
                    style = VTheme.type.bodyStrong.colored(c.ink2),
                )
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
        // signal chips (deterministic reasons)
        if (s.signals.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.signals.take(3).forEach { sig ->
                    SignalChip(sig.label)
                }
            }
        }
        // AI one-liner — only when present
        val aiLine = s.aiNarrative ?: s.aiCause
        if (!aiLine.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(13.dp))
                Text(
                    aiLine,
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun SignalChip(label: String) {
    val c = VTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(c.cream).padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
    }
}
