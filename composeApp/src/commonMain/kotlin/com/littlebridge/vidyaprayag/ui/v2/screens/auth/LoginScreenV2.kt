package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthStep
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLogo
import com.littlebridge.vidyaprayag.ui.v2.components.VLogoTone
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * LoginScreenV2 — portal selector + identifier/password/OTP, translated from Auth.tsx → `Login`.
 *
 * Re-binds 1:1 to the existing [AuthViewModel] (`shared/`). The teacher portal is shown as a
 * disabled "Coming soon" chip until the backend teacher-auth route ships (master doc G1) — the
 * UI never wires to a non-existent route.
 */
@Composable
fun LoginScreenV2(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(state.isAuthSuccessful) {
        if (state.isAuthSuccessful) onAuthSuccess()
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = d.lg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(d.xxl))
        VLogo(size = 64.dp, withWord = true, tone = VLogoTone.Navy)
        Spacer(Modifier.height(d.lg))
        Text(
            "Sign in to VidyaSetu",
            style = VTheme.type.h2.colored(c.ink),
            textAlign = TextAlign.Center,
        )
        Text(
            "Choose your portal to continue",
            style = VTheme.type.caption.colored(c.ink2),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = d.xs),
        )
        Spacer(Modifier.height(d.lg))

        // ── Portal selector ────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.sm),
        ) {
            VTag(
                text = "Parent",
                active = state.role == "PARENT",
                onClick = { viewModel.onRoleChanged("PARENT") },
                modifier = Modifier.weight(1f),
            )
            VTag(
                text = "School / Admin",
                active = state.role == "ADMIN",
                onClick = { viewModel.onRoleChanged("ADMIN") },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = d.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.sm),
        ) {
            VTag(text = "Teacher", active = false, onClick = null, modifier = Modifier.weight(1f))
            VBadge(text = "COMING SOON", tone = VBadgeTone.Neutral)
        }

        Spacer(Modifier.height(d.lg))

        // ── Step-driven fields ──────────────────────────────────────────────────
        when (state.step) {
            AuthStep.Identifier -> {
                VInput(
                    value = state.identifier,
                    onValueChange = viewModel::onIdentifierChanged,
                    label = "Email or phone",
                    placeholder = "you@school.edu or 98xxxxxxxx",
                    leadingIcon = VIcons.Mail,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AuthStep.LoginPassword -> {
                VInput(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Password",
                    placeholder = "••••••••",
                    leadingIcon = VIcons.Lock,
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AuthStep.SignupDetails -> {
                VInput(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = "Your name",
                    placeholder = "Full name",
                    leadingIcon = VIcons.User,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(d.md))
                VInput(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Create password",
                    placeholder = "••••••••",
                    leadingIcon = VIcons.Lock,
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AuthStep.Otp -> {
                VInput(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChanged,
                    label = "Verification code",
                    placeholder = "6-digit OTP",
                    leadingIcon = VIcons.Lock,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(d.sm))
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }

        Spacer(Modifier.height(d.lg))
        val isIdentifierStep = state.step == AuthStep.Identifier
        VButton(
            text = if (isIdentifierStep) "Continue" else "Sign in",
            onClick = { if (isIdentifierStep) viewModel.onContinue() else viewModel.onSubmit() },
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            soft = false,
            loading = state.isLoading,
        )
        if (!isIdentifierStep) {
            Spacer(Modifier.height(d.sm))
            VButton(
                text = "Back",
                onClick = viewModel::goBack,
                full = true,
                variant = VButtonVariant.Ghost,
                tone = VButtonTone.Navy,
            )
        }
        Spacer(Modifier.height(d.xl))
    }
}
