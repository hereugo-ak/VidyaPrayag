package com.littlebridge.vidyaprayag.ui.screens.parent

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import com.littlebridge.vidyaprayag.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentDashboardScreen() {
    val viewModel: ParentDashboardViewModel = koinViewModel()
    val schoolsState by viewModel.schools.collectAsState()
    val shortlist by viewModel.shortlist.collectAsState()

    BaseScreen(
        bottomBar = {
            VidyaPrayagBottomBar(
                items = listOf(
                    BottomNavItem("Discover", Icons.Default.Search, isSelected = true),
                    BottomNavItem("Compare", Icons.AutoMirrored.Filled.CompareArrows),
                    BottomNavItem("Dashboard", Icons.Default.Analytics),
                    BottomNavItem("Hub", Icons.Default.AccountBalance),
                    BottomNavItem("Admin", Icons.Default.DashboardCustomize)
                )
            )
        }
    ) { paddingValues,scrollModi ->
        when (val state = schoolsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                DashboardContent(
                    schools = state.data,
                    shortlist = shortlist,
                    onToggleShortlist = viewModel::toggleShortlist,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    schools: List<School>,
    shortlist: Set<String>,
    onToggleShortlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HeaderSection(schoolCount = schools.size)
        }

        item {
            FilterSection()
        }

        if (shortlist.isNotEmpty()) {
            item {
                ComparisonShortlistSection(
                    selectedSchools = schools.filter { shortlist.contains(it.id) },
                    totalCount = shortlist.size
                )
            }
        }

        items(schools) { school ->
            SchoolDashboardCard(
                school = school,
                isShortlisted = shortlist.contains(school.id),
                onShortlistToggle = { onToggleShortlist(school.id) }
            )
        }
        
        item {
            ExpertHelpCard()
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HeaderSection(schoolCount: Int) {
    Column {
        Text(
            text = "School Discovery",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Showing $schoolCount premium institutions matching your criteria",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun FilterSection() {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("Silicon Valley, CA") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = CircleShape
        )
        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("IB Curriculum") },
            leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = CircleShape
        )
        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("$20k - $35k") },
            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = CircleShape
        )
        Button(
            onClick = {},
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Filters")
        }
    }
}

@Composable
private fun ComparisonShortlistSection(
    selectedSchools: List<School>,
    totalCount: Int
) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "COMPARISON SHORTLIST",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        selectedSchools.forEachIndexed { index, school ->
                            AsyncImage(
                                model = school.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .offset(x = (index * (-8)).dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text(
                        "$totalCount/3 Selected",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Compare Now")
            }
        }
    }
}

@Composable
private fun SchoolDashboardCard(
    school: School,
    isShortlisted: Boolean,
    onShortlistToggle: () -> Unit
) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AsyncImage(
                    model = school.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (school.isVerified) {
                    Surface(
                        modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VERIFIED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = school.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onShortlistToggle) {
                        Icon(
                            imageVector = if (isShortlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isShortlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetadataItem(label = "SRI SCORE", value = "${school.sriScore}/10", valueColor = MaterialTheme.colorScheme.secondary)
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    MetadataItem(label = "ANNUAL FEES", value = school.feesRange)
                }
                
                Text(
                    text = school.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("One-Tap Inquiry")
                    }
                    OutlinedIconButton(
                        onClick = {},
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = valueColor)
    }
}

@Composable
private fun ExpertHelpCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCBHe2vycVVnMdCIUqKbSVKXJCgHJqLSQWptX7CqqU-xa0o9tu1eawmSjBExpDNB7zqndQBE0Zl3CvROj-CUqWGkSyYJPjXl4sL6MPvcGoCIY-_1jRuyU1-TbAbNGD64MLEhJ0lRuDP4QZwd2F9vO32pnLyYtwPBXAnWr84GxbLFDI__r3Zkzs4c9b7Ri_-t8fvpojKOlmmHj-UoeLuN_1_w9dv6OEHkfJ6444_G5Q4jVSjt3YpCd1sFdiV-4habdFt6PTBQ28nk8gS",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "FEATURED SERVICE",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                "Need Expert Help?",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Book a free 1-on-1 session with our admission specialists.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Book Free Consultation")
            }
        }
    }
}
