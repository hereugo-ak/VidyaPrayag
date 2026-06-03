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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.admin.presentation.RiskStudent
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SubjectEngagement
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAnalyticsScreen() {
    val viewModel: StudentAnalyticsViewModel = koinViewModel()
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
                StudentAnalyticsHeader()
            }

            item {
                VolatilityIndexCard(trend = state.dailyVolatility)
            }

            item {
                CorrelationInsightsCard()
            }

            item {
                PEWSSection(
                    critical = state.criticalRiskCount,
                    medium = state.mediumRiskCount,
                    low = state.lowRiskCount,
                    atRiskStudents = state.atRiskStudents
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SubjectEngagementCard(state.subjectEngagements)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CohortComparisonCard(state.cohortComparison, state.cohortLabels)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun StudentAnalyticsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Student Tracking",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Analytics Overview: Daily attendance variance & anomalies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VolatilityIndexCard(trend: List<Float>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QueryStats, null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Column {
                        Text("Daily Attendance Volatility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Impact analysis & anomalies", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Anomaly detected", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Live volatility chart: anomaly = the day with the lowest value (biggest drop)
            if (trend.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No volatility data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Detect the anomaly index = the minimum value (largest attendance drop)
                val anomalyIndex = trend.indexOf(trend.minOrNull())
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    trend.forEachIndexed { index, value ->
                        val isAnomaly = index == anomalyIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(value.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (isAnomaly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Predictive Impact: Current volatility suggests a potential 3% drop in monthly syllabus coverage if unaddressed.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CorrelationInsightsCard() {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0F172A) // Dark Navy
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Psychology, null, tint = Color(0xFFFBBF24))
                Text("Correlation Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InsightBox(
                    label = "Attendance vs. Performance",
                    value = "Students with <85% attendance show a 15.4% drop in Math mastery.",
                    color = Color(0xFFFACC15)
                )
                InsightBox(
                    label = "Social Factor",
                    value = "Late arrivals correlate with 22% higher behavioral incident reports.",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun InsightBox(label: String, value: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun PEWSSection(
    critical: Int,
    medium: Int,
    low: Int,
    atRiskStudents: List<RiskStudent>
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PriorityHigh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Column {
                    Text("PEWS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Predictive Early Warning System", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RiskIndicator("Critical Risk", critical, 0.08f, MaterialTheme.colorScheme.error)
                    RiskIndicator("Medium Risk", medium, 0.28f, Color(0xFFF59E0B))
                    RiskIndicator("Low/No Risk", low, 0.64f, MaterialTheme.colorScheme.secondary)
                }
                
                Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Priority Intervention List", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    atRiskStudents.forEach { student ->
                        RiskStudentItem(student)
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskIndicator(label: String, count: Int, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("$count Students", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun RiskStudentItem(student: RiskStudent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = student.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(student.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Retention Risk: ${student.retentionRisk}%", style = MaterialTheme.typography.labelSmall, color = if (student.riskLevel == "Critical") MaterialTheme.colorScheme.error else Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SubjectEngagementCard(engagements: List<SubjectEngagement>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.secondary)
                Text("Subject Engagement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                engagements.forEach { engagement ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(engagement.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(8.dp))
                            Text("${(engagement.percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                        }
                        LinearProgressIndicator(
                            progress = { engagement.percentage },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (engagement.percentage < 0.8f) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CohortComparisonCard(comparison: List<Float>, labels: List<String>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.secondary)
                Text("Cohort Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (comparison.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No cohort data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Highlight the strongest-performing cohort (highest bar)
                val topIndex = comparison.indexOf(comparison.maxOrNull())
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    comparison.forEachIndexed { index, value ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .fillMaxHeight(value.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (index == topIndex) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Text(
                                labels.getOrNull(index) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
