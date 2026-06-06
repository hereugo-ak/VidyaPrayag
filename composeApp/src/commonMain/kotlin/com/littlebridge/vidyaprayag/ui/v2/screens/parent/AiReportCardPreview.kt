package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

private data class Grade(val subj: String, val grade: String, val score: Int, val up: Boolean)
private data class Insight(val icon: ImageVector, val label: String, val body: String, val tone: Color)

/**
 * AiReportCardPreview — `mockups.tsx → AIReportCardPreview`. Preview slot for the Parent "AI Report
 * Card" VComingSoon: a navy AI-narrative card, a 2-col grade grid, and three pastel insight cards
 * (Strengths / Focus areas / Study tips).
 */
@Composable
fun AiReportCardPreview(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val grades = listOf(
        Grade("Mathematics", "A", 88, true),
        Grade("Science", "A-", 84, true),
        Grade("English", "B+", 78, false),
        Grade("Hindi", "A", 91, true),
    )
    val upInk = Color(0xFF155E3A)
    val insights = listOf(
        Insight(VIcons.Heart, "STRENGTHS", "Number sense · Independent reading · Lab discipline", Color(0xFFA8E6CF).copy(alpha = 0.4f)),
        Insight(VIcons.Target, "FOCUS AREAS", "English long-form writing · Punctuality in PE", Color(0xFFFFD4A3).copy(alpha = 0.5f)),
        Insight(VIcons.BookOpen, "STUDY TIPS", "15-min daily journaling · NCERT exemplar Q14–18 weekly", c.teal.copy(alpha = 0.16f)),
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Navy AI-narrative card.
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.navy).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                Text("AI NARRATIVE · TERM 2", style = VTheme.type.label.colored(Color.White.copy(alpha = 0.7f)).copy(fontSize = 10.sp, letterSpacing = 0.10.em, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "\"Riya had a focused term. Her Mathematics is the steadiest it has been all year, and she's now consistently scoring above class average in Hindi. English is holding — short reading sprints will help.\"",
                style = VTheme.type.caption.colored(Color.White).copy(fontSize = 13.sp, lineHeight = 20.sp),
            )
        }
        // Grade grid (2 columns).
        grades.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { g ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.cream).padding(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(g.subj, style = VTheme.type.label.colored(c.ink2).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                            Text(if (g.up) "↑" else "→", style = VTheme.type.caption.colored(if (g.up) upInk else c.ink3).copy(fontSize = 11.sp))
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                            Text(g.grade, style = VTheme.type.h2.colored(c.navy).copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                            Text(g.score.toString(), style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 11.sp))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        // Insight cards.
        insights.forEach { b ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(b.tone).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(b.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(12.dp))
                    Text(b.label, style = VTheme.type.label.colored(c.ink).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                }
                Text(b.body, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 18.sp), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
