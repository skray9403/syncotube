package com.metrolist.android.nectarcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.ui.icons.MetrolistIcons

@Composable
fun NectarCastSection(
    peers: List<NectarPeer>,
    session: NectarSessionUiState,
    isScanning: Boolean,
    missingBlePermission: Boolean,
    onRequestBlePermission: () -> Unit,
    onSelectPeer: (NectarPeer) -> Unit,
    onReturnToPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "NectarCast (Wi-Fi · BLE · QUIC)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        if (session.activePeer != null) {
            FilledTonalButton(
                onClick = onReturnToPhone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(MetrolistIcons.Outlined.PhoneAndroid, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Playing on ${session.activePeer.displayName} · Return to phone")
            }
        }

        if (missingBlePermission) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Bluetooth scan off — Wi-Fi & NectarLink active",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRequestBlePermission) { Text("Allow") }
                }
            }
        }

        session.lastError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (peers.isEmpty() && !isScanning) {
            Text(
                text = "No NectarCast daemons detected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            peers.forEach { peer ->
                val isActive = session.activePeer?.deviceId == peer.deviceId
                val isTransferring = session.transferringDeviceId == peer.deviceId
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransferring) { onSelectPeer(peer) },
                    leadingContent = {
                        Icon(
                            imageVector = when (peer.platform) {
                                NectarPlatform.WINDOWS, NectarPlatform.LINUX -> MetrolistIcons.Outlined.Computer
                                NectarPlatform.ANDROID -> MetrolistIcons.Outlined.PhoneAndroid
                                NectarPlatform.TIZEN -> MetrolistIcons.Filled.Tv
                                NectarPlatform.UNKNOWN -> MetrolistIcons.Outlined.Devices
                            },
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    headlineContent = {
                        Text(peer.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        val extras = if (peer.routes.size > 1) " (+${peer.routes.size - 1} route)" else ""
                        Text("${peer.primaryRoute.protocol.label}$extras")
                    },
                    trailingContent = {
                        when {
                            isTransferring -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            isActive -> Icon(
                                MetrolistIcons.Rounded.Cast,
                                contentDescription = "Connected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }

        HorizontalDivider()
    }
}
