package com.budgetkhata.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.R
import com.budgetkhata.data.backup.BackupCounts
import com.budgetkhata.data.backup.BackupImportResult
import com.budgetkhata.data.backup.BackupSettings
import com.budgetkhata.data.prefs.ThemeMode
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RestorePreview(val json: String, val counts: BackupCounts)

class DataSettingsViewModel(private val container: AppContainer) : ViewModel() {

    var message: Any? by mutableStateOf(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var restorePreview by mutableStateOf<RestorePreview?>(null)
        private set

    fun clearMessage() {
        message = null
    }

    suspend fun buildBackupJson(): String {
        val current = container.settingsRepository.current()
        return container.backupRepository.exportJson(
            BackupSettings(
                upiId = current.upiId,
                payeeName = current.payeeName,
                paymentInstructions = current.paymentInstructions,
                defaultCountryCode = current.defaultCountryCode,
                reminderTemplate = current.reminderTemplate,
                themeMode = current.themeMode.name,
                dynamicColor = current.dynamicColor,
                lockOnBackground = current.lockOnBackground,
                screenshotProtection = current.screenshotProtection,
                allowOverpayment = current.allowOverpayment
            )
        )
    }

    fun prepareRestore(json: String) {
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.Default) { container.backupRepository.inspect(json) }
            busy = false
            when (result) {
                is BackupImportResult.Success -> restorePreview = RestorePreview(json, result.counts)
                is BackupImportResult.Error -> message = result.message
            }
        }
    }

    fun cancelRestore() {
        restorePreview = null
    }

    fun confirmRestore() {
        val preview = restorePreview ?: return
        viewModelScope.launch {
            busy = true
            when (val result = container.backupRepository.importJson(preview.json)) {
                is BackupImportResult.Success -> {
                    applySettings(result.settings)
                    message = Triple(
                        R.string.restore_success_msg,
                        listOf(result.counts.students, result.counts.events, result.counts.payments, result.counts.expenses),
                        true
                    )
                }

                is BackupImportResult.Error -> message = result.message
            }
            restorePreview = null
            busy = false
        }
    }

    fun deleteEverything() {
        viewModelScope.launch {
            busy = true
            container.backupRepository.deleteEverything()
            busy = false
            message = R.string.delete_everything_success
        }
    }

    private suspend fun applySettings(backup: BackupSettings?) {
        if (backup == null) return
        val current = container.settingsRepository.current()
        container.settingsRepository.applySettings(
            current.copy(
                upiId = backup.upiId ?: current.upiId,
                payeeName = backup.payeeName ?: current.payeeName,
                paymentInstructions = backup.paymentInstructions ?: current.paymentInstructions,
                defaultCountryCode = backup.defaultCountryCode ?: current.defaultCountryCode,
                reminderTemplate = backup.reminderTemplate ?: current.reminderTemplate,
                themeMode = backup.themeMode?.let { ThemeMode.fromName(it) } ?: current.themeMode,
                dynamicColor = backup.dynamicColor ?: current.dynamicColor,
                lockOnBackground = backup.lockOnBackground ?: current.lockOnBackground,
                screenshotProtection = backup.screenshotProtection ?: current.screenshotProtection,
                allowOverpayment = backup.allowOverpayment ?: current.allowOverpayment
            )
        )
    }
}
