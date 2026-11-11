package com.budgetkhata.data.repo

import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.dao.EventDao
import com.budgetkhata.data.local.dao.StatsDao
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.model.DashboardStats
import com.budgetkhata.data.local.model.EventSummary
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val eventDao: EventDao,
    private val statsDao: StatsDao
) {

    fun observeEvents(): Flow<List<EventEntity>> = eventDao.observeEvents()

    fun observeActiveEvents(): Flow<List<EventEntity>> = eventDao.observeActiveEvents()

    fun observeEvent(id: Long): Flow<EventEntity?> = eventDao.observeEvent(id)

    fun observeSummaries(): Flow<List<EventSummary>> = statsDao.observeEventSummaries()

    fun observeSummary(id: Long): Flow<EventSummary?> = statsDao.observeEventSummary(id)

    fun observeDashboard(): Flow<DashboardStats> = statsDao.observeDashboard()

    suspend fun getEvent(id: Long): EventEntity? = eventDao.getEvent(id)

    suspend fun enrollmentCount(eventId: Long): Long = eventDao.enrollmentCount(eventId)

    suspend fun save(event: EventEntity): SaveResult = try {
        if (event.id == 0L) {
            SaveResult.Success(eventDao.insert(event))
        } else {
            eventDao.update(event)
            SaveResult.Success(event.id)
        }
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not save the event")
    }

    suspend fun setArchived(eventId: Long, archived: Boolean) {
        eventDao.setArchived(eventId, archived)
    }

    suspend fun delete(id: Long): SaveResult = try {
        eventDao.delete(id)
        SaveResult.Success(id)
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Could not delete the event")
    }
}
