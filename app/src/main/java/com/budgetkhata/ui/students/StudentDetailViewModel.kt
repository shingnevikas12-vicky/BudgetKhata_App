package com.budgetkhata.ui.students

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
import com.budgetkhata.data.local.model.PaymentRow
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentDetailUiState(
    val student: StudentEntity? = null,
    val enrollments: List<EnrollmentRow> = emptyList(),
    val payments: List<PaymentRow> = emptyList(),
    val loading: Boolean = true
) {
    val requiredPaise: Long get() = enrollments.sumOf { it.requiredPaise }
    val paidPaise: Long get() = enrollments.sumOf { it.paidPaise }
    val pendingPaise: Long get() = (requiredPaise - paidPaise).coerceAtLeast(0L)
}

class StudentDetailViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val studentId: Long = savedStateHandle.get<Long>("studentId") ?: 0L

    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var dialogError by mutableStateOf<String?>(null)
        private set
    var events by mutableStateOf<List<EventEntity>>(emptyList())
        private set

    val state: StateFlow<StudentDetailUiState> = combine(
        container.studentRepository.observeStudent(studentId),
        container.enrollmentRepository.observeRows(studentId = studentId, includeArchived = true),
        container.paymentRepository.observePayments(studentId = studentId)
    ) { student, enrollments, payments ->
        StudentDetailUiState(
            student = student,
            enrollments = enrollments,
            payments = payments,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentDetailUiState()
    )

    init {
        viewModelScope.launch {
            events = container.eventRepository.observeActiveEvents().first()
        }
    }

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
                    message = "Payment of ${Money.format(amountPaise)} recorded for ${enrollment.eventName}"
                    onSuccess()
                }
            }
            saving = false
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

    fun removeEnrollment(enrollmentId: Long) {
        viewModelScope.launch {
            when (val result = container.enrollmentRepository.remove(enrollmentId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> message = "Removed from the event"
            }
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

    fun addToEvent(eventId: Long, amountPaise: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.enrollmentRepository.enroll(studentId, eventId, amountPaise)) {
                is SaveResult.Error -> dialogError = result.message
                is SaveResult.Success -> {
                    dialogError = null
                    message = "Student added to the event"
                    onSuccess()
                }
            }
        }
    }

    fun deleteStudent(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.studentRepository.delete(studentId)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> onDeleted()
            }
        }
    }
}
