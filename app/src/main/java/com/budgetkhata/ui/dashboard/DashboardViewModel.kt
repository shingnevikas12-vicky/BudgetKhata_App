package com.budgetkhata.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.data.local.model.DashboardStats
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.data.local.model.PaymentRow
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val summaries: List<EventSummary> = emptyList(),
    val recentPayments: List<PaymentRow> = emptyList()
) {
    val hasData: Boolean get() = stats.studentCount > 0 || stats.eventCount > 0
}

class DashboardViewModel(container: AppContainer) : ViewModel() {

    val state: StateFlow<DashboardUiState?> = combine(
        container.eventRepository.observeDashboard(),
        container.eventRepository.observeSummaries(),
        container.paymentRepository.observePayments()
    ) { stats, summaries, payments ->
        DashboardUiState(
            stats = stats,
            summaries = summaries.filter { !it.eventArchived },
            recentPayments = payments.take(4)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
}
