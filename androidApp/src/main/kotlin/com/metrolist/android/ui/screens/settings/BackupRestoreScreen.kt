@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.android.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.metrolist.android.backup.AutomaticBackupWorker
import com.metrolist.android.backup.BackupRestoreManager
import com.metrolist.android.onboarding.LegacyInstallMigration
import com.metrolist.shared.backup.AutoBackupFrequency
import com.metrolist.shared.backup.defaultBackupFilename
import com.metrolist.shared.backup.foreignImportMessage
import com.metrolist.shared.backup.importForeignBackup
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.action_backup
import com.metrolist.shared.generated.resources.action_restore
import com.metrolist.shared.generated.resources.automatic_backup
import com.metrolist.shared.generated.resources.automatic_backup_daily
import com.metrolist.shared.generated.resources.automatic_backup_description
import com.metrolist.shared.generated.resources.automatic_backup_frequency
import com.metrolist.shared.generated.resources.automatic_backup_location
import com.metrolist.shared.generated.resources.automatic_backup_monthly
import com.metrolist.shared.generated.resources.automatic_backup_restore
import com.metrolist.shared.generated.resources.automatic_backup_restore_empty
import com.metrolist.shared.generated.resources.automatic_backup_weekly
import com.metrolist.shared.generated.resources.backup_create_failed
import com.metrolist.shared.generated.resources.backup_create_success
import com.metrolist.shared.generated.resources.backup_filename_prefix
import com.metrolist.shared.generated.resources.backup_restore_busy
import com.metrolist.shared.generated.resources.backup_restore_export_desc
import com.metrolist.shared.generated.resources.backup_restore_import_desc
import com.metrolist.shared.generated.resources.backup_restore_title
import com.metrolist.shared.generated.resources.backup_sensitive_message
import com.metrolist.shared.generated.resources.backup_sensitive_title
import com.metrolist.shared.generated.resources.foreign_import_desc
import com.metrolist.shared.generated.resources.foreign_import_title
import com.metrolist.shared.generated.resources.legacy_recovery_export_desc
import com.metrolist.shared.generated.resources.legacy_recovery_export_failed
import com.metrolist.shared.generated.resources.legacy_recovery_export_success
import com.metrolist.shared.generated.resources.legacy_recovery_export_title
import com.metrolist.shared.generated.resources.restore_backup
import com.metrolist.shared.generated.resources.restore_confirm_message
import com.metrolist.shared.generated.resources.restore_confirm_title
import com.metrolist.shared.generated.resources.restore_database_incompatible
import com.metrolist.shared.generated.resources.restore_error_unknown
import com.metrolist.shared.generated.resources.restore_failed
import com.metrolist.shared.generated.resources.restore_failed_detail
import com.metrolist.shared.generated.resources.restore_in_progress
import com.metrolist.shared.generated.resources.restore_stage_applying_settings
import com.metrolist.shared.generated.resources.restore_stage_converting_database
import com.metrolist.shared.generated.resources.restore_stage_installing_database
import com.metrolist.shared.generated.resources.restore_stage_reading_backup
import com.metrolist.shared.generated.resources.restore_stage_reading_settings
import com.metrolist.shared.generated.resources.restore_stage_restarting_app
import com.metrolist.shared.generated.resources.restore_stage_stopping_app
import com.metrolist.shared.generated.resources.restore_stage_validating_database
import com.metrolist.shared.generated.resources.restore_stage_verifying_database
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.AlertDialog
import com.metrolist.shared.ui.components.PopupContent
import com.metrolist.shared.ui.components.PopupVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.res.stringResource as androidStringResource
import com.metrolist.android.ui.component.DensityAwareDialog as Dialog

