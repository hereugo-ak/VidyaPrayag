package com.littlebridge.enrollplus.platform

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Android implementation — uses rememberLauncherForActivityResult with
 * ActivityResultContracts.RequestPermission() for the Compose-native
 * permission flow (replaces the deprecated ActivityCompat.requestPermissions
 * approach that never delivered the callback).
 */
@Composable
actual fun rememberNotificationPermissionLauncher(
    onResult: (Boolean) -> Unit
): NotificationPermissionLauncher {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onResult(granted)
    }

    return remember(launcher) {
        object : NotificationPermissionLauncher {
            override fun launch() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Permission not required below API 33 — always granted.
                    onResult(true)
                }
            }
        }
    }
}
