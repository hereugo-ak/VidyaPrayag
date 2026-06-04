package com.littlebridge.vidyaprayag.ui.screens.admin

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalProfileViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.GalleryImage
import com.littlebridge.vidyaprayag.ui.components.*
import com.littlebridge.vidyaprayag.ui.media.MediaPickType
import com.littlebridge.vidyaprayag.ui.media.rememberMediaPicker
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionalProfileScreen() {
    val viewModel: InstitutionalProfileViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface backend error / info messages to the user instead of swallowing them.
    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Real device file pickers → binary upload to Supabase Storage. The single
    // picker callback routes the result by which "slot" the user tapped, so we
    // reuse one launcher for both the gallery photo and the tour video.
    var pendingSlot by remember { mutableStateOf<String?>(null) }
    val pickPhoto = rememberMediaPicker { picked ->
        pendingSlot = null
        if (picked != null) viewModel.uploadGalleryPhoto(picked)
    }
    val pickVideo = rememberMediaPicker { picked ->
        pendingSlot = null
        if (picked != null) viewModel.uploadTourVideo(picked)
    }

    BaseScreen(
        bottomBar = {
            SchoolDashboardBottomBar(selectedTab = SchoolTab.PROFILE)
        }
    ) { paddingValues, scrollModifier ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier)
                    .padding(paddingValues),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item {
                    AnimatedEntrance(delayMillis = staggerDelay(0)) {
                        ProfileHeaderSection(
                            isPublic = state.isPublic,
                            onTogglePublic = viewModel::togglePublic
                        )
                    }
                }

                item {
                    AnimatedEntrance(delayMillis = staggerDelay(1)) {
                        PhilosophyForm(
                            mission = state.missionStatement,
                            onMissionChange = viewModel::updateMission,
                            learningModel = state.learningModel,
                            onModelChange = viewModel::updateLearningModel,
                            language = state.primaryLanguage,
                            onLanguageChange = viewModel::updateLanguage,
                            isSaving = state.isSaving,
                            onSave = viewModel::saveProfile
                        )
                    }
                }

                item {
                    AnimatedEntrance(delayMillis = staggerDelay(2)) {
                        VirtualTourPreview(
                            tourName = state.activeTourName.ifBlank { "No active tour" },
                            previewImageUrl = state.galleryImages.firstOrNull()?.url,
                            isUploading = state.isUploading,
                            onAddTour = {
                                pendingSlot = "tour"
                                pickVideo(MediaPickType.VIDEO)
                            }
                        )
                    }
                }

                item {
                    AnimatedEntrance(delayMillis = staggerDelay(3)) {
                        GallerySection(
                            images = state.galleryImages,
                            storageUsage = state.storageUsage,
                            storageUsedHuman = state.storageUsedHuman,
                            totalStorageHuman = state.totalStorageHuman,
                            isUploading = state.isUploading,
                            onAddPhoto = {
                                pendingSlot = "photo"
                                pickPhoto(MediaPickType.IMAGE)
                            },
                            onRemovePhoto = { url ->
                                val remaining = state.galleryImages.map { it.url }.filterNot { it == url }
                                viewModel.saveGallery(remaining)
                            }
                        )
                    }
                }

                item {
                    AnimatedEntrance(delayMillis = staggerDelay(4)) {
                        ShowcaseHealthCard(completion = state.profileCompletion)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

}

@Composable
private fun ProfileHeaderSection(isPublic: Boolean, onTogglePublic: (Boolean) -> Unit) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // weight(1f) lets the title shrink first so it never squeezes the
                // toggle below — fixes the clipped "P U B L I C" wrap on narrow
                // phones (report §4.3). (false) keeps text from stealing all space.
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text("Institutional Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Manage your school\'s digital presence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isPublic) "PUBLIC" else "PRIVATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Switch(
                            checked = isPublic,
                            onCheckedChange = onTogglePublic,
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhilosophyForm(
    mission: String,
    onMissionChange: (String) -> Unit,
    learningModel: String,
    onModelChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Pedagogical Philosophy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            OnboardingTextField(
                label = "CORE MISSION STATEMENT",
                value = mission,
                onValueChange = onMissionChange,
                placeholder = "Enter your school\'s primary educational mission...",
                singleLine = false,
                minLines = 3
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LEARNING MODEL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = learningModel,
                        onValueChange = onModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRIMARY LANGUAGE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = language,
                        onValueChange = onLanguageChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }

            VidyaPrayagPrimaryButton(
                text = if (isSaving) "SAVING..." else "UPDATE PHILOSOPHY",
                onClick = { if (!isSaving) onSave() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VirtualTourPreview(
    tourName: String,
    previewImageUrl: String?,
    isUploading: Boolean,
    onAddTour: () -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Virtual Tour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (previewImageUrl != null) {
                    NetworkImage(
                        model = previewImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.6f
                    )
                } else {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text("CURRENT ACTIVE TOUR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, fontSize = 8.sp, color = Color.Black)
                        Text(tourName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onAddTour,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UPLOADING…", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UPLOAD TOUR VIDEO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GallerySection(
    images: List<GalleryImage>,
    storageUsage: Float,
    storageUsedHuman: String,
    totalStorageHuman: String,
    isUploading: Boolean,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Gallery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Text("${images.size} / 50", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            if (images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photos yet. Add your first one below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Render the real gallery images in a responsive wrapping grid.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    images.chunked(3).forEach { rowImages ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowImages.forEach { img ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Gray)
                                ) {
                                    NetworkImage(
                                        model = img.url,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        onClick = { onRemovePhoto(img.url) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                            // Fill empty cells to keep alignment consistent.
                            repeat(3 - rowImages.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddPhoto,
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isUploading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("UPLOADING…", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("UPLOAD PHOTO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("STORAGE USAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("$storageUsedHuman / $totalStorageHuman", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                LinearProgressIndicator(
                    progress = { storageUsage },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShowcaseHealthCard(completion: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Showcase Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            val hint = when {
                completion >= 100 -> "Your profile is complete. Nice work!"
                else -> "Your profile is $completion% complete. Fill the mission, learning model, language, add a gallery photo and a tour video to reach 100%."
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            LinearProgressIndicator(
                progress = { completion / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
