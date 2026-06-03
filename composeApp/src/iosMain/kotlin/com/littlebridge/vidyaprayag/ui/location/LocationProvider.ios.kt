/*
 * File: LocationProvider.ios.kt  (iosMain — actual)
 * Module: ui.location
 *
 * iOS placeholder. The Android app is the primary school-side client; iOS can
 * later be wired to CLLocationManager + CLGeocoder. For now it reports the
 * provider as unavailable so the UI cleanly falls back to manual address entry
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
            LocationResult.Unavailable("Current location isn't available on this platform yet.")
        )
    }
}
