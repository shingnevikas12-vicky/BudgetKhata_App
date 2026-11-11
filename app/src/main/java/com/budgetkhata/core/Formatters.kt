package com.budgetkhata.core

fun plural(count: Long, singular: String, pluralForm: String = "${singular}s"): String =
    if (count == 1L) "$count $singular" else "$count $pluralForm"

fun plural(count: Int, singular: String, pluralForm: String = "${singular}s"): String =
    plural(count.toLong(), singular, pluralForm)
