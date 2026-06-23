package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherCheckInState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherCheckInViewModel
import com.littlebridge.vidyaprayag.platform.BiometricCapability
import com.littlebridge.vidyaprayag.platform.BiometricMethod
import com.littlebridge.vidyaprayag.platform.BiometricResult
import com.littlebridge.vidyaprayag.platform.rememberBiometricAuthenticator
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherCheckInCard — the morning self check-in band on Today (T-106c, Doc 06 §2,
 * Doc 10 §6.1/§6.6). Two faces:
 *   - **Not checked in** → amber (`warning`) plate with a "Check in" CTA that runs the
 *     biometric ladder (biometric → device credential → manual confirm).
 *   - **Checked in** → green (`success`) "Checked in HH:mm ✓" pill with the method.
 *
 * The biometric prompt is launched via the platform
 * [com.littlebridge.vidyaprayag.platform.BiometricAuthenticator]; on success/cancel/
 * fail we ALWAYS leave the teacher a working manual confirm (never a hard gate). The
 * timestamp shown is the SERVER-stamped one from the VM (Doc 06 §2.4) — never the
 * device clock. Status is color + icon + letter-free text so it never relies on hue
 * alone (accessibility checklist, Doc 10 §11).
 */
@Composable
fun TeacherCheckInCard(
    modifier: Modifier = Modifier,
    viewModel: TeacherCheckInViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val biometric = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()

    // Run the ladder: try the system prompt (when capable); on success post with the
    // honest method, otherwise drop to a manual confirm the caller always offers.
    fun runCheckIn() {
        if (state.checkedIn || state.isCheckingIn) return
        if (biometric.capability == BiometricCapability.None) {
            // No system prompt available — manual confirm IS the working rung.
            viewModel.checkIn(BiometricMethod.Manual.wire)
            return
        }
        scope.launch {
            when (val r = biometric.authenticate("Confirm your check-in")) {
                is BiometricResult.Success -> viewModel.checkIn(r.method.wire)
                // Cancel / failure are never dead-ends: fall back to manual confirm.
                is BiometricResult.Cancelled -> viewModel.checkIn(BiometricMethod.Manual.wire)
                is BiometricResult.Failed -> viewModel.checkIn(BiometricMethod.Manual.wire)
            }
        }
    }

    TeacherCheckInCardContent(
        state = state,
        onCheckIn = ::runCheckIn,
        onManualConfirm = { viewModel.checkIn(BiometricMethod.Manual.wire) },
        biometricCapable = biometric.capability != BiometricCapability.None,
        modifier = modifier,
    )
}

@Composable
private fun TeacherCheckInCardContent(
    state: TeacherCheckInState,
    onCheckIn: () -> Unit,
    onManualConfirm: () -> Unit,
    biometricCapable: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    VCard(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = state.checkedIn,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "checkin-face",
        ) { checkedIn ->
            if (checkedIn) {
                // ── Checked-in (green) ──────────────────────────────────────────
                val time = state.checkedInAt.toClockHHmm()
                Row(
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "Checked in" + (time?.let { " at $it" } ?: "")
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusDot(fill = c.success, icon = VIcons.Check, iconTint = c.successInk)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Checked in" + (time?.let { " $it" } ?: ""),
                            style = VTheme.type.bodyStrong.colored(c.ink)
                                .copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                        )
                        Text(
                            state.method?.methodLabel() ?: "Your attendance is recorded for today",
                            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                        )
                    }
                }
            } else {
                // ── Not-checked-in (amber) ──────────────────────────────────────
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusDot(fill = c.warning, icon = VIcons.Clock, iconTint = c.warningInk)
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.statusUnavailable) "Check-in" else "Mark your presence",
                                style = VTheme.type.bodyStrong.colored(c.ink)
                                    .copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                            )
                            Text(
                                when {
                                    state.statusUnavailable -> "Status unavailable — you can still confirm below"
                                    biometricCapable -> "Verify it's you to check in for today"
                                    else -> "Confirm your check-in for today"
                                },
                                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                            )
                        }
                    }

                    // Primary CTA — runs the biometric ladder (or manual when no prompt).
                    VButton(
                        text = if (biometricCapable) "Check in" else "Confirm check-in",
                        onClick = onCheckIn,
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Lavender,
                        soft = false,
                        size = VButtonSize.Md,
                        full = true,
                        loading = state.isCheckingIn,
                        leading = {
                            Icon(
                                if (biometricCapable) VIcons.ShieldCheck else VIcons.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )

                    // Always-available manual fallback (Doc 06 §2.1) — visible whenever
                    // a system prompt exists, so the teacher is never trapped behind it.
                    if (biometricCapable && !state.isCheckingIn) {
                        Text(
                            "Confirm manually instead",
                            style = VTheme.type.caption.colored(c.accentDeep)
                                .copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !state.isCheckingIn) { onManualConfirm() }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .semantics { contentDescription = "Confirm check-in manually" },
                        )
                    }

                    state.error?.let {
                        Text(
                            it,
                            style = VTheme.type.caption.colored(c.dangerInk).copy(fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(
    fill: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

/** Map the wire method to an honest human caption. */
private fun String.methodLabel(): String = when (this) {
    "biometric" -> "Verified with biometrics"
    "pin" -> "Verified with device credential"
    "manual" -> "Confirmed manually"
    else -> "Your attendance is recorded for today"
}

/**
 * Extract "HH:mm" from a server ISO timestamp (e.g. "2026-06-23T08:42:17Z" or with an
 * offset). The server is authoritative (Doc 06 §2.4); we only display its time, never
 * compute it on-device. Returns null when the shape is unexpected.
 */
private fun String?.toClockHHmm(): String? {
    val s = this ?: return null
    val t = s.substringAfter('T', "")
    if (t.length < 5) return null
    val hhmm = t.take(5)
    return if (hhmm[2] == ':') hhmm else null
}
