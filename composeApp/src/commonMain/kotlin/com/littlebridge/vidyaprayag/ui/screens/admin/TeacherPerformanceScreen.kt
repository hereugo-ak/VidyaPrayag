package com.littlebridge.vidyaprayag.ui.screens.admin

import com.littlebridge.vidyaprayag.ui.theme.StatusColors

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.FacultyAccountability
import com.littlebridge.vidyaprayag.feature.admin.presentation.StarTeacher
import com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherPerformanceViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPerformanceScreen() {
    val viewModel: TeacherPerformanceViewModel = koinViewModel()
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
                FacultyHealthHeader(
                    compliance = state.aggregateCompliance,
                    trend = state.complianceTrend
                )
            }

            item {
                UpdateFrequencyCard(trend = state.syllabusUpdateTrend)
            }

            item {
                SectionHeader(title = "Star Faculty", icon = Icons.Default.MilitaryTech)
            }

            items(state.starFaculty) { teacher ->
                StarFacultyItem(teacher)
            }

            item {
                SectionHeader(title = "Accountability Matrix")
            }

            items(state.accountabilityMatrix) { faculty ->
                AccountabilityRow(faculty)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DepartmentEfficiencyCard(state.deptEfficiencies)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PerformanceForecastingCard()
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
private fun FacultyHealthHeader(compliance: String, trend: String) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.secondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Institutional Faculty Health", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Real-time oversight of teacher engagement and accountability metrics.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        VidyaPrayagCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AGGREGATE COMPLIANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f), letterSpacing = 1.sp)
                    Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                }
                Text(compliance, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(trend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UpdateFrequencyCard(trend: List<Float>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Syllabus Update Frequency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Weekly faculty log consistency (24 Weeks)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Monthly", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Live chart driven by the weekly faculty log series from the API.
            if (trend.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No update-frequency data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    trend.forEach { value ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(value.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StarFacultyItem(teacher: StarTeacher) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                Box {
                    NetworkImage(
                        model = teacher.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ) {
                        Text("#${teacher.rank}", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(teacher.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(teacher.department.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(teacher.score.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                Row {
                    repeat(3) {
                        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountabilityRow(faculty: FacultyAccountability) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1.2f)) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (faculty.riskCorrelation == "High Risk") StatusColors.criticalSoft else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(faculty.initials, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (faculty.riskCorrelation == "High Risk") Color.Red else MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(faculty.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(faculty.department, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${faculty.complianceScore}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = if (faculty.complianceScore < 70) Color.Red else MaterialTheme.colorScheme.onSurface)
                LinearProgressIndicator(
                    progress = { faculty.complianceScore / 100f },
                    modifier = Modifier.width(60.dp).height(4.dp).clip(CircleShape),
                    color = if (faculty.complianceScore < 70) Color.Red else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Surface(
                color = when(faculty.riskCorrelation) {
                    "Stable" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    "High Risk" -> Color.Red.copy(alpha = 0.1f)
                    else -> StatusColors.warning.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = faculty.riskCorrelation.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    color = when(faculty.riskCorrelation) {
                        "Stable" -> MaterialTheme.colorScheme.secondary
                        "High Risk" -> Color.Red
                        else -> StatusColors.warningStrong
                    }
                )
            }
        }
    }
}

@Composable
private fun DepartmentEfficiencyCard(efficiencies: List<com.littlebridge.vidyaprayag.feature.admin.presentation.DeptEfficiency>) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Department Efficiency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                efficiencies.forEach { dept ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dept.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("${dept.percentage}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        }
                        LinearProgressIndicator(
                            progress = { dept.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = if (dept.percentage < 85) StatusColors.warning else MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceForecastingCard() {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("Performance Forecasting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("AI-driven prediction based on current accountability trends.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ForecastBox(label = "PREDICTED", value = "+14%", subValue = "Projected Oct. 2024", modifier = Modifier.weight(1f))
                ForecastBox(label = "RISK", value = "Low", subValue = "98% Confidence", modifier = Modifier.weight(1f))
            }
            
            ComingSoonPill(label = "Forecast export — coming soon", onLight = true)
        }
    }
}

@Composable
private fun ForecastBox(label: String, value: String, subValue: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
            Text(subValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
