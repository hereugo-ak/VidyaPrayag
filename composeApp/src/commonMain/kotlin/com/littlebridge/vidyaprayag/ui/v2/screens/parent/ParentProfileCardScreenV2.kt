package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VConfirmDialog
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * House — the deterministic, decorative collectible "team" a child belongs to.
 *
 * IMPORTANT: there is NO house column in the schema (Tables.kt → ChildrenTable has only
 * gender/currentGrade/interests/section). So this is a purely cosmetic treatment derived
 * *deterministically* from the child's stable id — the same child always lands in the same
 * house. No backend NUMBER/STATE is invented: every stat on the card (attendance %, score %,
 * level, topics) still comes from the real [ParentDashboardViewModel].
 */
enum class ProfileHouse(
    val title: String,
    val crest: String,
    val top: Color,
    val bottom: Color,
    val foil: Color,
    val onColor: Color,
) {
    Aether("Aether", "AE", Color(0xFF6C5CE0), Color(0xFF362F73), Color(0xFFB9AEFF), Color(0xFFFFFFFF)),
    Verdant("Verdant", "VR", Color(0xFF1FA89A), Color(0xFF064F49), Color(0xFF8CF0E2), Color(0xFFFFFFFF)),
    Ember("Ember", "EM", Color(0xFFE0734B), Color(0xFF7A2E18), Color(0xFFFFC4A6), Color(0xFFFFFFFF)),
    Solace("Solace", "SO", Color(0xFFD9A441), Color(0xFF7A551A), Color(0xFFFFE6A6), Color(0xFF26234D)),
    Indigo("Indigo", "IN", Color(0xFF2D63C8), Color(0xFF12305E), Color(0xFF9EC0FF), Color(0xFFFFFFFF)),
}

/** Stable house assignment — sum of the id's code points mod the house count. */
fun houseFor(childId: String): ProfileHouse {
    if (childId.isBlank()) return ProfileHouse.Aether
    val hash = childId.fold(0) { acc, ch -> (acc * 31 + ch.code) and 0x7FFFFFFF }
    val houses = ProfileHouse.entries
    return houses[hash % houses.size]
}

