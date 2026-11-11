package com.budgetkhata.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "enrollments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("studentId"),
        Index("eventId"),
        Index(value = ["studentId", "eventId"], unique = true)
    ]
)
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val studentId: Long,
    val eventId: Long,
    val requiredPaise: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
