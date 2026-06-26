package com.littlebridge.enrollplus.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android [BiometricAuthenticator] — real AndroidX [BiometricPrompt] with a device
 * credential fallback, then the universal manual confirm the caller owns
 * (Doc 06 §2.1 ladder; never a hard gate).
 *
 * AndroidX's [BiometricPrompt] requires a [FragmentActivity] host. The app's
 * `MainActivity` is a plain [ComponentActivity], so when the resolved host is NOT a
 * FragmentActivity we honestly report [BiometricCapability.None] and let the UI fall
 * straight through to the manual confirm — rather than silently pretending the prompt
 * exists. (Deviation flagged in the commit: the prompt is wired and fully functional
 * the moment the host activity extends FragmentActivity; until then the always-
 * available manual rung keeps check-in working for every teacher.)
 */
private class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity?,
    appContext: Context,
) : BiometricAuthenticator {

    private val allowedAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private val biometricManager = BiometricManager.from(appContext)

    override val capability: BiometricCapability
        get() {
            if (activity == null) return BiometricCapability.None
            val strongWeak = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            return when (biometricManager.canAuthenticate(strongWeak)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.BiometricAvailable
                else ->
                    // No usable biometric — can we still demand a device credential?
                    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                        BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.DeviceCredentialOnly
                        else -> BiometricCapability.None
                    }
            }
        }

    override suspend fun authenticate(reason: String): BiometricResult {
        val host = activity ?: return BiometricResult.Failed("Biometric prompt unavailable")
        if (capability == BiometricCapability.None) {
            return BiometricResult.Failed("No biometric or device credential enrolled")
        }
        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(host)
            val prompt = BiometricPrompt(
                host,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (!cont.isActive) return
                        // authenticationType tells us which rung actually verified.
                        val method = when (result.authenticationType) {
                            BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL ->
                                BiometricMethod.Pin
                            else -> BiometricMethod.Biometric
                        }
                        cont.resume(BiometricResult.Success(method))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!cont.isActive) return
                        val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        cont.resume(
                            if (cancelled) BiometricResult.Cancelled
                            else BiometricResult.Failed(errString.toString()),
                        )
                    }

                    // onAuthenticationFailed = a single non-matching attempt; the
                    // prompt stays open, so we do NOT resume here.
                },
            )

            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm it's you")
                .setSubtitle(reason)
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()

            runCatching { prompt.authenticate(info) }
                .onFailure { if (cont.isActive) cont.resume(BiometricResult.Failed(it.message ?: "Prompt error")) }

            cont.invokeOnCancellation { runCatching { prompt.cancelAuthentication() } }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) {
        AndroidBiometricAuthenticator(
            activity = context.findFragmentActivity(),
            appContext = context.applicationContext,
        )
    }
}
