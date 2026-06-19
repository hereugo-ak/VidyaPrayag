package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardState
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
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
 * Commit 11 layers the swipe-down account-options reveal ON TOP of this; this commit owns the
 * card and its motion.
 */
@Composable
fun ParentProfileCardScreenV2(
    modifier: Modifier = Modifier,
    viewModel: ParentDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(c.navyDeep, c.navy)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        ProfilePlayerCard(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        )
    }
}

/**
 * The collectible card itself — tilt + holo + real stats. Pulled out as a stateless-ish body so
 * commit 11's swipe-down reveal can host it without duplicating the motion rig.
 */
@Composable
fun ProfilePlayerCard(
    state: ParentDashboardState,
    modifier: Modifier = Modifier,
) {
    val child = state.selectedChild
    val house = houseFor(child?.id ?: "")

    // overallProgress arrives either as a 0..1 fraction or an already-scaled 0..100 percent
    // depending on the endpoint build — normalise to a whole-number percent for display.
    val rawProgress = child?.overallProgress ?: 0.0
    val progressPct = (if (rawProgress <= 1.0) rawProgress * 100.0 else rawProgress).roundToInt().coerceIn(0, 100)

    // ── Drag-driven tilt ──────────────────────────────────────────────────────
    // Raw drag offset (px) → target rotation; spring back to 0 on release.
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    val tiltY by animateFloatAsState(
        targetValue = if (dragging) (dragX / 18f).coerceIn(-14f, 14f) else 0f,
        animationSpec = tween(durationMillis = if (dragging) 60 else 520),
        label = "tiltY",
    )
    val tiltX by animateFloatAsState(
        targetValue = if (dragging) (-dragY / 18f).coerceIn(-14f, 14f) else 0f,
        animationSpec = tween(durationMillis = if (dragging) 60 else 520),
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
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; dragX = 0f; dragY = 0f },
                    onDragCancel = { dragging = false; dragX = 0f; dragY = 0f },
                ) { change, drag ->
                    change.consume()
                    dragX += drag.x
                    dragY += drag.y
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
            attendancePct = state.attendance?.attendanceRate ?: 0,
            statusLabel = state.today.label,
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
    attendancePct: Int,
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

        // ── Stat tiles: all real backend numbers ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("ATTEND", "$attendancePct%", house, Modifier.weight(1f))
            StatTile("SCORE", scorePct?.let { "$it%" } ?: "—", house, Modifier.weight(1f))
            StatTile("TODAY", topicsToday.toString(), house, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    house: ProfileHouse,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .drawWithContent {
                drawContent()
                // thin top sheen on each glass tile
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        endY = size.height * 0.5f,
                    ),
                )
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = VTheme.type.h3.colored(house.onColor).copy(fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = VTheme.type.label.colored(house.onColor.copy(alpha = 0.8f)).copy(fontSize = 9.sp),
        )
    }
}
