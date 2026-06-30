package com.littlebridge.enrollplus.ui.v2.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.core.connectivity.NetworkMonitor
import com.littlebridge.enrollplus.core.connectivity.NetworkStatus
import com.littlebridge.enrollplus.core.offline.sync.SyncStateHolder
import com.littlebridge.enrollplus.core.offline.sync.SyncStatus
import org.koin.compose.koinInject

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    networkMonitor: NetworkMonitor = koinInject(),
    syncStateHolder: SyncStateHolder = koinInject(),
) {
    val networkStatus by networkMonitor.status.collectAsState()
    val syncState by syncStateHolder.state.collectAsState()

    val showOffline = networkStatus == NetworkStatus.Unavailable
    val showSyncing = syncState.status == SyncStatus.SYNCING && syncState.pendingCount > 0
    val showSyncError = syncState.status == SyncStatus.ERROR && syncState.pendingCount > 0

    AnimatedVisibility(
        visible = showOffline || showSyncing || showSyncError,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        when {
            showOffline -> BannerRow(
                text = "You're offline. Changes will sync when you reconnect.",
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                dotColor = MaterialTheme.colorScheme.error,
            )
            showSyncing -> BannerRow(
                text = "Syncing ${syncState.pendingCount} pending change${if (syncState.pendingCount > 1) "s" else ""}…",
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                dotColor = MaterialTheme.colorScheme.tertiary,
            )
            showSyncError -> BannerRow(
                text = "Sync failed: ${syncState.lastError ?: "Unknown error"}. Will retry.",
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                dotColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BannerRow(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}
