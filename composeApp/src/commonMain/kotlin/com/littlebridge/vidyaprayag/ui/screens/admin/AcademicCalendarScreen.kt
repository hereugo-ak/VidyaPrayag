package com.littlebridge.vidyaprayag.ui.screens.admin

import com.littlebridge.vidyaprayag.ui.theme.StatusColors

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
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarEventDto
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

    // Refresh on screen entry to keep the data fresh after returning from
    // sibling screens (announcements, PTM, etc.).
    LaunchedEffect(Unit) { viewModel.loadCalendar() }

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
                CalendarHeaderSection(
                    isLoading = state.isLoading,
                    onSyncSyllabus = { viewModel.syncSyllabus() },
                    onShowHolidays = { viewModel.showHolidaysOnly() }
                )
            }

            state.errorMessage?.let { msg ->
                item {
                    CalendarBanner(
                        message = msg,
                        isError = true,
                        onDismiss = { viewModel.clearMessages() }
                    )
                }
            }
            state.infoMessage?.let { msg ->
                item {
                    CalendarBanner(
                        message = msg,
                        isError = false,
                        onDismiss = { viewModel.clearMessages() }
                    )
                }
            }

            item {
                MainCalendarCard(
                    month = state.currentMonth.ifBlank { "This Month" },
                    anchorDate = state.currentDate,
                    events = state.calendarEvents,
                    isLoading = state.isLoading,
                    onPrev = { viewModel.goToPreviousMonth() },
                    onNext = { viewModel.goToNextMonth() }
                )
            }

            item {
                SyllabusTrackerSection(targets = state.syllabusTargets)
            }

            item {
                QuickUploadCard(onImportClick = { viewModel.syncSyllabus() })
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
private fun CalendarHeaderSection(
    isLoading: Boolean,
    onSyncSyllabus: () -> Unit,
    onShowHolidays: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(
                "Academic Calendar",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Plan working days, holidays and syllabus targets across the year.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSyncSyllabus,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Syllabus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShowHolidays,
                enabled = !isLoading,
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
private fun CalendarBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
    val onContainer = if (isError) MaterialTheme.colorScheme.onErrorContainer
                      else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (isError) Icons.Default.ErrorOutline else Icons.Default.Info,
                contentDescription = null,
                tint = onContainer
            )
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = onContainer
            )
            TextButton(onClick = onDismiss) {
                Text("DISMISS", fontWeight = FontWeight.Bold, color = onContainer)
            }
        }
    }
}

@Composable
private fun MainCalendarCard(
    month: String,
    anchorDate: String,
    events: List<CalendarEventDto>,
    isLoading: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = onPrev,
                        enabled = !isLoading,
                        modifier = Modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month") }
                    Text(month, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = onNext,
                        enabled = !isLoading,
                        modifier = Modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) { Icon(Icons.Default.ChevronRight, contentDescription = "Next month") }
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

            // Real month grid, computed from the visible anchor month and
            // overlaid with the real events returned by the API. No mock data.
            val grid = remember(anchorDate, events) { buildMonthGrid(anchorDate, events) }

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
                if (grid.isEmpty()) {
                    // No anchor date resolved yet — keep the header but show a
                    // neutral empty row instead of fabricated days.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isLoading) "Loading…" else "No calendar data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    grid.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                            week.forEach { cell ->
                                CalendarDayCell(cell = cell, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One cell in the month grid. `dayOfMonth == 0` means the cell belongs to the
 * leading/trailing padding (outside the visible month) and is rendered blank.
 */
private data class CalendarCell(
    val dayOfMonth: Int,
    val eventTitle: String? = null
)

/**
 * Builds a 7-column month grid for the month anchored by [anchorIso]
 * (YYYY-MM-DD). Each day that has a matching event in [events] gets its
 * title attached so the cell can render a real marker. Returns an empty list
 * when [anchorIso] cannot be parsed (no fabricated fallback).
 */
private fun buildMonthGrid(
    anchorIso: String,
    events: List<CalendarEventDto>
): List<List<CalendarCell>> {
    val parts = anchorIso.split("-")
    if (parts.size < 3) return emptyList()
    val year = parts[0].toIntOrNull() ?: return emptyList()
    val month = parts[1].toIntOrNull() ?: return emptyList()
    if (month !in 1..12) return emptyList()

    val daysInMonth = daysInMonth(year, month)
    val firstWeekday = dayOfWeek(year, month, 1) // 0=Sun..6=Sat

    // Map day-of-month -> first event title that falls on it (this month/year).
    val eventsByDay = HashMap<Int, String>()
    val prefix = "$year-${month.toString().padStart(2, '0')}-"
    for (e in events) {
        if (!e.date.startsWith(prefix)) continue
        val dom = e.date.split("-").getOrNull(2)?.take(2)?.toIntOrNull() ?: continue
        if (dom in 1..daysInMonth && !eventsByDay.containsKey(dom)) {
            eventsByDay[dom] = e.eventTitle
        }
    }

    val cells = ArrayList<CalendarCell>()
    repeat(firstWeekday) { cells.add(CalendarCell(0)) } // leading padding
    for (d in 1..daysInMonth) cells.add(CalendarCell(d, eventsByDay[d]))
    while (cells.size % 7 != 0) cells.add(CalendarCell(0)) // trailing padding

    return cells.chunked(7)
}

/** Days in a given month, accounting for leap years. */
private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}

/**
 * Day of week for a date using Zeller-style Sakamoto algorithm.
 * Returns 0=Sunday .. 6=Saturday (matches the SUN..SAT header order).
 */
private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var y = year
    if (month < 3) y -= 1
    return (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
}

@Composable
private fun CalendarDayCell(cell: CalendarCell, modifier: Modifier = Modifier) {
    val hasEvent = cell.eventTitle != null
    Box(
        modifier = modifier
            .height(80.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(
                if (cell.dayOfMonth == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                else if (hasEvent) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                else Color.White
            )
            .padding(4.dp)
    ) {
        if (cell.dayOfMonth != 0) {
            Text(
                cell.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasEvent) FontWeight.Bold else FontWeight.Normal
            )
            if (hasEvent) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        cell.eventTitle!!.uppercase(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
private fun QuickUploadCard(onImportClick: () -> Unit) {
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
                    .clickable { onImportClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("Sync calendar and syllabus", color = Color.White, style = MaterialTheme.typography.labelLarge)
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
            color = StatusColors.critical // Rose
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
