package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// VTopTabs — horizontally scrollable underline tab bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * VTopTabs — a row of text tabs with a teal underline indicator under the active tab.
 * Translated from primitives.tsx → `VTopTabs`.
 */
@Composable
fun VTopTabs(
    tabs: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    // RA-PP-THEME: portals can override the active accent. Parents Portal passes the
    // website violet (#544AB8) so the underline/active label reads lavender, never green.
    // Defaults to teal so Admin/Teacher keep their existing aesthetic (token reuse, no rework).
    activeColor: Color = VTheme.colors.tealDeep,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxWidth().background(c.card)) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { tab ->
                val active = tab == selected
                val color by animateColorAsState(if (active) activeColor else c.ink3, tween(180), label = "tabColor")
                val interaction = remember { MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(interactionSource = interaction, indication = null) { onSelect(tab) }
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = tab,
                        // React VTopTabs: 13 / 600 (not caption's 12). §matrix.
                        style = VTheme.type.dataSm.colored(color).copy(
                            fontFamily = VTheme.type.uiFamily,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) activeColor else Color.Transparent),
                    )
                }
            }
        }
        VDivider()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VBottomNav — sticky bottom navigation with optional per-item badge
// ─────────────────────────────────────────────────────────────────────────────

/** One destination in [VBottomNav]. */
data class VNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
)

/**
 * VBottomNav — the five-or-fewer tab bar pinned to the screen bottom.
 * Translated from primitives.tsx → `VBottomNav`.
 */
@Composable
fun VBottomNav(
    items: List<VNavItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    // RA-PP-THEME: the active-state accent. Defaults to the legacy primary
    // (tealDeep) so every other portal is untouched, but the Parents Portal —
    // the first to migrate to the website's lavender/navy/violet palette —
    // passes `accentDeep` (the violet #544AB8) so the active tab/pill matches
    // the reference dashboard instead of the old green.
    activeColor: Color? = null,
) {
    val c = VTheme.colors
    val active0 = activeColor ?: c.tealDeep
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // §14/§13.1 — the design floats the bar with a soft UPWARD navy-tinted
    // shadow (`0 -4px 20px navy@4%`), not the default downward Material shadow.
    // Modifier.shadow can only cast downward + can't tint, so we draw a short
    // navy gradient that fades upward just above the bar's top edge.
    val topShadow = if (c.isNight) {
        Modifier
    } else {
        Modifier.drawBehind {
            val h = 20.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, c.shadowTint.copy(alpha = 0.04f)),
                    startY = -h,
                    endY = 0f,
                ),
                topLeft = Offset(0f, -h),
                size = androidx.compose.ui.geometry.Size(size.width, h),
            )
        }
    }

    // ── Animated pill indicator ───────────────────────────────────────────────
    // Each tab reports its own bounds (x-offset within the Row + measured width)
    // via onGloballyPositioned. The pill animates its x-offset and width toward
    // the currently-selected tab's bounds with a bouncy spring so it *slides*
    // under the icon rather than jumping. Bounds are stored in Dp so the layer is
    // density-independent and CMP-safe (no platform APIs).
    val itemXs = remember { mutableStateMapOf<String, Dp>() }
    val itemWidths = remember { mutableStateMapOf<String, Dp>() }
    val targetX = itemXs[selected] ?: 0.dp
    val targetWidth = itemWidths[selected] ?: 0.dp
    val pillX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navPillX",
    )
    val pillWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navPillWidth",
    )

    Column(
        modifier
            .fillMaxWidth()
            .then(topShadow)
            .background(c.card),
    ) {
        VDivider()
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            // Pill sits *behind* the row. Only drawn once bounds are known so it
            // never flashes a zero-width sliver on first composition.
            if (pillWidth > 0.dp) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = pillX)
                        .width(pillWidth)
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        // tint of the active accent (portal-configurable)
                        .background(active0.copy(alpha = if (c.isNight) 0.18f else 0.10f)),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val active = item.id == selected
                    val tint = if (active) active0 else c.ink3
                    val interaction = remember { MutableInteractionSource() }
                    // Selected icon scales to 1.1f, unselected to 1.0f, with a soft spring.
                    val iconScale by animateFloatAsState(
                        targetValue = if (active) 1.1f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "navIconScale",
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                // x relative to the parent Box (Row fills it), width of this tab.
                                // CMP 1.10: LayoutCoordinates.positionInParent() was removed;
                                // boundsInParent().left gives the same parent-relative x offset.
                                itemXs[item.id] = with(density) { coords.boundsInParent().left.toDp() }
                                itemWidths[item.id] = with(density) { coords.size.width.toDp() }
                            }
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(interactionSource = interaction, indication = null) {
                                // Haptic only when the selection actually changes — no
                                // buzz on re-tapping the already-active tab.
                                if (!active) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSelect(item.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Box {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            if (item.badge > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(start = 10.dp)
                                        .clip(CircleShape)
                                        // §matrix: bottom-nav badge is #c14a44 (rose), not dangerInk #b3261e.
                                        .background(Color(0xFFC14A44))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = if (item.badge > 99) "99+" else item.badge.toString(),
                                        style = VTheme.type.dataSm.colored(Color.White).copy(fontSize = 9.sp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.label,
                            style = VTheme.type.label.colored(tint).copy(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = TextUnit.Unspecified,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VBackHeader — a back chevron, centered title, and optional trailing action
// ─────────────────────────────────────────────────────────────────────────────

/**
 * VBackHeader — top app bar with a circular back button, centered title, trailing slot.
 * Translated from primitives.tsx → `VBackHeader`.
 */
@Composable
fun VBackHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxWidth().background(c.card)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // back button
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.cream)
                    .clickable(interactionSource = interaction, indication = null, enabled = onBack != null) { onBack?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ChevronLeft, contentDescription = "Back", tint = c.ink, modifier = Modifier.size(20.dp))
            }
            Text(title, style = VTheme.type.h3.colored(c.ink))
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                action?.invoke()
            }
        }
        VDivider()
    }
}
