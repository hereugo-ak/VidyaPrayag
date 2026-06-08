package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
 * AdminAuthScreenV2 — the credential sign-in for **school staff** (PHASE 4 & 5).
 *
 * Handles BOTH `SCHOOL_ADMIN` and `TEACHER` from one screen: the role is resolved server-side from
 * the JWT, not chosen here. The user enters an email / school-ID / teacher-credential, then a
 * password. Copy is "School Administration Login" with zero mention of parents (LAW 3: no role
 * leakage) — there is no portal selector and no OTP affordance.
 *
 * Wiring: binds to the real [AuthViewModel]. On mount we set the role hint to ADMIN so the
 * credential (email/password) flow is selected by the repository's `checkUser`. `onContinue()`
 * advances to the password step; `onSubmit()` performs the login. The actual landing role
 * (SCHOOL_ADMIN vs TEACHER) is then decided by NavGraphV2 from the persisted JWT role (PHASE 7).
 */
@Composable
fun AdminAuthScreenV2(
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var passwordVisible by remember { mutableStateOf(false) }

    // Clear any state left over from a prior session (the AuthViewModel is reused across a
    // logout → landing → login round-trip) so the email/password fields start blank, then pin
    // to the ADMIN credential flow; the real role comes from the JWT post-login.
    LaunchedEffect(Unit) {
        viewModel.reset()
        viewModel.onRoleChanged("ADMIN")
    }
    LaunchedEffect(state.isAuthSuccessful) { if (state.isAuthSuccessful) onAuthSuccess() }

    AuthScaffoldV2(
        title = "School Administration",
        subtitle = "Sign in with your staff credentials to manage your institution.",
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
                    label = "Email or staff ID",
                    placeholder = "office@svm.edu.in  ·  SVM001.T07",
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
                    passwordVisible = passwordVisible,
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        Icon(
                            VIcons.Eye,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = c.ink3,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { passwordVisible = !passwordVisible },
                        )
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Forgot Password?",
                    style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.align(Alignment.End),
                )
            }
            AuthStep.SignupDetails -> {
                // New school admin creating an account (email signup path).
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
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Create password",
                    placeholder = "••••••••",
                    leadingIcon = VIcons.Lock,
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Staff never use OTP — keep the branch exhaustive but safe.
            AuthStep.Otp -> {
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
        }

        Spacer(Modifier.height(24.dp))

        val ctaLabel = when (state.step) {
            AuthStep.Identifier -> "Continue"
            AuthStep.SignupDetails -> "Create account"
            else -> "Sign In"
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
            AuthBackLink(onClick = viewModel::goBack, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
