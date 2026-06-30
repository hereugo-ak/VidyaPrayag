package com.littlebridge.enrollplus.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.littlebridge.enrollplus.feature.notification.domain.service.NotificationService

/**
 * AndroidNotificationService — Android implementation of [NotificationService].
 * Delegates to the [DeviceTokenRegistrar] singleton for token sync, and handles
 * the Android 13+ (API 33) notification permission flow.
 */
class AndroidNotificationService(
    private val context: Context
) : NotificationService {

    override suspend fun syncDeviceToken(force: Boolean): Boolean {
        return DeviceTokenRegistrar.syncRegistration(context, force)
    }

    override fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Permission not required on API < 33
            true
        }
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        // The Compose layer now handles permission launching via
        // rememberNotificationPermissionLauncher (ActivityResultContracts).
        // This method is kept for interface compliance; the actual launch
        // happens in the screen composable, and the result is routed through
        // PermissionViewModel.onPermissionResult().
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true)
        } else {
            // Return current state — the Compose launcher will update it.
            onResult(hasPermission())
        }
    }

    override fun shouldShowRationale(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val activity = findActivity(context) ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    private fun findActivity(context: Context): Activity? {
        var c = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}
