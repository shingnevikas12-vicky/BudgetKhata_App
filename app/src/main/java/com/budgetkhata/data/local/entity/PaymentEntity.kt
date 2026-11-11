package com.budgetkhata.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A confirmed payment. Rows are only written after the administrator confirms the money
 * was received — never because a WhatsApp message was sent or a UPI link was opened.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = EnrollmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["enrollmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("enrollmentId"), Index("paidEpochDay")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val enrollmentId: Long,
    val amountPaise: Long,
    val paidEpochDay: Long,
    val method: PaymentMethod = PaymentMethod.CASH,
    val reference: String? = null,
    val notes: String? = null,
    val source: PaymentSource = PaymentSource.MANUAL,
    val confirmedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
