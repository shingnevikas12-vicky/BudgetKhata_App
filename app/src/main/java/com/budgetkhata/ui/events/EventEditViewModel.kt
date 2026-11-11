package com.budgetkhata.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.core.Dates
import com.budgetkhata.core.SaveResult
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.launch

data class EventFormState(
    val name: String = "",
    val type: EventType = EventType.JATRA,
    val description: String = "",
    val startEpochDay: Long = Dates.todayEpochDay(),
    val nameError: Int? = null
)

class EventEditViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: 0L

    var form by mutableStateOf(EventFormState())
        private set
    var isNew by mutableStateOf(eventId == 0L)
        private set
    var loading by mutableStateOf(eventId != 0L)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var isArchived by mutableStateOf(false)
        private set

    init {
        if (eventId != 0L) {
            viewModelScope.launch {
                container.eventRepository.getEvent(eventId)?.let { event ->
                    isArchived = event.isArchived
                    form = EventFormState(
                        name = event.name,
                        type = event.type,
                        description = event.description.orEmpty(),
                        startEpochDay = event.startEpochDay ?: Dates.todayEpochDay()
                    )
                }
                loading = false
            }
        }
    }

    fun updateName(value: String) {
        form = form.copy(name = value, nameError = null)
    }

    fun updateType(value: EventType) {
        form = form.copy(type = value)
    }

    fun updateDescription(value: String) {
        form = form.copy(description = value)
    }

    fun updateStartDate(epochDay: Long) {
        form = form.copy(startEpochDay = epochDay)
    }

    fun dismissMessage() {
        message = null
    }

    fun save(onSaved: (Long) -> Unit) {
        if (saving) return
        val nameErrorRes = Validation.eventName(form.name)
        if (nameErrorRes != null) {
            form = form.copy(nameError = nameErrorRes)
            return
        }

        viewModelScope.launch {
            saving = true
            val entity = EventEntity(
                id = eventId,
                name = form.name.trim(),
                type = form.type,
                description = form.description.trim().takeIf { it.isNotEmpty() },
                startEpochDay = form.startEpochDay,
                isArchived = isArchived
            )
            when (val result = container.eventRepository.save(entity)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> {
                    saving = false
                    onSaved(result.id)
                    return@launch
                }
            }
            saving = false
        }
    }
}
