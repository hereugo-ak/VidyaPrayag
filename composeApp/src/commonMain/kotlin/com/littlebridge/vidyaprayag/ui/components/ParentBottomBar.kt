package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator

enum class ParentTab {
    DISCOVER, TRACK_PROGRESS, FEES, ANNOUNCEMENT, MESSAGES
}

@Composable
fun ParentDashboardBottomBar(selectedTab: ParentTab) {
    val navigator = LocalAppNavigator.current
    VidyaPrayagBottomBar(
        items = listOf(
            BottomNavItem(
                "Discover", 
                Icons.Default.Search, 
                isSelected = selectedTab == ParentTab.DISCOVER,
                onClick = { navigator.navigateTo(Destination.ParentDashboard) }
            ),
            BottomNavItem(
                "Track Progress", 
                Icons.Default.Analytics, 
                isSelected = selectedTab == ParentTab.TRACK_PROGRESS,
                onClick = { navigator.navigateTo(Destination.TrackProgress) }
            ),
            BottomNavItem(
                "Fees", 
                Icons.Default.Payments, 
                isSelected = selectedTab == ParentTab.FEES,
                onClick = { navigator.navigateTo(Destination.Fees) }
            ),
            BottomNavItem(
                "Announcement", 
                Icons.Default.Campaign, 
                isSelected = selectedTab == ParentTab.ANNOUNCEMENT,
                onClick = { navigator.navigateTo(Destination.ParentAnnouncements) }
            ),
            BottomNavItem(
                "Messages", 
                Icons.AutoMirrored.Filled.Chat, 
                isSelected = selectedTab == ParentTab.MESSAGES,
                onClick = { navigator.navigateTo(Destination.ParentMessages) }
            )
        )
    )
}
