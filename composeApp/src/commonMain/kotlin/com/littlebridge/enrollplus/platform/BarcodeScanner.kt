package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable

/**
 * Platform-abstracted barcode scanner for library quick-issue and bulk-return.
 *
 * On Android this uses Google's ML Kit / CameraX to present a live camera
 * preview that scans barcodes in real time. On iOS it uses AVFoundation.
 * On JVM/Web it falls back to a manual text input field.
 *
 * Usage:
 * ```
 * BarcodeScanner(
 *     onScanned = { barcode -> viewModel.quickIssue(...) },
 *     onClose = { showScanner = false },
 * )
 * ```
 */
interface BarcodeScanner {
    /** Start the camera preview and begin scanning. */
    fun start()

    /** Stop the camera and release resources. */
    fun stop()

    /** Whether the device has a camera (and thus supports live scanning). */
    val hasCamera: Boolean
}

@Composable
expect fun rememberBarcodeScanner(
    onScanned: (String) -> Unit,
): BarcodeScanner

/**
 * Composable that renders the platform-specific scanner UI.
 * On platforms without a camera, renders a manual text input.
 */
@Composable
expect fun BarcodeScannerView(
    onScanned: (String) -> Unit,
    onClose: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
)
