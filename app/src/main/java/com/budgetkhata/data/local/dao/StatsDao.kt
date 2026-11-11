package com.budgetkhata.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.budgetkhata.data.local.model.DashboardStats
import com.budgetkhata.data.local.model.EventSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM students WHERE isArchived = 0) AS studentCount,
            (SELECT COUNT(*) FROM events WHERE isArchived = 0) AS eventCount,
            (SELECT COALESCE(SUM(e.requiredPaise), 0) FROM enrollments e
                INNER JOIN students s ON s.id = e.studentId
                INNER JOIN events ev ON ev.id = e.eventId
                WHERE s.isArchived = 0 AND ev.isArchived = 0) AS expectedPaise,
            (SELECT COALESCE(SUM(p.amountPaise), 0) FROM payments p
                INNER JOIN enrollments e ON e.id = p.enrollmentId
                INNER JOIN students s ON s.id = e.studentId
                INNER JOIN events ev ON ev.id = e.eventId
                WHERE s.isArchived = 0 AND ev.isArchived = 0) AS collectedPaise,
            (SELECT COUNT(*) FROM enrollments e
                INNER JOIN students s ON s.id = e.studentId
                INNER JOIN events ev ON ev.id = e.eventId
                WHERE s.isArchived = 0 AND ev.isArchived = 0
                  AND COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) >= e.requiredPaise) AS paidCount,
            (SELECT COUNT(*) FROM enrollments e
                INNER JOIN students s ON s.id = e.studentId
                INNER JOIN events ev ON ev.id = e.eventId
                WHERE s.isArchived = 0 AND ev.isArchived = 0
                  AND COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) > 0
                  AND COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) < e.requiredPaise) AS partialCount,
            (SELECT COUNT(*) FROM enrollments e
                INNER JOIN students s ON s.id = e.studentId
                INNER JOIN events ev ON ev.id = e.eventId
                WHERE s.isArchived = 0 AND ev.isArchived = 0
                  AND COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) = 0) AS pendingCount
        """
    )
    fun observeDashboard(): Flow<DashboardStats>

    @Query(
        """
        SELECT
            ev.id AS eventId,
            ev.name AS eventName,
            ev.type AS eventType,
            ev.isArchived AS eventArchived,
            ev.startEpochDay AS eventStartEpochDay,
            ev.createdAt AS eventCreatedAt,
            (SELECT COUNT(*) FROM enrollments e WHERE e.eventId = ev.id) AS studentCount,
            (SELECT COALESCE(SUM(e.requiredPaise), 0) FROM enrollments e WHERE e.eventId = ev.id) AS expectedPaise,
            (SELECT COALESCE(SUM(p.amountPaise), 0) FROM payments p
                INNER JOIN enrollments e ON e.id = p.enrollmentId
                WHERE e.eventId = ev.id) AS collectedPaise,
            (SELECT COALESCE(SUM(x.amountPaise), 0) FROM expenses x WHERE x.eventId = ev.id) AS expensePaise,
            (SELECT COUNT(*) FROM enrollments e
                WHERE e.eventId = ev.id
                  AND COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) >= e.requiredPaise) AS paidCount
        FROM events ev
        ORDER BY ev.isArchived ASC, ev.createdAt DESC
        """
    )
    fun observeEventSummaries(): Flow<List<EventSummary>>

    @Query(
        """
        SELECT
            ev.id AS eventId,
            ev.name AS eventName,
            ev.type AS eventType,
            ev.isArchived AS eventArchived,
            ev.startEpochDay AS eventStartEpochDay,
            ev.createdAt AS eventCreatedAt,
            (SELECT COUNT(*) FROM enrollments e WHERE e.eventId = ev.id) AS studentCount,
            (SELECT COALESCE(SUM(e.requiredPaise), 0) FROM enrollments e WHERE e.eventId = ev.id) AS expectedPaise,
            (SELECT COALESCE(SUM(p.amountPaise), 0) FROM payments p
                INNER JOIN enrollments e ON e.id = p.enrollmentId
                WHERE e.eventId = ev.id) AS collectedPaise,
            (SELECT COALESCE(SUM(x.amountPaise), 0) FROM expenses x WHERE x.eventId = ev.id) AS expensePaise,
            (SELECT COUNT(*) FROM enrollments e
                WHERE e.eventId = ev.id
                  AND COALESCE((SELECT SUM(p.amountPaise) FROM payments p WHERE p.enrollmentId = e.id), 0) >= e.requiredPaise) AS paidCount
        FROM events ev
        WHERE ev.id = :eventId
        """
    )
    fun observeEventSummary(eventId: Long): Flow<EventSummary?>
}
