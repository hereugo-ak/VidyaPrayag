package com.littlebridge.enrollplus.ui.v2.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.admin.presentation.RegistrationOnboardingViewModel
import com.littlebridge.enrollplus.feature.admin.presentation.RegistrationOnboardingViewModel.FlowStep
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VMaterialDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VMaterialTimePicker
import com.littlebridge.enrollplus.ui.v2.components.VSheetPicker
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.ui.v2.theme.shapeCard
import com.littlebridge.enrollplus.ui.v2.theme.shapePill
import com.littlebridge.enrollplus.ui.components.VBackHeader as LegacyBackHeader
import com.littlebridge.enrollplus.ui.components.VButton as LegacyButton
import com.littlebridge.enrollplus.ui.components.VButtonVariant as LegacyButtonVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.littlebridge.enrollplus.util.parseIsoDate
import com.littlebridge.enrollplus.util.todayIso
import org.koin.compose.viewmodel.koinViewModel

private data class FieldError(val field: String, val message: String)

private fun validateName(name: String): String? {
    if (name.isBlank()) return "Name is required"
    if (name.trim().length < 2) return "Name must be at least 2 characters"
    if (!name.trim().all { it.isLetter() || it == ' ' || it == '.' || it == '-' }) return "Name must contain only letters"
    return null
}

private val emailPattern = Regex("^[A-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$", RegexOption.IGNORE_CASE)

private fun validateEmail(email: String): String? {
    if (email.isBlank()) return "Email is required"
    val value = email.trim()
    val localPart = value.substringBefore('@', missingDelimiterValue = "")
    if (
        value.length > 254 ||
        localPart.length !in 1..64 ||
        localPart.startsWith('.') ||
        localPart.endsWith('.') ||
        value.contains("..") ||
        !emailPattern.matches(value)
    ) return "Enter a valid email address"
    return null
}

private fun validatePhone(phone: String): String? {
    if (phone.isBlank()) return "Phone number is required"
    if (phone.length != 10) return "Phone must be exactly 10 digits"
    if (!phone.all { it.isDigit() }) return "Phone must contain only digits"
    if (phone.first() !in '6'..'9') return "Enter a valid Indian mobile number"
    return null
}

private fun validatePassword(password: String): String? {
    if (password.isBlank()) return "Password is required"
    if (password.length < 8) return "Must be at least 8 characters"
    if (password.length > 128) return "Must be 128 characters or fewer"
    if (!password.any { it.isUpperCase() }) return "Must contain an uppercase letter"
    if (!password.any { it.isLowerCase() }) return "Must contain a lowercase letter"
    if (!password.any { it.isDigit() }) return "Must contain a number"
    if (!password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) return "Must contain a special character"
    return null
}

private fun validateConfirmPassword(password: String, confirm: String): String? {
    if (confirm.isBlank()) return "Please confirm your password"
    if (password != confirm) return "Passwords do not match"
    return null
}

private fun validateSchoolName(name: String): String? {
    if (name.isBlank()) return "School name is required"
    if (name.trim().length < 3) return "School name must be at least 3 characters"
    return null
}

private fun validatePrincipalName(name: String): String? {
    if (name.isBlank()) return "Principal name is required"
    if (!name.trim().all { it.isLetter() || it == ' ' || it == '.' || it == '-' }) return "Name must contain only letters"
    return null
}

private fun validatePrincipalPhone(phone: String): String? {
    if (phone.isBlank()) return "Principal phone is required"
    if (phone.length != 10) return "Phone must be exactly 10 digits"
    if (!phone.all { it.isDigit() }) return "Phone must contain only digits"
    if (phone.first() !in '6'..'9') return "Enter a valid Indian mobile number"
    return null
}

@Composable
private fun VChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val bg = if (selected) c.accent else c.cream
    val fg = if (selected) c.card else c.ink2
    val border = if (selected) c.accent else c.hairline

    Box(
        modifier = Modifier
            .clip(VTheme.dimens.shapePill)
            .background(bg)
            .border(1.dp, border, VTheme.dimens.shapePill)
            .clickable { onClick() }
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = VTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold).colored(fg),
        )
    }
}

