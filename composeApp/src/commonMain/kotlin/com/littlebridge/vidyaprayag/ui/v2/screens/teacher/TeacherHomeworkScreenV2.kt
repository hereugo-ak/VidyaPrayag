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
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherHomeworkScreenV2 — a pixel-faithful copy of `Teacher.tsx → HomeworkFlow`.
 *
 * Two homework cards (with submission progress / "just assigned" badges) and an "Assign new
 * homework" button.
 */
@Composable
fun TeacherHomeworkScreenV2(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Algebra worksheet", style = VTheme.type.bodyStrong.colored(c.ink))
                    // §6.2 React subline fontSize 11 / text-dark-2 (Teacher.tsx:240)
                    Text("Mathematics • Class 10-A • Due 6 Jun", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                }
                VBadge(text = "28 / 32 submitted", tone = VBadgeTone.Warning)
            }
            Spacer(Modifier.height(8.dp))
            VProgressBar(value = 88f)
        }
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Reading — Ch 4 The Postmaster", style = VTheme.type.bodyStrong.colored(c.ink))
                    // §6.2 React subline fontSize 11 / text-dark-2 (Teacher.tsx:250)
                    Text("English • Class 9-A • Due 7 Jun", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                }
                VBadge(text = "Just assigned", tone = VBadgeTone.Arctic)
            }
        }
        VButton(text = "Assign new homework", onClick = {}, full = true, size = VButtonSize.Lg, tone = VButtonTone.Lavender, leading = { Icon(VIcons.Plus, null, modifier = Modifier.size(16.dp)) })
    }
}
