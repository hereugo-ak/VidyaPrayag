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
import com.littlebridge.vidyaprayag.ui.location.LocationResult
import com.littlebridge.vidyaprayag.ui.location.rememberLocationProvider
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionalBasicOBScreen() {
    val viewModel: InstitutionalBasicOBViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val navigator = LocalAppNavigator.current

    // "Use current location" → permission → GPS fix → reverse geocode → state.
    var isLocating by remember { mutableStateOf(false) }
    var locationNotice by remember { mutableStateOf<String?>(null) }
    val captureLocation = rememberLocationProvider { result ->
        isLocating = false
        when (result) {
            is LocationResult.Success -> {
                val loc = result.location
                viewModel.applyCapturedLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    fullAddress = loc.fullAddress,
                    city = loc.city,
                    district = loc.district,
                    state = loc.state,
                    pincode = loc.pincode
                )
                locationNotice = "Location captured."
            }
            is LocationResult.PermissionDenied ->
                locationNotice = "Location permission denied. You can type the address instead."
            is LocationResult.Unavailable ->
                locationNotice = result.reason
        }
    }

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
                    address = state.address,
                    onAddressChange = viewModel::updateAddress,
                    hasCoordinates = state.latitude != null && state.longitude != null,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    isLocating = isLocating,
                    locationNotice = locationNotice,
                    onUseCurrentLocation = {
                        locationNotice = null
                        isLocating = true
                        captureLocation()
                    }
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
    address: String,
    onAddressChange: (String) -> Unit,
    hasCoordinates: Boolean,
    latitude: Double?,
    longitude: Double?,
    isLocating: Boolean,
    locationNotice: String?,
    onUseCurrentLocation: () -> Unit
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

        // Real location capture (replaces the old static map placeholder).
        LocationPicker(
            address = address,
            onAddressChange = onAddressChange,
            hasCoordinates = hasCoordinates,
            latitude = latitude,
            longitude = longitude,
            isLocating = isLocating,
            locationNotice = locationNotice,
            onUseCurrentLocation = onUseCurrentLocation
        )
    }
}

@Composable
private fun LocationPicker(
    address: String,
    onAddressChange: (String) -> Unit,
    hasCoordinates: Boolean,
    latitude: Double?,
    longitude: Double?,
    isLocating: Boolean,
    locationNotice: String?,
    onUseCurrentLocation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("School Location", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                if (hasCoordinates) "GPS set" else "Tap to set",
                color = if (hasCoordinates) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Real "Use current location" button → runtime permission → GPS fix →
        // reverse geocode (report §11.2). Falls back gracefully if unavailable.
        Button(
            onClick = onUseCurrentLocation,
            enabled = !isLocating,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            if (isLocating) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Locating…", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (hasCoordinates) "Update current location" else "Use current location", fontWeight = FontWeight.Bold)
            }
        }

        if (hasCoordinates && latitude != null && longitude != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Lat ${latitude.format5()}, Lng ${longitude.format5()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Editable address — pre-filled by reverse geocoding, still manually
        // correctable so onboarding never blocks on an imperfect geocode.
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            label = { Text("Full Address") },
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
            minLines = 2,
            textStyle = MaterialTheme.typography.bodySmall
        )

        if (locationNotice != null) {
            Text(
                locationNotice,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Trim a coordinate to 5 decimal places (~1.1 m) without platform String.format. */
private fun Double.format5(): String {
    val scaled = kotlin.math.round(this * 100000.0) / 100000.0
    return scaled.toString()
}
