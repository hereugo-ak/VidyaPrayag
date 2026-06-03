/*
 * File: LocationProvider.jvm.kt  (jvmMain — actual)
 * Module: ui.location
 *
 * Desktop placeholder. There's no portable desktop GPS API, so this reports
 * the provider as unavailable and the UI falls back to manual address entry.
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
            LocationResult.Unavailable("Current location isn't available on desktop. Enter the address manually.")
        )
    }
}
