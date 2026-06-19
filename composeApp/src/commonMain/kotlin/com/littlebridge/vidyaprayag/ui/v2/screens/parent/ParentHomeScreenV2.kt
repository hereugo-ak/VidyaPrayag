package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.domain.model.DashboardAlertDto
import com.littlebridge.vidyaprayag.feature.parent.domain.model.DashboardChildSummary
import com.littlebridge.vidyaprayag.feature.parent.presentation.AttendanceDayState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * ParentHomeScreenV2 — the rebuilt parent dashboard, a premium Compose adaptation of the
 * website reference (`PhoneMockup.tsx`). The website is a React/Tailwind marketing mockup; this
 * is a native KMP app, so the design language is *adapted*, not copied verbatim — same lavender
 * canvas, navy ink, violet accent, the aurora wash and the layered card stack, rendered with
 * native gestures, springs and Canvas craft.
 *
 * DESIGN LAW — NEVER COLLAPSE TO WHITE SPACE. Every card renders a rich, intentional, premium
 * state even when the backend returns sparse/empty data (the demo child has empty marks /
 * timetable). The screen is always full and composed: an aurora-washed hero with the child
 * identity + a live journey ring, a school-day timeline rail, then the live feature cards. No
 * card "returns" early; sparse data becomes a polished, on-brand empty state.
 *
 * NO FLOATING TOASTS (LAW): the reference mockup decorates itself with floating glass plates
 * ("Marked present", "Maths result published"). Those are presentation flourishes and are
 * intentionally surfaced natively inside the cards / the bell — never as transient overlays.
 */
@Composable
fun ParentHomeScreenV2(
    modifier: Modifier = Modifier,
    onDiscoverSchools: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenFees: () -> Unit = {},
    onOpenAcademics: () -> Unit = {},
    viewModel: ParentDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    // Live clock — re-derive the time-aware greeting + period/end-of-day fields each minute.
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
        onOpenAcademics = onOpenAcademics,
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
    onOpenAcademics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val d = VTheme.dimens

    var coveredDetailOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = coveredDetailOpen) { coveredDetailOpen = false }

    Box(
        modifier
            .fillMaxSize()
            // Aurora-washed lavender canvas (adapts the reference's radial wash). Soft violet
            // bloom top-left, a cooler tint bottom-right, over the lavender base.
            .background(c.lavender)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accent.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.04f),
                        radius = size.width * 0.9f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accentSoft.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.22f),
                        radius = size.width * 0.8f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                // CRITICAL LAYOUT FIX: VStateHost runs its content through an AnimatedContent,
                // whose internal layout is Box-like — it STACKS its children. The dashboard emits
                // ~8 sibling cards, so without an explicit Column they all piled on top of one
                // another at y=0 (the "Fees over Results over Attendance" overlap bug). Wrapping
                // them in a single Column makes AnimatedContent host exactly one child that lays
                // the cards out vertically, with the same 14dp rhythm as the outer scroll column.
                val child = state.selectedChild
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ── Top bar: eyebrow + chat/bell actions ─────────────────────────
                    TopBar(onOpenNotifications = onOpenNotifications)

                    // ── Hero: identity + live journey ring (always rich) ─────────────
                    HeroJourneyCard(
                        child = child,
                        className = state.timetable?.className.orEmpty(),
                        todayState = state.today.state,
                        statusLabel = state.today.label,
                        contextLine = contextLineFor(state),
                    )

                    // ── Child switcher (only when 2+ children are linked) ────────────
                    if (state.children.size > 1) {
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

                    // ── Alert strip (real /dashboard alerts — populated for the demo) ─
                    if (state.alerts.isNotEmpty()) {
                        AlertStrip(alerts = state.alerts)
                    }

                    // ── Attendance card (primary feature) ────────────────────────────
                    ParentAttendanceCard(
                        today = state.today,
                        attendance = state.attendance,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // ── Today's schedule card (live) ─────────────────────────────────
                    ParentScheduleCard(
                        todayPeriods = state.todayPeriods,
                        timetable = state.timetable,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // ── Covered today card (live + end-of-day summary) ───────────────
                    ParentCoveredCard(
                        coveredToday = state.coveredToday,
                        schoolDayEnded = state.schoolDayEnded,
                        onOpenDetail = { coveredDetailOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // ── Academics: latest published result + sparkline ───────────────
                    ParentResultsCard(
                        latestMark = state.latestMark,
                        previousMark = state.previousMarkForSubject,
                        trend = state.markTrend,
                        onOpenAcademics = onOpenAcademics,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // ── Fees ─────────────────────────────────────────────────────────
                    ParentFeesCard(
                        fees = state.fees,
                        onOpenFees = onOpenFees,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(2.dp))
                }
            }
        }

        ParentCoveredDetailOverlay(
            visible = coveredDetailOpen,
            coveredToday = state.coveredToday,
            syllabus = state.syllabus,
            schoolDayEnded = state.schoolDayEnded,
            onDismiss = { coveredDetailOpen = false },
        )
    }
}

/** Top bar — the portal eyebrow on the left, a chat + a bell action on the right. */
@Composable
private fun TopBar(onOpenNotifications: () -> Unit) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "ENROLL+ · PARENTS PORTAL",
                style = VTheme.type.label.colored(c.accentDeep).copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    fontSize = 10.sp,
                ),
            )
        }
        CircleAction(VIcons.Bell, onOpenNotifications)
    }
}

