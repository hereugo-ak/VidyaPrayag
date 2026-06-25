package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedDayUi
import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.formatClock12h
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay
import com.littlebridge.vidyaprayag.util.parseHourMinute

/**
 * TodayClassStrip — the loop's signature "Today Strip" (P2-T2): a horizontally
 * scrollable, pill-based timeline of the teacher's whole day at a glance. No other
 * Indian school app fronts the day this way (Design Spec PART 2 §SIGNATURE #1).
 *
 * Fully data-driven from the server-resolved [ResolvedDayUi] — period order, times,
 * room, subject and attendance status are all real; the active period is the
 * authoritative [ResolvedDayUi.nowIndex] (NOT the device clock), exactly as the
 * schedule card already trusts it. Past/active/future is derived from that index
 * with the real clock as a tiebreak for the pre/post-day window.
 *
 * Pill state colours (all via the `Enroll.*` bridge → VTheme; PrimaryIndigo maps to
 * the portal violet accent, no new colour):
 *   • Past   → `surfaceSubtle` fill, `textTertiary` ink
 *   • Active → `primary` fill, white ink, a soft accent glow drawn behind the pill
 *   • Future → `surfaceCard` fill, `surfaceSubtle` 1.dp border, `textPrimary` ink
 *
 * Tapping a pill expands an inline detail block BELOW the strip (AnimatedVisibility):
 * full class label, room, attendance taken/not-taken, and a pre-scoped
 * "Take attendance" CTA — never navigating away to find it.
 */
@Composable
fun TodayClassStrip(
    day: ResolvedDayUi,
    onTakeAttendance: (ResolvedPeriodUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = day.periods
    if (periods.isEmpty()) return

    // Active period: prefer the server's authoritative nowIndex; -1 means "not in a
    // class right now", in which case nothing is highlighted as active.
    val activeIndex = day.nowIndex
    val nowMin = nowMinutesOfDay()

    var expandedIndex by remember(day.date) { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = "TODAY'S SCHEDULE")
        Spacer(Modifier.height(Enroll.space.md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Enroll.space.sm),
        ) {
            itemsIndexed(
                items = periods,
                key = { index, p -> p.periodId ?: "p$index" },
            ) { index, period ->
                val state = periodState(index, activeIndex, period, nowMin)
                PeriodPill(
                    periodNumber = index + 1,
                    period = period,
                    state = state,
                    selected = expandedIndex == index,
                    onClick = { expandedIndex = if (expandedIndex == index) -1 else index },
                )
            }
        }

        // Inline expansion for the tapped period.
        val expanded = periods.getOrNull(expandedIndex)
        AnimatedVisibility(
            visible = expanded != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            if (expanded != null) {
                Spacer(Modifier.height(Enroll.space.md))
                PeriodDetail(period = expanded, onTakeAttendance = { onTakeAttendance(expanded) })
            }
        }
    }
}

/** Past / Active / Future — the three pill aesthetics from the spec. */
private enum class PillState { Past, Active, Future }

private fun periodState(
    index: Int,
    activeIndex: Int,
    period: ResolvedPeriodUi,
    nowMin: Int,
): PillState = when {
    activeIndex >= 0 && index == activeIndex -> PillState.Active
    // When there is a known active period, anything before it is past.
    activeIndex >= 0 && index < activeIndex -> PillState.Past
    activeIndex >= 0 -> PillState.Future
    // No active period (before/after the teaching day): fall back to the clock.
    else -> {
        val end = parseHourMinute(period.endTime)
        if (end != null && nowMin > end) PillState.Past else PillState.Future
    }
}

