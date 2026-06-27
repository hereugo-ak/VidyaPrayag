package com.littlebridge.enrollplus.ui.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.VThemeDef
import com.littlebridge.enrollplus.ui.v2.theme.VThemeRegistry
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * VThemePicker — shared theme selection component.
 *
 * Iterates [VThemeRegistry.allThemes] dynamically, so new themes appear
 * automatically without any changes to this component.
 *
 * Shows a "System" option (follows OS dark/light) followed by one card per
 * registered theme. The active selection is highlighted with the accent colour.
 *
 * @param currentMode The current [VThemeMode] storage value ("system"/"light"/"dark"/"custom").
 * @param currentCustomId The current custom theme id (used when mode == "custom").
 * @param onSelect Called with (mode, customId) when the user picks a theme.
 *   - "system" → (mode="system", customId=null)
 *   - A theme that matches "light" or "dark" → (mode=thatId, customId=null)
 *   - Any other theme → (mode="custom", customId=themeId)
 */
@Composable
fun VThemePicker(
    currentMode: String,
    currentCustomId: String?,
    onSelect: (mode: String, customId: String?) -> Unit,
) {
    val c = VTheme.colors

    Column {
        Text("Appearance", style = VTheme.type.h3.colored(c.navyDeep))
        Spacer(Modifier.height(10.dp))

        // ── System option ──────────────────────────────────────────────────────
        ThemePickerRow(
            icon = VIcons.Settings,
            label = "System",
            caption = "Follow your device setting",
            active = currentMode == "system",
            onClick = { onSelect("system", null) },
        )

        Spacer(Modifier.height(8.dp))

        // ── Registered themes ───────────────────────────────────────────────────
        VThemeRegistry.allThemes.forEachIndexed { index, def ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            val active = when (def.id) {
                "light" -> currentMode == "light"
                "dark" -> currentMode == "dark"
                else -> currentMode == "custom" && currentCustomId == def.id
            }
            ThemePickerRow(
                icon = def.icon,
                label = def.displayName,
                caption = def.description,
                active = active,
                onClick = {
                    when (def.id) {
                        "light" -> onSelect("light", null)
                        "dark" -> onSelect("dark", null)
                        else -> onSelect("custom", def.id)
                    }
                },
            )
        }
    }
}

@Composable
private fun ThemePickerRow(
    icon: ImageVector,
    label: String,
    caption: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) c.accentTint else c.cream)
            .border(1.dp, if (active) c.accent.copy(alpha = 0.35f) else c.hairline, RoundedCornerShape(14.dp))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) c.accent.copy(alpha = 0.15f) else c.card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) c.accentDeep else c.ink2,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = VTheme.type.bodyStrong.colored(if (active) c.accentDeep else c.navyDeep))
            Text(caption, style = VTheme.type.caption.colored(c.ink3))
        }
        if (active) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(c.accentDeep),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Check, contentDescription = "Selected", tint = c.card, modifier = Modifier.size(14.dp))
            }
        }
    }
}