@Composable
private fun VChipGroup(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { opt ->
            VChip(text = opt, selected = selected == opt, onClick = { onSelect(opt) })
        }
    }
}

@Composable
private fun VSectionLabel(text: String) {
    Text(
        text = text,
        style = VTheme.type.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp).colored(VTheme.colors.ink3),
    )
}

@Composable
private fun StepHeader(
    title: String,
    subtitle: String,
    currentStep: Int,
    totalSteps: Int = 4,
) {
    val c = VTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            repeat(totalSteps) { index ->
                val isCompleted = index < currentStep
                val isCurrent = index == currentStep
                val color = when {
                    isCompleted -> c.accent
                    isCurrent -> c.accent
                    else -> c.hairline
                }
                val width = if (isCurrent) 24.dp else 8.dp
                Box(
                    modifier = Modifier.height(4.dp).width(width).clip(VTheme.dimens.shapePill).background(color),
                )
            }
        }
        Text(
            text = title,
            style = VTheme.type.h2.copy(fontWeight = FontWeight.ExtraBold).colored(c.ink),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
            Text(
            text = subtitle,
            style = VTheme.type.body.colored(c.ink2),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BackHeader(onBack: () -> Unit) {
    LegacyBackHeader(onBack = onBack)
}

@Composable
private fun SignInFooter(onSignIn: () -> Unit) {
    val c = VTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            text = "Already have an account?",
            style = VTheme.type.caption.colored(c.ink3),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        LegacyButton(
            text = "Sign in",
            onClick = onSignIn,
            variant = LegacyButtonVariant.Outline,
        )
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val c = VTheme.colors
    val strength = remember(password) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) score++
        score
    }
    val color = when (strength) {
        0 -> c.hairline
        1 -> c.danger
        2 -> c.warning
        3 -> c.teal
        4 -> c.successInk
        else -> c.hairline
    }
    val label = when (strength) {
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Strong"
        else -> ""
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            Box(modifier = Modifier.weight(1f).height(3.dp).clip(VTheme.dimens.shapePill).background(if (index < strength) color else c.hairline))
        }
        if (label.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = VTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold).colored(color))
        }
    }
}

