package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * JVM/desktop [BiometricAuthenticator] — there is no system biometric prompt on the
 * desktop host, so capability is honestly [BiometricCapability.None] and [authenticate]
 * resolves immediately to [BiometricResult.Failed]. The caller's universal manual
 * confirm (Doc 06 §2.1) is the working rung here — check-in is never blocked.
 */
private object DesktopBiometricAuthenticator : BiometricAuthenticator {
    override val capability: BiometricCapability = BiometricCapability.None
    override suspend fun authenticate(reason: String): BiometricResult =
        BiometricResult.Failed("Biometric not available on this device")
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator =
    remember { DesktopBiometricAuthenticator }
