package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable

/**
 * Platform-abstracted notification permission launcher.
 *
 * Returns a [NotificationPermissionLauncher] that the caller can invoke to
 * trigger the system permission dialog. On Android 13+ this uses
 * `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`;
 * on iOS it uses the UNUserNotificationCenter request; on JVM/Web it is a
 * no-op (notifications are always "granted" on those platforms).
 *
 * Usage in a screen:
 * ```
 * val launcher = rememberNotificationPermissionLauncher { granted ->
 *     permissionVm.onPermissionResult(granted)
 * }
 * LaunchedEffect(launchPermission) {
 *     if (launchPermission) {
 *         permissionVm.consumeLaunchPermissionRequest()
 *         launcher.launch()
 *     }
 * }
 * ```
 */
interface NotificationPermissionLauncher {
    /** Launch the system permission request. Safe to call multiple times. */
    fun launch()
}

@Composable
expect fun rememberNotificationPermissionLauncher(
    onResult: (Boolean) -> Unit
): NotificationPermissionLauncher
