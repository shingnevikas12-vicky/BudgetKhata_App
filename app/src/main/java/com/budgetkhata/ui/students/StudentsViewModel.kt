package com.budgetkhata.ui.students

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.model.StudentGroup
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentsUiState(
    val groups: List<StudentGroup> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val query: String = "",
    val statusFilter: String? = null,
    val eventFilter: Long? = null,
    val loading: Boolean = true
) {
    val requiredPaise: Long get() = groups.sumOf { it.requiredPaise }
    val paidPaise: Long get() = groups.sumOf { it.paidPaise }
    val pendingPaise: Long get() = groups.sumOf { it.remainingPaise }
}

private data class Filters(
    val query: String,
    val status: String?,
    val eventId: Long?,
    val events: List<EventEntity>
)

class StudentsViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _query = MutableStateFlow(savedStateHandle.get<String>("query").orEmpty())
    private val _status = MutableStateFlow(
        savedStateHandle.get<String>("status")?.takeIf { it.isNotBlank() }
    )
    private val _eventId = MutableStateFlow<Long?>(null)

    var errorMessage by mutableStateOf<String?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StudentsUiState> = combine(
        _query,
        _status,
        _eventId,
        container.eventRepository.observeActiveEvents()
    ) { query, status, eventId, events ->
        Filters(query, status, eventId, events)
    }.flatMapLatest { filters ->
        container.enrollmentRepository
            .observeRows(query = filters.query, eventId = filters.eventId)
            .map { rows -> toUiState(filters, rows) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentsUiState(loading = true)
    )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onStatusChange(value: String?) {
        _status.value = value
    }

    fun onEventChange(eventId: Long?) {
        _eventId.value = eventId
    }

    fun clearError() {
        errorMessage = null
    }

    fun deleteStudent(studentId: Long) {
        viewModelScope.launch {
            when (val result = container.studentRepository.delete(studentId)) {
                is SaveResult.Error -> errorMessage = result.message
                is SaveResult.Success -> Unit
            }
        }
    }

    private fun toUiState(
        filters: Filters,
        rows: List<com.budgetkhata.data.local.model.EnrollmentRow>
    ): StudentsUiState {
        val groups = rows
            .groupBy { it.studentId }
            .map { (studentId, enrollments) ->
                val first = enrollments.first()
                StudentGroup(
                    studentId = studentId,
                    name = first.studentName,
                    phone = first.studentPhone,
                    isArchived = first.studentArchived,
                    enrollments = enrollments.sortedBy { it.eventName.lowercase() }
                )
            }
            .filter { group ->
                val status = filters.status
                status == null || group.enrollments.any { it.status.label.equals(status, ignoreCase = true) }
            }
            .sortedBy { it.name.lowercase() }

        return StudentsUiState(
            groups = groups,
            events = filters.events,
            query = filters.query,
            statusFilter = filters.status,
            eventFilter = filters.eventId,
            loading = false
        )
    }
}