@Composable
@Suppress("FunctionName")
fun BackupRestoreScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val appSettings = Settings.shared
    val database: MusicDatabase = koinInject()
    val applicationScope: CoroutineScope = koinInject()
    val scope = rememberCoroutineScope()

    val manager =
        remember(context, database) {
            BackupRestoreManager(context.applicationContext, database)
        }
    LaunchedEffect(appSettings.autoBackupEnabled, appSettings.autoBackupFrequency) {
        AutomaticBackupWorker.schedule(
            context.applicationContext,
            appSettings.autoBackupEnabled,
            AutoBackupFrequency.fromSetting(appSettings.autoBackupFrequency),
        )
    }

    var isRestoring by remember { mutableStateOf(false) }
    var playlistCollectionJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(database, isRestoring) {
        if (!isRestoring) {
            playlistCollectionJob = currentCoroutineContext().job
        }
    }

    var isBusy by remember { mutableStateOf(false) }
    val controlsEnabled = !isBusy

    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var automaticBackups by remember { mutableStateOf(emptyList<File>()) }
    var showAutomaticBackupPicker by remember { mutableStateOf(false) }
    var automaticBackupToRestore by remember { mutableStateOf<File?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var showBackupWarning by remember { mutableStateOf(false) }
    var warningSeconds by remember { mutableIntStateOf(5) }

    LaunchedEffect(showBackupWarning) {
        if (showBackupWarning) {
            warningSeconds = 5
            while (warningSeconds > 0) {
                delay(1_000.milliseconds)
                warningSeconds--
            }
        }
    }

    val backupSuccess = stringResource(Res.string.backup_create_success)
    val backupFailed = stringResource(Res.string.backup_create_failed)
    val backupFilenamePrefix = stringResource(Res.string.backup_filename_prefix)

    val createBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    isBusy = true
                    val result = manager.backup(uri)
                    isBusy = false
                    Toast.makeText(context, if (result.isSuccess) backupSuccess else backupFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }

    val recoveryExportSuccess = stringResource(Res.string.legacy_recovery_export_success)
    val recoveryExportFailed = stringResource(Res.string.legacy_recovery_export_failed)
    var legacyRecoveryCopy by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(Unit) {
        legacyRecoveryCopy = withContext(Dispatchers.IO) { LegacyInstallMigration.latestRecoveryCopy(context) }
    }
    val exportRecoveryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri ->
            val recovery = legacyRecoveryCopy
            if (uri != null && recovery != null) {
                scope.launch {
                    isBusy = true
                    val result = manager.exportRecoveryCopy(recovery, uri)
                    isBusy = false
                    Toast
                        .makeText(
                            context,
                            if (result.isSuccess) recoveryExportSuccess else recoveryExportFailed,
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                restoreUri = uri
            }
        }

    val foreignImportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                isBusy = true
                applicationScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    database.importForeignBackup(input, File(context.cacheDir, "foreign-import"), AndroidSQLiteDriver())
                                } ?: error("Could not open backup")
                            }
                        }
                    val message = result.foreignImportMessage()
                    withContext(Dispatchers.Main) {
                        isBusy = false
                        Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    PopupVisibility(visible = showBackupWarning) {
        AlertDialog(
            onDismissRequest = { showBackupWarning = false },
            title = { Text(stringResource(Res.string.backup_sensitive_title)) },
            text = {
                Text(
                    stringResource(Res.string.backup_sensitive_message),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = warningSeconds == 0,
                    onClick = {
                        showBackupWarning = false
                        runCatching { createBackupLauncher.launch("${defaultBackupFilename(backupFilenamePrefix)}.zip") }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(
                        if (warningSeconds == 0) {
                            stringResource(Res.string.action_backup)
                        } else {
                            "${stringResource(Res.string.action_backup)} ($warningSeconds)"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupWarning = false
                }, shapes = ButtonDefaults.shapes()) { Text(androidStringResource(android.R.string.cancel)) }
            },
        )
    }

    PopupContent(restoreUri) { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text(stringResource(Res.string.restore_confirm_title)) },
            text = { Text(stringResource(Res.string.restore_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreUri = null
                        scope.launch {
                            isBusy = true
                            isRestoring = true
                            playlistCollectionJob?.cancelAndJoin()
                            val result = manager.restore(uri)
                            isBusy = false
                            isRestoring = false
                            result.exceptionOrNull()?.let { error ->
                                restoreError = error.restoreMessage()
                            }
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreUri = null }, shapes = ButtonDefaults.shapes()) {
                    Text(androidStringResource(android.R.string.cancel))
                }
            },
        )
    }

    PopupVisibility(visible = showAutomaticBackupPicker) {
        AlertDialog(
            onDismissRequest = { showAutomaticBackupPicker = false },
            title = { Text(stringResource(Res.string.automatic_backup_restore)) },
            text = {
                if (automaticBackups.isEmpty()) {
                    Text(stringResource(Res.string.automatic_backup_restore_empty))
                } else {
                    LazyColumn {
                        items(automaticBackups, key = File::getName) { backup ->
                            TextButton(
                                onClick = {
                                    automaticBackupToRestore = backup
                                    showAutomaticBackupPicker = false
                                },
                            ) {
                                Text(backup.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAutomaticBackupPicker = false
                        runCatching {
                            restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        }
                    },
                ) {
                    Text(stringResource(Res.string.restore_backup))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutomaticBackupPicker = false }) {
                    Text(androidStringResource(android.R.string.cancel))
                }
            },
        )
    }

    PopupContent(automaticBackupToRestore) { backup ->
        AlertDialog(
            onDismissRequest = { automaticBackupToRestore = null },
            title = { Text(stringResource(Res.string.restore_confirm_title)) },
            text = { Text(stringResource(Res.string.restore_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        automaticBackupToRestore = null
                        scope.launch {
                            isBusy = true
                            isRestoring = true
                            playlistCollectionJob?.cancelAndJoin()
                            val result = manager.restore(backup)
                            isBusy = false
                            isRestoring = false
                            result.exceptionOrNull()?.let { error -> restoreError = error.restoreMessage() }
                        }
                    },
                ) {
                    Text(stringResource(Res.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { automaticBackupToRestore = null }) {
                    Text(androidStringResource(android.R.string.cancel))
                }
            },
        )
    }

    PopupContent(restoreError) { message ->
        AlertDialog(
            onDismissRequest = { restoreError = null },
            title = { Text(stringResource(Res.string.restore_failed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { restoreError = null }, shapes = ButtonDefaults.shapes()) {
                    Text(androidStringResource(android.R.string.ok))
                }
            },
        )
    }

    PopupVisibility(visible = isRestoring) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        stringResource(Res.string.restore_in_progress),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    SettingsSubScreen(
        title = stringResource(Res.string.backup_restore_title),
        onNavigateBack = onNavigateBack,
    ) {
        val frequency = AutoBackupFrequency.fromSetting(appSettings.autoBackupFrequency)
        SettingsSubScreenGroup(
            title = stringResource(Res.string.automatic_backup),
            description = stringResource(Res.string.automatic_backup_location),
            items =
                listOf(
                    SettingsSubScreenItem(
                        title = stringResource(Res.string.automatic_backup),
                        description = stringResource(Res.string.automatic_backup_description),
                        checked = appSettings.autoBackupEnabled,
                        onClick = { appSettings.autoBackupEnabled = !appSettings.autoBackupEnabled },
                    ),
                    listChooserSettingsItem(
                        title = stringResource(Res.string.automatic_backup_frequency),
                        options = AutoBackupFrequency.entries,
                        selectedOption = frequency,
                        optionToString = { selectedFrequency ->
                            stringResource(
                                when (selectedFrequency) {
                                    AutoBackupFrequency.DAILY -> Res.string.automatic_backup_daily
                                    AutoBackupFrequency.WEEKLY -> Res.string.automatic_backup_weekly
                                    AutoBackupFrequency.MONTHLY -> Res.string.automatic_backup_monthly
                                },
                            )
                        },
                        enabled = appSettings.autoBackupEnabled,
                    ) { selectedFrequency -> appSettings.autoBackupFrequency = selectedFrequency.name },
                ),
        )
        if (isBusy) LinearWavyProgressIndicator(Modifier.fillMaxWidth())
        SettingsSubScreenGroup(
            title = stringResource(Res.string.backup_restore_title),
            items =
                listOfNotNull(
                    SettingsSubScreenItem(
                        title = stringResource(Res.string.action_backup),
                        description =
                            if (isBusy) {
                                stringResource(
                                    Res.string.backup_restore_busy,
                                )
                            } else {
                                stringResource(Res.string.backup_restore_export_desc)
                            },
                        enabled = controlsEnabled,
                        onClick = { showBackupWarning = true },
                    ),
                    SettingsSubScreenItem(
                        title = stringResource(Res.string.action_restore),
                        description =
                            if (isBusy) {
                                stringResource(
                                    Res.string.backup_restore_busy,
                                )
                            } else {
                                stringResource(Res.string.backup_restore_import_desc)
                            },
                        enabled = controlsEnabled,
                        onClick = {
                            scope.launch {
                                automaticBackups =
                                    withContext(Dispatchers.IO) {
                                        File(context.filesDir, "automatic-backups")
                                            .listFiles { file -> file.isFile && file.extension == "zip" }
                                            .orEmpty()
                                            .sortedByDescending(File::lastModified)
                                    }
                                if (automaticBackups.isNotEmpty()) {
                                    showAutomaticBackupPicker = true
                                } else {
                                    runCatching {
                                        restoreLauncher.launch(
                                            arrayOf("application/zip", "application/octet-stream", "*/*"),
                                        )
                                    }
                                }
                            }
                        },
                    ),
                    legacyRecoveryCopy?.let {
                        SettingsSubScreenItem(
                            title = stringResource(Res.string.legacy_recovery_export_title),
                            description = stringResource(Res.string.legacy_recovery_export_desc),
                            enabled = controlsEnabled,
                            onClick = {
                                runCatching { exportRecoveryLauncher.launch("${defaultBackupFilename(backupFilenamePrefix)}-v13.zip") }
                            },
                        )
                    },
                    SettingsSubScreenItem(
                        title = stringResource(Res.string.foreign_import_title),
                        description =
                            if (isBusy) {
                                stringResource(Res.string.backup_restore_busy)
                            } else {
                                stringResource(Res.string.foreign_import_desc)
                            },
                        enabled = controlsEnabled,
                        onClick = {
                            runCatching {
                                foreignImportLauncher.launch(
                                    arrayOf("application/zip", "application/octet-stream", "*/*"),
                                )
                            }
                        },
                    ),
                ),
        )
    }
}

private suspend fun Throwable.restoreMessage(): String =
    when (this) {
        is BackupRestoreManager.IncompatibleBackupException -> {
            getString(
                Res.string.restore_database_incompatible,
                backupVersion,
                currentVersion,
            )
        }

        is BackupRestoreManager.RestoreFailedException -> {
            getString(
                Res.string.restore_failed_detail,
                getString(stage.descriptionResource),
                cause?.message?.takeIf(String::isNotBlank) ?: getString(Res.string.restore_error_unknown),
            )
        }

        else -> {
            getString(
                Res.string.restore_failed_detail,
                getString(Res.string.restore_stage_reading_backup),
                message?.takeIf(String::isNotBlank) ?: getString(Res.string.restore_error_unknown),
            )
        }
    }

private val BackupRestoreManager.RestoreStage.descriptionResource: StringResource
    get() =
        when (this) {
            BackupRestoreManager.RestoreStage.READING_BACKUP -> Res.string.restore_stage_reading_backup
            BackupRestoreManager.RestoreStage.READING_SETTINGS -> Res.string.restore_stage_reading_settings
            BackupRestoreManager.RestoreStage.VALIDATING_DATABASE -> Res.string.restore_stage_validating_database
            BackupRestoreManager.RestoreStage.CONVERTING_DATABASE -> Res.string.restore_stage_converting_database
            BackupRestoreManager.RestoreStage.STOPPING_APP -> Res.string.restore_stage_stopping_app
            BackupRestoreManager.RestoreStage.INSTALLING_DATABASE -> Res.string.restore_stage_installing_database
            BackupRestoreManager.RestoreStage.VERIFYING_DATABASE -> Res.string.restore_stage_verifying_database
            BackupRestoreManager.RestoreStage.APPLYING_SETTINGS -> Res.string.restore_stage_applying_settings
            BackupRestoreManager.RestoreStage.RESTARTING_APP -> Res.string.restore_stage_restarting_app
        }
