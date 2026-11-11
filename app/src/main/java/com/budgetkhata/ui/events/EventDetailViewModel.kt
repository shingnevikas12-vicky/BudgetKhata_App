package com.budgetkhata.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.core.Money
import com.budgetkhata.core.SaveResult
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.data.local.entity.StudentEntity
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.data.local.model.ExpenseRow
import com.budgetkhata.data.local.model.PaymentRow
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: EventEntity? = null,
    val summary: EventSummary? = null,
    val enrollments: List<EnrollmentRow> = emptyList(),
    val payments: List<PaymentRow> = emptyList(),
    val expenses: List<ExpenseRow> = emptyList(),
    val loading: Boolean = true
)

class EventDetailViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val eventId: Long = savedStateHandle.get<Long>("eventId") ?: 0L

    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var dialogError by mutableStateOf<String?>(null)
        private set

    val state: StateFlow<EventDetailUiState> = combine(
        container.eventRepository.observeEvent(eventId),
        container.eventRepository.observeSummary(eventId),
        container.enrollmentRepository.observeRows(eventId = eventId, includeArchived = true),
        container.paymentRepository.observePayments(eventId = eventId),
        container.expenseRepository.observeExpenses(eventId = eventId)
    ) { event, summary, enrollments, payments, expenses ->
        EventDetailUiState(
            event = event,
            summary = summary,
            enrollments = enrollments,
            payments = payments,
            expenses = expenses,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventDetailUiState()
    )

    val students: StateFlow<List<StudentEntity>> = container.studentRepository
        .observeStudents(query = "", includeArchived = false)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun dismissMessage() {
        message = null
    }

    fun clearDialogError() {
        dialogError = null
    }

    fun recordPayment(
        enrollment: EnrollmentRow,
        amountPaise: Long,
        epochDay: Long,
        method: PaymentMethod,
        reference: String?,
        notes: String?,
        allowOverpayment: Boolean,
        onSuccess: () -> Unit
    ) {
        if (saving) return
        viewModelScope.launch {
            saving = true
            when (
                val result = container.paymentRepository.recordPayment(
                    enrollmentId = enrollment.enrollmentId,
                    amountPaise = amountPaise,
                    paidEpochDay = epochDay,
                    method = method,
                    reference = reference,
                    notes = notes,
                    allowOverpayment = allowOverpayment
                )
            ) {
                is SaveResult.Error -> dialogError = result.message
                is SaveResult.Success -> {
                    dialogError = null
                    message = "Payment of ${Money.format(amountPaise)} recorded"
                    onSuccess()
                }
            }
            saving = false
        }
    }

    fun enrollStudents(studentIds: List<Long>, amountPaise: Long, onDone: () -> Unit) {
        if (studentIds.isEmpty()) return
        viewModelScope.launch {
            var failures = 0
            var failureMessage: String? = null
            studentIds.forEach { studentId ->
                when (val result = container.enrollmentRepository.enroll(studentId, eventId, amountPaise)) {
                    is SaveResult.Error -> {
                        failures += 1
                        failureMessage = result.message
                    }

                    is SaveResult.Success -> Unit
                }
            }
            message = when {
                failures == 0 -> "${studentIds.size} student(s) added to the event"
                failures == studentIds.size -> failureMessage ?: "Could not add the students"
                else -> "${studentIds.size - failures} added, $failures failed"
            }
            onDone()
        }
    }

    fun updateContribution(enrollment: EnrollmentRow, amountPaise: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (
                val result = container.enrollmentRepository.updateContribution(
                    enrollmentId = enrollment.enrollmentId,
                    requiredPaise = amountPaise,
                    note = enrollment.note
                )
            ) {
                is SaveResult.Error -> dialogError = result.message
                is SaveResult.Success -> {
                    dialogError = null
                    message = "Contribution updated"
                    onSuccess()
                }
            }
        }
    }

    fun removeEnrollment(enrollmentId: Long) {
        viewModelScope.launch {
            when (val result = container.enrollmentRepository.remove(enrollmentId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> message = "Student removed from the event"
            }
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            when (val result = container.paymentRepository.deletePayment(paymentId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> message = "Payment entry deleted"
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            when (val result = container.expenseRepository.delete(expenseId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> message = "Expense deleted"
            }
        }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch {
            container.eventRepository.setArchived(eventId, archived)
            message = if (archived) "Event archived" else "Event restored"
        }
    }

    fun deleteEvent(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.eventRepository.delete(eventId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> onDeleted()
            }
        }
    }
}