/**
 * ParentProfileCardScreenV2 — Phase 4 (commit 10): the flagship, full-screen, house-colored
 * **collectible player card** for the selected child.
 *
 * Real depth & motion (no cheap popups):
 *  - A parallax 3D tilt driven by a drag gesture (rotationX/Y), the card physically leaning
 *    toward the finger and springing back on release — like tilting a real foil card in the light.
 *  - A holographic sheen sweep (infinite) + a drag-reactive specular highlight whose centre
 *    tracks the tilt, so the gloss "catches the light" as you move it.
 *  - A house-colored foil border and crest, layered glass stat tiles.
 *
 * Every stat is real backend data from [ParentDashboardViewModel]; the house is the only
 * decorative (deterministic-from-id) element — see [ProfileHouse].
 *
 * Phase 4 (commit 11): a **swipe-down on the card** smoothly reveals the account-options panel
 * (personal details, linked children, discover schools, log out…). The reveal is a real,
 * drag-tracked transition — the card lifts + tilts + shrinks toward the top as an options sheet
 * rises from below — NOT a popup/dialog/toast.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ParentProfileCardScreenV2(
    onLogout: () -> Unit = {},
    onLinkChild: () -> Unit = {},
    onDiscoverSchools: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentDashboardViewModel = koinViewModel(),
    profileViewModel: ParentProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val profileState by profileViewModel.state.collectAsStateV2()
    val c = VTheme.colors

    // ── Swipe-down reveal rig ──────────────────────────────────────────────────
    // `revealed` is the settled rest state (false = card, true = options open).
    // `dragPx` is the in-flight signed finger travel (down = +, up = −) while a vertical drag is
    // active; `dragging` gates whether we track the finger 1:1 or spring to the settled state.
    //
    // CRITICAL FIX: the reveal gesture is now hosted ON THE CARD ITSELF (see [ProfilePlayerCard]'s
    // unified gesture classifier), not on a tiny top strip. A predominantly-VERTICAL drag anywhere
    // on the card drives this reveal; a small/lateral drag instead drives the 3D tilt. So "swipe the
    // card down → options" finally works the way the brief intends.
    var revealed by remember { mutableStateOf(false) }
    var dragPx by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val revealThresholdPx = 240f

    // §11 — when the options panel is open, system/predictive back collapses it back to the card
    // (instead of leaving the tab), matching the swipe-up gesture.
    BackHandler(enabled = revealed) { revealed = false; dragPx = 0f }

    val baseProgress = if (revealed) 1f else 0f
    // While dragging we map signed finger travel to progress *relative to the current rest*, so a
    // swipe-up from the revealed state closes, and a swipe-down from the card opens. Rubber-banding
    // past the ends keeps it from feeling like it hit a wall.
    val rawProgress = (baseProgress + dragPx / revealThresholdPx).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = if (dragging) rawProgress else baseProgress,
        // Premium settle: a soft spring (not a linear tween) so the surface "arrives" with weight.
        animationSpec = if (dragging) tween(durationMillis = 0)
        else spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "reveal-progress",
    )

    fun settle() {
        dragging = false
        // Velocity-free threshold settle with hysteresis: easier to open than to keep open.
        revealed = if (revealed) rawProgress > 0.35f else rawProgress >= 0.5f
        dragPx = 0f
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.navyDeep, c.navy))),
    ) {
        // The card — lifts up + scales down + fades slightly as options reveal.
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProfilePlayerCard(
                state = state,
                // Reveal is interactive only when collapsed OR mid-drag — once fully open the panel
                // owns the gestures (its own swipe-up + Back-to-card button close it).
                revealEnabled = !revealed || dragging,
                onRevealDragStart = { dragging = true },
                onRevealDragDelta = { delta -> dragPx = delta },
                onRevealDragEnd = { settle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = -size.height * 0.20f * progress
                        val s = 1f - 0.18f * progress
                        scaleX = s
                        scaleY = s
                        alpha = 1f - 0.30f * progress
                    },
            )

            // The grab-handle affordance + caption float just under the card while collapsed,
            // fading out as the panel takes over.
            ProfileRevealHint(
                progress = progress,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // The options panel — rises from the bottom as progress → 1. Swiping it back down/up or the
        // explicit "Back to card" button collapses it.
        ProfileOptionsPanel(
            parentName = profileState.profile?.name ?: "",
            parentContact = listOfNotNull(
                profileState.profile?.phone?.takeIf { it.isNotBlank() },
                profileState.profile?.email?.takeIf { it.isNotBlank() },
            ).joinToString("  ·  "),
            progress = progress,
            onCollapseDragStart = { dragging = true },
            onCollapseDragDelta = { delta -> dragPx = delta },
            onCollapseDragEnd = { settle() },
            onLinkChild = onLinkChild,
            onDiscoverSchools = onDiscoverSchools,
            onLogout = onLogout,
            onClose = { revealed = false; dragPx = 0f },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * The collectible card itself — tilt + holo + real stats + the swipe-down reveal gesture.
 *
 * Unified gesture model (the fix): a SINGLE [pointerInput] classifies each drag once, on its first
 * decisive movement:
 *   - predominantly VERTICAL  → drives the parent's reveal (card → account options)
 *   - otherwise (lateral/small)→ drives the foil 3D tilt (parallax lean), springing back on release
 * This removes the old conflict where a tilt detector swallowed every drag so the card could never
 * be swiped down.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProfilePlayerCard(
    state: ParentDashboardState,
    modifier: Modifier = Modifier,
    revealEnabled: Boolean = false,
    onRevealDragStart: () -> Unit = {},
    onRevealDragDelta: (Float) -> Unit = {},
    onRevealDragEnd: () -> Unit = {},
) {
    val child = state.selectedChild
    val house = houseFor(child?.id ?: "")

    // overallProgress arrives either as a 0..1 fraction or an already-scaled 0..100 percent
    // depending on the endpoint build — normalise to a whole-number percent for display.
    val rawProgress = child?.overallProgress ?: 0.0
    val progressPct = (if (rawProgress <= 1.0) rawProgress * 100.0 else rawProgress).roundToInt().coerceIn(0, 100)

    // ── Drag-driven tilt ──────────────────────────────────────────────────────
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var tilting by remember { mutableStateOf(false) }

    val tiltY by animateFloatAsState(
        targetValue = if (tilting) (dragX / 18f).coerceIn(-14f, 14f) else 0f,
        animationSpec = if (tilting) tween(durationMillis = 60)
        else spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "tiltY",
    )
    val tiltX by animateFloatAsState(
        targetValue = if (tilting) (-dragY / 18f).coerceIn(-14f, 14f) else 0f,
        animationSpec = if (tilting) tween(durationMillis = 60)
        else spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "tiltX",
    )

    // ── Infinite holographic sweep ─────────────────────────────────────────────
    val holo = rememberInfiniteTransition(label = "holo")
    val sweep by holo.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Box(
        modifier
            .aspectRatio(0.66f)
            .graphicsLayer {
                rotationX = tiltX
                rotationY = tiltY
                cameraDistance = 16f * density
            }
            // ── Unified drag classifier (reveal vs tilt) ────────────────────────
            .pointerInput(revealEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var mode = 0 // 0 = undecided, 1 = reveal (vertical), 2 = tilt
                    var accY = 0f
                    val touchSlop = viewConfiguration.touchSlop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dx = change.positionChange().x
                        val dy = change.positionChange().y
                        if (mode == 0) {
                            val totalX = change.position.x - down.position.x
                            val totalY = change.position.y - down.position.y
                            if (abs(totalX) > touchSlop || abs(totalY) > touchSlop) {
                                // Decide once: a clearly vertical intent (and reveal allowed) → reveal.
                                mode = if (revealEnabled && abs(totalY) > abs(totalX) * 1.1f) {
                                    onRevealDragStart(); 1
                                } else {
                                    tilting = true; 2
                                }
                            }
                        }
                        when (mode) {
                            1 -> { accY += dy; onRevealDragDelta(accY); change.consume() }
                            2 -> { dragX += dx; dragY += dy; change.consume() }
                        }
                    }
                    when (mode) {
                        1 -> onRevealDragEnd()
                        2 -> { tilting = false; dragX = 0f; dragY = 0f }
                    }
                }
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(house.top, house.bottom)))
            // Foil border — a thin bright rim that brightens toward the tilt direction.
            .drawBehind {
                drawHoloAndFoil(house = house, sweep = sweep, tiltX = tiltX, tiltY = tiltY)
            },
    ) {
        ProfileCardContent(
            childName = child?.name ?: state.today.label.ifBlank { "Your child" },
            childPhoto = child?.profilePic,
            house = house,
            level = child?.currentLevel ?: 0,
            progressPct = progressPct,
            // Attendance %: only a real figure once the month actually has recorded school days —
            // otherwise null so the tile shows a graceful "—" instead of a stark, misleading 0%.
            attendancePct = state.attendance?.takeIf { it.totalDays > 0 }?.attendanceRate,
            // Status pill: prefer the resolved live label (Holiday/Sunday/…); fall back to the
            // dashboard's own attendance_status so the card is never blank for a real child.
            statusLabel = state.today.label.ifBlank { child?.attendanceStatus.orEmpty() },
            scorePct = state.latestMark?.let { m ->
                val marks = m.marks
                if (marks != null && m.maxMarks > 0) ((marks / m.maxMarks) * 100).roundToInt() else null
            },
            topicsToday = state.coveredToday.size,
            modifier = Modifier.fillMaxSize().padding(22.dp),
        )
    }
}

/** Draws the moving holo sweep + the drag-reactive specular highlight + foil rim. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoloAndFoil(
    house: ProfileHouse,
    sweep: Float,
    tiltX: Float,
    tiltY: Float,
) {
    val w = size.width
    val h = size.height

    // 1) Diagonal holographic band that sweeps across continuously.
    val bandCenter = sweep * w
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                house.foil.copy(alpha = 0.0f),
                house.foil.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f),
                house.foil.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            start = Offset(bandCenter - w * 0.5f, 0f),
            end = Offset(bandCenter + w * 0.5f, h),
            tileMode = TileMode.Clamp,
        ),
        size = Size(w, h),
    )

    // 2) Specular highlight that tracks the tilt — the card "catches light" where you lean it.
    val hx = (w / 2f) + (tiltY / 14f) * (w * 0.45f)
    val hy = (h / 2f) - (tiltX / 14f) * (h * 0.45f)
    val intensity = (abs(tiltX) + abs(tiltY)) / 28f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f + intensity * 0.20f),
                Color.Transparent,
            ),
            center = Offset(hx, hy),
            radius = w * 0.9f,
        ),
        size = Size(w, h),
    )

    // 3) Foil rim — bright inset border.
    val stroke = 3f
    drawRect(
        brush = Brush.linearGradient(listOf(house.foil.copy(alpha = 0.9f), house.top.copy(alpha = 0.6f), house.foil.copy(alpha = 0.9f))),
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(w - stroke, h - stroke),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
    )
}

@Composable
private fun ProfileCardContent(
    childName: String,
    childPhoto: String?,
    house: ProfileHouse,
    level: Int,
    progressPct: Int,
    attendancePct: Int?,
    statusLabel: String,
    scorePct: Int?,
    topicsToday: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        // ── Header: house crest + status pill ──
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(house.foil.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        house.crest,
                        style = VTheme.type.labelStrong.colored(house.onColor).copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "House ${house.title}",
                    style = VTheme.type.labelStrong.colored(house.onColor.copy(alpha = 0.85f)),
                )
            }
            if (statusLabel.isNotBlank()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        statusLabel,
                        style = VTheme.type.caption.colored(house.onColor).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    )
                }
            }
        }

        // ── Hero: avatar + name + level ──
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.Center) {
                // Glow halo behind the avatar.
                Box(
                    Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(house.foil.copy(alpha = 0.45f), Color.Transparent),
                            ),
                        ),
                )
                VAvatar(name = childName.ifBlank { "?" }, src = childPhoto, size = 104.dp, ring = true)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                childName.ifBlank { "Your child" },
                style = VTheme.type.h2.colored(house.onColor).copy(fontWeight = FontWeight.Black),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Level $level  ·  $progressPct% journey",
                style = VTheme.type.caption.colored(house.onColor.copy(alpha = 0.85f)),
            )
        }

        // ── Stat tiles: real backend numbers, each a layered-glass tile with a mini
        //    progress arc (ATTEND/SCORE) or a count badge (TODAY). Premium depth, not capsules. ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "ATTEND",
                value = attendancePct?.let { "$it%" } ?: "—",
                ratio = attendancePct?.let { it / 100f },
                house = house,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "SCORE",
                value = scorePct?.let { "$it%" } ?: "—",
                ratio = scorePct?.let { it / 100f },
                house = house,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "TODAY",
                value = topicsToday.toString(),
                ratio = null,
                sub = if (topicsToday == 1) "topic" else "topics",
                house = house,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A premium glass stat tile. Renders a layered glass surface (top sheen + inset foil hairline)
 * with a mini progress arc behind the value when a [ratio] is supplied, or a clean count when not.
 */
