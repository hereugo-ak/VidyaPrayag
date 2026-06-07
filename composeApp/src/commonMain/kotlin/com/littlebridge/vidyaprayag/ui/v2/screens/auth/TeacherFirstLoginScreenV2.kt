package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherFirstLoginScreenV2 — the one-time "set a new password" gate, translated from
 * `Auth.tsx → TeacherFirstLogin`.
 *
 * **Backend gap (documented in PHASE_PLAN):** there is no change-password / first-login endpoint in
 * the shared `AuthRepository` and no `must_change_password` flag in the auth schema. `AuthViewModel`
 * only supports identifier+password / OTP login and registration. So this screen performs **local
 * validation only** (current password present, new ≥ 8 chars, confirmation matches) and, on success,
 * calls [onDone] to continue. It does NOT persist a new password — per the hard UI rule we never fake
 * a server write. When a `POST /auth/change-password` (+ `must_change_password`) lands, swap the
 * local `validate()` for a real `viewModel.changePassword(...) { onDone() }`.
 */
@Composable
fun TeacherFirstLoginScreenV2(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    teacherName: String? = "Mr. Vikram",
) = VTheme(tone = VPortalTone.Night) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(VTheme.colors.background)
            // §11 cross-platform — respect the status bar, IME, and gesture bar
            // on both Android and iOS. Order matters: statusBars first, then IME
            // (so the keyboard lifts the content), then navigationBars (so the
            // bottom CTA never sits under the gesture bar).
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(VTheme.dimens.xxl))
        // §5: React `Label` component = labelStrong (11/700/0.10em UPPER), ink3.
        Text(
            if (teacherName.isNullOrBlank()) "Welcome" else "Welcome, $teacherName",
            style = VTheme.type.labelStrong.colored(VTheme.colors.ink3),
        )
        Spacer(Modifier.height(VTheme.dimens.xs))
        // §5: React heading is <h1> (32/800), not h2.
        Text("Set a new password", style = VTheme.type.h1.colored(VTheme.colors.ink))
        Text(
            "For your security, choose a fresh password before continuing. You'll only do this once.",
            style = VTheme.type.body.colored(VTheme.colors.ink2),
            modifier = Modifier.padding(top = VTheme.dimens.xs),
        )

        Spacer(Modifier.height(VTheme.dimens.xl))
        VInput(current, { current = it; error = null }, label = "Current temporary password", placeholder = "••••••••", isPassword = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(VTheme.dimens.md))
        VInput(newPassword, { newPassword = it; error = null }, label = "New password", placeholder = "At least 8 characters", isPassword = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(VTheme.dimens.md))
        VInput(confirm, { confirm = it; error = null }, label = "Confirm new password", placeholder = "Re-enter", isPassword = true, modifier = Modifier.fillMaxWidth())

        if (error != null) {
            Spacer(Modifier.height(VTheme.dimens.sm))
            Text(error!!, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
        }

        Spacer(Modifier.height(VTheme.dimens.xl))
        VButton(
            text = "Update & continue",
            onClick = {
                error = validate(current, newPassword, confirm)
                if (error == null) onDone()
            },
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            soft = false,
            // §5: React places ArrowRight AFTER the label (trailing), not leading.
            trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        Spacer(Modifier.height(VTheme.dimens.md))
        VButton(
            text = "Need help signing in?",
            onClick = {},
            full = true,
            variant = VButtonVariant.Ghost,
            tone = VButtonTone.Navy,
        )
        Spacer(Modifier.height(VTheme.dimens.xl))
    }
}

/** Local-only validation until a real change-password endpoint exists. */
private fun validate(current: String, newPassword: String, confirm: String): String? = when {
    current.isBlank() -> "Enter your current temporary password."
    newPassword.length < 8 -> "New password must be at least 8 characters."
    newPassword != confirm -> "Passwords don't match."
    else -> null
}
