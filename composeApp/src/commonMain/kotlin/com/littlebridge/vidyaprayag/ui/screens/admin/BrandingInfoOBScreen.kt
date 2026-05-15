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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.admin.presentation.BrandingInfoOBViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandingInfoOBScreen() {
    val viewModel: BrandingInfoOBViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        onBackClick = { navigator.goBack() },
        bottomBar = {
            OnboardingBottomBar(
                onSaveDraft = { /* Save draft */ },
                onContinue = { navigator.navigateTo(Destination.AcademicInfoOB) }
            )
        }
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
                StepProgressHeader(currentStep = 2, totalSteps = 4, currentLabel = "Branding")
            }

            item {
                Column {
                    Text(
                        "Branding & Showcase",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Define how your institution is perceived by prospective students and parents.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                CoverPhotoSection(
                    imageUrl = state.coverImageUrl,
                    onUploadClick = { /* Handle upload */ }
                )
            }

            item {
                LogoSection(
                    logoUrl = state.logoUrl,
                    onUploadClick = { /* Handle upload */ }
                )
            }

            item {
                MissionVisionSection(
                    mission = state.pedagogicalMission,
                    onMissionChange = viewModel::updatePedagogicalMission,
                    vision = state.visionStatement,
                    onVisionChange = viewModel::updateVisionStatement
                )
            }

            item {
                VirtualTourSection(
                    url = state.virtualTourUrl,
                    onUrlChange = viewModel::updateVirtualTour
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun CoverPhotoSection(imageUrl: String?, onUploadClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CAMPUS COVER PHOTO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(21f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clickable { onUploadClick() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                    Text("Change Cover Image", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Text("Recommended size: 1920x820px (Max 5MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LogoSection(logoUrl: String?, onUploadClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SCHOOL LOGO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable { onUploadClick() },
            contentAlignment = Alignment.Center
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            }
        }
        Text("Circular or square format (Max 2MB)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MissionVisionSection(
    mission: String,
    onMissionChange: (String) -> Unit,
    vision: String,
    onVisionChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        OnboardingTextField(
            label = "PEDAGOGICAL MISSION",
            value = mission,
            onValueChange = onMissionChange,
            placeholder = "Describe your primary teaching philosophy and educational goals...",
            trailingIcon = Icons.Default.AutoAwesome,
            singleLine = false,
            minLines = 4
        )

        OnboardingTextField(
            label = "VISION STATEMENT",
            value = vision,
            onValueChange = onVisionChange,
            placeholder = "Where do you see your students in 10 years? Share your long-term vision...",
            trailingIcon = Icons.Default.Visibility,
            singleLine = false,
            minLines = 4
        )
    }
}

@Composable
private fun VirtualTourSection(url: String, onUrlChange: (String) -> Unit) {
    EduTrustCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("True3D Virtual Tour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Enable parents to explore your classrooms and facilities from anywhere in the world.", style = MaterialTheme.typography.bodySmall)
            
            OnboardingTextField(
                label = "MATTERPORT OR CUSTOM 3D LINK",
                value = url,
                onValueChange = onUrlChange,
                placeholder = "https://my.matterport.com/show/?m=...",
                trailingIcon = Icons.Default.Link
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Matterport", "Kuula", "YouTube VR").forEach { tag ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(tag, fontSize = 10.sp) },
                        shape = CircleShape
                    )
                }
            }
        }
    }
}
