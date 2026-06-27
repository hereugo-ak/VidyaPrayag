package com.littlebridge.enrollplus.platform.transport

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.context.GlobalContext

@Serializable
data class LocationPayload(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float? = null,
    val heading: Float? = null,
)

class TransportLocationService : Service() {

    companion object {
        const val EXTRA_VEHICLE_ID = "vehicle_id"
        const val EXTRA_BASE_URL = "base_url"
        private const val CHANNEL_ID = "transport_tracking"
        private const val NOTIFICATION_ID = 53001
        private const val INTERVAL_MS = 30_000L

        fun start(context: Context, vehicleId: String, baseUrl: String) {
            val intent = Intent(context, TransportLocationService::class.java).apply {
                putExtra(EXTRA_VEHICLE_ID, vehicleId)
                putExtra(EXTRA_BASE_URL, baseUrl)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TransportLocationService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var vehicleId: String = ""
    private var baseUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        vehicleId = intent?.getStringExtra(EXTRA_VEHICLE_ID) ?: ""
        baseUrl = intent?.getStringExtra(EXTRA_BASE_URL) ?: ""
        if (vehicleId.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Tracking bus location…"))
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc -> sendLocation(loc) }
            }
        }

        fusedClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun sendLocation(loc: Location) {
        scope.launch {
            try {
                val koin = GlobalContext.getOrNull() ?: return@launch
                val prefs = koin.get<com.littlebridge.enrollplus.core.prefs.PreferenceRepository>()
                val token = prefs.getUserToken().first() ?: return@launch
                val client = koin.get<HttpClient>()

                val payload = LocationPayload(
                    vehicleId = vehicleId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    speed = if (loc.hasSpeed()) loc.speed * 3.6f else null, // m/s → km/h
                    heading = if (loc.hasBearing()) loc.bearing else null,
                )

                val url = "${baseUrl.trimEnd('/')}/api/v1/transport/location"
                client.post(url) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            } catch (e: Exception) {
                // Silently retry on next interval — the service is resilient.
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vidya Prayag Transport")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transport Tracking",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Background bus location tracking" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
