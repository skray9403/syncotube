package com.metrolist.android.utils

import android.content.Context
import android.content.Intent

fun Context.startActivitySafely(intent: Intent): Boolean {
    if (intent.resolveActivity(packageManager) == null) return false
    return runCatching {
        startActivity(intent)
        true
    }.getOrDefault(false)
}
