package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VColors
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.util.nowMinutesOfDay

/**
 * TeacherKit — the shared visual vocabulary for the rebuilt Teacher Portal.
 *
 * The teacher portal is built FROM SCRATCH on the Parents Portal's design language
 * (lavender canvas, white rounded cards with hairline borders, a violet brand accent
 * used ONLY for active/brand moments, semantic colour for data, Canvas progress rings,
 * tinted hero plates, and the signature swipe-to-expand card). Everything here reads
 * tokens from [VTheme]; nothing hardcodes a hex except the harmonious subject rotation
 * (mirrored from the parents portal's ParentPalette).
 *
 * COLOUR IS MEANING (design law): green = good/present/done, amber = pending/late,
 * red = absent/overdue, navy = calm/holiday, violet = brand/active.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Palette — harmonious per-subject accent rotation (mirrors ParentPalette).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun teacherSubjectPalette(c: VColors): List<Color> = listOf(
    c.accent,          // violet (brand)
    c.teal,            // teal
    Color(0xFF6C8DF5), // sky
    Color(0xFFFF8A65), // peach
    c.tealDeep,        // deep teal
)

/** Deterministic, stable per-key accent so a given subject keeps its hue everywhere. */
@Composable
fun teacherSubjectColor(c: VColors, key: String): Color {
    val palette = teacherSubjectPalette(c)
    if (key.isBlank()) return palette.first()
    val idx = ((key.hashCode() % palette.size) + palette.size) % palette.size
    return palette[idx]
}

// ─────────────────────────────────────────────────────────────────────────────
// Greeting — time-sensitive (matches the parents portal greeting voice).
// ─────────────────────────────────────────────────────────────────────────────

/** "Good morning" / "Good afternoon" / "Good evening" by the device clock. */
fun teacherGreeting(hour: Int = nowMinutesOfDay() / 60): String = when (hour) {
    in 0..4 -> "Good evening"
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen scaffold helpers
// ─────────────────────────────────────────────────────────────────────────────

/** The portal's lavender canvas + a barely-there violet aurora wash, top-left. */
fun Modifier.teacherCanvas(c: VColors): Modifier = this
    .background(c.background)

@Composable
fun TeacherAurora(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(c.accent.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.02f),
                    radius = size.width * 0.9f,
                ),
            )
        }
    }
}

/** A bounded, centred host for loading / error / empty legs on the lavender wash. */
@Composable
fun TeacherCenterState(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The portal's signature brand-violet spinner. */
@Composable
fun TeacherSpinner(size: androidx.compose.ui.unit.Dp = 34.dp) {
    CircularProgressIndicator(color = VTheme.colors.accent, modifier = Modifier.size(size))
}

// ─────────────────────────────────────────────────────────────────────────────
// SwipeExpandCard — THE signature interaction (lifted from ParentAttendanceCard).
//
// A white rounded card that flips between an ordered list of "faces" via a
// contained horizontal swipe (left → next face / more detail, right → previous).
// The card grows vertically and the parent column reflows. Small drags are ignored
// so vertical scroll still wins; the gesture NEVER navigates the page.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SwipeExpandCard(
    face: Int,
    faceCount: Int,
    onFaceChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable (Int) -> Unit,
) {
    val c = VTheme.colors
    val swipe = Modifier.pointerInput(faceCount) {
        var dx = 0f
        detectHorizontalDragGestures(
            onDragStart = { dx = 0f },
            onDragEnd = {
                if (dx <= -40f && face < faceCount - 1) onFaceChange(face + 1)
                else if (dx >= 40f && face > 0) onFaceChange(face - 1)
            },
        ) { _, delta -> dx += delta }
    }
    Box(
        modifier
            .then(swipe)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            .border(1.dp, c.hairline, RoundedCornerShape(24.dp))
            .padding(padding),
    ) {
        AnimatedContent(
            targetState = face,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(160)))
                } else {
                    (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(160)))
                }
            },
            label = "swipeFace",
        ) { f -> content(f) }
    }
}

/** A row of tiny dots indicating which face of a SwipeExpandCard is shown. */
@Composable
fun FaceDots(face: Int, count: Int, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == face) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (i == face) c.accent else c.ink3.copy(alpha = 0.3f)),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small reusable atoms (cards, pills, rings, labels, tiles)
// ─────────────────────────────────────────────────────────────────────────────

