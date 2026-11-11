package com.budgetkhata.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_khata_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val UPI_ID = stringPreferencesKey("upi_id")
        val PAYEE_NAME = stringPreferencesKey("payee_name")
        val INSTRUCTIONS = stringPreferencesKey("payment_instructions")
        val USER_QR_PATH = stringPreferencesKey("user_qr_path")
        val COUNTRY_CODE = stringPreferencesKey("default_country_code")
        val REMINDER_TEMPLATE = stringPreferencesKey("reminder_template")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LOCK_ON_BACKGROUND = booleanPreferencesKey("lock_on_background")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val ALLOW_OVERPAYMENT = booleanPreferencesKey("allow_overpayment")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                upiId = prefs[Keys.UPI_ID] ?: defaults.upiId,
                payeeName = prefs[Keys.PAYEE_NAME] ?: defaults.payeeName,
                paymentInstructions = prefs[Keys.INSTRUCTIONS] ?: defaults.paymentInstructions,
                userQrPath = prefs[Keys.USER_QR_PATH],
                defaultCountryCode = prefs[Keys.COUNTRY_CODE] ?: defaults.defaultCountryCode,
                reminderTemplate = prefs[Keys.REMINDER_TEMPLATE] ?: defaults.reminderTemplate,
                themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
                language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
                lockOnBackground = prefs[Keys.LOCK_ON_BACKGROUND] ?: defaults.lockOnBackground,
                screenshotProtection = prefs[Keys.SCREENSHOT_PROTECTION] ?: defaults.screenshotProtection,
                allowOverpayment = prefs[Keys.ALLOW_OVERPAYMENT] ?: defaults.allowOverpayment
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setUpi(upiId: String, payeeName: String, instructions: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.UPI_ID] = upiId.trim()
            prefs[Keys.PAYEE_NAME] = payeeName.trim()
            prefs[Keys.INSTRUCTIONS] = instructions.trim()
        }
    }

    suspend fun setUserQrPath(path: String?) {
        context.settingsDataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.USER_QR_PATH) else prefs[Keys.USER_QR_PATH] = path
        }
    }

    suspend fun setCountryCode(code: String) {
        context.settingsDataStore.edit { it[Keys.COUNTRY_CODE] = code.filter { ch -> ch.isDigit() } }
    }

    suspend fun setReminderTemplate(template: String) {
        context.settingsDataStore.edit { it[Keys.REMINDER_TEMPLATE] = template }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLockOnBackground(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LOCK_ON_BACKGROUND] = enabled }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SCREENSHOT_PROTECTION] = enabled }
    }

    suspend fun setAllowOverpayment(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALLOW_OVERPAYMENT] = enabled }
    }

    suspend fun applySettings(value: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.UPI_ID] = value.upiId
            prefs[Keys.PAYEE_NAME] = value.payeeName
            prefs[Keys.INSTRUCTIONS] = value.paymentInstructions
            if (value.userQrPath == null) prefs.remove(Keys.USER_QR_PATH) else prefs[Keys.USER_QR_PATH] = value.userQrPath
            prefs[Keys.COUNTRY_CODE] = value.defaultCountryCode
            prefs[Keys.REMINDER_TEMPLATE] = value.reminderTemplate
            prefs[Keys.THEME_MODE] = value.themeMode.name
            prefs[Keys.LANGUAGE] = value.language.code
            prefs[Keys.DYNAMIC_COLOR] = value.dynamicColor
            prefs[Keys.LOCK_ON_BACKGROUND] = value.lockOnBackground
            prefs[Keys.SCREENSHOT_PROTECTION] = value.screenshotProtection
            prefs[Keys.ALLOW_OVERPAYMENT] = value.allowOverpayment
        }
    }
}
