package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMediaPicker(
    onPicked: (bytes: ByteArray, mimeType: String, fileName: String) -> Unit,
    onUnsupported: (String) -> Unit,
): MediaPicker {
    return remember {
        object : MediaPicker {
            override fun launchImage() {
                onUnsupported("Image picker is not yet supported on Web. Please use 'Paste Text' mode.")
            }

            override fun launchPdf() {
                onUnsupported("PDF picker is not yet supported on Web. Please use 'Paste Text' mode.")
            }
        }
    }
}
