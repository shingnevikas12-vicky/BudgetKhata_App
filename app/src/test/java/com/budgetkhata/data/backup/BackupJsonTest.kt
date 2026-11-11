package com.budgetkhata.data.backup

import com.budgetkhata.data.local.entity.EnrollmentEntity
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.entity.ExpenseCategory
import com.budgetkhata.data.local.entity.ExpenseEntity
import com.budgetkhata.data.local.entity.PaymentEntity
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.data.local.entity.PaymentSource
import com.budgetkhata.data.local.entity.StudentEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val payload = BackupPayload(
        exportedAtMillis = 1_700_000_000_000L,
        students = listOf(StudentEntity(id = 1L, name = "Rahul", phone = "919876543210")),
        events = listOf(EventEntity(id = 2L, name = "Jatra", type = EventType.JATRA)),
        enrollments = listOf(EnrollmentEntity(id = 3L, studentId = 1L, eventId = 2L, requiredPaise = 50_000L)),
        payments = listOf(
            PaymentEntity(
                id = 4L,
                enrollmentId = 3L,
                amountPaise = 20_000L,
                paidEpochDay = 20_000L,
                method = PaymentMethod.UPI,
                source = PaymentSource.MANUAL
            )
        ),
        expenses = listOf(
            ExpenseEntity(
                id = 5L,
                eventId = 2L,
                title = "Bus",
                amountPaise = 12_000L,
                spentEpochDay = 20_001L,
                category = ExpenseCategory.TRANSPORT
            )
        ),
        settings = BackupSettings(
            upiId = "organiser@okhdfcbank",
            payeeName = "Organiser",
            themeMode = "DARK",
            allowOverpayment = true
        )
    )

    @Test
    fun `payload survives a json round trip`() {
        val json = gson.toJson(payload)
        val restored = gson.fromJson(json, BackupPayload::class.java)

        assertNotNull(restored)
        assertEquals(BackupPayload.FORMAT, restored.format)
        assertEquals(BackupPayload.VERSION, restored.version)
        assertEquals(1, restored.students.size)
        assertEquals("Rahul", restored.students.first().name)
        assertEquals(EventType.JATRA, restored.events.first().type)
        assertEquals(50_000L, restored.enrollments.first().requiredPaise)
        assertEquals(PaymentMethod.UPI, restored.payments.first().method)
        assertEquals(PaymentSource.MANUAL, restored.payments.first().source)
        assertEquals(ExpenseCategory.TRANSPORT, restored.expenses.first().category)
        assertEquals("DARK", restored.settings?.themeMode)
        assertTrue(restored.settings?.allowOverpayment == true)
    }

    @Test
    fun `json keeps the format marker so other files are rejected`() {
        val json = gson.toJson(payload)
        assertTrue(json.contains(BackupPayload.FORMAT))
        assertFalse("""{"hello":"world"}""".contains(BackupPayload.FORMAT))
    }
}
