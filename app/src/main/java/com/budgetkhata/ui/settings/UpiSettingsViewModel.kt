package com.budgetkhata.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.R
import com.budgetkhata.core.Validation
import com.budgetkhata.data.prefs.DEFAULT_PAYMENT_INSTRUCTIONS
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.launch

data class UpiFormState(
    val upiId: String = "",
    val payeeName: String = "",
    val instructions: String = DEFAULT_PAYMENT_INSTRUCTIONS,
    val upiIdError: Int? = null,
    val payeeError: Int? = null
)

class UpiSettingsViewModel(private val container: AppContainer) : ViewModel() {

    var form by mutableStateOf(UpiFormState())
        private set
    var userQrPath by mutableStateOf<String?>(null)
        private set
    var message: Int? by mutableStateOf(null)
        private set
    var saving by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val settings = container.settingsRepository.current()
            form = UpiFormState(
                upiId = settings.upiId,
                payeeName = settings.payeeName,
                instructions = settings.paymentInstructions
            )
            userQrPath = settings.userQrPath
        }
    }

    fun updateUpiId(value: String) {
        form = form.copy(upiId = value, upiIdError = null)
    }

    fun updatePayeeName(value: String) {
        form = form.copy(payeeName = value, payeeError = null)
    }

    fun updateInstructions(value: String) {
        form = form.copy(instructions = value)
    }

    fun clearMessage() {
        message = null
    }

    fun updateUserQrPath(path: String?) {
        viewModelScope.launch {
            container.settingsRepository.setUserQrPath(path)
            userQrPath = path
            message = if (path == null) R.string.custom_qr_removed else R.string.custom_qr_saved
        }
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        val upiError = Validation.upiIdOptional(form.upiId)
        val payeeError = if (form.upiId.isNotBlank() && form.payeeName.isBlank()) {
            R.string.payee_name_required
        } else {
            null
        }

        if (upiError != null || payeeError != null) {
            form = form.copy(upiIdError = upiError, payeeError = payeeError)
            return
        }

        viewModelScope.launch {
            saving = true
            container.settingsRepository.setUpi(
                upiId = form.upiId,
                payeeName = form.payeeName,
                instructions = form.instructions
            )
            saving = false
            message = R.string.upi_settings_saved
            onSaved()
        }
    }
}
