package com.littlebridge.vidyaprayag.ui.screens.parent

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.ChildBasicInfoViewModel
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChildBasicInfoScreen() {
    val viewModel: ChildBasicInfoViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        onBackClick = { navigator.goBack() },
        immersiveTopBar = true
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
                OnboardingHeader()
            }

            item {
                StepIndicator()
            }

            item {
                VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        OnboardingTextField(
                            label = "CHILD'S FULL NAME",
                            value = state.name,
                            onValueChange = viewModel::updateName,
                            placeholder = "Enter name..."
                        )

                        OnboardingTextField(
                            label = "CURRENT GRADE/LEVEL",
                            value = state.grade,
                            onValueChange = viewModel::updateGrade,
                            placeholder = "Select grade..."
                        )

                        OnboardingTextField(
                            label = "DATE OF BIRTH",
                            value = state.dob,
                            onValueChange = viewModel::updateDob,
                            placeholder = "YYYY-MM-DD"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("CURRENT INTERESTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.availableInterests.forEach { interest ->
                                    val isSelected = state.selectedInterests.contains(interest)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleInterest(interest) },
                                        label = { Text(interest) },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                            selectedLabelColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { navigator.goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skip for now", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { navigator.navigateTo(Destination.YourPreferences) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Continue", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item {
                OnboardingFooter()
            }
        }
    }
}

@Composable
private fun OnboardingHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                Text("Personalized Journey", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text("Let's build a profile for your child.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("We use this information to curate the best learning path, aligning with their developmental milestones.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepIndicator() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Step 1 of 3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Text("Basic Information", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        LinearProgressIndicator(
            progress = { 0.33f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun OnboardingFooter() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 16.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            FooterItem(Icons.Default.VerifiedUser, "Your data is encrypted and secure. We comply with institutional privacy standards.")
        }
    }
}

@Composable
private fun FooterItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
    }
}
