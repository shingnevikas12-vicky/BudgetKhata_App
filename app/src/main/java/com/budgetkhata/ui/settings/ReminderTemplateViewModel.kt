package com.budgetkhata.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.prefs.DEFAULT_REMINDER_TEMPLATE
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderTemplateViewModel(private val container: AppContainer) : ViewModel() {

    var template by mutableStateOf(DEFAULT_REMINDER_TEMPLATE)
        private set
    var sampleRow by mutableStateOf<EnrollmentRow?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            template = container.settingsRepository.current().reminderTemplate
            val rows = container.enrollmentRepository.observeRows(includeArchived = false).first()
            sampleRow = rows.firstOrNull { it.remainingPaise > 0L } ?: rows.firstOrNull()
            loaded = true
        }
    }

    fun updateTemplate(value: String) {
        template = value
    }

    fun appendPlaceholder(placeholder: String) {
        template = if (template.endsWith(" ") || template.isEmpty()) {
            template + placeholder
        } else {
            "$template $placeholder"
        }
    }

    fun resetToDefault() {
        template = DEFAULT_REMINDER_TEMPLATE
    }

    fun clearMessage() {
        message = null
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        if (template.isBlank()) {
            message = "The reminder message cannot be empty"
            return
        }
        viewModelScope.launch {
            saving = true
            container.settingsRepository.setReminderTemplate(template)
            saving = false
            message = "Reminder message saved"
            onSaved()
        }
    }
}
