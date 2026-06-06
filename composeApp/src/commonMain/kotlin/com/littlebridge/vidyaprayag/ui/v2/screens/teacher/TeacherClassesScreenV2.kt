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
                        Text(MockV2.classDisplay(cls), style = VTheme.type.h3.colored(c.ink).copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        Text("Mathematics", style = VTheme.type.caption.colored(c.ink2))
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
        VBackHeader(title = "Class ${MockV2.classDisplay(id)}", onBack = onBack)
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
                            Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text("Roll ${s.roll}", style = VTheme.type.dataSm.colored(c.ink3))
                        }
                        Text("${s.attendance}%", style = VTheme.type.data.colored(c.ink))
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
