package com.budgetkhata.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetkhata.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY isArchived ASC, createdAt DESC")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActiveEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeEvent(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEvent(id: Long): EventEntity?

    @Query("SELECT * FROM events ORDER BY id ASC")
    suspend fun getAll(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM enrollments WHERE eventId = :eventId")
    suspend fun enrollmentCount(eventId: Long): Long
}
