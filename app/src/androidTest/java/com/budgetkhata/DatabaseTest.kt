package com.budgetkhata

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.budgetkhata.core.PaymentStatus
import com.budgetkhata.data.local.BudgetKhataDatabase
import com.budgetkhata.data.local.entity.EnrollmentEntity
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.entity.PaymentEntity
import com.budgetkhata.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: BudgetKhataDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BudgetKhataDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dashboardTotalsAndStatusAreDerivedFromPayments() = runBlocking {
        val studentId = database.studentDao().insert(
            StudentEntity(name = "Rahul Patil", phone = "919876543210")
        )
        val eventId = database.eventDao().insert(
            EventEntity(name = "College Jatra", type = EventType.JATRA)
        )
        val enrollmentId = database.enrollmentDao().insert(
            EnrollmentEntity(studentId = studentId, eventId = eventId, requiredPaise = 50_000L)
        )

        database.paymentDao().insert(
            PaymentEntity(
                enrollmentId = enrollmentId,
                amountPaise = 20_000L,
                paidEpochDay = 20_000L
            )
        )

        val stats = database.statsDao().observeDashboard().first()
        assertEquals(1L, stats.studentCount)
        assertEquals(50_000L, stats.expectedPaise)
        assertEquals(20_000L, stats.collectedPaise)
        assertEquals(30_000L, stats.pendingPaise)
        assertEquals(0L, stats.paidCount)
        assertEquals(1L, stats.partialCount)

        val row = database.enrollmentDao().getRow(enrollmentId)!!
        assertEquals(PaymentStatus.PARTIAL, row.status)

        database.paymentDao().insert(
            PaymentEntity(
                enrollmentId = enrollmentId,
                amountPaise = 30_000L,
                paidEpochDay = 20_001L
            )
        )

        val paidStats = database.statsDao().observeDashboard().first()
        assertEquals(50_000L, paidStats.collectedPaise)
        assertEquals(0L, paidStats.pendingPaise)
        assertEquals(1L, paidStats.paidCount)

        val paidRow = database.enrollmentDao().getRow(enrollmentId)!!
        assertEquals(PaymentStatus.PAID, paidRow.status)
    }

    @Test
    fun deletingStudentCascadesEnrollmentsAndPayments() = runBlocking {
        val studentId = database.studentDao().insert(
            StudentEntity(name = "Sneha", phone = "919876543211")
        )
        val eventId = database.eventDao().insert(
            EventEntity(name = "NSS Camp", type = EventType.NSS)
        )
        val enrollmentId = database.enrollmentDao().insert(
            EnrollmentEntity(studentId = studentId, eventId = eventId, requiredPaise = 20_000L)
        )
        database.paymentDao().insert(
            PaymentEntity(enrollmentId = enrollmentId, amountPaise = 5_000L, paidEpochDay = 20_000L)
        )

        database.studentDao().delete(studentId)

        assertTrue(database.enrollmentDao().getAll().isEmpty())
        assertTrue(database.paymentDao().getAll().isEmpty())
        assertEquals(1, database.eventDao().getAll().size)
    }

    @Test
    fun eventSummaryCombinesCollectionsAndExpenses() = runBlocking {
        val studentId = database.studentDao().insert(
            StudentEntity(name = "Amit", phone = "919876543212")
        )
        val eventId = database.eventDao().insert(
            EventEntity(name = "Festival", type = EventType.FESTIVAL)
        )
        val enrollmentId = database.enrollmentDao().insert(
            EnrollmentEntity(studentId = studentId, eventId = eventId, requiredPaise = 40_000L)
        )
        database.paymentDao().insert(
            PaymentEntity(enrollmentId = enrollmentId, amountPaise = 40_000L, paidEpochDay = 20_000L)
        )
        database.expenseDao().insert(
            com.budgetkhata.data.local.entity.ExpenseEntity(
                eventId = eventId,
                title = "Lighting",
                amountPaise = 12_000L,
                spentEpochDay = 20_001L
            )
        )

        val summary = database.statsDao().observeEventSummary(eventId).first()!!
        assertEquals(40_000L, summary.collectedPaise)
        assertEquals(12_000L, summary.expensePaise)
        assertEquals(28_000L, summary.netPaise)
        assertEquals(0L, summary.pendingPaise)
    }
}
