package com.budgetkhata.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.budgetkhata.data.local.dao.EnrollmentDao
import com.budgetkhata.data.local.dao.EventDao
import com.budgetkhata.data.local.dao.ExpenseDao
import com.budgetkhata.data.local.dao.PaymentDao
import com.budgetkhata.data.local.dao.StatsDao
import com.budgetkhata.data.local.dao.StudentDao
import com.budgetkhata.data.local.entity.EnrollmentEntity
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.ExpenseEntity
import com.budgetkhata.data.local.entity.PaymentEntity
import com.budgetkhata.data.local.entity.StudentEntity

@Database(
    entities = [
        StudentEntity::class,
        EventEntity::class,
        EnrollmentEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BudgetKhataDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun eventDao(): EventDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun statsDao(): StatsDao

    companion object {
        const val NAME = "budget_khata.db"

        fun build(context: Context): BudgetKhataDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BudgetKhataDatabase::class.java,
                NAME
            ).build()
    }
}
