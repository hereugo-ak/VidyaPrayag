package com.littlebridge.vidyaprayag.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.admin.presentation.ResultsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentResult
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen() {
    val viewModel: ResultsViewModel = koinViewModel()
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
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                AcademicCycleNav(
                    selectedTest = state.selectedTest,
                    onTestSelect = viewModel::selectTest
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    FilterSection(
                        selectedClass = state.selectedClass,
                        selectedSubject = state.selectedSubject
                    )

                    PerformanceSummarySection(state)

                    ClassAverageTrendCard(
                        average = state.classAverage,
                        trend = state.averageTrend
                    )

                    SectionHeader(title = "Student Results")

                    BulkActionsRow()

                    state.students.forEach { student ->
                        StudentResultRow(student)
                    }
                    
                    PaginationFooter()
                }
            }
        }
    }
}

@Composable
private fun AcademicCycleNav(selectedTest: String, onTestSelect: (String) -> Unit) {
    val tests = listOf("Unit Test I", "Unit Test II", "Half-Yearly", "Pre-Board", "Finals")
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            tests.forEach { test ->
                val isSelected = test == selectedTest
                Box(
                    modifier = Modifier
                        .clickable { onTestSelect(test) }
                        .padding(bottom = 4.dp)
                ) {
                    Text(
                        text = test,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 12.dp)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(selectedClass: String, selectedSubject: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterDropdown(label = "Class", value = selectedClass, icon = Icons.Default.School)
                FilterDropdown(label = "Subject", value = selectedSubject, icon = Icons.Default.Science)
            }
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FilterDropdown(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.height(44.dp).fillMaxWidth(0.5f),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PerformanceSummarySection(state: com.littlebridge.vidyaprayag.feature.admin.presentation.ResultsState) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.secondary)
                Text("Performance Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryStatRow("Exceeding", "${state.exceedingCount} Students", MaterialTheme.colorScheme.secondary)
                    SummaryStatRow("Meeting", "${state.meetingCount} Students", Color(0xFF60A5FA))
                    SummaryStatRow("Below", "${state.belowCount} Students", Color(0xFFFBBF24))
                }
                
                // Mock Chart
                Row(modifier = Modifier.weight(1.5f).height(120.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(0.4f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(0.55f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(0.85f).background(Color(0xFF60A5FA), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(0.65f).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(0.3f).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                }
            }
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ClassAverageTrendCard(average: String, trend: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background decoration
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                null,
                modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp).alpha(0.1f),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CLASS AVERAGE TREND", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(average, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Text("/ 100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("$trend vs UT-1 (74.2)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Stars, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Text("Class performance is currently at its highest this session.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkActionsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.weight(1f).height(48.dp),
            placeholder = { Text("Search by name or ID...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudentResultRow(student: StudentResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = student.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(student.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ID: ${student.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                        Icon(
                            imageVector = if (student.trend.startsWith("+")) Icons.AutoMirrored.Filled.TrendingUp else if (student.trend.startsWith("-")) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingFlat,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = if (student.trend.startsWith("+")) MaterialTheme.colorScheme.secondary else if (student.trend.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                        Text(student.trend, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (student.trend.startsWith("+")) MaterialTheme.colorScheme.secondary else if (student.trend.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                if (student.score.isNotEmpty()) {
                    Text(student.score, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                } else {
                    Text("--", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                }
            }

            Surface(
                color = when(student.status) {
                    "Exceeding" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    "Meeting" -> Color(0xFF60A5FA).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = student.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    color = when(student.status) {
                        "Exceeding" -> MaterialTheme.colorScheme.secondary
                        "Meeting" -> Color(0xFF2563EB)
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
private fun PaginationFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Page 1 of 4 (32 Students)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {}, modifier = Modifier.size(32.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))) { Icon(Icons.Default.ChevronLeft, null) }
            IconButton(onClick = {}, modifier = Modifier.size(32.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))) { Icon(Icons.Default.ChevronRight, null) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.outline,
        letterSpacing = 1.5.sp
    )
}
