package com.littlebridge.vidyaprayag.ui.screens.admin

import com.littlebridge.vidyaprayag.ui.theme.StatusColors

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.littlebridge.vidyaprayag.feature.admin.presentation.Announcement
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolAnnouncementsViewModel
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolAnnouncementsScreen() {
    val viewModel: SchoolAnnouncementsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadAnnouncements() }

    BaseScreen(
        bottomBar = {
            SchoolDashboardBottomBar(selectedTab = SchoolTab.ANNOUNCEMENTS)
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
                SearchBarSection(
                    query = query,
                    onQueryChange = {
                        query = it
                        viewModel.searchAnnouncements(it)
                    }
                )
            }

            state.errorMessage?.let { msg ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearMessages() }) { Text("Dismiss") }
                        }
                    }
                }
            }

            item {
                FilterChipsSection(
                    selectedCategory = state.selectedCategory,
                    onSelect = { viewModel.setCategoryFilter(it) }
                )
            }

            // Featured Announcement (Bento Style)
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

        // Floating Create Announcement action (overlay bottom-end).
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            FloatingActionButton(
                onClick = { showCreate = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New announcement", tint = Color.White)
            }
        }

        if (showCreate) {
            CreateAnnouncementDialog(
                isCreating = state.isCreating,
                onDismiss = { showCreate = false },
                onCreate = { type, title, description, date ->
                    viewModel.createAnnouncement(
                        type = type,
                        title = title,
                        description = description,
                        date = date,
                        onCreated = { showCreate = false }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAnnouncementDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (type: String, title: String, description: String, date: String) -> Unit
) {
    var type by remember { mutableStateOf("Update") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    val types = listOf("Update", "Holidays", "PTM", "Events", "Reminder")

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("New announcement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isCreating && title.isNotBlank() && description.isNotBlank() && date.isNotBlank(),
                onClick = { onCreate(type, title.trim(), description.trim(), date.trim()) }
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
        }
    )
}

@Composable
private fun AnnouncementsHeader(
    isWhatsAppEnabled: Boolean,
    onWhatsAppToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "School Announcements",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                )
                Text(
                    "Stay updated with the latest news, events, and important meetings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = StatusColors.whatsApp)
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
private fun SearchBarSection(
    query: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search announcements...", color = MaterialTheme.colorScheme.outline) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun FilterChipsSection(
    selectedCategory: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf("All", "Holidays", "PTM", "Events")
        filters.forEach { filter ->
            // "All" is selected when no category filter is active; otherwise
            // case-insensitive match against the server-side category.
            val isSelected = when {
                filter.equals("All", ignoreCase = true) -> selectedCategory.isNullOrBlank()
                else -> filter.equals(selectedCategory, ignoreCase = true)
            }
            Surface(
                onClick = {
                    if (filter.equals("All", ignoreCase = true)) onSelect(null)
                    else onSelect(filter)
                },
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
private fun FeaturedAnnouncementCard(announcement: Announcement) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ) {
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(announcement.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(announcement.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Full schedule details are shown in this announcement.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (announcement.imageUrl != null) {
                NetworkImage(
                    model = announcement.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 24.dp, vertical = 0.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    val isEvent = announcement.category == "Events"
    val isReminder = announcement.category == "Reminder"
    
    val containerColor = when {
        isEvent -> MaterialTheme.colorScheme.primaryContainer
        isReminder -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        else -> Color.White
    }
    
    val contentColor = if (isEvent) Color.White else MaterialTheme.colorScheme.onSurface

    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = containerColor
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
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
                
                if (isReminder) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                } else if (!isEvent) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (announcement.category == "PTM") Icons.Default.Groups else Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(announcement.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
                Text(announcement.date, style = MaterialTheme.typography.labelSmall, color = if (isEvent) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
            }

            Text(announcement.description, style = MaterialTheme.typography.bodyMedium, color = if (isEvent) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant)

            if (isEvent) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Participant avatars mockup
                    Row(modifier = Modifier.weight(1f)) {
                        repeat(3) {
                            Surface(
                                modifier = Modifier.size(32.dp).offset(x = (it * -12).dp),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ('A' + it).toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (announcement.category == "PTM") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ComingSoonPill(label = "Book from PTM module — coming soon")
                }
            }
        }
    }
}
