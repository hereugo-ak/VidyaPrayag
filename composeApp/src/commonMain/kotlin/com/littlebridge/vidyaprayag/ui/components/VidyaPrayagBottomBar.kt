/*
 * File: VidyaPrayagBottomBar.kt  (commonMain)
 * Module: ui.components
 *
 * Premium iOS-style bottom navigation. This is the SINGLE source of truth for
 * both the school (SchoolDashboardBottomBar) and parent (ParentDashboardBottomBar)
 * navigation bars — upgrading it here lifts every caller with ZERO conflicts,
 * because the public `VidyaPrayagBottomBar(items)` signature is unchanged.
 *
 * What replaces the old generic M3 NavigationBar:
 *   - floating glassy surface (rounded, hairline border, soft elevation shadow)
 *   - animated emerald "pill" that springs behind the active tab
 *   - spring-scale + color cross-fade on the active icon
 *   - selected label fades/expands in; unselected stay icon-only (cleaner, premium)
 * Pure Compose Multiplatform — no extra libraries.
 */
package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val isSelected: Boolean = false,
    val onClick: () -> Unit = {}
)

@Composable
fun VidyaPrayagBottomBar(
    items: List<BottomNavItem>
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(30.dp),
                spotColor = primary.copy(alpha = 0.22f),
                ambientColor = primary.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        surface.copy(alpha = 0.98f),
                        surface.copy(alpha = 0.94f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BottomBarTab(item = item)
            }
        }
    }
}

@Composable
private fun BottomBarTab(item: BottomNavItem) {
    val secondary = MaterialTheme.colorScheme.secondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val outline = MaterialTheme.colorScheme.outline

    val iconScale by animateFloatAsState(
        targetValue = if (item.isSelected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon-scale"
    )
    val tint by animateColorAsState(
        targetValue = if (item.isSelected) secondary else outline,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "icon-tint"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .tappableScale(onClick = item.onClick)
            .then(
                if (item.isSelected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                secondaryContainer.copy(alpha = 0.55f),
                                secondaryContainer.copy(alpha = 0.30f)
                            )
                        )
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            AnimatedVisibility(
                visible = item.isSelected,
                enter = fadeIn(tween(220)) + expandHorizontally(tween(220, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(160)) + shrinkHorizontally(tween(160))
            ) {
                Text(
                    text = item.label,
                    color = secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
