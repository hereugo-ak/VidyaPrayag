/*
 * File: LocationProvider.js.kt  (jsMain — actual)
 * Module: ui.location
 *
 * Web (Kotlin/JS) placeholder. The browser Geolocation API could be wired here
 * later; for now this reports unavailable so the UI falls back to manual entry
 * and the multiplatform build stays green.
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
