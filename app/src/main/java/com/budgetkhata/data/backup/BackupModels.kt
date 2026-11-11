package com.budgetkhata.data.backup

import com.budgetkhata.data.local.entity.EnrollmentEntity
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.ExpenseEntity
import com.budgetkhata.data.local.entity.PaymentEntity
import com.budgetkhata.data.local.entity.StudentEntity

data class BackupSettings(
    val upiId: String? = null,
    val payeeName: String? = null,
    val paymentInstructions: String? = null,
    val defaultCountryCode: String? = null,
    val reminderTemplate: String? = null,
    val themeMode: String? = null,
    val dynamicColor: Boolean? = null,
    val lockOnBackground: Boolean? = null,
    val screenshotProtection: Boolean? = null,
    val allowOverpayment: Boolean? = null
)

data class BackupPayload(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAtMillis: Long = 0L,
    val students: List<StudentEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val enrollments: List<EnrollmentEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val settings: BackupSettings? = null
) {
    companion object {
        const val FORMAT = "budget-khata-backup"
        const val VERSION = 1
    }
}

data class BackupCounts(
    val students: Int = 0,
    val events: Int = 0,
    val enrollments: Int = 0,
    val payments: Int = 0,
    val expenses: Int = 0
) {
    val total: Int get() = students + events + enrollments + payments + expenses
}

sealed interface BackupImportResult {
    data class Success(val counts: BackupCounts, val settings: BackupSettings?) : BackupImportResult
    data class Error(val message: String) : BackupImportResult
}
