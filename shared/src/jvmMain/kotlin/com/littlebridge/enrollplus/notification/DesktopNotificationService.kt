package com.littlebridge.enrollplus.notification

import com.littlebridge.enrollplus.feature.notification.domain.service.NotificationService

/**
 * DesktopNotificationService — JVM/desktop implementation of [NotificationService].
 *
 * Desktop builds have no FCM/APNs push integration, so this is a no-op
 * implementation (mirroring [IosNotificationService]). It exists purely to
 * satisfy the [NotificationService] dependency required by common code such as
 * [com.littlebridge.enrollplus.presentation.MainViewModel] when running the
 * desktop (`:composeApp:run`) target.
 */
class DesktopNotificationService : NotificationService {

    override suspend fun syncDeviceToken(force: Boolean): Boolean {
        // No push provider on desktop. Report success so common code can proceed.
        return true
    }

    override fun hasPermission(): Boolean = true

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        onResult(true)
    }

    override fun shouldShowRationale(): Boolean = false
}
