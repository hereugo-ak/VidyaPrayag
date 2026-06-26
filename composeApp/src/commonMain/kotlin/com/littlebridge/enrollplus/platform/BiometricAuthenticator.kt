package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable

/**
 * BiometricAuthenticator — the platform-abstracted identity check that fronts the
 * teacher self check-in (Doc 06 §2, T-106c).
 *
 * The hard design rule (Doc 06 §2.1 / Doc 10 §6.6 / accessibility checklist):
 * **biometric verifies it's really this teacher; it must NEVER be a hard gate.**
 * Every platform's [authenticate] therefore resolves the strict fallback ladder:
 *
 *   1. biometric  — Android `BiometricPrompt` (strong/weak) / iOS `LAContext`
 *   2. pin        — device credential (PIN / pattern / passcode)
 *   3. manual     — a plain "Confirm check-in" the caller always offers
 *
 * The returned [BiometricResult] tells the caller which rung produced success (so
 * the check-in records the honest `method`), or that the attempt failed/was
 * cancelled — in which case the caller still surfaces the always-available manual
 * confirm. The authenticator NEVER blocks the manual path; [capability] lets the
 * UI phrase the prompt correctly (e.g. hide "Use biometrics" on a device that has
 * none, jump straight to the manual confirm on desktop/web).
 */
enum class BiometricCapability {
    /** Hardware present AND a biometric is enrolled — the full prompt is offered. */
    BiometricAvailable,

    /** No biometric, but a device credential (PIN/passcode) can be used. */
    DeviceCredentialOnly,

    /** Neither — only the manual "Confirm check-in" fallback applies. */
    None,
}

/** Which rung of the ladder succeeded (mirrors the server's `method` enum). */
enum class BiometricMethod(val wire: String) {
    Biometric("biometric"),
    Pin("pin"),
    Manual("manual"),
}

/** Outcome of an [BiometricAuthenticator.authenticate] attempt. */
sealed interface BiometricResult {
    /** The user verified via [method]; the caller may complete the check-in. */
    data class Success(val method: BiometricMethod) : BiometricResult

    /** The user dismissed/cancelled the prompt — fall back to manual confirm. */
    data object Cancelled : BiometricResult

    /**
     * The attempt errored (no hardware mid-flight, lockout, etc.). [message] is a
     * short human-readable cause for an optional caption; manual confirm remains.
     */
    data class Failed(val message: String) : BiometricResult
}

/**
 * The platform identity check. Obtained via [rememberBiometricAuthenticator] so
 * each platform can capture whatever host handle it needs (the Android Activity,
 * an iOS LAContext, …) at composition time, without leaking that into common code.
 */
interface BiometricAuthenticator {
    /** What the current device can do — drives how the UI phrases the prompt. */
    val capability: BiometricCapability

    /**
     * Run the biometric → device-credential ladder. [reason] is shown in the system
     * prompt subtitle. Implementations must resolve (never hang): a host without a
     * usable prompt returns [BiometricResult.Failed]/[BiometricResult.Cancelled]
     * immediately so the caller can show the manual confirm.
     */
    suspend fun authenticate(reason: String): BiometricResult
}

/**
 * Compose-scoped factory for the platform [BiometricAuthenticator]. Implemented per
 * target: Android (BiometricPrompt + device credential), iOS (LAContext capability +
 * graceful manual), JVM/desktop & Web (manual-only). Always returns a non-null
 * authenticator — the manual fallback is universal.
 */
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
