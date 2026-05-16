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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.admin.presentation.AnalyticsDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AnalyticsCardData
import com.littlebridge.vidyaprayag.feature.admin.presentation.InsightItem
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen() {
    val viewModel: AnalyticsDashboardViewModel = koinViewModel()
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
                AnalyticsHeader()
            }

            item {
                GlobalPerformanceTrendCard(
                    trend = state.performanceTrend,
                    growth = state.currentGrowth
                )
            }

            items(state.cards) { cardData ->
                AnalyticsDataCard(
                    card = cardData,
                    onClick = {
                        when (cardData.title) {
                            "Student Tracking" -> navigator.navigateTo(Destination.StudentAnalytics)
                            "Syllabus Coverage" -> navigator.navigateTo(Destination.SyllabusCoverage)
                            "Teacher Accountability" -> navigator.navigateTo(Destination.TeacherPerformance)
                            "Class Performance" -> navigator.navigateTo(Destination.ClassPerformance)
                        }
                    }
                )
            }

            item {
                SectionTitle(title = "Key Institutional Insights")
            }

            items(state.insights) { insight ->
                InsightListItem(insight)
            }

            item {
                DownloadReportButton()
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AnalyticsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Institutional Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Central District Administration Dashboard",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlobalPerformanceTrendCard(trend: List<Float>, growth: String) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Global Performance Trend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Academic progress aggregated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(growth, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // Mock Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEachIndexed { index, value ->
                    val isActive = index == 3 // Mock Apr as active
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
                                .background(if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(
                            text = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")[index],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsDataCard(card: AnalyticsCardData, onClick: () -> Unit) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = card.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                }
            }
            
            Column {
                Text(card.title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(card.subValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun InsightListItem(insight: InsightItem) {
    Surface(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(insight.iconColor).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(insight.iconName) {
                        "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
                        "warning" -> Icons.Default.Warning
                        "stars" -> Icons.Default.Stars
                        else -> Icons.Default.Insights
                    },
                    contentDescription = null,
                    tint = Color(insight.iconColor),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(insight.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun DownloadReportButton() {
    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.Default.Download, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download Full Report", fontWeight = FontWeight.Bold)
    }
}
