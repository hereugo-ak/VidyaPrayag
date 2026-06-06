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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherClassesScreenV2 — a pixel-faithful copy of `Teacher.tsx → MyClasses` + `ClassDetail`.
 *
 * A 2-column grid of class cards (students + today %), tapping one opens a back-headered class
 * detail (3-stat header + message button + roster). From [MockV2].
 */
@Composable
fun TeacherClassesScreenV2(
    onOpenClass: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val me = MockV2.teachers[1]
    var detail by remember { mutableStateOf<String?>(null) }

    detail?.let { id ->
        ClassDetail(id = id, onBack = { detail = null }, modifier = modifier)
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("My Classes", style = VTheme.type.h1.colored(c.ink))
        me.classes.chunked(2).forEach { rowCls ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCls.forEach { cls ->
                    VCard(modifier = Modifier.weight(1f), onClick = { detail = cls; onOpenClass(cls) }) {
                        // §6.3 React card title is `${c.slice(0,-1)}-${c.slice(-1)}` → "10-A" (NO spaces),
                        // unlike the ClassDetail header which uses " - " spacing (Teacher.tsx:269 vs :292).
                        Text(
                            cls.dropLast(1) + "-" + cls.takeLast(1),
                            style = VTheme.type.h3.colored(c.ink).copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        )
                        // §6.3 React: fontSize 11, color text-dark-2 (Teacher.tsx:270)
                        Text("Mathematics", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("32", style = VTheme.type.data.colored(c.ink).copy(fontSize = 18.sp))
                                Text("Students", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("92%", style = VTheme.type.data.colored(c.teal).copy(fontSize = 18.sp))
                                Text("Today", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
                            }
                        }
                    }
                }
                if (rowCls.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClassDetail(id: String, onBack: () -> Unit, modifier: Modifier) {
    val c = VTheme.colors
    Column(modifier.fillMaxSize()) {
        // §6.3 React: <VBackHeader … action={<button><Edit3 size={16}/></button>} /> (Teacher.tsx:292)
        VBackHeader(
            title = "Class ${MockV2.classDisplay(id)}",
            onBack = onBack,
            action = {
                Icon(VIcons.Edit3, contentDescription = "Edit", tint = c.ink, modifier = Modifier.size(16.dp))
            },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("32", "Students", Modifier.weight(1f))
                MiniStat("92%", "Today", Modifier.weight(1f))
                MiniStat("74", "UT2 avg", Modifier.weight(1f))
            }
            VButton(text = "Message class parents", onClick = {}, full = true, variant = VButtonVariant.Secondary)
            VCard {
                MockV2.students.filter { it.klass == id }.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VAvatar(name = s.name, size = 32.dp)
                        Column(Modifier.weight(1f)) {
                            // §6.3 React: name 13/600, "Roll N" mono 11/ink3 (Teacher.tsx:314-315)
                            Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp))
                            Text("Roll ${s.roll}", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 11.sp))
                        }
                        // §6.3 React: attendance% mono 13 (Teacher.tsx:317)
                        Text("${s.attendance}%", style = VTheme.type.data.colored(c.ink).copy(fontSize = 13.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = VTheme.type.data.colored(c.ink).copy(fontSize = 18.sp))
        Text(label, style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
    }
}
