package com.budgetkhata.data.repo

import androidx.room.withTransaction
import com.budgetkhata.core.Money
import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.BudgetKhataDatabase
import com.budgetkhata.data.local.dao.EnrollmentDao
import com.budgetkhata.data.local.dao.PaymentDao
import com.budgetkhata.data.local.entity.PaymentEntity
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.data.local.entity.PaymentSource
import com.budgetkhata.data.local.model.PaymentRow
import kotlinx.coroutines.flow.Flow

class PaymentRepository(
    private val database: BudgetKhataDatabase,
    private val paymentDao: PaymentDao,
    private val enrollmentDao: EnrollmentDao
) {

    fun observePayments(
        eventId: Long? = null,
        studentId: Long? = null,
        enrollmentId: Long? = null
    ): Flow<List<PaymentRow>> = paymentDao.observePayments(eventId, studentId, enrollmentId)

    suspend fun remainingFor(enrollmentId: Long): Long {
        val enrollment = enrollmentDao.getEnrollment(enrollmentId) ?: return 0L
        val paid = paymentDao.sumForEnrollment(enrollmentId)
        return (enrollment.requiredPaise - paid).coerceAtLeast(0L)
    }

    /**
     * The single code path that writes money. It is only ever called after the administrator
     * confirms the payment was actually received.
     */
    suspend fun recordPayment(
        enrollmentId: Long,
        amountPaise: Long,
        paidEpochDay: Long,
        method: PaymentMethod,
        reference: String? = null,
        notes: String? = null,
        allowOverpayment: Boolean = false
    ): SaveResult = database.withTransaction {
        val enrollment = enrollmentDao.getEnrollment(enrollmentId)
            ?: return@withTransaction SaveResult.Error("This student is no longer part of the event")

        if (amountPaise <= 0L) {
            return@withTransaction SaveResult.Error("Enter an amount greater than zero")
        }

        val alreadyPaid = paymentDao.sumForEnrollment(enrollmentId)
        val remaining = (enrollment.requiredPaise - alreadyPaid).coerceAtLeast(0L)
        if (!allowOverpayment && amountPaise > remaining) {
            return@withTransaction SaveResult.Error(
                "Amount is more than the pending balance of ${Money.format(remaining)}"
            )
        }

        try {
            val id = paymentDao.insert(
                PaymentEntity(
                    enrollmentId = enrollmentId,
                    amountPaise = amountPaise,
                    paidEpochDay = paidEpochDay,
                    method = method,
                    reference = reference?.trim()?.takeIf { it.isNotEmpty() },
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                    source = PaymentSource.MANUAL
                )
            )
            SaveResult.Success(id)
        } catch (error: Exception) {
            SaveResult.Error(error.message ?: "Could not record the payment")
        }
    }

    suspend fun deletePayment(paymentId: Long): SaveResult = try {
        paymentDao.delete(paymentId)
        SaveResult.Success(paymentId)
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not delete the payment")
    }
}