/** Plain white rounded card (no swipe), the portal's default surface. */
@Composable
fun TCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = VTheme.colors
    var base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(c.card)
        .border(1.dp, c.hairline, RoundedCornerShape(24.dp))
    if (onClick != null) {
        val ix = remember0()
        base = base.clickable(interactionSource = ix, indication = null, onClick = onClick)
    }
    Box(base.padding(padding)) { content() }
}

@Composable
private fun remember0() = androidx.compose.runtime.remember { MutableInteractionSource() }

/** An eyebrow label with an optional leading status dot. */
@Composable
fun TEyebrow(text: String, dot: Color? = null, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (dot != null) Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(
            text,
            style = VTheme.type.label.colored(c.ink3).copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp),
        )
    }
}

/** A small rounded status pill. */
@Composable
fun TPill(label: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(label, style = VTheme.type.label.colored(fg).copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
    }
}

/** A circular icon chip on a soft tinted disc. */
@Composable
fun TIconDisc(icon: ImageVector, tint: Color, bg: Color, size: androidx.compose.ui.unit.Dp = 40.dp, glyph: androidx.compose.ui.unit.Dp = 20.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(glyph))
    }
}

/**
 * A semantic progress ring (Canvas sweep over a soft track), percent centred.
 * [accent] defaults to the brand violet; pass a semantic colour where it carries meaning.
 */
@Composable
fun TRing(
    percent: Int,
    modifier: Modifier = Modifier,
    accent: Color = VTheme.colors.accent,
    track: Color = accent.copy(alpha = 0.15f),
    stroke: androidx.compose.ui.unit.Dp = 6.dp,
    label: String = "$percent%",
    labelColor: Color = VTheme.colors.navyDeep,
    labelSize: androidx.compose.ui.unit.TextUnit = 15.sp,
) {
    val sweep by animateFloatAsState(targetValue = (percent / 100f).coerceIn(0f, 1f), label = "tring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2f
            val arcSize = Size(size.width - s, size.height - s)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = s, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.7f), accent)),
                startAngle = -90f, sweepAngle = 360f * sweep, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = s, cap = StrokeCap.Round),
            )
        }
        Text(label, style = VTheme.type.dataLg.colored(labelColor).copy(fontWeight = FontWeight.ExtraBold, fontSize = labelSize))
    }
}

/** A compact metric tile (number over caption) on a soft tinted plate. */
@Composable
fun TMetricTile(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = VTheme.type.dataLg.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 10.sp))
    }
}

/** A circular header / nav action button — cream disc, navy glyph, no ripple. */
@Composable
fun TCircleButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    val ix = remember0()
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(c.cream)
            .clickable(interactionSource = ix, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = c.ink, modifier = Modifier.size(16.dp))
    }
}

/** A "swipe for more" hint line shown at the foot of an expandable card. */
@Composable
fun TSwipeHint(text: String = "Swipe for more", modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Text(
        text,
        style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp, letterSpacing = 0.4.sp),
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Format helpers (local, no extra util churn)
// ─────────────────────────────────────────────────────────────────────────────

/** "2026-06-25" → "25 Jun 2026"; blank-safe. */
fun prettyDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val y = parts[0]; val m = parts[1].toIntOrNull() ?: return iso; val d = parts[2].toIntOrNull() ?: return iso
    val mon = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").getOrNull(m - 1) ?: return iso
    return "$d $mon $y"
}

/** "2026-06-25" → "25 Jun" (short). */
fun prettyDateShort(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val m = parts[1].toIntOrNull() ?: return iso; val d = parts[2].toIntOrNull() ?: return iso
    val mon = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").getOrNull(m - 1) ?: return iso
    return "$d $mon"
}

/** Format a float to one decimal place (KMP-safe — no String.format in common). */
fun fmt1(v: Float): String {
    val scaled = kotlin.math.round(v * 10f).toInt()
    val whole = scaled / 10
    val frac = kotlin.math.abs(scaled % 10)
    return "$whole.$frac"
}

/** Resolve an icon for an obligation / reminder type. */
fun obligationIcon(type: String): ImageVector = when (type) {
    "attendance" -> VIcons.ListChecks
    "marks" -> VIcons.GraduationCap
    "homework" -> VIcons.FileText
    "leave" -> VIcons.Calendar
    else -> VIcons.Bell
}
