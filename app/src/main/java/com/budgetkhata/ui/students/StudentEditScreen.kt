package com.budgetkhata.ui.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTextField
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.ErrorBanner
import com.budgetkhata.ui.components.EventSelector
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.MoneyTextField
import com.budgetkhata.ui.components.SectionHeader

@Composable
fun StudentEditScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> StudentEditViewModel(app, handle) }
    val form = viewModel.form
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.isNew) stringResource(R.string.add_student) else stringResource(R.string.edit_student),
                onBack = onBack
            )
        }
    ) { padding ->
        if (viewModel.loading) {
            LoadingBox(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.message?.let { message ->
                ErrorBanner(message = message, onRetry = viewModel::dismissMessage)
            }

            SectionHeader(
                title = stringResource(R.string.student_details),
                subtitle = stringResource(R.string.mobile_numbers_msg)
            )

            AppTextField(
                value = form.name,
                onValueChange = viewModel::updateName,
                label = stringResource(R.string.full_name),
                isError = form.nameError != null,
                errorText = form.nameError
            )

            AppTextField(
                value = form.phone,
                onValueChange = viewModel::updatePhone,
                label = stringResource(R.string.mobile_number),
                placeholder = "98765 43210",
                keyboardType = KeyboardType.Phone,
                isError = form.phoneError != null,
                errorText = form.phoneError,
                supportingText = stringResource(R.string.country_code_msg)
            )

            AppTextField(
                value = form.altPhone,
                onValueChange = viewModel::updateAltPhone,
                label = stringResource(R.string.alternate_number),
                keyboardType = KeyboardType.Phone,
                isError = form.altPhoneError != null,
                errorText = form.altPhoneError
            )

            AppTextField(
                value = form.notes,
                onValueChange = viewModel::updateNotes,
                label = stringResource(R.string.notes_optional),
                singleLine = false,
                maxLines = 3
            )

            if (viewModel.isNew) {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    title = stringResource(R.string.add_to_event) + " (" + stringResource(R.string.optional) + ")",
                    subtitle = stringResource(R.string.choose_event_msg)
                )

                EventSelector(
                    events = viewModel.events,
                    selectedEventId = form.eventId,
                    onSelect = { id -> viewModel.updateEvent(id) },
                    enabled = viewModel.events.isNotEmpty()
                )

                if (viewModel.events.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_active_events_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (form.eventId != null) {
                    TextButton(onClick = { viewModel.updateEvent(null) }) {
                        Text(stringResource(R.string.do_not_add_to_event))
                    }
                }

                if (form.eventId != null) {
                    MoneyTextField(
                        value = form.requiredAmount,
                        onValueChange = viewModel::updateAmount,
                        label = stringResource(R.string.contribution_amount),
                        isError = form.amountError != null,
                        errorText = form.amountError,
                        supportingText = stringResource(R.string.required_contribution_msg)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(context, onSaved) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.saving) stringResource(R.string.loading) else stringResource(R.string.save) + " " + stringResource(R.string.student))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
            }

            Text(
                text = stringResource(R.string.payments_recorded_msg),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
