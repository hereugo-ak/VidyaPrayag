/*
 * File: LocationProvider.kt  (commonMain — expect)
 * Module: ui.location
 *
 * Cross-platform "use current location" capability for the school-onboarding
 * Basics step. Replaces the old static map preview (report §4.4 / §11.2): the
 * admin can now tap a button to capture the device's real GPS fix, which we
 * reverse-geocode to a human address and persist as latitude/longitude +
 * full_address on the school record.
 *
 * Android has a full, dependency-free implementation (LocationManager +
 * android.location.Geocoder). Other targets compile cleanly and return a
 * PermissionDenied/Unavailable result so the multiplatform build stays green
 * and the UI degrades to manual entry.
 */
package com.littlebridge.vidyaprayag.ui.location

import androidx.compose.runtime.Composable

/** A resolved device location + best-effort reverse-geocoded address parts. */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    /** Full single-line address, e.g. "12 MG Road, Sector 42, New Delhi 110001". */
    val fullAddress: String? = null,
    val city: String? = null,
    val district: String? = null,
    val state: String? = null,
    val pincode: String? = null
)

/** Outcome of a current-location request. */
sealed interface LocationResult {
    data class Success(val location: DeviceLocation) : LocationResult
    /** User declined the runtime permission. */
    data object PermissionDenied : LocationResult
    /** Location services off, no fix available, or platform has no provider. */
    data class Unavailable(val reason: String) : LocationResult
}

/**
 * Remembers a launcher that, when invoked, requests the runtime location
 * permission (if needed), reads the current GPS fix, reverse-geocodes it, and
 * delivers a [LocationResult] to [onResult].
 *
 * @return a lambda — call it (e.g. from a button onClick) to start the flow.
 */
@Composable
expect fun rememberLocationProvider(
    onResult: (LocationResult) -> Unit
): () -> Unit
