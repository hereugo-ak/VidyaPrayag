package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthStep
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * ParentAuthScreenV2 — the parent-scoped sign-in / sign-up (PHASE 4).
 *
 * The parent-only auth surface, locked to the **PARENT** role with the OTP (phone) flow
 * only. There is zero admin UI here (LAW 3: no role leakage) — no portal selector, no password
 * field, no email/credential affordance. Copy speaks only to families.
 *
 * Wiring: binds to the real [AuthViewModel]. On mount we pin the role to PARENT. The phone→OTP flow
 * is exactly the existing repository flow (`onContinue()` triggers `sendOtp`, `onSubmit()` verifies).
 * On `isAuthSuccessful` we call [onAuthSuccess]; the host (NavGraphV2) then routes on child-link
 * state (PHASE 6).
 */
@Composable
fun ParentAuthScreenV2(
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    // Pin the role to PARENT the moment this scoped screen mounts.
    LaunchedEffect(Unit) { viewModel.onRoleChanged("PARENT") }
    LaunchedEffect(state.isAuthSuccessful) { if (state.isAuthSuccessful) onAuthSuccess() }

    AuthScaffoldV2(
        title = "Welcome, parent 👋",
        subtitle = "Sign in with your mobile number to connect with your child's school.",
        error = state.error,
        onBack = onBack,
        modifier = modifier,
    ) {
        val c = VTheme.colors
        when (state.step) {
            AuthStep.Identifier -> {
                VInput(
                    value = state.identifier,
                    onValueChange = viewModel::onIdentifierChanged,
                    label = "Mobile number",
                    placeholder = "+91 98XXX XXXXX",
                    leadingIcon = VIcons.Phone,
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AuthStep.SignupDetails -> {
                // New family: we still collect a display name (phone signup asks for name with OTP).
                VInput(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = "Your name",
                    placeholder = "Full name",
                    leadingIcon = VIcons.User,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AuthStep.Otp -> {
                // Phone signup needs a name alongside the OTP; show it inline for new families.
                // (For an existing-user login the field stays empty and is ignored by the VM.)
                VInput(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = "Your name",
                    placeholder = "Full name",
                    leadingIcon = VIcons.User,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                VInput(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChanged,
                    label = "OTP",
                    placeholder = "6-digit code",
                    leadingIcon = VIcons.ShieldCheck,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "We sent a code to ${state.identifier.ifBlank { "your phone" }}.",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            // Parents never see the password step — but keep the branch exhaustive & safe.
            AuthStep.LoginPassword -> {
                VInput(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChanged,
                    label = "OTP",
                    placeholder = "6-digit code",
                    leadingIcon = VIcons.ShieldCheck,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        val ctaLabel = when (state.step) {
            AuthStep.Identifier -> "Send OTP"
            AuthStep.Otp -> "Verify & Continue"
            else -> "Continue"
        }
        VButton(
            text = ctaLabel,
            onClick = { if (state.step == AuthStep.Identifier) viewModel.onContinue() else viewModel.onSubmit() },
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            loading = state.isLoading,
            trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )

        if (state.step != AuthStep.Identifier) {
            Spacer(Modifier.height(8.dp))
            AuthBackLink(onClick = viewModel::goBack, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
        }
    }
}
