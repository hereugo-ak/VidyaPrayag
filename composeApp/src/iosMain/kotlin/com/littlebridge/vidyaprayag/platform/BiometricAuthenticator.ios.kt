package com.littlebridge.vidyaprayag.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

/**
 * iOS [BiometricAuthenticator] — real [LAContext] Face ID / Touch ID with a device-
 * passcode fallback, then the caller's universal manual confirm (Doc 06 §2.1).
 *
 * `LAPolicyDeviceOwnerAuthentication` already includes the passcode fallback, so a
 * single `evaluatePolicy` call walks biometric → passcode; we report which by
 * probing `LAPolicyDeviceOwnerAuthenticationWithBiometrics` for capability.
 */
private class IosBiometricAuthenticator : BiometricAuthenticator {

    override val capability: BiometricCapability
        get() {
            val ctx = LAContext()
            val biometricOk = ctx.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                error = null,
            )
            if (biometricOk) return BiometricCapability.BiometricAvailable
            val credentialOk = ctx.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthentication,
                error = null,
            )
            return if (credentialOk) BiometricCapability.DeviceCredentialOnly else BiometricCapability.None
        }

    override suspend fun authenticate(reason: String): BiometricResult {
        val ctx = LAContext()
        val canBiometric = ctx.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )
        // Prefer the biometrics-only policy when available (so a success is honestly
        // 'biometric'); otherwise the combined policy adds the passcode rung.
        val policy = if (canBiometric) {
            LAPolicyDeviceOwnerAuthenticationWithBiometrics
        } else {
            LAPolicyDeviceOwnerAuthentication
        }
        if (!ctx.canEvaluatePolicy(policy, error = null)) {
            return BiometricResult.Failed("No biometric or passcode available")
        }
        val method = if (canBiometric) BiometricMethod.Biometric else BiometricMethod.Pin
        return suspendCancellableCoroutine { cont ->
            ctx.evaluatePolicy(policy, localizedReason = reason) { success, error ->
                if (!cont.isActive) return@evaluatePolicy
                when {
                    success -> cont.resume(BiometricResult.Success(method))
                    error != null -> cont.resume(BiometricResult.Failed(error.localizedDescription))
                    else -> cont.resume(BiometricResult.Cancelled)
                }
            }
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { IosBiometricAuthenticator() }
