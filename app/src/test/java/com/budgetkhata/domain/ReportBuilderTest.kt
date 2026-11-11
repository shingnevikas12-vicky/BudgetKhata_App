package com.budgetkhata.domain

import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.entity.ExpenseCategory
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.data.local.model.ExpenseRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportBuilderTest {

    private fun enrollment(
        id: Long,
        name: String,
        required: Long,
        paid: Long,
        event: String = "Jatra"
    ) = EnrollmentRow(
        enrollmentId = id,
        studentId = id,
        eventId = 1L,
        requiredPaise = required,
        note = null,
        createdAt = 0L,
        studentName = name,
        studentPhone = "919876543210",
        studentAltPhone = null,
        studentNotes = null,
        studentArchived = false,
        eventName = event,
        eventType = EventType.JATRA,
        eventArchived = false,
        eventStartEpochDay = 20_000L,
        paidPaise = paid,
        paymentCount = if (paid > 0L) 1L else 0L,
        lastPaidEpochDay = if (paid > 0L) 20_000L else null
    )

    private val rows = listOf(
        enrollment(1, "Rahul", 50_000L, 50_000L),
        enrollment(2, "Sneha", 50_000L, 20_000L),
        enrollment(3, "Amit", 50_000L, 0L)
    )

    @Test
    fun `paid report only contains fully paid students`() {
        val table = ReportBuilder.build(ReportType.PAID, rows, emptyList(), emptyList())
        assertEquals(1, table.rows.size)
        assertEquals("Rahul", table.rows.first().first())
        assertTrue(table.totals.contains("Collected" to "\u20B9500"))
    }

    @Test
    fun `partial report lists students with a balance`() {
        val table = ReportBuilder.build(ReportType.PARTIAL, rows, emptyList(), emptyList())
        assertEquals(1, table.rows.size)
        assertEquals("Sneha", table.rows.first().first())
        assertTrue(table.rows.first().contains("\u20B9300"))
    }

    @Test
    fun `pending report totals the outstanding amount`() {
        val table = ReportBuilder.build(ReportType.PENDING, rows, emptyList(), emptyList())
        assertEquals(1, table.rows.size)
        assertEquals("Amit", table.rows.first().first())
        assertTrue(table.totals.contains("Pending" to "\u20B9500"))
    }

    @Test
    fun `event summary computes net balance`() {
        val summary = EventSummary(
            eventId = 1L,
            eventName = "Jatra",
            eventType = EventType.JATRA,
            eventArchived = false,
            eventStartEpochDay = 20_000L,
            eventCreatedAt = 0L,
            studentCount = 3L,
            expectedPaise = 150_000L,
            collectedPaise = 70_000L,
            expensePaise = 25_000L,
            paidCount = 1L
        )
        val table = ReportBuilder.build(ReportType.SUMMARY, rows, emptyList(), listOf(summary))
        assertEquals(1, table.rows.size)
        val row = table.rows.first()
        assertEquals("Jatra", row[0])
        assertEquals("\u20B9700", row[4])
        assertEquals("\u20B9450", row[7])

        val netTotal = table.totals.first { it.first == "Net balance" }.second
        assertEquals("\u20B9450", netTotal)
    }

    @Test
    fun `expense report totals by category rows`() {
        val expense = ExpenseRow(
            id = 1L,
            eventId = 1L,
            eventName = "Jatra",
            title = "Bus booking",
            amountPaise = 120_000L,
            spentEpochDay = 20_000L,
            category = ExpenseCategory.TRANSPORT,
            receiptUri = null,
            notes = "Advance",
            createdAt = 0L
        )
        val table = ReportBuilder.build(ReportType.EXPENSES, emptyList(), listOf(expense), emptyList())
        assertEquals(1, table.rows.size)
        assertTrue(table.rows.first().contains("Transport"))
        assertTrue(table.totals.contains("Total expenses" to "\u20B91,200"))
    }

    @Test
    fun `empty reports keep their hint`() {
        val table = ReportBuilder.build(ReportType.PENDING, emptyList(), emptyList(), emptyList())
        assertTrue(table.rows.isEmpty())
        assertEquals("No students match this report", table.emptyMessage)
    }
}
