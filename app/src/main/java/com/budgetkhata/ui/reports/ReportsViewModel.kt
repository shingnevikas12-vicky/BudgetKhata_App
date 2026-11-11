package com.budgetkhata.ui.reports

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.R
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.data.local.model.ExpenseRow
import com.budgetkhata.di.AppContainer
import com.budgetkhata.domain.ReportBuilder
import com.budgetkhata.domain.ReportTable
import com.budgetkhata.domain.ReportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val rows: List<EnrollmentRow> = emptyList(),
    val expenses: List<ExpenseRow> = emptyList(),
    val summaries: List<EventSummary> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val eventFilter: Long? = null,
    val selectedReport: ReportType = ReportType.SUMMARY,
    val loading: Boolean = true
) {
    fun getFilterLabel(context: Context): String =
        eventFilter?.let { id -> events.firstOrNull { it.id == id }?.name } ?: context.getString(R.string.all_events)

    private val filteredRows: List<EnrollmentRow>
        get() = if (eventFilter == null) rows else rows.filter { it.eventId == eventFilter }

    private val filteredExpenses: List<ExpenseRow>
        get() = if (eventFilter == null) expenses else expenses.filter { it.eventId == eventFilter }

    private val filteredSummaries: List<EventSummary>
        get() = if (eventFilter == null) {
            summaries
        } else {
            summaries.filter { it.eventId == eventFilter }
        }

    fun buildTable(context: Context, type: ReportType = selectedReport): ReportTable = ReportBuilder.build(
        context = context,
        type = type,
        rows = filteredRows,
        expenses = filteredExpenses,
        summaries = filteredSummaries,
        filterLabel = getFilterLabel(context)
    )

    fun buildAllTables(context: Context): List<ReportTable> = ReportType.entries.map { buildTable(context, it) }
}

class ReportsViewModel(
    container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventFilter = MutableStateFlow(savedStateHandle.get<Long>("eventId")?.takeIf { it != 0L })
    private val selectedReport = MutableStateFlow(ReportType.SUMMARY)

    val state: StateFlow<ReportsUiState> = combine(
        container.enrollmentRepository.observeRows(includeArchived = true),
        container.expenseRepository.observeExpenses(null),
        container.eventRepository.observeSummaries(),
        container.eventRepository.observeEvents(),
        combine(eventFilter, selectedReport) { filter, report -> filter to report }
    ) { rows, expenses, summaries, events, filters ->
        ReportsUiState(
            rows = rows,
            expenses = expenses,
            summaries = summaries,
            events = events,
            eventFilter = filters.first,
            selectedReport = filters.second,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    fun selectReport(type: ReportType) {
        selectedReport.value = type
    }

    fun selectEvent(eventId: Long?) {
        eventFilter.value = eventId
    }
}