@Composable
private fun PasswordRequirement(text: String, met: Boolean) {
    val c = VTheme.colors
    val color = if (met) c.successInk else c.ink3
    val icon = if (met) "✓" else "○"
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = icon, style = VTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold).colored(color), modifier = Modifier.size(16.dp), textAlign = TextAlign.Center)
        Text(text = text, style = VTheme.type.caption.copy(fontSize = 13.sp).colored(color))
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Main screen
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SchoolRegistrationFlow(
    onNavigateToLogin: () -> Unit,
    onOnboardingComplete: () -> Unit,
    viewModel: RegistrationOnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val stepOrder = listOf(FlowStep.One, FlowStep.Two, FlowStep.Three, FlowStep.Four, FlowStep.Success)
    var previousStepIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(VTheme.colors.background).statusBarsPadding()) {
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = {
                val oldIdx = stepOrder.indexOf(previousStepIndex.let { stepOrder.getOrElse(it) { state.step } })
                val newIdx = stepOrder.indexOf(targetState)
                val dir = if (newIdx >= oldIdx) 1 else -1
                previousStepIndex = newIdx

                (slideInHorizontally(tween(350)) { it * dir } + fadeIn(tween(250)))
                    .togetherWith(slideOutHorizontally(tween(350)) { -it * dir } + fadeOut(tween(200)))
            },
            label = "step-transition",
        ) { step ->
            when (step) {
                FlowStep.One -> StepOneBasicDetails(viewModel, onNavigateToLogin)
                FlowStep.Two -> StepTwoCreatePassword(viewModel, onNavigateToLogin)
                FlowStep.Three -> StepThreeSchoolIdentity(viewModel)
                FlowStep.Four -> StepFourAcademicYear(viewModel)
                FlowStep.Success -> SuccessScreen(viewModel, onOnboardingComplete)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Step 1: Basic Details
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepOneBasicDetails(
    viewModel: RegistrationOnboardingViewModel,
    onSignIn: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors
    var validationErrors by remember { mutableStateOf<List<FieldError>>(emptyList()) }
    fun getError(field: String): String? = validationErrors.find { it.field == field }?.message

    fun validateAndSubmit() {
        val errors = mutableListOf<FieldError>()
        validateName(state.adminName)?.let { errors.add(FieldError("name", it)) }
        if (state.adminRole.isBlank()) errors.add(FieldError("role", "Select your role"))
        validateEmail(state.email)?.let { errors.add(FieldError("email", it)) }
        validatePhone(state.contactPhone)?.let { errors.add(FieldError("phone", it)) }
        if (errors.isNotEmpty()) { validationErrors = errors; return }
        viewModel.submitBasicDetails {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(onBack = onSignIn)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StepHeader(title = "Basic Details", subtitle = "Tell us about yourself and your school", currentStep = 0)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VInput(
                    label = "Your Full Name", value = state.adminName,
                    onValueChange = { viewModel.update { s -> s.copy(adminName = it) }; validationErrors = validationErrors.filter { it.field != "name" } },
                    placeholder = "Dr. Rajesh Sharma",
                    isError = getError("name") != null, errorText = getError("name"),
                )
                VSheetPicker(
                    label = "Your Role", value = state.adminRole,
                    options = listOf("Principal", "Vice Principal", "Administrator", "Director", "Manager"),
                    onSelect = { viewModel.update { s -> s.copy(adminRole = it) }; validationErrors = validationErrors.filter { it.field != "role" } },
                    placeholder = "Select your role", searchable = true,
                )
                if (getError("role") != null) Text(text = getError("role")!!, style = VTheme.type.caption.colored(c.dangerInk))
                VInput(
                    label = "Email Address", value = state.email,
                    onValueChange = { viewModel.update { s -> s.copy(email = it) }; validationErrors = validationErrors.filter { it.field != "email" } },
                    placeholder = "principal@dps.edu.in", keyboardType = KeyboardType.Email,
                    isError = getError("email") != null, errorText = getError("email"),
                )
                VInput(
                    label = "Phone Number", value = state.contactPhone,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) { viewModel.update { s -> s.copy(contactPhone = it) }; validationErrors = validationErrors.filter { it.field != "phone" } } },
                    placeholder = "98765 43210", keyboardType = KeyboardType.Phone,
                    isError = getError("phone") != null, errorText = getError("phone"),
                )
                if (state.error != null) Text(text = state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
                LegacyButton(
                    text = "Continue", onClick = { validateAndSubmit() },
                    enabled = state.adminName.isNotBlank() && state.email.isNotBlank() && state.contactPhone.isNotBlank(),
                    loading = state.isLoading,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                )
                SignInFooter(onSignIn = onSignIn)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Step 2: Create Password
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepTwoCreatePassword(
    viewModel: RegistrationOnboardingViewModel,
    onSignIn: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<List<FieldError>>(emptyList()) }
    fun getError(field: String): String? = validationErrors.find { it.field == field }?.message

    fun validateAndSubmit() {
        val errors = mutableListOf<FieldError>()
        validatePassword(state.password)?.let { errors.add(FieldError("password", it)) }
        validateConfirmPassword(state.password, state.confirmPassword)?.let { errors.add(FieldError("confirm", it)) }
        if (errors.isNotEmpty()) { validationErrors = errors; return }
        viewModel.createAccount {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(onBack = { viewModel.goBack() })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StepHeader(title = "Create Password", subtitle = "Set a strong password for your account", currentStep = 1)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VInput(
                    label = "Create Password", value = state.password,
                    onValueChange = { value ->
                        if (value.length <= 128) {
                            viewModel.update { s -> s.copy(password = value) }
                            validationErrors = validationErrors.filter { it.field != "password" }
                        }
                    },
                    placeholder = "Min. 8 characters", keyboardType = KeyboardType.Password,
                    isPassword = true, passwordVisible = passwordVisible,
                    isError = getError("password") != null, errorText = getError("password"),
                    trailing = { Box(Modifier.size(48.dp).clickable { passwordVisible = !passwordVisible }, contentAlignment = Alignment.Center) { Icon(VIcons.Eye, contentDescription = "Toggle password", tint = c.ink3, modifier = Modifier.size(18.dp)) } },
                )
                if (state.password.isNotBlank()) PasswordStrengthBar(state.password)
                VInput(
                    label = "Confirm Password", value = state.confirmPassword,
                    onValueChange = { value ->
                        if (value.length <= 128) {
                            viewModel.update { s -> s.copy(confirmPassword = value) }
                            validationErrors = validationErrors.filter { it.field != "confirm" }
                        }
                    },
                    placeholder = "Re-enter password", keyboardType = KeyboardType.Password,
                    isPassword = true, passwordVisible = confirmPasswordVisible,
                    isError = getError("confirm") != null || (state.confirmPassword.isNotBlank() && state.password != state.confirmPassword),
                    errorText = getError("confirm") ?: if (state.confirmPassword.isNotBlank() && state.password != state.confirmPassword) "Passwords do not match" else null,
                    trailing = { Box(Modifier.size(48.dp).clickable { confirmPasswordVisible = !confirmPasswordVisible }, contentAlignment = Alignment.Center) { Icon(VIcons.Eye, contentDescription = "Toggle password", tint = c.ink3, modifier = Modifier.size(18.dp)) } },
                )
                Column(
                    modifier = Modifier.fillMaxWidth().clip(VTheme.dimens.shapeCard).background(c.accentTint).border(1.dp, c.accent.copy(alpha = 0.15f), VTheme.dimens.shapeCard).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Password requirements", style = VTheme.type.caption.copy(fontWeight = FontWeight.Bold).colored(c.ink))
                    PasswordRequirement("At least 8 characters", state.password.length >= 8)
                    PasswordRequirement("Uppercase and lowercase letters", state.password.any { it.isUpperCase() } && state.password.any { it.isLowerCase() })
                    PasswordRequirement("One number (0–9)", state.password.any { it.isDigit() })
                    PasswordRequirement("One special character (!@#\$%)", state.password.any { !it.isLetterOrDigit() && !it.isWhitespace() })
                }
                if (state.error != null) Text(text = state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
                LegacyButton(
                    text = "Create Account", onClick = { validateAndSubmit() },
                    enabled = state.password.length >= 8 && state.password == state.confirmPassword,
                    loading = state.isLoading,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                )
                SignInFooter(onSignIn = onSignIn)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Step 3: School Identity
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepThreeSchoolIdentity(
    viewModel: RegistrationOnboardingViewModel,
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors
    var validationErrors by remember { mutableStateOf<List<FieldError>>(emptyList()) }
    fun getError(field: String): String? = validationErrors.find { it.field == field }?.message

    fun validateAndSubmit() {
        val errors = mutableListOf<FieldError>()
        validateSchoolName(state.schoolName)?.let { errors.add(FieldError("schoolName", it)) }
        if (state.board.isBlank()) errors.add(FieldError("board", "Select a board"))
        if (state.schoolType.isBlank()) errors.add(FieldError("schoolType", "Select school type"))
        if (state.principalName.isNotBlank()) validatePrincipalName(state.principalName)?.let { errors.add(FieldError("principalName", it)) }
        if (state.principalPhone.isNotBlank()) validatePrincipalPhone(state.principalPhone)?.let { errors.add(FieldError("principalPhone", it)) }
        if (errors.isNotEmpty()) { validationErrors = errors; return }
        viewModel.submitSchoolIdentity {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(onBack = { viewModel.goBack() })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StepHeader(title = "School Identity", subtitle = "Tell us about your school", currentStep = 2)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VInput(
                    label = "Full legal name", value = state.schoolName,
                    onValueChange = { viewModel.update { s -> s.copy(schoolName = it) }; validationErrors = validationErrors.filter { it.field != "schoolName" } },
                    placeholder = "Delhi Public School",
                    isError = getError("schoolName") != null, errorText = getError("schoolName"),
                )
                VInput(label = "Short name", value = state.shortName, onValueChange = { viewModel.update { s -> s.copy(shortName = it) } }, placeholder = "DPS")
                VInput(label = "Affiliation number", value = state.affiliationNumber, onValueChange = { viewModel.update { s -> s.copy(affiliationNumber = it) } }, placeholder = "1234567")
                VSectionLabel("BOARD")
                VChipGroup(
                    options = listOf("CBSE", "ICSE", "UP State", "Other"), selected = state.board,
                    onSelect = { viewModel.update { s -> s.copy(board = it) }; validationErrors = validationErrors.filter { it.field != "board" } },
                )
                if (getError("board") != null) Text(text = getError("board")!!, style = VTheme.type.caption.colored(c.dangerInk))
                VSectionLabel("SCHOOL TYPE")
                VChipGroup(options = listOf("Government", "Private Aided", "Private Unaided", "Central"), selected = state.schoolType, onSelect = { viewModel.update { s -> s.copy(schoolType = it) }; validationErrors = validationErrors.filter { it.field != "schoolType" } })
                if (getError("schoolType") != null) Text(text = getError("schoolType")!!, style = VTheme.type.caption.colored(c.dangerInk))
                VInput(
                    label = "Principal's name", value = state.principalName,
                    onValueChange = { viewModel.update { s -> s.copy(principalName = it) }; validationErrors = validationErrors.filter { it.field != "principalName" } },
                    placeholder = "Dr. Rajesh Sharma",
                    isError = getError("principalName") != null, errorText = getError("principalName"),
                )
                VInput(
                    label = "Principal's mobile", value = state.principalPhone,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) { viewModel.update { s -> s.copy(principalPhone = it) }; validationErrors = validationErrors.filter { it.field != "principalPhone" } } },
                    placeholder = "98765 43210", keyboardType = KeyboardType.Phone,
                    isError = getError("principalPhone") != null, errorText = getError("principalPhone"),
                )
                VSheetPicker(
                    label = "City", value = state.city,
                    options = listOf("New Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad", "Pune", "Ahmedabad", "Jaipur", "Lucknow", "Kanpur", "Varanasi", "Meerut", "Noida", "Ghaziabad", "Gurugram"),
                    onSelect = { viewModel.update { s -> s.copy(city = it) } }, placeholder = "Select city", searchable = true,
                )
                if (state.error != null) Text(text = state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
            }
            LegacyButton(
                text = "Continue", onClick = { validateAndSubmit() },
                enabled = state.schoolName.isNotBlank() && state.board.isNotBlank(),
                loading = state.isLoading,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            LegacyButton(
                text = "Back", onClick = { viewModel.goBack() },
                variant = LegacyButtonVariant.Outline,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Step 4: Academic Year
// ════════════════════════════════════════════════════════════════════════════

private fun academicYearOptions(): List<String> {
    val currentYear = parseIsoDate(todayIso())?.first ?: return emptyList()
    return listOf(currentYear, currentYear + 1).map { start ->
        "$start-${((start + 1) % 100).toString().padStart(2, '0')}"
    }
}

@Composable
private fun StepFourAcademicYear(
    viewModel: RegistrationOnboardingViewModel,
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors
    var validationErrors by remember { mutableStateOf<List<FieldError>>(emptyList()) }
    fun getError(field: String): String? = validationErrors.find { it.field == field }?.message

    fun validateAndSubmit() {
        val errors = mutableListOf<FieldError>()
        if (state.academicYearLabel.isBlank()) errors.add(FieldError("year", "Select an academic year"))
        if (state.yearStartDate.isBlank()) errors.add(FieldError("startDate", "Select start date"))
        if (state.yearEndDate.isBlank()) errors.add(FieldError("endDate", "Select end date"))
        if (state.yearStartDate.isNotBlank() && state.yearEndDate.isNotBlank() && state.yearStartDate >= state.yearEndDate) {
            errors.add(FieldError("endDate", "End date must be after start date"))
        }
        if (state.workingDays.isBlank()) errors.add(FieldError("workingDays", "Select working days"))
        if (state.periodsPerDay.isBlank()) errors.add(FieldError("periods", "Select periods per day"))
        val startH = state.schoolStartTime.substringBefore(":").toIntOrNull() ?: 0
        val startM = state.schoolStartTime.substringAfter(":").take(2).ifBlank { "0" }.toIntOrNull() ?: 0
        val endH = state.schoolEndTime.substringBefore(":").toIntOrNull() ?: 0
        val endM = state.schoolEndTime.substringAfter(":").take(2).ifBlank { "0" }.toIntOrNull() ?: 0
        if (startH > endH || (startH == endH && startM >= endM)) {
            errors.add(FieldError("endTime", "End time must be after start time"))
        }
        if (errors.isNotEmpty()) { validationErrors = errors; return }
        viewModel.submitAcademicYear {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(onBack = { viewModel.goBack() })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StepHeader(title = "Academic Year", subtitle = "Set up your academic calendar", currentStep = 3)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VSectionLabel("CURRENT ACADEMIC YEAR")
                VChipGroup(
                    options = academicYearOptions(), selected = state.academicYearLabel,
                    onSelect = { viewModel.update { s -> s.copy(academicYearLabel = it) }; validationErrors = validationErrors.filter { it.field != "year" } },
                )
                if (getError("year") != null) Text(text = getError("year")!!, style = VTheme.type.caption.colored(c.dangerInk))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        VMaterialDatePicker(value = state.yearStartDate, onValueChange = { viewModel.update { s -> s.copy(yearStartDate = it) }; validationErrors = validationErrors.filter { it.field != "startDate" } }, label = "Year starts", placeholder = "Select date")
                        if (getError("startDate") != null) Text(text = getError("startDate")!!, style = VTheme.type.caption.colored(c.dangerInk), modifier = Modifier.padding(top = 4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        VMaterialDatePicker(value = state.yearEndDate, onValueChange = { viewModel.update { s -> s.copy(yearEndDate = it) }; validationErrors = validationErrors.filter { it.field != "endDate" } }, label = "Year ends", placeholder = "Select date")
                        if (getError("endDate") != null) Text(text = getError("endDate")!!, style = VTheme.type.caption.colored(c.dangerInk), modifier = Modifier.padding(top = 4.dp))
                    }
                }
                VSectionLabel("WORKING DAYS")
                VChipGroup(options = listOf("Mon-Fri", "Mon-Sat"), selected = state.workingDays, onSelect = { viewModel.update { s -> s.copy(workingDays = it) }; validationErrors = validationErrors.filter { it.field != "workingDays" } })
                if (getError("workingDays") != null) Text(text = getError("workingDays")!!, style = VTheme.type.caption.colored(c.dangerInk))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        val startHour = state.schoolStartTime.substringBefore(":").toIntOrNull() ?: 8
                        val startMinute = state.schoolStartTime.substringAfter(":").take(2).ifBlank { "00" }
                        VMaterialTimePicker(
                            hour = startHour, minute = startMinute,
                            onHourChange = { h -> viewModel.update { s -> val m = s.schoolStartTime.substringAfter(":").take(2).ifBlank { "00" }; s.copy(schoolStartTime = "${h.toString().padStart(2, '0')}:$m") } },
                            onMinuteChange = { m -> viewModel.update { s -> val h = s.schoolStartTime.substringBefore(":").padStart(2, '0').ifBlank { "08" }; s.copy(schoolStartTime = "$h:$m") } },
                            label = "Start time",
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val endHour = state.schoolEndTime.substringBefore(":").toIntOrNull() ?: 14
                        val endMinute = state.schoolEndTime.substringAfter(":").take(2).ifBlank { "00" }
                        VMaterialTimePicker(
                            hour = endHour, minute = endMinute,
                            onHourChange = { h -> viewModel.update { s -> val m = s.schoolEndTime.substringAfter(":").take(2).ifBlank { "00" }; s.copy(schoolEndTime = "${h.toString().padStart(2, '0')}:$m") } },
                            onMinuteChange = { m -> viewModel.update { s -> val h = s.schoolEndTime.substringBefore(":").padStart(2, '0').ifBlank { "14" }; s.copy(schoolEndTime = "$h:$m") } },
                            label = "End time",
                        )
                        if (getError("endTime") != null) Text(text = getError("endTime")!!, style = VTheme.type.caption.colored(c.dangerInk), modifier = Modifier.padding(top = 4.dp))
                    }
                }
                VSheetPicker(
                    label = "Periods per day", value = state.periodsPerDay,
                    options = listOf("4", "5", "6", "7", "8", "9", "10", "11", "12"),
                    onSelect = { viewModel.update { s -> s.copy(periodsPerDay = it) }; validationErrors = validationErrors.filter { it.field != "periods" } }, placeholder = "Select periods",
                )
                if (getError("periods") != null) Text(text = getError("periods")!!, style = VTheme.type.caption.colored(c.dangerInk))
                if (state.error != null) Text(text = state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
            }
            Spacer(Modifier.height(8.dp))
            LegacyButton(
                text = "Register School", onClick = { validateAndSubmit() },
                enabled = state.academicYearLabel.isNotBlank(),
                loading = state.isLoading,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            LegacyButton(
                text = "Back", onClick = { viewModel.goBack() },
                variant = LegacyButtonVariant.Outline,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Success screen
// ════════════════════════════════════════════════════════════════════════════

private enum class SuccessArtifact { Command, Admissions, Pews, Fees, Comms }

private data class SuccessFeature(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val artifact: SuccessArtifact,
)

@Composable
private fun SuccessScreen(
    viewModel: RegistrationOnboardingViewModel,
    onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val c = VTheme.colors
    val features = remember {
        listOf(
            SuccessFeature("Command Desk", "A calm, live view of your entire school.", Color(0xFF6546E8), SuccessArtifact.Command),
            SuccessFeature("Admissions CRM", "Move every applicant forward with clarity.", Color(0xFF22B982), SuccessArtifact.Admissions),
            SuccessFeature("PEWS Alerts", "See risk early and intervene with confidence.", Color(0xFFF34D6D), SuccessArtifact.Pews),
            SuccessFeature("Fee Collection", "Real-time collections without spreadsheet work.", Color(0xFFE6A400), SuccessArtifact.Fees),
            SuccessFeature("Communication Hub", "Reach every family from one trusted channel.", Color(0xFF1BA9E8), SuccessArtifact.Comms),
        )
    }
    val pagerState = rememberPagerState(pageCount = { features.size })
    val congrats = remember { Animatable(0f) }
    val headline = remember { Animatable(0f) }
    val subtitle = remember { Animatable(0f) }
    val carousel = remember { Animatable(0f) }
    val dots = remember { Animatable(0f) }
    val bottom = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        listOf(
            150L to congrats,
            300L to headline,
            450L to subtitle,
            600L to carousel,
            750L to dots,
            900L to bottom,
        ).forEach { (delayMillis, animation) ->
            launch {
                delay(delayMillis)
                animation.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enroll", style = VTheme.type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold).colored(c.ink))
            Text("+", style = VTheme.type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold).colored(c.accent))
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                "CONGRATULATIONS",
                style = VTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp).colored(c.accent),
                modifier = Modifier.premiumEntrance(congrats.value),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your school is ready to run beautifully.",
                style = VTheme.type.h2.copy(fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold).colored(c.ink),
                modifier = Modifier.premiumEntrance(headline.value),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "A focused operating system for your team, families and every school day.",
                style = VTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp).colored(c.ink3),
                modifier = Modifier.premiumEntrance(subtitle.value),
            )
        }
        Spacer(Modifier.height(18.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(286.dp).premiumEntrance(carousel.value),
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
        ) { page ->
            val offset = ((page - pagerState.currentPage) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val distance = kotlin.math.abs(offset)
            SuccessFeatureCard(
                feature = features[page],
                modifier = Modifier.graphicsLayer {
                    scaleX = 1f - 0.08f * distance
                    scaleY = 1f - 0.08f * distance
                    alpha = 1f - 0.4f * distance
                },
            )
        }
        Row(
            Modifier.fillMaxWidth().height(28.dp).premiumEntrance(dots.value),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(features.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    Modifier.padding(horizontal = 3.dp).height(6.dp).width(if (selected) 22.dp else 6.dp)
                        .clip(CircleShape).background(if (selected) c.accent else c.hairline),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).premiumEntrance(bottom.value),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(1.dp, c.hairline, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                    Text(state.schoolName.trim().firstOrNull()?.uppercase() ?: "S", color = c.card, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(state.schoolName.ifBlank { "Your school" }, style = VTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold).colored(c.ink), maxLines = 1)
                    Text("Workspace activated", style = VTheme.type.caption.copy(fontSize = 11.sp).colored(c.ink3))
                }
                Row(
                    Modifier.clip(VTheme.dimens.shapePill).background(c.successInk.copy(alpha = .1f)).padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c.successInk))
                    Text("ONBOARDED", style = VTheme.type.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold).colored(c.successInk))
                }
            }
            LegacyButton(
                text = "Enter your command desk",
                onClick = { viewModel.completeOnboarding(onComplete) },
                loading = state.isLoading,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
            if (state.error != null) Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Your setup checklist is waiting on the dashboard.", style = VTheme.type.caption.copy(fontSize = 11.sp).colored(c.ink3), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun Modifier.premiumEntrance(progress: Float): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * 14f
}

@Composable
private fun SuccessFeatureCard(feature: SuccessFeature, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(c.card)
            .border(1.dp, feature.accent.copy(alpha = .16f), RoundedCornerShape(24.dp)),
    ) {
        Box(
            Modifier.fillMaxWidth().height(194.dp)
                .background(Brush.linearGradient(listOf(feature.accent.copy(alpha = .9f), feature.accent))),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(176.dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)))
            SuccessArtifactView(feature.artifact)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(feature.accent))
                Text(feature.title, style = VTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold).colored(c.ink))
            }
            Spacer(Modifier.height(4.dp))
            Text(feature.subtitle, style = VTheme.type.caption.copy(fontSize = 12.sp).colored(c.ink3), maxLines = 2)
        }
    }
}

@Composable
private fun SuccessArtifactView(type: SuccessArtifact) {
    when (type) {
        SuccessArtifact.Command -> CommandArtifact()
        SuccessArtifact.Admissions -> AdmissionsArtifact()
        SuccessArtifact.Pews -> PewsArtifact()
        SuccessArtifact.Fees -> FeesArtifact()
        SuccessArtifact.Comms -> CommsArtifact()
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .17f))
            .border(1.dp, Color.White.copy(alpha = .24f), RoundedCornerShape(16.dp)).padding(14.dp),
        content = content,
    )
}

@Composable
private fun CommandArtifact() {
    Box(Modifier.fillMaxSize().padding(26.dp)) {
        GlassPanel(Modifier.fillMaxWidth().align(Alignment.Center)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(VIcons.Sparkles, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Box(Modifier.width(42.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = .35f)))
            }
            Spacer(Modifier.height(18.dp))
            repeat(3) { index ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = .25f + index * .08f)))
                    Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = .65f)))
                }
                if (index < 2) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AdmissionsArtifact() {
    GlassPanel(Modifier.widthIn(max = 250.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
        repeat(3) { index ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = .28f)), contentAlignment = Alignment.Center) {
                    Icon(VIcons.User, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth(if (index == 1) .72f else .9f).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = .72f)))
                    Box(Modifier.fillMaxWidth(.5f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = .3f)))
                }
                Icon(VIcons.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            if (index < 2) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PewsArtifact() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Color.White.copy(alpha = .2f), -90f, 360f, false, style = Stroke(10.dp.toPx()))
                drawArc(Color.White, -90f, 250f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            }
            Icon(VIcons.AlertTriangle, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(.9f, .65f, .4f).forEach { width -> Box(Modifier.width(72.dp * width).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = .75f))) }
            Text("EARLY SIGNALS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun FeesArtifact() {
    GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 26.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(VIcons.Wallet, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text("LIVE COLLECTIONS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f))) {
            Box(Modifier.fillMaxWidth(.72f).fillMaxHeight().clip(CircleShape).background(Color.White))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("RECEIPTS", "RECONCILIATION").forEach {
                Text(
                    it,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = .16f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun CommsArtifact() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(false, true, false).forEachIndexed { index, sent ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (sent) Arrangement.End else Arrangement.Start) {
                Column(
                    Modifier.fillMaxWidth(if (index == 1) .62f else .72f).clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = if (sent) .32f else .18f)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = .8f)))
                    Box(Modifier.fillMaxWidth(.65f).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = .4f)))
                }
            }
        }
    }
}