@Composable
private fun StatTile(
    label: String,
    value: String,
    ratio: Float?,
    house: ProfileHouse,
    modifier: Modifier = Modifier,
    sub: String? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .drawWithContent {
                drawContent()
                // top sheen
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        endY = size.height * 0.5f,
                    ),
                )
                // inset foil hairline
                val s = 1.2.dp.toPx()
                drawRoundRect(
                    color = house.foil.copy(alpha = 0.28f),
                    topLeft = Offset(s / 2f, s / 2f),
                    size = Size(size.width - s, size.height - s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(15.dp.toPx(), 15.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = s),
                )
            }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp)) {
            if (ratio != null) {
                StatArc(ratio = ratio.coerceIn(0f, 1f), house = house, modifier = Modifier.fillMaxSize())
            }
            Text(
                value,
                style = VTheme.type.dataLg.colored(house.onColor).copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = VTheme.type.label.colored(house.onColor.copy(alpha = 0.82f)).copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
        )
        if (sub != null) {
            Text(
                sub,
                style = VTheme.type.caption.colored(house.onColor.copy(alpha = 0.6f)).copy(fontSize = 8.5.sp),
            )
        }
    }
}

/** A small foil progress arc drawn behind a stat value. */
@Composable
private fun StatArc(ratio: Float, house: ProfileHouse, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState(targetValue = ratio, label = "statArc")
    Box(modifier) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.5.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(house.foil.copy(alpha = 0.8f), Color.White)),
                startAngle = -90f, sweepAngle = 360f * sweep, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}

