package com.budgetkhata.data.local.model

import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.data.local.entity.PaymentSource
import com.budgetkhata.core.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    private fun row(required: Long, paid: Long, studentId: Long = 1L) = EnrollmentRow(
        enrollmentId = studentId,
        studentId = studentId,
        eventId = 1L,
        requiredPaise = required,
        note = null,
        createdAt = 0L,
        studentName = "Student $studentId",
        studentPhone = "919876543210",
        studentAltPhone = null,
        studentNotes = null,
        studentArchived = false,
        eventName = "Jatra",
        eventType = EventType.JATRA,
        eventArchived = false,
        eventStartEpochDay = 20_000L,
        paidPaise = paid,
        paymentCount = 1L,
        lastPaidEpochDay = 20_000L
    )

    @Test
    fun `enrollment derives remaining and status`() {
        val partial = row(50_000L, 20_000L)
        assertEquals(30_000L, partial.remainingPaise)
        assertEquals(0L, partial.overpaidPaise)
        assertEquals(PaymentStatus.PARTIAL, partial.status)
        assertEquals(0.4f, partial.progress, 0.001f)

        val overpaid = row(50_000L, 60_000L)
        assertEquals(0L, overpaid.remainingPaise)
        assertEquals(10_000L, overpaid.overpaidPaise)
        assertEquals(PaymentStatus.PAID, overpaid.status)
    }

    @Test
    fun `student group aggregates events`() {
        val group = StudentGroup(
            studentId = 1L,
            name = "Rahul",
            phone = "919876543210",
            isArchived = false,
            enrollments = listOf(row(50_000L, 50_000L), row(20_000L, 0L))
        )
        assertEquals(70_000L, group.requiredPaise)
        assertEquals(50_000L, group.paidPaise)
        assertEquals(20_000L, group.remainingPaise)
        assertEquals(2, group.eventCount)
        assertEquals(PaymentStatus.PENDING, group.worstStatus)
    }

    @Test
    fun `event summary pending and net`() {
        val summary = EventSummary(
            eventId = 1L,
            eventName = "Jatra",
            eventType = EventType.JATRA,
            eventArchived = false,
            eventStartEpochDay = null,
            eventCreatedAt = 0L,
            studentCount = 2L,
            expectedPaise = 100_000L,
            collectedPaise = 60_000L,
            expensePaise = 15_000L,
            paidCount = 1L
        )
        assertEquals(40_000L, summary.pendingPaise)
        assertEquals(45_000L, summary.netPaise)
        assertEquals(1L, summary.pendingCount)
        assertEquals(60f, summary.collectionPercent, 0.01f)
    }

    @Test
    fun `dashboard totals derive pending and percent`() {
        val stats = DashboardStats(
            studentCount = 2L,
            eventCount = 1L,
            expectedPaise = 100_000L,
            collectedPaise = 45_000L,
            paidCount = 1L,
            partialCount = 1L,
            pendingCount = 0L
        )
        assertEquals(55_000L, stats.pendingPaise)
        assertEquals(45f, stats.collectionPercent, 0.01f)
        assertEquals(2L, stats.enrollmentCount)
    }

    @Test
    fun `payment row keeps method and source`() {
        val payment = PaymentRow(
            id = 1L,
            enrollmentId = 1L,
            amountPaise = 20_000L,
            paidEpochDay = 20_000L,
            method = PaymentMethod.UPI,
            reference = "TXN123",
            notes = null,
            source = PaymentSource.MANUAL,
            confirmedAt = 0L,
            createdAt = 0L,
            eventId = 1L,
            eventName = "Jatra",
            studentId = 1L,
            studentName = "Rahul",
            studentPhone = "919876543210"
        )
        assertEquals(PaymentMethod.UPI, payment.method)
        assertEquals(PaymentSource.MANUAL, payment.source)
    }
}
