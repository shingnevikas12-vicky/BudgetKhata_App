package com.budgetkhata.ui.students

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetkhata.R
import com.budgetkhata.core.Money
import com.budgetkhata.core.PhoneUtils
import com.budgetkhata.core.SaveResult
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.entity.StudentEntity
import com.budgetkhata.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class StudentFormState(
    val name: String = "",
    val phone: String = "",
    val altPhone: String = "",
    val notes: String = "",
    val eventId: Long? = null,
    val requiredAmount: String = "",
    val nameError: Int? = null,
    val phoneError: Int? = null,
    val altPhoneError: Int? = null,
    val amountError: Int? = null
)

class StudentEditViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId") ?: 0L

    /** Set once the student row exists, so a retry after a failed enrollment updates instead of duplicating. */
    private var persistedStudentId: Long = 0L

    var form by mutableStateOf(StudentFormState())
        private set
    var events by mutableStateOf<List<EventEntity>>(emptyList())
        private set
    var isNew by mutableStateOf(studentId == 0L)
        private set
    var loading by mutableStateOf(studentId != 0L)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            events = container.eventRepository.observeActiveEvents().first()
            if (studentId != 0L) {
                container.studentRepository.getStudent(studentId)?.let { student ->
                    form = StudentFormState(
                        name = student.name,
                        phone = student.phone,
                        altPhone = student.altPhone.orEmpty(),
                        notes = student.notes.orEmpty()
                    )
                }
                loading = false
            }
        }
    }

    fun updateName(value: String) {
        form = form.copy(name = value, nameError = null)
    }

    fun updatePhone(value: String) {
        form = form.copy(phone = value, phoneError = null)
    }

    fun updateAltPhone(value: String) {
        form = form.copy(altPhone = value, altPhoneError = null)
    }

    fun updateNotes(value: String) {
        form = form.copy(notes = value)
    }

    fun updateEvent(eventId: Long?) {
        form = form.copy(eventId = eventId, amountError = null)
    }

    fun updateAmount(value: String) {
        form = form.copy(requiredAmount = value, amountError = null)
    }

    fun dismissMessage() {
        message = null
    }

    fun save(context: Context, onSaved: (Long) -> Unit) {
        if (saving) return
        viewModelScope.launch {
            val settings = container.settingsRepository.current()
            val countryCode = settings.defaultCountryCode

            val nameError = Validation.personName(form.name)
            val phoneError = Validation.phone(form.phone, countryCode)
            val altPhoneError = Validation.optionalPhone(form.altPhone, countryCode)
            val requiredPaise = Money.parseToPaise(form.requiredAmount)
            val amountError = if (isNew && form.eventId != null) {
                Validation.contribution(requiredPaise)
            } else {
                null
            }

            if (nameError != null || phoneError != null || altPhoneError != null || amountError != null) {
                form = form.copy(
                    nameError = nameError,
                    phoneError = phoneError,
                    altPhoneError = altPhoneError,
                    amountError = amountError
                )
                return@launch
            }

            val normalizedPhone = PhoneUtils.normalize(form.phone, countryCode) ?: return@launch
            val duplicate = container.studentRepository.findByPhone(normalizedPhone)
            val effectiveId = if (persistedStudentId != 0L) persistedStudentId else studentId
            if (duplicate != null && duplicate.id != effectiveId) {
                // We don't have a specific error res for this yet, so using a raw string for now
                // Or I should add it to strings.xml
                message = "Another student already uses this mobile number"
                return@launch
            }

            saving = true
            val entity = StudentEntity(
                id = effectiveId,
                name = form.name.trim(),
                phone = normalizedPhone,
                altPhone = form.altPhone.takeIf { it.isNotBlank() }?.let {
                    PhoneUtils.normalize(it, countryCode) ?: it.trim()
                },
                notes = form.notes.trim().takeIf { it.isNotEmpty() }
            )

            when (val result = container.studentRepository.save(entity)) {
                is SaveResult.Error -> message = result.message
                is SaveResult.Success -> {
                    persistedStudentId = result.id
                    val eventId = form.eventId
                    if (isNew && eventId != null && requiredPaise != null) {
                        when (val enrollment = container.enrollmentRepository.enroll(result.id, eventId, requiredPaise)) {
                            is SaveResult.Error -> {
                                saving = false
                                message = "Student saved, but adding them to the event failed: ${enrollment.message}. " +
                                    "Press Save again to retry."
                                return@launch
                            }

                            is SaveResult.Success -> Unit
                        }
                    }
                    saving = false
                    onSaved(result.id)
                    return@launch
                }
            }
            saving = false
        }
    }
}