@Composable
private fun CircleAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(c.card)
            .border(1.dp, c.hairline, CircleShape)
            .clickable(interactionSource = ix, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.navy, modifier = Modifier.size(16.dp))
    }
}

/**
 * The hero — a premium gradient panel adapting the reference's identity block. Carries the
 * child's avatar, name + class, a time-aware context line, and a live **journey ring** built
 * from the real `overall_progress`. This card always renders rich content (the journey figure
 * is always present for a linked child), so the dashboard never opens on emptiness.
 */
@Composable
private fun HeroJourneyCard(
    child: DashboardChildSummary?,
    className: String,
    todayState: AttendanceDayState,
    statusLabel: String,
    contextLine: String,
) {
    val c = VTheme.colors
    val name = child?.name?.ifBlank { "Your child" } ?: "Your child"
    val level = child?.currentLevel ?: 0
    val rawProgress = child?.overallProgress ?: 0.0
    val progressPct = (if (rawProgress <= 1.0) rawProgress * 100.0 else rawProgress).roundToInt().coerceIn(0, 100)

    val statusTone = statusToneFor(todayState)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(c.accent, c.accentDeep)))
            // soft top sheen so the panel reads glossy, not flat
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                        endY = size.height * 0.55f,
                    ),
                )
                // faint orbit ring decoration, bottom-right
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.92f, size.height * 0.9f),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                VAvatar(name = name, src = child?.profilePic, size = 56.dp, ring = true)
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = VTheme.type.h2.colored(Color.White).copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                        maxLines = 1,
                    )
                    val sub = listOfNotNull(
                        "Level $level".takeIf { level > 0 },
                        className.takeIf { it.isNotBlank() },
                    ).joinToString("  ·  ")
                    if (sub.isNotBlank()) {
                        Text(sub, style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.85f)).copy(fontSize = 12.sp))
                    }
                }
                // status chip — truthful, reflects resolved today-state
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        statusLabel.ifBlank { statusTone.word },
                        style = VTheme.type.label.colored(Color.White).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Journey row: a real progress ring + a contextual line.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                JourneyRing(percent = progressPct, modifier = Modifier.size(72.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Learning journey",
                        style = VTheme.type.label.colored(Color.White.copy(alpha = 0.8f)).copy(
                            fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.8.sp,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "$progressPct% of the way to the next level",
                        style = VTheme.type.bodyStrong.colored(Color.White).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                    if (contextLine.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            contextLine,
                            style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.85f)).copy(fontSize = 11.sp),
                        )
                    }
                }
            }
        }
    }
}

/** White-on-gradient progress ring with the percent centred — the hero's signature element. */
@Composable
private fun JourneyRing(percent: Int, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState(targetValue = percent / 100f, label = "journeySweep")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White)),
                startAngle = -90f, sweepAngle = 360f * sweep, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$percent%",
            style = VTheme.type.dataLg.colored(Color.White).copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
        )
    }
}

/** A compact strip of the real dashboard alerts (e.g. "Upcoming PTM · Nov 25"). */
@Composable
private fun AlertStrip(alerts: List<DashboardAlertDto>) {
    val c = VTheme.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(alerts, key = { it.id }) { a ->
            val (bg, fg, icon) = when (a.type.uppercase()) {
                "CRITICAL" -> Triple(c.danger.copy(alpha = 0.5f), c.dangerInk, VIcons.AlertCircle)
                "WARNING" -> Triple(c.warning.copy(alpha = 0.55f), c.warningInk, VIcons.AlertTriangle)
                else -> Triple(c.accent.copy(alpha = 0.12f), c.accentDeep, VIcons.Bell)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.card)
                    .border(1.dp, c.hairline, RoundedCornerShape(999.dp))
                    .padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
                }
                Column {
                    Text(a.title, style = VTheme.type.label.colored(c.navyDeep).copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp))
                    if (a.value.isNotBlank()) {
                        Text(a.value, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
                    }
                }
            }
        }
    }
}

private data class StatusTone(val word: String)

private fun statusToneFor(state: AttendanceDayState): StatusTone = when (state) {
    AttendanceDayState.Present -> StatusTone("Present")
    AttendanceDayState.Late -> StatusTone("Late")
    AttendanceDayState.Absent -> StatusTone("Absent")
    AttendanceDayState.Holiday -> StatusTone("Holiday")
    AttendanceDayState.Vacation -> StatusTone("Break")
    AttendanceDayState.Sunday -> StatusTone("Sunday")
    AttendanceDayState.NoData -> StatusTone("Today")
}

/** Build the time-aware contextual line referencing the selected child. */
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

/** A child selector chip used when a parent has 2+ linked children. */
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
