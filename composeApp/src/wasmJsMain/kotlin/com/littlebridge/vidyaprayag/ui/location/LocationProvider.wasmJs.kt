/*
 * File: LocationProvider.wasmJs.kt  (wasmJsMain — actual)
 * Module: ui.location
 *
 * Wasm/JS placeholder. Reports unavailable so the UI falls back to manual
 * address entry and the multiplatform build stays green.
 */
package com.littlebridge.vidyaprayag.ui.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationProvider(
    onResult: (LocationResult) -> Unit
): () -> Unit = remember {
    {
        onResult(
            LocationResult.Unavailable("Current location isn't available in this build. Enter the address manually.")
        )
    }
}
