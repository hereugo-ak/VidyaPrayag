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
import com.littlebridge.vidyaprayag.feature.admin.presentation.DepartmentProgress
import com.littlebridge.vidyaprayag.feature.admin.presentation.LaggingAlert
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicMilestone
import com.littlebridge.vidyaprayag.feature.admin.presentation.SyllabusCoverageViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusCoverageScreen() {
    val viewModel: SyllabusCoverageViewModel = koinViewModel()
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
                SyllabusHeader()
            }

            item {
                IntelligenceGraphCard(state.departmentStats)
            }

            item {
                GrowthInsightsCard()
            }

            item {
                ProgressRingsRow(state.departmentProgress)
            }

            item {
                LaggingPerformanceSection(state.alerts)
            }

            item {
                MilestonesSection(state.milestones)
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun SyllabusHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Syllabus Coverage",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Curriculum tracking & predictive completion modeling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IntelligenceGraphCard(stats: List<Float>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Syllabus Intelligence Graph", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Predictive completion modeling", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.secondary)
                }
            }

            // Mock Graph
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val labels = listOf("Science", "Math", "Arts", "Languages", "History")
                stats.forEachIndexed { index, value ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(value)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthInsightsCard() {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1A2B48) // Deep Navy
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Growth Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Our AI analyzed 42,000 data points to optimize your curriculum timeline.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Generate Audit", fontWeight = FontWeight.Bold)
                }
            }
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida/ADBb0ug5uSTvAE_12_BQv9mfA3RYnXO7WeFeeM3JBLcOlibkJdUGs26Q8C2oWHF5YR9_n87IIjcPEKsO37q7JFIlxZ48EafBzpxgDZ3s3q5WJDrSnF2RLeA6ouAojYLOIRljQEum_zAnLYsG16W4uFCWT_ASrvVMjX-VgSObU1uWc-OSH_EqmkqtYJ-HQqe_9tDhkqWK_s6Z5CLWv_2b1juizCY5fr_XaXVZOZM4I5S2lSCVnSEEwM32ywDXiQyB",
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ProgressRingsRow(progressList: List<DepartmentProgress>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        progressList.forEach { dept ->
            VidyaPrayagCard(modifier = Modifier.width(160.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                        CircularProgressIndicator(
                            progress = { dept.progress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (dept.isDelayed) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary,
                            strokeWidth = 8.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${(dept.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dept.isDelayed) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dept.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(dept.trend, style = MaterialTheme.typography.labelSmall, color = if (dept.isDelayed) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LaggingPerformanceSection(alerts: List<LaggingAlert>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color.Red)
            Text("Lagging Performance Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        alerts.forEach { alert ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(if (alert.isCritical) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(alert.className.filter { it.isDigit() || it.isLetter() }.take(3), fontWeight = FontWeight.Bold, color = if (alert.isCritical) Color.Red else Color(0xFFF59E0B))
                        }
                        Column {
                            Text("${alert.subject} - ${alert.className}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("${alert.delayPercentage}% Behind Schedule • Instructor: ${alert.instructor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonesSection(milestones: List<AcademicMilestone>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.EventNote, null, tint = MaterialTheme.colorScheme.secondary)
            Text("Upcoming Academic Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        milestones.forEach { milestone ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (milestone.isVerified) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f) else Color.White,
                border = BorderStroke(1.dp, if (milestone.isVerified) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                        Text(milestone.month.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (milestone.isVerified) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
                        Text(milestone.day, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(milestone.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(milestone.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
