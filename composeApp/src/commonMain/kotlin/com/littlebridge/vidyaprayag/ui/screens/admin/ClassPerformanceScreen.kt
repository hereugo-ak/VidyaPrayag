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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.ClassPerformanceViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.GradeDistribution
import com.littlebridge.vidyaprayag.feature.admin.presentation.SubjectMatrixItem
import com.littlebridge.vidyaprayag.feature.admin.presentation.ProgressMonitoringItem
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassPerformanceScreen() {
    val viewModel: ClassPerformanceViewModel = koinViewModel()
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
                ClassPerformanceHeader()
            }

            item {
                GradeDistributionCard(state.gradeDistribution, state.avgProficiency, state.activeStudents, state.medianGrade)
            }

            item {
                SubjectComparisonCard(state.subjectMatrix)
            }

            item {
                PEWSAlertCard(state.criticalRiskCount, state.moderateRiskCount)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProficiencyBenchmarkCard(state.proficiencyTargetReach)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TopPerformerCard(state.topPerformerName, state.topPerformerDetails)
                    }
                }
            }

            item {
                SectionHeader(title = "Recent Progress Monitoring")
            }

            items(state.recentProgress) { item ->
                ProgressMonitoringRow(item)
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ClassPerformanceHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            Text("Class-Wise Performance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        Text("Academic Year 2023-24 • Institutional Portal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GradeDistributionCard(distribution: List<GradeDistribution>, avg: String, students: Int, median: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Aggregated Grade Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Distribution across all core subjects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)) {
                        Text("Term 2", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                distribution.forEach { item ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(item.value)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (item.grade == "B") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(item.grade, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryStat(avg, "Avg Proficiency", MaterialTheme.colorScheme.secondary)
                SummaryStat(students.toString(), "Active Students", MaterialTheme.colorScheme.primary)
                SummaryStat(median, "Median Grade", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SubjectComparisonCard(matrix: List<SubjectMatrixItem>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Subject Comparison Matrix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                matrix.forEach { item ->
                    val isRisk = item.percentage < 60
                    Surface(
                        color = if (isRisk) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isRisk) BorderStroke(1.dp, Color(0xFFFEE2E2)) else null
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(item.name, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            LinearProgressIndicator(
                                progress = { item.percentage / 100f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                color = if (isRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                            Text("${item.percentage}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = if (isRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                            Icon(
                                when(item.trend) {
                                    "up" -> Icons.AutoMirrored.Filled.TrendingUp
                                    "down" -> Icons.AutoMirrored.Filled.TrendingDown
                                    else -> Icons.Default.TrendingFlat
                                },
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PEWSAlertCard(critical: Int, moderate: Int) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth(), backgroundColor = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.secondary)
                Text("PEWS INTEGRATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
            }
            Text("Predictive Early Warning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Based on current engagement and quiz scores, the AI model has identified potential risks.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RiskRow("Critical Risk", "$critical Students", MaterialTheme.colorScheme.error)
                RiskRow("Moderate Risk", "$moderate Students", Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
private fun RiskRow(label: String, value: String, color: Color) {
    Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Surface(color = color, shape = CircleShape) {
                Text(value, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ProficiencyBenchmarkCard(reach: Int) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Proficiency Benchmarks", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                val color = MaterialTheme.colorScheme.secondary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(Color.LightGray.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(10.dp.toPx()))
                    drawArc(color, -90f, reach * 3.6f, false, style = Stroke(10.dp.toPx()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$reach%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("TARGET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun TopPerformerCard(name: String, details: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOP PERFORMER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                Icon(Icons.Default.MilitaryTech, null, tint = MaterialTheme.colorScheme.secondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
                }
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ProgressMonitoringRow(item: ProgressMonitoringItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text(item.initials, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = when(item.status) {
                        "EXCELLING" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        "PEWS ALERT" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                ) {
                    Text(item.status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = if (item.status == "PEWS ALERT") MaterialTheme.colorScheme.error else if (item.status == "EXCELLING") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Math", item.math)
                MetricItem("Science", item.science)
                MetricItem("Literature", item.literature)
                MetricItem("Attendance", item.attendance)
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
