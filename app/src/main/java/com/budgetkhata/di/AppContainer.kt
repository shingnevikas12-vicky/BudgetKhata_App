package com.budgetkhata.di

import android.content.Context
import com.budgetkhata.data.backup.BackupRepository
import com.budgetkhata.data.local.BudgetKhataDatabase
import com.budgetkhata.data.prefs.SettingsRepository
import com.budgetkhata.data.repo.EnrollmentRepository
import com.budgetkhata.data.repo.EventRepository
import com.budgetkhata.data.repo.ExpenseRepository
import com.budgetkhata.data.repo.PaymentRepository
import com.budgetkhata.data.repo.StudentRepository
import com.budgetkhata.domain.PdfReportWriter
import com.budgetkhata.integrations.QrCodeGenerator
import com.budgetkhata.integrations.ShareUtils
import com.budgetkhata.integrations.UpiLauncher
import com.budgetkhata.integrations.WhatsAppLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: BudgetKhataDatabase = BudgetKhataDatabase.build(appContext)

    private val studentDao = database.studentDao()
    private val eventDao = database.eventDao()
    private val enrollmentDao = database.enrollmentDao()
    private val paymentDao = database.paymentDao()
    private val expenseDao = database.expenseDao()

    val studentRepository = StudentRepository(studentDao, enrollmentDao)
    val eventRepository = EventRepository(eventDao, database.statsDao())
    val enrollmentRepository = EnrollmentRepository(enrollmentDao)
    val paymentRepository = PaymentRepository(database, paymentDao, enrollmentDao)
    val expenseRepository = ExpenseRepository(expenseDao)

    val settingsRepository = SettingsRepository(appContext)

    val backupRepository = BackupRepository(database)

    val shareUtils = ShareUtils(appContext)
    val whatsAppLauncher = WhatsAppLauncher(appContext)
    val upiLauncher = UpiLauncher(appContext)
    val pdfReportWriter = PdfReportWriter(appContext)

    val qrCodeGenerator = QrCodeGenerator
}
