package com.budgetkhata.integrations

import com.budgetkhata.core.Money
import java.net.URLEncoder

object UpiLinkBuilder {

    private val upiRegex = Regex("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64}$")

    /**
     * Builds a UPI deep link. UPI apps treat this as a request to pay, never as proof
     * that money arrived — so this app always waits for the administrator to confirm.
     */
    fun build(
        upiId: String,
        payeeName: String,
        amountPaise: Long? = null,
        note: String? = null,
        transactionRef: String? = null
    ): String {
        val params = linkedMapOf(
            "pa" to upiId.trim(),
            "pn" to payeeName.trim(),
            "cu" to "INR"
        )
        if (amountPaise != null && amountPaise > 0L) {
            params["am"] = Money.upiAmount(amountPaise)
        }
        if (!note.isNullOrBlank()) {
            params["tn"] = note.trim().take(50)
        }
        if (!transactionRef.isNullOrBlank()) {
            params["tr"] = transactionRef.trim().take(35)
        }
        return "upi://pay?" + params.entries.joinToString("&") { entry ->
            "${entry.key}=${encode(entry.value)}"
        }
    }

    fun note(eventName: String): String = "Contribution - $eventName"

    fun isValidUpiId(upiId: String): Boolean = upiRegex.matches(upiId.trim())

    /** RFC 3986-ish encoding that works without the Android framework (testable on the JVM). */
    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
