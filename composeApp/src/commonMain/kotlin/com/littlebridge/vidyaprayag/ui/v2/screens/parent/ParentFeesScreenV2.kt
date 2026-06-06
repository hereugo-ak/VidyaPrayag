package com.littlebridge.vidyaprayag.ui.v2.screens.parent

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VChartDatum
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VDonut
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VLegendDot
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * ParentFeesScreenV2 — a pixel-faithful copy of `Parent.tsx → Fees`.
 *
 * Navy-gradient balance hero (₹ total due + "Pay now · Coming Soon"), a donut split of the fee
 * breakdown with a legend, the per-head breakdown list, and payment history — all from [MockV2].
 */
@Composable
fun ParentFeesScreenV2(
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    val total = MockV2.feeBreakdown.sumOf { it.amount }
    val paid = MockV2.feeHistory.sumOf { it.amount }
    val palette = listOf(c.tealDeep, c.navy, Color(0xFFE08A3C), Color(0xFFA87CF0), Color(0xFFC14A44))
    val donutData = MockV2.feeBreakdown.mapIndexed { i, f ->
        VChartDatum(label = f.head, value = f.amount.toFloat(), color = palette[i % palette.size])
    }
    val paidPct = ((paid.toFloat() / (paid + total).toFloat()) * 100f).toInt()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(d.sm + 4.dp),
    ) {
        Text("Fees", style = VTheme.type.h1.colored(c.ink))

        // ── Hero: balance + donut ──────────────────────────────────────────
        VCard(padding = 0.dp) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(c.navy, Color(0xFF3B3870))),
                    )
                    .padding(20.dp),
            ) {
                Column {
                    VLabel("Balance due", color = Color.White.copy(alpha = 0.7f))
                    Text(
                        "₹ ${formatInt(total)}",
                        style = VTheme.type.dataLg.colored(Color.White).copy(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFFFD4A3)))
                        Text("Due 11 Jun 2026 · 6 days remaining", style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.78f)))
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.teal)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pay now", style = VTheme.type.bodyStrong.colored(Color.White))
                            Text(" · Coming Soon", style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.8f)))
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                VDonut(
                    data = donutData,
                    size = 132.dp,
                    thickness = 14.dp,
                    center = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL", style = VTheme.type.label.colored(c.ink3))
                            Text("₹${total / 1000}k", style = VTheme.type.data.colored(c.navy).copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                            Text("$paidPct% paid YTD", style = VTheme.type.label.colored(c.ink3))
                        }
                    },
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    donutData.forEach { dt ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            VLegendDot(color = dt.color, label = dt.label)
                            Text("₹${oneDecimalK(dt.value.toInt())}k", style = VTheme.type.dataSm.colored(c.ink).copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // ── Breakdown ──────────────────────────────────────────────────────
        VLabel("Fee breakdown")
        MockV2.feeBreakdown.forEach { f ->
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(f.head, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("${f.status} • Due ${f.dueDate}", style = VTheme.type.caption.colored(c.ink2))
                    }
                    Text("₹ ${formatInt(f.amount)}", style = VTheme.type.data.colored(c.ink))
                }
            }
        }

        // ── History ────────────────────────────────────────────────────────
        VLabel("Payment history")
        MockV2.feeHistory.forEach { f ->
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(f.head, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("${f.date} • ${f.receipt}", style = VTheme.type.dataSm.colored(c.ink2))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹ ${formatInt(f.amount)}", style = VTheme.type.data.colored(c.ink))
                        Text("Receipt", style = VTheme.type.label.colored(c.tealDeep))
                    }
                }
            }
        }

        Text(
            "Have a question about your fees? Message the school →",
            style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** Group an integer with thousands commas (e.g. 12500 → "12,500"). */
private fun formatInt(v: Int): String {
    val s = v.toString()
    val sb = StringBuilder()
    val rev = s.reversed()
    for (i in rev.indices) {
        if (i > 0 && i % 3 == 0) sb.append(',')
        sb.append(rev[i])
    }
    return sb.reverse().toString()
}

/** Amount in thousands with one decimal (e.g. 8500 → "8.5"). */
private fun oneDecimalK(v: Int): String {
    val whole = v / 1000
    val frac = (v % 1000) / 100
    return "$whole.$frac"
}
