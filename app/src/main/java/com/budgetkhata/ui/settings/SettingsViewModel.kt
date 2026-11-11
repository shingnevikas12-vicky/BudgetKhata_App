package com.budgetkhata.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.data.prefs.AppLanguage
import com.budgetkhata.data.prefs.AppSettings
import com.budgetkhata.data.prefs.ThemeMode
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { container.settingsRepository.setLanguage(language) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setDynamicColor(enabled) }
    }

    fun setScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setScreenshotProtection(enabled) }
    }

    fun setAllowOverpayment(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAllowOverpayment(enabled) }
    }
}
