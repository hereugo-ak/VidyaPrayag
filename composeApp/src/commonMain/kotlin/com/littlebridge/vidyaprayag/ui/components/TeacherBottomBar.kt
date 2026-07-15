package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator

enum class TeacherTab {
    HOME, TIMETABLE, CLASSES, MESSAGES, PROFILE
}

@Composable
fun TeacherBottomBar(selectedTab: TeacherTab) {
    val navigator = LocalAppNavigator.current

    VidyaPrayagBottomBar(
        items = listOf(
            BottomNavItem(
                label = "HOME",
                icon = Icons.Default.Home,
                isSelected = selectedTab == TeacherTab.HOME,
                onClick = { if (selectedTab != TeacherTab.HOME) navigator.navigateTo(Destination.TeacherHome) }
            ),
            BottomNavItem(
                label = "Timetable",
                icon = Icons.Default.CalendarMonth,
                isSelected = selectedTab == TeacherTab.TIMETABLE,
                onClick = { if (selectedTab != TeacherTab.TIMETABLE) navigator.navigateTo(Destination.TeacherTimetable) }
            ),
            BottomNavItem(
                label = "Classes",
                icon = Icons.Default.School,
                isSelected = selectedTab == TeacherTab.CLASSES,
                onClick = { if (selectedTab != TeacherTab.CLASSES) navigator.navigateTo(Destination.TeacherClasses) }
            ),
            BottomNavItem(
                label = "Messages",
                icon = Icons.AutoMirrored.Filled.Chat,
                isSelected = selectedTab == TeacherTab.MESSAGES,
                onClick = { if (selectedTab != TeacherTab.MESSAGES) navigator.navigateTo(Destination.TeacherMessages) }
            ),
            BottomNavItem(
                label = "PROFILE",
                icon = Icons.Default.Person,
                isSelected = selectedTab == TeacherTab.PROFILE,
                onClick = { if (selectedTab != TeacherTab.PROFILE) navigator.navigateTo(Destination.TeacherProfile) }
            )
        )
    )
}
