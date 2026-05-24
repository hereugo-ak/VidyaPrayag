package com.littlebridge.vidyaprayag.ui.screens.parent

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.parent.presentation.*
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParentReportsScreen() {
    val viewModel: ParentReportsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    BaseScreen(
        immersiveTopBar = true,
        bottomBar = {
            ParentDashboardBottomBar(selectedTab = ParentTab.TRACK_PROGRESS)
        }
    ) { paddingValues, scrollModi ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModi)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ReportsHeader()
            }

            item {
                AiLearningStoryCard(state.childName, state.termNarrative)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        TermOverviewCard(state.averageScore)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlobalRankCard(state.globalSubjectRank, state.totalStudents)
                    }
                }
            }

            item {
                ImprovementTrajectoryCard(state.improvementTrend, state.monthlyScores)
            }

            item {
                AssessmentHistorySection(state.assessmentHistory)
            }

            item {
                TeacherRemarksAndPews(state.teacherRemarks, state.leadInstructor, state.pewsStatus, state.pewsAlert)
            }

            item {
                EngagementAndSkillTrajectory(state.learningStreak, state.skillTrajectory)
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ReportsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Text("VidyaPrayag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Academic Guardian", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("PREMIUM MEMBER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun AiLearningStoryCard(name: String, narrative: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida/ADBb0uiesddkmz2baGSxcsg_BUUQovOZXC_zhPHmTq2vtyOmjrPQKM95tIg8DHWPW5RBfoqtbyKsu9PIpUtlo555txEJKypL9ms57Q94s5bbnrp4k_haix4SXySTXglHaenpFXK_0RZLNdChNAuFC86RXM2Q38DqkavsDnZn-jXUkeODh9WX1fX30P1ULfAEiRXg20XFK5t4tj6ZGnCFUbU68MvEyUGUhcDnkcJ8RgaPBSbeuKhfN1Od9jv0ZFmW",
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), shape = CircleShape) {
                        Text("AI NARRATIVE REPORT", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("$name's Term Narrative", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("\"$narrative\"", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share to WhatsApp", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TermOverviewCard(avgScore: Int) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(Color.LightGray.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(8.dp.toPx()))
                    drawArc(Color(0xFF10B981), -90f, avgScore * 3.6f, false, style = Stroke(8.dp.toPx()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$avgScore%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("AVG SCORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                }
            }
            Text("Term Overview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Outstanding performance across core STEM subjects.", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun GlobalRankCard(rank: Int, total: Int) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth(), backgroundColor = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida/ADBb0uicrmIvnR_ImEFcEJvW71nMnAc1cD9gIggNKIk3Fdipz8V4xE_zfI2Zww2SPer1jYQJvsQZyBUE3UmMlLp1KpaH7sS3bTU3cWYbO4DVFgqPrN4vXTYGJS7G_dxBzbw6VzI2m-LbUDJG3X_6JH0qZh-36OZfZL4hVpyfCWpiLzDo4qTDldnLlqGpCI7arcKc4c8Xd18YOK8bzLTCqmuXWfLirJQpiulb2IgiZv05gE5HUe6KylH0PLqpV0wU",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column {
                Text("GLOBAL SUBJECT RANK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("#${rank.toString().padStart(2, '0')}", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                    Text("/$total", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 6.dp))
                }
                Text("Top 10% Percentile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ImprovementTrajectoryCard(trend: Float, scores: List<Int>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Improvement Trajectory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Consistent growth over the last 6 months", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Text("+$trend%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }

            // Mock Chart
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May")
                scores.forEachIndexed { index, score ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(score / 100f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (index == scores.lastIndex) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(months.getOrNull(index) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentHistorySection(history: List<AssessmentItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Assessment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            history.forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(item.iconName) {
                                        "functions" -> Icons.Default.Functions
                                        "biotech" -> Icons.Default.Biotech
                                        else -> Icons.AutoMirrored.Filled.MenuBook
                                    },
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(item.subject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Last Assessment: ${item.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${item.score}/${item.totalScore}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
                            Text("CLASS AVG: ${item.classAverage}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherRemarksAndPews(remarks: String, instructor: String, status: String, alert: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        VidyaPrayagCard(modifier = Modifier.weight(1.5f)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Comment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Teacher Remarks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(remarks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("— $instructor", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1f),
            color = Color(0xFFFFF9EB),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                    Text("PEWS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF92400E))
                }
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFDE68A)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PriorityHigh, null, tint = Color(0xFF92400E), modifier = Modifier.size(24.dp))
                }
                Text(status, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                Text(alert, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = Color(0xFFB45309), lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun EngagementAndSkillTrajectory(streak: Int, skills: Map<String, Int>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        VidyaPrayagCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Learning Streak", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("$streak Days", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("New Personal Best!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        VidyaPrayagCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Skill Trajectory", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    skills.forEach { (skill, value) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(skill, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                                Text("$value%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            }
                            LinearProgressIndicator(
                                progress = { value / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = if (value < 70) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
