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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.ui.v2.theme.shapeInput

private val HOURS = (0..23).map { it.toString().padStart(2, '0') }
private val MINUTES = listOf("00", "15", "30", "45")

private fun formatClock12h(hour: Int, minute: String): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$h12:$minute $period"
}

@Composable
fun VTimePicker(
    hour: Int,
    minute: String,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val c = VTheme.colors

    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                style = VTheme.type.inputLabel.colored(c.ink2),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                VIcons.Clock,
                contentDescription = null,
                tint = if (enabled) c.tealDeep else c.ink3,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(4.dp))
            DropdownField(
                value = hour.toString().padStart(2, '0'),
                options = HOURS,
                onSelect = { onHourChange(it.toInt()) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            Text(":", style = VTheme.type.h3.colored(c.ink2))
            DropdownField(
                value = minute,
                options = MINUTES,
                onSelect = onMinuteChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                formatClock12h(hour, minute),
                style = VTheme.type.body.colored(c.ink3),
            )
        }
    }
}

@Composable
private fun DropdownField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = VTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(VTheme.dimens.shapeInput)
                .background(c.cream)
                .border(1.dp, c.hairline, VTheme.dimens.shapeInput)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = VTheme.type.body.colored(if (enabled) c.ink else c.ink3),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, style = VTheme.type.body.colored(c.ink)) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}
