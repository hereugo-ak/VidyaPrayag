package com.littlebridge.vidyaprayag.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Web (JS + Wasm) [BiometricAuthenticator] — the browser exposes no portal-grade
 * biometric prompt we rely on here, so capability is honestly
 * [BiometricCapability.None] and [authenticate] resolves immediately to a failure.
 * The caller's universal manual "Confirm check-in" (Doc 06 §2.1) is the working rung
 * on the web — check-in is never blocked. (A future WebAuthn rung could upgrade this
 * actual without touching common code.)
 */
private object WebBiometricAuthenticator : BiometricAuthenticator {
    override val capability: BiometricCapability = BiometricCapability.None
    override suspend fun authenticate(reason: String): BiometricResult =
        BiometricResult.Failed("Biometric not available in browser")
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { WebBiometricAuthenticator }
