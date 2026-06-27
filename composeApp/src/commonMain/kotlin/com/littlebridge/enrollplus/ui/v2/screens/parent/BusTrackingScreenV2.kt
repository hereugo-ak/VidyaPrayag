package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.transport.domain.model.RouteProgress
import com.littlebridge.enrollplus.feature.transport.domain.model.TransportStop
import com.littlebridge.enrollplus.feature.transport.presentation.TransportViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BusTrackingScreenV2(
    childId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TransportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(childId) {
        viewModel.loadChildRoute(childId)
        viewModel.startPollingLiveLocation(childId)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        VBackHeader(title = "Bus Tracking", onBack = onBack)

        VStateHost(
            loading = state.isLoading && state.routeProgress == null,
            error = state.error,
            isEmpty = state.routeProgress == null && state.childRoute == null,
            emptyTitle = "No transport assignment found",
            emptyBody = "This child is not assigned to any bus route yet.",
            onRetry = { viewModel.loadLiveLocation(childId) },
            modifier = Modifier.fillMaxSize(),
        ) {
            BusTrackingContent(
                progress = state.routeProgress,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BusTrackingContent(
    progress: RouteProgress?,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val stops = progress?.stops ?: emptyList()
    val currentLoc = progress?.currentLocation

    Column(modifier) {
        // ── Map area ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(c.background)
        ) {
            if (stops.isNotEmpty() && currentLoc != null) {
                TransportMapCanvas(
                    stops = stops,
                    busLat = currentLoc.latitude,
                    busLng = currentLoc.longitude,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (stops.isNotEmpty()) {
                TransportMapCanvas(
                    stops = stops,
                    busLat = null,
                    busLng = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Waiting for bus location…",
                        style = VTheme.type.body,
                        color = c.ink3,
                    )
                }
            }
        }

        // ── Info card ─────────────────────────────────────────────────────
        VCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        progress?.routeName ?: "Route",
                        style = VTheme.type.h3,
                        color = c.ink,
                    )
                    Text(
                        "Bus: ${progress?.busNumber ?: "—"}",
                        style = VTheme.type.body,
                        color = c.ink2,
                    )
                }

                progress?.etaMinutes?.let { eta ->
                    VBadge(
                        text = "ETA $eta min",
                        tone = VBadgeTone.Accent,
                    )
                }
            }

            progress?.nextStop?.let { nextStop ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = VIcons.MapPin,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Next stop: ${nextStop.name}",
                        style = VTheme.type.body,
                        color = c.ink,
                    )
                    nextStop.estimatedTime?.let { time ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "($time)",
                            style = VTheme.type.caption,
                            color = c.ink3,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportMapCanvas(
    stops: List<TransportStop>,
    busLat: Double?,
    busLng: Double?,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    Canvas(modifier = modifier) {
        if (stops.isEmpty()) return@Canvas

        // Compute bounds
        val allLats = stops.map { it.latitude } + (busLat?.let { listOf(it) } ?: emptyList())
        val allLngs = stops.map { it.longitude } + (busLng?.let { listOf(it) } ?: emptyList())
        val minLat = allLats.min()
        val maxLat = allLats.max()
        val minLng = allLngs.min()
        val maxLng = allLngs.max()
        val latRange = (maxLat - minLat).takeIf { it > 0.0 } ?: 0.01
        val lngRange = (maxLng - minLng).takeIf { it > 0.0 } ?: 0.01

        val padding = 80f
        val w = size.width - padding * 2
        val h = size.height - padding * 2

        fun project(lat: Double, lng: Double): Offset {
            val x = padding + ((lng - minLng) / lngRange).toFloat() * w
            val y = padding + (1 - ((lat - minLat) / latRange).toFloat()) * h
            return Offset(x, y)
        }

        // Draw route polyline
        val path = Path()
        stops.forEachIndexed { i, stop ->
            val p = project(stop.latitude, stop.longitude)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color = c.accent.copy(alpha = 0.5f), style = Stroke(width = 6f))

        // Draw stops
        stops.forEach { stop ->
            val p = project(stop.latitude, stop.longitude)
            drawCircle(
                color = c.accent,
                radius = 10f,
                center = p,
            )
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = p,
            )
        }

        // Draw bus marker
        if (busLat != null && busLng != null) {
            val bp = project(busLat, busLng)
            drawCircle(
                color = c.danger.copy(alpha = 0.3f),
                radius = 24f,
                center = bp,
            )
            drawCircle(
                color = c.danger,
                radius = 14f,
                center = bp,
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = bp,
            )
        }
    }
}
