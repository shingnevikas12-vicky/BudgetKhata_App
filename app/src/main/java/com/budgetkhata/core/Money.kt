package com.budgetkhata.core

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * All money is stored as [Long] paise (1 rupee = 100 paise) so no rounding drift
 * can ever occur in collections, expenses or balances.
 */
object Money {

    const val SYMBOL = "\u20B9"

    private val amountRegex = Regex("^\\d{1,9}(\\.\\d{1,2})?$")

    /** Accepts "500", "500.5", "1,500", "₹1,500.50", "300/-". Returns null when invalid. */
    fun parseToPaise(input: String): Long? {
        if (input.isBlank()) return null
        val cleaned = input.trim()
            .replace(SYMBOL, "")
            .replace(",", "")
            .replace(" ", "")
            .removeSuffix("/-")
        if (!amountRegex.matches(cleaned)) return null
        return try {
            BigDecimal(cleaned)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Formats with Indian digit grouping, e.g. ₹1,23,456.50. Whole amounts drop the decimals. */
    fun format(paise: Long, withSymbol: Boolean = true, forceDecimals: Boolean = false): String {
        val negative = paise < 0
        val absolute = abs(paise)
        val rupees = absolute / 100
        val fraction = (absolute % 100).toInt()
        val decimals = if (fraction == 0 && !forceDecimals) "" else "." + fraction.toString().padStart(2, '0')
        val prefix = (if (negative) "-" else "") + (if (withSymbol) SYMBOL else "")
        return prefix + groupIndian(rupees) + decimals
    }

    /** Plain two-decimal value used in UPI links (e.g. 300.00). */
    fun upiAmount(paise: Long): String {
        val absolute = abs(paise)
        return "${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }

    fun percent(partPaise: Long, wholePaise: Long): Float {
        if (wholePaise <= 0L) return 0f
        return (partPaise.toFloat() / wholePaise.toFloat() * 100f).coerceIn(0f, 100f)
    }

    private fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits

        val lastThree = digits.takeLast(3)
        var head = digits.dropLast(3)
        val groups = ArrayList<String>()
        while (head.length > 2) {
            groups.add(0, head.takeLast(2))
            head = head.dropLast(2)
        }
        if (head.isNotEmpty()) groups.add(0, head)
        return groups.joinToString(",") + "," + lastThree
    }
}
