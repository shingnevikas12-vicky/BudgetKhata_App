package com.budgetkhata.data.repo

import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.dao.EnrollmentDao
import com.budgetkhata.data.local.entity.EnrollmentEntity
import com.budgetkhata.data.local.model.EnrollmentRow
import kotlinx.coroutines.flow.Flow

class EnrollmentRepository(
    private val enrollmentDao: EnrollmentDao
) {

    fun observeRows(
        query: String = "",
        eventId: Long? = null,
        studentId: Long? = null,
        includeArchived: Boolean = false
    ): Flow<List<EnrollmentRow>> =
        enrollmentDao.observeRows(query.trim(), eventId, studentId, includeArchived)

    suspend fun getRow(enrollmentId: Long): EnrollmentRow? = enrollmentDao.getRow(enrollmentId)

    suspend fun find(studentId: Long, eventId: Long): EnrollmentEntity? =
        enrollmentDao.find(studentId, eventId)

    suspend fun countForEvent(eventId: Long): Long = enrollmentDao.countForEvent(eventId)

    suspend fun enroll(
        studentId: Long,
        eventId: Long,
        requiredPaise: Long,
        note: String? = null
    ): SaveResult {
        if (enrollmentDao.find(studentId, eventId) != null) {
            return SaveResult.Error("This student is already added to the event")
        }
        return try {
            SaveResult.Success(
                enrollmentDao.insert(
                    EnrollmentEntity(
                        studentId = studentId,
                        eventId = eventId,
                        requiredPaise = requiredPaise,
                        note = note?.trim()?.takeIf { it.isNotEmpty() }
                    )
                )
            )
        } catch (error: Exception) {
            SaveResult.Error(error.message ?: "Could not add the student to this event")
        }
    }

    suspend fun updateContribution(
        enrollmentId: Long,
        requiredPaise: Long,
        note: String? = null
    ): SaveResult {
        val existing = enrollmentDao.getEnrollment(enrollmentId)
            ?: return SaveResult.Error("Enrollment not found")
        return try {
            enrollmentDao.update(
                existing.copy(
                    requiredPaise = requiredPaise,
                    note = note?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
            SaveResult.Success(enrollmentId)
        } catch (error: Exception) {
            SaveResult.Error(error.message ?: "Could not update the contribution")
        }
    }

    suspend fun remove(enrollmentId: Long): SaveResult = try {
        enrollmentDao.delete(enrollmentId)
        SaveResult.Success(enrollmentId)
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not remove the student from this event")
    }
}
