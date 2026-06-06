package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.shapeMd

/**
 * VInput — a single-line text field with the design's focus treatment: surface lifts cream→card,
 * border turns teal-deep, and a soft 4dp teal glow ring appears.
 *
 * Translated from primitives.tsx → `VInput`. Built on [BasicTextField] for full multiplatform
 * control over the focus visuals (Material3 TextField's chrome doesn't match the design).
 */
@Composable
fun VInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderColor by animateColorAsState(if (focused) c.tealDeep else c.border1, tween(180), label = "border")
    val bg by animateColorAsState(if (focused) c.card else c.cream, tween(180), label = "bg")
    val glow by animateDpAsState(if (focused) 4.dp else 0.dp, tween(180), label = "glow")
    val iconTint by animateColorAsState(if (focused) c.tealDeep else c.ink3, tween(180), label = "iconTint")

    val shape = VTheme.dimens.shapeMd

    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                style = VTheme.type.caption.colored(c.ink2),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                // teal glow ring (drawn as an outer translucent border)
                .border(BorderStroke(glow, c.teal.copy(alpha = if (focused) 0.15f else 0f)), shape)
                .padding(glow)
                .clip(shape)
                .background(bg)
                .border(BorderStroke(1.dp, borderColor), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = VTheme.type.body.colored(c.placeholder))
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        textStyle = VTheme.type.body.colored(c.ink),
                        cursorBrush = SolidColor(c.tealDeep),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                        interactionSource = interaction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                style = VTheme.type.caption.colored(c.ink3),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
