package com.budgetkhata.ui.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.model.ExpenseRow
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val expenses: List<ExpenseRow> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val eventFilter: Long? = null,
    val loading: Boolean = true
) {
    val totalPaise: Long get() = expenses.sumOf { it.amountPaise }
    val grouped: List<Pair<String, List<ExpenseRow>>> get() = expenses.groupBy { it.eventName }.toList()
}

class ExpensesViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _eventFilter = MutableStateFlow(
        savedStateHandle.get<Long>("eventId")?.takeIf { it != 0L }
    )

    var message by mutableStateOf<String?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ExpensesUiState> = combine(
        _eventFilter.flatMapLatest { eventId -> container.expenseRepository.observeExpenses(eventId) },
        _eventFilter,
        container.eventRepository.observeEvents()
    ) { expenses, filter, events ->
        ExpensesUiState(
            expenses = expenses,
            events = events,
            eventFilter = filter,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState()
    )

    fun setFilter(eventId: Long?) {
        _eventFilter.value = eventId
    }

    fun dismissMessage() {
        message = null
    }

    fun delete(expenseId: Long) {
        viewModelScope.launch {
            when (val result = container.expenseRepository.delete(expenseId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> message = "Expense deleted"
            }
        }
    }
}
