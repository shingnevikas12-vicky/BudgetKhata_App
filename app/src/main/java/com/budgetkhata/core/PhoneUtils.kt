package com.budgetkhata.core

object PhoneUtils {

    fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }

    /**
     * Normalises a stored/typed number to international digits (no plus sign).
     * Returns null when the number cannot be a valid contact number.
     */
    fun normalize(raw: String?, defaultCountryCode: String = "91"): String? {
        if (raw.isNullOrBlank()) return null
        var digits = digitsOnly(raw)
        if (digits.isEmpty()) return null

        val country = defaultCountryCode.filter { it.isDigit() }.ifEmpty { "91" }
        if (digits.startsWith("00")) digits = digits.drop(2)

        digits = when {
            digits.startsWith(country) && digits.length >= country.length + 8 -> digits
            digits.length == 10 -> country + digits
            digits.length == 11 && digits.startsWith("0") -> country + digits.drop(1)
            else -> digits
        }

        return if (digits.length in 8..15) digits else null
    }

    /** Display form: +91 98765 43210 */
    fun display(raw: String?): String {
        if (raw.isNullOrBlank()) return "\u2014"
        val digits = digitsOnly(raw)
        if (digits.length < 10) return "+$digits"
        val last = digits.takeLast(10)
        val prefix = digits.dropLast(10)
        val grouped = last.substring(0, 5) + " " + last.substring(5)
        return if (prefix.isEmpty()) "+$last" else "+$prefix $grouped"
    }

    fun isValid(raw: String?, defaultCountryCode: String = "91"): Boolean =
        normalize(raw, defaultCountryCode) != null
}
