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

private val VALID_LEVELS = setOf("ALL", "PRIMARY", "SECONDARY")
private val SLOT_TYPES = listOf("TEACHING", "BREAK", "ASSEMBLY", "LAB", "FREE", "ZERO")

private fun isValidDays(days: String): Boolean {
    val parts = days.split(",").map { it.trim() }
    val nums = parts.mapNotNull { it.toIntOrNull() }
    return nums.size == parts.size && nums.all { it in 1..7 }
}

private fun emptySlot(index: Int) = SchoolDaySlotDto(
    slotIndex = index, slotType = "TEACHING", label = "",
    startTime = "08:00", endTime = "08:45", isDouble = false, doubleGroup = 0,
)

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
            onUpdate = { id, name, days, level, slots, onDone ->
                viewModel.updateConfig(id, name, days, level, slots, true, onDone)
            },
            onDeactivate = viewModel::deactivateConfig,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun SchoolDayConfigEmbeddedV2(
    modifier: Modifier = Modifier,
    viewModel: SchoolDayConfigViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    SchoolDayConfigContent(
        state = state,
        onRetry = viewModel::loadConfigs,
        onClearMessages = viewModel::clearMessages,
        onCreate = { name, days, level, slots, onDone ->
            viewModel.createConfig(name, days, level, slots, onDone)
        },
        onUpdate = { id, name, days, level, slots, onDone ->
            viewModel.updateConfig(id, name, days, level, slots, true, onDone)
        },
        onDeactivate = viewModel::deactivateConfig,
        modifier = modifier,
    )
}

@Composable
private fun SchoolDayConfigContent(
    state: SchoolDayConfigState,
    onRetry: () -> Unit,
    onClearMessages: () -> Unit,
    onCreate: (String, String, String, List<SchoolDaySlotDto>, (() -> Unit)?) -> Unit,
    onUpdate: (String, String, String, String, List<SchoolDaySlotDto>, (() -> Unit)?) -> Unit,
    onDeactivate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var composerOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("1,2,3,4,5") }
    var level by remember { mutableStateOf("ALL") }
    var slots by remember { mutableStateOf<List<SchoolDaySlotDto>>(emptyList()) }
    var deactivateTargetId by remember { mutableStateOf<String?>(null) }

    val isEditing = editingId != null
    val showForm = composerOpen || isEditing
    val daysValid = isValidDays(days.trim())
    val levelValid = level.trim() in VALID_LEVELS
    val formValid = name.isNotBlank() && daysValid && levelValid && !state.isSaving

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
            isEmpty = state.configs.isEmpty() && !showForm,
            emptyTitle = "No day configs yet",
            emptyBody = "Create your first school day configuration to define the bell schedule.",
            emptyIcon = VIcons.Calendar,
            onRetry = onRetry,
        ) {
            if (showForm) {
                ConfigFormCard(
                    title = if (isEditing) "Edit Day Config" else "New Day Config",
                    name = name, onNameChange = { name = it },
                    days = days, onDaysChange = { days = it },
                    daysValid = daysValid,
                    level = level, onLevelChange = { level = it },
                    levelValid = levelValid,
                    slots = slots, onSlotsChange = { slots = it },
                    isSaving = state.isSaving,
                    formValid = formValid,
                    onSubmit = {
                        if (isEditing) {
                            onUpdate(editingId!!, name, days, level, slots) {
                                editingId = null
                                name = ""; days = "1,2,3,4,5"; level = "ALL"; slots = emptyList()
                            }
                        } else {
                            onCreate(name, days, level, slots) {
                                composerOpen = false
                                name = ""; days = "1,2,3,4,5"; level = "ALL"; slots = emptyList()
                            }
                        }
                    },
                    onCancel = {
                        composerOpen = false
                        editingId = null
                        name = ""; days = "1,2,3,4,5"; level = "ALL"; slots = emptyList()
                        onClearMessages()
                    },
                    infoMessage = state.infoMessage,
                )
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
                        onEdit = {
                            editingId = config.id
                            name = config.name
                            days = config.applicableDays
                            level = config.classLevel
                            slots = config.slots.map { s -> s.copy() }
                            composerOpen = false
                        },
                        isSaving = state.isSaving,
                    )
                }
            }

            val info = state.infoMessage
            if (info != null && !showForm) {
                Text(info, style = VTheme.type.caption.colored(c.successInk))
            }
        }
    }
}

