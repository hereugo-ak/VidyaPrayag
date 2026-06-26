package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherConfirmDialog — a calm, on-brand confirm sheet used for the deliberate, irreversible
 * teacher actions (publish marks & notify parents, close homework, …). A white rounded card over a
 * navy scrim; the destructive intent is phrased plainly and the confirm names the action.
 */
@Composable
fun TeacherConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val c = VTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(c.navyDeep.copy(alpha = 0.42f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .padding(28.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .clip(RoundedCornerShape(24.dp))
                .background(c.card)
                .border(1.dp, c.hairline, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Column {
                Text(title, style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(8.dp))
                Text(body, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.5.sp))
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Md,
                    )
                    VButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        variant = if (destructive) VButtonVariant.Destructive else VButtonVariant.Primary,
                        tone = if (destructive) VButtonTone.Rose else VButtonTone.Lavender,
                        size = VButtonSize.Md,
                    )
                }
            }
        }
    }
}
