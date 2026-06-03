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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalBasicOBViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionalBasicOBScreen() {
    val viewModel: InstitutionalBasicOBViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        onBackClick = { navigator.goBack() },
        bottomBar = {
            OnboardingBottomBar(
                onSaveDraft = {
                    if (!isSubmitting) viewModel.submit { }
                },
                onContinue = {
                    if (!isSubmitting) {
                        viewModel.submit {
                            navigator.navigateTo(Destination.BrandingInfoOB)
                        }
                    }
                },
                continueText = if (isSubmitting) "Saving..." else "Continue"
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
                StepProgressHeader(currentStep = 1, totalSteps = 4, currentLabel = "Basics")
            }

            if (errorMessage != null) {
                item {
                    OnboardingErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            item {
                Column {
                    Text(
                        "Institutional Basics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Please provide the registered details of your institution to begin the setup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SchoolBasicsForm(
                    schoolName = state.schoolName,
                    onSchoolNameChange = viewModel::updateSchoolName,
                    selectedBoard = state.boardAffiliation,
                    onBoardChange = viewModel::updateBoard,
                    email = state.officialEmail,
                    onEmailChange = viewModel::updateEmail,
                    contactNumber = state.contactNumber,
                    onContactChange = viewModel::updateContact,
                    address = state.address
                )
            }

            item {
                AchievementBadgePlaceholder()
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun SchoolBasicsForm(
    schoolName: String,
    onSchoolNameChange: (String) -> Unit,
    selectedBoard: String,
    onBoardChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    contactNumber: String,
    onContactChange: (String) -> Unit,
    address: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // School Name
        OnboardingTextField(
            label = "Official School Name",
            value = schoolName,
            onValueChange = onSchoolNameChange,
            placeholder = "e.g. St. Xavier\'s International",
            trailingIcon = Icons.Default.School,
            required = true
        )

        // Board Affiliation
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Board Affiliation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("CBSE", "ICSE", "State Board", "IB / IGCSE").forEach { board ->
                    FilterChip(
                        selected = selectedBoard == board,
                        onClick = { onBoardChange(board) },
                        label = { Text(board) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Contact Info
        OnboardingTextField(
            label = "Official Email",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "admin@schoolname.edu",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contact Number", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+91", fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = onContactChange,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("98765 43210") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
            }
        }

        // Location Picker Placeholder
        LocationPicker(address = address)
    }
}

@Composable
private fun LocationPicker(address: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("School Location", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                "Default Campus",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
        ) {
            // Local, deterministic map preview. The previous implementation loaded
            // a temporary Google-hosted image that now returns HTTP 403 in Coil.
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                modifier = Modifier
                    .size(112.dp)
                    .align(Alignment.Center)
            )
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .align(if (index % 2 == 0) Alignment.TopStart else Alignment.BottomEnd)
                        .padding((16 + index * 18).dp)
                        .size((42 + index * 10).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
            
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(address, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Knowledge Hub, Sector 42", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
