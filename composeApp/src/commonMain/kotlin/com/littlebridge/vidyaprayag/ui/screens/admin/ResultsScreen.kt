package com.littlebridge.vidyaprayag.ui.screens.admin

import com.littlebridge.vidyaprayag.ui.theme.StatusColors

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Local, real search filter over the loaded students (by name or id).
    var searchQuery by remember { mutableStateOf("") }
    val visibleStudents = remember(state.students, searchQuery) {
        if (searchQuery.isBlank()) state.students
        else state.students.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
        }
    }

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
            // Test cycle nav driven by real CMS-backed available tests.
            if (state.availableTests.isNotEmpty()) {
                item {
                    AcademicCycleNav(
                        tests = state.availableTests,
                        selectedTest = state.selectedTest,
                        onTestSelect = viewModel::selectTest
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    FilterSection(
                        selectedClass = state.selectedClass,
                        availableClasses = state.availableClasses,
                        onClassSelect = viewModel::selectClass,
                        selectedSubject = state.selectedSubject,
                        availableSubjects = state.availableSubjects,
                        onSubjectSelect = viewModel::selectSubject,
                        onRefresh = {
                            viewModel.loadResults(
                                state.selectedTest, state.selectedClass, state.selectedSubject
                            )
                        }
                    )

                    when {
                        state.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                        state.errorMessage != null -> {
                            ErrorRetryBlock(
                                message = state.errorMessage!!,
                                onRetry = {
                                    viewModel.loadResults(
                                        state.selectedTest, state.selectedClass, state.selectedSubject
                                    )
                                }
                            )
                        }
                        else -> {
                            PerformanceSummarySection(state)

                            ClassAverageTrendCard(
                                average = state.classAverage,
                                trend = state.averageTrend,
                                selectedTest = state.selectedTest
                            )

                            SectionHeader(title = "Student Results")

                            SearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it }
                            )

                            if (visibleStudents.isEmpty()) {
                                EmptyStudentsBlock(hasAny = state.students.isNotEmpty())
                            } else {
                                visibleStudents.forEach { student ->
                                    StudentResultRow(student)
                                }
                                ResultsFooter(
                                    shown = visibleStudents.size,
                                    total = state.students.size
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademicCycleNav(
    tests: List<String>,
    selectedTest: String,
    onTestSelect: (String) -> Unit
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    selectedClass: String,
    availableClasses: List<String>,
    onClassSelect: (String) -> Unit,
    selectedSubject: String,
    availableSubjects: List<String>,
    onSubjectSelect: (String) -> Unit,
    onRefresh: () -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Class",
                    value = selectedClass.ifBlank { "Select" },
                    icon = Icons.Default.School,
                    options = availableClasses,
                    onSelect = onClassSelect
                )
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Subject",
                    value = selectedSubject.ifBlank { "Select" },
                    icon = Icons.Default.Science,
                    options = availableSubjects,
                    onSelect = onSubjectSelect
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (options.isNotEmpty()) expanded = it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun PerformanceSummarySection(state: com.littlebridge.vidyaprayag.feature.admin.presentation.ResultsState) {
    val exceeding = state.exceedingCount
    val meeting = state.meetingCount
    val below = state.belowCount
    val maxCount = maxOf(exceeding, meeting, below, 1)

    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.secondary)
                Text("Performance Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryStatRow("Exceeding", "$exceeding Students", MaterialTheme.colorScheme.secondary)
                    SummaryStatRow("Meeting", "$meeting Students", StatusColors.infoLight)
                    SummaryStatRow("Below", "$below Students", StatusColors.gold)
                }

                // Real distribution chart: bar heights are proportional to the
                // actual exceeding / meeting / below student counts.
                Row(
                    modifier = Modifier.weight(1.5f).height(120.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DistributionBar("Exc", exceeding, maxCount, MaterialTheme.colorScheme.secondary)
                    DistributionBar("Meet", meeting, maxCount, StatusColors.infoLight)
                    DistributionBar("Below", below, maxCount, StatusColors.gold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.DistributionBar(label: String, count: Int, maxCount: Int, color: Color) {
    val fraction = (count.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1f)
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
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
private fun ClassAverageTrendCard(average: String, trend: String, selectedTest: String) {
    val isUp = trend.startsWith("+") && trend.removePrefix("+").removeSuffix("%").toDoubleOrNull()?.let { it > 0.0 } == true
    val isDown = trend.startsWith("-")
    val trendColor = when {
        isUp -> MaterialTheme.colorScheme.secondary
        isDown -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val trendIcon = when {
        isUp -> Icons.AutoMirrored.Filled.TrendingUp
        isDown -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }

    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                null,
                modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp).alpha(0.1f),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CLASS AVERAGE${if (selectedTest.isNotBlank()) " · ${selectedTest.uppercase()}" else ""}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(average, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Text("/ 100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(trendIcon, null, modifier = Modifier.size(16.dp), tint = trendColor)
                    Text(
                        text = "$trend vs previous test",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        placeholder = { Text("Search by name or ID...", fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
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
                NetworkImage(
                    model = student.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(student.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ID: ${student.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
                        if (student.trend.isNotBlank()) {
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
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                if (student.score.isNotEmpty()) {
                    Text(student.score, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                } else {
                    Text("--", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                }
            }

            Surface(
                color = when (student.status) {
                    "Exceeding" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    "Meeting" -> StatusColors.infoLight.copy(alpha = 0.1f)
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
                    color = when (student.status) {
                        "Exceeding" -> MaterialTheme.colorScheme.secondary
                        "Meeting" -> StatusColors.infoStrong
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultsFooter(shown: Int, total: Int) {
    Text(
        text = if (shown == total) "$total student(s)" else "Showing $shown of $total student(s)",
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyStudentsBlock(hasAny: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
            Text(
                if (hasAny) "No students match your search." else "No results published for this selection yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorRetryBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Retry") }
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
