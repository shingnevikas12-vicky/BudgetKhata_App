package com.budgetkhata.data.local.entity

import com.budgetkhata.R

enum class EventType(val label: String, val labelRes: Int) {
    JATRA("College Jatra", R.string.event_type_jatra),
    NSS("NSS Activity", R.string.event_type_nss),
    FESTIVAL("College Festival", R.string.event_type_festival),
    TOUR("Educational Tour", R.string.event_type_tour),
    PARTY("Class Party", R.string.event_type_party),
    OTHER("Other Activity", R.string.event_type_other);

    companion object {
        fun fromName(value: String?): EventType =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class PaymentMethod(val label: String, val labelRes: Int) {
    CASH("Cash", R.string.payment_method_cash),
    UPI("UPI", R.string.payment_method_upi),
    BANK("Bank Transfer", R.string.payment_method_bank),
    OTHER("Other", R.string.payment_method_other);

    companion object {
        fun fromName(value: String?): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class PaymentSource {
    /** Confirmed by the administrator inside the app. */
    MANUAL,

    /** Reserved for a future server-verified gateway integration. */
    GATEWAY
}

enum class ExpenseCategory(val label: String, val labelRes: Int) {
    FOOD("Food & Refreshments", R.string.expense_cat_food),
    TRANSPORT("Transport", R.string.expense_cat_transport),
    VENUE("Venue & Booking", R.string.expense_cat_venue),
    PRINTING("Printing & Materials", R.string.expense_cat_printing),
    OTHER("Other", R.string.expense_cat_other);

    companion object {
        fun fromName(value: String?): ExpenseCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
