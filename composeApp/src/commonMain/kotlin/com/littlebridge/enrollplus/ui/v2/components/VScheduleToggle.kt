package com.littlebridge.enrollplus.ui.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.ui.v2.theme.shapeInput
import com.littlebridge.enrollplus.util.todayIso

data class ScheduleSelection(
    val isScheduled: Boolean,
    val dateIso: String,
    val hour: Int,
    val minute: String,
)

fun ScheduleSelection.toIso8601(): String? {
    if (!isScheduled) return null
    val h = hour.toString().padStart(2, '0')
    return "${dateIso}T${h}:${minute}:00Z"
}

@Composable
fun VScheduleToggle(
    selection: ScheduleSelection,
    onSelectionChange: (ScheduleSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    Column(modifier.fillMaxWidth()) {
        RadioRow(
            label = "Publish now",
            selected = !selection.isScheduled,
            onClick = { onSelectionChange(selection.copy(isScheduled = false)) },
        )
        Spacer(Modifier.size(8.dp))
        RadioRow(
            label = "Schedule for later",
            selected = selection.isScheduled,
            onClick = { onSelectionChange(selection.copy(isScheduled = true)) },
        )

        if (selection.isScheduled) {
            Spacer(Modifier.size(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(VTheme.dimens.shapeInput)
                    .background(c.cream)
                    .border(1.dp, c.hairline, VTheme.dimens.shapeInput)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VDatePicker(
                    value = selection.dateIso,
                    onValueChange = { onSelectionChange(selection.copy(dateIso = it)) },
                    modifier = Modifier.weight(1f),
                    label = "Date",
                )
                VTimePicker(
                    hour = selection.hour,
                    minute = selection.minute,
                    onHourChange = { onSelectionChange(selection.copy(hour = it)) },
                    onMinuteChange = { onSelectionChange(selection.copy(minute = it)) },
                    modifier = Modifier.weight(1f),
                    label = "Time",
                )
            }
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = VTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(VTheme.dimens.shapeInput)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) c.tealDeep else c.hairline, CircleShape)
                .background(if (selected) c.tealDeep else c.card),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape).background(c.card),
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = label,
            style = VTheme.type.body.colored(if (selected) c.ink else c.ink2),
        )
    }
}
