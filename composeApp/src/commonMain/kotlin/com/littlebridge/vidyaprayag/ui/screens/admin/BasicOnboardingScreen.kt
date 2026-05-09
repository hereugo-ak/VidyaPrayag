package com.littlebridge.vidyaprayag.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.admin.presentation.BasicOnboardingViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicOnboardingScreen() {
    val viewModel: BasicOnboardingViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        onBackClick = { navigator.goBack() },
        bottomBar = {
            OnboardingBottomBar(
                onSaveDraft = { /* Save draft */ },
                onContinue = { /* Next step */ }
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
                StepProgressHeader(currentStep = 1, totalSteps = 5)
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
private fun StepProgressHeader(currentStep: Int, totalSteps: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )
            Text(
                "Basics",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < currentStep) MaterialTheme.colorScheme.secondary 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepIconItem("Basics", Icons.Default.CorporateFare, isActive = true)
            StepIconItem("Brand", Icons.Default.Palette, isActive = false)
            StepIconItem("Fees", Icons.Default.AccountBalanceWallet, isActive = false)
            StepIconItem("Curriculum", Icons.AutoMirrored.Filled.LibraryBooks, isActive = false)
            StepIconItem("Launch", Icons.Default.RocketLaunch, isActive = false)
        }
    }
}

@Composable
private fun StepIconItem(label: String, icon: ImageVector, isActive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp).alpha(if (isActive) 1f else 0.4f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                        shape = CircleShape,
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
private fun OnboardingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingIcon: ImageVector? = null,
    required: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (required) {
                Text(" *", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            trailingIcon = if (trailingIcon != null) { { Icon(trailingIcon, contentDescription = null) } } else null,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
        )
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
                "Use Current",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { }
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida/ADBb0ujKS0F1JiULtLeeVDpTqgaNyFbwA67q0g2mU5kpdJ3STuxY-9WZXhkeDtjdHaEErpJQ2WGLrgQBMs8LG5ZxDw45A_TiYvX37WedwysCnF5r2iOJHOitbJg5S0uwgXuTeU1jGHtw6cEuOj-pNLPrdwJh92Cr8i2q0fXbSuAv0HWfUBbUGilE3F8PgjdfdfEe3SesTla-LxmAJWgi6JfGq4p3gTnHdmAkzetEsjjgZGiwHlacf8cCz-NRhWs",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
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

@Composable
private fun AchievementBadgePlaceholder() {
    Surface(
        modifier = Modifier.padding(top = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Text("Level 1 Started", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingBottomBar(onSaveDraft: () -> Unit, onContinue: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSaveDraft,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text("Save Draft", color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.weight(2f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Continue")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
