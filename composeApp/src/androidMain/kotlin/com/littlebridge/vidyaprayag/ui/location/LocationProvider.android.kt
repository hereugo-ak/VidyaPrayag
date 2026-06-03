/*
 * File: LocationProvider.android.kt  (androidMain — actual)
 * Module: ui.location
 *
 * Real, dependency-free Android implementation. Uses the platform
 * LocationManager (no Google Play Services dependency) to obtain a fix, and
 * android.location.Geocoder for reverse geocoding. Runtime permission is
 * requested via ActivityResultContracts.RequestMultiplePermissions.
 *
 * Strategy:
 *   1) Ask for ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION at tap time.
 *   2) Try getLastKnownLocation() across enabled providers (instant, common).
 *   3) Fall back to a single requestSingleUpdate-style live fix with a timeout.
 *   4) Reverse-geocode off the main thread; tolerate geocoder failure (the
 *      lat/lng is still returned so persistence never blocks on geocoding).
 */
package com.littlebridge.vidyaprayag.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
actual fun rememberLocationProvider(
    onResult: (LocationResult) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            onResult(LocationResult.PermissionDenied)
        } else {
            scope.launch { onResult(resolveLocation(context)) }
        }
    }

    return remember(permissionLauncher) {
        {
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                scope.launch { onResult(resolveLocation(context)) }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}

private suspend fun resolveLocation(context: Context): LocationResult {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return LocationResult.Unavailable("Location service unavailable on this device.")

    val location = withContext(Dispatchers.IO) {
        lastKnown(lm) ?: liveFix(lm)
    } ?: return LocationResult.Unavailable(
        "Couldn't get a location fix. Turn on location services and try again."
    )

    val parts = withContext(Dispatchers.IO) {
        reverseGeocode(context, location.latitude, location.longitude)
    }

    return LocationResult.Success(
        DeviceLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            fullAddress = parts?.fullAddress,
            city = parts?.city,
            district = parts?.district,
            state = parts?.state,
            pincode = parts?.pincode
        )
    )
}

private fun lastKnown(lm: LocationManager): Location? {
    val providers = runCatching { lm.getProviders(true) }.getOrNull().orEmpty()
    var best: Location? = null
    for (p in providers) {
        val loc = runCatching {
            @Suppress("MissingPermission")
            lm.getLastKnownLocation(p)
        }.getOrNull()
        if (loc != null && (best == null || loc.time > best!!.time)) {
            best = loc
        }
    }
    return best
}

private suspend fun liveFix(lm: LocationManager): Location? {
    val provider = when {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return null
    }
    // Cap the wait so the UI never hangs on a missing fix.
    return withTimeoutOrNull(12_000L) {
        suspendCoroutine { cont ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { lm.removeUpdates(this) }
                    if (cont.context.isActiveSafely()) cont.resume(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            runCatching {
                @Suppress("MissingPermission")
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }.onFailure {
                if (cont.context.isActiveSafely()) cont.resume(null)
            }
        }
    }
}

// Guard against double-resume if the coroutine was cancelled mid-flight.
private fun kotlin.coroutines.CoroutineContext.isActiveSafely(): Boolean =
    this[kotlinx.coroutines.Job]?.isActive ?: true

private data class GeoParts(
    val fullAddress: String?,
    val city: String?,
    val district: String?,
    val state: String?,
    val pincode: String?
)

@Suppress("DEPRECATION")
private fun reverseGeocode(context: Context, lat: Double, lng: Double): GeoParts? = runCatching {
    val geocoder = android.location.Geocoder(context)
    val results = geocoder.getFromLocation(lat, lng, 1)
    val a = results?.firstOrNull() ?: return@runCatching null
    val line = (0..a.maxAddressLineIndex).mapNotNull { a.getAddressLine(it) }
        .joinToString(", ").ifBlank { null }
    GeoParts(
        fullAddress = line,
        city = a.locality ?: a.subAdminArea,
        district = a.subAdminArea ?: a.adminArea,
        state = a.adminArea,
        pincode = a.postalCode
    )
}.getOrNull()
