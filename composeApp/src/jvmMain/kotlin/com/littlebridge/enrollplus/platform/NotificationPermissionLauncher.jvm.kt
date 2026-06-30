package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * JVM/Desktop implementation — no runtime permission dialog needed.
 * Notifications are always considered granted on desktop.
 */
@Composable
actual fun rememberNotificationPermissionLauncher(
    onResult: (Boolean) -> Unit
): NotificationPermissionLauncher = remember {
    object : NotificationPermissionLauncher {
        override fun launch() {
            onResult(true)
        }
    }
}
