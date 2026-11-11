package com.budgetkhata.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetkhata.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query(
        """
        SELECT * FROM students
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
          AND (:includeArchived = 1 OR isArchived = 0)
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeStudents(query: String, includeArchived: Boolean): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeStudent(id: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudent(id: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): StudentEntity?

    @Query("SELECT * FROM students ORDER BY id ASC")
    suspend fun getAll(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>)

    @Update
    suspend fun update(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM students")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM enrollments WHERE studentId = :studentId")
    suspend fun enrollmentCount(studentId: Long): Long
}
