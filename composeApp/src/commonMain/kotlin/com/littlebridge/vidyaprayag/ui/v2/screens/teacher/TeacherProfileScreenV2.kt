package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherProfileScreenV2 — a pixel-faithful copy of `Teacher.tsx → Profile`.
 *
 * Centered avatar + name + username + subject badges, a stack of setting rows, and a log-out button.
 */
@Composable
fun TeacherProfileScreenV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val me = MockV2.teachers[1]

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            VAvatar(name = me.name, size = 88.dp)
            Text(me.name, style = VTheme.type.h2.colored(c.ink), modifier = Modifier.padding(top = 12.dp))
            Text(me.username, style = VTheme.type.dataSm.colored(c.ink2))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                me.subjects.forEach { VBadge(text = it, tone = VBadgeTone.Arctic) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = listOf(
                "Personal details" to "Mobile, email, photo",
                "Notification preferences" to "Push, WhatsApp, quiet hours",
                "Change password" to "Last changed 26 days ago",
                "Help & support" to "Contact VidyaSetu",
            )
            rows.forEach { (title, sub) ->
                VCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(sub, style = VTheme.type.caption.colored(c.ink2))
                        }
                        Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
                    }
                }
            }
            VButton(text = "Log out", onClick = onLogout, full = true, variant = VButtonVariant.Ghost)
        }
    }
}
