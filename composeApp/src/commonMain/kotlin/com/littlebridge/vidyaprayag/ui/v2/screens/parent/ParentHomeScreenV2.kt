package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.domain.model.DashboardChildSummary
import com.littlebridge.vidyaprayag.feature.parent.presentation.AttendanceDayState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentHomeScreenV2 — the rebuilt parent dashboard, a faithful Compose port of the
 * website reference (`website/.../parents/PhoneMockup.tsx`): a calm lavender canvas
 * with a greeting hero (portal eyebrow → bell → child identity), then a stack of
 * live feature cards (attendance, schedule, covered-today, results, fees).
 *
 * Wired to the new [ParentDashboardViewModel] — the single source of truth that
 * aggregates every real backend read scoped to the shared selected child
 * (RA-S05 [SelectedChildHolder]) and derives the live, clock-driven "today" state.
 *
 * NO FLOATING TOASTS (LAW): the website reference deliberately decorates its marketing
 * mockup with two floating glass plates ("Marked present", "Maths result published").
 * Those are presentation-only flourishes and are intentionally NOT reproduced here — the
 * app surfaces that same information natively, inside the attendance + results cards and
 * the Activity feed (the bell), never as transient floating popups/toasts overlaid on the
 * dashboard. Do not add Snackbar/Toast/floating-notification overlays to this surface.
 *
 * Commit 2 lays the shell: the top bar (child switcher), the time-aware greeting +
 * contextual line referencing the selected child, and the notification bell that
 * opens the Activity feed. The feature cards land in subsequent commits.
 */
@Composable
fun ParentHomeScreenV2(
    modifier: Modifier = Modifier,
    onDiscoverSchools: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenFees: () -> Unit = {},
    viewModel: ParentDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    // Live clock — re-derive the time-aware greeting + period/end-of-day fields each minute
    // so the dashboard reads live as the school day progresses (no manual refresh).
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            viewModel.refreshLiveClock()
        }
    }

    ParentDashboardContent(
        state = state,
        onRetry = viewModel::load,
        onSelectChild = viewModel::selectChild,
        onOpenNotifications = onOpenNotifications,
        onOpenFees = onOpenFees,
        modifier = modifier,
    )
}

/** Stateless body. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ParentDashboardContent(
    state: ParentDashboardState,
    onRetry: () -> Unit,
    onSelectChild: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenFees: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    // In-app "covered today" detail overlay state — component-scoped, NOT a separate screen.
    var coveredDetailOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = coveredDetailOpen) { coveredDetailOpen = false }

    Box(modifier.fillMaxSize().background(c.lavender)) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(d.sm),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.children.isEmpty(),
            emptyTitle = "No child linked yet",
            emptyBody = "Link your child to see their daily journey and progress.",
            onRetry = onRetry,
            skeleton = { com.littlebridge.vidyaprayag.ui.v2.screens.SkeletonDashboard() },
        ) {
            val child = state.selectedChild

            // ── Greeting hero ───────────────────────────────────────────────────
            GreetingHero(
                child = child,
                className = state.timetable?.className.orEmpty(),
                todayState = state.today.state,
                contextLine = contextLineFor(state),
                onOpenNotifications = onOpenNotifications,
            )

            // ── Child switcher (only when 2+ children are linked) ───────────────
            if (state.children.size > 1) {
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.children, key = { it.id }) { ch ->
                        ChildChip(
                            name = ch.name.ifBlank { "—" },
                            src = ch.profilePic,
                            selected = ch.id == state.selectedChildId,
                            onClick = { onSelectChild(ch.id) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Attendance card (primary feature) ───────────────────────────────
            ParentAttendanceCard(
                today = state.today,
                attendance = state.attendance,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Today's schedule card (live) ────────────────────────────────────
            ParentScheduleCard(
                todayPeriods = state.todayPeriods,
                timetable = state.timetable,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Covered today card (live + end-of-day summary) ──────────────────
            ParentCoveredCard(
                coveredToday = state.coveredToday,
                schoolDayEnded = state.schoolDayEnded,
                onOpenDetail = { coveredDetailOpen = true },
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Academics: latest published result + sparkline ──────────────────
            ParentResultsCard(
                latestMark = state.latestMark,
                previousMark = state.previousMarkForSubject,
                trend = state.markTrend,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Fees ────────────────────────────────────────────────────────────
            ParentFeesCard(
                fees = state.fees,
                onOpenFees = onOpenFees,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // In-app detail overlay (mounts above the dashboard, not a navigation push).
    ParentCoveredDetailOverlay(
        visible = coveredDetailOpen,
        coveredToday = state.coveredToday,
        syllabus = state.syllabus,
        schoolDayEnded = state.schoolDayEnded,
        onDismiss = { coveredDetailOpen = false },
    )
    }
}

/**
 * The greeting hero — mirrors the reference: a portal eyebrow + notification bell on the
 * top row, then the child's avatar (with a live status badge) + "[Name]'s day" + a real
 * class subline, and a time-aware contextual line referencing the selected child.
 */
