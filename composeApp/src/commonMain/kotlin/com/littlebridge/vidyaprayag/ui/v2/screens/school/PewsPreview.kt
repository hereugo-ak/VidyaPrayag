package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * PewsPreview — a pixel-faithful copy of `mockups.tsx → PEWSPreview`.
 *
 * The rich preview body shown inside the AdminHome "PEWS — Predictive Early Warning" [VComingSoon]
 * (Admin.tsx:168). Renders: a "RISK BAND • TODAY" card with a 3-tile Low/Watch/High grid, a
 * "HIGHEST PRIORITY" at-risk list (per-student score chip color-coded by threshold), and a dashed
 * "SUGGESTED INTERVENTION" box. All colors are the exact React literals.
 */
@Composable
fun PewsPreview(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val atRisk = listOf(
        Triple("Aman Verma", "9-B", 78) to "Attendance ↓ 22%, Maths −18 pts",
        Triple("Ishita Roy", "10-A", 64) to "Late submissions, mood dip flagged",
        Triple("Karan Singh", "9-A", 52) to "Fee overdue, syllabus gap",
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Risk band card ───────────────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cream).padding(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // 11 / 700 / ink-3 / 0.08em
                Text(
                    "RISK BAND • TODAY",
                    style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.em),
                )
                Text("180 students scored", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RiskTile("Low", 142, Color(0xFFA8E6CF), Color(0xFF155E3A), Modifier.weight(1f))
                RiskTile("Watch", 27, Color(0xFFFFD4A3), Color(0xFF7A3F00), Modifier.weight(1f))
                RiskTile("High", 11, Color(0xFFFFADA8), Color(0xFF7A1C18), Modifier.weight(1f))
            }
        }

        // ── Highest priority ──────────────────────────────────────────────────────
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(VIcons.AlertTriangle, contentDescription = null, tint = Color(0xFF7A1C18), modifier = Modifier.size(13.dp))
                Text(
                    "HIGHEST PRIORITY",
                    style = VTheme.type.label.colored(c.ink2).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.04.em),
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                atRisk.forEach { (who, drivers) ->
                    val (name, cls, score) = who
                    // § §mockups score chip: >70 rose / >55 peach / else #FFE7B0; fg >70 #7a1c18 else #7a3f00.
                    val chipBg = when {
                        score > 70 -> Color(0xFFFFADA8)
                        score > 55 -> Color(0xFFFFD4A3)
                        else -> Color(0xFFFFE7B0)
                    }
                    val chipFg = if (score > 70) Color(0xFF7A1C18) else Color(0xFF7A3F00)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.cream).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.width(40.dp).clip(RoundedCornerShape(6.dp)).background(chipBg).padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(score.toString(), style = VTheme.type.dataSm.colored(chipFg).copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
                                    withStyle(SpanStyle(color = c.ink3, fontWeight = FontWeight.Medium)) { append(" · $cls") }
                                },
                                style = VTheme.type.caption.colored(c.ink).copy(fontSize = 12.sp),
                            )
                            Text(drivers, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp), maxLines = 1)
                        }
                    }
                }
            }
        }

        // ── Suggested intervention (dashed teal box) ───────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.teal.copy(alpha = 0.10f))
                .dashedBorder(c.tealDeep.copy(alpha = 0.30f), 1.dp, 10.dp)
                .padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(11.dp))
                Text(
                    "SUGGESTED INTERVENTION",
                    style = VTheme.type.label.colored(c.tealDeep).copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Schedule a parent call for the 3 high-risk students this week. Recommend remedial Maths slot Tue/Thu.",
                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 18.sp),
            )
        }
    }
}

@Composable
private fun RiskTile(label: String, n: Int, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(n.toString(), style = VTheme.type.dataLg.colored(fg).copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
        Text(label, style = VTheme.type.caption.colored(fg).copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
    }
}

/** 1px dashed rounded border (the React `border: 1px dashed rgba(0,106,96,0.3)` style). */
private fun Modifier.dashedBorder(color: Color, strokeWidth: Dp, cornerRadius: Dp): Modifier =
    this.drawBehind {
        val sw = strokeWidth.toPx()
        val r = cornerRadius.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(sw * 3f, sw * 3f), 0f)
        drawRoundRect(
            color = color,
            topLeft = Offset(sw / 2f, sw / 2f),
            size = Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = sw, pathEffect = dash),
        )
    }
