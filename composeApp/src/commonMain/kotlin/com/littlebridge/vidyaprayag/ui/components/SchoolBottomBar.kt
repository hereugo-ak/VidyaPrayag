package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.runtime.Composable
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator

enum class SchoolTab {
    HOME, MESSAGES, ANNOUNCEMENTS, ENQUIRIES, PROFILE
}

@Composable
fun SchoolDashboardBottomBar(selectedTab: SchoolTab) {
    val navigator = LocalAppNavigator.current
    
    VidyaPrayagBottomBar(
        items = listOf(
            BottomNavItem(
                label = "Home", 
                icon = Icons.Default.Home, 
                isSelected = selectedTab == SchoolTab.HOME,
                onClick = { if (selectedTab != SchoolTab.HOME) navigator.navigateTo(Destination.SchoolDashboard) }
            ),
            BottomNavItem(
                label = "Messages", 
                icon = Icons.AutoMirrored.Filled.Chat,
                isSelected = selectedTab == SchoolTab.MESSAGES,
                onClick = { if (selectedTab != SchoolTab.MESSAGES) navigator.navigateTo(Destination.Messages) }
            ),
            BottomNavItem(
                label = "Announcements", 
                icon = Icons.Default.Campaign, 
                isSelected = selectedTab == SchoolTab.ANNOUNCEMENTS,
                onClick = { if (selectedTab != SchoolTab.ANNOUNCEMENTS) navigator.navigateTo(Destination.SchoolAnnouncements) }
            ),
            BottomNavItem(
                label = "Enquiries", 
                icon = Icons.Default.QuestionAnswer,
                isSelected = selectedTab == SchoolTab.ENQUIRIES,
                onClick = { if (selectedTab != SchoolTab.ENQUIRIES) navigator.navigateTo(Destination.AdmissionCRMDashboard) }
            ),
            BottomNavItem(
                label = "Profile", 
                icon = Icons.Default.Person, 
                isSelected = selectedTab == SchoolTab.PROFILE,
                onClick = { if (selectedTab != SchoolTab.PROFILE) navigator.navigateTo(Destination.InstitutionalProfile) }
            )
        )
    )
}
