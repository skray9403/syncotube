/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat

class NectarCastAdvertiser(
    private val context: Context,
    private val identity: NectarDeviceIdentity,
) {
    companion object {
        const val BLE_MANUFACTURER_ID = 0xFFFF
    }

    private val nsd = context.getSystemService(NsdManager::class.java)
    private var mdnsListener: NsdManager.RegistrationListener? = null
    private var bleCallback: AdvertiseCallback? = null
    @Volatile private var activePort: Int? = null

    fun start(listenPort: Int) {
        activePort = listenPort
        startMdns(listenPort)
        startBle(listenPort)
    }

    fun refresh() {
        val port = activePort ?: return
        if (mdnsListener == null) startMdns(port)
        if (bleCallback == null) startBle(port)
    }

    fun stop() {
        activePort = null
        mdnsListener?.let { listener ->
            runCatching { nsd.unregisterService(listener) }
        }
        mdnsListener = null

        bleCallback?.let { cb ->
            val advertiser = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
                ?.adapter?.bluetoothLeAdvertiser
            @SuppressLint("MissingPermission")
            runCatching { advertiser?.stopAdvertising(cb) }
        }
        bleCallback = null
    }

    private fun startMdns(listenPort: Int) {
        if (mdnsListener != null) return
        val info = NsdServiceInfo().apply {
            serviceName = "Nectar-${identity.deviceId.take(6)}"
            serviceType = NectarCastDiscovery.MDNS_SERVICE_TYPE
            port = listenPort
            setAttribute("id", identity.deviceId)
            setAttribute("name", identity.deviceName)
            setAttribute("plat", NectarPlatform.ANDROID.ordinal.toString())
            setAttribute("t", "tcp")
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                mdnsListener = null
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        mdnsListener = listener
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { mdnsListener = null }
    }

    @SuppressLint("MissingPermission")
    private fun startBle(listenPort: Int) {
        if (bleCallback != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val advertiser = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter?.bluetoothLeAdvertiser ?: return

        // Primary packet: 3B flags + 18B 128-bit UUID = 21B (<= 31B legacy BLE limit)
        val advData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(NectarCastDiscovery.BLE_SERVICE_UUID)
            .build()

        // Scan response: 2B header + 2B mfg ID + 20B payload = 24B (<= 31B limit)
        // Layout: [ver:1][dev_id:16][plat:1][port_hi:1][port_lo:1]
        val payload = ByteArray(20)
        payload[0] = 1
        identity.deviceIdBytes().copyInto(payload, destinationOffset = 1)
        payload[17] = NectarPlatform.ANDROID.ordinal.toByte()
        payload[18] = ((listenPort ushr 8) and 0xFF).toByte()
        payload[19] = (listenPort and 0xFF).toByte()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(BLE_MANUFACTURER_ID, payload)
            .build()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(false)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                bleCallback = null
            }
        }
        bleCallback = callback
        runCatching { advertiser.startAdvertising(settings, advData, scanResponse, callback) }
            .onFailure { bleCallback = null }
    }
}
