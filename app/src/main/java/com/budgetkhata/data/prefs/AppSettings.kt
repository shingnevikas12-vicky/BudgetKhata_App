package com.budgetkhata.data.prefs

import com.budgetkhata.R

enum class ThemeMode(val labelRes: Int) {
    SYSTEM(R.string.follow_system),
    LIGHT(R.string.light),
    DARK(R.string.dark);

    companion object {
        fun fromName(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class AppLanguage(val label: String, val code: String) {
    ENGLISH("English", "en"),
    MARATHI("मराठी", "mr");

    companion object {
        fun fromCode(value: String?): AppLanguage = entries.firstOrNull { it.code == value } ?: ENGLISH
    }
}

const val DEFAULT_PAYMENT_INSTRUCTIONS =
    "Pay using the UPI ID or QR code below, then share the transaction screenshot. " +
        "Your contribution is marked paid only after the organiser verifies it."

const val DEFAULT_REMINDER_TEMPLATE =
    "Hello {name},\n\n" +
        "This is a reminder regarding your contribution for our {event}.\n\n" +
        "Total Contribution: {total}\n" +
        "Amount Paid: {paid}\n" +
        "Pending Amount: {pending}\n\n" +
        "Please pay the remaining {pending} using the UPI details or payment QR code shared by the organiser.\n\n" +
        "{upi_block}" +
        "After making the payment, please share the transaction confirmation.\n\n" +
        "Thank you!"

data class AppSettings(
    val upiId: String = DEFAULT_UPI_ID,
    val payeeName: String = DEFAULT_PAYEE_NAME,
    val paymentInstructions: String = DEFAULT_PAYMENT_INSTRUCTIONS,
    val userQrPath: String? = null,
    val defaultCountryCode: String = "91",
    val reminderTemplate: String = DEFAULT_REMINDER_TEMPLATE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val dynamicColor: Boolean = false,
    val lockOnBackground: Boolean = true,
    val screenshotProtection: Boolean = false,
    val allowOverpayment: Boolean = false
) {
    val isUpiConfigured: Boolean get() = upiId.isNotBlank() && payeeName.isNotBlank()
}

/** Permanently configured organiser UPI account (can be edited in Settings → UPI & QR). */
const val DEFAULT_UPI_ID = "9730914525@ptyes"
const val DEFAULT_PAYEE_NAME = "Mr Vikas Devidas Shingne"
