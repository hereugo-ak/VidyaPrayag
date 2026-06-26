package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherCheckInState
import com.littlebridge.vidyaprayag.platform.BiometricCapability
import com.littlebridge.vidyaprayag.platform.BiometricMethod
import com.littlebridge.vidyaprayag.platform.BiometricResult
import com.littlebridge.vidyaprayag.platform.rememberBiometricAuthenticator
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import kotlinx.coroutines.launch

/**
 * TeacherCheckInPopup — the first-login-of-day fingerprint check-in dialog (Doc 06 §2). It rides as
 * a full-screen scrim overlay above the Home tab on the first open of the day where the teacher has
 * not yet checked in. It is fully CLOSEABLE via the X (the teacher can dismiss and check in later
 * from the Home greeting ring), honouring "never block the manual path".
 *
 * The biometric ladder is the platform concern: if the device has a biometric/credential we prompt;
 * on Success we record the honest method, on Cancel/Failed we fall back to a manual confirm. The VM
 * only ever sees the resolved wire method — it never touches the prompt.
 */
@Composable
fun TeacherCheckInPopup(
    state: TeacherCheckInState,
    visible: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: (method: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val authenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }

    // Once the server confirms a check-in (optimistic reconcile), close the popup automatically.
    LaunchedEffect(state.checkedIn) {
        if (state.checkedIn) onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.navyDeep.copy(alpha = 0.42f))
                // Tap-outside dismisses (still closeable).
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visible = visible, enter = scaleIn(initialScale = 0.92f) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(
                    Modifier
                        .padding(28.dp)
                        // Swallow inner taps so they don't dismiss.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.card)
                        .border(1.dp, c.hairline, RoundedCornerShape(28.dp))
                        .padding(22.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Close (X) — top-right.
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(c.cream)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(VIcons.Close, contentDescription = "Close", tint = c.ink2, modifier = Modifier.size(15.dp))
                            }
                        }

                        // Fingerprint emblem on a soft violet plate.
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(c.accent.copy(alpha = 0.16f), c.accentSoft.copy(alpha = 0.10f))),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (working) {
                                TeacherSpinner(30.dp)
                            } else {
                                Icon(VIcons.ShieldCheck, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(38.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Check in for today",
                            style = VTheme.type.h2.colored(c.navyDeep).copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        val body = when (authenticator.capability) {
                            BiometricCapability.BiometricAvailable -> "Verify with your fingerprint to mark yourself present at school."
                            BiometricCapability.DeviceCredentialOnly -> "Verify with your device PIN to mark yourself present at school."
                            BiometricCapability.None -> "Confirm your arrival to mark yourself present at school."
                        }
                        Text(
                            body,
                            style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.5.sp),
                            textAlign = TextAlign.Center,
                        )
                        val errorText = state.error
                        if (errorText != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(errorText, style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp), textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(18.dp))

                        val primaryLabel = when (authenticator.capability) {
                            BiometricCapability.BiometricAvailable -> "Use fingerprint"
                            BiometricCapability.DeviceCredentialOnly -> "Use device PIN"
                            BiometricCapability.None -> "Confirm check-in"
                        }
                        VButton(
                            text = primaryLabel,
                            onClick = {
                                scope.launch {
                                    if (authenticator.capability == BiometricCapability.None) {
                                        onCheckIn(BiometricMethod.Manual.wire)
                                        return@launch
                                    }
                                    working = true
                                    val result = authenticator.authenticate("Check in for today")
                                    working = false
                                    when (result) {
                                        is BiometricResult.Success -> onCheckIn(result.method.wire)
                                        is BiometricResult.Cancelled -> Unit // stay open; teacher may retry or close
                                        is BiometricResult.Failed -> onCheckIn(BiometricMethod.Manual.wire)
                                    }
                                }
                            },
                            full = true,
                            tone = VButtonTone.Lavender,
                            size = VButtonSize.Lg,
                            loading = state.isCheckingIn || working,
                            leading = { Icon(VIcons.ShieldCheck, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                        Spacer(Modifier.height(8.dp))
                        VButton(
                            text = "Later",
                            onClick = onDismiss,
                            full = true,
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Md,
                        )
                    }
                }
            }
        }
    }
}
