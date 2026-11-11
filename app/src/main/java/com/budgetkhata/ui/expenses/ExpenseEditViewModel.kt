package com.budgetkhata.ui.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.core.Dates
import com.budgetkhata.core.Money
import com.budgetkhata.core.SaveResult
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.ExpenseCategory
import com.budgetkhata.data.local.entity.ExpenseEntity
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.budgetkhata.R

data class ExpenseFormState(
    val eventId: Long? = null,
    val title: String = "",
    val amount: String = "",
    val epochDay: Long = Dates.todayEpochDay(),
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val notes: String = "",
    val receiptUri: String? = null,
    val titleError: Int? = null,
    val amountError: Int? = null,
    val eventError: Int? = null
)

class ExpenseEditViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val expenseId: Long = savedStateHandle.get<Long>("expenseId") ?: 0L
    private val initialEventId: Long? = savedStateHandle.get<Long>("eventId")?.takeIf { it != 0L }

    var form by mutableStateOf(ExpenseFormState(eventId = initialEventId))
        private set
    var events by mutableStateOf<List<EventEntity>>(emptyList())
        private set
    var isNew by mutableStateOf(expenseId == 0L)
        private set
    var loading by mutableStateOf(expenseId != 0L)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            events = container.eventRepository.observeActiveEvents().first()
            if (expenseId != 0L) {
                container.expenseRepository.getExpense(expenseId)?.let { expense ->
                    form = ExpenseFormState(
                        eventId = expense.eventId,
                        title = expense.title,
                        amount = if (expense.amountPaise % 100L == 0L) {
                            (expense.amountPaise / 100L).toString()
                        } else {
                            Money.upiAmount(expense.amountPaise)
                        },
                        epochDay = expense.spentEpochDay,
                        category = expense.category,
                        notes = expense.notes.orEmpty(),
                        receiptUri = expense.receiptUri
                    )
                }
                loading = false
            }
        }
    }

    fun updateEvent(eventId: Long) {
        form = form.copy(eventId = eventId, eventError = null)
    }

    fun updateTitle(value: String) {
        form = form.copy(title = value, titleError = null)
    }

    fun updateAmount(value: String) {
        form = form.copy(amount = value, amountError = null)
    }

    fun updateDate(epochDay: Long) {
        form = form.copy(epochDay = epochDay)
    }

    fun updateCategory(category: ExpenseCategory) {
        form = form.copy(category = category)
    }

    fun updateNotes(value: String) {
        form = form.copy(notes = value)
    }

    fun updateReceipt(uri: String?) {
        form = form.copy(receiptUri = uri)
    }

    fun dismissMessage() {
        message = null
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        val titleResError = Validation.expenseTitle(form.title)
        val amount = Money.parseToPaise(form.amount)
        val amountResError = Validation.expenseAmount(amount)
        // Note: Using a fixed resource if we had one, but keeping logic consistent
        val eventResError = if (form.eventId == null) R.string.event_type else null // Reusing for now or add new

        if (titleResError != null || amountResError != null || eventResError != null) {
            form = form.copy(titleError = titleResError, amountError = amountResError, eventError = eventResError)
            return
        }

        val eventId = form.eventId ?: return
        val paise = amount ?: return

        viewModelScope.launch {
            saving = true
            val entity = ExpenseEntity(
                id = expenseId,
                eventId = eventId,
                title = form.title.trim(),
                amountPaise = paise,
                spentEpochDay = form.epochDay,
                category = form.category,
                receiptUri = form.receiptUri,
                notes = form.notes.trim().takeIf { it.isNotEmpty() }
            )
            when (val result = container.expenseRepository.save(entity)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> {
                    saving = false
                    onSaved()
                    return@launch
                }
            }
            saving = false
        }
    }
}
