package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

private data class InsightChrome(val icon: ImageVector, val label: String, val tone: Color)

/**
 * AiReportCardPreview — a **label-free schematic teaser** for the parent "AI Report Card"
 * (a `VComingSoon` feature on the Report tab).
 *
 * RA-S11 (TIER 0, honesty / LAW 6): the previous implementation rendered **fabricated grades** for an
 * invented child named "Riya" (Mathematics A 88, Science A- 84, English B+ 78, Hindi A 91), a made-up
 * AI narrative paragraph, and invented "strengths / focus / study tips" — on a parent's real academics
 * screen. A parent could mistake the fiction for their child's actual results.
 *
 * The real published marks render on the **Marks** tab (`GET /api/v1/parent/child/{id}/marks`). This
 * preview is now purely illustrative chrome — an abstract narrative bar, a 2-col grade-card grid with
 * shimmer placeholders (no subjects, no grades, no scores) and three label-only insight chips — that
 * reads unmistakably as a "coming soon" preview. No invented academic data anywhere.
 */
@Composable
fun AiReportCardPreview(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val insights = listOf(
        InsightChrome(VIcons.Heart, "STRENGTHS", Color(0xFFA8E6CF).copy(alpha = 0.4f)),
        InsightChrome(VIcons.Target, "FOCUS AREAS", Color(0xFFFFD4A3).copy(alpha = 0.5f)),
        InsightChrome(VIcons.BookOpen, "STUDY TIPS", c.teal.copy(alpha = 0.16f)),
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Navy AI-narrative card — header + abstract text bars (no fabricated narrative).
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.navy).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                Text("AI NARRATIVE", style = VTheme.type.label.colored(Color.White.copy(alpha = 0.7f)).copy(fontSize = 10.sp, letterSpacing = 0.10.em, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.16f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.85f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.12f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.6f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.10f)))
        }
        // Grade grid (2 columns) — placeholder cards, no subjects/grades/scores.
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { GradePlaceholderCard(Modifier.weight(1f)) }
            }
        }
        // Insight cards — label only, no fabricated body copy.
        insights.forEach { b ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(b.tone).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(b.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(12.dp))
                    Text(b.label, style = VTheme.type.label.colored(c.ink).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em))
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.7f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(c.ink.copy(alpha = 0.08f)))
            }
        }
    }
}

@Composable
private fun GradePlaceholderCard(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(c.cream).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth(0.55f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.ink.copy(alpha = 0.10f)))
        Box(Modifier.size(width = 34.dp, height = 18.dp).clip(RoundedCornerShape(5.dp)).background(c.navy.copy(alpha = 0.12f)))
    }
}