/**
 * ProfileRevealHint — a non-interactive grab-handle + caption that floats just beneath the card
 * while it's collapsed, inviting the swipe-down. It owns NO gesture (the card itself does), so it
 * never competes for the drag; it simply fades + drifts away as the panel reveals.
 */
@Composable
private fun ProfileRevealHint(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val fade = (1f - progress * 2f).coerceIn(0f, 1f)
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .graphicsLayer {
                alpha = fade
                translationY = 18f * progress
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.42f)),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                VIcons.ChevronDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Swipe down for account options",
                style = VTheme.type.caption.colored(Color.White.copy(alpha = 0.6f)),
            )
        }
    }
}

/**
 * ProfileOptionsPanel — the account-options surface that rises from the bottom as the card is
 * swiped down. Slides + fades with `progress`, hosting the real account rows (personal details,
 * linked children, discover schools, help) and a gated **Log out**. No popup/toast — it's a
 * drag-tracked sheet that becomes interactive only once mostly revealed.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProfileOptionsPanel(
    parentName: String,
    parentContact: String,
    progress: Float,
    onCollapseDragStart: () -> Unit,
    onCollapseDragDelta: (Float) -> Unit,
    onCollapseDragEnd: () -> Unit,
    onLinkChild: () -> Unit,
    onDiscoverSchools: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val uriHandler = LocalUriHandler.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    VConfirmDialog(
        visible = showLogoutConfirm,
        title = "Log out?",
        message = "You'll need to sign in again to follow your child's progress.",
        confirmLabel = "Log out",
        onConfirm = { showLogoutConfirm = false; onLogout() },
        onDismiss = { showLogoutConfirm = false },
        icon = VIcons.AlertTriangle,
    )

    // Panel occupies ~74% of height; translates fully off-screen at progress 0. Below ~2% it's
    // effectively closed, so we skip pointer input to let the card own all gestures.
    val interactive = progress > 0.02f
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight(0.74f)
            .graphicsLayer {
                translationY = size.height * (1f - progress)
                alpha = progress
            }
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(c.background)
            .navigationBarsPadding(),
    ) {
        // Top grab handle — swipe UP here (negative dy → reveal stays/closes path) collapses the
        // panel back to the card. Reports accumulated dy to the host like the card does.
        var acc by remember { mutableStateOf(0f) }
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    if (interactive) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { acc = 0f; onCollapseDragStart() },
                            onDragEnd = { acc = 0f; onCollapseDragEnd() },
                            onDragCancel = { acc = 0f; onCollapseDragEnd() },
                        ) { change, dy ->
                            change.consume()
                            acc += dy
                            onCollapseDragDelta(acc)
                        }
                    } else Modifier,
                )
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.ink3.copy(alpha = 0.35f)),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Text("Account", style = VTheme.type.h2.colored(c.ink))
            if (parentName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(parentName, style = VTheme.type.bodyStrong.colored(c.ink2))
            }
            if (parentContact.isNotBlank()) {
                Text(parentContact, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
            }
            Spacer(Modifier.height(16.dp))

            data class Row2(val title: String, val sub: String, val onClick: (() -> Unit)?)
            val rows = listOf(
                Row2("Personal details", parentContact.ifBlank { "Mobile, email, photo" }, null),
                Row2("Linked children", "Link a child or manage who you follow", onLinkChild),
                Row2("Discover schools", "Browse all schools on VidyaPrayag", onDiscoverSchools),
                Row2("Notification preferences", "Push, WhatsApp, quiet hours", null),
                Row2("Change password", "Keep your account secure", null),
                Row2(
                    "Help & support",
                    "Email ${com.littlebridge.vidyaprayag.ui.v2.screens.auth.SUPPORT_EMAIL}",
                    {
                        runCatching {
                            uriHandler.openUri(
                                "mailto:${com.littlebridge.vidyaprayag.ui.v2.screens.auth.SUPPORT_EMAIL}?subject=VidyaPrayag%20Support",
                            )
                        }
                    },
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row -> OptionRow(row.title, row.sub, row.onClick) }
            }

            Spacer(Modifier.height(16.dp))
            VButton(
                text = "Log out",
                onClick = { showLogoutConfirm = true },
                full = true,
                variant = VButtonVariant.Destructive,
            )
            Spacer(Modifier.height(8.dp))
            VButton(
                text = "Back to card",
                onClick = onClose,
                full = true,
                variant = VButtonVariant.Ghost,
            )
        }
    }
}

@Composable
private fun OptionRow(title: String, sub: String, onClick: (() -> Unit)?) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(c.card)
    val rowMod = if (onClick != null) {
        base.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        base
    }
    Row(
        rowMod.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = VTheme.type.bodyStrong.colored(c.ink))
            Text(sub, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
        }
        if (onClick != null) {
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(16.dp))
        }
    }
}
