package com.budgetkhata.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.core.Dates
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.InfoBanner
import com.budgetkhata.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DataSettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel = appViewModel(container) { app, _ -> DataSettingsViewModel(app) }
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var working by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }

    val backupSavedMsg = stringResource(R.string.backup_saved)
    val backupSaveFailedMsg = stringResource(R.string.backup_save_failed)
    val backupReadFailedMsg = stringResource(R.string.backup_read_failed)

    LaunchedEffect(viewModel.message) {
        val message = viewModel.message
        if (message != null) {
            val text = when (message) {
                is Int -> context.getString(message)
                is String -> message
                is Triple<*, *, *> -> {
                    val resId = message.first as Int
                    val args = (message.second as List<*>).toTypedArray()
                    context.getString(resId, *args)
                }
                else -> message.toString()
            }
            snackbar?.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                working = true
                val json = runCatching { viewModel.buildBackupJson() }.getOrNull()
                val saved = if (json == null) {
                    false
                } else {
                    withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                stream.write(json.toByteArray(Charsets.UTF_8))
                            }
                            true
                        } catch (error: Exception) {
                            false
                        }
                    }
                }
                working = false
                snackbar?.showSnackbar(if (saved) backupSavedMsg else backupSaveFailedMsg)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                working = true
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    } catch (error: Exception) {
                        null
                    }
                }
                working = false
                if (text.isNullOrBlank()) {
                    snackbar?.showSnackbar(backupReadFailedMsg)
                } else {
                    viewModel.prepareRestore(text)
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.backup_restore_title), onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (working || viewModel.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            SectionHeader(
                title = stringResource(R.string.data_stays_phone),
                subtitle = stringResource(R.string.data_stays_phone_msg)
            )

            InfoBanner(
                text = stringResource(R.string.backup_file_warning)
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.export_backup), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.export_backup_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            exportLauncher.launch("budget-khata-backup-${Dates.fileStamp()}.json")
                        },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.choose_save_location))
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.restore_backup), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.restore_backup_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream")
                            )
                        },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.select_backup_file))
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_all_records), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.delete_all_records_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            deleteText = ""
                            showDeleteDialog = true
                        },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.delete_all_data))
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        }
    }

    viewModel.restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text(stringResource(R.string.replace_all_data_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.backup_contains_msg), fontWeight = FontWeight.SemiBold)
                    Text("\u2022 ${preview.counts.students} " + stringResource(R.string.students))
                    Text("\u2022 ${preview.counts.events} " + stringResource(R.string.events))
                    Text("\u2022 ${preview.counts.enrollments} enrollments")
                    Text("\u2022 ${preview.counts.payments} " + stringResource(R.string.payments))
                    Text("\u2022 ${preview.counts.expenses} " + stringResource(R.string.expenses))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.replace_warning_msg),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_all_data_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cannot_be_undone_msg))
                    OutlinedTextField(
                        value = deleteText,
                        onValueChange = { deleteText = it },
                        label = { Text(stringResource(R.string.type_delete_label)) },
                        singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.tip_export_backup),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deleteText.trim().equals("DELETE", ignoreCase = true),
                    onClick = {
                        showDeleteDialog = false
                        deleteText = ""
                        viewModel.deleteEverything()
                    }
                ) { Text(stringResource(R.string.delete_everything), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
