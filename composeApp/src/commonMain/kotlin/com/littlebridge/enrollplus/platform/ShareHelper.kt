package com.littlebridge.enrollplus.platform

/**
 * Platform-abstracted share helper for library book QR codes and catalog exports.
 *
 * On Android this uses an [android.content.Intent] with ACTION_SEND.
 * On iOS this uses a UIActivityViewController / ShareLink.
 *
 * The QR sharing UI ([QrShareDialog]) already renders a QR image inline;
 * this helper is for the "Share" button that invokes the native OS share
 * sheet to send the deep-link URL or a text snippet to other apps.
 */
interface ShareHelper {

    /**
     * Share a text payload via the native OS share sheet.
     *
     * @param text   The text content to share (e.g. a deep-link URL).
     * @param subject Optional subject / title (used by email clients).
     */
    fun shareText(text: String, subject: String? = null)
}

/**
 * Composable-scoped factory that returns the platform-specific [ShareHelper].
 * Must be called inside a @Composable function.
 */
@androidx.compose.runtime.Composable
expect fun rememberShareHelper(): ShareHelper
