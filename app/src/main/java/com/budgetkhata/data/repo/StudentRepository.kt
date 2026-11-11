package com.budgetkhata.data.repo

import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.dao.EnrollmentDao
import com.budgetkhata.data.local.dao.StudentDao
import com.budgetkhata.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

class StudentRepository(
    private val studentDao: StudentDao,
    private val enrollmentDao: EnrollmentDao
) {

    fun observeStudents(query: String, includeArchived: Boolean = false): Flow<List<StudentEntity>> =
        studentDao.observeStudents(query.trim(), includeArchived)

    fun observeStudent(id: Long): Flow<StudentEntity?> = studentDao.observeStudent(id)

    suspend fun getStudent(id: Long): StudentEntity? = studentDao.getStudent(id)

    suspend fun findByPhone(phone: String): StudentEntity? = studentDao.findByPhone(phone)

    suspend fun enrollmentCount(studentId: Long): Long = studentDao.enrollmentCount(studentId)

    suspend fun save(student: StudentEntity): SaveResult = try {
        if (student.id == 0L) {
            SaveResult.Success(studentDao.insert(student))
        } else {
            studentDao.update(student)
            SaveResult.Success(student.id)
        }
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not save the student")
    }

    suspend fun setArchived(student: StudentEntity, archived: Boolean): SaveResult =
        save(student.copy(isArchived = archived))

    suspend fun delete(id: Long): SaveResult = try {
        studentDao.delete(id)
        SaveResult.Success(id)
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not delete the student")
    }
}