@Composable
private fun ConfigCard(
    config: SchoolDayConfigDto,
    onDeactivate: (String) -> Unit,
    onEdit: () -> Unit,
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

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "Edit",
                    onClick = onEdit,
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                    enabled = !isSaving,
                )
            }
            if (config.isActive) {
                Box(Modifier.weight(1f)) {
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

@Composable
private fun ConfigFormCard(
    title: String,
    name: String, onNameChange: (String) -> Unit,
    days: String, onDaysChange: (String) -> Unit, daysValid: Boolean,
    level: String, onLevelChange: (String) -> Unit, levelValid: Boolean,
    slots: List<SchoolDaySlotDto>, onSlotsChange: (List<SchoolDaySlotDto>) -> Unit,
    isSaving: Boolean,
    formValid: Boolean,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    infoMessage: String?,
) {
    val c = VTheme.colors
    VCard {
        Text(title, style = VTheme.type.h3.colored(c.ink))
        Spacer(Modifier.height(12.dp))
        VInput(value = name, onValueChange = onNameChange, label = "Name", placeholder = "e.g. Default Weekday")
        Spacer(Modifier.height(8.dp))
        VInput(value = days, onValueChange = onDaysChange, label = "Applicable Days", placeholder = "1,2,3,4,5 (Mon-Fri)")
        if (!daysValid && days.isNotBlank()) {
            Text("Format: comma-separated 1-7", style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(8.dp))
        VInput(value = level, onValueChange = onLevelChange, label = "Class Level", placeholder = "ALL / PRIMARY / SECONDARY")
        if (!levelValid && level.isNotBlank()) {
            Text("Must be: ALL, PRIMARY, or SECONDARY", style = VTheme.type.caption.colored(c.dangerInk))
        }

        Spacer(Modifier.height(12.dp))
        Text("Slots (${slots.size})", style = VTheme.type.bodyStrong.colored(c.ink))
        if (slots.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            slots.forEachIndexed { idx, slot ->
                if (idx > 0) Spacer(Modifier.height(6.dp))
                SlotEditorRow(
                    slot = slot,
                    onLabelChange = { newLabel ->
                        onSlotsChange(slots.mapIndexed { i, s -> if (i == idx) s.copy(label = newLabel) else s })
                    },
                    onStartChange = { newStart ->
                        onSlotsChange(slots.mapIndexed { i, s -> if (i == idx) s.copy(startTime = newStart) else s })
                    },
                    onEndChange = { newEnd ->
                        onSlotsChange(slots.mapIndexed { i, s -> if (i == idx) s.copy(endTime = newEnd) else s })
                    },
                    onTypeChange = { newType ->
                        onSlotsChange(slots.mapIndexed { i, s -> if (i == idx) s.copy(slotType = newType) else s })
                    },
                    onRemove = {
                        onSlotsChange(slots.filterIndexed { i, _ -> i != idx })
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        VButton(
            text = "+ Add Slot",
            onClick = {
                val nextIndex = if (slots.isNotEmpty()) slots.maxOf { it.slotIndex } + 1 else 0
                onSlotsChange(slots + emptySlot(nextIndex))
            },
            full = true,
            variant = VButtonVariant.Secondary,
            tone = VButtonTone.Navy,
            enabled = !isSaving,
        )

        infoMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = VTheme.type.caption.colored(c.successInk))
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "Cancel",
                    onClick = onCancel,
                    full = true,
                    variant = VButtonVariant.Secondary,
                    tone = VButtonTone.Navy,
                )
            }
            Box(Modifier.weight(1f)) {
                VButton(
                    text = if (isSaving) "Saving…" else "Save",
                    onClick = onSubmit,
                    full = true,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                    loading = isSaving,
                    enabled = formValid,
                )
            }
        }
    }
}

@Composable
private fun SlotEditorRow(
    slot: SchoolDaySlotDto,
    onLabelChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val c = VTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.cream).padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#${slot.slotIndex}", style = VTheme.type.dataSm.colored(c.ink2))
            Text(slot.slotType, style = VTheme.type.label.colored(slotTypeColor(slot.slotType, c)))
            Box(Modifier.weight(1f)) {}
            VButton(
                text = "×",
                onClick = onRemove,
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Navy,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VInput(
                    value = slot.label,
                    onValueChange = onLabelChange,
                    label = "Label",
                    placeholder = "e.g. Period 1",
                )
            }
            Box(Modifier.weight(1f)) {
                VInput(
                    value = slot.startTime,
                    onValueChange = onStartChange,
                    label = "Start",
                    placeholder = "08:00",
                )
            }
            Box(Modifier.weight(1f)) {
                VInput(
                    value = slot.endTime,
                    onValueChange = onEndChange,
                    label = "End",
                    placeholder = "08:45",
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SLOT_TYPES.forEach { type ->
                Box(Modifier.weight(1f)) {
                    VButton(
                        text = type.take(3),
                        onClick = { onTypeChange(type) },
                        full = true,
                        variant = if (slot.slotType == type) VButtonVariant.Primary else VButtonVariant.Secondary,
                        tone = VButtonTone.Navy,
                    )
                }
            }
        }
    }
}
