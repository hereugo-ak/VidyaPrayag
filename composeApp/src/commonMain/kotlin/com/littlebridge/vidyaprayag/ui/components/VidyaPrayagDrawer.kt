package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.ui.theme.AppTheme
import com.littlebridge.vidyaprayag.ui.theme.LocalAppTheme
import com.littlebridge.vidyaprayag.ui.theme.LocalThemeSwitcher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VidyaPrayagDrawerSheet(
    onShowAuthSheet: () -> Unit
) {
    val mainViewModel: MainViewModel = koinViewModel()
    val userRole by mainViewModel.userRole.collectAsState()
    val navigator = LocalAppNavigator.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(280.dp)
    ) {
        DrawerHeader(userRole, onShowAuthSheet)
        DrawerContent(
            userRole = userRole,
            onShowAuthSheet = onShowAuthSheet,
            onLogout = { mainViewModel.logout() }
        ) {
            navigator.navigateTo(it)
        }
        Spacer(modifier = Modifier.weight(1f))
        DrawerFooter()
    }
}

@Composable
private fun DrawerHeader(
    userRole: String,
    onShowAuthSheet: () -> Unit
) {
    val title = if (userRole == "ADMIN") "School Admin" else if (userRole == "PARENT") "Parent User" else "Guest User"
    val subTitle = if (userRole == "GUEST") "Welcome to VidyaPrayag" else "Welcome back!"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(top = 48.dp, start = 32.dp, end = 32.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(subTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (userRole == "GUEST") {
            Spacer(modifier = Modifier.height(24.dp))
            VidyaPrayagPrimaryButton(
                text = "Login / Signup",
                onClick = onShowAuthSheet,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DrawerContent(
    userRole: String,
    onShowAuthSheet: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (Destination) -> Unit
) {
    val currentTheme = LocalAppTheme.current
    val themeSwitcher = LocalThemeSwitcher.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        if (userRole != "GUEST") {
            DrawerItem(Icons.Default.Home, "Home", isSelected = true) { onNavigate(Destination.SchoolDashboard) }
        }
        
        if (userRole == "ADMIN") {
            Text("SCHOOL OPTIONS", modifier = Modifier.padding(vertical = 12.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline, letterSpacing = 2.sp)
            DrawerItem(Icons.Default.Analytics, "Analytics") { onNavigate(Destination.AnalyticsDashboard) }
            DrawerItem(Icons.Default.CalendarMonth, "Academic Calendar") { onNavigate(Destination.AcademicCalendar) }
            DrawerItem(Icons.Default.AssignmentTurnedIn, "Daily Attendance") { onNavigate(Destination.DailyAttendance) }
            DrawerItem(Icons.Default.PendingActions, "Leave Request") { onNavigate(Destination.LeaveRequests) }
            DrawerItem(Icons.Default.Assessment, "Results") { onNavigate(Destination.Results) }
            DrawerItem(Icons.Default.Groups, "Schedule PTM") { onNavigate(Destination.SchedulePTM) }
        }
        
        if (userRole == "PARENT") {
            Text("PARENT OPTIONS", modifier = Modifier.padding(vertical = 12.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline, letterSpacing = 2.sp)
            DrawerItem(Icons.Default.Explore, "Career Path") { onNavigate(Destination.CareerPath) }
            DrawerItem(Icons.Default.CardGiftcard, "Scholarship For You") { onNavigate(Destination.Scholarships) }
            DrawerItem(Icons.AutoMirrored.Filled.FactCheck, "Daily Status") { onNavigate(Destination.DailyStatus) }
            DrawerItem(Icons.Default.Assessment, "Report") { onNavigate(Destination.ParentReports) }
            DrawerItem(Icons.Default.Groups, "Schedule PTM") { onNavigate(Destination.ParentSchedulePTM) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
        
        Text("THEME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        ThemeToggleItem("Day Mode", Icons.Default.LightMode, isSelected = currentTheme == AppTheme.LIGHT) { themeSwitcher(AppTheme.LIGHT) }
        ThemeToggleItem("Night Mode", Icons.Default.DarkMode, isSelected = currentTheme == AppTheme.DARK) { themeSwitcher(AppTheme.DARK) }
        ThemeToggleItem("Midnight", Icons.Default.AutoAwesome, isSelected = currentTheme == AppTheme.MIDNIGHT) { themeSwitcher(AppTheme.MIDNIGHT) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
        
        val signText = if (userRole == "GUEST") "Sign In" else "Sign Out"
        val signIcon = if (userRole == "GUEST") Icons.Default.Login else Icons.AutoMirrored.Filled.Logout
        
        DrawerItem(signIcon, signText) {
            if (userRole == "GUEST") {
                onShowAuthSheet()
            } else {
                onLogout()
            }
        }
        DrawerItem(Icons.Default.Help, "Support")
        DrawerItem(Icons.Default.Security, "Privacy Policy")
        DrawerItem(Icons.Default.Description, "Terms & Conditions")
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, isSelected: Boolean = false, onClick: () -> Unit = {}) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = contentColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ThemeToggleItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun DrawerFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("VIDYAPRAYAG V2.4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.secondary))
            Spacer(modifier = Modifier.width(4.dp))
            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        }
    }
}
