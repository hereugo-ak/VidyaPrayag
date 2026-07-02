package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable

/**
 * Platform-abstracted media picker for images and PDFs.
 *
 * On Android, uses `ActivityResultContracts.PickVisualMedia` for images
 * and `ActivityResultContracts.GetContent` for PDFs.
 * On JVM/Web/iOS, shows an unsupported message via [onUnsupported].
 *
 * Usage:
 * ```kotlin
 * val picker = rememberMediaPicker(
 *     onPicked = { bytes, mimeType, fileName -> ... },
 *     onUnsupported = { message -> ... },
 * )
 * picker.launchImage()   // or picker.launchPdf()
 * ```
 */
interface MediaPicker {
    fun launchImage()
    fun launchPdf()
}

@Composable
expect fun rememberMediaPicker(
    onPicked: (bytes: ByteArray, mimeType: String, fileName: String) -> Unit,
    onUnsupported: (String) -> Unit,
): MediaPicker
