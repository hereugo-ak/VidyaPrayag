package com.littlebridge.enrollplus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.notification.domain.service.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * PermissionViewModel — common logic for managing runtime permissions (Notification, etc).
 *
 * Encapsulates the "when to ask" logic:
 *   1. Check if permission is already granted.
 *   2. Check if the user has previously permanently declined the rationale.
 *   3. If missing, decide between showing a custom rationale or triggering the system prompt.
 */
class PermissionViewModel(
    private val notificationService: NotificationService,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _showNotificationRationale = MutableStateFlow(false)
    val showNotificationRationale: StateFlow<Boolean> = _showNotificationRationale.asStateFlow()

    /** One-shot event telling the screen to launch the system permission dialog. */
    private val _launchPermissionRequest = MutableStateFlow(false)
    val launchPermissionRequest: StateFlow<Boolean> = _launchPermissionRequest.asStateFlow()

    /**
     * Check the current state of notification permissions and decide if we should
     * prompt the user. Called typically from a screen's `LaunchedEffect(Unit)`.
     */
    fun checkNotificationPermission() {
        viewModelScope.launch {
            if (notificationService.hasPermission()) {
                _showNotificationRationale.value = false
                return@launch
            }

            // Respect the "Not Now" choice from a previous rationale display.
            val declined = preferenceRepository.getNotificationsDeclined().first()
            if (declined) return@launch

            if (notificationService.shouldShowRationale()) {
                // User denied once — show our explanation before asking again.
                _showNotificationRationale.value = true
            } else {
                // First time asking or permanently denied. Signal the screen
                // to launch the system dialog via rememberLauncherForActivityResult.
                _launchPermissionRequest.value = true
            }
        }
    }

    /** Trigger the system permission prompt directly (typically from the Rationale "Enable" button). */
    fun requestNotificationPermission() {
        _showNotificationRationale.value = false
        _launchPermissionRequest.value = true
    }

    /** Called by the screen when the system permission dialog returns a result. */
    fun onPermissionResult(granted: Boolean) {
        _launchPermissionRequest.value = false
        if (granted) {
            _showNotificationRationale.value = false
        }
    }

    /** Consumes the launch event (call after the screen has read it and launched the dialog). */
    fun consumeLaunchPermissionRequest() {
        _launchPermissionRequest.value = false
    }

    /** User clicked "Not Now" on our rationale — persist this so we don't pester them again. */
    fun declineNotifications() {
        _showNotificationRationale.value = false
        viewModelScope.launch {
            preferenceRepository.setNotificationsDeclined(true)
        }
    }
}
