package com.budgetkhata.core

import com.budgetkhata.R

/**
 * Payment state is always derived from the confirmed transactions, never stored,
 * so it can never drift out of sync with the ledger.
 */
enum class PaymentStatus(val label: String, val labelRes: Int) {
    PAID("Paid", R.string.paid),
    PARTIAL("Partial", R.string.partial),
    PENDING("Pending", R.string.pending);

    companion object {
        fun of(requiredPaise: Long, paidPaise: Long): PaymentStatus = when {
            paidPaise >= requiredPaise -> PAID
            paidPaise > 0L -> PARTIAL
            else -> PENDING
        }
    }
}
