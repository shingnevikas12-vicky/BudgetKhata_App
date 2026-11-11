package com.budgetkhata.data.local.model

import com.budgetkhata.core.Money
import com.budgetkhata.core.PaymentStatus
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.entity.ExpenseCategory
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.data.local.entity.PaymentSource

/** One student's participation in one event, with confirmed collections rolled up. */
data class EnrollmentRow(
    val enrollmentId: Long,
    val studentId: Long,
    val eventId: Long,
    val requiredPaise: Long,
    val note: String?,
    val createdAt: Long,
    val studentName: String,
    val studentPhone: String,
    val studentAltPhone: String?,
    val studentNotes: String?,
    val studentArchived: Boolean,
    val eventName: String,
    val eventType: EventType,
    val eventArchived: Boolean,
    val eventStartEpochDay: Long?,
    val paidPaise: Long,
    val paymentCount: Long,
    val lastPaidEpochDay: Long?
) {
    val remainingPaise: Long get() = (requiredPaise - paidPaise).coerceAtLeast(0L)
    val overpaidPaise: Long get() = (paidPaise - requiredPaise).coerceAtLeast(0L)
    val status: PaymentStatus get() = PaymentStatus.of(requiredPaise, paidPaise)
    val progress: Float get() = Money.percent(paidPaise, requiredPaise) / 100f
}

data class DashboardStats(
    val studentCount: Long = 0,
    val eventCount: Long = 0,
    val expectedPaise: Long = 0,
    val collectedPaise: Long = 0,
    val paidCount: Long = 0,
    val partialCount: Long = 0,
    val pendingCount: Long = 0
) {
    val pendingPaise: Long get() = (expectedPaise - collectedPaise).coerceAtLeast(0L)
    val collectionPercent: Float get() = Money.percent(collectedPaise, expectedPaise)
    val enrollmentCount: Long get() = paidCount + partialCount + pendingCount
}

data class EventSummary(
    val eventId: Long,
    val eventName: String,
    val eventType: EventType,
    val eventArchived: Boolean,
    val eventStartEpochDay: Long?,
    val eventCreatedAt: Long,
    val studentCount: Long,
    val expectedPaise: Long,
    val collectedPaise: Long,
    val expensePaise: Long,
    val paidCount: Long
) {
    val pendingPaise: Long get() = (expectedPaise - collectedPaise).coerceAtLeast(0L)
    val netPaise: Long get() = collectedPaise - expensePaise
    val collectionPercent: Float get() = Money.percent(collectedPaise, expectedPaise)
    val pendingCount: Long get() = (studentCount - paidCount).coerceAtLeast(0L)
}

data class PaymentRow(
    val id: Long,
    val enrollmentId: Long,
    val amountPaise: Long,
    val paidEpochDay: Long,
    val method: PaymentMethod,
    val reference: String?,
    val notes: String?,
    val source: PaymentSource,
    val confirmedAt: Long,
    val createdAt: Long,
    val eventId: Long,
    val eventName: String,
    val studentId: Long,
    val studentName: String,
    val studentPhone: String
)

data class ExpenseRow(
    val id: Long,
    val eventId: Long,
    val eventName: String,
    val title: String,
    val amountPaise: Long,
    val spentEpochDay: Long,
    val category: ExpenseCategory,
    val receiptUri: String?,
    val notes: String?,
    val createdAt: Long
)

/** Student list grouping: one card per person, one line per event they joined. */
data class StudentGroup(
    val studentId: Long,
    val name: String,
    val phone: String,
    val isArchived: Boolean,
    val enrollments: List<EnrollmentRow>
) {
    val requiredPaise: Long get() = enrollments.sumOf { it.requiredPaise }
    val paidPaise: Long get() = enrollments.sumOf { it.paidPaise }
    val remainingPaise: Long get() = (requiredPaise - paidPaise).coerceAtLeast(0L)
    val status: PaymentStatus get() = PaymentStatus.of(requiredPaise, paidPaise)
    val eventCount: Int get() = enrollments.size
    val worstStatus: PaymentStatus get() = when {
        enrollments.isEmpty() -> PaymentStatus.PENDING
        enrollments.any { it.status == PaymentStatus.PENDING } -> PaymentStatus.PENDING
        enrollments.any { it.status == PaymentStatus.PARTIAL } -> PaymentStatus.PARTIAL
        else -> PaymentStatus.PAID
    }
}
