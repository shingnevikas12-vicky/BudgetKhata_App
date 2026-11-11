package com.budgetkhata.core

import com.budgetkhata.R

object Validation {

    private val upiRegex = Regex("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64}$")

    fun personName(raw: String): Int? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> R.string.err_name_required
            value.length < 2 -> R.string.err_name_short
            value.length > 80 -> R.string.err_name_long
            else -> null
        }
    }

    fun phone(raw: String, countryCode: String): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return R.string.err_phone_required
        val digits = PhoneUtils.digitsOnly(value)
        if (digits.length < 8) return R.string.err_phone_invalid
        if (PhoneUtils.normalize(value, countryCode) == null) return R.string.err_phone_country_code
        return null
    }

    fun optionalPhone(raw: String, countryCode: String): Int? {
        if (raw.isBlank()) return null
        return phone(raw, countryCode) ?: null // Note: Caller handles distinction if needed
    }

    fun contribution(paise: Long?): Int? = when {
        paise == null -> R.string.err_contribution_required
        paise <= 0L -> R.string.err_contribution_greater_zero
        paise > 99_999_999_900L -> R.string.err_amount_large
        else -> null
    }

    fun paymentAmount(paise: Long?, remainingPaise: Long, allowOverpayment: Boolean): Int? = when {
        paise == null -> R.string.err_amount_required
        paise <= 0L -> R.string.err_amount_greater_zero
        !allowOverpayment && paise > remainingPaise -> R.string.err_amount_exceed_pending
        else -> null
    }

    fun expenseAmount(paise: Long?): Int? = when {
        paise == null -> R.string.err_expense_amount_required
        paise <= 0L -> R.string.err_amount_greater_zero
        else -> null
    }

    fun eventName(raw: String): Int? = when {
        raw.trim().isEmpty() -> R.string.err_event_name_required
        raw.trim().length > 80 -> R.string.err_event_name_long
        else -> null
    }

    fun expenseTitle(raw: String): Int? = when {
        raw.trim().isEmpty() -> R.string.err_desc_required
        raw.trim().length > 120 -> R.string.err_desc_long
        else -> null
    }

    fun upiId(raw: String): Int? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> R.string.err_upi_id_required
            !upiRegex.matches(value) -> R.string.err_upi_id_invalid
            else -> null
        }
    }

    fun upiIdOptional(raw: String): Int? = if (raw.isBlank()) null else upiId(raw)

    fun pin(raw: String): Int? = when {
        raw.length !in 4..6 -> R.string.err_pin_digits
        !raw.all { it.isDigit() } -> R.string.err_pin_only_digits
        else -> null
    }
}
