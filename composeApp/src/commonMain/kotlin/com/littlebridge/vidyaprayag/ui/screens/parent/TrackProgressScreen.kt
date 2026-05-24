package com.littlebridge.vidyaprayag.ui.screens.parent

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.parent.presentation.*
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrackProgressScreen() {
    val viewModel: TrackProgressViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    BaseScreen(
        bottomBar = {
            ParentDashboardBottomBar(selectedTab = ParentTab.TRACK_PROGRESS)
        }
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
                ProgressHero(state.overallProgress, state.currentLevel, state.childName, state.journeyDescription)
            }

            item {
                AchievementBadgesRow(state.badges)
            }

            item {
                AcademicCompetenciesSection(state.academicCompetencies)
            }

            item {
                EmotionalIntelligenceCard(state.emotionalIntelligence)
            }

            item {
                PlayBasedSection(state.playIndicators)
            }

            item {
                DetailedReportButton()
            }

            item {
                Text(
                    "Last updated: Today, 10:45 AM",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ProgressHero(progress: Float, level: Int, name: String, description: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            val color1 = Color(0xFF10B981)
            val color2 = Color(0xFF34D399)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx())
                )
                drawArc(
                    brush = Brush.linearGradient(listOf(color1, color2)),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Text(
                        "LEVEL $level REACHED", 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$name's Holistic Journey", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun AchievementBadgesRow(badges: List<AchievementBadge>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        badges.forEach { badge ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .then(
                            if (badge.isLocked) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            else Modifier.background(Brush.linearGradient(badge.gradientColors.map { Color(it) }))
                        )
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when(badge.iconName) {
                            "workspace_premium" -> Icons.Default.WorkspacePremium
                            "auto_stories" -> Icons.Default.AutoStories
                            "rocket_launch" -> Icons.Default.RocketLaunch
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = if (badge.isLocked) MaterialTheme.colorScheme.outline else Color.White
                    )
                }
                Text(badge.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AcademicCompetenciesSection(competencies: List<AcademicCompetency>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Academic Core", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("NEP ALIGNED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            competencies.forEach { comp ->
                VidyaPrayagCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when(comp.iconName) {
                                    "translate" -> Icons.Default.Translate
                                    else -> Icons.Default.Calculate
                                }, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(comp.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { comp.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmotionalIntelligenceCard(metrics: Map<String, Float>) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Emotional Intelligence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            
            // Simplified "Spider" representation for now, or just a grid of chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { (label, value) ->
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            "$label: ${(value * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
            }
            
            Text(
                "Significant growth in Social Interaction this month.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlayBasedSection(indicators: List<PlayIndicator>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Play-based Discovery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                indicators.forEachIndexed { index, indicator ->
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = indicator.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(indicator.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(indicator.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    if (index < indicators.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailedReportButton() {
    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text("View Detailed Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
