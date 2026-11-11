package com.budgetkhata.data.backup

import androidx.room.withTransaction
import com.budgetkhata.data.local.BudgetKhataDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class BackupRepository(
    private val database: BudgetKhataDatabase,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {

    suspend fun exportPayload(): BackupPayload = BackupPayload(
        exportedAtMillis = System.currentTimeMillis(),
        students = database.studentDao().getAll(),
        events = database.eventDao().getAll(),
        enrollments = database.enrollmentDao().getAll(),
        payments = database.paymentDao().getAll(),
        expenses = database.expenseDao().getAll()
    )

    suspend fun exportJson(settings: BackupSettings?): String {
        val payload = exportPayload().copy(settings = settings)
        return gson.toJson(payload)
    }

    suspend fun importJson(json: String): BackupImportResult {
        val payload = try {
            gson.fromJson(json, BackupPayload::class.java)
        } catch (error: Exception) {
            null
        } ?: return BackupImportResult.Error("This file is not a valid Budget Khata backup")

        if (payload.format != BackupPayload.FORMAT) {
            return BackupImportResult.Error("This file was not created by Budget Khata")
        }
        if (payload.version > BackupPayload.VERSION) {
            return BackupImportResult.Error("This backup comes from a newer app version. Please update the app first.")
        }

        validationError(payload)?.let { return BackupImportResult.Error(it) }

        return try {
            database.withTransaction {
                val paymentDao = database.paymentDao()
                val expenseDao = database.expenseDao()
                val enrollmentDao = database.enrollmentDao()
                val studentDao = database.studentDao()
                val eventDao = database.eventDao()

                paymentDao.deleteAll()
                expenseDao.deleteAll()
                enrollmentDao.deleteAll()
                studentDao.deleteAll()
                eventDao.deleteAll()

                if (payload.students.isNotEmpty()) studentDao.insertAll(payload.students)
                if (payload.events.isNotEmpty()) eventDao.insertAll(payload.events)
                if (payload.enrollments.isNotEmpty()) enrollmentDao.insertAll(payload.enrollments)
                if (payload.payments.isNotEmpty()) paymentDao.insertAll(payload.payments)
                if (payload.expenses.isNotEmpty()) expenseDao.insertAll(payload.expenses)
            }

            BackupImportResult.Success(
                counts = countsOf(payload),
                settings = payload.settings
            )
        } catch (error: Exception) {
            BackupImportResult.Error("Restore failed: ${error.message ?: "unknown error"}")
        }
    }

    /** Validates a backup file and returns what it contains without writing anything. */
    fun inspect(json: String): BackupImportResult {
        val payload = try {
            gson.fromJson(json, BackupPayload::class.java)
        } catch (error: Exception) {
            null
        } ?: return BackupImportResult.Error("This file is not a valid Budget Khata backup")

        if (payload.format != BackupPayload.FORMAT) {
            return BackupImportResult.Error("This file was not created by Budget Khata")
        }
        if (payload.version > BackupPayload.VERSION) {
            return BackupImportResult.Error("This backup comes from a newer app version. Please update the app first.")
        }
        validationError(payload)?.let { return BackupImportResult.Error(it) }

        return BackupImportResult.Success(countsOf(payload), payload.settings)
    }

    private fun countsOf(payload: BackupPayload) = BackupCounts(
        students = payload.students.size,
        events = payload.events.size,
        enrollments = payload.enrollments.size,
        payments = payload.payments.size,
        expenses = payload.expenses.size
    )

    suspend fun deleteEverything() {
        database.withTransaction {
            database.paymentDao().deleteAll()
            database.expenseDao().deleteAll()
            database.enrollmentDao().deleteAll()
            database.studentDao().deleteAll()
            database.eventDao().deleteAll()
        }
    }

    private fun validationError(payload: BackupPayload): String? {
        if (payload.students.any { it.id <= 0L || it.name.isBlank() || it.phone.isBlank() }) {
            return "Backup contains invalid student records"
        }
        if (payload.events.any { it.id <= 0L || it.name.isBlank() }) {
            return "Backup contains invalid event records"
        }
        if (payload.students.map { it.id }.toSet().size != payload.students.size) {
            return "Backup contains duplicate students"
        }
        if (payload.events.map { it.id }.toSet().size != payload.events.size) {
            return "Backup contains duplicate events"
        }

        val studentIds = payload.students.map { it.id }.toSet()
        val eventIds = payload.events.map { it.id }.toSet()
        val enrollmentIds = payload.enrollments.map { it.id }.toSet()

        if (payload.enrollments.any { it.id <= 0L || it.studentId !in studentIds || it.eventId !in eventIds }) {
            return "Backup contains enrollments that reference missing students or events"
        }
        if (payload.enrollments.any { it.requiredPaise <= 0L }) {
            return "Backup contains invalid contribution amounts"
        }
        if (payload.payments.any { it.id <= 0L || it.enrollmentId !in enrollmentIds || it.amountPaise <= 0L }) {
            return "Backup contains invalid payment records"
        }
        if (payload.expenses.any { it.id <= 0L || it.eventId !in eventIds || it.amountPaise <= 0L }) {
            return "Backup contains invalid expense records"
        }
        return null
    }
}
