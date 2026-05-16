package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.isSelected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = null) },
                label = { 
                    Text(
                        item.label, 
                        fontSize = 10.sp, 
                        letterSpacing = 0.5.sp 
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                )
            )
        }
    }
}
