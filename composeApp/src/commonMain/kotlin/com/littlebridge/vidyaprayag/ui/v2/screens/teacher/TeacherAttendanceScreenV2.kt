package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherAttendanceScreenV2 — a pixel-faithful copy of `Teacher.tsx → AttendanceFlow`.
 *
 * Class/date selectors, "Mark all present", per-student P/A/L pill row, and a sticky submit summary.
 */
@Composable
fun TeacherAttendanceScreenV2(
    classId: String = "",
    date: String = "",
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VInput(value = "Class 10-A", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
            VInput(value = "Today, 5 Jun", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false)
        }
        VButton(text = "Mark all present", onClick = {}, full = true, variant = VButtonVariant.Secondary)
        VCard {
            MockV2.students.filter { it.klass == "10A" }.take(6).forEachIndexed { i, s ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VAvatar(name = s.name, size = 32.dp)
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("Roll ${s.roll}", style = VTheme.type.dataSm.colored(c.ink3))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PalPill("P", s.today == MockV2.Presence.Present, c.successInk)
                        PalPill("A", s.today == MockV2.Presence.Absent, c.dangerInk)
                        PalPill("L", s.today == MockV2.Presence.Late, c.warningInk)
                    }
                }
            }
        }
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("28 marked • 4 remaining", style = VTheme.type.caption.colored(c.ink2))
                    Spacer(Modifier.height(4.dp))
                    VProgressBar(value = 87f)
                }
                Spacer(Modifier.height(0.dp))
                VButton(text = "Submit", onClick = {}, size = VButtonSize.Lg, tone = VButtonTone.Lavender, stateful = true, successLabel = "Submitted")
            }
        }
    }
}

@Composable
private fun PalPill(letter: String, active: Boolean, tone: Color) {
    val c = VTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) tone else c.ink.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = VTheme.type.label.colored(if (active) Color.White else c.ink3).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified, fontWeight = FontWeight.SemiBold))
    }
}
