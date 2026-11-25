/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.metrolist.android.LocalMiniPlayerBottomInset
import com.metrolist.android.ui.screens.rememberDeviceConnectSettingsHost
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.copied
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.SharedIntegrationsHost
import com.metrolist.shared.ui.settings.SharedIntegrationsScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun IntegrationsScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToListenTogether: () -> Unit = {},
    initialPage: SettingsPageId? = null,
) {
    val copiedMessage = stringResource(Res.string.copied)
    val context = LocalContext.current

    SharedIntegrationsScreen(
        host =
            SharedIntegrationsHost(
                platform = SettingsPlatform.ANDROID,
                onClose = onNavigateBack,
                bottomContentPadding = LocalMiniPlayerBottomInset.current,
                initialPage = initialPage ?: SettingsPageId.DEVICE_CONNECT,
                includeSocialIntegrations = false,
                deviceConnect = rememberDeviceConnectSettingsHost(),
                onCopyText = { label, text -> context.copyToClipboard(label, text, copiedMessage) },
                onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
            ),
    )
}

private fun Context.copyToClipboard(
    label: String,
    text: String,
    copiedMessage: String,
) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, copiedMessage, Toast.LENGTH_SHORT).show()
}
