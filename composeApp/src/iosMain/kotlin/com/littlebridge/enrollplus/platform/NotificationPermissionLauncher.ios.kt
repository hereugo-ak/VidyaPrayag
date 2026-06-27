package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation — placeholder. iOS notification permission is requested
 * via UNUserNotificationCenter when the FCM/APNs token is first registered,
 * not via a Compose launcher. This no-op lets common code compile.
 */
@Composable
actual fun rememberNotificationPermissionLauncher(
    onResult: (Boolean) -> Unit
): NotificationPermissionLauncher = remember {
    object : NotificationPermissionLauncher {
        override fun launch() {
            // iOS handles notification permission via the SDK during token registration.
            onResult(true)
        }
    }
}
