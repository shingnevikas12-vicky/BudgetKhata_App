package com.budgetkhata.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("spentEpochDay")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventId: Long,
    val title: String,
    val amountPaise: Long,
    val spentEpochDay: Long,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val receiptUri: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
