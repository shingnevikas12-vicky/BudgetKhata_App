package com.budgetkhata.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetkhata.data.local.entity.PaymentEntity
import com.budgetkhata.data.local.model.PaymentRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query(
        """
        SELECT
            p.id AS id,
            p.enrollmentId AS enrollmentId,
            p.amountPaise AS amountPaise,
            p.paidEpochDay AS paidEpochDay,
            p.method AS method,
            p.reference AS reference,
            p.notes AS notes,
            p.source AS source,
            p.confirmedAt AS confirmedAt,
            p.createdAt AS createdAt,
            e.eventId AS eventId,
            ev.name AS eventName,
            e.studentId AS studentId,
            s.name AS studentName,
            s.phone AS studentPhone
        FROM payments p
        INNER JOIN enrollments e ON e.id = p.enrollmentId
        INNER JOIN events ev ON ev.id = e.eventId
        INNER JOIN students s ON s.id = e.studentId
        WHERE (:eventId IS NULL OR e.eventId = :eventId)
          AND (:studentId IS NULL OR e.studentId = :studentId)
          AND (:enrollmentId IS NULL OR p.enrollmentId = :enrollmentId)
        ORDER BY p.paidEpochDay DESC, p.id DESC
        """
    )
    fun observePayments(
        eventId: Long?,
        studentId: Long?,
        enrollmentId: Long?
    ): Flow<List<PaymentRow>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM payments WHERE enrollmentId = :enrollmentId")
    suspend fun sumForEnrollment(enrollmentId: Long): Long

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPayment(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments ORDER BY id ASC")
    suspend fun getAll(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
