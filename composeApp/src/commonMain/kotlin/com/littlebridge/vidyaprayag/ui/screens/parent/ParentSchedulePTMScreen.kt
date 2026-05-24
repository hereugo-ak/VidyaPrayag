package com.littlebridge.vidyaprayag.ui.screens.parent

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.parent.presentation.*
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSchedulePTMScreen() {
    val viewModel: ParentSchedulePTMViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        immersiveTopBar = true,
        onBackClick = { navigator.goBack() }
    ) { paddingValues, scrollModi ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModi)
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                PTMHeader()
            }

            item {
                CalendarSection(
                    month = state.selectedMonth,
                    selectedDate = state.selectedDate,
                    ptmWindowDays = state.ptmWindowDays,
                    onDateClick = viewModel::selectDate
                )
            }

            item {
                TeacherAndSlotsSection(
                    name = state.teacherName,
                    subject = state.teacherSubject,
                    imageUrl = state.teacherImageUrl,
                    slots = state.slots,
                    onSlotClick = viewModel::selectSlot,
                    selectedMonth = state.selectedMonth,
                    selectedDate = state.selectedDate
                )
            }

            item {
                MyBookingsSection(state.bookings)
            }

            item {
                ReminderSettingsCard()
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun PTMHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "PTM Scheduling",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Connect with your child's educators. Select a teacher and book your preferred 15-minute consultation slot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                Text("2 Slots Left", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CalendarSection(
    month: String,
    selectedDate: Int,
    ptmWindowDays: List<Int>,
    onDateClick: (Int) -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(month, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { }) { Icon(Icons.Default.ChevronLeft, null) }
                    IconButton(onClick = { }) { Icon(Icons.Default.ChevronRight, null) }
                }
            }

            // Days of week
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Simple calendar grid mock
            val days = (1..16).toList()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until (days.size + 6) / 7).forEach { rowIndex ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (0 until 7).forEach { colIndex ->
                            val dayIndex = rowIndex * 7 + colIndex - 4 // Offset for mock
                            if (dayIndex in 1..16) {
                                val isSelected = dayIndex == selectedDate
                                val isWindow = ptmWindowDays.contains(dayIndex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isWindow -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isWindow && !isSelected) 1.dp else 0.dp,
                                            color = if (isWindow && !isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onDateClick(dayIndex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayIndex.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected || isWindow) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else if (isWindow) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CalendarLegendItem(Color.Black, "Selected Date")
                CalendarLegendItem(MaterialTheme.colorScheme.secondaryContainer, "PTM Window")
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun TeacherAndSlotsSection(
    name: String,
    subject: String,
    imageUrl: String,
    slots: List<PTMTimeSlot>,
    onSlotClick: (String) -> Unit,
    selectedMonth: String,
    selectedDate: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                TextButton(onClick = { }) {
                    Text("Change", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text("Available Time Slots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Duration: 15 Minutes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slots.forEach { slot ->
                        SlotButton(
                            slot = slot,
                            onClick = { if (slot.isAvailable) onSlotClick(slot.time) },
                            modifier = Modifier.weight(1f).widthIn(min = 100.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Booking for $selectedMonth $selectedDate at ${slots.find { it.isSelected }?.time ?: "..."}. A confirmation email will be sent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book Slot", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SlotButton(slot: PTMTimeSlot, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            slot.isSelected -> MaterialTheme.colorScheme.secondary
            !slot.isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> Color.White
        },
        border = if (slot.isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Text(
            text = slot.time,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (slot.isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (slot.isSelected) Color.White else if (!slot.isAvailable) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MyBookingsSection(bookings: List<PTMBooking>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("My Bookings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }

        bookings.forEach { booking ->
            VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when(booking.iconName) {
                                        "science" -> Icons.Default.Science
                                        else -> Icons.Default.School
                                    },
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(booking.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(booking.teacher, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), shape = CircleShape) {
                            Text("Upcoming", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(booking.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(booking.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text("Auto-Reminder On", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row {
                            IconButton(onClick = { }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            IconButton(onClick = { }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSettingsCard() {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth(), backgroundColor = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Reminder Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Never miss a parent-teacher meeting again with our automated alert system.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderToggleRow("Email Notification", Icons.Default.Mail)
                ReminderToggleRow("SMS Alert (1hr before)", Icons.Default.Sms)
            }
            
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Update Preferences", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            Switch(checked = true, onCheckedChange = { }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.secondary))
        }
    }
}
