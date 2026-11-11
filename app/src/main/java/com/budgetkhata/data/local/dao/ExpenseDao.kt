package com.budgetkhata.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetkhata.data.local.entity.ExpenseEntity
import com.budgetkhata.data.local.model.ExpenseRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query(
        """
        SELECT
            x.id AS id,
            x.eventId AS eventId,
            ev.name AS eventName,
            x.title AS title,
            x.amountPaise AS amountPaise,
            x.spentEpochDay AS spentEpochDay,
            x.category AS category,
            x.receiptUri AS receiptUri,
            x.notes AS notes,
            x.createdAt AS createdAt
        FROM expenses x
        INNER JOIN events ev ON ev.id = x.eventId
        WHERE (:eventId IS NULL OR x.eventId = :eventId)
        ORDER BY x.spentEpochDay DESC, x.id DESC
        """
    )
    fun observeExpenses(eventId: Long?): Flow<List<ExpenseRow>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM expenses WHERE eventId = :eventId")
    suspend fun sumForEvent(eventId: Long): Long

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