@Composable
private fun PeriodPill(
    periodNumber: Int,
    period: ResolvedPeriodUi,
    state: PillState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fill = when (state) {
        PillState.Past -> Enroll.colors.surfaceSubtle
        PillState.Active -> Enroll.colors.primary
        PillState.Future -> Enroll.colors.surfaceCard
    }
    val titleInk = when (state) {
        PillState.Past -> Enroll.colors.textTertiary
        PillState.Active -> Color.White
        PillState.Future -> Enroll.colors.textPrimary
    }
    val subInk = if (state == PillState.Active) Color.White.copy(alpha = 0.82f) else Enroll.colors.textSecondary
    val timeInk = if (state == PillState.Active) Color.White.copy(alpha = 0.70f) else Enroll.colors.textTertiary
    val glow = Enroll.colors.primary
    val ix = remember { MutableInteractionSource() }

    Box(
        Modifier
            // Active pill earns a soft accent glow drawn behind it (spec: indigo glow).
            .drawBehind {
                if (state == PillState.Active) {
                    drawRoundRect(
                        color = glow.copy(alpha = 0.22f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    )
                }
            }
            .clip(Enroll.shape.card)
            .background(fill)
            .let { base ->
                when {
                    selected -> base.border(2.dp, Enroll.colors.primary, Enroll.shape.card)
                    state == PillState.Future -> base.border(1.dp, Enroll.colors.surfaceSubtle, Enroll.shape.card)
                    else -> base
                }
            }
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.md)
            .widthIn(min = 96.dp),
    ) {
        Column {
            Text(
                text = "PERIOD $periodNumber",
                style = Enroll.type.labelCaps.colored(if (state == PillState.Active) Color.White.copy(alpha = 0.75f) else Enroll.colors.textTertiary),
            )
            Spacer(Modifier.height(Enroll.space.xs))
            Text(
                text = period.subject.ifBlank { "Class" },
                style = Enroll.type.labelBold.colored(titleInk),
                maxLines = 1,
            )
            Text(
                text = period.classLabel,
                style = Enroll.type.bodySmall.colored(subInk),
                maxLines = 1,
            )
            Spacer(Modifier.height(Enroll.space.xs))
            Text(
                text = "${clock(period.startTime)} – ${clock(period.endTime)}",
                style = Enroll.type.bodySmall.colored(timeInk),
                maxLines = 1,
            )
        }
    }
}

/** The inline detail revealed under the strip when a pill is tapped. */
@Composable
private fun PeriodDetail(period: ResolvedPeriodUi, onTakeAttendance: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Enroll.shape.card)
            .background(Enroll.colors.primarySoft)
            .padding(Enroll.space.lg),
    ) {
        Text(
            text = period.classLabel + "  ·  " + period.subject,
            style = Enroll.type.headingSmall.colored(Enroll.colors.textPrimary),
            maxLines = 1,
        )
        Spacer(Modifier.height(Enroll.space.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(VIcons.Calendar, contentDescription = null, tint = Enroll.colors.primaryDeep, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(Enroll.space.xs))
            Text(
                text = if (period.room.isBlank()) "Room not set" else "Room ${period.room}",
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
            )
        }
        Spacer(Modifier.height(Enroll.space.md))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Attendance status chip — semantic green when taken, amber when pending.
            val taken = period.attendanceMarked
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(Enroll.shape.pill)
                        .background(if (taken) Enroll.colors.statusPresent else Enroll.colors.statusLate),
                )
                Spacer(Modifier.width(Enroll.space.sm))
                Text(
                    text = if (taken) "Attendance taken" else "Attendance not taken",
                    style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
                )
            }
            // Pre-scoped CTA — straight to attendance for THIS period (P7-T2).
            val ix = remember { MutableInteractionSource() }
            Text(
                text = if (period.attendanceMarked) "Edit" else "Take attendance",
                style = Enroll.type.labelBold.colored(Color.White),
                modifier = Modifier
                    .clip(Enroll.shape.pill)
                    .background(Enroll.colors.primary)
                    .clickable(interactionSource = ix, indication = null, onClick = onTakeAttendance)
                    .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
            )
        }
    }
}

/** 24h "HH:mm" → friendly 12h label, falling back to the raw string. */
private fun clock(hm: String): String = parseHourMinute(hm)?.let { formatClock12h(it) } ?: hm
