package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherProfileStat — one data column in the profile stats strip (Loop task P6-T2).
 *
 * A pure value/label pair: the big tabular-number value over its small caps label.
 * Kept as a stable UI model (not a VM type) so the host can map any session /
 * analytics source into it. `onClick` is optional — populated only for the
 * deep-linked stats (Classes Taught → Gradebook, Total Students → student list)
 * wired in P7-T5; the non-navigating stats (Subjects, Attendance %) leave it null.
 */
data class TeacherProfileStat(
    val value: String,
    val label: String,
    val onClick: (() -> Unit)? = null,
)

/**
 * TeacherStatsRow — the 4-up profile stats card (Loop task P6-T2).
 *
 * Sits directly under [TeacherProfileHeader]. Per the spec: a single [EnrollCard]
 * containing a `Row` of four [StatColumn]s separated by 1dp hairline dividers, each
 * column showing a `DataLarge` value (TextPrimary, tabular figures) above a
 * `LabelCaps` label (TextSecondary). Canonical order — Classes Taught | Total
 * Students | Subjects | Attendance % — but the row simply renders whatever four
 * stats the caller passes, so the host stays the single source of truth.
 *
 * All colour / typography resolves through `Enroll.*` → VTheme (no new hex). The
 * dividers use `Enroll.colors.surfaceSubtle` per the spec; columns weight equally
 * so the card reads as a balanced ledger regardless of value width.
 *
 * Deep-linkable: each column is tappable only when its stat carries an `onClick`
 * (P7-T5), so unlinked stats stay inert and give no false affordance.
 */
@Composable
fun TeacherStatsRow(
    stats: List<TeacherProfileStat>,
    modifier: Modifier = Modifier,
) {
    EnrollCard(modifier = modifier.fillMaxWidth(), padding = Enroll.space.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            stats.forEachIndexed { index, stat ->
                StatColumn(
                    stat = stat,
                    modifier = Modifier.weight(1f),
                )
                // 1dp hairline between columns (never after the last one).
                if (index < stats.lastIndex) {
                    StatDivider()
                }
            }
        }
    }
}

/** A single stat: big value over a caps label, optionally tappable for a deep link. */
@Composable
private fun StatColumn(
    stat: TeacherProfileStat,
    modifier: Modifier = Modifier,
) {
    val onClick = stat.onClick
    val interaction = remember { MutableInteractionSource() }
    val colMod = if (onClick != null) {
        modifier
            .clip(Enroll.shape.chip)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .semantics { contentDescription = "${stat.label}: ${stat.value}" }
            .padding(horizontal = Enroll.space.xs, vertical = Enroll.space.xs)
    } else {
        modifier.padding(horizontal = Enroll.space.xs, vertical = Enroll.space.xs)
    }

    Column(
        modifier = colMod,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stat.value,
            style = Enroll.type.dataLarge.colored(Enroll.colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Enroll.space.xs))
        Text(
            text = stat.label,
            style = Enroll.type.labelCaps.colored(Enroll.colors.textSecondary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** The 1dp vertical hairline standing between two stat columns. */
@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Enroll.colors.surfaceSubtle),
    )
}
