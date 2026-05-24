package com.littlebridge.vidyaprayag.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthStep
import com.littlebridge.vidyaprayag.feature.auth.domain.model.AuthFlow
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val viewModel: AuthViewModel = koinViewModel()
    val mainViewModel: MainViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    LaunchedEffect(state.isAuthSuccessful) {
        if (state.isAuthSuccessful) {
            onDismissRequest()
            mainViewModel.setRole(state.role)
            if (state.role == "ADMIN") {
                navigator.navigateTo(Destination.SchoolDashboard)
            } else {
                navigator.navigateTo(Destination.ParentDashboard)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Access VidyaPrayag",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = when(state.step) {
                    AuthStep.Identifier -> "Enter email or phone number"
                    AuthStep.LoginPassword -> "Welcome back! Enter your password"
                    AuthStep.SignupDetails -> "Create your account"
                    AuthStep.Otp -> "Verify your identity"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { step ->
                when (step) {
                    AuthStep.Identifier -> IdentifierSection(
                        identifier = state.identifier,
                        onIdentifierChange = viewModel::onIdentifierChanged,
                        role = state.role,
                        onRoleChange = viewModel::onRoleChanged,
                        isLoading = state.isLoading,
                        onContinue = viewModel::onContinue
                    )
                    AuthStep.LoginPassword -> LoginPasswordSection(
                        password = state.password,
                        onPasswordChange = viewModel::onPasswordChanged,
                        isLoading = state.isLoading,
                        onLogin = viewModel::onSubmit,
                        onBack = viewModel::goBack
                    )
                    AuthStep.SignupDetails -> SignupDetailsSection(
                        name = state.name,
                        onNameChange = viewModel::onNameChanged,
                        password = state.password,
                        onPasswordChange = viewModel::onPasswordChanged,
                        confirmPassword = state.confirmPassword,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
                        isLoading = state.isLoading,
                        onSignup = viewModel::onSubmit,
                        onBack = viewModel::goBack
                    )
                    AuthStep.Otp -> OtpSection(
                        identifier = state.identifier,
                        name = state.name,
                        onNameChange = viewModel::onNameChanged,
                        otp = state.otp,
                        onOtpChange = viewModel::onOtpChanged,
                        showNameField = state.flow == AuthFlow.SIGNUP_PHONE,
                        isLoading = state.isLoading,
                        onVerify = viewModel::onSubmit,
                        onBack = viewModel::goBack
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to VidyaPrayag's Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun IdentifierSection(
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            RoleToggleButton(
                text = "Administrator",
                icon = Icons.Default.SupervisorAccount,
                isSelected = role == "ADMIN",
                onClick = { onRoleChange("ADMIN") },
                modifier = Modifier.weight(1f)
            )
            RoleToggleButton(
                text = "Parent",
                icon = Icons.Default.FamilyRestroom,
                isSelected = role == "PARENT",
                onClick = { onRoleChange("PARENT") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Email or phone number", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        VidyaPrayagPrimaryButton(
            text = if (isLoading) "Checking..." else "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && identifier.isNotBlank()
        )
    }
}

@Composable
private fun LoginPasswordSection(
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onLogin: () -> Unit,
    onBack: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Password", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        VidyaPrayagPrimaryButton(
            text = if (isLoading) "Logging in..." else "Login",
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && password.isNotBlank()
        )

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SignupDetailsSection(
    name: String,
    onNameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onSignup: () -> Unit,
    onBack: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Full Name", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Create Password", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Confirm Password", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        VidyaPrayagPrimaryButton(
            text = if (isLoading) "Creating Account..." else "Create Account",
            onClick = onSignup,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && name.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
        )

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun OtpSection(
    identifier: String,
    name: String,
    onNameChange: (String) -> Unit,
    otp: String,
    onOtpChange: (String) -> Unit,
    showNameField: Boolean,
    isLoading: Boolean,
    onVerify: () -> Unit,
    onBack: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Enter 6-digit code sent to $identifier",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        if (showNameField) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Full Name", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BasicTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(6) { index ->
                        val char = when {
                            index >= otp.length -> ""
                            else -> otp[index].toString()
                        }
                        val isFocused = otp.length == index
                        OtpBox(char, isFocused)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        VidyaPrayagPrimaryButton(
            text = if (isLoading) "Verifying..." else "Verify & Continue",
            onClick = onVerify,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && otp.length == 6 && (!showNameField || name.isNotBlank())
        )

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun OtpBox(char: String, isFocused: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.ifEmpty { "•" },
            style = MaterialTheme.typography.headlineMedium,
            color = if (char.isEmpty()) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleToggleButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}
