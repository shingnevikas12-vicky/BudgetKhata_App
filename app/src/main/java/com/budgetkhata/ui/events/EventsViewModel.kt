package com.budgetkhata.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventsUiState(
    val summaries: List<EventSummary> = emptyList(),
    val loading: Boolean = true
) {
    val active: List<EventSummary> get() = summaries.filter { !it.eventArchived }
    val archived: List<EventSummary> get() = summaries.filter { it.eventArchived }
    val expectedPaise: Long get() = active.sumOf { it.expectedPaise }
    val collectedPaise: Long get() = active.sumOf { it.collectedPaise }
    val pendingPaise: Long get() = active.sumOf { it.pendingPaise }
}

class EventsViewModel(private val container: AppContainer) : ViewModel() {

    var message by mutableStateOf<String?>(null)
        private set

    val state: StateFlow<EventsUiState> = container.eventRepository.observeSummaries()
        .map { EventsUiState(summaries = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EventsUiState()
        )

    fun dismissMessage() {
        message = null
    }

    fun setArchived(eventId: Long, archived: Boolean) {
        viewModelScope.launch {
            container.eventRepository.setArchived(eventId, archived)
            message = if (archived) "Event archived" else "Event restored"
        }
    }

    fun delete(eventId: Long) {
        viewModelScope.launch {
            container.eventRepository.delete(eventId)
            message = "Event deleted"
        }
    }
}
