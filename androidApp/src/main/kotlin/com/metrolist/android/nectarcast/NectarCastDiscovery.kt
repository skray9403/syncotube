/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.ext.SdkExtensions
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NectarCastDiscovery(
    private val context: Context,
    private val identity: NectarDeviceIdentity,
    private val transport: NectarTransportBridge,
) {
    companion object {
        const val MDNS_SERVICE_TYPE = "_nectarcast._udp."
        val BLE_SERVICE_UUID: ParcelUuid =
            ParcelUuid(UUID.fromString("7e4a0001-9c2d-4f6b-a31e-5d8f0c2b9100"))
    }

    @OptIn(FlowPreview::class)
    fun scanAll(
        canUseLocalNetwork: Boolean,
        canScanBle: Boolean,
    ): Flow<List<NectarPeer>> {
        val peers = ConcurrentHashMap<String, NectarPeer>()
        val sources = buildList {
            if (canUseLocalNetwork) add(scanMdnsLan())
            if (canScanBle) add(scanBle())
            add(transport.observeNectarLinkPeers())
        }
        if (sources.isEmpty()) return flowOf(emptyList())

        return merge(*sources.toTypedArray())
            .filterNot { delta ->
                when (delta) {
                    is RawDiscoveryDelta.Upsert -> delta.deviceId == identity.deviceId
                    is RawDiscoveryDelta.RemoveRoute -> delta.deviceId == identity.deviceId
                }
            }
            .map { delta ->
                when (delta) {
                    is RawDiscoveryDelta.Upsert -> {
                        peers.compute(delta.deviceId) { _, prev ->
                            if (prev == null) {
                                NectarPeer(
                                    deviceId = delta.deviceId,
                                    displayName = delta.displayName,
                                    platform = delta.platform,
                                    paired = delta.paired,
                                    routes = listOf(delta.route),
                                )
                            } else {
                                val mergedRoutes = (prev.routes.filterNot {
                                    it.protocol == delta.route.protocol
                                } + delta.route).sortedBy { it.protocol.priority }

                                prev.copy(
                                    displayName = delta.displayName.ifBlank { prev.displayName },
                                    platform = if (prev.platform == NectarPlatform.UNKNOWN) delta.platform else prev.platform,
                                    paired = prev.paired || delta.paired,
                                    routes = mergedRoutes,
                                )
                            }
                        }
                    }

                    is RawDiscoveryDelta.RemoveRoute -> {
                        peers.computeIfPresent(delta.deviceId) { _, prev ->
                            val remaining = prev.routes.filterNot { it.protocol == delta.protocol }
                            if (remaining.isEmpty()) null else prev.copy(routes = remaining)
                        }
                    }
                }

                peers.values.sortedWith(
                    compareBy<NectarPeer> { !it.paired }
                        .thenBy { it.primaryRoute.protocol.priority }
                        .thenBy { it.displayName.lowercase() },
                )
            }
            .debounce(200L)
    }

    private fun scanMdnsLan(): Flow<RawDiscoveryDelta> = callbackFlow {
        val nsd = context.getSystemService(NsdManager::class.java)
        val mainHandler = Handler(Looper.getMainLooper())
        val multicastLock =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) < 7
            ) {
                context.applicationContext
                    .getSystemService(WifiManager::class.java)
                    ?.createMulticastLock("nectarcast-mdns")
                    ?.apply {
                        setReferenceCounted(false)
                        runCatching { acquire() }
                    }
            } else {
                null
            }

        val pending = ArrayDeque<NsdServiceInfo>()
        val serviceNameToPeer = mutableMapOf<String, Pair<String, NectarProtocol>>()
        var resolving = false
        var active = true

        @Suppress("DEPRECATION")
        fun resolveNext() {
            if (!active || resolving || pending.isEmpty()) return
            resolving = true
            val next = pending.removeFirst()
            runCatching {
                nsd.resolveService(
                    next,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            mainHandler.post {
                                resolving = false
                                resolveNext()
                            }
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val attrs = info.attributes
                            val id = attrs["id"]?.decodeToString()
                            val host = info.host?.hostAddress
                            if (!id.isNullOrBlank() && !host.isNullOrBlank()) {
                                val name = attrs["name"]?.decodeToString()?.takeIf { it.isNotBlank() }
                                    ?: info.serviceName
                                val platform = attrs["plat"]?.decodeToString()?.toIntOrNull()
                                    ?.let { NectarPlatform.entries.getOrNull(it) }
                                    ?: NectarPlatform.UNKNOWN
                                val proto = when (attrs["t"]?.decodeToString()) {
                                    "quic" -> NectarProtocol.WIFI_QUIC
                                    else -> NectarProtocol.WIFI_TCP
                                }
                                mainHandler.post {
                                    serviceNameToPeer[info.serviceName] = id to proto
                                }
                                trySend(
                                    RawDiscoveryDelta.Upsert(
                                        deviceId = id,
                                        displayName = name,
                                        platform = platform,
                                        paired = true,
                                        route = NectarRoute(
                                            protocol = proto,
                                            host = host,
                                            port = info.port,
                                        ),
                                    ),
                                )
                            }
                            mainHandler.post {
                                resolving = false
                                resolveNext()
                            }
                        }
                    },
                )
            }.onFailure {
                resolving = false
                resolveNext()
            }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                mainHandler.post {
                    if (!active) return@post
                    pending.addLast(serviceInfo)
                    resolveNext()
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                mainHandler.post {
                    pending.removeAll { it.serviceName == serviceInfo.serviceName }
                    val lost = serviceNameToPeer.remove(serviceInfo.serviceName) ?: return@post
                    trySend(RawDiscoveryDelta.RemoveRoute(lost.first, lost.second))
                }
            }
        }

        runCatching {
            nsd.discoverServices(MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { close(it) }

        awaitClose {
            active = false
            runCatching { nsd.stopServiceDiscovery(listener) }
            if (multicastLock?.isHeld == true) multicastLock.release()
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanBle(): Flow<RawDiscoveryDelta> = callbackFlow {
        val scanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            close()
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord
                    ?.getManufacturerSpecificData(NectarCastAdvertiser.BLE_MANUFACTURER_ID)
                    ?: return
                if (data.size < 20) return // [ver:1][dev_id:16][plat:1][port_hi:1][port_lo:1]

                val deviceId = data.sliceArray(1..16).joinToString("") { "%02x".format(it) }
                val platform = NectarPlatform.entries.getOrNull(data[17].toInt() and 0xFF)
                    ?: NectarPlatform.UNKNOWN
                val advertisedPort = ((data[18].toInt() and 0xFF) shl 8) or (data[19].toInt() and 0xFF)
                val name = result.scanRecord?.deviceName ?: result.device?.name ?: "Nearby Device"

                trySend(
                    RawDiscoveryDelta.Upsert(
                        deviceId = deviceId,
                        displayName = name,
                        platform = platform,
                        paired = false,
                        route = NectarRoute(
                            protocol = NectarProtocol.BLE_WAKE,
                            port = advertisedPort.takeIf { it > 0 },
                            bleAddress = result.device?.address,
                        ),
                    ),
                )
            }
        }

        val filter = ScanFilter.Builder().setServiceUuid(BLE_SERVICE_UUID).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching { scanner.startScan(listOf(filter), settings, callback) }
            .onFailure { close(it) }

        awaitClose { runCatching { scanner.stopScan(callback) } }
    }
}
