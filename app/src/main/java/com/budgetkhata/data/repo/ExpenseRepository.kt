package com.budgetkhata.data.repo

import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.dao.ExpenseDao
import com.budgetkhata.data.local.entity.ExpenseEntity
import com.budgetkhata.data.local.model.ExpenseRow
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {

    fun observeExpenses(eventId: Long? = null): Flow<List<ExpenseRow>> =
        expenseDao.observeExpenses(eventId)

    suspend fun getExpense(id: Long): ExpenseEntity? = expenseDao.getExpense(id)

    suspend fun sumForEvent(eventId: Long): Long = expenseDao.sumForEvent(eventId)

    suspend fun save(expense: ExpenseEntity): SaveResult = try {
        if (expense.id == 0L) {
            SaveResult.Success(expenseDao.insert(expense))
        } else {
            expenseDao.update(expense)
            SaveResult.Success(expense.id)
        }
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not save the expense")
    }

    suspend fun delete(id: Long): SaveResult = try {
        expenseDao.delete(id)
        SaveResult.Success(id)
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not delete the expense")
    }
}