@Composable
private fun GreetingHero(
    child: DashboardChildSummary?,
    className: String,
    todayState: AttendanceDayState,
    contextLine: String,
    onOpenNotifications: () -> Unit,
) {
    val c = VTheme.colors
    val name = child?.name?.ifBlank { "Your child" } ?: "Your child"
    // A truthful status dot — its colour reflects the real resolved today-state.
    val dotColor = when (todayState) {
        AttendanceDayState.Present -> c.successInk
        AttendanceDayState.Late -> c.warningInk
        AttendanceDayState.Absent -> c.dangerInk
        AttendanceDayState.Holiday, AttendanceDayState.Vacation, AttendanceDayState.Sunday -> c.accentDeep
        AttendanceDayState.NoData -> c.ink3
    }

    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        // eyebrow + bell
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "PARENTS PORTAL",
                style = VTheme.type.label.colored(c.accentDeep).copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    fontSize = 10.sp,
                ),
            )
            val bellInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.card)
                    .border(1.dp, c.hairline, CircleShape)
                    .clickable(interactionSource = bellInteraction, indication = null) { onOpenNotifications() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Bell, contentDescription = "Activity", tint = c.navy, modifier = Modifier.size(13.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                VAvatar(name = name, src = child?.profilePic, size = 44.dp, ring = true)
                // live status badge — present = success check, else a neutral dot
                VStatusDot(
                    color = dotColor,
                    size = 12.dp,
                    ring = true,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            Column {
                Text(
                    "${name}'s day",
                    style = VTheme.type.h3.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                )
                if (className.isNotBlank()) {
                    Text(className, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
                }
            }
        }

        if (contextLine.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(contextLine, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
        }
    }
}

/**
 * Build the time-aware contextual line referencing the selected child. Time-of-day word
 * is derived from the live device clock; the rest references the real resolved today-state
 * so the copy always tells the truth about the child's day.
 */
private fun contextLineFor(state: ParentDashboardState): String {
    val name = state.selectedChild?.name?.takeIf { it.isNotBlank() } ?: "your child"
    val firstName = name.substringBefore(' ')
    val partOfDay = when (nowMinutesOfDay() / 60) {
        in 0..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }
    val tail = when (state.today.state) {
        AttendanceDayState.Present -> "$firstName is marked present today."
        AttendanceDayState.Late -> "$firstName arrived late today."
        AttendanceDayState.Absent -> "$firstName is marked absent today."
        AttendanceDayState.Holiday -> "It's a holiday — ${state.today.label}."
        AttendanceDayState.Vacation -> "${state.today.label} — enjoy the break."
        AttendanceDayState.Sunday -> "It's Sunday — no school today."
        AttendanceDayState.NoData -> "Here's $firstName's day at a glance."
    }
    return "Good $partOfDay. $tail"
}

/**
 * A child selector chip used when a parent has 2+ linked children. Theme tokens only —
 * selected uses the lavender accent tint, idle uses the neutral cream surface.
 */
@Composable
private fun ChildChip(name: String, src: String?, selected: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val (bg, fg) = if (selected) c.accent.copy(alpha = 0.14f) to c.accentDeep else c.card to c.ink2
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, if (selected) c.accent.copy(alpha = 0.3f) else c.hairline, RoundedCornerShape(999.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VAvatar(name = name, src = src, size = 24.dp)
        Text(
            name,
            style = VTheme.type.label.colored(fg).copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
