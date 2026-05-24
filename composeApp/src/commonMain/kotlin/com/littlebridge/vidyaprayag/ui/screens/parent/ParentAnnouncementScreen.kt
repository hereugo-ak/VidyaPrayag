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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAnnouncement
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAnnouncementViewModel
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAnnouncementScreen() {
    val viewModel: ParentAnnouncementViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    BaseScreen(
        bottomBar = {
            ParentDashboardBottomBar(selectedTab = ParentTab.ANNOUNCEMENT)
        }
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
                AnnouncementsHeader(
                    isWhatsAppEnabled = state.isWhatsAppSyncEnabled,
                    onWhatsAppToggle = viewModel::toggleWhatsAppSync
                )
            }

            item {
                SearchBarSection()
            }

            item {
                FilterChipsSection()
            }

            // Featured Announcement
            val featured = state.announcements.find { it.isFeatured }
            if (featured != null) {
                item {
                    FeaturedAnnouncementCard(featured)
                }
            }

            // Other announcements
            items(state.announcements.filter { !it.isFeatured }) { announcement ->
                AnnouncementCard(announcement)
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AnnouncementsHeader(
    isWhatsAppEnabled: Boolean,
    onWhatsAppToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "School Announcements",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Stay updated with the latest news and events from Aarav's school.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366))
                    Text("Sync to WhatsApp", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = isWhatsAppEnabled,
                    onCheckedChange = onWhatsAppToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
private fun SearchBarSection() {
    OutlinedTextField(
        value = "",
        onValueChange = { },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search announcements...", color = MaterialTheme.colorScheme.outline) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        )
    )
}

@Composable
private fun FilterChipsSection() {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf("All", "Holidays", "PTM", "Events")
        filters.forEach { filter ->
            val isSelected = filter == "All"
            Surface(
                onClick = { },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = filter,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeaturedAnnouncementCard(announcement: ParentAnnouncement) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape) {
                        Text(
                            announcement.category.uppercase(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(announcement.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                }
                Text(announcement.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(announcement.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { }, contentPadding = PaddingValues(0.dp)) {
                    Text("Read more", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
            if (announcement.imageUrl != null) {
                AsyncImage(
                    model = announcement.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: ParentAnnouncement) {
    val isEvent = announcement.category == "Events"
    val isPTM = announcement.category == "PTM"
    
    val containerColor = when {
        isEvent -> MaterialTheme.colorScheme.primaryContainer
        isPTM -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        else -> Color.White
    }
    
    val contentColor = if (isEvent) Color.White else MaterialTheme.colorScheme.onSurface

    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = containerColor
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(
                    color = if (isEvent) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        announcement.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEvent) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = when(announcement.category) {
                        "PTM" -> Icons.Default.Groups
                        "Events" -> Icons.Default.Celebration
                        else -> Icons.Default.Campaign
                    },
                    contentDescription = null,
                    tint = if (isEvent) Color.White else MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(announcement.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
                Text(announcement.date, style = MaterialTheme.typography.labelSmall, color = if (isEvent) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
            }

            Text(announcement.description, style = MaterialTheme.typography.bodyMedium, color = if (isEvent) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant)

            if (isPTM) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Book Slot", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
