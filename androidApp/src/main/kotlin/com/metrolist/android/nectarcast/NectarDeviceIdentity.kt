/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import android.content.Context
import android.os.Build
import java.security.SecureRandom

class NectarDeviceIdentity(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences("nectarcast_id", Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        prefs.getString("device_id", null) ?: ByteArray(16)
            .also {
                SecureRandom().nextBytes(it)
            }.joinToString("") { "%02x".format(it) }
            .also { generated ->
                prefs.edit().putString("device_id", generated).apply()
            }
    }

    val deviceName: String
        get() =
            prefs.getString("device_name", null)
                ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()

    fun deviceIdBytes(): ByteArray =
        deviceId.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
