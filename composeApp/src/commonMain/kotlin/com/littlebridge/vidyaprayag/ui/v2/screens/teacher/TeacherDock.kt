package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.theme.VElevationLevel
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.vElevation

/**
 * TeacherDock — the rebuilt Teacher Portal's signature **floating dock**, a faithful sibling of
 * the Parents Portal's ParentDock (same premium physics, teacher-namespaced so the two evolve
 * independently). A detached, rounded glass bar floating above the lavender canvas:
 *   • A liquid violet lozenge springs horizontally under the active tab and expands to seat the
 *     active tab's label beside its icon (icon-only when inactive).
 *   • The active glyph lifts + scales with a soft spring; selection fires a single haptic tick.
 *   • Real obligation badges ride the icons (e.g. the Update tab's outstanding count).
 *
 * Violet is the BRAND ACCENT for the active state only — the resting bar is clean near-white glass.
 */
@Composable
fun TeacherDock(
    items: List<VNavItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val accent = c.accentDeep

    // Each tab reports its bounds so the lozenge can slide+resize toward the active tab.
    val itemXs = remember { mutableStateMapOf<String, Dp>() }
    val itemWidths = remember { mutableStateMapOf<String, Dp>() }
    val targetX = itemXs[selected] ?: 0.dp
    val targetW = itemWidths[selected] ?: 0.dp
    val pillX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "tDockPillX",
    )
    val pillW by animateDpAsState(
        targetValue = targetW,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "tDockPillW",
    )

    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .vElevation(VElevationLevel.Raised, radius = 30.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(c.card.copy(alpha = if (c.isNight) 1f else 0.98f))
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (c.isNight) 0.04f else 0.6f), Color.Transparent),
                            endY = size.height * 0.5f,
                        ),
                    )
                }
                .border(1.dp, c.hairline, RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            if (pillW > 0.dp) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = pillX)
                        .width(pillW)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = if (c.isNight) 0.26f else 0.12f),
                                    c.accent.copy(alpha = if (c.isNight) 0.20f else 0.10f),
                                ),
                            ),
                        ),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val active = item.id == selected
                    DockItem(
                        item = item,
                        active = active,
                        accent = accent,
                        modifier = Modifier
                            .weight(if (active) 1.35f else 1f)
                            .onGloballyPositioned { coords ->
                                itemXs[item.id] = with(density) { coords.boundsInParent().left.toDp() }
                                itemWidths[item.id] = with(density) { coords.size.width.toDp() }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (!active) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(item.id)
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    item: VNavItem,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val tint = if (active) accent else c.ink3
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "tDockIconScale",
    )
    val iconLift by animateFloatAsState(
        targetValue = if (active) -1.5f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "tDockIconLift",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tDockLabelAlpha",
    )

    Row(
        modifier.height(44.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
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
                        translationY = iconLift * this.density
                    },
            )
            if (item.badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-3).dp)
                        .clip(CircleShape)
                        .background(c.dangerInk)
                        .border(1.5.dp, c.card, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        if (item.badge > 9) "9+" else item.badge.toString(),
                        style = VTheme.type.dataSm.colored(Color.White).copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
        if (active && labelAlpha > 0.01f) {
            Spacer(Modifier.width(7.dp))
            Text(
                item.label,
                maxLines = 1,
                style = VTheme.type.label.colored(accent).copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                modifier = Modifier.graphicsLayer { alpha = labelAlpha },
            )
        }
    }
}
