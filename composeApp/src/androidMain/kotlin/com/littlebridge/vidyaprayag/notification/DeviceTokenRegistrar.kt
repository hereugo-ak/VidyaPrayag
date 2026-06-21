/*
 * File: DeviceTokenRegistrar.kt
 * Module: composeApp / androidMain / notification
 *
 * Owns the client side of the device-token registration lifecycle:
 *   1. Fetch the current FCM token from FirebaseMessaging.
 *   2. Compare it against the cached token in PreferenceRepository.
 *   3. Only when it has CHANGED (or there is no cached token) POST it to the
 *      backend via NotificationApi — this is the "fetch → compare cached →
 *      register if changed" gate the foundation spec mandates, so a normal
 *      cold start with a stable token is a network no-op.
 *   4. Persist the freshly registered token back into the cache.
 *
 * WHY A DEDICATED REGISTRAR (NOT INLINE IN THE SERVICE / APP)
 *   Token registration happens from two entry points:
 *     - Application.onCreate()  (app-startup registration)
 *     - FirebaseMessagingService.onNewToken() (token rotation)
 *   Both share the exact same fetch → compare → register → persist flow, so it
 *   lives here once. The service and the app both call [registerIfChanged].
 *
 * GRACEFUL DEGRADATION
 *   - Firebase not initialised (no google-services config) → skip silently.
 *   - Not signed in (no access token in prefs) → skip; the token is not pushed
 *     until the next authenticated launch (the server requires JWT).
 *   - Network / API failure → the token is NOT cached, so the next launch
 *     retries. We only cache after a confirmed successful registration.
 *
 * NOTE on kotlinx-coroutines-play-services:
 *   That helper is not on the classpath, so we bridge the Firebase Task result
 *   via suspendCancellableCoroutine using only kotlinx-coroutines-core.
 */
package com.littlebridge.vidyaprayag.notification

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.notification.data.remote.NotificationApi
import com.littlebridge.vidyaprayag.feature.notification.domain.model.RegisterDeviceTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DeviceTokenRegistrar {

    private const val TAG = "DeviceTokenRegistrar"

    /**
     * Fetch the FCM token, compare with the cached value, and register with the
     * backend when it has changed. Safe to call on every app start and on every
     * onNewToken() callback. Returns true when a registration call succeeded
     * (or the token was already in sync); false when it was skipped or failed.
     */
    suspend fun registerIfChanged(context: Context): Boolean {
        // 1) Firebase must be initialised (FirebaseInitializer ran at app start).
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.d(TAG, "Firebase not initialised — skipping token registration.")
            return false
        }

        // 2) Fetch the current FCM token.
        val token = runCatching { fetchFcmToken() }.getOrNull()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "FCM token fetch returned empty — skipping registration.")
            return false
        }

        // 3) Resolve the prefs cache + API via Koin. Koin is started in
        //    Application.onCreate() BEFORE this registrar runs from the app
        //    entry point; from onNewToken() it is also started by then. Guard
        //    anyway so a very early callback can never crash the process.
        val prefs: PreferenceRepository = resolveKoin() ?: run {
            Log.w(TAG, "Koin/PreferenceRepository not available — skipping registration.")
            return false
        }

        // 4) Compare with the cached token — skip the network round-trip when
        //    unchanged (the common case on a normal cold start).
        val cached = runCatching { prefs.getFcmToken().first() }.getOrNull()
        if (cached != null && cached == token) {
            Log.d(TAG, "FCM token unchanged — skipping re-registration.")
            return true
        }

        // 5) Only push to the backend when the user is signed in. The server's
        //    POST /api/device-tokens is JWT-authenticated; calling it without a
        //    token would 401 and waste a round-trip. We do NOT cache here so
        //    the next authenticated launch retries the registration.
        val accessToken = runCatching { prefs.getUserToken().first() }.getOrNull()
        if (accessToken.isNullOrBlank()) {
            Log.d(TAG, "User not signed in — deferring FCM registration until authenticated.")
            return false
        }

        // 6) Register via the shared NotificationApi (resolved from Koin).
        val api: NotificationApi = resolveKoin() ?: run {
            Log.w(TAG, "NotificationApi not available from Koin — skipping registration.")
            return false
        }

        val request = RegisterDeviceTokenRequest(
            token = token,
            platform = "android",
            appVersion = appVersion(context),
            deviceModel = deviceModel()
        )

        val result = runCatching { api.registerDeviceToken(request) }.getOrElse { e ->
            Log.w(TAG, "Device token registration network call failed: ${e.message}")
            return false
        }

        // 7) Only cache after a confirmed success so a failed/partial
        //    registration retries on the next launch. Inspect the NetworkResult.
        val success = (result as? NetworkResult.Success)?.data?.success ?: false
        if (!success) {
            Log.w(TAG, "Device token registration reported failure — not caching; will retry.")
            return false
        }

        runCatching { prefs.setFcmToken(token) }
            .onFailure { Log.w(TAG, "Failed to cache FCM token: ${it.message}") }
        Log.i(TAG, "FCM token registered with backend and cached.")
        return true
    }

    // ------------------------------------------------------------------
    // FCM token fetch (Task → suspend bridge)
    // ------------------------------------------------------------------

    /**
     * Bridge the FirebaseMessaging.getInstance().token Task into a suspend call.
     * Resumes with the token string on success, or throws on failure. The Task
     * is awaited via addOnSuccessListener / addOnFailureListener so we never
     * block a thread.
     */
    private suspend fun fetchFcmToken(): String = suspendCancellableCoroutine { cont ->
        val task: Task<String> = FirebaseMessaging.getInstance().token
        task.addOnSuccessListener { token -> if (cont.isActive) cont.resume(token) }
        task.addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

    // ------------------------------------------------------------------
    // Diagnostics
    // ------------------------------------------------------------------

    private fun appVersion(context: Context): String? = runCatching {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        info.versionName
    }.getOrNull()

    private fun deviceModel(): String? = runCatching {
        val manufacturer = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        if (manufacturer.isBlank() && model.isBlank()) null
        else if (model.startsWith(manufacturer, ignoreCase = true)) model
        else "$manufacturer $model"
    }.getOrNull()

    // ------------------------------------------------------------------
    // Koin resolution
    // ------------------------------------------------------------------

    /**
     * Resolve a Koin singleton. Returns null if Koin has not been started yet
     * (e.g. a callback firing before Application.onCreate completes) so callers
     * can degrade gracefully instead of crashing.
     */
    private inline fun <reified T> resolveKoin(): T? = runCatching {
        org.koin.core.context.GlobalContext.get().get<T>()
    }.getOrNull()
}
