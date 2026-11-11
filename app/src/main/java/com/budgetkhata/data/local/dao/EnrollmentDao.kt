package com.budgetkhata.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetkhata.data.local.entity.EnrollmentEntity
import com.budgetkhata.data.local.model.EnrollmentRow
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrollmentDao {

    @Query(
        """
        SELECT
            e.id AS enrollmentId,
            e.studentId AS studentId,
            e.eventId AS eventId,
            e.requiredPaise AS requiredPaise,
            e.note AS note,
            e.createdAt AS createdAt,
            s.name AS studentName,
            s.phone AS studentPhone,
            s.altPhone AS studentAltPhone,
            s.notes AS studentNotes,
            s.isArchived AS studentArchived,
            ev.name AS eventName,
            ev.type AS eventType,
            ev.isArchived AS eventArchived,
            ev.startEpochDay AS eventStartEpochDay,
            COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) AS paidPaise,
            (SELECT COUNT(*) FROM payments p2 WHERE p2.enrollmentId = e.id) AS paymentCount,
            (SELECT MAX(p3.paidEpochDay) FROM payments p3 WHERE p3.enrollmentId = e.id) AS lastPaidEpochDay
        FROM enrollments e
        INNER JOIN students s ON s.id = e.studentId
        INNER JOIN events ev ON ev.id = e.eventId
        WHERE (:query = '' OR s.name LIKE '%' || :query || '%' OR s.phone LIKE '%' || :query || '%')
          AND (:eventId IS NULL OR e.eventId = :eventId)
          AND (:studentId IS NULL OR e.studentId = :studentId)
          AND (:includeArchived = 1 OR (s.isArchived = 0 AND ev.isArchived = 0))
        ORDER BY s.name COLLATE NOCASE ASC, ev.name COLLATE NOCASE ASC
        """
    )
    fun observeRows(
        query: String,
        eventId: Long?,
        studentId: Long?,
        includeArchived: Boolean
    ): Flow<List<EnrollmentRow>>

    @Query(
        """
        SELECT
            e.id AS enrollmentId,
            e.studentId AS studentId,
            e.eventId AS eventId,
            e.requiredPaise AS requiredPaise,
            e.note AS note,
            e.createdAt AS createdAt,
            s.name AS studentName,
            s.phone AS studentPhone,
            s.altPhone AS studentAltPhone,
            s.notes AS studentNotes,
            s.isArchived AS studentArchived,
            ev.name AS eventName,
            ev.type AS eventType,
            ev.isArchived AS eventArchived,
            ev.startEpochDay AS eventStartEpochDay,
            COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) AS paidPaise,
            (SELECT COUNT(*) FROM payments p2 WHERE p2.enrollmentId = e.id) AS paymentCount,
            (SELECT MAX(p3.paidEpochDay) FROM payments p3 WHERE p3.enrollmentId = e.id) AS lastPaidEpochDay
        FROM enrollments e
        INNER JOIN students s ON s.id = e.studentId
        INNER JOIN events ev ON ev.id = e.eventId
        WHERE e.id = :enrollmentId
        """
    )
    suspend fun getRow(enrollmentId: Long): EnrollmentRow?

    @Query("SELECT * FROM enrollments WHERE studentId = :studentId AND eventId = :eventId LIMIT 1")
    suspend fun find(studentId: Long, eventId: Long): EnrollmentEntity?

    @Query("SELECT * FROM enrollments WHERE id = :id")
    suspend fun getEnrollment(id: Long): EnrollmentEntity?

    @Query("SELECT * FROM enrollments ORDER BY id ASC")
    suspend fun getAll(): List<EnrollmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(enrollment: EnrollmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(enrollments: List<EnrollmentEntity>)

    @Update
    suspend fun update(enrollment: EnrollmentEntity)

    @Query("DELETE FROM enrollments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM enrollments")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM enrollments WHERE eventId = :eventId")
    suspend fun countForEvent(eventId: Long): Long
}
