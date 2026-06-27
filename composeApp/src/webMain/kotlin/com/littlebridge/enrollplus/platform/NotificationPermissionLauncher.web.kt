package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Web (wasm/JS) implementation — no runtime permission dialog needed.
 * Web notifications use the Notifications API browser-side, not a Compose launcher.
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
