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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.Attendee
import com.littlebridge.vidyaprayag.feature.admin.presentation.AttendanceStatus
import com.littlebridge.vidyaprayag.feature.admin.presentation.DailyAttendanceViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAttendanceScreen() {
    val viewModel: DailyAttendanceViewModel = koinViewModel()
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                AttendanceHeaderSection()
            }

            item {
                AttendanceTypeToggle(
                    selectedType = state.attendanceType,
                    onTypeSelect = viewModel::setAttendanceType
                )
            }

            item {
                ClassSelector(
                    selectedClass = state.selectedClass,
                    availableClasses = state.availableClasses,
                    onClassSelect = viewModel::selectClass
                )
            }

            item {
                SummaryCard(
                    presentCount = state.presentCount,
                    totalCount = state.totalCount
                )
            }

            item {
                SectionHeader(title = "${state.selectedClass} Students")
            }

            items(state.attendees) { attendee ->
                AttendeeItem(attendee = attendee)
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AttendanceHeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Attendance Management",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Track and manage daily attendance for students and faculty.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttendanceTypeToggle(selectedType: String, onTypeSelect: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf("Faculty", "Students").forEach { type ->
                val isSelected = selectedType == type
                Surface(
                    onClick = { onTypeSelect(type) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color.White else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassSelector(selectedClass: String, availableClasses: List<String>, onClassSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SELECT CLASS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedClass,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableClasses.forEach { className ->
                    DropdownMenuItem(
                        text = { Text(className) },
                        onClick = {
                            onClassSelect(className)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(presentCount: Int, totalCount: Int) {
    val percentage = (presentCount.toFloat() / totalCount * 100).toInt()
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FactCheck, null, tint = MaterialTheme.colorScheme.secondary)
                }
                Column {
                    Text("CLASS ATTENDANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$presentCount/$totalCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Present Today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("$percentage%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { }) {
            Text("Filter By Dept", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun AttendeeItem(attendee: Attendee) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // weight(1f) so a long name shrinks instead of shoving the status
            // badge off-screen on narrow phones.
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(attendee.initials, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(
                                when(attendee.status) {
                                    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.secondary
                                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                                    AttendanceStatus.LATE -> Color(0xFFFACC15) // Amber
                                }
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(attendee.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("ID: #2024-00${attendee.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                color = when(attendee.status) {
                    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    AttendanceStatus.LATE -> Color(0xFFFACC15).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = when(attendee.status) {
                        AttendanceStatus.PRESENT -> "Present"
                        AttendanceStatus.ABSENT -> "Absent"
                        AttendanceStatus.LATE -> "Late"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when(attendee.status) {
                        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.secondary
                        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                        AttendanceStatus.LATE -> Color(0xFFCA8A04) // Darker amber
                    }
                )
            }
        }
    }
}
