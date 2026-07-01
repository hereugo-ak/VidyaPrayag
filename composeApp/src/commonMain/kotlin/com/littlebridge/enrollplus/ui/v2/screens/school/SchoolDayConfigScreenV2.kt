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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDaySlotDto
import com.littlebridge.enrollplus.feature.admin.presentation.SchoolDayConfigState
import com.littlebridge.enrollplus.feature.admin.presentation.SchoolDayConfigViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VConfirmDialog
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SchoolDayConfigScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SchoolDayConfigViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    Column(modifier.fillMaxSize().statusBarsPadding()
        .imePadding()
        .navigationBarsPadding()) {
        VBackHeader(title = "School Day Config", onBack = onBack)
        SchoolDayConfigContent(
            state = state,
            onRetry = viewModel::loadConfigs,
            onClearMessages = viewModel::clearMessages,
            onCreate = { name, days, level, slots, onDone ->
                viewModel.createConfig(name, days, level, slots, onDone)
            },
            onDeactivate = viewModel::deactivateConfig,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SchoolDayConfigContent(
    state: SchoolDayConfigState,
    onRetry: () -> Unit,
    onClearMessages: () -> Unit,
    onCreate: (String, String, String, List<SchoolDaySlotDto>, (() -> Unit)?) -> Unit,
    onDeactivate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var composerOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("1,2,3,4,5") }
    var level by remember { mutableStateOf("ALL") }
    var deactivateTargetId by remember { mutableStateOf<String?>(null) }

    VConfirmDialog(
        visible = deactivateTargetId != null,
        title = "Deactivate config?",
        message = "This will deactivate the school day configuration. You can reactivate it later.",
        confirmLabel = "Deactivate",
        onConfirm = {
            deactivateTargetId?.let { onDeactivate(it) }
            deactivateTargetId = null
        },
        onDismiss = { deactivateTargetId = null },
        icon = VIcons.AlertTriangle,
    )

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.errorMessage,
            isEmpty = state.configs.isEmpty() && !composerOpen,
            emptyTitle = "No day configs yet",
            emptyBody = "Create your first school day configuration to define the bell schedule.",
            emptyIcon = VIcons.Calendar,
            onRetry = onRetry,
        ) {
            if (composerOpen) {
                VCard {
                    Text("New Day Config", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(12.dp))
                    VInput(value = name, onValueChange = { name = it }, label = "Name", placeholder = "e.g. Default Weekday")
                    Spacer(Modifier.height(8.dp))
                    VInput(value = days, onValueChange = { days = it }, label = "Applicable Days", placeholder = "1,2,3,4,5 (Mon-Fri)")
                    Spacer(Modifier.height(8.dp))
                    VInput(value = level, onValueChange = { level = it }, label = "Class Level", placeholder = "ALL / PRIMARY / SECONDARY")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Slots will be added after creation. You can edit them later.",
                        style = VTheme.type.caption.colored(c.ink3),
                    )
                    val info = state.infoMessage
                    if (info != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(info, style = VTheme.type.caption.colored(c.successInk))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            VButton(
                                text = "Cancel",
                                onClick = {
                                    composerOpen = false
                                    name = ""; days = "1,2,3,4,5"; level = "ALL"
                                    onClearMessages()
                                },
                                full = true,
                                variant = VButtonVariant.Secondary,
                                tone = VButtonTone.Navy,
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            VButton(
                                text = "Create",
                                onClick = {
                                    onCreate(name, days, level, emptyList()) {
                                        composerOpen = false
                                        name = ""; days = "1,2,3,4,5"; level = "ALL"
                                    }
                                },
                                full = true,
                                variant = VButtonVariant.Primary,
                                tone = VButtonTone.Teal,
                                loading = state.isSaving,
                                enabled = name.isNotBlank() && days.isNotBlank() && !state.isSaving,
                            )
                        }
                    }
                }
            } else {
                VButton(
                    text = "New Day Config",
                    onClick = { composerOpen = true },
                    full = true,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                )
            }

            if (state.configs.isNotEmpty()) {
                VSectionHeader(title = "CONFIGURATIONS")
                state.configs.forEach { config ->
                    ConfigCard(
                        config = config,
                        onDeactivate = { deactivateTargetId = it },
                        isSaving = state.isSaving,
                    )
                }
            }

            val info = state.infoMessage
            if (info != null && !composerOpen) {
                Text(info, style = VTheme.type.caption.colored(c.successInk))
            }
        }
    }
}

@Composable
private fun ConfigCard(
    config: SchoolDayConfigDto,
    onDeactivate: (String) -> Unit,
    isSaving: Boolean,
) {
    val c = VTheme.colors
    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.isActive) {
                        VBadge(text = "ACTIVE", tone = VBadgeTone.Success)
                    } else {
                        VBadge(text = "INACTIVE", tone = VBadgeTone.Neutral)
                    }
                    Text(config.name, style = VTheme.type.h3.colored(c.ink))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Days: ${config.applicableDays}  ·  Level: ${config.classLevel}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
        }

        if (config.slots.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            config.slots.forEachIndexed { i, slot ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
                SlotRow(slot)
            }
        }

        if (config.isActive) {
            Spacer(Modifier.height(12.dp))
            VButton(
                text = "Deactivate",
                onClick = { onDeactivate(config.id) },
                full = true,
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Navy,
                loading = isSaving,
                enabled = !isSaving,
            )
        }
    }
}

@Composable
private fun SlotRow(slot: SchoolDaySlotDto) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(slotTypeColor(slot.slotType, c))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                slot.slotType,
                style = VTheme.type.label.colored(c.ink),
            )
        }
        Column(Modifier.weight(1f)) {
            if (slot.label.isNotBlank()) {
                Text(slot.label, style = VTheme.type.bodyStrong.colored(c.ink))
            }
            Text(
                "${slot.startTime} – ${slot.endTime}",
                style = VTheme.type.caption.colored(c.ink3),
            )
        }
        Text(
            "#${slot.slotIndex}",
            style = VTheme.type.dataSm.colored(c.ink2),
        )
    }
}

@Composable
private fun slotTypeColor(type: String, c: com.littlebridge.enrollplus.ui.v2.theme.VColors): androidx.compose.ui.graphics.Color {
    return when (type) {
        "TEACHING" -> c.accent.copy(alpha = 0.1f)
        "BREAK" -> c.warning.copy(alpha = 0.1f)
        "ASSEMBLY" -> c.teal.copy(alpha = 0.1f)
        "LAB" -> c.lavenderLight.copy(alpha = 0.3f)
        else -> c.cream
    }
}
