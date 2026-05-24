package com.littlebridge.vidyaprayag.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicCalendarViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SyllabusTarget
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicCalendarScreen() {
    val viewModel: AcademicCalendarViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        onBackClick = { navigator.goBack() },
        immersiveTopBar = true
    ) { paddingValues, scrollModifier ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                CalendarHeaderSection()
            }

            item {
                MainCalendarCard(month = state.currentMonth)
            }

            item {
                SyllabusTrackerSection(targets = state.syllabusTargets)
            }

            item {
                QuickUploadCard()
            }

            item {
                StatsGrid(
                    workingDays = state.workingDays,
                    holidays = state.holidays,
                    conflicts = state.conflicts
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun CalendarHeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(
                "Academic Calendar 2023-24",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Central District Administration Dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Syllabus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Holidays", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MainCalendarCard(month: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) { Icon(Icons.Default.ChevronLeft, null) }
                    Text(month, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Text(
                            "Month",
                            modifier = Modifier.background(Color.White, RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "List",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Grid mockup
            Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.background(MaterialTheme.colorScheme.outlineVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                // Mock Week
                Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                    repeat(7) { col ->
                        val day = (col + 9) // Just a mock start
                        val isSpecial = day == 15
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .background(if (isSpecial) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f) else Color.White)
                                .padding(4.dp)
                        ) {
                            Text(day.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Normal)
                            if (isSpecial) {
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "UNIT 1 DEADLINE",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusTrackerSection(targets: List<SyllabusTarget>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                Text("Auto-Mapping", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Automated syllabus completion targets based on current academic pace.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                targets.forEach { target ->
                    SyllabusTargetItem(target)
                }
            }
        }
    }
}

@Composable
private fun SyllabusTargetItem(target: SyllabusTarget) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(target.subject, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Surface(
                    color = if (target.status == "TARGET SET") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        target.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp
                    )
                }
            }
            Text(target.chapter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            if (target.progress > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deadline: ${target.deadline}", style = MaterialTheme.typography.labelSmall)
                        Text("${(target.progress * 100).toInt()}% Pace", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    LinearProgressIndicator(
                        progress = { target.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            } else {
                Text("Deadline: ${target.deadline}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun QuickUploadCard() {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Bulk Upload", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Upload your district academic schedule in CSV or PDF format to auto-populate the calendar.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("Drop files here", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(workingDays: Int, holidays: Int, conflicts: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatRow(
            label = "Working Days Scheduled",
            value = workingDays.toString(),
            icon = Icons.Default.Verified,
            color = MaterialTheme.colorScheme.secondary
        )
        StatRow(
            label = "Public & School Holidays",
            value = holidays.toString(),
            icon = Icons.Default.BeachAccess,
            color = Color(0xFFE11D48) // Rose
        )
        StatRow(
            label = "Syllabus Conflicts Detected",
            value = conflicts.toString(),
            icon = Icons.Default.PriorityHigh,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, icon: ImageVector, color: Color) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
